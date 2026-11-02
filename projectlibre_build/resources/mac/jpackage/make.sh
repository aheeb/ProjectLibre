#!/bin/bash
# Builds ProjectLibre.app and a DMG per CPU architecture. Run it from packages/jpackage-dmg after
# "ant jpackage-dmg" (or let that target run it: ant jpackage-dmg -Djdk.arm64=... -Djdk.x86_64=...).
#
#   ./make.sh                                         host architecture, JDK from $JAVA_HOME
#   ./make.sh arm64=/path/to/jdk x86_64=/path/to/jdk  one app and DMG per architecture
#
# jpackage cannot cross-build, so each architecture needs a JDK 21 of that architecture (its
# Contents/Home). On an Apple Silicon Mac the x86_64 JDK runs under Rosetta 2:
#   softwareupdate --install-rosetta --agree-to-license
#   mkdir -p /tmp/jdk-x64 && cd /tmp/jdk-x64
#   curl -L -o jdk.tar.gz 'https://api.adoptium.net/v3/binary/latest/21/ga/mac/x64/jdk/hotspot/normal/eclipse'
#   tar xzf jdk.tar.gz          # -> /tmp/jdk-x64/jdk-21.*/Contents/Home
#
# Environment:
#   APPLE_TEAM_ID   sign with "Developer ID Application: ProjectLibre Inc. ($APPLE_TEAM_ID)" and notarize.
#                   Unset: unsigned local build (right-click > Open the first time).
#   NOTARY_PROFILE  notarytool keychain profile (default: notary-profile)
#   SKIP_CDS=1      skip the class-data-sharing training run. The run starts the app for
#                   CDS_SECONDS (default 20) in the logged-in GUI session and makes startup ~10% faster.
#   SKIP_DMG=1      build the app image only
#   EXTRA_MODULES   extra JDK modules, e.g. java.instrument,java.management for profiling

set -e
cd "$(dirname "$0")"

APP_VERSION="@version@"
MAIN_JAR="projectlibre-${APP_VERSION}.jar"

# JDK modules the app uses (jdeps on the fat jar, plus modules it loads reflectively: Groovy needs
# jdk.unsupported, date formats for all locales need jdk.localedata, MS Project import needs
# jdk.charsets, update checks need jdk.crypto.ec, VoiceOver needs jdk.accessibility).
MODULES="java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.naming,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.localedata,jdk.charsets,jdk.crypto.ec,jdk.accessibility,jdk.zipfs"
[ -n "$EXTRA_MODULES" ] && MODULES="$MODULES,$EXTRA_MODULES"

# Keep in sync with resources/projectlibre.sh and projectlibre.bat: small initial heap, up to half
# of RAM, G1 for short pauses. FlatLaf loads its native library from the app instead of extracting
# it to a temp directory on every start.
JAVA_OPTIONS=()
for o in -Xms64m -XX:MaxRAMPercentage=50 -XX:+UseG1GC -Dapple.laf.useScreenMenuBar=true '-Dflatlaf.nativeLibraryPath=$APPDIR'; do
	JAVA_OPTIONS+=(--java-options "$o")
done

SIGN_ID=""
[ -n "$APPLE_TEAM_ID" ] && SIGN_ID="Developer ID Application: ProjectLibre Inc. ($APPLE_TEAM_ID)"

arch_of() { # arm64 or x86_64, from a Mach-O file
	case "$(file -b "$1")" in
		*arm64*) echo arm64 ;;
		*x86_64*) echo x86_64 ;;
	esac
}

