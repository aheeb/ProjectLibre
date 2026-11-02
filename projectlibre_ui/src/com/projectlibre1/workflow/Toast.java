package com.projectlibre1.workflow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.strings.Messages;

/**
 * A short, non-modal message at the bottom of the main window, with optional action buttons.
 * It never takes focus and disappears by itself, so it can explain what just happened without
 * interrupting the user. Only one toast is shown at a time.
 */
public class Toast extends JPanel {
	private static final int SECONDS = 9;
	private static Toast current;

	private final Timer timer;
	private final JLayeredPane layer;
	private final ComponentAdapter resizer = new ComponentAdapter() {
		public void componentResized(ComponentEvent e) {
			place();
		}
	};

	/** Shows {@code message} near {@code anchor} (any component of the window; null for the main frame). */
	public static Toast show(Component anchor, String message) {
		return show(anchor, message, null, null, null, null);
	}

	public static Toast show(Component anchor, String message, String action, ActionListener listener) {
		return show(anchor, message, action, listener, null, null);
	}

	public static Toast show(Component anchor, String message, String action, ActionListener listener, String action2, ActionListener listener2) {
		JRootPane root = null;
		if (anchor != null) root = SwingUtilities.getRootPane(anchor);
		if (root == null) {
			Component frame = GraphicManager.getInstance() == null ? null : GraphicManager.getInstance().getFrame();
			if (frame != null) root = SwingUtilities.getRootPane(frame);
		}
		if (root == null) return null;
		hideCurrent();
		Toast toast = new Toast(root.getLayeredPane(), message);
		if (action != null) toast.addAction(action, listener);
		if (action2 != null) toast.addAction(action2, listener2);
		toast.open();
		return toast;
	}

	public static void hideCurrent() {
		if (current != null) current.close();
	}

	private Toast(JLayeredPane layer, String message) {
		super(new FlowLayout(FlowLayout.LEFT, 10, 0));
		this.layer = layer;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 8));
		JLabel label = new JLabel(message);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
		add(label);
		JButton close = flatButton("×", new Color(0xC9CDD3));
		close.setFont(close.getFont().deriveFont(15f));
		close.setToolTipText(Messages.getString("Toast.dismiss"));
		close.addActionListener(e -> close());
		add(close);
		timer = new Timer(SECONDS * 1000, e -> close());
		timer.setRepeats(false);
		addMouseListener(new MouseAdapter() { // keep it while the pointer is on it
			public void mouseEntered(MouseEvent e) {
				timer.stop();
			}
			public void mouseExited(MouseEvent e) {
				timer.restart();
			}
		});
	}

	private void addAction(String text, ActionListener listener) {
		JButton b = flatButton(text, new Color(0x8DB4FF));
		b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
		b.addActionListener(e -> {
			close();
			if (listener != null) listener.actionPerformed(e);
		});
		add(b, getComponentCount() - 1); // before the close button
	}

	private static JButton flatButton(String text, Color color) {
		JButton b = new JButton(text);
		b.setForeground(color);
		b.setContentAreaFilled(false);
		b.setBorderPainted(false);
		b.setFocusable(false);
		b.setOpaque(false);
		b.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return b;
	}

	private void open() {
		current = this;
		layer.add(this, JLayeredPane.POPUP_LAYER);
		layer.addComponentListener(resizer);
		place();
		revalidate();
		repaint();
		timer.start();
	}

	public void close() {
		timer.stop();
		layer.removeComponentListener(resizer);
		if (getParent() != null) {
			java.awt.Rectangle r = getBounds();
			layer.remove(this);
			layer.repaint(r.x, r.y, r.width, r.height);
		}
		if (current == this) current = null;
	}

	private void place() {
		Dimension d = getPreferredSize();
		int w = Math.min(d.width, layer.getWidth() - 40);
		setBounds((layer.getWidth() - w) / 2, layer.getHeight() - d.height - 36, w, d.height);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(new Color(0, 0, 0, 40));
		g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, 12, 12);
		g2.setColor(ModernLaf.TEXT_PRIMARY);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 12, 12);
		g2.setStroke(new BasicStroke(1f));
		g2.dispose();
	}
}
