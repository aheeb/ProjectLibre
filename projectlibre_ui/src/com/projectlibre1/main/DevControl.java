package com.projectlibre1.main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Development aid for driving a running instance without screen-recording or accessibility
 * permissions. Enabled only when -Dprojectlibre.snapshotDir=DIR is set (dev/run.sh does this).
 * <ul>
 * <li>touch DIR/request: paints every visible window to DIR/window-N.png and lists them in DIR/index.txt</li>
 * <li>write DIR/script.groovy: runs it on the EDT; output and errors go to DIR/script.out.
 * The binding has {@code out} (PrintWriter) and {@code dev} (this class: windows(), find(text), click(text),
 * tree(), snapshot()).</li>
 * </ul>
 */
public class DevControl {
	static File folder;

	public static void startIfEnabled() {
		String dir = System.getProperty("projectlibre.snapshotDir");
		if (dir == null || dir.isEmpty()) return;
		folder = new File(dir);
		folder.mkdirs();
		new File(folder, "script.groovy").delete();
		new File(folder, "request").delete();
		Thread t = new Thread(() -> {
			File request = new File(folder, "request");
			File script = new File(folder, "script.groovy");
			while (true) {
				try {
					Thread.sleep(200);
					if (script.exists()) {
						String code = Files.readString(script.toPath());
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						SwingUtilities.invokeAndWait(() -> runScript(code, pw));
						pw.flush();
						Files.writeString(new File(folder, "script.out").toPath(), sw.toString());
						script.delete();
					}
					if (request.exists()) {
						SwingUtilities.invokeAndWait(DevControl::snapshot);
						request.delete();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}, "DevControl");
		t.setDaemon(true);
		t.start();
	}

	static void runScript(String code, PrintWriter out) {
		try {
			Class<?> bindingClass = Class.forName("groovy.lang.Binding");
			Object binding = bindingClass.getConstructor().newInstance();
			Method setVariable = bindingClass.getMethod("setVariable", String.class, Object.class);
			setVariable.invoke(binding, "out", out);
			setVariable.invoke(binding, "dev", new DevControl());
			Class<?> shellClass = Class.forName("groovy.lang.GroovyShell");
			Object shell = shellClass.getConstructor(bindingClass).newInstance(binding);
			Object result = shellClass.getMethod("evaluate", String.class).invoke(shell, code);
			if (result != null) out.println("=> " + result);
		} catch (Throwable e) {
			Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
			cause.printStackTrace(out);
		}
	}

	public static List<Window> windows() {
		List<Window> list = new ArrayList<>();
		for (Window w : Window.getWindows())
			if (w.isShowing()) list.add(w);
		return list;
	}

	static String textOf(Component c) {
		if (c instanceof AbstractButton) return ((AbstractButton) c).getText();
		try {
			Object o = c.getClass().getMethod("getText").invoke(c);
			return o == null ? null : o.toString();
		} catch (Exception e) {
			return null;
		}
	}

	/** All showing components whose text or tooltip equals {@code text} (case-insensitive). */
	public static List<Component> find(String text) {
		List<Component> found = new ArrayList<>();
		for (Window w : windows()) collect(w, text, found);
		return found;
	}

	static void collect(Component c, String text, List<Component> found) {
		if (!c.isShowing()) return;
		String t = textOf(c);
		String tip = (c instanceof JComponent) ? ((JComponent) c).getToolTipText() : null;
		if ((t != null && t.trim().equalsIgnoreCase(text)) || (tip != null && tip.trim().equalsIgnoreCase(text)))
			found.add(c);
		if (c instanceof Container)
			for (Component child : ((Container) c).getComponents()) collect(child, text, found);
	}

	/**
	 * Clicks the first showing button (Swing or Flamingo ribbon command) labelled {@code text}.
	 * The click is queued with invokeLater so a modal dialog it opens does not block the script.
	 */
	public static String click(String text) {
		for (Component c : find(text)) {
			if (c instanceof AbstractButton) {
				SwingUtilities.invokeLater(() -> ((AbstractButton) c).doClick());
				return "clicked " + c.getClass().getSimpleName();
			}
			try {
				Method m = c.getClass().getMethod("doActionClick");
				SwingUtilities.invokeLater(() -> {
					try {
						m.invoke(c);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				return "clicked " + c.getClass().getSimpleName();
			} catch (Exception ignore) {
			}
		}
		return "not found: " + text;
	}

	/** Indented component tree of all showing windows: class, text, bounds. */
	public static String tree() {
		StringBuilder sb = new StringBuilder();
		for (Window w : windows()) tree(w, 0, sb);
		return sb.toString();
	}

	static void tree(Component c, int depth, StringBuilder sb) {
		if (!c.isShowing()) return;
		String t = textOf(c);
		sb.append("  ".repeat(depth)).append(c.getClass().getName());
		if (t != null && !t.isEmpty()) sb.append(" \"").append(t.length() > 60 ? t.substring(0, 60) + "…" : t).append('"');
		sb.append(" [").append(c.getX()).append(',').append(c.getY()).append(' ').append(c.getWidth()).append('x').append(c.getHeight()).append("]\n");
		if (c instanceof Container)
			for (Component child : ((Container) c).getComponents()) tree(child, depth + 1, sb);
	}

	public static String snapshot() {
		File[] old = folder.listFiles((d, n) -> n.startsWith("window-") && n.endsWith(".png"));
		if (old != null) for (File f : old) f.delete();
		int n = 0;
		StringBuilder index = new StringBuilder();
		for (Window w : windows()) {
			if (w.getWidth() <= 0 || w.getHeight() <= 0) continue;
			BufferedImage img = new BufferedImage(w.getWidth() * 2, w.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.scale(2, 2);
			w.printAll(g);
			g.dispose();
			try {
				String name = "window-" + (n++) + ".png";
				ImageIO.write(img, "png", new File(folder, name));
				String title = (w instanceof Frame) ? ((Frame) w).getTitle() : (w instanceof Dialog) ? ((Dialog) w).getTitle() : "";
				index.append(name).append('\t').append(w.getClass().getName()).append('\t').append(title)
						.append('\t').append(w.getWidth()).append('x').append(w.getHeight()).append('\n');
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Files.writeString(new File(folder, "index.txt").toPath(), index.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return index.toString();
	}
}
