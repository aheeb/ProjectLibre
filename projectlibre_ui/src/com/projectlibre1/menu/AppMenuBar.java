package com.projectlibre1.menu;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import com.projectlibre1.util.Environment;

/**
 * Menu bar built from the ribbon commands. On macOS it becomes the screen menu bar (File, Edit, Task, Project,
 * View, Window, Help) with the standard shortcuts; on other platforms the ribbon is the menu and only the
 * keyboard shortcuts are installed.
 */
public class AppMenuBar implements MenuActionConstants {
	private final MenuManager menuManager;
	private final Map<String, JRadioButtonMenuItem> viewItems = new HashMap<String, JRadioButtonMenuItem>();

	private AppMenuBar(MenuManager menuManager) {
		this.menuManager = menuManager;
	}

	/**
	 * Installs the shortcuts of {@link MenuShortcuts} on the frame and, on the Mac, the screen menu bar.
	 * Call after the default key bindings of the main window are registered.
	 * @param settingsAction opened by the "Settings…" item of the application menu (Cmd+,)
	 */
	public static void install(JFrame frame, MenuManager menuManager, Action settingsAction) {
		AppMenuBar bar = new AppMenuBar(menuManager);
		if (Environment.isMac()) {
			frame.setJMenuBar(bar.createMacMenuBar(frame));
			bar.removeRootBindingsOwnedByMenu(frame.getRootPane());
			if (settingsAction != null && Desktop.isDesktopSupported()
					&& Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
				Desktop.getDesktop().setPreferencesHandler(e -> settingsAction.actionPerformed(
						new ActionEvent(frame, ActionEvent.ACTION_PERFORMED, "Settings")));
			}
		} else {
			bar.addMissingRootBindings(frame.getRootPane());
		}
	}

