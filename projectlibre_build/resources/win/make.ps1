# Builds ProjectLibre for Windows: a portable zip and a per-user MSI installer, each with its own
# minimal Java runtime. Run it from packages/jpackage-msi after "ant jpackage-msi".
#
#   .\make.ps1                         JDK from $env:JAVA_HOME
#   .\make.ps1 -Jdk C:\path\to\jdk-21
#
# The CPU architecture follows the JDK: an x64 JDK builds ProjectLibre-<version>-windows-x64.*, an
# arm64 JDK (on Windows on ARM) ProjectLibre-<version>-windows-arm64.*. jpackage cannot cross-build.
# The MSI needs WiX Toolset 3 (candle.exe and light.exe on the PATH); JDK 21's jpackage does not
# support WiX 4 or later. .github/workflows/windows.yml builds both architectures this way.
#
# Environment:
#   SKIP_CDS=1   skip the class-data-sharing training run. The run starts the app with a sample project
#                for CDS_SECONDS (default 30), then quits it, and makes startup faster.
#   SKIP_MSI=1   build the portable zip only (no WiX needed)
param([string]$Jdk = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$AppVersion = '@version@'
$MainJar = "projectlibre-$AppVersion.jar"

# Keep in sync with resources/mac/jpackage/make.sh: the JDK modules the app uses.
$Modules = 'java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.naming,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.localedata,jdk.charsets,jdk.crypto.ec,jdk.accessibility,jdk.zipfs'

# Keep in sync with resources/projectlibre.bat: small initial heap, up to half of RAM, G1 for short
# pauses. FlatLaf loads its native library from the app instead of extracting it to a temp directory.
$JavaOptions = '-Xms64m', '-XX:MaxRAMPercentage=50', '-XX:+UseG1GC', '-Dflatlaf.nativeLibraryPath=$APPDIR' |
	ForEach-Object { '--java-options', $_ }

function Invoke-Tool {
	& $args[0] @($args | Select-Object -Skip 1)
	if ($LASTEXITCODE) { throw "$($args[0]) failed with exit code $LASTEXITCODE" }
}

if (-not $Jdk -or -not (Test-Path "$Jdk\bin\jpackage.exe")) { throw "No JDK 21 at '$Jdk'. Pass -Jdk or set JAVA_HOME." }
$Release = Get-Content "$Jdk\release" -Raw
$Arch = switch -Regex ($Release) {
	'OS_ARCH="aarch64"' { 'arm64'; break }
	'OS_ARCH="(x86_64|amd64)"' { 'x64'; break }
	default { throw "Unsupported JDK architecture in $Jdk\release" }
}
$FlatLafDll = @{ arm64 = 'flatlaf-windows-arm64.dll'; x64 = 'flatlaf-windows-x86_64.dll' }[$Arch]
Write-Host "== windows-$Arch with $Jdk"

$Work = "build-$Arch"
$Out = "out-$Arch"
$Image = "$Work\image\ProjectLibre"
Remove-Item -Recurse -Force $Work, $Out -ErrorAction SilentlyContinue
New-Item -ItemType Directory "$Work\input", $Out | Out-Null

Copy-Item "source\$MainJar" "$Work\input\"
Copy-Item -Recurse source\license "$Work\input\"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$Zip = [IO.Compression.ZipFile]::OpenRead("$PSScriptRoot\source\$MainJar")
try {
	[IO.Compression.ZipFileExtensions]::ExtractToFile($Zip.GetEntry("com/formdev/flatlaf/natives/$FlatLafDll"), "$PSScriptRoot\$Work\input\$FlatLafDll")
} finally { $Zip.Dispose() }

# Minimal runtime. --generate-cds-archive adds the JDK's class data sharing archive, which jpackage's
# own runtime lacks. The launcher needs no command-line tools except java.exe for the CDS archive.
Invoke-Tool "$Jdk\bin\jlink.exe" --add-modules $Modules --strip-debug --no-man-pages --no-header-files `
	--generate-cds-archive --output "$Work\runtime"
Get-ChildItem "$Work\runtime\bin" -Filter *.exe | Where-Object Name -notin 'java.exe', 'javaw.exe' | Remove-Item

Invoke-Tool "$Jdk\bin\jpackage.exe" --type app-image --runtime-image "$Work\runtime" --input "$Work\input" `
	--dest "$Work\image" --name ProjectLibre --main-jar $MainJar --icon source\projectlibre.ico `
	--app-version $AppVersion --vendor ProjectLibre --description 'Project planning' @JavaOptions

if (-not $env:SKIP_CDS) {
	# Training run: archive the classes loaded while opening a sample project and ship the archive in
	# the app. The app quits through DevControl (System.exit), because killing the process on Windows
	# would skip writing the archive.
	$Jsa = "$PSScriptRoot\$Image\app\projectlibre.jsa"
	$Control = New-Item -ItemType Directory "$Work\cds-control"
	$Seconds = if ($env:CDS_SECONDS) { [int]$env:CDS_SECONDS } else { 30 }
	Write-Host "CDS training run ($Seconds s)"
	$env:JAVA_TOOL_OPTIONS = "-XX:ArchiveClassesAtExit=$Jsa -Dprojectlibre.snapshotDir=$($Control.FullName)"
	try {
		$App = Start-Process "$Image\ProjectLibre.exe" -ArgumentList "`"$PSScriptRoot\source\sample.pod`"" -PassThru
		Start-Sleep $Seconds
		Set-Content "$Control\script.groovy" 'System.exit(0)'
		if (-not $App.WaitForExit(90000)) { Stop-Process $App -Force; Write-Host 'The training run did not quit' }
	} finally { Remove-Item Env:JAVA_TOOL_OPTIONS }
	Remove-Item -Recurse "$Image\app\.projectlibre" -ErrorAction SilentlyContinue
	if (Test-Path $Jsa) {
		Add-Content "$Image\app\ProjectLibre.cfg" 'java-options=-XX:SharedArchiveFile=$APPDIR\projectlibre.jsa'
		Write-Host "CDS archive: $([math]::Round((Get-Item $Jsa).Length / 1MB)) MB"
	} else {
		Write-Host 'CDS training failed; continuing without an app archive'
	}
}

Compress-Archive $Image "$Out\ProjectLibre-$AppVersion-windows-$Arch.zip"

if (-not $env:SKIP_MSI) {
	# Per-user install: no admin rights, installs to %LOCALAPPDATA%\ProjectLibre. Keep the upgrade UUID
	# so a new MSI replaces an installed one, whatever its architecture.
	Invoke-Tool "$Jdk\bin\jpackage.exe" --type msi --app-image $Image --dest "$Work\msi" --name ProjectLibre `
		--app-version $AppVersion --vendor ProjectLibre --description 'Project planning' `
		--license-file source\license\license.txt --win-per-user-install --win-menu --win-menu-group ProjectLibre `
		--win-shortcut --win-upgrade-uuid f3b03a3c-b290-475b-a254-8bc59eb20823 `
		--file-associations pod.properties --file-associations mpp.properties
	Move-Item "$Work\msi\*.msi" "$Out\ProjectLibre-$AppVersion-windows-$Arch.msi"
}

Get-ChildItem $Out | ForEach-Object { Write-Host "Built $($_.FullName) ($([math]::Round($_.Length / 1MB)) MB)" }
