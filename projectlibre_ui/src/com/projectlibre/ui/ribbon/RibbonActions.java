package com.projectlibre.ui.ribbon;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuActionsMap;
import com.projectlibre1.pm.graphic.frames.GraphicManager;

/**
 * Commands that only exist in the ribbon.
 */
public class RibbonActions {
	public static final String ACTION_NEW_RESOURCE = "NewResource";

	public static void register(GraphicManager graphicManager, MenuActionsMap actionsMap) {
		actionsMap.addHandler(ACTION_NEW_RESOURCE, new NewResourceAction(graphicManager));
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
				perform(MenuActionConstants.ACTION_RESOURCES, e);
			perform(MenuActionConstants.ACTION_INSERT_TASK, e);
		}

		private void perform(String id, ActionEvent e) {
			Action action = graphicManager.getMenuManager().getActionFromId(id);
			if (action != null)
				action.actionPerformed(e);
		}

		protected boolean allowed(boolean enable) {
			if (enable == false)
				return true;
			return graphicManager.isDocumentWritable();
		}
	}
}
