/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.dialog;

import java.awt.Font;
import java.awt.Frame;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.projectlibre1.dialog.util.ComponentFactory;
import com.projectlibre1.dialog.util.FieldComponentMap;
import com.projectlibre1.pm.graphic.frames.DocumentSelectedEvent;
import com.projectlibre1.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.document.ObjectEvent;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Environment;
/**
 *
 */
public class ProjectInformationDialog extends InformationDialog {
	private static final long serialVersionUID = 1L;

	public static ProjectInformationDialog getInstance(Frame owner, Project project) {
		return new ProjectInformationDialog(owner, project);
	}

	private ProjectInformationDialog(Frame owner, Project project) {
		super(owner, Messages.getString("ProjectInformationDialog.ProjectInformation")); //$NON-NLS-1$
		setObjectClass(Project.class);
		setObject(project);
		addDocHelp("Project_Information_Dialog");

	}

	private JTabbedPane tabbedPane;
	
	public JComponent createContentPanel() {	
	    	
		FormLayout layout = new FormLayout("max(350dlu;pref):grow","fill:max(250dlu;pref):grow"); //$NON-NLS-1$ //$NON-NLS-2$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		
		tabbedPane= new JTabbedPane();
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.General"),createGeneralPanel()); //$NON-NLS-1$
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.Statistics"),createStatisticsPanel()); //$NON-NLS-1$
		tabbedPane.addTab(Messages.getString("ProjectInformationDialog.Notes"), createNotesPanel()); //$NON-NLS-1$
		builder.add(tabbedPane);
		mainComponent = tabbedPane;
		return builder.getPanel();
	}

