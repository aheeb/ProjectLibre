package com.projectlibre1.pm.graphic.spreadsheet.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.workflow.People;

/**
 * Editor of the "Assigned to" cell: suggests team members while typing the name after the last comma.
 * Up/Down pick a suggestion, Enter or Tab take it. Names that match nobody are added to the team when
 * the cell is committed (see SpreadSheetModel).
 */
public class AssigneeEditor extends SimpleEditor {
	private final DefaultListModel<String> suggestions = new DefaultListModel<String>();
	private final JList<String> list = new JList<String>(suggestions);
	private final JPopupMenu popup = new JPopupMenu();
	private boolean adjusting = false;
	private List<String> team;

	public AssigneeEditor() {
		super(String.class);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFocusable(false);
		list.setVisibleRowCount(6);
		list.setSelectionBackground(ModernLaf.SELECTION);
		list.setSelectionForeground(ModernLaf.TEXT_PRIMARY);
		list.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int i = list.locationToIndex(e.getPoint());
				if (i >= 0) accept(suggestions.get(i));
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setFocusable(false);
		popup.setFocusable(false);
		popup.add(scroll);

		component.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				changed();
			}
			public void removeUpdate(DocumentEvent e) {
				changed();
			}
			public void changedUpdate(DocumentEvent e) {
			}
		});
		component.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (!popup.isVisible()) return;
				int n = suggestions.size();
				switch (e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					list.setSelectedIndex(Math.min(n - 1, list.getSelectedIndex() + 1));
					break;
				case KeyEvent.VK_UP:
					list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 1));
					break;
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_TAB:
					if (list.getSelectedValue() == null) {
						popup.setVisible(false);
						return; // let the table commit
					}
					accept(list.getSelectedValue());
					break;
				case KeyEvent.VK_ESCAPE:
					popup.setVisible(false);
					break;
				default:
					return;
				}
				list.ensureIndexIsVisible(list.getSelectedIndex());
				e.consume();
			}
		});
		component.setToolTipText(Messages.getString("Assignee.tooltip"));
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		adjusting = true;
		Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		adjusting = false;
		DocumentFrame frame = GraphicManager.getInstance(table) == null ? null : GraphicManager.getInstance(table).getCurrentFrame();
		ResourcePool pool = (frame == null || frame.getProject() == null) ? null : frame.getProject().getResourcePool();
		team = People.names(pool);
		popup.setVisible(false);
		return c;
	}

	/** The part being typed: text after the last separator. */
	private String currentWord() {
		String text = component.getText();
		int i = Math.max(text.lastIndexOf(','), text.lastIndexOf(';'));
		return text.substring(i + 1).trim();
	}

	private void changed() {
		if (adjusting) return;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}

	private void refresh() {
		if (!component.isShowing() || team == null) {
			popup.setVisible(false);
			return;
		}
		String word = currentWord().toLowerCase();
		List<String> chosen = People.split(component.getText());
		suggestions.clear();
		if (word.length() > 0) {
			for (String name : team) { // names starting with the typed text first, then names containing it
				if (name.toLowerCase().startsWith(word) && !name.equalsIgnoreCase(word)) suggestions.addElement(name);
			}
			for (String name : team) {
				String lower = name.toLowerCase();
				if (!lower.startsWith(word) && lower.contains(word) && !suggestions.contains(name)) suggestions.addElement(name);
			}
		} else {
			for (String name : team) {
				boolean taken = false;
				for (String c : chosen) taken |= c.equalsIgnoreCase(name);
				if (!taken) suggestions.addElement(name);
			}
		}
		if (suggestions.isEmpty()) {
			popup.setVisible(false);
			return;
		}
		list.setSelectedIndex(word.length() > 0 ? 0 : -1);
		list.setVisibleRowCount(Math.min(6, suggestions.size()));
		int width = Math.max(component.getWidth(), 160);
		list.setFixedCellWidth(width - 4);
		popup.setPreferredSize(null);
		Dimension d = popup.getPreferredSize();
		popup.setPopupSize(width, d.height);
		if (!popup.isVisible()) popup.show(component, 0, component.getHeight());
		else popup.pack();
	}

	private void accept(String name) {
		String text = component.getText();
		int i = Math.max(text.lastIndexOf(','), text.lastIndexOf(';'));
		String head = i < 0 ? "" : text.substring(0, i + 1) + " ";
		adjusting = true;
		component.setText(head + name);
		adjusting = false;
		popup.setVisible(false);
	}

	public boolean stopCellEditing() {
		popup.setVisible(false);
		return super.stopCellEditing();
	}

	public void cancelCellEditing() {
		popup.setVisible(false);
		super.cancelCellEditing();
	}
}
