package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.ImageIcon;

import com.projectlibre1.pm.graphic.laf.ModernLaf;

/**
 * Expand/collapse chevron for summary rows, drawn as vectors so it stays crisp on HiDPI screens.
 * Extends ImageIcon because the name cell keeps its outline icons as ImageIcons.
 */
public class ChevronIcon extends ImageIcon {
	private static final long serialVersionUID = 1L;
	public static final int SIZE = 16;
	private final boolean expanded;

	public ChevronIcon(boolean expanded) {
		this.expanded = expanded;
	}

	public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(x + SIZE / 2.0, y + SIZE / 2.0);
		GeneralPath path = new GeneralPath();
		if (expanded) {
			path.moveTo(-3.5, -1.75);
			path.lineTo(0, 1.75);
			path.lineTo(3.5, -1.75);
		} else {
			path.moveTo(-1.75, -3.5);
			path.lineTo(1.75, 0);
			path.lineTo(-1.75, 3.5);
		}
		g2.setColor(ModernLaf.TEXT_SECONDARY);
		g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(path);
		g2.dispose();
	}

	public int getIconWidth() {
		return SIZE;
	}

	public int getIconHeight() {
		return SIZE;
	}
}
