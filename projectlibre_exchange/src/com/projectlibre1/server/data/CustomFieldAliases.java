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
 * Copyright (c) 2012. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012. All Rights Reserved. Contributor 
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
 * Attribution Copyright Notice: Copyright (c) 2012, ProjectLibre, Inc.
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
package com.projectlibre1.server.data;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mpxj.CustomField;
import net.sf.mpxj.FieldType;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.TaskField;

import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.Field;
import com.projectlibre1.pm.task.ProjectFactory;

/**
 * The names users give their own columns (aliases of custom fields such as Text1 = "Profile")
 * as Microsoft Project custom field aliases, in both directions.
 */
public class CustomFieldAliases {
	private static final String[][] KINDS = { // ProjectLibre id prefix, Microsoft Project task field prefix
		{"Field.customText", "TEXT"},
		{"Field.customNumber", "NUMBER"},
		{"Field.customDate", "DATE"},
		{"Field.customFlag", "FLAG"},
		{"Field.customCost", "COST"},
		{"Field.customDuration", "DURATION"},
		{"Field.customStart", "START"},
		{"Field.customFinish", "FINISH"},
	};
	private static final Pattern MPX_NAME = Pattern.compile("([A-Z]+)(\\d+)"); //$NON-NLS-1$

	public static void toMpx(ProjectFile projectFile) {
		Map aliases = FieldDictionary.getAliasMap();
		for (Iterator i = aliases.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			String id = (String) e.getKey();
			for (int k = 0; k < KINDS.length; k++) {
				String index = id.startsWith(KINDS[k][0]) ? id.substring(KINDS[k][0].length()) : null;
				if (index == null || !index.matches("\\d+")) //$NON-NLS-1$
					continue;
				try {
					TaskField taskField = TaskField.valueOf(KINDS[k][1] + index);
					projectFile.getCustomFields().getCustomField(taskField).setAlias((String) e.getValue());
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
	}

	public static void fromMpx(ProjectFile projectFile) {
		boolean otherProjectsOpen = false; // the project being imported isn't in the portfolio yet
		try {
			otherProjectsOpen = ProjectFactory.getInstance().getPortfolio().getNodeModel().iterator().hasNext();
		} catch (Exception e) {
		}
		for (CustomField customField : projectFile.getCustomFields()) {
			FieldType type = customField.getFieldType();
			String alias = customField.getAlias();
			if (!(type instanceof TaskField) || alias == null || alias.trim().length() == 0)
				continue;
			Matcher m = MPX_NAME.matcher(((TaskField) type).name());
			if (!m.matches())
				continue;
			for (int k = 0; k < KINDS.length; k++) {
				if (KINDS[k][1].equals(m.group(1))) {
					Field field = Configuration.getFieldFromId(KINDS[k][0] + m.group(2));
					// Column names are shared by all open projects: while another project is open, only name
					// a field nobody has named yet, so importing never renames a column in that project.
					if (field != null && (!otherProjectsOpen || field.getAlias() == null || field.getAlias().trim().length() == 0))
						field.setAlias(alias.trim());
				}
			}
		}
	}
}
