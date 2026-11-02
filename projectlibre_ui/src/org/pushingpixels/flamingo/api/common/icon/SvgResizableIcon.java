package org.pushingpixels.flamingo.api.common.icon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Resizable icon backed by an SVG file, rendered crisply at any size and screen scale.
 */
public class SvgResizableIcon implements ResizableIcon {
	private final FlatSVGIcon base;
	private FlatSVGIcon current;

	public SvgResizableIcon(URL url, int width, int height) {
		this(new FlatSVGIcon(url), width, height);
	}

	private SvgResizableIcon(FlatSVGIcon base, int width, int height) {
		this.base = base;
		this.current = base.derive(width, height);
	}

	/** Greyed-out variant used for disabled buttons. */
	public SvgResizableIcon getDisabledIcon() {
		return new SvgResizableIcon((FlatSVGIcon) base.getDisabledIcon(), getIconWidth(), getIconHeight());
	}

	@Override
	public void setDimension(Dimension newDimension) {
		if (newDimension.width != current.getIconWidth() || newDimension.height != current.getIconHeight())
			current = base.derive(newDimension.width, newDimension.height);
	}

	@Override
	public int getIconWidth() {
		return current.getIconWidth();
	}

	@Override
	public int getIconHeight() {
		return current.getIconHeight();
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		current.paintIcon(c, g, x, y);
	}
}
