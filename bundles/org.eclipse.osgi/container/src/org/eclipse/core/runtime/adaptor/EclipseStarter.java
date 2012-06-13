/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt (bug 172969)
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.BaseStorageHook;
import org.eclipse.osgi.internal.profile.Profile;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Special startup class for the Eclipse Platform. This class cannot be 
 * instantiated; all functionality is provided by static methods. 
 * <p>
 * The Eclipse Platform makes heavy use of Java class loaders for loading 
 * plug-ins. Even the Eclipse Runtime itself and the OSGi framework need
 * to be loaded by special class loaders. The upshot is that a 
 * client program (such as a Java main program, a servlet) cannot  
 * reference any part of Eclipse directly. Instead, a client must use this 
 * loader class to start the platform, invoking functionality defined 
 * in plug-ins, and shutting down the platform when done. 
 * </p>
 * <p>Note that the fields on this class are not API. </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EclipseStarter {
	private static FrameworkAdaptor adaptor;
	private static BundleContext context;
	private static boolean initialize = false;
	public static boolean debug = false;
	private static boolean running = false;
	private static Framework framework = null;
	private static ServiceRegistration<?> defaultMonitorRegistration = null;
	private static ServiceRegistration<?> appLauncherRegistration = null;
	private static ServiceRegistration<?> splashStreamRegistration = null;

	// command line arguments
	private static final String CLEAN = "-clean"; //$NON-NLS-1$
	private static final String CONSOLE = "-console"; //$NON-NLS-1$
	private static final String CONSOLE_LOG = "-consoleLog"; //$NON-NLS-1$
	private static final String DEBUG = "-debug"; //$NON-NLS-1$
	private static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
	private static final String DEV = "-dev"; //$NON-NLS-1$
	private static final String WS = "-ws"; //$NON-NLS-1$
	private static final String OS = "-os"; //$NON-NLS-1$
	private static final String ARCH = "-arch"; //$NON-NLS-1$
	private static final String NL = "-nl"; //$NON-NLS-1$
	private static final String NL_EXTENSIONS = "-nlExtensions"; //$NON-NLS-1$
	private static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$	
	private static final String USER = "-user"; //$NON-NLS-1$
	private static final String NOEXIT = "-noExit"; //$NON-NLS-1$
	private static final String LAUNCHER = "-launcher"; //$NON-NLS-1$

	// this is more of an Eclipse argument but this OSGi implementation stores its 
	// metadata alongside Eclipse's.
	private static final String DATA = "-data"; //$NON-NLS-1$

	// System properties
	public static final String PROP_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	public static final String PROP_BUNDLES_STARTLEVEL = "osgi.bundles.defaultStartLevel"; //$NON-NLS-1$ //The start level used to install the bundles
	public static final String PROP_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
	public static final String PROP_INITIAL_STARTLEVEL = "osgi.startLevel"; //$NON-NLS-1$ //The start level when the fwl start
	public static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$
	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_CLASS = "osgi.consoleClass"; //$NON-NLS-1$
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	private static final String PROP_NL_EXTENSIONS = "osgi.nl.extensions"; //$NON-NLS-1$
	public static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String PROP_ADAPTOR = "osgi.adaptor"; //$NON-NLS-1$
	public static final String PROP_SYSPATH = "osgi.syspath"; //$NON-NLS-1$
	public static final String PROP_LOGFILE = "osgi.logfile"; //$NON-NLS-1$
	public static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	public static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	public static final String PROP_FRAMEWORK_SHAPE = "osgi.framework.shape"; //$NON-NLS-1$ //the shape of the fwk (jar, or folder)
	public static final String PROP_NOSHUTDOWN = "osgi.noShutdown"; //$NON-NLS-1$
	private static final String PROP_FORCED_RESTART = "osgi.forcedRestart"; //$NON-NLS-1$

	public static final String PROP_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	public static final String PROP_EXITDATA = "eclipse.exitdata"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_LOG = "eclipse.consoleLog"; //$NON-NLS-1$
	public static final String PROP_IGNOREAPP = "eclipse.ignoreApp"; //$NON-NLS-1$
	public static final String PROP_REFRESH_BUNDLES = "eclipse.refreshBundles"; //$NON-NLS-1$
	private static final String PROP_ALLOW_APPRELAUNCH = "eclipse.allowAppRelaunch"; //$NON-NLS-1$
	private static final String PROP_APPLICATION_LAUNCHDEFAULT = "eclipse.application.launchDefault"; //$NON-NLS-1$

	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$
	private static final String REFERENCE_SCHEME = "reference:"; //$NON-NLS-1$
	private static final String REFERENCE_PROTOCOL = "reference"; //$NON-NLS-1$
	private static final String INITIAL_LOCATION = "initial@"; //$NON-NLS-1$
	/** string containing the classname of the adaptor to be used in this framework instance */
	protected static final String DEFAULT_ADAPTOR_CLASS = "org.eclipse.osgi.baseadaptor.BaseAdaptor"; //$NON-NLS-1$

	private static final int DEFAULT_INITIAL_STARTLEVEL = 6; // default value for legacy purposes
	private static final String DEFAULT_BUNDLES_STARTLEVEL = "4"; //$NON-NLS-1$

	private static FrameworkLog log;
	// directory of serch candidates keyed by directory abs path -> directory listing (bug 122024)
	private static Map<String, String[]> searchCandidates = new HashMap<String, String[]>(4);
	private static EclipseAppLauncher appLauncher;
	private static List<Runnable> shutdownHandlers;

	private static ConsoleManager consoleMgr = null;

	/**
	 * This is the main to start osgi.
	 * It only works when the framework is being jared as a single jar
	 */
	public static void main(String[] args) throws Exception {
		if (FrameworkProperties.getProperty("eclipse.startTime") == null) //$NON-NLS-1$
			FrameworkProperties.setProperty("eclipse.startTime", Long.toString(System.currentTimeMillis())); //$NON-NLS-1$
		if (FrameworkProperties.getProperty(PROP_NOSHUTDOWN) == null)
			FrameworkProperties.setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
		// set the compatibility boot delegation flag to false to get "standard" OSGi behavior WRT boot delegation (bug 178477)
		if (FrameworkProperties.getProperty(Constants.OSGI_COMPATIBILITY_BOOTDELEGATION) == null)
			FrameworkProperties.setProperty(Constants.OSGI_COMPATIBILITY_BOOTDELEGATION, "false"); //$NON-NLS-1$
		Object result = run(args, null);
		if (result instanceof Integer && !Boolean.valueOf(FrameworkProperties.getProperty(PROP_NOSHUTDOWN)).booleanValue())
			System.exit(((Integer) result).intValue());
	}

	/**
	 * Launches the platform and runs a single application. The application is either identified
	 * in the given arguments (e.g., -application &lt;app id&gt;) or in the <code>eclipse.application</code> 
	 * System property.  This convenience method starts 
	 * up the platform, runs the indicated application, and then shuts down the 
	 * platform. The platform must not be running already. 
	 * 
	 * @param args the command line-style arguments used to configure the platform
	 * @param endSplashHandler the block of code to run to tear down the splash 
	 * 	screen or <code>null</code> if no tear down is required
	 * @return the result of running the application
	 * @throws Exception if anything goes wrong
	 */
	public static Object run(String[] args, Runnable endSplashHandler) throws Exception {
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logEnter("EclipseStarter.run()", null); //$NON-NLS-1$
		if (running)
			throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);
		boolean startupFailed = true;
		try {
			startup(args, endSplashHandler);
			startupFailed = false;
			if (Boolean.valueOf(FrameworkProperties.getProperty(PROP_IGNOREAPP)).booleanValue() || isForcedRestart())
				return null;
			return run(null);
		} catch (Throwable e) {
			// ensure the splash screen is down
			if (endSplashHandler != null)
				endSplashHandler.run();
			// may use startupFailed to understand where the error happened
			FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, startupFailed ? EclipseAdaptorMsg.ECLIPSE_STARTUP_STARTUP_ERROR : EclipseAdaptorMsg.ECLIPSE_STARTUP_APP_ERROR, 1, e, null);
			if (log != null)
				log.log(logEntry);
			else
				// TODO desperate measure - ideally, we should write this to disk (a la Main.log)
				e.printStackTrace();
		} finally {
			try {
				// The application typically sets the exit code however the framework can request that
				// it be re-started. We need to check for this and potentially override the exit code.
				if (isForcedRestart())
					FrameworkProperties.setProperty(PROP_EXITCODE, "23"); //$NON-NLS-1$
				if (!Boolean.valueOf(FrameworkProperties.getProperty(PROP_NOSHUTDOWN)).booleanValue())
					shutdown();
			} catch (Throwable e) {
				FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, EclipseAdaptorMsg.ECLIPSE_STARTUP_SHUTDOWN_ERROR, 1, e, null);
				if (log != null)
					log.log(logEntry);
				else
					// TODO desperate measure - ideally, we should write this to disk (a la Main.log)
					e.printStackTrace();
			}
			if (Profile.PROFILE && Profile.STARTUP)
				Profile.logExit("EclipseStarter.run()"); //$NON-NLS-1$
			if (Profile.PROFILE) {
				String report = Profile.getProfileLog();
				// avoiding writing to the console if there is nothing to print
				if (report != null && report.length() > 0)
					System.out.println(report);
			}
		}
		// we only get here if an error happened
		if (FrameworkProperties.getProperty(PROP_EXITCODE) == null) {
			FrameworkProperties.setProperty(PROP_EXITCODE, "13"); //$NON-NLS-1$
			FrameworkProperties.setProperty(PROP_EXITDATA, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_CHECK_LOG, log == null ? null : log.getFile().getPath()));
		}
		return null;
	}

	/**
	 * Returns true if the platform is already running, false otherwise.
	 * @return whether or not the platform is already running
	 */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * Starts the platform and sets it up to run a single application. The application is either identified
	 * in the given arguments (e.g., -application &lt;app id&gt;) or in the <code>eclipse.application</code>
	 * System property.  The platform must not be running already. 
	 * <p>
	 * The given runnable (if not <code>null</code>) is used to tear down the splash screen if required.
	 * </p>
	 * @param args the arguments passed to the application
	 * @return BundleContext the context of the system bundle
	 * @throws Exception if anything goes wrong
	 */
	public static BundleContext startup(String[] args, Runnable endSplashHandler) throws Exception {
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logEnter("EclipseStarter.startup()", null); //$NON-NLS-1$
		if (running)
			throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);
		FrameworkProperties.initializeProperties();
		processCommandLine(args);
		LocationManager.initializeLocations();
		loadConfigurationInfo();
		finalizeProperties();
		if (Profile.PROFILE)
			Profile.initProps(); // catch any Profile properties set in eclipse.properties...
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "props inited"); //$NON-NLS-1$ //$NON-NLS-2$
		adaptor = createAdaptor();
		log = adaptor.getFrameworkLog();
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "adapter created"); //$NON-NLS-1$ //$NON-NLS-2$
		framework = new Framework(adaptor);
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "OSGi created"); //$NON-NLS-1$ //$NON-NLS-2$
		context = framework.getBundle(0).getBundleContext();
		registerFrameworkShutdownHandlers();
		publishSplashScreen(endSplashHandler);
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "osgi launched"); //$NON-NLS-1$ //$NON-NLS-2$
		consoleMgr = ConsoleManager.startConsole(framework);
		if (Profile.PROFILE && Profile.STARTUP) {
			Profile.logTime("EclipseStarter.startup()", "console started"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		framework.launch();
		// save the cached timestamp before loading basic bundles; this is needed so we can do a proper timestamp check when logging resolver errors
		long stateStamp = adaptor.getState().getTimeStamp();
		Bundle[] startBundles = loadBasicBundles();

		if (startBundles == null || ("true".equals(FrameworkProperties.getProperty(PROP_REFRESH_BUNDLES)) && refreshPackages(getCurrentBundles(false)))) { //$NON-NLS-1$
			waitForShutdown();
			return context; // cannot continue; loadBasicBundles caused refreshPackages to shutdown the framework
		}

		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "loading basic bundles"); //$NON-NLS-1$ //$NON-NLS-2$

		// set the framework start level to the ultimate value.  This will actually start things
		// running if they are persistently active.
		setStartLevel(getStartLevel());
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logTime("EclipseStarter.startup()", "StartLevel set"); //$NON-NLS-1$ //$NON-NLS-2$
		// they should all be active by this time
		ensureBundlesActive(startBundles);

		// in the case where the built-in console is disabled we should try to start the console bundle
		try {
			consoleMgr.checkForConsoleBundle();
		} catch (BundleException e) {
			FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null);
			log.log(entry);
		}
		if (debug || FrameworkProperties.getProperty(PROP_DEV) != null)
			// only spend time showing unresolved bundles in dev/debug mode and the state has changed
			if (stateStamp != adaptor.getState().getTimeStamp())
				logUnresolvedBundles(context.getBundles());
		running = true;
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logExit("EclipseStarter.startup()"); //$NON-NLS-1$
		return context;
	}

	private static int getStartLevel() {
		String level = FrameworkProperties.getProperty(PROP_INITIAL_STARTLEVEL);
		if (level != null)
			try {
				return Integer.parseInt(level);
			} catch (NumberFormatException e) {
				if (debug)
					System.out.println("Start level = " + level + "  parsed. Using hardcoded default: 6"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		return DEFAULT_INITIAL_STARTLEVEL;
	}

	/**
	 * Runs the application for which the platform was started. The platform 
	 * must be running. 
	 * <p>
	 * The given argument is passed to the application being run.  If it is <code>null</code>
	 * then the command line arguments used in starting the platform, and not consumed
	 * by the platform code, are passed to the application as a <code>String[]</code>.
	 * </p>
	 * @param argument the argument passed to the application. May be <code>null</code>
	 * @return the result of running the application
	 * @throws Exception if anything goes wrong
	 */
	public static Object run(Object argument) throws Exception {
		if (Profile.PROFILE && Profile.STARTUP)
			Profile.logEnter("EclipseStarter.run(Object)()", null); //$NON-NLS-1$
		if (!running)
			throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_NOT_RUNNING);
		// if we are just initializing, do not run the application just return.
		if (initialize)
			return new Integer(0);
		try {
			if (appLauncher == null) {
				boolean launchDefault = Boolean.valueOf(FrameworkProperties.getProperty(PROP_APPLICATION_LAUNCHDEFAULT, "true")).booleanValue(); //$NON-NLS-1$
				// create the ApplicationLauncher and register it as a service
				appLauncher = new EclipseAppLauncher(context, Boolean.valueOf(FrameworkProperties.getProperty(PROP_ALLOW_APPRELAUNCH)).booleanValue(), launchDefault, log);
				appLauncherRegistration = context.registerService(ApplicationLauncher.class.getName(), appLauncher, null);
				// must start the launcher AFTER service restration because this method 
				// blocks and runs the application on the current thread.  This method 
				// will return only after the application has stopped.
				return appLauncher.start(argument);
			}
			return appLauncher.reStart(argument);
		} catch (Exception e) {
			if (log != null && context != null) // context can be null if OSGi failed to launch (bug 151413)
				logUnresolvedBundles(context.getBundles());
			throw e;
		}
	}

	/**
	 * Shuts down the Platform. The state of the Platform is not automatically 
	 * saved before shutting down. 
	 * <p>
	 * On return, the Platform will no longer be running (but could be re-launched 
	 * with another call to startup). If relaunching, care must be taken to reinitialize
	 * any System properties which the platform uses (e.g., osgi.instance.area) as
	 * some policies in the platform do not allow resetting of such properties on 
	 * subsequent runs.
	 * </p><p>
	 * Any objects handed out by running Platform, 
	 * including Platform runnables obtained via getRunnable, will be 
	 * permanently invalid. The effects of attempting to invoke methods 
	 * on invalid objects is undefined. 
	 * </p>
	 * @throws Exception if anything goes wrong
	 */
	public static void shutdown() throws Exception {
		if (!running || framework == null)
			return;
		if (appLauncherRegistration != null)
			appLauncherRegistration.unregister();
		if (splashStreamRegistration != null)
			splashStreamRegistration.unregister();
		if (defaultMonitorRegistration != null)
			defaultMonitorRegistration.unregister();
		if (appLauncher != null)
			appLauncher.shutdown();
		appLauncherRegistration = null;
		appLauncher = null;
		splashStreamRegistration = null;
		defaultMonitorRegistration = null;
		if (consoleMgr != null) {
			consoleMgr.stopConsole();
			consoleMgr = null;
		}
		framework.close();
		framework = null;
		context = null;
		running = false;
	}

	private static void ensureBundlesActive(Bundle[] bundles) {
		ServiceTracker<StartLevel, StartLevel> tracker = null;
		try {
			for (int i = 0; i < bundles.length; i++) {
				if (bundles[i].getState() != Bundle.ACTIVE) {
					if (bundles[i].getState() == Bundle.INSTALLED) {
						// Log that the bundle is not resolved
						log.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundles[i].getLocation()), 0, null, null));
						continue;
					}
					// check that the startlevel allows the bundle to be active (111550)
					if (tracker == null) {
						tracker = new ServiceTracker<StartLevel, StartLevel>(context, StartLevel.class.getName(), null);
						tracker.open();
					}
					StartLevel sl = tracker.getService();
					if (sl != null && (sl.getBundleStartLevel(bundles[i]) <= sl.getStartLevel())) {
						log.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_ACTIVE, bundles[i]), 0, null, null));
					}
				}
			}
		} finally {
			if (tracker != null)
				tracker.close();
		}
	}

	private static void logUnresolvedBundles(Bundle[] bundles) {
		State state = adaptor.getState();
		FrameworkLog logService = adaptor.getFrameworkLog();
		StateHelper stateHelper = adaptor.getPlatformAdmin().getStateHelper();

		// first lets look for missing leaf constraints (bug 114120)
		VersionConstraint[] leafConstraints = stateHelper.getUnsatisfiedLeaves(state.getBundles());
		// hash the missing leaf constraints by the declaring bundles
		Map<BundleDescription, List<VersionConstraint>> missing = new HashMap<BundleDescription, List<VersionConstraint>>();
		for (int i = 0; i < leafConstraints.length; i++) {
			// only include non-optional and non-dynamic constraint leafs
			if (leafConstraints[i] instanceof BundleSpecification && ((BundleSpecification) leafConstraints[i]).isOptional())
				continue;
			if (leafConstraints[i] instanceof ImportPackageSpecification) {
				if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) leafConstraints[i]).getDirective(Constants.RESOLUTION_DIRECTIVE)))
					continue;
				if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(((ImportPackageSpecification) leafConstraints[i]).getDirective(Constants.RESOLUTION_DIRECTIVE)))
					continue;
			}
			BundleDescription bundle = leafConstraints[i].getBundle();
			List<VersionConstraint> constraints = missing.get(bundle);
			if (constraints == null) {
				constraints = new ArrayList<VersionConstraint>();
				missing.put(bundle, constraints);
			}
			constraints.add(leafConstraints[i]);
		}

		// found some bundles with missing leaf constraints; log them first 
		if (missing.size() > 0) {
			FrameworkLogEntry[] rootChildren = new FrameworkLogEntry[missing.size()];
			int rootIndex = 0;
			for (Iterator<BundleDescription> iter = missing.keySet().iterator(); iter.hasNext(); rootIndex++) {
				BundleDescription description = iter.next();
				String symbolicName = description.getSymbolicName() == null ? FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME : description.getSymbolicName();
				String generalMessage = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, description.getLocation());
				List<VersionConstraint> constraints = missing.get(description);
				FrameworkLogEntry[] logChildren = new FrameworkLogEntry[constraints.size()];
				for (int i = 0; i < logChildren.length; i++)
					logChildren[i] = new FrameworkLogEntry(symbolicName, FrameworkLogEntry.WARNING, 0, MessageHelper.getResolutionFailureMessage(constraints.get(i)), 0, null, null);
				rootChildren[rootIndex] = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, generalMessage, 0, null, logChildren);
			}
			logService.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, EclipseAdaptorMsg.ECLIPSE_STARTUP_ROOTS_NOT_RESOLVED, 0, null, rootChildren));
		}

		// There may be some bundles unresolved for other reasons, causing the system to be unresolved
		// log all unresolved constraints now
		List<FrameworkLogEntry> allChildren = new ArrayList<FrameworkLogEntry>();
		for (int i = 0; i < bundles.length; i++)
			if (bundles[i].getState() == Bundle.INSTALLED) {
				String symbolicName = bundles[i].getSymbolicName() == null ? FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME : bundles[i].getSymbolicName();
				String generalMessage = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundles[i]);
				BundleDescription description = state.getBundle(bundles[i].getBundleId());
				// for some reason, the state does not know about that bundle
				if (description == null)
					continue;
				FrameworkLogEntry[] logChildren = null;
				VersionConstraint[] unsatisfied = stateHelper.getUnsatisfiedConstraints(description);
				if (unsatisfied.length > 0) {
					// the bundle wasn't resolved due to some of its constraints were unsatisfiable
					logChildren = new FrameworkLogEntry[unsatisfied.length];
					for (int j = 0; j < unsatisfied.length; j++)
						logChildren[j] = new FrameworkLogEntry(symbolicName, FrameworkLogEntry.WARNING, 0, MessageHelper.getResolutionFailureMessage(unsatisfied[j]), 0, null, null);
				} else {
					ResolverError[] resolverErrors = state.getResolverErrors(description);
					if (resolverErrors.length > 0) {
						logChildren = new FrameworkLogEntry[resolverErrors.length];
						for (int j = 0; j < resolverErrors.length; j++)
							logChildren[j] = new FrameworkLogEntry(symbolicName, FrameworkLogEntry.WARNING, 0, resolverErrors[j].toString(), 0, null, null);
					}
				}

				allChildren.add(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, generalMessage, 0, null, logChildren));
			}
		if (allChildren.size() > 0)
			logService.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, EclipseAdaptorMsg.ECLIPSE_STARTUP_ALL_NOT_RESOLVED, 0, null, allChildren.toArray(new FrameworkLogEntry[allChildren.size()])));
	}

	private static void publishSplashScreen(final Runnable endSplashHandler) {
		if (endSplashHandler == null)
			return;
		// register the output stream to the launcher if it exists
		try {
			Method method = endSplashHandler.getClass().getMethod("getOutputStream", new Class[0]); //$NON-NLS-1$
			Object outputStream = method.invoke(endSplashHandler, new Object[0]);
			if (outputStream instanceof OutputStream) {
				Dictionary<String, Object> osProperties = new Hashtable<String, Object>();
				osProperties.put("name", "splashstream"); //$NON-NLS-1$//$NON-NLS-2$
				splashStreamRegistration = context.registerService(OutputStream.class.getName(), outputStream, osProperties);
			}
		} catch (Exception ex) {
			// ignore
		}
		// keep this splash handler as the default startup monitor
		try {
			Dictionary<String, Object> monitorProps = new Hashtable<String, Object>();
			monitorProps.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
			defaultMonitorRegistration = context.registerService(StartupMonitor.class.getName(), new DefaultStartupMonitor(endSplashHandler), monitorProps);
		} catch (IllegalStateException e) {
			//splash handler did not provide the necessary methods, ignore it
		}
	}

	@SuppressWarnings("deprecation")
	private static URL searchForBundle(String name, String parent) throws MalformedURLException {
		URL url = null;
		File fileLocation = null;
		boolean reference = false;
		try {
			new URL(name); // quick check to see if the name is a valid URL
			url = new URL(new File(parent).toURL(), name);
		} catch (MalformedURLException e) {
			// TODO this is legacy support for non-URL names.  It should be removed eventually.
			// if name was not a URL then construct one.  
			// Assume it should be a reference and that it is relative.  This support need not 
			// be robust as it is temporary..
			File child = new File(name);
			fileLocation = child.isAbsolute() ? child : new File(parent, name);
			url = new URL(REFERENCE_PROTOCOL, null, fileLocation.toURL().toExternalForm());
			reference = true;
		}
		// if the name was a URL then see if it is relative.  If so, insert syspath.
		if (!reference) {
			URL baseURL = url;
			// if it is a reference URL then strip off the reference: and set base to the file:...
			if (url.getProtocol().equals(REFERENCE_PROTOCOL)) {
				reference = true;
				String baseSpec = url.getFile();
				if (baseSpec.startsWith(FILE_SCHEME)) {
					File child = new File(baseSpec.substring(5));
					baseURL = child.isAbsolute() ? child.toURL() : new File(parent, child.getPath()).toURL();
				} else
					baseURL = new URL(baseSpec);
			}

			fileLocation = new File(baseURL.getFile());
			// if the location is relative, prefix it with the parent
			if (!fileLocation.isAbsolute())
				fileLocation = new File(parent, fileLocation.toString());
		}
		// If the result is a reference then search for the real result and 
		// reconstruct the answer.
		if (reference) {
			String result = searchFor(fileLocation.getName(), new File(fileLocation.getParent()).getAbsolutePath());
			if (result != null)
				url = new URL(REFERENCE_PROTOCOL, null, FILE_SCHEME + result);
			else
				return null;
		}

		// finally we have something worth trying	
		try {
			URLConnection result = url.openConnection();
			result.connect();
			return url;
		} catch (IOException e) {
			//			int i = location.lastIndexOf('_');
			//			return i == -1? location : location.substring(0, i);
			return null;
		}
	}

	/*
	 * Ensure all basic bundles are installed, resolved and scheduled to start. Returns an array containing
	 * all basic bundles that are marked to start. 
	 * Returns null if the framework has been shutdown as a result of refreshPackages
	 */
	private static Bundle[] loadBasicBundles() {
		long startTime = System.currentTimeMillis();
		String osgiBundles = FrameworkProperties.getProperty(PROP_BUNDLES);
		String osgiExtensions = FrameworkProperties.getProperty(PROP_EXTENSIONS);
		if (osgiExtensions != null && osgiExtensions.length() > 0) {
			osgiBundles = osgiExtensions + ',' + osgiBundles;
			FrameworkProperties.setProperty(PROP_BUNDLES, osgiBundles);
		}
		String[] installEntries = getArrayFromList(osgiBundles, ","); //$NON-NLS-1$
		// get the initial bundle list from the installEntries
		InitialBundle[] initialBundles = getInitialBundles(installEntries);
		// get the list of currently installed initial bundles from the framework
		Bundle[] curInitBundles = getCurrentBundles(true);

		// list of bundles to be refreshed
		List<Bundle> toRefresh = new ArrayList<Bundle>(curInitBundles.length);
		// uninstall any of the currently installed bundles that do not exist in the 
		// initial bundle list from installEntries.
		uninstallBundles(curInitBundles, initialBundles, toRefresh);

		// install the initialBundles that are not already installed.
		List<Bundle> startBundles = new ArrayList<Bundle>(installEntries.length);
		List<Bundle> lazyActivationBundles = new ArrayList<Bundle>(installEntries.length);
		installBundles(initialBundles, curInitBundles, startBundles, lazyActivationBundles, toRefresh);

		// If we installed/uninstalled something, force a refresh of all installed/uninstalled bundles
		if (!toRefresh.isEmpty() && refreshPackages(toRefresh.toArray(new Bundle[toRefresh.size()])))
			return null; // cannot continue; refreshPackages shutdown the framework

		// schedule all basic bundles to be started
		Bundle[] startInitBundles = startBundles.toArray(new Bundle[startBundles.size()]);
		Bundle[] lazyInitBundles = lazyActivationBundles.toArray(new Bundle[lazyActivationBundles.size()]);
		startBundles(startInitBundles, lazyInitBundles);

		if (debug)
			System.out.println("Time to load bundles: " + (System.currentTimeMillis() - startTime)); //$NON-NLS-1$
		return startInitBundles;
	}

	private static InitialBundle[] getInitialBundles(String[] installEntries) {
		searchCandidates.clear();
		List<InitialBundle> result = new ArrayList<InitialBundle>(installEntries.length);
		int defaultStartLevel = Integer.parseInt(FrameworkProperties.getProperty(PROP_BUNDLES_STARTLEVEL, DEFAULT_BUNDLES_STARTLEVEL));
		String syspath = getSysPath();
		// should canonicalize the syspath.
		try {
			syspath = new File(syspath).getCanonicalPath();
		} catch (IOException ioe) {
			// do nothing
		}
		for (int i = 0; i < installEntries.length; i++) {
			String name = installEntries[i];
			int level = defaultStartLevel;
			boolean start = false;
			int index = name.lastIndexOf('@');
			if (index >= 0) {
				String[] attributes = getArrayFromList(name.substring(index + 1, name.length()), ":"); //$NON-NLS-1$
				for (int j = 0; j < attributes.length; j++) {
					String attribute = attributes[j];
					if (attribute.equals("start")) //$NON-NLS-1$
						start = true;
					else {
						try {
							level = Integer.parseInt(attribute);
						} catch (NumberFormatException e) { // bug 188089
							index = name.length();
							continue;
						}
					}
				}
				name = name.substring(0, index);
			}
			try {
				URL location = searchForBundle(name, syspath);
				if (location == null) {
					FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_BUNDLE_NOT_FOUND, installEntries[i]), 0, null, null);
					log.log(entry);
					// skip this entry
					continue;
				}
				location = makeRelative(LocationManager.getInstallLocation().getURL(), location);
				String locationString = INITIAL_LOCATION + location.toExternalForm();
				result.add(new InitialBundle(locationString, location, level, start));
			} catch (IOException e) {
				log.log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null));
			}
		}
		return result.toArray(new InitialBundle[result.size()]);
	}

	// returns true if the refreshPackages operation caused the framework to shutdown
	private static boolean refreshPackages(Bundle[] bundles) {
		ServiceReference<?> packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null)
			packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
		if (packageAdmin == null)
			return false;
		// TODO this is such a hack it is silly.  There are still cases for race conditions etc
		// but this should allow for some progress...
		final Semaphore semaphore = new Semaphore(0);
		StartupEventListener listener = new StartupEventListener(semaphore, FrameworkEvent.PACKAGES_REFRESHED);
		context.addFrameworkListener(listener);
		context.addBundleListener(listener);
		packageAdmin.refreshPackages(bundles);
		context.ungetService(packageAdminRef);
		updateSplash(semaphore, listener);
		if (isForcedRestart())
			return true;
		return false;
	}

	private static void waitForShutdown() {
		if (!isForcedRestart())
			return;
		// wait for the system bundle to stop
		Bundle systemBundle = framework.getBundle(0);
		int i = 0;
		while (i < 5000 && (systemBundle.getState() & (Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING)) != 0) {
			i += 200;
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	/**
	 *  Creates and returns the adaptor
	 *
	 *  @return a FrameworkAdaptor object
	 */
	private static FrameworkAdaptor createAdaptor() throws Exception {
		String adaptorClassName = FrameworkProperties.getProperty(PROP_ADAPTOR, DEFAULT_ADAPTOR_CLASS);
		Class<?> adaptorClass = Class.forName(adaptorClassName);
		Class<?>[] constructorArgs = new Class[] {String[].class};
		Constructor<?> constructor = adaptorClass.getConstructor(constructorArgs);
		return (FrameworkAdaptor) constructor.newInstance(new Object[] {new String[0]});
	}

	private static String[] processCommandLine(String[] args) throws Exception {
		EclipseEnvironmentInfo.setAllArgs(args);
		if (args.length == 0) {
			EclipseEnvironmentInfo.setFrameworkArgs(args);
			EclipseEnvironmentInfo.setAllArgs(args);
			return args;
		}
		int[] configArgs = new int[args.length];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)

			// check if debug should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
			// simply enable debug.  Otherwise, assume that that the following arg is
			// actually the filename of an options file.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEBUG) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				FrameworkProperties.setProperty(PROP_DEBUG, ""); //$NON-NLS-1$
				debug = true;
				found = true;
			}

			// check if development mode should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
			// simply enable development mode.  Otherwise, assume that that the following arg is
			// actually some additional development time class path entries.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				FrameworkProperties.setProperty(PROP_DEV, ""); //$NON-NLS-1$
				found = true;
			}

			// look for the initialization arg
			if (args[i].equalsIgnoreCase(INITIALIZE)) {
				initialize = true;
				found = true;
			}

			// look for the clean flag.
			if (args[i].equalsIgnoreCase(CLEAN)) {
				FrameworkProperties.setProperty(PROP_CLEAN, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the consoleLog flag
			if (args[i].equalsIgnoreCase(CONSOLE_LOG)) {
				FrameworkProperties.setProperty(PROP_CONSOLE_LOG, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the console with no port.  
			if (args[i].equalsIgnoreCase(CONSOLE) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				FrameworkProperties.setProperty(PROP_CONSOLE, ""); //$NON-NLS-1$
				found = true;
			}

			if (args[i].equalsIgnoreCase(NOEXIT)) {
				FrameworkProperties.setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
				found = true;
			}

			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}
			// check for args with parameters. If we are at the last argument or if the next one
			// has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) { //$NON-NLS-1$
				continue;
			}
			String arg = args[++i];

			// look for the console and port.  
			if (args[i - 1].equalsIgnoreCase(CONSOLE)) {
				FrameworkProperties.setProperty(PROP_CONSOLE, arg);
				found = true;
			}

			// look for the configuration location .  
			if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
				FrameworkProperties.setProperty(LocationManager.PROP_CONFIG_AREA, arg);
				found = true;
			}

			// look for the data location for this instance.  
			if (args[i - 1].equalsIgnoreCase(DATA)) {
				FrameworkProperties.setProperty(LocationManager.PROP_INSTANCE_AREA, arg);
				found = true;
			}

			// look for the user location for this instance.  
			if (args[i - 1].equalsIgnoreCase(USER)) {
				FrameworkProperties.setProperty(LocationManager.PROP_USER_AREA, arg);
				found = true;
			}

			// look for the launcher location
			if (args[i - 1].equalsIgnoreCase(LAUNCHER)) {
				FrameworkProperties.setProperty(LocationManager.PROP_LAUNCHER, arg);
				found = true;
			}
			// look for the development mode and class path entries.  
			if (args[i - 1].equalsIgnoreCase(DEV)) {
				FrameworkProperties.setProperty(PROP_DEV, arg);
				found = true;
			}

			// look for the debug mode and option file location.  
			if (args[i - 1].equalsIgnoreCase(DEBUG)) {
				FrameworkProperties.setProperty(PROP_DEBUG, arg);
				debug = true;
				found = true;
			}

			// look for the window system.  
			if (args[i - 1].equalsIgnoreCase(WS)) {
				FrameworkProperties.setProperty(PROP_WS, arg);
				found = true;
			}

			// look for the operating system
			if (args[i - 1].equalsIgnoreCase(OS)) {
				FrameworkProperties.setProperty(PROP_OS, arg);
				found = true;
			}

			// look for the system architecture
			if (args[i - 1].equalsIgnoreCase(ARCH)) {
				FrameworkProperties.setProperty(PROP_ARCH, arg);
				found = true;
			}

			// look for the nationality/language
			if (args[i - 1].equalsIgnoreCase(NL)) {
				FrameworkProperties.setProperty(PROP_NL, arg);
				found = true;
			}

			// look for the locale extensions
			if (args[i - 1].equalsIgnoreCase(NL_EXTENSIONS)) {
				FrameworkProperties.setProperty(PROP_NL_EXTENSIONS, arg);
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
			EclipseEnvironmentInfo.setFrameworkArgs(new String[0]);
			EclipseEnvironmentInfo.setAppArgs(args);
			return args;
		}
		String[] appArgs = new String[args.length - configArgIndex];
		String[] frameworkArgs = new String[configArgIndex];
		configArgIndex = 0;
		int j = 0;
		int k = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex]) {
				frameworkArgs[k++] = args[i];
				configArgIndex++;
			} else
				appArgs[j++] = args[i];
		}
		EclipseEnvironmentInfo.setFrameworkArgs(frameworkArgs);
		EclipseEnvironmentInfo.setAppArgs(appArgs);
		return appArgs;
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	private static String[] getArrayFromList(String prop, String separator) {
		return ManifestElement.getArrayFromList(prop, separator);
	}

	protected static String getSysPath() {
		String result = FrameworkProperties.getProperty(PROP_SYSPATH);
		if (result != null)
			return result;
		result = getSysPathFromURL(FrameworkProperties.getProperty(PROP_FRAMEWORK));
		if (result == null)
			result = getSysPathFromCodeSource();
		if (result == null)
			throw new IllegalStateException("Can not find the system path."); //$NON-NLS-1$
		if (Character.isUpperCase(result.charAt(0))) {
			char[] chars = result.toCharArray();
			chars[0] = Character.toLowerCase(chars[0]);
			result = new String(chars);
		}
		FrameworkProperties.setProperty(PROP_SYSPATH, result);
		return result;
	}

	private static String getSysPathFromURL(String urlSpec) {
		if (urlSpec == null)
			return null;
		URL url = LocationHelper.buildURL(urlSpec, false);
		if (url == null)
			return null;
		File fwkFile = new File(url.getFile());
		fwkFile = new File(fwkFile.getAbsolutePath());
		fwkFile = new File(fwkFile.getParent());
		return fwkFile.getAbsolutePath();
	}

	private static String getSysPathFromCodeSource() {
		ProtectionDomain pd = EclipseStarter.class.getProtectionDomain();
		if (pd == null)
			return null;
		CodeSource cs = pd.getCodeSource();
		if (cs == null)
			return null;
		URL url = cs.getLocation();
		if (url == null)
			return null;
		String result = url.getFile();
		if (result.endsWith(".jar")) { //$NON-NLS-1$
			result = result.substring(0, result.lastIndexOf('/'));
			if ("folder".equals(FrameworkProperties.getProperty(PROP_FRAMEWORK_SHAPE))) //$NON-NLS-1$
				result = result.substring(0, result.lastIndexOf('/'));
		} else {
			if (result.endsWith("/")) //$NON-NLS-1$
				result = result.substring(0, result.length() - 1);
			result = result.substring(0, result.lastIndexOf('/'));
			result = result.substring(0, result.lastIndexOf('/'));
		}
		return result;
	}

	private static Bundle[] getCurrentBundles(boolean includeInitial) {
		Bundle[] installed = context.getBundles();
		List<Bundle> initial = new ArrayList<Bundle>();
		for (int i = 0; i < installed.length; i++) {
			Bundle bundle = installed[i];
			if (bundle.getLocation().startsWith(INITIAL_LOCATION)) {
				if (includeInitial)
					initial.add(bundle);
			} else if (!includeInitial && bundle.getBundleId() != 0)
				initial.add(bundle);
		}
		return initial.toArray(new Bundle[initial.size()]);
	}

	private static Bundle getBundleByLocation(String location, Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (location.equalsIgnoreCase(bundle.getLocation()))
				return bundle;
		}
		return null;
	}

	private static void uninstallBundles(Bundle[] curInitBundles, InitialBundle[] newInitBundles, List<Bundle> toRefresh) {
		for (int i = 0; i < curInitBundles.length; i++) {
			boolean found = false;
			for (int j = 0; j < newInitBundles.length; j++) {
				if (curInitBundles[i].getLocation().equalsIgnoreCase(newInitBundles[j].locationString)) {
					found = true;
					break;
				}
			}
			if (!found)
				try {
					curInitBundles[i].uninstall();
					toRefresh.add(curInitBundles[i]);
				} catch (BundleException e) {
					FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_UNINSTALL, curInitBundles[i].getLocation()), 0, e, null);
					log.log(entry);
				}
		}
	}

	private static void installBundles(InitialBundle[] initialBundles, Bundle[] curInitBundles, List<Bundle> startBundles, List<Bundle> lazyActivationBundles, List<Bundle> toRefresh) {
		ServiceReference<?> reference = context.getServiceReference(StartLevel.class.getName());
		StartLevel startService = null;
		if (reference != null)
			startService = (StartLevel) context.getService(reference);
		try {
			for (int i = 0; i < initialBundles.length; i++) {
				Bundle osgiBundle = getBundleByLocation(initialBundles[i].locationString, curInitBundles);
				try {
					// don't need to install if it is already installed
					if (osgiBundle == null) {
						InputStream in = initialBundles[i].location.openStream();
						try {
							osgiBundle = context.installBundle(initialBundles[i].locationString, in);
						} catch (BundleException e) {
							StatusException status = e instanceof StatusException ? (StatusException) e : null;
							if (status != null && status.getStatusCode() == StatusException.CODE_OK && status.getStatus() instanceof Bundle) {
								osgiBundle = (Bundle) status.getStatus();
							} else
								throw e;
						}
						// only check for lazy activation header if this is a newly installed bundle and is not marked for persistent start
						if (!initialBundles[i].start && hasLazyActivationPolicy(osgiBundle))
							lazyActivationBundles.add(osgiBundle);
					}
					// always set the startlevel incase it has changed (bug 111549)
					// this is a no-op if the level is the same as previous launch.
					if ((osgiBundle.getState() & Bundle.UNINSTALLED) == 0 && initialBundles[i].level >= 0 && startService != null)
						startService.setBundleStartLevel(osgiBundle, initialBundles[i].level);
					// if this bundle is supposed to be started then add it to the start list
					if (initialBundles[i].start)
						startBundles.add(osgiBundle);
					// include basic bundles in case they were not resolved before
					if ((osgiBundle.getState() & Bundle.INSTALLED) != 0)
						toRefresh.add(osgiBundle);
				} catch (BundleException e) {
					FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_INSTALL, initialBundles[i].location), 0, e, null);
					log.log(entry);
				} catch (IOException e) {
					FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_INSTALL, initialBundles[i].location), 0, e, null);
					log.log(entry);
				}
			}
		} finally {
			if (reference != null)
				context.ungetService(reference);
		}
	}

	@SuppressWarnings("deprecation")
	private static boolean hasLazyActivationPolicy(Bundle target) {
		// check the bundle manifest to see if it defines a lazy activation policy
		Dictionary<String, String> headers = target.getHeaders(""); //$NON-NLS-1$
		// first check to see if this is a fragment bundle
		String fragmentHost = headers.get(Constants.FRAGMENT_HOST);
		if (fragmentHost != null)
			return false; // do not activate fragment bundles
		// look for the OSGi defined Bundle-ActivationPolicy header
		String activationPolicy = headers.get(Constants.BUNDLE_ACTIVATIONPOLICY);
		try {
			if (activationPolicy != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_ACTIVATIONPOLICY, activationPolicy);
				if (elements != null && elements.length > 0) {
					// if the value is "lazy" then it has a lazy activation poliyc
					if (Constants.ACTIVATION_LAZY.equals(elements[0].getValue()))
						return true;
				}
			} else {
				// check for Eclipse specific lazy start headers "Eclipse-LazyStart" and "Eclipse-AutoStart"
				String eclipseLazyStart = headers.get(Constants.ECLIPSE_LAZYSTART);
				if (eclipseLazyStart == null)
					eclipseLazyStart = headers.get(Constants.ECLIPSE_AUTOSTART);
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.ECLIPSE_LAZYSTART, eclipseLazyStart);
				if (elements != null && elements.length > 0) {
					// if the value is true then it is lazy activated
					if ("true".equals(elements[0].getValue())) //$NON-NLS-1$
						return true;
					// otherwise it is only lazy activated if it defines an exceptions directive.
					else if (elements[0].getDirective("exceptions") != null) //$NON-NLS-1$
						return true;
				}
			}
		} catch (BundleException be) {
			// ignore this
		}
		return false;
	}

	private static void startBundles(Bundle[] startBundles, Bundle[] lazyBundles) {
		for (int i = 0; i < startBundles.length; i++)
			startBundle(startBundles[i], 0);
		for (int i = 0; i < lazyBundles.length; i++)
			startBundle(lazyBundles[i], Bundle.START_ACTIVATION_POLICY);
	}

	private static void startBundle(Bundle bundle, int options) {
		try {
			bundle.start(options);
		} catch (BundleException e) {
			if ((bundle.getState() & Bundle.RESOLVED) != 0) {
				// only log errors if the bundle is resolved
				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_START, bundle.getLocation()), 0, e, null);
				log.log(entry);
			}
		}
	}

	private static void loadConfigurationInfo() {
		Location configArea = LocationManager.getConfigurationLocation();
		if (configArea == null)
			return;

		URL location = null;
		try {
			location = new URL(configArea.getURL().toExternalForm() + LocationManager.CONFIG_FILE);
		} catch (MalformedURLException e) {
			// its ok.  This should never happen
		}
		mergeProperties(FrameworkProperties.getProperties(), loadProperties(location));
	}

	private static Properties loadProperties(URL location) {
		Properties result = new Properties();
		if (location == null)
			return result;
		try {
			InputStream in = location.openStream();
			try {
				result.load(in);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			// its ok if there is no file.  We'll just use the defaults for everything
			// TODO but it might be nice to log something with gentle wording (i.e., it is not an error)
		}
		return substituteVars(result);
	}

	private static Properties substituteVars(Properties result) {
		if (result == null) {
			//nothing todo.
			return null;
		}
		for (Enumeration<Object> eKeys = result.keys(); eKeys.hasMoreElements();) {
			Object key = eKeys.nextElement();
			if (key instanceof String) {
				String value = result.getProperty((String) key);
				if (value != null)
					result.put(key, BaseStorageHook.substituteVars(value));
			}
		}
		return result;
	}

	/**
	 * Returns a URL which is equivalent to the given URL relative to the
	 * specified base URL. Works only for file: URLs
	 * @throws MalformedURLException 
	 */
	private static URL makeRelative(URL base, URL location) throws MalformedURLException {
		if (base == null)
			return location;
		if (!"file".equals(base.getProtocol())) //$NON-NLS-1$
			return location;
		if (!location.getProtocol().equals(REFERENCE_PROTOCOL))
			return location; // we can only make reference urls relative
		URL nonReferenceLocation = new URL(location.getPath());
		// if some URL component does not match, return the original location
		if (!base.getProtocol().equals(nonReferenceLocation.getProtocol()))
			return location;
		File locationPath = new File(nonReferenceLocation.getPath());
		// if location is not absolute, return original location 
		if (!locationPath.isAbsolute())
			return location;
		File relativePath = makeRelative(new File(base.getPath()), locationPath);
		String urlPath = relativePath.getPath();
		if (File.separatorChar != '/')
			urlPath = urlPath.replace(File.separatorChar, '/');
		if (nonReferenceLocation.getPath().endsWith("/")) //$NON-NLS-1$
			// restore original trailing slash 
			urlPath += '/';
		// couldn't use File to create URL here because it prepends the path with user.dir 
		URL relativeURL = new URL(base.getProtocol(), base.getHost(), base.getPort(), urlPath);
		// now make it back to a reference URL
		relativeURL = new URL(REFERENCE_SCHEME + relativeURL.toExternalForm());
		return relativeURL;
	}

	private static File makeRelative(File base, File location) {
		if (!location.isAbsolute())
			return location;
		File relative = new File(new FilePath(base).makeRelative(new FilePath(location)));
		return relative;
	}

	private static void mergeProperties(Properties destination, Properties source) {
		for (Enumeration<?> e = source.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String value = source.getProperty(key);
			if (destination.getProperty(key) == null)
				destination.setProperty(key, value);
		}
	}

	private static void setStartLevel(final int value) {
		ServiceReference<?> reference = context.getServiceReference(StartLevel.class.getName());
		final StartLevel startLevel = reference != null ? (StartLevel) context.getService(reference) : null;
		if (startLevel == null)
			return;
		final Semaphore semaphore = new Semaphore(0);
		StartupEventListener listener = new StartupEventListener(semaphore, FrameworkEvent.STARTLEVEL_CHANGED);
		context.addFrameworkListener(listener);
		context.addBundleListener(listener);
		startLevel.setStartLevel(value);
		context.ungetService(reference);
		updateSplash(semaphore, listener);
	}

	static class StartupEventListener implements SynchronousBundleListener, FrameworkListener {
		private final Semaphore semaphore;
		private final int frameworkEventType;

		public StartupEventListener(Semaphore semaphore, int frameworkEventType) {
			this.semaphore = semaphore;
			this.frameworkEventType = frameworkEventType;
		}

		public void bundleChanged(BundleEvent event) {
			if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STOPPING)
				semaphore.release();
		}

		public void frameworkEvent(FrameworkEvent event) {
			if (event.getType() == frameworkEventType)
				semaphore.release();
		}

	}

	private static void updateSplash(Semaphore semaphore, StartupEventListener listener) {
		ServiceTracker<StartupMonitor, StartupMonitor> monitorTracker = new ServiceTracker<StartupMonitor, StartupMonitor>(context, StartupMonitor.class.getName(), null);
		monitorTracker.open();
		try {
			while (true) {
				StartupMonitor monitor = monitorTracker.getService();
				if (monitor != null) {
					try {
						monitor.update();
					} catch (Throwable e) {
						// ignore exceptions thrown by the monitor
					}
				}
				// can we acquire the semaphore yet?
				if (semaphore.acquire(50))
					break; //done
				//else still working, spin another update
			}
		} finally {
			if (listener != null) {
				context.removeFrameworkListener(listener);
				context.removeBundleListener(listener);
			}
			monitorTracker.close();
		}
	}

	/**
	 * Searches for the given target directory immediately under
	 * the given start location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
	private static String searchFor(final String target, String start) {
		String[] candidates = searchCandidates.get(start);
		if (candidates == null) {
			File startFile = new File(start);
			// Pre-check if file exists, if not, and it contains escape characters,
			// try decoding the path
			if (!startFile.exists() && start.indexOf('%') >= 0) {
				String decodePath = FrameworkProperties.decode(start);
				File f = new File(decodePath);
				if (f.exists())
					startFile = f;
			}
			candidates = startFile.list();
			if (candidates != null)
				searchCandidates.put(start, candidates);
		}
		if (candidates == null)
			return null;
		String result = null;
		Object[] maxVersion = null;
		boolean resultIsFile = false;
		for (int i = 0; i < candidates.length; i++) {
			String candidateName = candidates[i];
			if (!candidateName.startsWith(target))
				continue;
			boolean simpleJar = false;
			final char versionSep = candidateName.length() > target.length() ? candidateName.charAt(target.length()) : 0;
			if (candidateName.length() > target.length() && versionSep != '_' && versionSep != '-') {
				// make sure this is not just a jar with no (_|-)version tacked on the end
				if (candidateName.length() == 4 + target.length() && candidateName.endsWith(".jar")) //$NON-NLS-1$
					simpleJar = true;
				else
					// name does not match the target properly with an (_|-) version at the end
					continue;
			}
			// Note: directory with version suffix is always > than directory without version suffix
			String version = candidateName.length() > target.length() + 1 && (versionSep == '_' || versionSep == '-') ? candidateName.substring(target.length() + 1) : ""; //$NON-NLS-1$ 
			Object[] currentVersion = getVersionElements(version);
			if (currentVersion != null && compareVersion(maxVersion, currentVersion) < 0) {
				File candidate = new File(start, candidateName);
				boolean candidateIsFile = candidate.isFile();
				// if simple jar; make sure it is really a file before accepting it
				if (!simpleJar || candidateIsFile) {
					result = candidate.getAbsolutePath();
					resultIsFile = candidateIsFile;
					maxVersion = currentVersion;
				}
			}
		}
		if (result == null)
			return null;
		return result.replace(File.separatorChar, '/') + (resultIsFile ? "" : "/"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier).  A value of null is returned if there are no valid Integers.  Note, that 
	 * returning anything else will cause exceptions in the caller.
	 */
	private static Object[] getVersionElements(String version) {
		Object[] result = {new Integer(-1), new Integer(-1), new Integer(-1), ""}; //$NON-NLS-1$
		StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
		String token;
		for (int i = 0; t.hasMoreTokens() && i < 4; i++) {
			token = t.nextToken();
			if (i < 3) {
				// major, minor or service ... numeric values
				try {
					result[i] = new Integer(token);
				} catch (Exception e) {
					if (i == 0)
						return null; // return null if no valid numbers are present
					// invalid number format - use default numbers (-1) for the rest
					break;
				}
			} else {
				// qualifier ... string value
				result[i] = token;
			}
		}
		return result;
	}

	/**
	 * Compares version strings. 
	 * @return result of comparison, as integer;
	 * <code><0</code> if left < right;
	 * <code>0</code> if left == right;
	 * <code>>0</code> if left > right;
	 */
	private static int compareVersion(Object[] left, Object[] right) {
		if (left == null)
			return -1;
		int result = ((Integer) left[0]).compareTo((Integer) right[0]); // compare major
		if (result != 0)
			return result;

		result = ((Integer) left[1]).compareTo((Integer) right[1]); // compare minor
		if (result != 0)
			return result;

		result = ((Integer) left[2]).compareTo((Integer) right[2]); // compare service
		if (result != 0)
			return result;

		return ((String) left[3]).compareTo((String) right[3]); // compare qualifier
	}

	private static void finalizeProperties() {
		// if check config is unknown and we are in dev mode, 
		if (FrameworkProperties.getProperty(PROP_DEV) != null && FrameworkProperties.getProperty(PROP_CHECK_CONFIG) == null)
			FrameworkProperties.setProperty(PROP_CHECK_CONFIG, "true"); //$NON-NLS-1$
	}

	private static class InitialBundle {
		public final String locationString;
		public final URL location;
		public final int level;
		public final boolean start;

		InitialBundle(String locationString, URL location, int level, boolean start) {
			this.locationString = locationString;
			this.location = location;
			this.level = level;
			this.start = start;
		}
	}

	/**
	 * Sets the initial properties for the platform.  
	 * This method must be called before calling the {@link  #run(String[], Runnable)} or 
	 * {@link #startup(String[], Runnable)} methods for the properties to be used in 
	 * a launched instance of the platform.  
	 * <p>
	 * If the specified properties contains a null value then the key for that value 
	 * will be cleared from the properties of the platform.
	 * </p>
	 * @param initialProperties the initial properties to set for the platform.
	 * @since 3.2
	 */
	public static void setInitialProperties(Map<String, String> initialProperties) {
		if (initialProperties == null || initialProperties.isEmpty())
			return;
		for (Map.Entry<String, String> entry : initialProperties.entrySet()) {
			if (entry.getValue() != null)
				FrameworkProperties.setProperty(entry.getKey(), entry.getValue());
			else
				FrameworkProperties.clearProperty(entry.getKey());
		}
	}

	/**
	 * Returns the context of the system bundle.  A value of 
	 * <code>null</code> is returned if the platform is not running.
	 * @return the context of the system bundle
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         appropriate <code>AdminPermission[system.bundle,CONTEXT]</code>, and
	 *         the Java Runtime Environment supports permissions.
	 */
	public static BundleContext getSystemBundleContext() {
		if (context == null || !running)
			return null;
		return context.getBundle().getBundleContext();
	}

	private static boolean isForcedRestart() {
		return Boolean.valueOf(FrameworkProperties.getProperty(PROP_FORCED_RESTART)).booleanValue();
	}

	/*
	 * NOTE: This is an internal/experimental method used by launchers that need to react when the framework
	 * is shutdown internally.
	 * 
	 * Adds a framework shutdown handler. <p>
	 * A handler implements the {@link Runnable} interface.  When the framework is shutdown
	 * the {@link Runnable#run()} method is called for each registered handler.  Handlers should 
	 * make no assumptions on the thread it is being called from.  If a handler object is 
	 * registered multiple times it will be called once for each registration.
	 * <p>
	 * At the time a handler is called the framework is shutdown.  Handlers must not depend on 
	 * a running framework to execute or attempt to load additional classes from bundles 
	 * installed in the framework.
	 * @param handler the framework shutdown handler
	 * @throws IllegalStateException if the platform is already running
	 */
	static void internalAddFrameworkShutdownHandler(Runnable handler) {
		if (running)
			throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);

		if (shutdownHandlers == null)
			shutdownHandlers = new ArrayList<Runnable>();

		shutdownHandlers.add(handler);
	}

	/*
	 * NOTE: This is an internal/experimental method used by launchers that need to react when the framework
	 * is shutdown internally.
	 * 
	 * Removes a framework shutdown handler. <p>
	 * @param handler the framework shutdown handler
	 * @throws IllegalStateException if the platform is already running
	 */
	static void internalRemoveFrameworkShutdownHandler(Runnable handler) {
		if (running)
			throw new IllegalStateException(EclipseAdaptorMsg.ECLIPSE_STARTUP_ALREADY_RUNNING);

		if (shutdownHandlers != null)
			shutdownHandlers.remove(handler);
	}

	private static void registerFrameworkShutdownHandlers() {
		if (shutdownHandlers == null)
			return;

		final Bundle systemBundle = context.getBundle();
		for (Iterator<Runnable> it = shutdownHandlers.iterator(); it.hasNext();) {
			final Runnable handler = it.next();
			BundleListener listener = new BundleListener() {
				public void bundleChanged(BundleEvent event) {
					if (event.getBundle() == systemBundle && event.getType() == BundleEvent.STOPPED) {
						handler.run();
					}
				}
			};
			context.addBundleListener(listener);
		}
	}
}