build() {
	local ARCH="$1" JDK="$2"
	local JAVA="$JDK/bin/java"
	[ -x "$JAVA" ] || { echo "No JDK at $JDK (expected its Contents/Home)"; exit 1; }
	[ "$(arch_of "$JAVA")" = "$ARCH" ] || { echo "$JDK is a $(arch_of "$JAVA") JDK, not $ARCH"; exit 1; }
	echo "== $ARCH with $JDK"

	local WORK="build-$ARCH" OUT="app-$ARCH"
	local APP="$OUT/ProjectLibre.app"
	rm -rf "$WORK" "$OUT"
	mkdir -p "$WORK/input" "$OUT"

	cp "source/$MAIN_JAR" "$WORK/input/"
	cp -R source/license "$WORK/input/"
	unzip -q -j "source/$MAIN_JAR" "com/formdev/flatlaf/natives/libflatlaf-macos-$ARCH.dylib" -d "$WORK/input"

	# Minimal runtime. --generate-cds-archive adds the JDK's class data sharing archive, which
	# jpackage's own runtime lacks; it needs bin/java, so strip the other commands afterwards.
	"$JDK/bin/jlink" --add-modules "$MODULES" --strip-debug --no-man-pages --no-header-files \
		--generate-cds-archive --output "$WORK/runtime"
	find "$WORK/runtime/bin" -type f ! -name java -delete

	"$JDK/bin/jpackage" --type app-image --runtime-image "$WORK/runtime" --input "$WORK/input" --dest "$OUT" \
		--name ProjectLibre --main-jar "$MAIN_JAR" --icon source/projectlibre.icns --app-version "$APP_VERSION" \
		--mac-package-identifier com.projectlibre1.main "${JAVA_OPTIONS[@]}"
	cp Info.plist "$APP/Contents/"

	if [ -z "$SKIP_CDS" ]; then
		# Training run: archive the classes loaded while opening a sample project, ship the archive
		# in the app and point the launcher at it. The archive stays valid when the app is moved.
		local JSA="$APP/Contents/app/projectlibre.jsa"
		echo "CDS training run (${CDS_SECONDS:-20} s)"
		JAVA_TOOL_OPTIONS="-XX:ArchiveClassesAtExit=$PWD/$JSA" "$APP/Contents/MacOS/ProjectLibre" "$PWD/source/sample.mpp" >"$WORK/cds-training.log" 2>&1 &
		local PID=$!
		sleep "${CDS_SECONDS:-20}"
		kill -TERM $PID 2>/dev/null || true
		wait $PID 2>/dev/null || true
		if [ -f "$JSA" ]; then
			echo 'java-options=-XX:SharedArchiveFile=$APPDIR/projectlibre.jsa' >>"$APP/Contents/app/ProjectLibre.cfg"
		else
			echo "CDS training failed, see $WORK/cds-training.log; continuing without app archive"
		fi
	fi

	echo "$(file -b "$APP/Contents/MacOS/ProjectLibre"), $(du -sh "$APP" | cut -f1)"

	if [ -n "$SIGN_ID" ]; then
		# Nested code first (runtime, FlatLaf library), then the bundle, which seals the rest.
		find "$APP/Contents/runtime" "$APP/Contents/app" -type f -exec file "{}" \; | grep "Mach-O" | cut -d: -f1 \
		| while read -r f; do
			codesign --force --timestamp --options runtime --entitlements entitlements.plist --sign "$SIGN_ID" "$f"
		done
		codesign --force --timestamp --options runtime --entitlements entitlements.plist --sign "$SIGN_ID" "$APP"
		codesign --verify --deep --strict "$APP"
	fi

	[ -n "$SKIP_DMG" ] && return
	local DMG="ProjectLibre-${APP_VERSION}-$ARCH.dmg"
	rm -f "$DMG"
	if command -v create-dmg >/dev/null; then
		create-dmg --volname "ProjectLibre" --background "background.png" --volicon "source/projectlibre.icns" \
			--window-pos 200 120 --window-size 600 350 --icon-size 100 --icon "ProjectLibre.app" 180 140 \
			--hide-extension "ProjectLibre.app" --app-drop-link 420 140 --eula source/license/license.txt "$DMG" "$OUT"
	else
		ln -s /Applications "$OUT/Applications"
		hdiutil create -volname ProjectLibre -srcfolder "$OUT" -ov -format UDZO "$DMG"
		rm "$OUT/Applications"
	fi
	if [ -n "$SIGN_ID" ]; then
		codesign --force --timestamp --sign "$SIGN_ID" "$DMG"
		xcrun notarytool submit "$DMG" --keychain-profile "${NOTARY_PROFILE:-notary-profile}" --wait
		xcrun stapler staple "$DMG"
	fi
	echo "Built $DMG"
}

if [ $# -eq 0 ]; then
	JDK="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
	set -- "$(arch_of "$JDK/bin/java")=$JDK"
fi
for spec in "$@"; do
	build "${spec%%=*}" "${spec#*=}"
done
