package org.pushingpixels.flamingo.internal.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import com.projectlibre1.pm.graphic.laf.ModernLaf;

/**
 * Flat, FlatLaf-consistent painting for the ribbon: no gradients or bevels, translucent rounded fills for
 * hover / pressed / selected states and hairline separators.
 */
public class FlatRibbonStyle {
	public static final int ARC = 6;

	public static boolean isActive() {
		return ModernLaf.isFlat();
	}

	public static Color background() {
		return ModernLaf.CHROME;
	}

	public static Color hairline() {
		return ModernLaf.HAIRLINE;
	}

	/** Font for band titles: one step smaller than the button font. */
	public static Font smallFont(Font base) {
		return base.deriveFont(Math.max(10f, base.getSize2D() - 2f));
	}

	public static void paintButtonBackground(Graphics g, Rectangle r, boolean enabled, boolean rollover,
			boolean pressed, boolean selected) {
		Color fill = null;
		if (selected)
			fill = ModernLaf.SELECTED;
		if (enabled && pressed)
			fill = ModernLaf.PRESSED;
		else if (enabled && rollover && !selected)
			fill = ModernLaf.HOVER;
		if (fill == null)
			return;
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(fill);
		g2d.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, ARC * 2, ARC * 2);
		if (selected && rollover && !pressed) {
			g2d.setColor(ModernLaf.HOVER);
			g2d.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, ARC * 2, ARC * 2);
		}
		g2d.dispose();
	}
}
