package com.projectlibre1.workflow;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.projectlibre1.document.ObjectEvent;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.scheduling.ScheduleEvent;
import com.projectlibre1.pm.scheduling.ScheduleEventListener;
import com.projectlibre1.pm.task.Project;

/**
 * Answers "is anyone over 100%, and when?" from the plan's own numbers: a person is overallocated
 * on a day when their work that day is more than their working time at their availability
 * (Max. Units, their calendar with days off). Computed per day for the whole project, kept until
 * the schedule or the team changes.
 */
public class Workload {
	private static final long TOLERANCE = 60 * 1000L; // a minute: ignore rounding
	private static final Map<Project, Workload> instances = new WeakHashMap<Project, Workload>();

	private final WeakReference<Project> project;
	/** Start of each overallocated day, ascending, per person; null until computed. */
	private Map<Resource, long[]> overloadedDays;

	private Workload(Project project) {
		this.project = new WeakReference<Project>(project);
		Listener listener = new Listener();
		project.addScheduleListener(listener);
		project.addObjectListener(listener);
		if (project.getResourcePool() != null)
			project.getResourcePool().addObjectListener(listener);
	}

	public static Workload of(Project project) {
		Workload w = instances.get(project);
		if (w == null) {
			w = new Workload(project);
			instances.put(project, w);
		}
		return w;
	}

	private class Listener implements ScheduleEventListener, ObjectEvent.Listener {
		public void scheduleChanged(ScheduleEvent evt) {
			overloadedDays = null;
		}
		public void objectChanged(ObjectEvent objectEvent) {
			overloadedDays = null;
		}
	}

	/** The work {@code resource} can do between start and end: their working time there at their availability. */
	public static long capacity(Resource resource, Project project, long start, long end) {
		WorkCalendar calendar = resource.getEffectiveWorkCalendar();
		if (calendar == null && project != null)
			calendar = project.getEffectiveWorkCalendar();
		if (calendar == null || end <= start)
			return 0;
		return (long) (resource.getMaximumUnits() * calendar.compare(end, start, false));
	}

	private static boolean counts(Resource resource) {
		return resource != null && resource.isLabor() && resource.getMaximumUnits() > 0;
	}

	private synchronized Map<Resource, long[]> days() {
		if (overloadedDays != null)
			return overloadedDays;
		Map<Resource, long[]> result = new HashMap<Resource, long[]>();
		Project p = project.get();
		if (p != null && p.getResourcePool() != null && p.getEnd() > p.getStart()) {
			for (Iterator i = p.getResourcePool().getResourceList().iterator(); i.hasNext();) {
				Resource r = (Resource) i.next();
				if (!counts(r))
					continue;
				List<Long> days = new ArrayList<Long>();
				Calendar day = Calendar.getInstance();
				day.setTimeInMillis(p.getStart());
				day.set(Calendar.HOUR_OF_DAY, 0);
				day.set(Calendar.MINUTE, 0);
				day.set(Calendar.SECOND, 0);
				day.set(Calendar.MILLISECOND, 0);
				while (day.getTimeInMillis() < p.getEnd()) {
					long start = day.getTimeInMillis();
					day.add(Calendar.DATE, 1);
					long end = day.getTimeInMillis();
					if (r.work(start, end) > capacity(r, p, start, end) + TOLERANCE)
						days.add(start);
				}
				long[] array = new long[days.size()];
				for (int k = 0; k < array.length; k++)
					array[k] = days.get(k);
				result.put(r, array);
			}
		}
		overloadedDays = result;
		return result;
	}

	/** Number of days on which {@code resource} has more work than they are available for. */
	public int overloadedDayCount(Resource resource) {
		long[] days = days().get(resource);
		return days == null ? 0 : days.length;
	}

	/** The first day {@code resource} has more work than they are available for, or -1. */
	public long firstOverloadedDay(Resource resource) {
		long[] days = days().get(resource);
		return days == null || days.length == 0 ? -1 : days[0];
	}

	public boolean isOverallocated(Resource resource) {
		return overloadedDayCount(resource) > 0;
	}

	/**
	 * True if {@code resource} is overallocated on a day that lies mostly in [start, end). Mostly:
	 * timescale periods start at midnight UTC, so they overlap neighbouring local days by an hour or two.
	 */
	public boolean isOverallocated(Resource resource, long start, long end) {
		long[] days = days().get(resource);
		if (days == null || days.length == 0)
			return false;
		int i = Arrays.binarySearch(days, start - WorkCalendar.MILLIS_IN_DAY);
		for (i = i >= 0 ? i : -i - 1; i < days.length && days[i] < end; i++) {
			long overlap = Math.min(days[i] + WorkCalendar.MILLIS_IN_DAY, end) - Math.max(days[i], start);
			if (overlap * 2 > Math.min(WorkCalendar.MILLIS_IN_DAY, end - start))
				return true;
		}
		return false;
	}

	/** The people who are overallocated on some day, in team order. */
	public List<Resource> overallocatedPeople() {
		List<Resource> people = new ArrayList<Resource>();
		Project p = project.get();
		if (p == null || p.getResourcePool() == null)
			return people;
		for (Iterator i = p.getResourcePool().getResourceList().iterator(); i.hasNext();) {
			Resource r = (Resource) i.next();
			if (isOverallocated(r))
				people.add(r);
		}
		return people;
	}
}
