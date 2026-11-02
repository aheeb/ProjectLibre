package com.projectlibre1.pm.graphic.frames;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Most recently opened or saved project files, newest first, kept in the user preferences.
 */
public class RecentFiles {
	private static final int MAX = 8;
	private static final String KEY = "recent.";

	private static Preferences prefs() {
		return Preferences.userNodeForPackage(RecentFiles.class).node("recentFiles");
	}

	public static synchronized List<String> get() {
		Preferences p = prefs();
		List<String> files = new ArrayList<String>();
		for (int i = 0; i < MAX; i++) {
			String f = p.get(KEY + i, null);
			if (f != null && f.length() > 0) files.add(f);
		}
		return files;
	}

	public static synchronized void add(String fileName) {
		if (fileName == null || SampleProjects.isSample(fileName)) return;
		String path = new File(fileName).getAbsolutePath();
		List<String> files = get();
		files.remove(path);
		files.add(0, path);
		store(files);
	}

	public static synchronized void remove(String fileName) {
		List<String> files = get();
		if (files.remove(fileName)) store(files);
	}

	private static void store(List<String> files) {
		Preferences p = prefs();
		for (int i = 0; i < MAX; i++) {
			if (i < files.size()) p.put(KEY + i, files.get(i));
			else p.remove(KEY + i);
		}
	}
}
