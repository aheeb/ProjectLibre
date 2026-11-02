package com.projectlibre1.menu;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.KeyStroke;

import com.projectlibre1.util.Environment;

/**
 * Keyboard shortcuts of the main commands, keyed by the action name used in menuInternal.properties
 * (the value of a ".action" entry, e.g. "SaveProjectAction"). Used by the macOS menu bar, the window
 * key bindings and the tooltips, so a shortcut is defined once and shown everywhere.
 */
public class MenuShortcuts {
	private static Map<String, KeyStroke> shortcuts;

	private static synchronized Map<String, KeyStroke> get() {
		if (shortcuts != null)
			return shortcuts;
		shortcuts = new HashMap<String, KeyStroke>();
		boolean mac = Environment.isMac();
		put("NewProjectAction", KeyEvent.VK_N, 0);
		put("OpenProjectAction", KeyEvent.VK_O, 0);
		put("SaveProjectAction", KeyEvent.VK_S, 0);
		put("SaveProjectAsAction", KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK);
		put("CloseProjectAction", KeyEvent.VK_W, 0);
		put("PrintAction", KeyEvent.VK_P, 0);
		put("UndoAction", KeyEvent.VK_Z, 0);
		if (mac)
			put("RedoAction", KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK);
		else
			put("RedoAction", KeyEvent.VK_Y, 0);
		put("CutAction", KeyEvent.VK_X, 0);
		put("CopyAction", KeyEvent.VK_C, 0);
		put("PasteAction", KeyEvent.VK_V, 0);
		put("FindAction", KeyEvent.VK_F, 0);
		put("NewTaskAction", KeyEvent.VK_I, 0);
		if (mac) {
			// Cmd+, is Settings and Cmd+. is Cancel on the Mac, so use the outliner convention
			put("IndentAction", KeyEvent.VK_CLOSE_BRACKET, 0);
			put("OutdentAction", KeyEvent.VK_OPEN_BRACKET, 0);
		} else {
			put("IndentAction", KeyEvent.VK_PERIOD, 0);
			put("OutdentAction", KeyEvent.VK_COMMA, 0);
		}
		return shortcuts;
	}

	private static void put(String action, int keyCode, int extraModifiers) {
		int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		shortcuts.put(action, KeyStroke.getKeyStroke(keyCode, menuMask | extraModifiers));
	}

	/** Shortcut for an action name such as "SaveProjectAction", or null. */
	public static KeyStroke getKeyStroke(String actionName) {
		return actionName == null ? null : get().get(actionName);
	}

	public static Map<String, KeyStroke> getAll() {
		return get();
	}

	/** Human readable shortcut: "⇧⌘S" on the Mac, "Ctrl+Shift+S" elsewhere. */
	public static String toDisplayString(KeyStroke ks) {
		if (ks == null)
			return null;
		int m = ks.getModifiers();
		String key;
		switch (ks.getKeyCode()) {
		case KeyEvent.VK_OPEN_BRACKET: key = "["; break;
		case KeyEvent.VK_CLOSE_BRACKET: key = "]"; break;
		case KeyEvent.VK_PERIOD: key = "."; break;
		case KeyEvent.VK_COMMA: key = ","; break;
		default: key = KeyEvent.getKeyText(ks.getKeyCode());
		}
		StringBuilder b = new StringBuilder();
		if (Environment.isMac()) {
			if ((m & InputEvent.CTRL_DOWN_MASK) != 0) b.append('⌃');
			if ((m & InputEvent.ALT_DOWN_MASK) != 0) b.append('⌥');
			if ((m & InputEvent.SHIFT_DOWN_MASK) != 0) b.append('⇧');
			if ((m & InputEvent.META_DOWN_MASK) != 0) b.append('⌘');
			return b.append(key).toString();
		}
		if ((m & InputEvent.CTRL_DOWN_MASK) != 0) b.append("Ctrl+");
		if ((m & InputEvent.ALT_DOWN_MASK) != 0) b.append("Alt+");
		if ((m & InputEvent.SHIFT_DOWN_MASK) != 0) b.append("Shift+");
		return b.append(key).toString();
	}

	public static String getDisplayString(String actionName) {
		return toDisplayString(getKeyStroke(actionName));
	}
}
