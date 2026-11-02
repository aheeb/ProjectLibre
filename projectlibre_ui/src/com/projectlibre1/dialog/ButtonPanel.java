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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.projectlibre1.util.Environment;


/**
 * Dialog button bar. The confirm and cancel buttons sit on the right in platform order
 * (macOS: Cancel then OK; elsewhere: OK then Cancel), the help button and any secondary
 * actions sit on the left.
 */
public class ButtonPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	static final String HELP_BUTTON = "ProjectLibre.helpButton"; //$NON-NLS-1$
	private static final int GAP = 8;
	private static final int GROUP_GAP = 24;
	private AbstractButton confirm;
	private AbstractButton dismiss;

	public ButtonPanel() {
		setLayout(new BarLayout());
		setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
	}
	public void addButton(JButton button) {
		add(button);
	}

	/**
	 * Marks the dialog's confirm (default) and cancel buttons. They are placed on the right in
	 * platform order; every other button except help moves to the left. Either may be null.
	 */
	public void setActionButtons(AbstractButton confirm, AbstractButton dismiss) {
		this.confirm = confirm;
		this.dismiss = dismiss;
		if (confirm != null && confirm.getParent() == this)
			remove(confirm);
		if (dismiss != null && dismiss.getParent() == this)
			remove(dismiss);
		AbstractButton first = Environment.isMac() ? dismiss : confirm;
		AbstractButton last = Environment.isMac() ? confirm : dismiss;
		if (first != null)
			add(first);
		if (last != null)
			add(last);
		revalidate();
	}

	private boolean onRight(Component c) {
		if (isHelp(c))
			return false;
		if (confirm == null && dismiss == null)
			return true;
		return c == confirm || c == dismiss;
	}
	private static boolean isHelp(Component c) {
		return c instanceof JComponent && ((JComponent) c).getClientProperty(HELP_BUTTON) != null;
	}

	private class BarLayout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}
		public void removeLayoutComponent(Component comp) {
		}
		private List<Component> visible(Container parent, boolean right) {
			List<Component> list = new ArrayList<Component>();
			for (Component c : parent.getComponents()) {
				if (!c.isVisible() || onRight(c) != right)
					continue;
				if (!right && isHelp(c))
					list.add(0, c); // help always leftmost
				else
					list.add(c);
			}
			return list;
		}
		private int width(List<Component> list) {
			int w = 0;
			for (Component c : list)
				w += c.getPreferredSize().width;
			return w + Math.max(0, list.size() - 1) * GAP;
		}
		public Dimension preferredLayoutSize(Container parent) {
			List<Component> left = visible(parent, false);
			List<Component> right = visible(parent, true);
			int h = 0;
			for (Component c : parent.getComponents())
				if (c.isVisible())
					h = Math.max(h, c.getPreferredSize().height);
			int w = width(left) + width(right) + (left.isEmpty() || right.isEmpty() ? 0 : GROUP_GAP);
			Insets in = parent.getInsets();
			return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
		}
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}
		public void layoutContainer(Container parent) {
			Insets in = parent.getInsets();
			int h = parent.getHeight() - in.top - in.bottom;
			int x = in.left;
			for (Component c : visible(parent, false)) {
				Dimension d = c.getPreferredSize();
				c.setBounds(x, in.top + (h - d.height) / 2, d.width, d.height);
				x += d.width + GAP;
			}
			List<Component> right = visible(parent, true);
			x = parent.getWidth() - in.right - width(right);
			for (Component c : right) {
				Dimension d = c.getPreferredSize();
				c.setBounds(x, in.top + (h - d.height) / 2, d.width, d.height);
				x += d.width + GAP;
			}
		}
	}
}
