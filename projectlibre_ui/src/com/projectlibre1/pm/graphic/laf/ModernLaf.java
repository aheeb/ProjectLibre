package com.projectlibre1.pm.graphic.laf;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.projectlibre1.util.Environment;

/**
 * Installs the application look and feel (FlatLaf) and the shared visual tokens used across the UI.
 * Must run before the first Swing component is created.
 */
public class ModernLaf {
	/** Accent used for selection, focus and primary actions. */
	public static final Color ACCENT = new Color(0x2F6FEB);
	/** Hairline color for grids and separators. */
	public static final Color HAIRLINE = new Color(0xE3E5E8);
	/** App chrome background (ribbon, headers). */
	public static final Color CHROME = new Color(0xF6F7F9);
	public static final Color TEXT_SECONDARY = new Color(0x6B7280);
	/** Soft accent tint for rows that are "on" (e.g. resources already assigned). */
	public static final Color ACCENT_SOFT = new Color(0xE4EDFD);
	/** Soft amber tint for rows that are partly on (e.g. assigned to only some selected tasks). */
	public static final Color WARNING_SOFT = new Color(0xFFF1CC);

	private static boolean installed = false;

	public static synchronized void install() {
		if (installed) return;
		installed = true;
		if (Environment.isMac()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.application.name", "ProjectLibre");
			// Light theme only for now: the Gantt and chart painters still use fixed light colors.
			System.setProperty("apple.awt.application.appearance", "NSAppearanceNameAqua");
		}
		FlatLaf.setPreferredFontFamily(null);
		UIManager.put("Component.accentColor", ACCENT);
		UIManager.put("Component.arc", 8);
		UIManager.put("Button.arc", 8);
		UIManager.put("TextComponent.arc", 6);
		UIManager.put("CheckBox.arc", 4);
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("ScrollBar.showButtons", false);
		UIManager.put("ScrollBar.thumbArc", 999);
		UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
		UIManager.put("ScrollBar.width", 10);
		UIManager.put("Table.showHorizontalLines", true);
		UIManager.put("Table.showVerticalLines", true);
		UIManager.put("Table.gridColor", HAIRLINE);
		UIManager.put("TableHeader.separatorColor", HAIRLINE);
		UIManager.put("TableHeader.bottomSeparatorColor", HAIRLINE);
		UIManager.put("TabbedPane.showTabSeparators", false);
		UIManager.put("SplitPane.dividerSize", 6);
		UIManager.put("SplitPaneDivider.style", "plain");
		UIManager.put("TitlePane.unifiedBackground", true);
		UIManager.put("ToolTip.border", javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
		boolean ok = Environment.isMac() ? FlatMacLightLaf.setup() : FlatLightLaf.setup();
		if (!ok) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isFlat() {
		return UIManager.getLookAndFeel() instanceof FlatLaf;
	}
}