	private JMenuBar createMacMenuBar(final JFrame frame) {
		JMenuBar bar = new JMenuBar();

		JMenu file = menu("MacMenu.File");
		item(file, ACTION_NEW_PROJECT, "RibbonNewProject", false);
		item(file, ACTION_OPEN_PROJECT, "RibbonOpenProject", true);
		file.addSeparator();
		item(file, ACTION_CLOSE_PROJECT, "RibbonCloseProject", false);
		item(file, ACTION_SAVE_PROJECT, "RibbonSaveProject", false);
		item(file, ACTION_SAVE_PROJECT_AS, "RibbonSaveProjectAs", true);
		file.addSeparator();
		item(file, ACTION_PDF, "RibbonPDF", true);
		item(file, ACTION_PRINT_PREVIEW, "RibbonPrintPreview", false);
		item(file, ACTION_PRINT, "RibbonPrint", true);
		bar.add(file);

		JMenu edit = menu("MacMenu.Edit");
		edit.add(editItem("RibbonTopBarUndo", ACTION_UNDO, null));
		edit.add(editItem("RibbonTopBarRedo", ACTION_REDO, null));
		edit.addSeparator();
		edit.add(editItem("RibbonCut", ACTION_CUT, "cut-to-clipboard"));
		edit.add(editItem("RibbonCopy", ACTION_COPY, "copy-to-clipboard"));
		edit.add(editItem("RibbonPaste", ACTION_PASTE, "paste-from-clipboard"));
		item(edit, ACTION_DELETE, "RibbonDelete", false);
		edit.add(selectAllItem());
		edit.addSeparator();
		item(edit, ACTION_FIND, "RibbonFind", true);
		bar.add(edit);

		JMenu task = menu("MacMenu.Task");
		item(task, "NewTask", "RibbonInsert", false);
		item(task, "NewResource", "RibbonNewResource", false);
		task.addSeparator();
		item(task, ACTION_INDENT, "RibbonIndent", false);
		item(task, ACTION_OUTDENT, "RibbonOutdent", false);
		task.addSeparator();
		item(task, ACTION_LINK, "RibbonLink", false);
		item(task, ACTION_UNLINK, "RibbonUnlink", false);
		task.addSeparator();
		item(task, ACTION_INFORMATION, "RibbonTaskInformation", true);
		item(task, ACTION_ASSIGN_RESOURCES, "RibbonAssignResources", true);
		item(task, ACTION_NOTES, "RibbonNotes", true);
		item(task, ACTION_UPDATE_TASKS, "RibbonUpdateTasks", true);
		bar.add(task);

		JMenu project = menu("MacMenu.Project");
		item(project, ACTION_PROJECT_INFORMATION, "RibbonProjectInformation", true);
		item(project, ACTION_CHANGE_WORKING_TIME, "RibbonChangeWorkingTime", true);
		item(project, ACTION_PROJECTS_DIALOG, "RibbonProjectsDialog", true);
		project.addSeparator();
		item(project, ACTION_SAVE_BASELINE, "RibbonSaveBaseline", true);
		item(project, ACTION_CLEAR_BASELINE, "RibbonClearBaseline", true);
		item(project, ACTION_UPDATE_PROJECT, "RibbonUpdateProject", true);
		bar.add(project);

		JMenu view = menu("MacMenu.View");
		ButtonGroup topViews = new ButtonGroup();
		viewItem(view, topViews, ACTION_GANTT);
		viewItem(view, topViews, ACTION_TRACKING_GANTT);
		viewItem(view, topViews, ACTION_NETWORK);
		viewItem(view, topViews, ACTION_WBS);
		viewItem(view, topViews, ACTION_TASK_USAGE_DETAIL);
		view.addSeparator();
		viewItem(view, topViews, ACTION_RESOURCES);
		viewItem(view, topViews, ACTION_RBS);
		viewItem(view, topViews, ACTION_RESOURCE_USAGE_DETAIL);
		view.addSeparator();
		viewItem(view, topViews, ACTION_PROJECTS);
		viewItem(view, topViews, ACTION_REPORT);
		view.addSeparator();
		JMenu bottom = menu("MacMenu.BottomPane");
		ButtonGroup bottomViews = new ButtonGroup();
		viewItem(bottom, bottomViews, ACTION_HISTOGRAM);
		viewItem(bottom, bottomViews, ACTION_CHARTS);
		viewItem(bottom, bottomViews, ACTION_TASK_USAGE);
		viewItem(bottom, bottomViews, ACTION_RESOURCE_USAGE);
		viewItem(bottom, bottomViews, ACTION_NO_SUB_WINDOW);
		view.add(bottom);
		view.addSeparator();
		item(view, ACTION_ZOOM_IN, "RibbonZoomIn", false);
		item(view, ACTION_ZOOM_OUT, "RibbonZoomOut", false);
		item(view, ACTION_SCROLL_TO_TASK, "RibbonScrollToTask", false);
		view.addSeparator();
		item(view, ACTION_LOCALE, "RibbonLocale", true);
		bar.add(view);
		menuManager.addViewSelectionListener(new MenuManager.ViewSelectionListener() {
			public void setActivatedView(String id, boolean selected) {
				JRadioButtonMenuItem item = viewItems.get(id);
				if (item != null && selected)
					item.setSelected(true);
			}
		});

		JMenu window = menu("MacMenu.Window");
		JMenuItem minimize = new JMenuItem(text("MacMenu.Minimize"));
		minimize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask()));
		minimize.addActionListener(e -> frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED));
		window.add(minimize);
		JMenuItem zoom = new JMenuItem(text("MacMenu.Zoom"));
		zoom.addActionListener(e -> frame.setExtendedState(
				(frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH ? Frame.NORMAL : Frame.MAXIMIZED_BOTH));
		window.add(zoom);
		bar.add(window);

		JMenu help = menu("MacMenu.Help");
		JMenuItem guide = actionItem(text("MacMenu.Help.Guide"), ACTION_PROJECTLIBRE_DOCUMENTATION);
		if (guide != null)
			help.add(guide);
		JMenuItem site = actionItem(text("MacMenu.Help.Website"), ACTION_PROJECTLIBRE);
		if (site != null)
			help.add(site);
		bar.add(help);
		return bar;
	}

	private static int menuMask() {
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	}

	private JMenu menu(String key) {
		return new JMenu(text(key));
	}

	private String text(String key) {
		String s = MenuManager.getMenuStringOrNull(key);
		return s == null ? key : s;
	}

	/** Menu label from the ribbon label: title case in English, with an ellipsis when it opens a dialog. */
	private String label(String ribbonKey, boolean opensDialog) {
		String s = MenuManager.getMenuStringOrNull(ribbonKey + ".text");
		if (s == null)
			s = ribbonKey;
		if ("en".equals(Locale.getDefault().getLanguage()))
			s = titleCase(s);
		return opensDialog ? s + "…" : s;
	}

	private static String titleCase(String s) {
		StringBuilder b = new StringBuilder();
		String[] words = s.split(" ");
		for (int i = 0; i < words.length; i++) {
			String w = words[i];
			boolean small = i > 0 && (w.equals("to") || w.equals("of") || w.equals("and"));
			if (!small && w.length() > 0)
				w = Character.toUpperCase(w.charAt(0)) + w.substring(1);
			if (i > 0)
				b.append(' ');
			b.append(w);
		}
		return b.toString();
	}

	private Action action(String id) {
		try {
			return menuManager.getActionFromId(id);
		} catch (Exception e) {
			return null;
		}
	}

	private JMenuItem item(JMenu menu, String id, String ribbonKey, boolean opensDialog) {
		JMenuItem item = actionItem(label(ribbonKey, opensDialog), id);
		if (item != null)
			menu.add(item);
		return item;
	}

	private JMenuItem actionItem(String text, String id) {
		final Action action = action(id);
		if (action == null)
			return null;
		JMenuItem item = new JMenuItem(text);
		bind(item, action, id);
		item.addActionListener(action);
		return item;
	}

	private void bind(final JMenuItem item, final Action action, String id) {
		KeyStroke ks = MenuShortcuts.getKeyStroke(menuManager.getActionStringFromId(id));
		if (ks != null)
			item.setAccelerator(ks);
		item.setEnabled(action.isEnabled());
		action.addPropertyChangeListener(evt -> {
			if ("enabled".equals(evt.getPropertyName()))
				item.setEnabled(action.isEnabled());
		});
	}

	private void viewItem(JMenu menu, ButtonGroup group, final String id) {
		final Action action = action(id);
		if (action == null)
			return;
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(label("Ribbon" + id, false));
		bind(item, action, id);
		item.addActionListener(action);
		group.add(item);
		viewItems.put(id, item);
		menu.add(item);
	}

	/**
	 * Edit commands act on the text field being edited if there is one (e.g. a cell editor), and on the
	 * selected rows otherwise.
	 */
	private JMenuItem editItem(String ribbonKey, String id, final String textAction) {
		final Action appAction = action(id);
		JMenuItem item = new JMenuItem(label(ribbonKey, false));
		if (appAction == null)
			return item;
		KeyStroke ks = MenuShortcuts.getKeyStroke(menuManager.getActionStringFromId(id));
		if (ks != null)
			item.setAccelerator(ks);
		item.addActionListener(e -> {
			Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (textAction != null && focus instanceof JTextComponent) {
				Action a = ((JTextComponent) focus).getActionMap().get(textAction);
				if (a != null) {
					a.actionPerformed(new ActionEvent(focus, ActionEvent.ACTION_PERFORMED, textAction));
					return;
				}
			}
			if (appAction.isEnabled())
				appAction.actionPerformed(e);
		});
		return item;
	}

	private JMenuItem selectAllItem() {
		JMenuItem item = new JMenuItem(text("MacMenu.SelectAll"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask()));
		item.addActionListener(e -> {
			Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (focus instanceof JComponent) {
				ActionMap map = ((JComponent) focus).getActionMap();
				Action a = map.get(focus instanceof JTextComponent ? "select-all" : "selectAll");
				if (a != null)
					a.actionPerformed(new ActionEvent(focus, ActionEvent.ACTION_PERFORMED, "selectAll"));
			}
		});
		return item;
	}

	/**
	 * The menu bar handles its shortcuts natively; window-level bindings for the same keys would run the
	 * command twice. Cmd+, (Settings) and Cmd+. (Cancel) are reserved on the Mac.
	 */
	private void removeRootBindingsOwnedByMenu(JRootPane root) {
		InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for (KeyStroke ks : MenuShortcuts.getAll().values())
			inputMap.remove(ks);
		int mask = menuMask();
		inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask)); // redo is Shift+Cmd+Z here
		inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, mask));
		inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, mask));
	}

	/**
	 * Without a menu bar, put every shortcut of the table on the window. The table is the reference, so it
	 * replaces older bindings of the same keys (they point to the same commands, except Ctrl+I which now adds
	 * a task even without a selection).
	 */
	private void addMissingRootBindings(JRootPane root) {
		InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = root.getActionMap();
		for (Map.Entry<String, KeyStroke> e : MenuShortcuts.getAll().entrySet()) {
			final String actionName = e.getKey();
			inputMap.put(e.getValue(), actionName);
			actionMap.put(actionName, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent ev) {
					Action a;
					try {
						a = menuManager.rootActionMap.getAction(actionName);
					} catch (Exception ex) {
						return;
					}
					if (a != null && a.isEnabled())
						a.actionPerformed(ev);
				}
			});
		}
		// common alternative for redo
		KeyStroke shiftZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask() | InputEvent.SHIFT_DOWN_MASK);
		if (inputMap.get(shiftZ) == null)
			inputMap.put(shiftZ, inputMap.get(MenuShortcuts.getKeyStroke("RedoAction")));
	}
}
