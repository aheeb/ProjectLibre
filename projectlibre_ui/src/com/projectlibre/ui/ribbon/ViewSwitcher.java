package com.projectlibre.ui.ribbon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;

/**
 * Segmented control at the top right of the ribbon that switches the main view (Gantt, Network, Resources, ...).
 * The views offered are listed in menuInternal.properties under RibbonViewSwitcher; labels, tooltips and icons
 * come from the matching Ribbon* entries.
 */
public class ViewSwitcher extends JPanel implements MenuManager.ViewSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final int HEIGHT = 30;
	private static final int ARC = 8;

	private final Map<String, Segment> segments = new LinkedHashMap<String, Segment>();
	private final ButtonGroup group = new ButtonGroup();
	private boolean compact;

	public ViewSwitcher(MenuManager menuManager) {
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		String ids = menuManager.getStringOrNull("RibbonViewSwitcher");
		if (ids != null) {
			for (String id : ids.trim().split("\\s+")) {
				Action action = menuManager.getActionFromId(id);
				if (action == null)
					continue;
				String key = "Ribbon" + id;
				String text = menuManager.getStringOrNull(key + ".text");
				String iconKey = menuManager.getStringOrNull(key + ".icon");
				Segment s = new Segment(id, action, text == null ? id : text.trim(),
						iconKey == null ? null : IconManager.getSvgIcon(iconKey.trim(), 16));
				String tip = menuManager.getStringOrNull(key + ".tooltip");
				if (tip != null)
					s.setToolTipText(tip.trim());
				segments.put(id, s);
				group.add(s);
				add(s);
			}
		}
		menuManager.addViewSelectionListener(this);
	}

	/** Icons only, for narrow windows. Labels stay available as tooltips. */
	public void setCompact(boolean compact) {
		if (this.compact == compact)
			return;
		this.compact = compact;
		for (Segment s : segments.values())
			s.setText(compact ? null : s.label);
		revalidate();
	}

	public boolean isCompact() {
		return compact;
	}

	/** Preferred width with labels, whatever the current mode. */
	public int getFullWidth() {
		boolean was = compact;
		setCompact(false);
		int w = getPreferredSize().width;
		setCompact(was);
		return w;
	}

	@Override
	public void setActivatedView(String id, boolean selected) {
		Segment s = segments.get(id);
		if (s != null) {
			if (selected)
				s.setSelected(true);
			else if (s.isSelected())
				group.clearSelection();
		}
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		return new Dimension(d.width, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(new Color(0, 0, 0, 15));
		g2d.fillRoundRect(0, 0, getWidth(), getHeight(), ARC + 2, ARC + 2);
		g2d.dispose();
	}

	private class Segment extends JToggleButton {
		private static final long serialVersionUID = 1L;
		final String id;
		final Action action;
		final String label;

		Segment(String id, Action action, String label, Icon icon) {
			super(label, icon);
			this.id = id;
			this.action = action;
			this.label = label;
			setFocusable(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setRolloverEnabled(true);
			setIconTextGap(6);
			setMargin(new Insets(0, 10, 0, 12));
			setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 12));
			setEnabled(action.isEnabled());
			action.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if ("enabled".equals(evt.getPropertyName()))
						setEnabled(Segment.this.action.isEnabled());
				}
			});
			getModel().addChangeListener(e -> setForeground(isSelected() ? ModernLaf.TEXT_PRIMARY : ModernLaf.TEXT_SECONDARY));
			setForeground(ModernLaf.TEXT_SECONDARY);
			addActionListener(e -> {
				Segment.this.action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Segment.this.id));
			});
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth(), h = getHeight();
			if (isSelected()) {
				g2d.setColor(new Color(0, 0, 0, 18));
				g2d.fillRoundRect(0, 1, w, h - 1, ARC, ARC);
				g2d.setColor(Color.WHITE);
				g2d.fillRoundRect(0, 0, w, h - 1, ARC, ARC);
			} else if (isEnabled() && getModel().isRollover()) {
				g2d.setColor(ModernLaf.HOVER);
				g2d.fillRoundRect(0, 0, w, h, ARC, ARC);
			}
			g2d.dispose();
			super.paintComponent(g);
		}
	}
}
