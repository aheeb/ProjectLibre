package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.Icon;

import com.projectlibre1.pm.graphic.laf.ModernLaf;

/** Small row status glyphs, drawn to match {@link InfoGlyphIcon}: a red "!" for a problem, a hollow dot for "nothing yet". */
public class StatusGlyphIcon implements Icon {
	private static final int SIZE = 14;
	public static final StatusGlyphIcon WARNING = new StatusGlyphIcon(true, ModernLaf.DANGER);
	public static final StatusGlyphIcon EMPTY = new StatusGlyphIcon(false, ModernLaf.TEXT_DISABLED);

	private final boolean warning;
	private final Color color;

	private StatusGlyphIcon(boolean warning, Color color) {
		this.warning = warning;
		this.color = color;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(x, y);
		g2.setColor(color);
		if (warning) {
			g2.fill(new Ellipse2D.Double(1, 1, SIZE - 2, SIZE - 2));
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(new Line2D.Double(SIZE / 2.0, 3.8, SIZE / 2.0, 7.8));
			g2.fill(new Ellipse2D.Double(SIZE / 2.0 - 0.95, 9.3, 1.9, 1.9));
		} else {
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(new Ellipse2D.Double(3, 3, SIZE - 6, SIZE - 6));
		}
		g2.dispose();
	}

	public int getIconWidth() {
		return SIZE;
	}

	public int getIconHeight() {
		return SIZE;
	}
}
