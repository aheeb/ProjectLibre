package com.projectlibre1.menu;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;

import com.projectlibre.ui.toolbar.MainToolBar;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.toolbar.FilterToolBarManager;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.Environment;

/**
 * The menu bar of the main window: File, Edit, Task, Resource, Project, View, Window, Help. It holds every
 * command; the toolbar only shows the most common ones. On macOS it is the screen menu bar; on Windows and
 * Linux it is in the window, with the ProjectLibre logo at its left.
 */
public class AppMenuBar implements MenuActionConstants {
	private final MenuManager menuManager;
	private final Map<String, JRadioButtonMenuItem> viewItems = new HashMap<String, JRadioButtonMenuItem>();
	private final boolean mac = Environment.isMac();
	private JMenu windowMenu;

	private AppMenuBar(MenuManager menuManager) {
		this.menuManager = menuManager;
	}

	/**
	 * Installs the menu bar and its shortcuts (see {@link MenuShortcuts}) on the frame.
	 * Call after the default key bindings of the main window are registered.
	 * @param settingsAction opened by Settings… (Cmd+, in the application menu on the Mac)
	 * @param filters filter, sort and group choices offered in the View menu, or null
	 * @return the Window menu, to which the open projects are added
	 */
	public static JMenu install(JFrame frame, MenuManager menuManager, Action settingsAction, FilterToolBarManager filters) {
		AppMenuBar bar = new AppMenuBar(menuManager);
		frame.setJMenuBar(bar.createMenuBar(frame, settingsAction, filters));
		bar.removeRootBindingsOwnedByMenu(frame.getRootPane());
		if (bar.mac && settingsAction != null && Desktop.isDesktopSupported()
				&& Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(e -> settingsAction.actionPerformed(
					new ActionEvent(frame, ActionEvent.ACTION_PERFORMED, "Settings")));
		}
		return bar.windowMenu;
	}

