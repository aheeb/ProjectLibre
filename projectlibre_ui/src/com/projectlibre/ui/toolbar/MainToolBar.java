package com.projectlibre.ui.toolbar;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatClientProperties;
import com.projectlibre.ui.ribbon.ViewSwitcher;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.menu.MenuShortcuts;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.DocumentSelectedEvent;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.projectlibre1.pm.graphic.views.SearchContext;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.Environment;

/**
 * The single row at the top of the main window: ProjectLibre logo, project title, view switcher, a few commands
 * that depend on the active view, and zoom / search / share / help at the right. Everything else is in the
 * menu bar.
 *
 * The commands are listed in menuInternal.properties (Toolbar.TaskCommands, Toolbar.ResourceCommands,
 * Toolbar.NoProjectCommands) as Ribbon* entries, which give the action, icon, label and tooltip. To add a
 * command (e.g. a milestone), define its Ribbon* entry and add it to the list; a "Toolbar.<entry>.text" in
 * menu.properties overrides the label for the toolbar, and an empty one makes the button icon-only.
 *
 * When the window is too narrow, the toolbar drops the project title, then the command labels, then the view
 * labels, and finally moves the right-hand buttons into a "more" menu.
 */
public class MainToolBar extends JPanel implements MenuManager.ViewSelectionListener, DocumentSelectedEvent.Listener {
	private static final long serialVersionUID = 1L;
	/** Client property of the root pane holding the search field, used by Find (Cmd/Ctrl+F). */
	public static final String SEARCH_FIELD_PROPERTY = "ProjectLibre.searchField";
	public static final int ICON_SIZE = 20;
	private static final int HEIGHT = 44;
	private static final int GROUP_GAP = 14;
	private static final int SEARCH_WIDTH = 170;
	private static final int TITLE_WIDTH = 200;
	private static final int SEARCH_NARROW_WIDTH = 130;

	private static final Set<String> TASK_VIEWS = new HashSet<String>(Arrays.asList(MenuActionConstants.ACTION_GANTT,
			MenuActionConstants.ACTION_TRACKING_GANTT, MenuActionConstants.ACTION_NETWORK, MenuActionConstants.ACTION_WBS,
			MenuActionConstants.ACTION_TASK_USAGE_DETAIL));
	private static final Set<String> RESOURCE_VIEWS = new HashSet<String>(Arrays.asList(MenuActionConstants.ACTION_RESOURCES,
			MenuActionConstants.ACTION_RBS, MenuActionConstants.ACTION_RESOURCE_USAGE_DETAIL));
	/** Views that can zoom (time scale or chart). */
	private static final Set<String> ZOOM_VIEWS = new HashSet<String>(Arrays.asList(MenuActionConstants.ACTION_GANTT,
			MenuActionConstants.ACTION_TRACKING_GANTT, MenuActionConstants.ACTION_NETWORK, MenuActionConstants.ACTION_WBS,
			MenuActionConstants.ACTION_RBS, MenuActionConstants.ACTION_TASK_USAGE_DETAIL,
			MenuActionConstants.ACTION_RESOURCE_USAGE_DETAIL));
	/** Views with a Gantt chart, where "Today" can scroll the timeline. */
	private static final Set<String> GANTT_VIEWS = new HashSet<String>(Arrays.asList(MenuActionConstants.ACTION_GANTT,
			MenuActionConstants.ACTION_TRACKING_GANTT));
	/** Views with a sheet the search field can search. */
	private static final Set<String> SEARCH_VIEWS = new HashSet<String>(Arrays.asList(MenuActionConstants.ACTION_GANTT,
			MenuActionConstants.ACTION_TRACKING_GANTT, MenuActionConstants.ACTION_TASK_USAGE_DETAIL,
			MenuActionConstants.ACTION_RESOURCES, MenuActionConstants.ACTION_RESOURCE_USAGE_DETAIL,
			MenuActionConstants.ACTION_PROJECTS));

	private final GraphicManager graphicManager;
	private final MenuManager menuManager;
	private final JFrame frame;

