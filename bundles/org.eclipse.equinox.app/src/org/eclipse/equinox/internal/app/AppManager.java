/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ScheduledApplication;
import org.osgi.service.event.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Managers all persistent data for ApplicationDescriptors (lock status, 
 * scheduled applications etc.)
 */
public class AppManager {
	private static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$

	private static final String FILTER_PREFIX = "(&(objectClass=org.eclipse.osgi.service.datalocation.Location)(type="; //$NON-NLS-1$
	private static final String FILE_APPLOCKS = ".locks"; //$NON-NLS-1$
	private static final String FILE_APPSCHEDULED = ".scheduled"; //$NON-NLS-1$
	private static final String EVENT_HANDLER = "org.osgi.service.event.EventHandler"; //$NON-NLS-1$
	private static final String EVENT_TIMER_TOPIC = "org/osgi/application/timer"; //$NON-NLS-1$

	private static final int DATA_VERSION = 1;
	private static final byte NULL = 0;
	private static final int OBJECT = 1;

	// obsolete command line args
	private static final String NO_PACKAGE_PREFIXES = "-noPackagePrefixes"; //$NON-NLS-1$
	private static final String CLASSLOADER_PROPERTIES = "-classloaderProperties"; //$NON-NLS-1$	
	private static final String PLUGINS = "-plugins"; //$NON-NLS-1$
	private static final String FIRST_USE = "-firstUse"; //$NON-NLS-1$
	private static final String NO_UPDATE = "-noUpdate"; //$NON-NLS-1$
	private static final String NEW_UPDATES = "-newUpdates"; //$NON-NLS-1$
	private static final String UPDATE = "-update"; //$NON-NLS-1$
	private static final String BOOT = "-boot"; //$NON-NLS-1$

	// command line args not used by app container
	private static final String KEYRING = "-keyring"; //$NON-NLS-1$
	private static final String PASSWORD = "-password"; //$NON-NLS-1$

	// command line args used by app container
	private static final String PRODUCT = "-product"; //$NON-NLS-1$
	private static final String FEATURE = "-feature"; //$NON-NLS-1$
	private static final String APPLICATION = "-application"; //$NON-NLS-1$	

	private static BundleContext context;
	private static ServiceTracker configuration;
	private static ServiceTracker pkgAdminTracker;
	private static Collection locks = new ArrayList();
	private static Map scheduledApps = new HashMap();
	static ArrayList timerApps = new ArrayList();
	private static StorageManager storageManager;
	private static boolean dirty;
	private static boolean scheduling = false;
	static boolean shutdown = false;
	private static int nextScheduledID = 1;
	private static Thread timerThread;
	private static String[] appArgs;

	static synchronized void setBundleContext(BundleContext context) {
		if (context != null) {
			AppManager.context = context;
			init();
		} else {
			shutdown();
			AppManager.context = context;
		}
	}

	private static void init() {
		shutdown = false;
		appArgs = processCommandLine();
		initPackageAdmin();
		initConfiguration();
		loadData(FILE_APPLOCKS);
		loadData(FILE_APPSCHEDULED);
	}

	private static void shutdown() {
		shutdown = true;
		stopTimer();
		saveData();
		if (storageManager != null) {
			storageManager.close();
			storageManager = null;
		}
		closeConfiguration();
		closePackageAdmin();
		appArgs = null;
	}

	private static void initPackageAdmin() {
		closePackageAdmin(); // just incase
		pkgAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		pkgAdminTracker.open();
	}

	private static void closePackageAdmin() {
		if (pkgAdminTracker != null)
			pkgAdminTracker.close();
		pkgAdminTracker = null;
	}

