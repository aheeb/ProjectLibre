package com.projectlibre1.pm.graphic.frames;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Sample projects bundled in the jar under /samples (see projectlibre_build/build.xml).
 * A sample is copied to a temporary file to be opened; the opened project forgets that path
 * so that saving asks where to put it instead of writing into the temporary directory.
 */
public class SampleProjects {
	public static final String[] NAMES = {
		"Commercial construction project plan",
		"New Product",
		"Microsoft Office Project 2003 deployment"
	};
	private static File dir;

	public static boolean isAvailable(String name) {
		return SampleProjects.class.getResource(resource(name)) != null;
	}

	/** @return the path of a temporary copy of the sample, or null if it isn't bundled */
	public static synchronized String extract(String name) throws IOException {
		InputStream in = SampleProjects.class.getResourceAsStream(resource(name));
		if (in == null) return null;
		try {
			if (dir == null) {
				dir = Files.createTempDirectory("projectlibre-samples").toFile();
				dir.deleteOnExit();
			}
			File file = new File(dir, name + ".pod");
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			file.deleteOnExit();
			return file.getPath();
		} finally {
			in.close();
		}
	}

	public static synchronized boolean isSample(String fileName) {
		return dir != null && fileName != null && new File(fileName).getAbsoluteFile().getParentFile().equals(dir.getAbsoluteFile());
	}

	private static String resource(String name) {
		return "/samples/" + name + ".pod";
	}
}
