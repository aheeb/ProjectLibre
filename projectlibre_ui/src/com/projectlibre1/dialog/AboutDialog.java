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
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.VersionUtils;

public final class AboutDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final String WEBSITE = "http://www.projectlibre.com"; //$NON-NLS-1$

	public static AboutDialog getInstance(Frame owner) {
		return new AboutDialog(owner);
	}

	private AboutDialog(Frame owner) {
		super(owner, Messages.getString("AboutDialog.About") + " " + Messages.getContextString("Text.ApplicationTitle"), true); //$NON-NLS-1$ //$NON-NLS-2$
		setResizable(false);
	}

	public JComponent createContentPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(28, 40, 12, 40));

		JLabel logo = new JLabel(IconManager.getIcon("logo.ProjectLibre"));
		logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		logo.setToolTipText(WEBSITE);
		logo.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				BrowserControl.displayURL(WEBSITE);
			}});
		append(panel, logo, 0);

		JLabel tagline = new JLabel(Messages.getString("AboutDialog.tagline")); //$NON-NLS-1$
		tagline.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4"); //$NON-NLS-1$
		append(panel, tagline, 16);

		String version = VersionUtils.getVersion();
		JLabel versionLabel = new JLabel(MessageFormat.format(Messages.getString("AboutDialog.version"), version == null ? "?" : version)); //$NON-NLS-1$ //$NON-NLS-2$
		append(panel, versionLabel, 6);

		JLabel runtime = new JLabel(MessageFormat.format(Messages.getString("AboutDialog.runtime"), //$NON-NLS-1$
				System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		runtime.putClientProperty(FlatClientProperties.STYLE_CLASS, "small"); //$NON-NLS-1$
		runtime.setForeground(ModernLaf.TEXT_SECONDARY);
		append(panel, runtime, 4);

		JLabel license = new JLabel(Messages.getString("AboutDialog.licenseSummary")); //$NON-NLS-1$
		append(panel, license, 18);

		JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton website = new JButton(Messages.getString("AboutDialog.website")); //$NON-NLS-1$
		website.setToolTipText(WEBSITE);
		website.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BrowserControl.displayURL(WEBSITE);
			}});
		JButton viewLicense = new JButton(Messages.getString("AboutDialog.license")); //$NON-NLS-1$
		viewLicense.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LicenseDialog.showDialog(owner, true);
			}});
		links.add(website);
		links.add(viewLicense);
		append(panel, links, 12);

		JLabel copyright = new JLabel(Messages.getString("AboutDialog.copyright")); //$NON-NLS-1$
		copyright.putClientProperty(FlatClientProperties.STYLE_CLASS, "small"); //$NON-NLS-1$
		copyright.setForeground(ModernLaf.TEXT_SECONDARY);
		append(panel, copyright, 20);
		return panel;
	}

	private static void append(JPanel panel, JComponent component, int gapAbove) {
		if (gapAbove > 0) panel.add(Box.createVerticalStrut(gapAbove));
		component.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(component);
	}

	public ButtonPanel createButtonPanel() {
		ButtonPanel panel = super.createButtonPanel();
		getRootPane().setDefaultButton(ok);
		return panel;
	}

	protected boolean hasCloseButton() {
		return true;
	}

	protected boolean hasOkAndCancelButtons() {
		return false;
	}

}