	private static void initConfiguration() {
		closeConfiguration(); // just incase
		Filter filter = null;
		try {
			filter = context.createFilter(FILTER_PREFIX + PROP_CONFIG_AREA + "))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		configuration = new ServiceTracker(context, filter, null);
		configuration.open();
	}

	private static void closeConfiguration() {
		if (configuration != null)
			configuration.close();
		configuration = null;
	}

	/**
	 * Used by {@link ApplicationDescriptor} to determine if an application is locked.
	 * @param desc the application descriptor
	 * @return true if the application is persistently locked.
	 */
	public synchronized static boolean isLocked(ApplicationDescriptor desc) {
		return locks.contains(desc.getApplicationId());
	}

	/**
	 * Used by {@link ApplicationDescriptor} to determine lock and unlock and application.
	 * @param desc the application descriptor
	 * @param locked the locked flag
	 */
	public synchronized static void saveLock(ApplicationDescriptor desc, boolean locked) {
		if (locked) {
			if (!locks.contains(desc.getApplicationId())) {
				dirty = true;
				locks.add(desc.getApplicationId());
			}
		} else if (locks.remove(desc.getApplicationId())) {
			dirty = true;
		}
	}

	synchronized static void removeScheduledApp(EclipseScheduledApplication scheduledApp) {
		if (scheduledApps.remove(scheduledApp.getID()) != null) {
			timerApps.remove(scheduledApp);
			dirty = true;
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
	 */
	public synchronized static ScheduledApplication addScheduledApp(ApplicationDescriptor descriptor, Map arguments, String topic, String eventFilter, boolean recurring) throws InvalidSyntaxException {
		if (!scheduling && !checkSchedulingSupport())
			throw new UnsupportedOperationException("Cannot support scheduling without org.osgi.service.event package"); //$NON-NLS-1$
		// check the event filter for correct syntax
		context.createFilter(eventFilter);
		EclipseScheduledApplication result = new EclipseScheduledApplication(context, getNextScheduledID(), descriptor.getApplicationId(), arguments, topic, eventFilter, recurring);
		addScheduledApp(result);
		dirty = true;
		return result;
	}

	static void addScheduledApp(EclipseScheduledApplication scheduledApp) {
		if (EVENT_TIMER_TOPIC.equals(scheduledApp.getTopic())) {
			timerApps.add(scheduledApp);
			if (timerThread == null)
				startTimer();
		}
		scheduledApps.put(scheduledApp.getID(), scheduledApp);
		Hashtable serviceProps = new Hashtable();
		if (scheduledApp.getTopic() != null)
			serviceProps.put(EventConstants.EVENT_TOPIC, new String[] {scheduledApp.getTopic()});
		if (scheduledApp.getEventFilter() != null)
			serviceProps.put(EventConstants.EVENT_FILTER, scheduledApp.getEventFilter());
		ServiceRegistration sr = context.registerService(new String[] {ScheduledApplication.class.getName(), EVENT_HANDLER}, scheduledApp, serviceProps);
		scheduledApp.setServiceRegistration(sr);
	}

	private static Integer getNextScheduledID() {
		if (nextScheduledID == Integer.MAX_VALUE)
			nextScheduledID = 0;
		Integer result = new Integer(nextScheduledID++);
		while (scheduledApps.get(result) != null && nextScheduledID < Integer.MAX_VALUE)
			result = new Integer(nextScheduledID++);
		if (nextScheduledID == Integer.MAX_VALUE)
			throw new IllegalStateException("Maximum number of scheduled applications reached"); //$NON-NLS-1$
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
			Location location = (Location) configuration.getService();
			if (location == null)
				return false;
			File theStorageDir = new File(location.getURL().getPath() + '/' + Activator.PI_APP);
			boolean readOnly = location.isReadOnly();
			storageManager = new StorageManager(theStorageDir, readOnly ? "none" : null, readOnly); //$NON-NLS-1$
			storageManager.open(!readOnly);
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
			if (fileName.equals(FILE_APPLOCKS))
				loadLocks(dataFile);
			else if (fileName.equals(FILE_APPSCHEDULED))
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
			for (int i = 0; i < numLocks; i++)
				locks.add(in.readUTF());
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
				Integer id = new Integer(in.readInt());
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

	private synchronized static void saveData() {
		if (!dirty || storageManager.isReadOnly())
			return;
		try {
			File locksData = storageManager.createTempFile(FILE_APPLOCKS);
			saveLocks(locksData);
			File schedulesData = storageManager.createTempFile(FILE_APPSCHEDULED);
			saveSchedules(schedulesData);
			storageManager.lookup(FILE_APPLOCKS, true);
			storageManager.lookup(FILE_APPSCHEDULED, true);
			storageManager.update(new String[] {FILE_APPLOCKS, FILE_APPSCHEDULED}, new String[] {locksData.getName(), schedulesData.getName()});
		} catch (IOException e) {
			// TODO should log this!!
		}
		dirty = false;
	}

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

	private static void saveSchedules(File schedulesData) throws IOException {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(schedulesData));
			out.writeInt(DATA_VERSION);
			out.writeInt(scheduledApps.size());
			for (Iterator apps = scheduledApps.values().iterator(); apps.hasNext();) {
				EclipseScheduledApplication app = (EclipseScheduledApplication) apps.next();
				out.writeInt(app.getID().intValue());
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

	private static class AppTimer implements Runnable {
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
					props.put("year", new Integer(cal.get(Calendar.YEAR))); //$NON-NLS-1$
					props.put("month", new Integer(cal.get(Calendar.MONTH))); //$NON-NLS-1$
					props.put("day_of_month", new Integer(cal.get(Calendar.DAY_OF_MONTH))); //$NON-NLS-1$
					props.put("day_of_week", new Integer(cal.get(Calendar.DAY_OF_WEEK))); //$NON-NLS-1$
					props.put("hour_of_day", new Integer(cal.get(Calendar.HOUR_OF_DAY))); //$NON-NLS-1$
					props.put("minute", new Integer(minute)); //$NON-NLS-1$
					Event timerEvent = new Event(EVENT_TIMER_TOPIC, props);
					synchronized (AppManager.class) {
						// poor mans implementation of dispatching events; the spec will not allow us to use event admin to dispatch the virtual timer events; boo!!
						if (timerApps.size() == 0)
							continue;
						EclipseScheduledApplication[] apps = (EclipseScheduledApplication[]) timerApps.toArray(new EclipseScheduledApplication[timerApps.size()]);
						for (int i = 0; i < apps.length; i++) {
							try {
								String filterString = apps[i].getEventFilter();
								Filter filter = filterString == null ? null : FrameworkUtil.createFilter(filterString);
								if (filter == null || filter.match(props))
									apps[i].handleEvent(timerEvent);
							} catch (Throwable t) {
								t.printStackTrace();
								// TODO should log this
							}
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

	public static Bundle getBundle(String symbolicName) {
		PackageAdmin packageAdmin = (PackageAdmin) getService(pkgAdminTracker);
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++)
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				return bundles[i];
		return null;
	}

	/**
	 * Returns the default application args that should be used to launch an application 
	 * when args are not supplied.
	 * @return the default application args.
	 */
	public static String[] getApplicationArgs() {
		return appArgs;
	}

	private static String[] processCommandLine() {
		ServiceReference infoRef = context.getServiceReference(EnvironmentInfo.class.getName());
		if (infoRef == null)
			return null;
		EnvironmentInfo envInfo = (EnvironmentInfo) context.getService(infoRef);
		if (envInfo == null)
			return null;
		String[] args = envInfo.getNonFrameworkArgs();
		context.ungetService(infoRef);
		if (args == null)
			return args;
		if (args.length == 0)
			return args;

		int[] configArgs = new int[args.length];
		//need to initialize the first element to something that could not be an index.
		configArgs[0] = -1;
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)

			// consume obsolete args
			if (args[i].equalsIgnoreCase(CLASSLOADER_PROPERTIES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NO_PACKAGE_PREFIXES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(PLUGINS))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(FIRST_USE))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NO_UPDATE))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NEW_UPDATES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(UPDATE))
				found = true; // ignored

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}
			// check for args with parameters
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			// consume args not used by app container
			if (args[i - 1].equalsIgnoreCase(KEYRING))
				found = true;
			if (args[i - 1].equalsIgnoreCase(PASSWORD))
				found = true;

			// consume obsolete args for compatibilty
			if (args[i - 1].equalsIgnoreCase(CLASSLOADER_PROPERTIES))
				found = true; // ignore
			if (args[i - 1].equalsIgnoreCase(BOOT))
				found = true; // ignore

			// look for the product to run
			// treat -feature as a synonym for -product for compatibility.
			if (args[i - 1].equalsIgnoreCase(PRODUCT) || args[i - 1].equalsIgnoreCase(FEATURE)) {
				// use the long way to set the property to compile against eeminimum
				System.getProperties().setProperty(ContainerManager.PROP_PRODUCT, arg);
				found = true;
			}

			// look for the application to run.  
			if (args[i - 1].equalsIgnoreCase(APPLICATION)) {
				// use the long way to set the property to compile against eeminimum
				System.getProperties().setProperty(ContainerManager.PROP_ECLIPSE_APPLICATION, arg);
				found = true;
			}

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0) {
			appArgs = args;
			return args;
		}
		appArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int j = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex])
				configArgIndex++;
			else
				appArgs[j++] = args[i];
		}
		return appArgs;
	}

	static void openTracker(final ServiceTracker tracker, final boolean allServices) {
		if (System.getSecurityManager() == null)
			tracker.open(allServices);
		else
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					tracker.open(allServices);
					return null;
				}
			});
	}

	static Object getService(final ServiceTracker tracker) {
		if (System.getSecurityManager() == null)
			return tracker.getService();
		return AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return tracker.getService();
			}
		});
	}

	static String getLocation(final Bundle bundle) {
		if (System.getSecurityManager() == null)
			return bundle.getLocation();
		return (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return bundle.getLocation();
			}
		});
	}

	static BundleContext getContext() {
		return context;
	}
}