	private JMenuBar createMenuBar(final JFrame frame, Action settingsAction, FilterToolBarManager filters) {
		JMenuBar bar = new JMenuBar();
		if (!mac) {
			// one band with the toolbar below (and with the title bar, which FlatLaf merges on Windows)
			bar.setBackground(ModernLaf.CHROME);
			bar.setBorder(BorderFactory.createEmptyBorder());
			bar.add(createLogo());
		}

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
		file.addSeparator();
		if (!mac && settingsAction != null) {
			JMenuItem settings = new JMenuItem(text("AppMenu.Settings"));
			bind(settings, settingsAction, ACTION_CALENDAR_OPTIONS);
			settings.addActionListener(settingsAction);
			file.add(settings);
		}
		item(file, ACTION_LOCALE, "RibbonLocale", true);
		if (!mac) {
			file.addSeparator();
			JMenuItem exit = actionItem(text("AppMenu.Exit"), ACTION_EXIT);
			if (exit != null)
				file.add(exit);
		}
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
		edit.add(findItem(frame));
		bar.add(edit);

		JMenu task = menu("MacMenu.Task");
		item(task, "NewTask", "RibbonInsert", false);
		// HOOK: more ways to add tasks (e.g. a milestone) go here, see Toolbar.TaskCommands in menuInternal.properties
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

		JMenu resource = menu("AppMenu.Resource");
		item(resource, "NewResource", "RibbonNewResource", false);
		resource.addSeparator();
		item(resource, ACTION_INFORMATION, "RibbonResourceInformation", true);
		item(resource, ACTION_NOTES, "RibbonNotes", true);
		item(resource, ACTION_CHANGE_WORKING_TIME, "RibbonChangeWorkingTime", true);
		bar.add(resource);

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
		if (filters != null) {
			view.addSeparator();
			view.add(transformMenu("AppMenu.Filter", filters.getFiltersComboBox()));
			view.add(transformMenu("AppMenu.Sort", filters.getSortersComboBox()));
			view.add(transformMenu("AppMenu.Group", filters.getGroupersComboBox()));
		}
		view.addSeparator();
		item(view, ACTION_ZOOM_IN, "RibbonZoomIn", false);
		item(view, ACTION_ZOOM_OUT, "RibbonZoomOut", false);
		item(view, "Today", "RibbonToday", false);
		item(view, ACTION_SCROLL_TO_TASK, "RibbonScrollToTask", false);
		bar.add(view);
		menuManager.addViewSelectionListener(new MenuManager.ViewSelectionListener() {
			public void setActivatedView(String id, boolean selected) {
				JRadioButtonMenuItem item = viewItems.get(id);
				if (item != null && selected)
					item.setSelected(true);
			}
		});

		windowMenu = menu("MacMenu.Window");
		if (mac) {
			JMenuItem minimize = new JMenuItem(text("MacMenu.Minimize"));
			minimize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask()));
			minimize.addActionListener(e -> frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED));
			windowMenu.add(minimize);
			JMenuItem zoom = new JMenuItem(text("MacMenu.Zoom"));
			zoom.addActionListener(e -> frame.setExtendedState(
					(frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH ? Frame.NORMAL : Frame.MAXIMIZED_BOTH));
			windowMenu.add(zoom);
			windowMenu.addSeparator();
		}
		// the open projects are added below by GraphicManager
		bar.add(windowMenu);

		JMenu help = menu("MacMenu.Help");
		JMenuItem guide = actionItem(text("MacMenu.Help.Guide"), ACTION_PROJECTLIBRE_DOCUMENTATION);
		if (guide != null)
			help.add(guide);
		JMenuItem site = actionItem(text("MacMenu.Help.Website"), ACTION_PROJECTLIBRE);
		if (site != null)
			help.add(site);
		if (!mac) {
			help.addSeparator();
			JMenuItem about = actionItem(text("AppMenu.About"), ACTION_ABOUT_PROJECTLIBRE);
			if (about != null)
				help.add(about);
		}
		bar.add(help);
		return bar;
	}

	/** The ProjectLibre logo at the left of the in-window menu bar, linking to projectlibre.com. */
	private JComponent createLogo() {
		JLabel logo = new JLabel(new ImageIcon(IconManager.getImage("logo.ProjectLibre")));
		logo.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 12));
		logo.setToolTipText(text("RibbonLogo.tooltip"));
		logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		logo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				BrowserControl.displayURL("http://www.projectlibre.com");
			}
		});
		return logo;
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

	/**
	 * Menu label from the ribbon label (or AppMenu.<ribbon key>.text when the menu needs a longer one): title
	 * case in English, with an ellipsis when it opens a dialog.
	 */
	private String label(String ribbonKey, boolean opensDialog) {
		String s = MenuManager.getMenuStringOrNull("AppMenu." + ribbonKey + ".text");
		if (s == null)
			s = MenuManager.getMenuStringOrNull(ribbonKey + ".text");
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

	/** Filter, sort or group submenu listing the choices of the (hidden) combo box, filled when opened. */
	private JMenu transformMenu(String key, final JComboBox combo) {
		final JMenu menu = menu(key);
		menu.setEnabled(combo.isEnabled());
		combo.addPropertyChangeListener("enabled", evt -> menu.setEnabled(combo.isEnabled()));
		menu.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				menu.removeAll();
				ComboBoxModel model = combo.getModel();
				ButtonGroup group = new ButtonGroup();
				for (int i = 0; i < model.getSize(); i++) {
					Object choice = model.getElementAt(i);
					final int index = i;
					JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(choice));
					item.setSelected(combo.getSelectedIndex() == i);
					item.addActionListener(ev -> combo.setSelectedIndex(index));
					group.add(item);
					menu.add(item);
				}
			}
			public void menuDeselected(MenuEvent e) {
			}
			public void menuCanceled(MenuEvent e) {
			}
		});
		// a screen menu bar needs an item to show the submenu at all
		menu.add(new JMenuItem(""));
		return menu;
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

	/** Find goes to the search field of the toolbar, or opens the Find dialog in views without one. */
	private JMenuItem findItem(final JFrame frame) {
		final Action find = action(ACTION_FIND);
		JMenuItem item = new JMenuItem(label("RibbonFind", true));
		item.setAccelerator(MenuShortcuts.getKeyStroke("FindAction"));
		item.addActionListener(e -> {
			if (MainToolBar.focusSearch(frame.getRootPane()))
				return;
			if (find != null && find.isEnabled())
				find.actionPerformed(e);
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
	 * The menu bar handles its shortcuts; window-level bindings for the same keys would run the command twice.
	 * On the Mac, Cmd+, (Settings) and Cmd+. (Cancel) are reserved and redo is Shift+Cmd+Z.
	 */
	private void removeRootBindingsOwnedByMenu(JRootPane root) {
		InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for (KeyStroke ks : MenuShortcuts.getAll().values())
			inputMap.remove(ks);
		int mask = menuMask();
		if (mac) {
			inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask));
			inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, mask));
			inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, mask));
		} else {
			// common alternative for redo
			KeyStroke shiftZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | InputEvent.SHIFT_DOWN_MASK);
			Action redo = action(ACTION_REDO);
			if (redo != null && inputMap.get(shiftZ) == null) {
				inputMap.put(shiftZ, "RedoAlternative");
				root.getActionMap().put("RedoAlternative", redo);
			}
		}
	}
}
