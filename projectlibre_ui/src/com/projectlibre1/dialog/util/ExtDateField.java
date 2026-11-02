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
package com.projectlibre1.dialog.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.DateFormatter;

import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.strings.Messages;

import net.sf.nachocalendar.components.ArrowButton;
import net.sf.nachocalendar.components.DateField;
import net.sf.nachocalendar.components.DayPanel;
import net.sf.nachocalendar.components.DayRenderer;
import net.sf.nachocalendar.components.HeaderPanel;
import net.sf.nachocalendar.components.HeaderRenderer;

public class ExtDateField extends DateField {
	private JButton pickButton;

	public ExtDateField(boolean showWeekNumbers) {
		super(showWeekNumbers);
		initRenderers();
	}

	public ExtDateField() {
		initRenderers();
	}

	public ExtDateField(DateFormatter formatter) {
		super(formatter);
		initRenderers();
	}

	public ExtDateField(DateFormat dateFormat) {
		super(dateFormat);
		initRenderers();
	}

	public ExtDateField(Locale locale) {
		super(locale);
		initRenderers();
	}

	public Date getDateValue() {
		Object value = getValue();
		if (value instanceof String) {
			try {
				value =  getDateFormat().parse((String)value);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return (Date)value;

	}

	private void initRenderers() {
		setRenderer(new ModernDayRenderer());
		setHeaderRenderer(new ModernHeaderRenderer());
	}

	/**
	 * Makes the field look like a FlatLaf combo box for use in forms: padded text and a
	 * calendar button instead of nachocalendar's small painted arrow.
	 */
	public void useFormStyle() {
		if (pickButton != null)
			return;
		for (Component c : getComponents())
			if (c instanceof ArrowButton)
				remove(c);
		getFormattedTextField().setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 2));
		pickButton = new JButton(new CalendarIcon());
		pickButton.putClientProperty("JButton.buttonType", "toolBarButton"); //$NON-NLS-1$ //$NON-NLS-2$
		pickButton.setFocusable(false);
		pickButton.setMargin(new Insets(1, 4, 1, 4));
		pickButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		pickButton.setToolTipText(Messages.getString("DateField.pickDate")); //$NON-NLS-1$
		pickButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExtDateField.this.actionPerformed(e);
			}});
		pickButton.setEnabled(isEnabled());
		pickButton.setOpaque(false);
		add(pickButton, BorderLayout.EAST);
		setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$
		revalidate();
	}

	/** In form style, as tall as a text field so it lines up with its neighbours. */
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		if (pickButton != null)
			d.height = Math.max(d.height, new JTextField().getPreferredSize().height);
		return d;
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (pickButton != null)
			pickButton.setEnabled(enabled);
	}

	/** Small outline calendar glyph drawn in the secondary text colour. */
	private static class CalendarIcon implements Icon {
		public int getIconWidth() {
			return 14;
		}
		public int getIconHeight() {
			return 14;
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(c.isEnabled() ? ModernLaf.TEXT_SECONDARY : UIManager.getColor("Label.disabledForeground")); //$NON-NLS-1$
			g2.setStroke(new BasicStroke(1.2f));
			g2.drawRoundRect(x + 1, y + 2, 12, 11, 3, 3);
			g2.drawLine(x + 1, y + 6, x + 13, y + 6);
			g2.drawLine(x + 4, y + 1, x + 4, y + 3);
			g2.drawLine(x + 10, y + 1, x + 10, y + 3);
			g2.dispose();
		}
	}

	/** Day cell: plain number, accent pill when selected, accent text for today. */
	static class ModernDayRenderer extends JLabel implements DayRenderer {
		private static final long serialVersionUID = 1L;
		private final Calendar cal = Calendar.getInstance();
		private boolean selected;

		ModernDayRenderer() {
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setOpaque(false);
		}

		public Component getDayRenderer(DayPanel panel, Date day, Object data, boolean selected, boolean working, boolean enabled) {
			this.selected = selected && enabled;
			cal.setTime(day);
			int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
			setText(Integer.toString(dayOfMonth));
			Color fg = UIManager.getColor("Label.foreground"); //$NON-NLS-1$
			if (!enabled)
				fg = ModernLaf.HAIRLINE.darker();
			else if (this.selected)
				fg = Color.WHITE;
			else if (isToday(cal))
				fg = ModernLaf.ACCENT;
			else if (!working)
				fg = ModernLaf.TEXT_SECONDARY;
			setForeground(fg);
			setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
			return this;
		}

		private static boolean isToday(Calendar c) {
			Calendar now = Calendar.getInstance();
			return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
		}

		protected void paintComponent(Graphics g) {
			if (selected) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(ModernLaf.ACCENT);
				int d = Math.min(getWidth(), getHeight()) - 2;
				int w = Math.min(getWidth() - 2, d + 6);
				g2.fillRoundRect((getWidth() - w) / 2, (getHeight() - d) / 2, w, d, d, d);
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}

	/** Weekday header: small secondary text on the popup background, no dark bar. */
	static class ModernHeaderRenderer extends JLabel implements HeaderRenderer {
		private static final long serialVersionUID = 1L;

		ModernHeaderRenderer() {
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setOpaque(false);
		}

		public Component getHeaderRenderer(HeaderPanel panel, Object value, boolean isHeader, boolean isWorking) {
			setText(value == null ? "" : value.toString()); //$NON-NLS-1$
			Font f = UIManager.getFont("Label.font"); //$NON-NLS-1$
			setFont(f.deriveFont(f.getSize2D() - 1f));
			setForeground(ModernLaf.TEXT_SECONDARY);
			return this;
		}
	}

}
