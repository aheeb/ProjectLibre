package com.projectlibre1.pm.graphic.laf;

import java.awt.Color;
import java.awt.Font;
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
	public static final Color TEXT_PRIMARY = new Color(0x1F2329);
	/** Light accent tint behind selected rows and cells. */
	public static final Color SELECTION = new Color(0xDCE7FC);
	/** Selection tint in the row-number gutter and headers. */
	public static final Color SELECTION_CHROME = new Color(0xC9DAFA);
	/** "Today" marker in the timeline charts. */
	public static final Color TODAY = new Color(0xE8590C);
	/** Soft accent tint for rows that are "on" (e.g. resources already assigned). */
	public static final Color ACCENT_SOFT = new Color(0xE4EDFD);
	/** Soft amber tint for rows that are partly on (e.g. assigned to only some selected tasks). */
	public static final Color WARNING_SOFT = new Color(0xFFF1CC);
	/** Selection fill behind dark text where ACCENT would be too strong (e.g. selected calendar days). */
	public static final Color SELECTION_SOFT = new Color(0xC7D9FB);
	public static final Color TEXT_DISABLED = new Color(0xA8ADB4);
	/** Overallocation and other problems the user has to fix (text, glyphs, histogram bars). */
	public static final Color DANGER = new Color(0xD93B41);
	/** Soft red tint behind cells with a problem (e.g. a week someone is over 100%). */
	public static final Color DANGER_SOFT = new Color(0xFDE8E8);
	/** Translucent fills for flat buttons and tabs (hover, pressed, selected). */
	public static final Color HOVER = new Color(0, 0, 0, 13);
	public static final Color PRESSED = new Color(0, 0, 0, 26);
	public static final Color SELECTED = new Color(0x2F, 0x6F, 0xEB, 36);

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
		// Gantt labels and network boxes use the UI font instead of the old serif default
		Font uiFont = UIManager.getFont("Label.font");
		if (uiFont != null) {
			Environment.setFont(uiFont.getFamily() + " PLAIN 11", Environment.GANTT_ANNOTATIONS_FONT);
			Environment.setFont(uiFont.getFamily() + " PLAIN 12", Environment.NETWORK_FONT);
		}
	}

	/**
	 * Scroll pane border without the accent focus ring: for the main grids, where the focused cell
	 * already shows where the keyboard is and a ring around the whole sheet only adds noise.
	 */
	public static javax.swing.border.Border quietScrollPaneBorder() {
		if (!isFlat()) return UIManager.getBorder("ScrollPane.border");
		// wrapped so it isn't a UIResource: JTable and the look and feel replace UIResource borders with their own
		return new javax.swing.border.CompoundBorder(new com.formdev.flatlaf.ui.FlatScrollPaneBorder() {
			@Override
			protected boolean isFocused(java.awt.Component c) {
				return false;
			}
		}, null);
	}

	public static boolean isFlat() {
		return UIManager.getLookAndFeel() instanceof FlatLaf;
	}
}