	private final JPanel left = row();
	private final JPanel right = row();
	/** Project name, only when the toolbar replaces the title bar. Long names are cut with an ellipsis. */
	private final JLabel title = new JLabel() {
		private static final long serialVersionUID = 1L;
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			return new Dimension(Math.min(d.width, TITLE_WIDTH), d.height);
		}
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}
	};
	private final boolean showTitle;
	private final Component titleGap = Box.createHorizontalStrut(GROUP_GAP);
	private final ViewSwitcher views;
	private final Component viewsGap = Box.createHorizontalStrut(GROUP_GAP);
	private final JPanel taskCommands;
	private final JPanel resourceCommands;
	private final JPanel noProjectCommands;
	private final List<ToolButton> commandButtons = new ArrayList<ToolButton>();

	private final ToolButton zoomOut;
	private final ToolButton zoomIn;
	private final ToolButton today;
	private final JTextField search = new JTextField();
	private final ToolButton share;
	private final ToolButton help;
	private final ToolButton more;
	private final Component zoomGap = Box.createHorizontalStrut(GROUP_GAP - 6);
	private final Component searchGap = Box.createHorizontalStrut(GROUP_GAP - 6);

	/** Right-hand items that collapse into the "more" menu, in the order they collapse. */
	private final List<Collapsible> collapsibles = new ArrayList<Collapsible>();

	private String viewId = MenuActionConstants.ACTION_GANTT;
	private boolean documentOpen;
	private int level = -1;
	private boolean laying;

	private SearchContext searchContext;
	private CommonSpreadSheet searchSheet;

	/**
	 * @param withLogo false when the logo is shown elsewhere (in the menu bar on Windows and Linux)
	 * @param leadingSpace placeholder for the macOS window buttons when the toolbar is in the title bar, or null
	 */
	public MainToolBar(GraphicManager graphicManager, JFrame frame, boolean withLogo, JComponent leadingSpace) {
		this.graphicManager = graphicManager;
		this.menuManager = graphicManager.getMenuManager();
		this.frame = frame;
		setLayout(null);
		setOpaque(true);
		setBackground(ModernLaf.CHROME);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernLaf.HAIRLINE));

		if (leadingSpace != null) {
			left.add(leadingSpace);
			left.add(Box.createHorizontalStrut(4));
		} else
			left.add(Box.createHorizontalStrut(12));
		if (withLogo) {
			left.add(createLogo());
			left.add(Box.createHorizontalStrut(GROUP_GAP));
		}
		showTitle = leadingSpace != null;
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		title.setForeground(ModernLaf.TEXT_PRIMARY);
		left.add(title);
		left.add(titleGap);
		views = new ViewSwitcher(menuManager);
		left.add(views);
		left.add(viewsGap);
		taskCommands = commands("Toolbar.TaskCommands");
		resourceCommands = commands("Toolbar.ResourceCommands");
		noProjectCommands = commands("Toolbar.NoProjectCommands");
		left.add(taskCommands);
		left.add(resourceCommands);
		left.add(noProjectCommands);

		zoomOut = command("RibbonZoomOut", "");
		zoomIn = command("RibbonZoomIn", "");
		today = command("RibbonToday", null);
		initSearch();
		share = new ToolButton(null, IconManager.getSvgIcon("toolbar.share", ICON_SIZE));
		share.setToolTipText(text("Toolbar.Share.tooltip"));
		share.addActionListener(e -> showShareMenu());
		help = new ToolButton(null, IconManager.getSvgIcon("ribbon.help", ICON_SIZE));
		help.setToolTipText(text("RibbonHelp.tooltip"));
		help.addActionListener(e -> graphicManager.showHelpDialog());
		more = new ToolButton(null, IconManager.getSvgIcon("toolbar.more", ICON_SIZE));
		more.setToolTipText(text("Toolbar.More.tooltip"));
		more.addActionListener(e -> showMoreMenu());

		right.add(zoomOut);
		right.add(zoomIn);
		right.add(today);
		right.add(zoomGap);
		right.add(search);
		right.add(searchGap);
		right.add(share);
		right.add(help);
		right.add(more);
		right.add(Box.createHorizontalStrut(10));
		left.add(Box.createHorizontalGlue());
		add(left);
		add(right);

		collapsibles.add(new Collapsible(today));
		collapsibles.add(new Collapsible(zoomIn));
		collapsibles.add(new Collapsible(zoomOut));
		collapsibles.add(new Collapsible(share));
		collapsibles.add(new Collapsible(help));

		frame.getRootPane().putClientProperty(SEARCH_FIELD_PROPERTY, search);
		frame.getRootPane().addPropertyChangeListener(FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_BOUNDS, evt -> revalidate());
		frame.addPropertyChangeListener("title", evt -> updateTitle());
		menuManager.addViewSelectionListener(this);
		DocumentSelectedEvent.addListener(this);
		// the close command is enabled exactly when a project is open
		Action close = menuManager.getActionFromId(MenuActionConstants.ACTION_CLOSE_PROJECT);
		if (close != null) {
			close.addPropertyChangeListener(evt -> {
				if ("enabled".equals(evt.getPropertyName()))
					updateContext();
			});
		}
		updateContext();
	}

	/**
	 * Toolbar for the main window. On the Mac it carries the logo (and sits in the title bar when the frame
	 * uses a unified title bar); elsewhere the logo is in the menu bar.
	 */
	public static MainToolBar create(GraphicManager graphicManager, JFrame frame) {
		JComponent leading = null;
		if (Boolean.TRUE.equals(frame.getRootPane().getClientProperty("apple.awt.fullWindowContent"))) {
			leading = new JPanel();
			leading.setOpaque(false);
			leading.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_PLACEHOLDER, "mac zeroInFullScreen");
		}
		return new MainToolBar(graphicManager, frame, Environment.isMac(), leading);
	}

	private static JPanel row() {
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		return p;
	}

	private JComponent createLogo() {
		JLabel logo = new JLabel(new ImageIcon(IconManager.getImage("logo.ProjectLibre")));
		logo.setToolTipText(text("RibbonLogo.tooltip"));
		logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		logo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				BrowserControl.displayURL("http://www.projectlibre.com");
			}
		});
		return logo;
	}

	private String text(String key) {
		String s = MenuManager.getMenuStringOrNull(key);
		return s == null ? key : s;
	}

	/** Group of command buttons from a list of Ribbon* entries; "|" adds a gap. */
	private JPanel commands(String listKey) {
		JPanel p = row();
		String list = menuManager.getStringOrNull(listKey);
		if (list == null)
			return p;
		boolean first = true;
		for (String key : list.trim().split("\\s+")) {
			if ("|".equals(key)) {
				p.add(Box.createHorizontalStrut(GROUP_GAP - 4));
				continue;
			}
			ToolButton b = command(key, MenuManager.getMenuStringOrNull("Toolbar." + key + ".text"));
			if (b == null)
				continue;
			if (!first)
				p.add(Box.createHorizontalStrut(2));
			b.primary = first || "Toolbar.NoProjectCommands".equals(listKey);
			first = false;
			p.add(b);
			commandButtons.add(b);
		}
		return p;
	}

	/**
	 * Button for a Ribbon* entry, following the enabled state of its action.
	 * @param label null for the ribbon label, "" for an icon-only button
	 */
	private ToolButton command(String key, String label) {
		String actionName = menuManager.getStringOrNull(key + ".action");
		final Action action = actionName == null ? null : menuManager.getActionFromName(actionName.trim());
		if (action == null)
			return null;
		if (label == null)
			label = MenuManager.getMenuStringOrNull(key + ".text");
		String iconKey = menuManager.getStringOrNull(key + ".icon");
		final ToolButton b = new ToolButton(label == null || label.isEmpty() ? null : label,
				iconKey == null ? null : IconManager.getSvgIcon(iconKey.trim(), ICON_SIZE));
		String tip = MenuManager.getMenuStringOrNull(key + ".tooltip");
		String name = MenuManager.getMenuStringOrNull(key + ".text");
		String shortcut = MenuShortcuts.getDisplayString(actionName.trim());
		StringBuilder t = new StringBuilder("<html>");
		if (name != null)
			t.append("<b>").append(name).append("</b>");
		if (shortcut != null)
			t.append("&nbsp;&nbsp;<font color='#6B7280'>").append(shortcut).append("</font>");
		if (tip != null)
			t.append("<br>").append(tip);
		b.setToolTipText(t.toString());
		b.action = action;
		b.setEnabled(action.isEnabled());
		action.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if ("enabled".equals(evt.getPropertyName()))
					b.setEnabled(action.isEnabled());
			}
		});
		b.addActionListener(e -> {
			if (action.isEnabled())
				action.actionPerformed(new ActionEvent(b, ActionEvent.ACTION_PERFORMED, key));
		});
		return b;
	}

	// Context **************************************************************

	@Override
	public void setActivatedView(String id, boolean selected) {
		if (selected && id != null && !id.equals(viewId)) {
			viewId = id;
			updateContext();
		}
	}

	@Override
	public void documentSelected(DocumentSelectedEvent evt) {
		SwingUtilities.invokeLater(this::updateContext);
	}

	private void updateContext() {
		DocumentFrame df = graphicManager.getCurrentFrame();
		Action close = menuManager.getActionFromId(MenuActionConstants.ACTION_CLOSE_PROJECT);
		documentOpen = df != null && df.getProject() != null && (close == null || close.isEnabled());
		if (documentOpen && graphicManager.getTopViewId() != null)
			viewId = graphicManager.getTopViewId();
		views.setVisible(documentOpen);
		viewsGap.setVisible(documentOpen);
		taskCommands.setVisible(documentOpen && TASK_VIEWS.contains(viewId));
		resourceCommands.setVisible(documentOpen && RESOURCE_VIEWS.contains(viewId));
		noProjectCommands.setVisible(!documentOpen);
		boolean zoom = documentOpen && ZOOM_VIEWS.contains(viewId);
		setWanted(zoomOut, zoom);
		setWanted(zoomIn, zoom);
		setWanted(today, documentOpen && GANTT_VIEWS.contains(viewId));
		zoomGap.setVisible(zoom);
		boolean searchable = documentOpen && SEARCH_VIEWS.contains(viewId);
		search.setVisible(searchable);
		searchGap.setVisible(searchable);
		search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
				text(RESOURCE_VIEWS.contains(viewId) ? "Toolbar.SearchResources.placeholder" : "Toolbar.Search.placeholder"));
		setWanted(share, documentOpen);
		updateTitle();
		level = -1;
		revalidate();
		repaint();
	}

	private void updateTitle() {
		DocumentFrame df = graphicManager.getCurrentFrame();
		Project project = documentOpen && df != null ? df.getProject() : null;
		String name = project == null ? null : project.getName();
		boolean modified = project != null && frame.getTitle() != null && frame.getTitle().endsWith("*");
		title.setText(name == null || name.trim().isEmpty() ? null : name.trim());
		title.setToolTipText(project == null ? null : project.getFileName());
		// unsaved changes show as a dot in the close button
		frame.getRootPane().putClientProperty("Window.documentModified", modified);
		level = -1;
		revalidate();
	}

	// Menus ****************************************************************

	private void showShareMenu() {
		JPopupMenu menu = new JPopupMenu();
		addMenuItem(menu, "RibbonPDF");
		addMenuItem(menu, "RibbonPrint");
		addMenuItem(menu, "RibbonPrintPreview");
		menu.addSeparator();
		addMenuItem(menu, "RibbonSaveProjectAs");
		menu.show(share, 0, share.getHeight() + 2);
	}

	private void showMoreMenu() {
		JPopupMenu menu = new JPopupMenu();
		if (isCollapsed(zoomOut) || isCollapsed(zoomIn)) {
			addMenuItem(menu, "RibbonZoomOut");
			addMenuItem(menu, "RibbonZoomIn");
		}
		if (isCollapsed(today))
			addMenuItem(menu, "RibbonToday");
		if (isCollapsed(share)) {
			if (menu.getComponentCount() > 0)
				menu.addSeparator();
			addMenuItem(menu, "RibbonPDF");
			addMenuItem(menu, "RibbonPrint");
			addMenuItem(menu, "RibbonSaveProjectAs");
		}
		if (isCollapsed(help)) {
			if (menu.getComponentCount() > 0)
				menu.addSeparator();
			JMenuItem item = new JMenuItem(text("Toolbar.Help.text"), IconManager.getSvgIcon("ribbon.help", 16));
			item.addActionListener(e -> graphicManager.showHelpDialog());
			menu.add(item);
		}
		menu.show(more, more.getWidth() - menu.getPreferredSize().width, more.getHeight() + 2);
	}

	private void addMenuItem(JPopupMenu menu, String key) {
		String actionName = menuManager.getStringOrNull(key + ".action");
		Action action = actionName == null ? null : menuManager.getActionFromName(actionName.trim());
		if (action == null)
			return;
		String label = MenuManager.getMenuStringOrNull(key + ".text");
		String iconKey = menuManager.getStringOrNull(key + ".icon");
		JMenuItem item = new JMenuItem(label, iconKey == null ? null : IconManager.getSvgIcon(iconKey.trim(), 16));
		KeyStroke ks = MenuShortcuts.getKeyStroke(actionName.trim());
		if (ks != null)
			item.setAccelerator(ks);
		item.setEnabled(action.isEnabled());
		item.addActionListener(e -> action.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, key)));
		menu.add(item);
	}

	// Search ***************************************************************

	private void initSearch() {
		search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, IconManager.getSvgIcon("ribbon.find", 16));
		search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
		search.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 2,6,2,6; background: #FFFFFF");
		String tip = MenuManager.getMenuStringOrNull("Toolbar.Search.tooltip");
		String shortcut = MenuShortcuts.getDisplayString("FindAction");
		search.setToolTipText(shortcut == null ? tip : tip + " (" + shortcut + ")");
		setSearchWidth(SEARCH_WIDTH);
		// only focusable on demand (click, Cmd/Ctrl+F), so it never takes the initial focus from the sheet
		search.setFocusable(false);
		search.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (!search.isFocusable()) {
					search.setFocusable(true);
					search.requestFocusInWindow();
				}
			}
		});
		search.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent e) {
				if (!e.isTemporary())
					search.setFocusable(false);
			}
		});
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				find(false, true);
			}
			public void removeUpdate(DocumentEvent e) {
				find(false, true);
			}
			public void changedUpdate(DocumentEvent e) {
			}
		});
		search.addActionListener(e -> find(true, true));
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "findPrevious");
		search.getActionMap().put("findPrevious", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				find(true, false);
			}
		});
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "endSearch");
		search.getActionMap().put("endSearch", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				search.setText("");
				DocumentFrame df = graphicManager.getCurrentFrame();
				CommonSpreadSheet sheet = df == null ? null : df.getTopSpreadSheet();
				if (sheet != null)
					sheet.requestFocusInWindow();
			}
		});
	}

	private void setSearchWidth(int width) {
		Dimension d = new Dimension(width, 28);
		search.setPreferredSize(d);
		search.setMaximumSize(d);
		search.setMinimumSize(d);
	}

	/**
	 * Selects the next row whose name contains the search text. While typing the current row is checked first,
	 * so the selection stays put as long as it matches; Enter moves on, Shift+Enter goes back, and the search
	 * wraps around at the end.
	 */
	private void find(boolean next, boolean forward) {
		String value = search.getText().trim();
		DocumentFrame df = graphicManager.getCurrentFrame();
		CommonSpreadSheet sheet = df == null ? null : df.getTopSpreadSheet();
		if (sheet == null || value.isEmpty()) {
			searchContext = null;
			search.putClientProperty(FlatClientProperties.OUTLINE, null);
			return;
		}
		if (!next || searchContext == null || searchSheet != sheet) {
			searchContext = sheet.createSearchContext();
			searchSheet = sheet;
		}
		searchContext.setSearchValue(value);
		searchContext.setForward(forward);
		boolean found = sheet.findNext(searchContext);
		if (!found && sheet.getRowCount() > 0) {
			int previous = sheet.getCurrentRow();
			int column = Math.max(sheet.getSelectedColumn(), 0);
			sheet.changeSelection(forward ? 0 : sheet.getRowCount() - 1, column, false, false);
			searchContext = sheet.createSearchContext();
			searchContext.setSearchValue(value);
			searchContext.setForward(forward);
			found = sheet.findNext(searchContext);
			if (!found && previous >= 0)
				sheet.changeSelection(previous, column, false, false);
		}
		search.putClientProperty(FlatClientProperties.OUTLINE, found ? null : FlatClientProperties.OUTLINE_ERROR);
	}

	/** Moves the keyboard focus to the search field. Returns false if the active view has no search field. */
	public static boolean focusSearch(JComponent rootPane) {
		Object o = rootPane.getClientProperty(SEARCH_FIELD_PROPERTY);
		if (!(o instanceof JTextField) || !((JTextField) o).isShowing())
			return false;
		JTextField field = (JTextField) o;
		field.setFocusable(true);
		field.requestFocusInWindow();
		field.selectAll();
		return true;
	}

	// Layout ***************************************************************

	private void setWanted(Component c, boolean wanted) {
		for (Collapsible k : collapsibles)
			if (k.component == c)
				k.wanted = wanted;
		c.setVisible(wanted && !isCollapsed(c));
	}

	private boolean isCollapsed(Component c) {
		for (Collapsible k : collapsibles)
			if (k.component == c)
				return k.wanted && k.collapsed;
		return false;
	}

	/**
	 * Levels of compaction: 0 everything, 1 icon-only secondary commands, 2 no project title, 3 icon-only first
	 * command ("Add task"), 4 icon-only views, 5 narrower search, then each further level moves one more
	 * right-hand button into the "more" menu.
	 */
	private void applyLevel(int level) {
		title.setVisible(showTitle && level < 2 && title.getText() != null);
		titleGap.setVisible(title.isVisible());
		for (ToolButton b : commandButtons)
			b.setLabelShown(level < (b.primary ? 3 : 1));
		views.setCompact(level >= 4);
		setSearchWidth(level >= 5 ? SEARCH_NARROW_WIDTH : SEARCH_WIDTH);
		int collapsed = Math.max(0, level - 5);
		boolean anyCollapsed = false;
		for (int i = 0; i < collapsibles.size(); i++) {
			Collapsible k = collapsibles.get(i);
			k.collapsed = i < collapsed;
			k.component.setVisible(k.wanted && !k.collapsed);
			anyCollapsed |= k.wanted && k.collapsed;
		}
		more.setVisible(anyCollapsed);
	}

	private int maxLevel() {
		return 5 + collapsibles.size();
	}

	@Override
	public void doLayout() {
		if (laying)
			return;
		laying = true;
		try {
			Insets in = getInsets();
			int width = getWidth() - in.left - in.right;
			int newLevel = 0;
			for (; newLevel < maxLevel(); newLevel++) {
				applyLevel(newLevel);
				// a child's revalidate() does not reach parents that are already invalid, so clear their caches
				invalidateTree(left);
				invalidateTree(right);
				if (left.getPreferredSize().width + right.getPreferredSize().width + GROUP_GAP <= width)
					break;
			}
			if (newLevel == maxLevel())
				applyLevel(newLevel);
			level = newLevel;
			int h = getHeight() - in.top - in.bottom;
			int rw = Math.min(right.getPreferredSize().width, width);
			right.setBounds(in.left + width - rw, in.top, rw, h);
			left.setBounds(in.left, in.top, Math.max(0, width - rw), h);
			left.doLayout();
			right.doLayout();
		} finally {
			laying = false;
		}
	}

	private static void invalidateTree(Component c) {
		c.invalidate();
		if (c instanceof JPanel)
			for (Component child : ((JPanel) c).getComponents())
				invalidateTree(child);
	}

	/** As high as the title bar when the toolbar is in it, so the window buttons are centered on the toolbar. */
	@Override
	public Dimension getPreferredSize() {
		int h = HEIGHT;
		Object buttons = frame.getRootPane().getClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_BOUNDS);
		if (buttons instanceof java.awt.Rectangle && ((java.awt.Rectangle) buttons).height > 0)
			h = Math.max(((java.awt.Rectangle) buttons).height, 40);
		return new Dimension(left.getPreferredSize().width + right.getPreferredSize().width, h);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0, getPreferredSize().height);
	}

	private static class Collapsible {
		final Component component;
		boolean wanted = true;
		boolean collapsed;

		Collapsible(Component component) {
			this.component = component;
		}
	}

	/** Flat toolbar button: icon with an optional label, filled on hover. */
	static class ToolButton extends JButton {
		private static final long serialVersionUID = 1L;
		private final String label;
		Action action;
		/** Keeps its label longer when space is short. */
		boolean primary;

		ToolButton(String label, Icon icon) {
			super(label, icon);
			this.label = label;
			putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
			setFocusable(false);
			setIconTextGap(6);
			setHorizontalTextPosition(SwingConstants.TRAILING);
			setForeground(ModernLaf.TEXT_PRIMARY);
			updateMargin();
		}

		void setLabelShown(boolean shown) {
			String t = shown ? label : null;
			if (t == null ? getText() == null || getText().isEmpty() : t.equals(getText()))
				return;
			setText(t);
			updateMargin();
		}

		private void updateMargin() {
			boolean text = getText() != null && !getText().isEmpty();
			setMargin(text ? new Insets(4, 7, 4, 9) : new Insets(4, 5, 4, 5));
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}
	}

}
