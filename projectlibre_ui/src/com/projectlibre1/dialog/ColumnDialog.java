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

import com.projectlibre1.dialog.util.FieldComponentMap;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.field.Field;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;

/**
 * Insert Column: either show existing information, or create a column of your own by giving it a name.
 * A column of your own is the first unused custom field of the chosen kind (Text1, Text2...) renamed with
 * an alias, which is saved with the project and exported to Microsoft Project.
 */
public final class ColumnDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	/** custom field id prefixes and how many of each exist, in the order of the kind choice */
	private static final String[] KIND_IDS = {"Field.customText", "Field.customNumber", "Field.customDate", "Field.customFlag", "Field.customCost"};
	private static final String[] KIND_KEYS = {"ColumnDialog.KindText", "ColumnDialog.KindNumber", "ColumnDialog.KindDate", "ColumnDialog.KindFlag", "ColumnDialog.KindCost"};

	JComboBox combo = null;
	/** Offered first in the list of existing fields, when the sheet has them and doesn't show them yet. */
	private static final String[] COMMON_FIELDS = {"Field.duration", "Field.work", "Field.start", "Field.finish",
			"Field.percentComplete", "Field.predecessors", "Field.successors", "Field.resourceNames", "Field.cost",
			"Field.notes", "Field.wbs", "Field.baselineStart", "Field.baselineFinish", "Field.totalSlack",
			"Field.maxUnits", "Field.standardRate", "Field.group", "Field.emailAddress"};

	JRadioButton existing;
	JRadioButton own;
	JTextField ownName;
	JComboBox ownKind;
	List fieldList;
	ArrayList currentFields;
	Field field;
	Project project;
	public static Field getFieldFromDialog(Component component, List fieldList, ArrayList currentFields) {
		ColumnDialog dlg = new ColumnDialog(component,fieldList,currentFields);
		if (dlg.doModal())
			return dlg.getField();
		return null;
	}
	
	
	private ColumnDialog(Component component,List fieldList, ArrayList currentFields) {
		super(GraphicManager.getInstance(component).getFrame(), Messages.getString("ColumnDialog.InsertColumn"), true); //$NON-NLS-1$
		this.fieldList = fieldList;
		this.currentFields = currentFields;
		DocumentFrame frame = GraphicManager.getInstance(component).getCurrentFrame();
		project = frame == null ? null : frame.getProject();
		addDocHelp("Spreadsheet");
	}

	// Component Creation and Initialization **********************************

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		existing = new JRadioButton(Messages.getString("ColumnDialog.Existing")); //$NON-NLS-1$
		own = new JRadioButton(Messages.getString("ColumnDialog.Own")); //$NON-NLS-1$
		ButtonGroup group = new ButtonGroup();
		group.add(existing);
		group.add(own);
		existing.setSelected(true);
		ownName = new JTextField();
		ownName.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { own.setSelected(true); }
			public void removeUpdate(DocumentEvent e) {}
			public void changedUpdate(DocumentEvent e) {}
		});
		String[] kinds = new String[KIND_KEYS.length];
		for (int i = 0; i < kinds.length; i++)
			kinds[i] = Messages.getString(KIND_KEYS[i]);
		ownKind = new JComboBox(kinds);
		ownKind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { own.setSelected(true); }
		});
		bind(true);
		combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { existing.setSelected(true); }
		});
	}

	protected boolean bind(boolean get) {
		if (get) {
			// the fields people add most come first, then all of them by name
			ArrayList rest = new ArrayList();
			rest.addAll(fieldList);
			Collections.sort(rest);
			final ArrayList l = new ArrayList();
			for (String id : COMMON_FIELDS) {
				Field f = Configuration.getFieldFromId(id);
				if (f != null && rest.contains(f) && !currentFields.contains(f)) {
					l.add(f);
					rest.remove(f);
				}
			}
			final int common = l.size();
			l.addAll(rest);
			combo = new JComboBox(new DefaultComboBoxModel(l.toArray()));
			combo.setMaximumRowCount(16);
			combo.setRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					JComponent c = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if (index == common - 1)
						c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernLaf.HAIRLINE), c.getBorder()));
					return c;
				}
			});
		} else if (own.isSelected()) {
			String name = ownName.getText().trim();
			if (name.length() == 0) {
				Alert.warn(Messages.getString("ColumnDialog.NameMissing"), this); //$NON-NLS-1$
				ownName.requestFocusInWindow();
				return false;
			}
			field = findFreeField(KIND_IDS[ownKind.getSelectedIndex()]);
			if (field == null) {
				Alert.warn(Messages.getString("ColumnDialog.NoneFree"), this); //$NON-NLS-1$
				return false;
			}
			field.setAlias(name);
			if (project != null)
				project.setGroupDirty(true);
		} else {
			field = (Field)combo.getSelectedItem();
		}
		return true;
	}

	/** The first custom field of the kind that has no name of its own, is not shown and holds no task values. */
	private Field findFreeField(String idPrefix) {
		for (int i = 1; ; i++) {
			Field f = Configuration.getFieldFromId(idPrefix + i);
			if (f == null)
				return null;
			if (f.getAlias() == null && !currentFields.contains(f) && !hasValues(f, project))
				return f;
		}
	}

	/** Whether any task of the project has a value (not blank, No or 0) in the field. */
	public static boolean hasValues(Field f, Project project) {
		if (project == null)
			return false;
		for (Iterator i = project.getTaskOutlineIterator(); i.hasNext();) {
			Object value = f.getValue(i.next(), null);
			if (value != null && !"".equals(value) && !Boolean.FALSE.equals(value) //$NON-NLS-1$
					&& !(value instanceof Number && ((Number) value).doubleValue() == 0))
				return true;
		}
		return false;
	}

	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout("12dlu, default, 4dlu, 110dlu:grow, 4dlu, default", // cols //$NON-NLS-1$
				"p, 3dlu, p, 3dlu, p, 10dlu, p, 3dlu, p, 3dlu, p"); // rows //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.add(existing, cc.xyw(1, 1, 6));
		builder.add(combo, cc.xyw(2, 3, 5));
		builder.add(FieldComponentMap.createHintLabelText(Messages.getString("ColumnDialog.Hint"), 180), cc.xyw(2, 5, 5)); //$NON-NLS-1$
		builder.add(own, cc.xyw(1, 7, 6));
		builder.addLabel(Messages.getString("ColumnDialog.OwnName"), cc.xy(2, 9)); //$NON-NLS-1$
		builder.add(ownName, cc.xy(4, 9));
		builder.add(ownKind, cc.xy(6, 9));
		builder.add(FieldComponentMap.createHintLabelText(Messages.getString("ColumnDialog.OwnHint"), 180), cc.xyw(2, 11, 5)); //$NON-NLS-1$
		return builder.getPanel();
	}

	protected String getOkButtonText() {
		return Messages.getString("ColumnDialog.Insert"); //$NON-NLS-1$
	}
	public final Field getField() {
		return field;
	}

}
