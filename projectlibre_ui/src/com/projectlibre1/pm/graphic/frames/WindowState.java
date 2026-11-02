package com.projectlibre1.pm.graphic.frames;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * Sizes the main window to the screen on first run and remembers its bounds and maximized state.
 */
public class WindowState {
	private static final int MIN_WIDTH = 900, MIN_HEIGHT = 600;

	private static Preferences prefs() {
		return Preferences.userNodeForPackage(WindowState.class).node("mainWindow");
	}

	public static void restore(Frame frame) {
		Preferences p = prefs();
		Rectangle saved = new Rectangle(p.getInt("x", 0), p.getInt("y", 0), p.getInt("width", 0), p.getInt("height", 0));
		if (saved.width >= MIN_WIDTH && saved.height >= MIN_HEIGHT && isOnScreen(saved)) {
			frame.setBounds(saved);
		} else {
			Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			int w = Math.max(Math.min(MIN_WIDTH, usable.width), Math.min((int) (usable.width * 0.85), 1680));
			int h = Math.max(Math.min(MIN_HEIGHT, usable.height), Math.min((int) (usable.height * 0.88), 1080));
			frame.setBounds(usable.x + (usable.width - w) / 2, usable.y + (usable.height - h) / 2, w, h);
		}
		if (p.getBoolean("maximized", false)) frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
		track(frame);
	}

	private static void track(final Frame frame) {
		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				save(frame);
			}
			public void componentMoved(ComponentEvent e) {
				save(frame);
			}
		});
		frame.addWindowStateListener(new WindowAdapter() {
			public void windowStateChanged(WindowEvent e) {
				prefs().putBoolean("maximized", (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
			}
		});
	}

	private static void save(Frame frame) {
		if (!frame.isShowing() || frame.getExtendedState() != Frame.NORMAL) return; // keep the normal bounds while maximized
		Rectangle r = frame.getBounds();
		Preferences p = prefs();
		p.putInt("x", r.x);
		p.putInt("y", r.y);
		p.putInt("width", r.width);
		p.putInt("height", r.height);
	}

	/** True if a reasonable part of the window (its title bar area) is visible on some screen. */
	private static boolean isOnScreen(Rectangle r) {
		Rectangle titleBar = new Rectangle(r.x, r.y, r.width, 40);
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle i = device.getDefaultConfiguration().getBounds().intersection(titleBar);
			if (!i.isEmpty() && i.width >= 200) return true;
		}
		return false;
	}
}
