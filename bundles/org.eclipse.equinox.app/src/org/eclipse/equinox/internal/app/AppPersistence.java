/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import java.io.*;
import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.application.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Manages all persistent data for ApplicationDescriptors (lock status, 
 * scheduled applications etc.)
 */
public class AppPersistence implements ServiceTrackerCustomizer {
	private static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$

	private static final String FILTER_PREFIX = "(&(objectClass=org.eclipse.osgi.service.datalocation.Location)(type="; //$NON-NLS-1$
	private static final String FILE_APPLOCKS = ".locks"; //$NON-NLS-1$
	private static final String FILE_APPSCHEDULED = ".scheduled"; //$NON-NLS-1$
	private static final String EVENT_HANDLER = "org.osgi.service.event.EventHandler"; //$NON-NLS-1$

	private static final int DATA_VERSION = 2;
	private static final byte NULL = 0;
	private static final int OBJECT = 1;

	private static BundleContext context;
	private static ServiceTracker configTracker;
	private static Location configLocation;
	private static Collection locks = new ArrayList();
	private static Map scheduledApps = new HashMap();
	static ArrayList timerApps = new ArrayList();
	private static StorageManager storageManager;
	private static boolean scheduling = false;
	static boolean shutdown = false;
	private static int nextScheduledID = 1;
	private static Thread timerThread;

	static void start(BundleContext bc) {
		context = bc;
		shutdown = false;
		initConfiguration();
	}

	static void stop() {
		shutdown = true;
		stopTimer();
		if (storageManager != null) {
			storageManager.close();
			storageManager = null;
		}
		closeConfiguration();
		context = null;
	}