	private JComponent createGeneralPanel(){
		FieldComponentMap map = createMap();
		boolean accessControl = !Environment.getStandAlone();
		Collection extraFields = FieldDictionary.extractExtraFields(FieldDictionary.getInstance().getProjectFields(),false);
		FormLayout layout = new FormLayout(FORM_COLUMNS,
				"p, 4dlu, p, 12dlu, p, 4dlu, p, 4dlu, p, 4dlu, p, 12dlu, p, 4dlu, p, 4dlu, p, 4dlu, p, 4dlu, p, 4dlu, p" //$NON-NLS-1$
				+ (accessControl ? ", 4dlu, p" : "") + ", 4dlu, p, 0dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.setDefaultDialogBorder();
		map.append(builder,"Field.name",FORM_SPAN - 3); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.manager"); //$NON-NLS-1$
		builder.nextLine(2);

		addSection(builder, Messages.getString("ProjectInformationDialog.Schedule")); //$NON-NLS-1$
		builder.nextLine(2);
		map.appendSometimesReadOnly(builder,"Field.startDate"); //$NON-NLS-1$
		map.appendSometimesReadOnly(builder,"Field.finishDate"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.forward",3); //$NON-NLS-1$
		builder.nextColumn(4);
		map.append(builder,"Field.baseCalendar"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.currentDate"); //$NON-NLS-1$
		map.append(builder,"Field.statusDate"); //$NON-NLS-1$
		builder.nextLine(2);

		addSection(builder, Messages.getString("ProjectInformationDialog.Details")); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.projectStatus"); //$NON-NLS-1$
		map.append(builder,"Field.priority"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.projectType"); //$NON-NLS-1$
		map.append(builder,"Field.expenseType"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.projectDivision"); //$NON-NLS-1$
		map.append(builder,"Field.projectGroup"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.netPresentValue"); //$NON-NLS-1$
		map.append(builder,"Field.benefit"); //$NON-NLS-1$
		builder.nextLine(2);
		map.append(builder,"Field.risk"); //$NON-NLS-1$
		builder.nextLine(2);

		if (accessControl){
			map.append(builder,"Field.accessControlPolicy",3); //$NON-NLS-1$
			builder.nextLine(2);
		}
		JComponent extra = createFieldsPanel(map, extraFields);
		if (extra != null) {
			builder.add(extra,cc.xyw(1, builder.getRow(), FORM_SPAN - 1));
		}
		return builder.getPanel();
	}

	/** Current, baseline, actual and remaining values side by side, one row per measure. */
	private JComponent createStatisticsPanel(){
		FieldComponentMap map = createMap();
		String[][] rows = {
				{"Field.startDate", "Field.baselineStart", "Field.actualStart", null}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"Field.finishDate", "Field.baselineFinish", "Field.actualFinish", null}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"Field.duration", "Field.baselineDuration", "Field.actualDuration", "Field.remainingDuration"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{"Field.work", "Field.baselineWork", "Field.actualWork", "Field.remainingWork"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{"Field.cost", "Field.baselineCost", "Field.actualCost", "Field.remainingCost"}}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String[] headers = {"ProjectInformationDialog.Current", "ProjectInformationDialog.Baseline", //$NON-NLS-1$ //$NON-NLS-2$
				"ProjectInformationDialog.Actual", "ProjectInformationDialog.Remaining"}; //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer rowSpec = new StringBuffer("p, 8dlu, p, 4dlu, p, 6dlu, p"); //$NON-NLS-1$
		for (int i = 0; i < rows.length; i++)
			rowSpec.append(", 6dlu, p"); //$NON-NLS-1$
		rowSpec.append(", 14dlu, p, 6dlu, p, 6dlu, p"); //$NON-NLS-1$ // size of the plan
		FormLayout layout = new FormLayout("max(75dlu;pref), 4dlu, 70dlu, 8dlu, 70dlu, 8dlu, 70dlu, 8dlu, 70dlu, 0dlu:grow", //$NON-NLS-1$
				rowSpec.toString());
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(createHeaderFieldsPanel(map),cc.xyw(1, 1, 10));
		builder.add(FieldComponentMap.createHintLabelText(Messages.getString("ProjectInformationDialog.StatisticsHint"), 370), cc.xyw(1, 3, 10)); //$NON-NLS-1$
		int row = 5;
		for (int c = 0; c < headers.length; c++) {
			JLabel header = new JLabel(Messages.getString(headers[c]));
			header.setFont(header.getFont().deriveFont(Font.BOLD));
			builder.add(header, cc.xy(3 + 2 * c, row));
		}
		row += 2;
		JSeparator line = new JSeparator();
		builder.add(line, cc.xyw(1, row, 9));
		for (int r = 0; r < rows.length; r++) {
			row += 2;
			builder.addLabel(map.getLabel(rows[r][0]) + ":", cc.xy(1, row)); //$NON-NLS-1$
			for (int c = 0; c < rows[r].length; c++) {
				if (rows[r][c] != null)
					builder.add(map.getComponent(rows[r][c], ComponentFactory.READ_ONLY), cc.xy(3 + 2 * c, row));
			}
		}
		row += 2;
		builder.addSeparator(Messages.getString("ProjectInformationDialog.Size"), cc.xyw(1, row, 9)); //$NON-NLS-1$
		row += 2;
		builder.addLabel(Messages.getString("ProjectInformationDialog.Tasks"), cc.xy(1, row)); //$NON-NLS-1$
		builder.add(taskCount, cc.xyw(3, row, 7));
		row += 2;
		builder.addLabel(Messages.getString("ProjectInformationDialog.People"), cc.xy(1, row)); //$NON-NLS-1$
		builder.add(peopleCount, cc.xyw(3, row, 7));
		return builder.getPanel();
	}

	/* (non-Javadoc)
	 * @see com.projectlibre1.dialog.InformationDialog#createHeaderFieldsPanel(com.projectlibre1.dialog.util.FieldComponentMap)
	 */
	protected JComponent createHeaderFieldsPanel(FieldComponentMap map) {
		FormLayout layout = new FormLayout(NAME_COLUMNS, "p"); //$NON-NLS-1$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		map.append(builder,"Field.name"); //$NON-NLS-1$
		return builder.getPanel();
	}
	

	public void documentSelected(DocumentSelectedEvent evt) {
		setObject(evt.getCurrent().getProject());
		updateAll();
	}

	public void selectionChanged(SelectionNodeEvent e) {
		
	}
	public void objectChanged(ObjectEvent objectEvent) {
		if (!isVisible())
			return;
		Object obj = objectEvent.getObject();
		Project project = (Project)getObject();
		boolean update = false;
		if (obj == getObject()) {
			update = true;
		} else if (obj instanceof Task) {
			if (((Task)obj).getProject() == project)
				update = true;
				
		} else if (obj instanceof Resource) {
			if (((Resource)obj).getDocument() == project.getResourcePool())
				update = true;
		}
		if (update)
			updateAll();
	}
	
	public void setObject(Object object) {
		super.setObject(object);
		if (object != null)	
			updateAll();
	}	

	protected void updateAll() {
		super.updateAll();
		updateCounts();
	}

	private final JLabel taskCount = new JLabel();
	private final JLabel peopleCount = new JLabel();

	/** How many tasks (not counting summary tasks) the plan has, and how many team members have work in it. */
	private void updateCounts() {
		if (!(getObject() instanceof Project))
			return;
		Project project = (Project) getObject();
		int tasks = 0, summaries = 0;
		Set people = new HashSet();
		for (Iterator i = project.getTaskOutlineIterator(); i.hasNext();) {
			Task task = (Task) i.next();
			if (task.isWbsParent()) {
				summaries++;
				continue;
			}
			tasks++;
			if (task instanceof NormalTask) {
				for (Iterator a = ((NormalTask) task).getAssignments().iterator(); a.hasNext();) {
					Resource resource = ((Assignment) a.next()).getResource();
					if (resource instanceof ResourceImpl && !((ResourceImpl) resource).isDefault())
						people.add(resource);
				}
			}
		}
		int team = 0;
		for (Iterator i = project.getResourcePool().getResourceList().iterator(); i.hasNext();)
			if (!((ResourceImpl) i.next()).isDefault())
				team++;
		taskCount.setText(MessageFormat.format(Messages.getString("ProjectInformationDialog.TaskCount"), tasks, summaries)); //$NON-NLS-1$
		String peopleKey = team == 0 ? "ProjectInformationDialog.PeopleCountNone" : team == 1 ? "ProjectInformationDialog.PeopleCountOne" : "ProjectInformationDialog.PeopleCount"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		peopleCount.setText(MessageFormat.format(Messages.getString(peopleKey), people.size(), team));
	}
	
}
