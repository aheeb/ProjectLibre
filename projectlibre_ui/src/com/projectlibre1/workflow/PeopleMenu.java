package com.projectlibre1.workflow;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.strings.Messages;

/**
 * The right-click menu of people (resource) rows, in the Resources and Workload views: the things a
 * team needs, such as someone's vacation, one click away from their row.
 */
public class PeopleMenu {
	private final SpreadSheet sheet;

	public PeopleMenu(SpreadSheet sheet) {
		this.sheet = sheet;
	}

	/** True when {@code sheet} lists the people of a project (Resources view, Workload view). */
	public static boolean isPeopleSheet(SpreadSheet sheet) {
		if (!(sheet.getModel() instanceof SpreadSheetModel)) return false;
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		return model.getCache() != null && model.getCache().getModel().getDataFactory() instanceof ResourcePool;
	}

	/** Selected people in row order (no blank rows, no assignment rows). */
	public List<Resource> selectedPeople() {
		List<Resource> people = new ArrayList<Resource>();
		for (Object o : sheet.getSelectedNodesImpl())
			if (o instanceof Resource) people.add((Resource) o);
		return people;
	}

	public JPopupMenu build() {
		JPopupMenu menu = new JPopupMenu();
		boolean editable = !sheet.isReadOnly();
		List<Resource> people = selectedPeople();
		boolean one = people.size() == 1;
		final GraphicManager gm = GraphicManager.getInstance(sheet);

		add(menu, "PeopleMenu.insert", editable, null, () -> sheet.executeAction(MenuActionConstants.ACTION_NEW));
		add(menu, "PeopleMenu.delete", editable && !people.isEmpty(), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), () -> sheet.executeAction(MenuActionConstants.ACTION_DELETE));
		menu.addSeparator();
		add(menu, "PeopleMenu.workingTime", one, null, () -> run(gm, MenuActionConstants.ACTION_CHANGE_WORKING_TIME));
		add(menu, "PeopleMenu.details", one, null, () -> gm.doInformationDialog(false));
		boolean separated = false;
		for (String id : new String[] {MenuActionConstants.ACTION_CUT, MenuActionConstants.ACTION_COPY, MenuActionConstants.ACTION_PASTE}) {
			if (!(sheet.getAction(id) instanceof Action)) continue;
			if (!separated) menu.addSeparator();
			separated = true;
			menu.add(new JMenuItem((Action) sheet.prepareAction(id)));
		}
		return menu;
	}

	private void run(GraphicManager gm, String actionId) {
		Action action = gm.getRawAction(actionId);
		if (action != null) action.actionPerformed(new ActionEvent(sheet, ActionEvent.ACTION_PERFORMED, actionId));
	}

	private static void add(JPopupMenu menu, String key, boolean enabled, KeyStroke accelerator, final Runnable run) {
		JMenuItem item = new JMenuItem(new AbstractAction(Messages.getString(key)) {
			public void actionPerformed(ActionEvent e) {
				run.run();
			}
		});
		item.setEnabled(enabled);
		if (accelerator != null) item.setAccelerator(accelerator);
		menu.add(item);
	}
}
