package com.projectlibre.ui.ribbon;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.Action;

import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuActionsMap;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.projectlibre1.pm.graphic.views.GanttView;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.util.DateTime;

/**
 * Commands that only exist in the ribbon.
 */
public class RibbonActions {
	public static final String ACTION_NEW_TASK = "NewTask";
	public static final String ACTION_NEW_RESOURCE = "NewResource";
	public static final String ACTION_TODAY = "Today";

	public static void register(final GraphicManager graphicManager, MenuActionsMap actionsMap) {
		actionsMap.addHandler(ACTION_NEW_RESOURCE, new NewResourceAction(graphicManager));
		actionsMap.addHandler(ACTION_NEW_TASK, new NewTaskAction(graphicManager));
		actionsMap.addHandler(ACTION_TODAY, new TodayAction(graphicManager));
	}

	/**
	 * Inserting needs a selected row. Without one, pick the first empty row so the new item is added at the end
	 * instead of nothing happening.
	 */
	static void selectFirstEmptyRowIfNone(GraphicManager graphicManager) {
		DocumentFrame frame = graphicManager.getCurrentFrame();
		CommonSpreadSheet sheet = frame == null ? null : frame.getTopSpreadSheet();
		if (sheet == null || sheet.getSelectedRow() != -1 || sheet.getRowCount() == 0)
			return;
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		int row = 0;
		while (row < sheet.getRowCount() - 1) {
			GraphicNode node = model.getNode(row);
			if (node == null || node.getNode() == null || node.getNode().isVoid())
				break;
			row++;
		}
		sheet.changeSelection(row, 1, false, false);
	}

	/**
	 * Inserts a blank row above the selected row (unless the selected row is blank already, like the rows after
	 * the last task) and starts editing its name, so the new task or resource can be typed right away.
	 * The menu's InsertTask action does nothing by itself; the sheet's own insert action does the work.
	 */
	static void insertAndEditName(GraphicManager graphicManager) {
		DocumentFrame frame = graphicManager.getCurrentFrame();
		CommonSpreadSheet sheet = frame == null ? null : frame.getTopSpreadSheet();
		if (sheet == null)
			return;
		int row = sheet.getCurrentRow();
		if (row < 0)
			return;
		GraphicNode node = ((SpreadSheetModel) sheet.getModel()).getNode(row);
		if (node != null && node.getNode() != null && !node.getNode().isVoid())
			frame.addNodeForImpl(null);
		row = sheet.getCurrentRow();
		int column = sheet.getFieldArray().indexOf(Configuration.getFieldFromId("Field.name")) - 1;
		if (row < 0 || column < 0)
			return;
		sheet.changeSelection(row, column, false, false);
		if (sheet.editCellAt(row, column)) {
			Component editor = sheet.getEditorComponent();
			if (editor != null)
				editor.requestFocusInWindow();
		}
	}

	static void perform(GraphicManager graphicManager, String id, ActionEvent e) {
		Action action = graphicManager.getMenuManager().getActionFromId(id);
		if (action != null)
			action.actionPerformed(e);
	}

	/** Adds a task; works without a selection too. */
	public static class NewTaskAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		private final GraphicManager graphicManager;

		NewTaskAction(GraphicManager graphicManager) {
			this.graphicManager = graphicManager;
		}

		public void actionPerformed(ActionEvent e) {
			if (!graphicManager.isDocumentActive())
				return;
			// views without a task sheet (network, resources, reports) switch to the Gantt chart first
			String top = graphicManager.getTopViewId();
			if (!MenuActionConstants.ACTION_GANTT.equals(top) && !MenuActionConstants.ACTION_TRACKING_GANTT.equals(top)
					&& !MenuActionConstants.ACTION_TASK_USAGE_DETAIL.equals(top))
				perform(graphicManager, MenuActionConstants.ACTION_GANTT, e);
			selectFirstEmptyRowIfNone(graphicManager);
			insertAndEditName(graphicManager);
		}

		protected boolean allowed(boolean enable) {
			if (enable == false)
				return true;
			return graphicManager.isDocumentWritable();
		}
	}

	/**
	 * Adds a resource. The generic insert command adds a row to whatever view is shown, so switch to
	 * the resource sheet first when a task view is active.
	 */
	public static class NewResourceAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		private final GraphicManager graphicManager;

		NewResourceAction(GraphicManager graphicManager) {
			this.graphicManager = graphicManager;
		}

		public void actionPerformed(ActionEvent e) {
			if (!graphicManager.isDocumentActive())
				return;
			String top = graphicManager.getTopViewId();
			if (!MenuActionConstants.ACTION_RESOURCES.equals(top) && !MenuActionConstants.ACTION_RBS.equals(top)
					&& !MenuActionConstants.ACTION_RESOURCE_USAGE_DETAIL.equals(top))
				perform(graphicManager, MenuActionConstants.ACTION_RESOURCES, e);
			selectFirstEmptyRowIfNone(graphicManager);
			insertAndEditName(graphicManager);
		}

		protected boolean allowed(boolean enable) {
			if (enable == false)
				return true;
			return graphicManager.isDocumentWritable();
		}
	}

	/** Scrolls the Gantt chart to today. */
	public static class TodayAction extends MenuActionsMap.DocumentMenuAction {
		private static final long serialVersionUID = 1L;
		private final GraphicManager graphicManager;

		TodayAction(GraphicManager graphicManager) {
			this.graphicManager = graphicManager;
		}

		public void actionPerformed(ActionEvent e) {
			DocumentFrame frame = graphicManager.getCurrentFrame();
			if (!graphicManager.isDocumentActive() || frame == null || frame.getMainView() == null)
				return;
			Object top = frame.getMainView().getTopComponent();
			if (!(top instanceof GanttView))
				return;
			final long today = DateTime.dayFloor(DateTime.gmt(new Date()));
			((GanttView) top).getGantt().scrollToTask(new HasStartAndEnd() {
				public long getStart() {
					return today;
				}
				public long getEnd() {
					return today;
				}
			}, false);
		}
	}
}
