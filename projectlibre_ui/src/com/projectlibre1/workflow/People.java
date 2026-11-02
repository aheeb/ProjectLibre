package com.projectlibre1.workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.projectlibre1.configuration.Settings;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.resource.ResourcePool;

/**
 * Lets people be assigned by just typing their names: names are matched to the team ignoring case,
 * and names nobody has yet become new team members (resources) instead of an error.
 */
public class People {

	/** Names of the people (non-default resources) in {@code pool}, in pool order. */
	public static List<String> names(ResourcePool pool) {
		List<String> names = new ArrayList<String>();
		if (pool == null) return names;
		for (Iterator i = pool.getResourceList().iterator(); i.hasNext();) {
			Resource r = (Resource) i.next();
			if (isDefault(r) || r.getName() == null || r.getName().trim().length() == 0) continue;
			names.add(r.getName());
		}
		return names;
	}

	static boolean isDefault(Resource r) {
		return r instanceof ResourceImpl && ((ResourceImpl) r).isDefault();
	}

	static Resource find(ResourcePool pool, String name) {
		for (Iterator i = pool.getResourceList().iterator(); i.hasNext();) {
			Resource r = (Resource) i.next();
			if (!isDefault(r) && r.getName() != null && r.getName().trim().equalsIgnoreCase(name)) return r;
		}
		return null;
	}

	/** Splits "Anna, ben[50%]; Carl" into its elements, trimmed, empty ones dropped. */
	public static List<String> split(String text) {
		List<String> list = new ArrayList<String>();
		if (text == null) return list;
		for (String s : text.replace(';', ',').replace(Settings.LIST_SEPARATOR, ",").split(",")) {
			s = s.trim();
			if (s.length() > 0) list.add(s);
		}
		return list;
	}

	/**
	 * Rewrites {@code text} so every name matches the spelling of an existing team member, and adds
	 * a team member for every name not found.
	 * @param created receives the names of the people added
	 * @return the text to store in the Resource Names field
	 */
	public static String resolve(ResourcePool pool, String text, List<String> created) {
		if (pool == null || text == null) return text;
		StringBuilder sb = new StringBuilder();
		for (String element : split(text)) {
			String name = element, units = "";
			int bracket = element.indexOf(Settings.LEFT_BRACKET);
			if (bracket > 0) {
				name = element.substring(0, bracket).trim();
				units = element.substring(bracket);
			}
			Resource r = find(pool, name);
			if (r == null) {
				r = pool.createScriptedResource();
				r.setName(name);
				if (created != null) created.add(name);
			}
			if (sb.length() > 0) sb.append(Settings.LIST_SEPARATOR);
			sb.append(r.getName()).append(units);
		}
		return sb.toString();
	}
}
