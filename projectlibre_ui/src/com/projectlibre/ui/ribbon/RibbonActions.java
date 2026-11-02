package com.projectlibre.ui.ribbon;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuActionsMap;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheet;

/**
 * Commands that only exist in the ribbon.
 */
public class RibbonActions {
	public static final String ACTION_NEW_TASK = "NewTask";
	public static final String ACTION_NEW_RESOURCE = "NewResource";

	public static void register(final GraphicManager graphicManager, MenuActionsMap actionsMap) {
		actionsMap.addHandler(ACTION_NEW_RESOURCE, new NewResourceAction(graphicManager));
		actionsMap.addHandler(ACTION_NEW_TASK, new NewTaskAction(graphicManager));
		// "New task" is available exactly when the plain insert command is
		Action insert = actionsMap.getActionFromMenuId(MenuActionConstants.ACTION_INSERT_TASK);
		if (insert != null)
			insert.addPropertyChangeListener(evt -> {
				if ("enabled".equals(evt.getPropertyName()))
					graphicManager.getMenuManager().setActionEnabled(ACTION_NEW_TASK, Boolean.TRUE.equals(evt.getNewValue()));
			});
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
			selectFirstEmptyRowIfNone(graphicManager);
			perform(graphicManager, MenuActionConstants.ACTION_INSERT_TASK, e);
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
			perform(graphicManager, MenuActionConstants.ACTION_INSERT_TASK, e);
		}

		protected boolean allowed(boolean enable) {
			if (enable == false)
				return true;
			return graphicManager.isDocumentWritable();
		}
	}
}
