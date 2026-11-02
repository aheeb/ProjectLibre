package com.projectlibre1.main;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Development-only threading checks, off unless -Dprojectlibre.devChecks=true.
 * <ul>
 * <li>EDT watchdog: any event that keeps the EDT busy for more than {@link #STALL_MS} ms is logged with
 * its duration and the EDT stack sampled while it was stuck.</li>
 * <li>Thread-checking RepaintManager: Swing components revalidated or repainted off the EDT are logged
 * with the offending stack, once per distinct stack.</li>
 * </ul>
 * Both write to System.err, prefixed with [EDT-STALL] / [EDT-VIOLATION] so they are easy to grep.
 */
public class DevChecks {
	public static final long STALL_MS = Long.getLong("projectlibre.devChecks.stallMs", 100);
	private static boolean installed;

	public static void installIfEnabled() {
		if (Boolean.getBoolean("projectlibre.devChecks")) install();
	}

	public static synchronized void install() {
		if (installed) return;
		installed = true;
		try {
			SwingUtilities.invokeAndWait(() -> {
				RepaintManager.setCurrentManager(new CheckingRepaintManager());
				Toolkit.getDefaultToolkit().getSystemEventQueue().push(new WatchedEventQueue());
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread t = new Thread(DevChecks::watch, "EDT watchdog");
		t.setDaemon(true);
		t.start();
		System.err.println("[DevChecks] EDT watchdog (" + STALL_MS + " ms) and thread-checking RepaintManager installed");
	}

	// ---- EDT watchdog ----

	private static volatile Thread edt;
	private static volatile long dispatchStart; // 0 when idle
	private static volatile long dispatchId;
	private static volatile String dispatchEvent;

	static class WatchedEventQueue extends EventQueue {
		private long nextId;
		private int depth;
		private boolean nested; // a modal loop ran inside the current event

		protected void dispatchEvent(AWTEvent event) {
			// A modal dialog dispatches nested events inside the event that opened it. The outer event is
			// not timed after that: its wall time includes the user looking at the dialog.
			if (depth > 0) nested = true;
			boolean outerNested = nested;
			String outerEvent = dispatchEvent;
			nested = false;
			depth++;
			edt = Thread.currentThread();
			dispatchEvent = describe(event);
			dispatchId = ++nextId;
			long start = System.nanoTime();
			dispatchStart = start;
			try {
				super.dispatchEvent(event);
			} finally {
				long ms = (System.nanoTime() - start) / 1000000;
				if (ms > STALL_MS && !nested && !isDevScript(event)) System.err.println("[EDT-STALL] " + ms + " ms in " + dispatchEvent);
				depth--;
				nested = outerNested || depth > 0;
				dispatchEvent = outerEvent;
				dispatchStart = 0;
			}
		}
	}

	/** dev/eval.sh scripts run on the EDT and include Groovy compilation; they are the test harness, not the app. */
	static boolean isDevScript(AWTEvent e) {
		return e.toString().contains("DevControl");
	}

	static String describe(AWTEvent e) {
		String s = e.toString();
		int on = s.indexOf(" on ");
		if (on > 0 && s.length() > on + 120) s = s.substring(0, on + 120) + "…";
		return s;
	}

	private static void watch() {
		long reportedId = -1;
		while (true) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				return;
			}
			long start = dispatchStart, id = dispatchId;
			Thread t = edt;
			if (start == 0 || t == null || id == reportedId || dispatchEvent.contains("DevControl")) continue;
			long ms = (System.nanoTime() - start) / 1000000;
			if (ms < STALL_MS) continue;
			StackTraceElement[] stack = t.getStackTrace();
			if (id != dispatchId) continue; // finished meanwhile
			if (isWaitingForEvents(stack)) continue; // idle inside a modal dialog's nested event loop
			reportedId = id;
			StringBuilder sb = new StringBuilder("[EDT-STALL] EDT busy > ").append(ms).append(" ms in ").append(dispatchEvent).append('\n');
			appendStack(sb, stack, 40);
			System.err.print(sb);
		}
	}

	static boolean isWaitingForEvents(StackTraceElement[] stack) {
		for (int i = 0; i < Math.min(stack.length, 12); i++)
			if (stack[i].getClassName().equals("java.awt.EventQueue") && stack[i].getMethodName().equals("getNextEvent")) return true;
		return false;
	}

	static void appendStack(StringBuilder sb, StackTraceElement[] stack, int max) {
		int n = 0;
		for (StackTraceElement f : stack) {
			if (n++ >= max) {
				sb.append("\t...\n");
				break;
			}
			sb.append("\tat ").append(f).append('\n');
		}
	}

	// ---- off-EDT Swing access ----

	static class CheckingRepaintManager extends RepaintManager {
		private final Set<String> seen = Collections.synchronizedSet(new HashSet<String>());

		public synchronized void addInvalidComponent(JComponent c) {
			check(c);
			super.addInvalidComponent(c);
		}

		public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
			check(c);
			super.addDirtyRegion(c, x, y, w, h);
		}

		private void check(JComponent c) {
			if (SwingUtilities.isEventDispatchThread()) return;
			StackTraceElement[] stack = new Throwable().getStackTrace();
			boolean fromRepaint = false;
			for (StackTraceElement f : stack) {
				String m = f.getMethodName();
				// repaint() is documented as thread-safe; only revalidate/state changes off the EDT are bugs
				if (m.equals("repaint") && f.getClassName().startsWith("java")) fromRepaint = true;
				if (m.equals("imageUpdate")) return;
			}
			if (fromRepaint && !hasStateChange(stack)) return;
			StringBuilder key = new StringBuilder();
			for (int i = 0; i < Math.min(stack.length, 25); i++) key.append(stack[i]).append('|');
			if (!seen.add(key.toString())) return;
			StringBuilder sb = new StringBuilder("[EDT-VIOLATION] ").append(c.getClass().getName()).append(" touched on thread ")
					.append(Thread.currentThread().getName()).append('\n');
			appendStack(sb, stack, 40);
			System.err.print(sb);
		}

		/** A repaint() reached from a setter (setText, setEnabled, ...) means component state changed off the EDT. */
		private static boolean hasStateChange(StackTraceElement[] stack) {
			for (StackTraceElement f : stack) {
				if (!f.getClassName().startsWith("javax.swing") && !f.getClassName().startsWith("java.awt")) return false;
				if (f.getMethodName().startsWith("set") || f.getMethodName().startsWith("fire")) return true;
			}
			return false;
		}
	}
}