	private static void initConfiguration() {
		closeConfiguration(); // just incase
		Filter filter = null;
		try {
			filter = context.createFilter(FILTER_PREFIX + PROP_CONFIG_AREA + "))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		configTracker = new ServiceTracker(context, filter, new AppPersistence());
		configTracker.open();
	}

	private static void closeConfiguration() {
		if (configTracker != null)
			configTracker.close();
		configTracker = null;
	}

	/**
	 * Used by {@link ApplicationDescriptor} to determine if an application is locked.
	 * @param desc the application descriptor
	 * @return true if the application is persistently locked.
	 */
	public static boolean isLocked(ApplicationDescriptor desc) {
		synchronized (locks) {
			return locks.contains(desc.getApplicationId());
		}
	}

	/**
	 * Used by {@link ApplicationDescriptor} to determine lock and unlock and application.
	 * @param desc the application descriptor
	 * @param locked the locked flag
	 */
	public static void saveLock(ApplicationDescriptor desc, boolean locked) {
		synchronized (locks) {
			if (locked) {
				if (!locks.contains(desc.getApplicationId())) {
					locks.add(desc.getApplicationId());
					saveData(FILE_APPLOCKS);
				}
			} else if (locks.remove(desc.getApplicationId())) {
				saveData(FILE_APPLOCKS);
			}
		}
	}

	static void removeScheduledApp(EclipseScheduledApplication scheduledApp) {
		boolean removed;
		synchronized (scheduledApps) {
			removed = scheduledApps.remove(scheduledApp.getScheduleId()) != null;
			if (removed) {
				saveData(FILE_APPSCHEDULED);
			}
		}
		if (removed)
			synchronized (timerApps) {
				timerApps.remove(scheduledApp);
			}
	}

	/**
	 * Used by {@link ScheduledApplication} to persistently schedule an application launch
	 * @param descriptor
	 * @param arguments
	 * @param topic
	 * @param eventFilter
	 * @param recurring
	 * @return the scheduled application
	 * @throws InvalidSyntaxException
	 * @throws ApplicationException 
	 */
	public static ScheduledApplication addScheduledApp(ApplicationDescriptor descriptor, String scheduleId, Map arguments, String topic, String eventFilter, boolean recurring) throws InvalidSyntaxException, ApplicationException {
		if (!scheduling && !checkSchedulingSupport())
			throw new ApplicationException(ApplicationException.APPLICATION_SCHEDULING_FAILED, "Cannot support scheduling without org.osgi.service.event package"); //$NON-NLS-1$
		// check the event filter for correct syntax
		context.createFilter(eventFilter);
		EclipseScheduledApplication result;
		synchronized (scheduledApps) {
			result = new EclipseScheduledApplication(context, getNextScheduledID(scheduleId), descriptor.getApplicationId(), arguments, topic, eventFilter, recurring);
			addScheduledApp(result);
			saveData(FILE_APPSCHEDULED);
		}
		return result;
	}

	// must call this method while holding the scheduledApps lock
	private static void addScheduledApp(EclipseScheduledApplication scheduledApp) {
		if (ScheduledApplication.TIMER_TOPIC.equals(scheduledApp.getTopic())) {
			synchronized (timerApps) {
				timerApps.add(scheduledApp);
				if (timerThread == null)
					startTimer();
			}
		}
		scheduledApps.put(scheduledApp.getScheduleId(), scheduledApp);
		Hashtable serviceProps = new Hashtable();
		if (scheduledApp.getTopic() != null)
			serviceProps.put(EventConstants.EVENT_TOPIC, new String[] {scheduledApp.getTopic()});
		if (scheduledApp.getEventFilter() != null)
			serviceProps.put(EventConstants.EVENT_FILTER, scheduledApp.getEventFilter());
		serviceProps.put(ScheduledApplication.SCHEDULE_ID, scheduledApp.getScheduleId());
		serviceProps.put(ScheduledApplication.APPLICATION_PID, scheduledApp.getAppPid());
		ServiceRegistration sr = context.registerService(new String[] {ScheduledApplication.class.getName(), EVENT_HANDLER}, scheduledApp, serviceProps);
		scheduledApp.setServiceRegistration(sr);
	}

	private static String getNextScheduledID(String scheduledId) throws ApplicationException {
		if (scheduledId != null) {
			if (scheduledApps.get(scheduledId) != null)
				throw new ApplicationException(ApplicationException.APPLICATION_DUPLICATE_SCHEDULE_ID, "Duplicate scheduled ID: " + scheduledId); //$NON-NLS-1$
			return scheduledId;
		}
		if (nextScheduledID == Integer.MAX_VALUE)
			nextScheduledID = 0;
		String result = new Integer(nextScheduledID++).toString();
		while (scheduledApps.get(result) != null && nextScheduledID < Integer.MAX_VALUE)
			result = new Integer(nextScheduledID++).toString();
		if (nextScheduledID == Integer.MAX_VALUE)
			throw new ApplicationException(ApplicationException.APPLICATION_DUPLICATE_SCHEDULE_ID, "Maximum number of scheduled applications reached"); //$NON-NLS-1$
		return result;
	}

	private static boolean checkSchedulingSupport() {
		// cannot support scheduling without the event admin package
		try {
			Class.forName(EVENT_HANDLER);
			scheduling = true;
			return true;
		} catch (ClassNotFoundException e) {
			scheduling = false;
			return false;
		}
	}

	private synchronized static boolean loadData(String fileName) {
		try {
			Location location = configLocation;
			if (location == null)
				return false;
			File theStorageDir = new File(location.getURL().getPath() + '/' + Activator.PI_APP);
			if (storageManager == null) {
				boolean readOnly = location.isReadOnly();
				storageManager = new StorageManager(theStorageDir, readOnly ? "none" : null, readOnly); //$NON-NLS-1$
				storageManager.open(!readOnly);
			}
			File dataFile = storageManager.lookup(fileName, false);
			if (dataFile == null || !dataFile.isFile()) {
				Location parent = location.getParentLocation();
				if (parent != null) {
					theStorageDir = new File(parent.getURL().getPath() + '/' + Activator.PI_APP);
					StorageManager tmp = new StorageManager(theStorageDir, "none", true); //$NON-NLS-1$
					tmp.open(false);
					dataFile = tmp.lookup(fileName, false);
					tmp.close();
				}
			}
			if (dataFile == null || !dataFile.isFile())
				return true;
			if (FILE_APPLOCKS.equals(fileName))
				loadLocks(dataFile);
			else if (FILE_APPSCHEDULED.equals(fileName))
				loadSchedules(dataFile);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private static void loadLocks(File locksData) throws IOException {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(locksData));
			int dataVersion = in.readInt();
			if (dataVersion != DATA_VERSION)
				return;
			int numLocks = in.readInt();
			synchronized (locks) {
				for (int i = 0; i < numLocks; i++)
					locks.add(in.readUTF());
			}
		} finally {
			if (in != null)
				in.close();
		}
	}

	private static void loadSchedules(File schedulesData) throws IOException {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(schedulesData));
			int dataVersion = in.readInt();
			if (dataVersion != DATA_VERSION)
				return;
			int numScheds = in.readInt();
			for (int i = 0; i < numScheds; i++) {
				String id = readString(in, false);
				String appPid = readString(in, false);
				String topic = readString(in, false);
				String eventFilter = readString(in, false);
				boolean recurring = in.readBoolean();
				Map args = (Map) in.readObject();
				EclipseScheduledApplication schedApp = new EclipseScheduledApplication(context, id, appPid, args, topic, eventFilter, recurring);
				addScheduledApp(schedApp);
			}
		} catch (InvalidSyntaxException e) {
			throw new IOException(e.getMessage());
		} catch (NoClassDefFoundError e) {
			throw new IOException(e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		} finally {
			if (in != null)
				in.close();
		}
	}

	private synchronized static void saveData(String fileName) {
		if (storageManager == null || storageManager.isReadOnly())
			return;
		try {
			File data = storageManager.createTempFile(fileName);
			if (FILE_APPLOCKS.equals(fileName))
				saveLocks(data);
			else if (FILE_APPSCHEDULED.equals(fileName))
				saveSchedules(data);
			storageManager.lookup(fileName, true);
			storageManager.update(new String[] {fileName}, new String[] {data.getName()});
		} catch (IOException e) {
			Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.ERROR, 0, NLS.bind(Messages.persistence_error_saving, fileName), 0, e, null));
		}
	}

	// must call this while holding the locks lock
	private static void saveLocks(File locksData) throws IOException {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(locksData));
			out.writeInt(DATA_VERSION);
			out.writeInt(locks.size());
			for (Iterator iterLocks = locks.iterator(); iterLocks.hasNext();)
				out.writeUTF((String) iterLocks.next());
		} finally {
			if (out != null)
				out.close();
		}
	}

	// must call this while holding the scheduledApps lock
	private static void saveSchedules(File schedulesData) throws IOException {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(schedulesData));
			out.writeInt(DATA_VERSION);
			out.writeInt(scheduledApps.size());
			for (Iterator apps = scheduledApps.values().iterator(); apps.hasNext();) {
				EclipseScheduledApplication app = (EclipseScheduledApplication) apps.next();
				writeStringOrNull(out, app.getScheduleId());
				writeStringOrNull(out, app.getAppPid());
				writeStringOrNull(out, app.getTopic());
				writeStringOrNull(out, app.getEventFilter());
				out.writeBoolean(app.isRecurring());
				out.writeObject(app.getArguments());
			}
		} finally {
			if (out != null)
				out.close();
		}
	}

	private static void startTimer() {
		timerThread = new Thread(new AppTimer(), "app schedule timer"); //$NON-NLS-1$
		timerThread.start();
	}

	private static void stopTimer() {
		if (timerThread != null)
			timerThread.interrupt();
		timerThread = null;
	}

	static class AppTimer implements Runnable {
		public void run() {
			int lastMin = -1;
			while (!shutdown) {
				try {
					Thread.sleep(30000); // sleeping 30 secs instead of 60 to try to avoid skipping minutes
					Calendar cal = Calendar.getInstance();
					int minute = cal.get(Calendar.MINUTE);
					if (minute == lastMin)
						continue;
					lastMin = minute;
					Hashtable props = new Hashtable();
					props.put(ScheduledApplication.YEAR, new Integer(cal.get(Calendar.YEAR)));
					props.put(ScheduledApplication.MONTH, new Integer(cal.get(Calendar.MONTH)));
					props.put(ScheduledApplication.DAY_OF_MONTH, new Integer(cal.get(Calendar.DAY_OF_MONTH)));
					props.put(ScheduledApplication.DAY_OF_WEEK, new Integer(cal.get(Calendar.DAY_OF_WEEK)));
					props.put(ScheduledApplication.HOUR_OF_DAY, new Integer(cal.get(Calendar.HOUR_OF_DAY)));
					props.put(ScheduledApplication.MINUTE, new Integer(minute));
					Event timerEvent = new Event(ScheduledApplication.TIMER_TOPIC, (Dictionary) props);
					EclipseScheduledApplication[] apps = null;
					// poor mans implementation of dispatching events; the spec will not allow us to use event admin to dispatch the virtual timer events; boo!!
					synchronized (timerApps) {
						if (timerApps.size() == 0)
							continue;
						apps = (EclipseScheduledApplication[]) timerApps.toArray(new EclipseScheduledApplication[timerApps.size()]);
					}
					for (int i = 0; i < apps.length; i++) {
						try {
							String filterString = apps[i].getEventFilter();
							Filter filter = filterString == null ? null : FrameworkUtil.createFilter(filterString);
							if (filter == null || filter.match(props))
								apps[i].handleEvent(timerEvent);
						} catch (Throwable t) {
							String message = NLS.bind(Messages.scheduled_app_launch_error, apps[i].getAppPid());
							Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, t, null));
						}
					}
				} catch (InterruptedException e) {
					// do nothing;
				}
			}
		}
	}

	private static String readString(ObjectInputStream in, boolean intern) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		return intern ? in.readUTF().intern() : in.readUTF();
	}

	private static void writeStringOrNull(ObjectOutputStream out, String string) throws IOException {
		if (string == null)
			out.writeByte(NULL);
		else {
			out.writeByte(OBJECT);
			out.writeUTF(string);
		}
	}

	public Object addingService(ServiceReference reference) {
		if (configLocation != null)
			return null; // only care about one configuration
		configLocation = (Location) context.getService(reference);
		loadData(FILE_APPLOCKS);
		loadData(FILE_APPSCHEDULED);
		return configLocation;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// don't care
	}

	public void removedService(ServiceReference reference, Object service) {
		if (service == configLocation)
			configLocation = null;
	}
}
