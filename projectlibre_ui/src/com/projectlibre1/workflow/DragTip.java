package com.projectlibre1.workflow;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.projectlibre1.pm.graphic.laf.ModernLaf;

/**
 * A label that follows the pointer while dragging in a chart and tells what the drop will do,
 * e.g. the new dates of a bar being moved. Tooltips can't do this: they don't update during a drag.
 */
public class DragTip {
	private static JLabel label;

	public static void show(Component c, int x, int y, String html) {
		JRootPane root = SwingUtilities.getRootPane(c);
		if (root == null || html == null) {
			hide();
			return;
		}
		JLayeredPane layer = root.getLayeredPane();
		if (label == null) {
			label = new JLabel();
			label.setOpaque(true);
			label.setBackground(UIManager.getColor("ToolTip.background") == null ? java.awt.Color.WHITE : UIManager.getColor("ToolTip.background"));
			label.setForeground(ModernLaf.TEXT_PRIMARY);
			label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ModernLaf.HAIRLINE), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
		}
		if (label.getParent() != layer) {
			if (label.getParent() != null) label.getParent().remove(label);
			layer.add(label, JLayeredPane.DRAG_LAYER);
		}
		label.setText(html);
		Dimension d = label.getPreferredSize();
		Point p = SwingUtilities.convertPoint(c, x + 16, y + 18, layer);
		if (p.x + d.width > layer.getWidth() - 4) p.x = Math.max(4, layer.getWidth() - d.width - 4);
		if (p.y + d.height > layer.getHeight() - 4) p.y = p.y - d.height - 30;
		label.setBounds(p.x, p.y, d.width, d.height);
		label.repaint();
	}

	public static void hide() {
		if (label != null && label.getParent() != null) {
			java.awt.Container parent = label.getParent();
			java.awt.Rectangle r = label.getBounds();
			parent.remove(label);
			parent.repaint(r.x, r.y, r.width, r.height);
		}
	}
}
