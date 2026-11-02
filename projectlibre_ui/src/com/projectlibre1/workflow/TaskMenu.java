package com.projectlibre1.workflow;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.projectlibre1.association.InvalidAssociationException;
import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldParseException;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.scheduling.ConstraintType;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.undo.UndoController;
import com.projectlibre1.util.Alert;

/**
 * The everyday task commands in one place: the right-click menu of task rows and Gantt bars, and
 * keyboard shortcuts for structuring the plan. Everything acts on the rows selected in the task grid.
 */
public class TaskMenu {
	private final SpreadSheet sheet;

	public TaskMenu(SpreadSheet sheet) {
		this.sheet = sheet;
	}

	/** True when {@code sheet} lists tasks (not resources, dependencies...). */
	public static boolean isTaskSheet(SpreadSheet sheet) {
		if (!(sheet.getModel() instanceof SpreadSheetModel)) return false;
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		return model.getCache() != null && model.getCache().getModel().getDataFactory() instanceof com.projectlibre1.pm.task.Project;
	}

	public static int menuMask() {
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	}

	/** Cmd/Ctrl+] and Cmd/Ctrl+[ (and Alt+Shift+Right/Left as in other planners) indent and outdent rows. */
	public static void installKeys(final SpreadSheet sheet) {
		Action indent = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (!sheet.isReadOnly()) sheet.executeAction(MenuActionConstants.ACTION_INDENT);
			}
		};
		Action outdent = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (!sheet.isReadOnly()) sheet.executeAction(MenuActionConstants.ACTION_OUTDENT);
			}
		};
		sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuMask()), "plIndent");
		sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuMask()), "plOutdent");
		sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "plIndent");
		sheet.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "plOutdent");
		sheet.getActionMap().put("plIndent", indent);
		sheet.getActionMap().put("plOutdent", outdent);
	}

	/** Selected tasks in row order (no blank rows, no assignments). */
	public List<Task> selectedTasks() {
		List<Task> tasks = new ArrayList<Task>();
		for (Object o : sheet.getSelectedNodesImpl())
			if (o instanceof Task) tasks.add((Task) o);
		return tasks;
	}

	public JPopupMenu build() {
		JPopupMenu menu = new JPopupMenu();
		List<Task> tasks = selectedTasks();
		boolean editable = !sheet.isReadOnly();
		boolean any = !tasks.isEmpty();
		boolean leafs = any;
		boolean milestones = any;
		for (Task t : tasks) {
			leafs &= !t.isWbsParent();
			milestones &= t.isMilestone();
		}

		add(menu, "TaskMenu.insert", editable, null, () -> sheet.executeAction(MenuActionConstants.ACTION_NEW));
		add(menu, "TaskMenu.delete", editable && any, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), () -> sheet.executeAction(MenuActionConstants.ACTION_DELETE));
		menu.addSeparator();
		add(menu, "TaskMenu.indent", editable && any, KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuMask()), () -> sheet.executeAction(MenuActionConstants.ACTION_INDENT));
		add(menu, "TaskMenu.outdent", editable && any, KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuMask()), () -> sheet.executeAction(MenuActionConstants.ACTION_OUTDENT));
		menu.addSeparator();
		add(menu, tasks.size() > 1 ? "TaskMenu.linkSequence" : "TaskMenu.linkPrevious", editable && any, null, this::linkToPrevious);
		add(menu, "TaskMenu.unlink", editable && any, null, () -> GraphicManager.getInstance(sheet).getCurrentFrame().doUnlinkTasks());
		menu.addSeparator();
		add(menu, milestones ? "TaskMenu.normalTask" : "TaskMenu.milestone", editable && leafs, null, () -> setMilestone(!milestonesSelected()));
		add(menu, "TaskMenu.assign", editable && leafs, null, () -> GraphicManager.getInstance(sheet).showAssignmentDialog(GraphicManager.getInstance(sheet).getCurrentFrame()));
		boolean pinned = false;
		for (Task t : tasks)
			pinned |= !t.isWbsParent() && t.getConstraintType() != ConstraintType.ASAP;
		if (pinned)
			add(menu, "TaskMenu.unpin", editable, null, () -> setField("Field.constraintType", Integer.valueOf(ConstraintType.ASAP)));
		menu.addSeparator();
		add(menu, "TaskMenu.notStarted", editable && leafs, null, () -> setPercentComplete(0));
		add(menu, "TaskMenu.halfDone", editable && leafs, null, () -> setPercentComplete(0.5));
		add(menu, "TaskMenu.done", editable && leafs, null, () -> setPercentComplete(1));
		menu.addSeparator();
		add(menu, "TaskMenu.details", any, null, () -> GraphicManager.getInstance(sheet).doInformationDialog(false));
		boolean separated = false;
		for (String id : new String[] {MenuActionConstants.ACTION_CUT, MenuActionConstants.ACTION_COPY, MenuActionConstants.ACTION_PASTE}) {
			if (!(sheet.getAction(id) instanceof Action)) continue;
			if (!separated) menu.addSeparator();
			separated = true;
			menu.add(new JMenuItem((Action) sheet.prepareAction(id)));
		}
		return menu;
	}

	private boolean milestonesSelected() {
		List<Task> tasks = selectedTasks();
		for (Task t : tasks)
			if (!t.isMilestone()) return false;
		return !tasks.isEmpty();
	}

	private static void add(JPopupMenu menu, String key, boolean enabled, KeyStroke accelerator, final Runnable run) {
		JMenuItem item = new JMenuItem(new AbstractAction(Messages.getString(key)) {
			public void actionPerformed(ActionEvent e) {
				run.run();
			}
		});
		item.setEnabled(enabled);
		if (accelerator != null) item.setAccelerator(accelerator); // shown as a hint; the keys are bound on the grid
		menu.add(item);
	}

	private List<GraphicNode> selectedGraphicNodes() {
		List<GraphicNode> nodes = new ArrayList<GraphicNode>();
		for (Object o : sheet.getSelectedGraphicNodes()) {
			GraphicNode n = (GraphicNode) o;
			if (n.getNode().getImpl() instanceof Task) nodes.add(n);
		}
		return nodes;
	}

	private void setField(String fieldId, Object value) {
		setField(fieldId, value, null);
	}

	/** Sets the field on the selected tasks (only those in {@code only}, if given) through the model, so it can be undone. */
	private void setField(String fieldId, Object value, List<Task> only) {
		sheet.finishCurrentOperations();
		Field field = Configuration.getFieldFromId(fieldId);
		NodeModel model = ((SpreadSheetModel) sheet.getModel()).getCache().getModel();
		try {
			for (GraphicNode n : selectedGraphicNodes()) {
				Node node = n.getNode();
				Task task = (Task) node.getImpl();
				if (task.isWbsParent() || (only != null && !only.contains(task))) continue;
				model.setFieldValue(field, node, this, value, null, NodeModel.NORMAL);
			}
		} catch (FieldParseException e) {
			Alert.error(e.getMessage(), sheet);
		}
	}

	public void setPercentComplete(double fraction) {
		setField("Field.percentComplete", Double.valueOf(fraction));
	}

	/** A milestone is a task with no duration; turning it back gives it the default length of a new task. */
	public void setMilestone(boolean milestone) {
		if (milestone) {
			setField("Field.duration", "0");
		} else {
			List<Task> marked = new ArrayList<Task>();
			List<Task> zero = new ArrayList<Task>();
			for (Task t : selectedTasks()) {
				if (t instanceof NormalTask && t.isMarkTaskAsMilestone()) marked.add(t);
				if (t.isZeroDuration()) zero.add(t); // only tasks without a length get the default one
			}
			UndoController undo = GraphicManager.getInstance(sheet).getUndoController();
			if (undo != null) undo.beginUpdate(); // one Undo step
			try {
				if (!marked.isEmpty()) setField("Field.markTaskAsMilestone", Boolean.FALSE, marked);
				if (!zero.isEmpty()) setField("Field.duration", "1", zero);
			} finally {
				if (undo != null) undo.endUpdate();
			}
		}
	}

	/**
	 * One task: it starts after the task in the row above (skipping the group it is in).
	 * Several tasks: each starts after the one above it.
	 */
	public void linkToPrevious() {
		sheet.finishCurrentOperations();
		List<Task> tasks = selectedTasks();
		if (tasks.isEmpty()) return;
		List<Task> chain = new ArrayList<Task>();
		if (tasks.size() == 1) {
			Task previous = previousTask(tasks.get(0));
			if (previous == null) {
				Toast.show(sheet, Messages.getString("TaskMenu.noPrevious"));
				return;
			}
			chain.addAll(Arrays.asList(previous, tasks.get(0)));
		} else {
			chain.addAll(tasks);
		}
		try {
			DependencyService.getInstance().connect(chain, this, null);
		} catch (InvalidAssociationException e) {
			Alert.error(e.getMessage(), sheet);
		}
	}

	/** The nearest task above {@code task} in the grid that is not one of its parents. */
	private Task previousTask(Task task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		int row = -1;
		for (int r = 0; r < sheet.getRowCount(); r++) {
			if (model.getNode(r).getNode().getImpl() == task) {
				row = r;
				break;
			}
		}
		for (int r = row - 1; r >= 0; r--) {
			Object impl = model.getNode(r).getNode().getImpl();
			if (!(impl instanceof Task)) continue;
			Task candidate = (Task) impl;
			if (isAncestor(candidate, task)) continue;
			return candidate;
		}
		return null;
	}

	private static boolean isAncestor(Task parent, Task task) {
		for (Task t = task.getWbsParentTask(); t != null; t = t.getWbsParentTask())
			if (t == parent) return true;
		return false;
	}
}
