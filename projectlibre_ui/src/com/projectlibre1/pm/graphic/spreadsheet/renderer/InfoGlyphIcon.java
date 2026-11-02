package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.Icon;

import com.projectlibre1.pm.graphic.laf.ModernLaf;

/**
 * Small outlined "i" drawn as vectors, used as the header of the indicators column.
 */
public class InfoGlyphIcon implements Icon {
	private static final int SIZE = 14;

	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(x, y);
		g2.setColor(ModernLaf.TEXT_SECONDARY);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(new Ellipse2D.Double(1, 1, SIZE - 2, SIZE - 2));
		g2.fill(new Ellipse2D.Double(SIZE / 2.0 - 0.9, 3.6, 1.8, 1.8));
		g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(new Line2D.Double(SIZE / 2.0, 6.8, SIZE / 2.0, 10.4));
		g2.dispose();
	}

	public int getIconWidth() {
		return SIZE;
	}

	public int getIconHeight() {
		return SIZE;
	}
}
