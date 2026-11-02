package com.projectlibre1.pm.graphic.frames;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.pushingpixels.flamingo.api.common.AbstractCommandButton;

/**
 * Clicking a ribbon, toolbar or view-switch button should not take keyboard focus away from the grid:
 * after clicking Undo or Indent the user expects to keep typing in the cell, as in native Mac and
 * Windows apps. Buttons stay reachable with the keyboard; only mouse presses no longer move focus.
 * Applies to buttons in the main window outside the project views; dialogs keep normal behaviour.
 */
public class ToolbarFocus implements AWTEventListener {
	public static void install() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new ToolbarFocus(), AWTEvent.MOUSE_EVENT_MASK);
	}

	public void eventDispatched(AWTEvent event) {
		// AWT event listeners run before the component's own mouse listeners, which request focus on press
		if (event.getID() != MouseEvent.MOUSE_PRESSED) return;
		Object source = event.getSource();
		if (!(source instanceof AbstractButton || source instanceof AbstractCommandButton)) return;
		if (source instanceof JMenuItem) return;
		JComponent button = (JComponent) source;
		if (!button.isRequestFocusEnabled()) return;
		if (SwingUtilities.getAncestorOfClass(DocumentFrame.class, button) != null) return;
		JRootPane root = SwingUtilities.getRootPane(button);
		Component window = root == null ? null : root.getParent();
		if (window != MainFrameFactory.getMainFrame()) return;
		button.setRequestFocusEnabled(false);
	}
}
