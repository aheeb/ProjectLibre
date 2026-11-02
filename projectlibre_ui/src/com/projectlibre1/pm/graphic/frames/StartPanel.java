package com.projectlibre1.pm.graphic.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.Environment;

/**
 * Start screen shown in the main window whenever no project is open:
 * new project, open, recent projects and sample projects.
 */
public class StartPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String ICONS = "com/projectlibre1/pm/graphic/frames/start/";
	private static final int CONTENT_WIDTH = 820;
	private static final int MAX_RECENT = 6;

	private final GraphicManager graphicManager;
	private final JPanel content = new JPanel() {
		public Dimension getPreferredSize() {
			return new Dimension(CONTENT_WIDTH, super.getPreferredSize().height);
		}
	};
	private JButton newProjectButton;

	public StartPanel(GraphicManager graphicManager) {
		super(new BorderLayout());
		this.graphicManager = graphicManager;
		setBackground(ModernLaf.CHROME);
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JPanel centered = new JPanel(new GridBagLayout());
		centered.setBackground(ModernLaf.CHROME);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(48, 32, 48, 32);
		centered.add(content, c);

		JScrollPane scroll = new JScrollPane(centered);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);
	}

	/** Rebuilt each time the panel is shown, so the recent list is current. */
	public void addNotify() {
		super.addNotify();
		rebuild();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (newProjectButton != null && isShowing()) newProjectButton.requestFocusInWindow();
			}
		});
	}

	private void rebuild() {
		content.removeAll();

		JLabel title = new JLabel(Messages.getString("StartPanel.title"));
		title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
		append(title, 0);
		JLabel intro = new JLabel("<html><div style='width:" + (CONTENT_WIDTH * 3 / 4 - 20) + "px'>" + Messages.getString("StartPanel.intro") + "</div></html>");
		intro.putClientProperty(FlatClientProperties.STYLE_CLASS, "large");
		intro.setForeground(ModernLaf.TEXT_SECONDARY);
		append(intro, 8);

		JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
		cards.setOpaque(false);
		newProjectButton = card("new", Messages.getString("StartPanel.newProject"), Messages.getString("StartPanel.newProjectHint"), true, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicManager.doNewProjectDialog();
			}
		});
		cards.add(newProjectButton);
		cards.add(card("open", Messages.getString("StartPanel.open"), Messages.getString("StartPanel.openHint"), false, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphicManager.new OpenProjectAction().actionPerformed(e);
			}
		}));
		append(cards, 28);

		JPanel lists = new JPanel(new GridLayout(1, 2, 24, 0));
		lists.setOpaque(false);
		lists.add(recentSection());
		lists.add(sampleSection());
		append(lists, 36);

		content.revalidate();
		content.repaint();
	}

	private void append(JComponent component, int gapAbove) {
		if (gapAbove > 0) content.add(Box.createVerticalStrut(gapAbove));
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(component);
	}

	private JComponent recentSection() {
		JPanel list = listPanel();
		List<String> files = RecentFiles.get();
		if (files.size() > MAX_RECENT) files = files.subList(0, MAX_RECENT);
		for (final String path : files) {
			File file = new File(path);
			String name = file.getName();
			if (name.toLowerCase().endsWith(".pod")) name = name.substring(0, name.length() - 4); // other formats keep their extension //$NON-NLS-1$
			String folder = file.getParent() == null ? "" : abbreviatePath(file.getParent());
			JButton row = row(icon("file", 20), name, folder, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					openRecent(path);
				}
			});
			row.setToolTipText(path);
			addRemovePopup(row, path);
			list.add(row);
		}
		if (files.isEmpty()) {
			JLabel empty = new JLabel("<html>" + Messages.getString("StartPanel.noRecent") + "</html>");
			empty.setForeground(ModernLaf.TEXT_SECONDARY);
			empty.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
			list.add(empty);
		}
		return section(Messages.getString("StartPanel.recent"), null, list);
	}

	private JComponent sampleSection() {
		JPanel list = listPanel();
		for (final String name : SampleProjects.NAMES) {
			if (!SampleProjects.isAvailable(name)) continue;
			list.add(row(icon("sample", 20), name, null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					openSample(name);
				}
			}));
		}
		if (list.getComponentCount() == 0) return new JPanel(); // not bundled in this build
		return section(Messages.getString("StartPanel.samples"), Messages.getString("StartPanel.samplesHint"), list);
	}

	private JComponent section(String heading, String hint, JComponent list) {
		JPanel section = new JPanel();
		section.setOpaque(false);
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		JLabel label = new JLabel(heading);
		label.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		section.add(label);
		section.add(Box.createVerticalStrut(4));
		JLabel hintLabel = new JLabel("<html>" + (hint == null ? "&nbsp;" : hint) + "</html>");
		hintLabel.setForeground(ModernLaf.TEXT_SECONDARY);
		hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		section.add(hintLabel);
		section.add(Box.createVerticalStrut(10));
		list.setAlignmentX(Component.LEFT_ALIGNMENT);
		section.add(list);
		section.add(Box.createVerticalGlue());
		return section;
	}

	private JPanel listPanel() {
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(Color.WHITE);
		list.putClientProperty(FlatClientProperties.STYLE, "arc:12");
		list.setBorder(new FlatLineBorder(new Insets(4, 4, 4, 4), ModernLaf.HAIRLINE, 1, 12));
		return list;
	}

	/** Large action button: icon, title and one line of explanation. */
	private JButton card(String iconName, String title, String hint, boolean primary, ActionListener action) {
		JButton button = new JButton();
		button.setLayout(new BorderLayout(14, 0));
		button.setMargin(new Insets(16, 18, 16, 18));
		Icon icon = icon(iconName, 32);
		if (primary) ((FlatSVGIcon) icon).setColorFilter(new FlatSVGIcon.ColorFilter().add(ModernLaf.ACCENT, Color.WHITE));
		JLabel iconLabel = new JLabel(icon);
		button.add(iconLabel, BorderLayout.WEST);
		JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
		text.setOpaque(false);
		JLabel titleLabel = new JLabel(title);
		titleLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
		JLabel hintLabel = new JLabel(hint);
		text.add(titleLabel);
		text.add(hintLabel);
		button.add(text, BorderLayout.CENTER);
		if (primary) {
			button.putClientProperty(FlatClientProperties.STYLE, "arc:14; borderWidth:0; focusWidth:0; innerFocusWidth:2;"
					+ "background:#2F6FEB; hoverBackground:#2A63D4; pressedBackground:#2558BD; focusedBackground:#2F6FEB;"
					+ "focusedBorderColor:#9DBDF7; borderColor:#2F6FEB");
			titleLabel.setForeground(Color.WHITE);
			hintLabel.setForeground(new Color(0xDCE7FC));
		} else {
			button.putClientProperty(FlatClientProperties.STYLE, "arc:14; focusWidth:0; innerFocusWidth:1;"
					+ "background:#FFFFFF; hoverBackground:#F2F6FE; pressedBackground:#E5EDFD; focusedBackground:#FFFFFF;"
					+ "borderColor:#E3E5E8; hoverBorderColor:#B9CDF6; focusedBorderColor:#2F6FEB");
			hintLabel.setForeground(ModernLaf.TEXT_SECONDARY);
		}
		Dimension d = button.getPreferredSize();
		button.setPreferredSize(new Dimension(d.width, Math.max(d.height, 84)));
		setup(button, title, action);
		return button;
	}

	/** One entry of a list: icon, name and an optional second line. */
	private JButton row(Icon icon, String name, String detail, ActionListener action) {
		JButton button = new JButton();
		button.setLayout(new BorderLayout(10, 0));
		button.setMargin(new Insets(7, 10, 7, 10));
		button.setHorizontalAlignment(JButton.LEFT);
		button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
		button.putClientProperty(FlatClientProperties.STYLE, "arc:8; hoverBackground:#F2F6FE; pressedBackground:#E5EDFD");
		button.add(new JLabel(icon), BorderLayout.WEST);
		JPanel text = new JPanel(new GridLayout(detail == null ? 1 : 2, 1, 0, 1));
		text.setOpaque(false);
		JLabel nameLabel = new JLabel(name);
		nameLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold");
		text.add(nameLabel);
		if (detail != null) {
			JLabel detailLabel = new JLabel(detail);
			detailLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
			detailLabel.setForeground(ModernLaf.TEXT_SECONDARY);
			text.add(detailLabel);
		}
		button.add(text, BorderLayout.CENTER);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		setup(button, name, action);
		return button;
	}

	/** Enter activates the button; up and down arrows move between buttons. */
	private void setup(final JButton button, String accessibleName, ActionListener action) {
		button.getAccessibleContext().setAccessibleName(accessibleName);
		button.addActionListener(action);
		InputMap keys = button.getInputMap(JComponent.WHEN_FOCUSED);
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pressed");
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "released");
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focusNext");
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "focusNext");
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "focusPrevious");
		keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "focusPrevious");
		button.getActionMap().put("focusNext", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(button);
			}
		});
		button.getActionMap().put("focusPrevious", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(button);
			}
		});
	}

	private void addRemovePopup(JButton row, final String path) {
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem remove = new JMenuItem(Messages.getString("StartPanel.removeRecent"));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RecentFiles.remove(path);
				rebuild();
			}
		});
		popup.add(remove);
		row.setComponentPopupMenu(popup);
	}

	private void openRecent(String path) {
		if (!new File(path).exists()) {
			RecentFiles.remove(path);
			rebuild();
			Alert.warn(Messages.getStringWithParam("StartPanel.missingFile", path));
			return;
		}
		graphicManager.loadLocalDocument(path, !Environment.getStandAlone());
	}

	private void openSample(String name) {
		try {
			String path = SampleProjects.extract(name);
			if (path != null) graphicManager.loadLocalDocument(path, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Icon icon(String name, int size) {
		return new FlatSVGIcon(ICONS + name + ".svg", size, size);
	}

	/** Replaces the home directory with ~ and elides the middle of long paths, keeping the last folders. */
	private static String abbreviatePath(String path) {
		String home = System.getProperty("user.home");
		if (home != null && path.startsWith(home)) path = "~" + path.substring(home.length());
		if (path.length() <= 48) return path;
		String sep = File.separator;
		String[] parts = path.split(java.util.regex.Pattern.quote(sep));
		if (parts.length <= 3) return path;
		String tail = parts[parts.length - 2] + sep + parts[parts.length - 1];
		return parts[0] + sep + "\u2026" + sep + tail; //$NON-NLS-1$
	}
}
