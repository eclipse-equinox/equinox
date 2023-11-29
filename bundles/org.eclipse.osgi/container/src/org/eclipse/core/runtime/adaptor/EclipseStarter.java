/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt (bug 172969)
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.eclipse.osgi.storage.url.reference.Handler;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
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
	private static BundleContext context;
	private static boolean initialize = false;
	public static boolean debug = false;
	private static boolean running = false;
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

	public static final String PROP_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	public static final String PROP_EXITDATA = "eclipse.exitdata"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_LOG = "eclipse.consoleLog"; //$NON-NLS-1$
	public static final String PROP_IGNOREAPP = "eclipse.ignoreApp"; //$NON-NLS-1$
	public static final String PROP_REFRESH_BUNDLES = "eclipse.refreshBundles"; //$NON-NLS-1$
	public static final String PROP_ALLOW_APPRELAUNCH = "eclipse.allowAppRelaunch"; //$NON-NLS-1$
	private static final String PROP_APPLICATION_LAUNCHDEFAULT = "eclipse.application.launchDefault"; //$NON-NLS-1$

	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$
	private static final String REFERENCE_SCHEME = "reference:"; //$NON-NLS-1$
	private static final String REFERENCE_PROTOCOL = "reference"; //$NON-NLS-1$
	private static final String INITIAL_LOCATION = "initial@"; //$NON-NLS-1$

	private static final int DEFAULT_INITIAL_STARTLEVEL = 6; // default value for legacy purposes
	private static final String DEFAULT_BUNDLES_STARTLEVEL = "4"; //$NON-NLS-1$

	private static FrameworkLog log;
	// directory of serch candidates keyed by directory abs path -> directory listing (bug 122024)
	private static Map<String, String[]> searchCandidates = new HashMap<>(4);
	private static EclipseAppLauncher appLauncher;
	private static List<Runnable> shutdownHandlers;

	private static ConsoleManager consoleMgr = null;

	private static Map<String, String> configuration = null;
	private static Framework framework = null;
	private static EquinoxConfiguration equinoxConfig;
	private static String[] allArgs = null;
	private static String[] frameworkArgs = null;
	private static String[] appArgs = null;

	private synchronized static String getProperty(String key) {
		if (equinoxConfig != null) {
			return equinoxConfig.getConfiguration(key);
		}
		return getConfiguration().get(key);
	}

	private synchronized static String getProperty(String key, String dft) {
		if (equinoxConfig != null) {
			return equinoxConfig.getConfiguration(key, dft);
		}
		String result = getConfiguration().get(key);
		return result == null ? dft : result;
	}

	private synchronized static Object setProperty(String key, String value) {
		if (equinoxConfig != null) {
			return equinoxConfig.setProperty(key, value);
		}
		if ("true".equals(getConfiguration().get(EquinoxConfiguration.PROP_USE_SYSTEM_PROPERTIES))) { //$NON-NLS-1$
			System.setProperty(key, value);
		}
		return getConfiguration().put(key, value);
	}

	private synchronized static Object clearProperty(String key) {
		if (equinoxConfig != null) {
			return equinoxConfig.clearConfiguration(key);
		}
		return getConfiguration().remove(key);
	}

	private synchronized static Map<String, String> getConfiguration() {
		if (configuration == null) {
			configuration = new HashMap<>();
			// TODO hack to set these to defaults for EclipseStarter
			// Note that this hack does not allow this property to be specified in config.ini
			configuration.put(EquinoxConfiguration.PROP_USE_SYSTEM_PROPERTIES, System.getProperty(EquinoxConfiguration.PROP_USE_SYSTEM_PROPERTIES, "true")); //$NON-NLS-1$
			// we handle this compatibility setting special for EclipseStarter
			String systemCompatibilityBoot = System.getProperty(EquinoxConfiguration.PROP_COMPATIBILITY_BOOTDELEGATION);
			if (systemCompatibilityBoot != null) {
				// The system properties have a specific setting; use it
				configuration.put(EquinoxConfiguration.PROP_COMPATIBILITY_BOOTDELEGATION, systemCompatibilityBoot);
			} else {
				// set a default value; but this value can be overriden by the config.ini
				configuration.put(EquinoxConfiguration.PROP_COMPATIBILITY_BOOTDELEGATION + EquinoxConfiguration.PROP_DEFAULT_SUFFIX, "true"); //$NON-NLS-1$
			}

			String dsDelayedKeepInstances = System.getProperty(EquinoxConfiguration.PROP_DS_DELAYED_KEEPINSTANCES);
			if (dsDelayedKeepInstances != null) {
				// The system properties have a specific setting; use it
				configuration.put(EquinoxConfiguration.PROP_DS_DELAYED_KEEPINSTANCES, dsDelayedKeepInstances);
			} else {
				// set a default value; but this value can be overriden by the config.ini
				configuration.put(EquinoxConfiguration.PROP_DS_DELAYED_KEEPINSTANCES + EquinoxConfiguration.PROP_DEFAULT_SUFFIX, "true"); //$NON-NLS-1$
			}
		}
		return configuration;
	}

	/**
	 * This is the main to start osgi.
	 * It only works when the framework is being jared as a single jar
	 */
	public static void main(String[] args) throws Exception {
		if (getProperty("eclipse.startTime") == null) //$NON-NLS-1$
			setProperty("eclipse.startTime", Long.toString(System.currentTimeMillis())); //$NON-NLS-1$
		if (getProperty(PROP_NOSHUTDOWN) == null)
			setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
		// set the compatibility boot delegation flag to false to get "standard" OSGi behavior WRT boot delegation (bug 178477)
		if (getProperty(EquinoxConfiguration.PROP_COMPATIBILITY_BOOTDELEGATION) == null)
			setProperty(EquinoxConfiguration.PROP_COMPATIBILITY_BOOTDELEGATION, "false"); //$NON-NLS-1$
		Object result = run(args, null);
		if (result instanceof Integer && !Boolean.valueOf(getProperty(PROP_NOSHUTDOWN)).booleanValue())
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
		if (running)
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ALREADY_RUNNING);
		boolean startupFailed = true;
		try {
			startup(args, endSplashHandler);
			startupFailed = false;
			if (Boolean.valueOf(getProperty(PROP_IGNOREAPP)).booleanValue() || isForcedRestart())
				return null;
			return run(null);
		} catch (Throwable e) {
			// ensure the splash screen is down
			if (endSplashHandler != null)
				endSplashHandler.run();
			// may use startupFailed to understand where the error happened
			FrameworkLogEntry logEntry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, startupFailed ? Msg.ECLIPSE_STARTUP_STARTUP_ERROR : Msg.ECLIPSE_STARTUP_APP_ERROR, 1, e, null);
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
					setProperty(PROP_EXITCODE, "23"); //$NON-NLS-1$
				if (!Boolean.valueOf(getProperty(PROP_NOSHUTDOWN)).booleanValue())
					shutdown();
			} catch (Throwable e) {
				FrameworkLogEntry logEntry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, Msg.ECLIPSE_STARTUP_SHUTDOWN_ERROR, 1, e, null);
				if (log != null)
					log.log(logEntry);
				else
					// TODO desperate measure - ideally, we should write this to disk (a la Main.log)
					e.printStackTrace();
			}
		}
		// we only get here if an error happened
		if (getProperty(PROP_EXITCODE) == null) {
			setProperty(PROP_EXITCODE, "13"); //$NON-NLS-1$
			setProperty(PROP_EXITDATA, NLS.bind(Msg.ECLIPSE_STARTUP_ERROR_CHECK_LOG, log == null ? null : log.getFile().getPath()));
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
		if (running)
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ALREADY_RUNNING);
		processCommandLine(args);
		framework = new Equinox(getConfiguration());
		framework.init();
		context = framework.getBundleContext();
		ServiceReference<FrameworkLog> logRef = context.getServiceReference(FrameworkLog.class);
		log = context.getService(logRef);
		ServiceReference<EnvironmentInfo> configRef = context.getServiceReference(EnvironmentInfo.class);
		equinoxConfig = (EquinoxConfiguration) context.getService(configRef);

		equinoxConfig.setAllArgs(allArgs);
		equinoxConfig.setFrameworkArgs(frameworkArgs);
		equinoxConfig.setAppArgs(appArgs);

		registerFrameworkShutdownHandlers();
		publishSplashScreen(endSplashHandler);
		consoleMgr = ConsoleManager.startConsole(context, equinoxConfig);

		Bundle[] startBundles = loadBasicBundles();

		if (startBundles == null || ("true".equals(getProperty(PROP_REFRESH_BUNDLES)) && refreshPackages(getCurrentBundles(false)))) { //$NON-NLS-1$
			waitForShutdown();
			return context; // cannot continue; loadBasicBundles caused refreshPackages to shutdown the framework
		}

		framework.start();

		if (isForcedRestart()) {
			waitForShutdown();
			return context;
		}
		// set the framework start level to the ultimate value.  This will actually start things
		// running if they are persistently active.
		setStartLevel(getStartLevel());
		// they should all be active by this time
		ensureBundlesActive(startBundles);

		// in the case where the built-in console is disabled we should try to start the console bundle
		try {
			consoleMgr.checkForConsoleBundle();
		} catch (BundleException e) {
			FrameworkLogEntry entry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null);
			log.log(entry);
		}
		// TODO should log unresolved bundles if in debug or dev mode
		running = true;
		return context;
	}

	private static int getStartLevel() {
		String level = getProperty(PROP_INITIAL_STARTLEVEL);
		if (level != null)
			try {
				return Integer.parseInt(level);
			} catch (NumberFormatException e) {
				if (debug)
					Debug.println("Start level = " + level + "  parsed. Using hardcoded default: 6"); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (!running)
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_NOT_RUNNING);
		// if we are just initializing, do not run the application just return.
		if (initialize)
			return Integer.valueOf(0);
		try {
			if (appLauncher == null) {

				boolean launchDefault = Boolean.parseBoolean(getProperty(PROP_APPLICATION_LAUNCHDEFAULT, "true")); //$NON-NLS-1$
				// create the ApplicationLauncher and register it as a service
				appLauncher = new EclipseAppLauncher(context, Boolean.parseBoolean(getProperty(PROP_ALLOW_APPRELAUNCH)), launchDefault, log, equinoxConfig);
				appLauncherRegistration = context.registerService(ApplicationLauncher.class.getName(), appLauncher, null);
				// must start the launcher AFTER service restration because this method
				// blocks and runs the application on the current thread.  This method
				// will return only after the application has stopped.
				return appLauncher.start(argument);
			}
			return appLauncher.reStart(argument);
		} catch (Exception e) {
			if (log != null && context != null) { // context can be null if OSGi failed to launch (bug 151413)
				ResolutionReport report = context.getBundle().adapt(Module.class).getContainer().resolve(null, false);
				for (Resource unresolved : report.getEntries().keySet()) {
					String bsn = ((ModuleRevision) unresolved).getSymbolicName();
					FrameworkLogEntry logEntry = new FrameworkLogEntry(bsn != null ? bsn : EquinoxContainer.NAME, FrameworkLogEntry.WARNING, 0, Msg.Module_ResolveError + report.getResolutionReportMessage(unresolved), 1, null, null);
					log.log(logEntry);
				}
			}
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
		if (framework.getState() == Bundle.ACTIVE) {
			if (appLauncherRegistration != null)
				appLauncherRegistration.unregister();
			if (splashStreamRegistration != null)
				splashStreamRegistration.unregister();
			if (defaultMonitorRegistration != null)
				defaultMonitorRegistration.unregister();
		}
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
		if (framework.getState() == Bundle.ACTIVE) {
			framework.stop();
			framework.waitForStop(0);
			framework = null;
		}
		configuration = null;
		equinoxConfig = null;
		context = null;
		running = false;
	}

	private static void ensureBundlesActive(Bundle[] bundles) {
		for (Bundle bundle : bundles) {
			if (bundle.getState() != Bundle.ACTIVE) {
				if (bundle.getState() == Bundle.INSTALLED) {
					// Log that the bundle is not resolved
					log.log(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundle.getLocation()), 0, null, null));
					continue;
				}
				// check that the startlevel allows the bundle to be active (111550)
				FrameworkStartLevel fwStartLevel = context.getBundle().adapt(FrameworkStartLevel.class);
				BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
				if (fwStartLevel != null && (bundleStartLevel.getStartLevel() <= fwStartLevel.getStartLevel())) {
					log.log(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_ACTIVE, bundle), 0, null, null));
				}
			}
		}
	}

	private static void publishSplashScreen(final Runnable endSplashHandler) {
		if (endSplashHandler == null)
			return;
		// register the output stream to the launcher if it exists
		try {
			Method method = endSplashHandler.getClass().getMethod("getOutputStream", new Class[0]); //$NON-NLS-1$
			Object outputStream = method.invoke(endSplashHandler, new Object[0]);
			if (outputStream instanceof OutputStream) {
				Dictionary<String, Object> osProperties = new Hashtable<>();
				osProperties.put("name", "splashstream"); //$NON-NLS-1$//$NON-NLS-2$
				splashStreamRegistration = context.registerService(OutputStream.class.getName(), outputStream, osProperties);
			}
		} catch (Exception ex) {
			// ignore
		}
		// keep this splash handler as the default startup monitor
		try {
			Dictionary<String, Object> monitorProps = new Hashtable<>();
			monitorProps.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
			defaultMonitorRegistration = context.registerService(StartupMonitor.class.getName(), new DefaultStartupMonitor(endSplashHandler, equinoxConfig), monitorProps);
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
			createURL(name); // quick check to see if the name is a valid URL
			url = createURL(new File(parent).toURL(), name);
		} catch (MalformedURLException e) {
			// TODO this is legacy support for non-URL names.  It should be removed eventually.
			// if name was not a URL then construct one.
			// Assume it should be a reference and that it is relative.  This support need not
			// be robust as it is temporary..
			File child = new File(name);
			fileLocation = child.isAbsolute() ? child : new File(parent, name);
			url = createURL(REFERENCE_PROTOCOL, null, fileLocation.toURL().toExternalForm());
			reference = true;
		}
		// if the name was a URL then see if it is relative.  If so, insert syspath.
		if (!reference) {
			URL baseURL = url;
			// if it is a reference URL then strip off the reference: and set base to the file:...
			if (url.getProtocol().equals(REFERENCE_PROTOCOL)) {
				reference = true;
				String baseSpec = url.getPath();
				if (baseSpec.startsWith(FILE_SCHEME)) {
					File child = new File(baseSpec.substring(5));
					baseURL = child.isAbsolute() ? child.toURL() : new File(parent, child.getPath()).toURL();
				} else
					baseURL = createURL(baseSpec);
			}

			fileLocation = new File(baseURL.getPath());
			// if the location is relative, prefix it with the parent
			if (!fileLocation.isAbsolute())
				fileLocation = new File(parent, fileLocation.toString());
		}
		// If the result is a reference then search for the real result and
		// reconstruct the answer.
		if (reference) {
			String result = searchFor(fileLocation.getName(), new File(fileLocation.getParent()).getAbsolutePath());
			if (result != null)
				url = createURL(REFERENCE_PROTOCOL, null, FILE_SCHEME + result);
			else
				return null;
		}

		// finally we have something worth trying
		try {
			URLConnection result = LocationHelper.getConnection(url);
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
	private static Bundle[] loadBasicBundles() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		String osgiBundles = getProperty(PROP_BUNDLES);
		String osgiExtensions = getProperty(PROP_EXTENSIONS);
		if (osgiExtensions != null && osgiExtensions.length() > 0) {
			osgiBundles = osgiExtensions + ',' + osgiBundles;
			setProperty(PROP_BUNDLES, osgiBundles);
		}
		String[] installEntries = getArrayFromList(osgiBundles, ","); //$NON-NLS-1$
		// get the initial bundle list from the installEntries
		InitialBundle[] initialBundles = getInitialBundles(installEntries);
		// get the list of currently installed initial bundles from the framework
		Bundle[] curInitBundles = getCurrentBundles(true);

		// list of bundles to be refreshed
		List<Bundle> toRefresh = new ArrayList<>(curInitBundles.length);
		// uninstall any of the currently installed bundles that do not exist in the
		// initial bundle list from installEntries.
		uninstallBundles(curInitBundles, initialBundles, toRefresh);

		// install the initialBundles that are not already installed.
		List<Bundle> startBundles = new ArrayList<>(installEntries.length);
		List<Bundle> lazyActivationBundles = new ArrayList<>(installEntries.length);
		installBundles(initialBundles, curInitBundles, startBundles, lazyActivationBundles, toRefresh);

		// If we installed/uninstalled something, force a refresh of all installed/uninstalled bundles
		if (!toRefresh.isEmpty() && refreshPackages(toRefresh.toArray(new Bundle[toRefresh.size()])))
			return null; // cannot continue; refreshPackages shutdown the framework

		// schedule all basic bundles to be started
		Bundle[] startInitBundles = startBundles.toArray(new Bundle[startBundles.size()]);
		Bundle[] lazyInitBundles = lazyActivationBundles.toArray(new Bundle[lazyActivationBundles.size()]);
		startBundles(startInitBundles, lazyInitBundles);

		if (debug)
			Debug.println("Time to load bundles: " + (System.currentTimeMillis() - startTime)); //$NON-NLS-1$
		return startInitBundles;
	}

	private static InitialBundle[] getInitialBundles(String[] installEntries) {
		searchCandidates.clear();
		List<InitialBundle> result = new ArrayList<>(installEntries.length);
		int defaultStartLevel = Integer.parseInt(getProperty(PROP_BUNDLES_STARTLEVEL, DEFAULT_BUNDLES_STARTLEVEL));
		String syspath = getSysPath();
		// should canonicalize the syspath.
		try {
			syspath = new File(syspath).getCanonicalPath();
		} catch (IOException ioe) {
			// do nothing
		}
		Collection<ServiceReference<Location>> installLocRef;
		try {
			installLocRef = context.getServiceReferences(Location.class, Location.INSTALL_FILTER);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		Location installLocation = installLocRef == null ? null : context.getService(installLocRef.iterator().next());
		if (installLocation == null) {
			throw new IllegalStateException(Msg.EclipseStarter_InstallLocation);
		}
		for (String name : installEntries) {
			int level = defaultStartLevel;
			boolean start = false;
			int index = name.lastIndexOf('@');
			if (index >= 0) {
				String[] attributes = getArrayFromList(name.substring(index + 1, name.length()), ":"); //$NON-NLS-1$
				for (String attribute : attributes) {
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
					FrameworkLogEntry entry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_BUNDLE_NOT_FOUND, name), 0, null, null);
					log.log(entry);
					// skip this entry
					continue;
				}
				location = makeRelative(installLocation.getURL(), location);
				String locationString = INITIAL_LOCATION + location.toExternalForm();
				result.add(new InitialBundle(locationString, location, level, start));
			}catch (IOException e) {
				log.log(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null));
			}
		}
		return result.toArray(new InitialBundle[result.size()]);
	}

	// returns true if the refreshPackages operation caused the framework to shutdown
	private static boolean refreshPackages(Bundle[] bundles) throws InterruptedException {
		FrameworkWiring frameworkWiring = context.getBundle().adapt(FrameworkWiring.class);
		if (frameworkWiring == null)
			return false;
		Semaphore semaphore = new Semaphore(0);
		StartupEventListener listener = new StartupEventListener(semaphore, FrameworkEvent.PACKAGES_REFRESHED);
		context.addBundleListener(listener);
		frameworkWiring.refreshBundles(Arrays.asList(bundles), listener);
		updateSplash(semaphore, listener);
		return isForcedRestart();
	}

	private static void waitForShutdown() {
		// wait for the system bundle to stop
		try {
			framework.waitForStop(0);
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		}
	}

	private static void processCommandLine(String[] args) throws Exception {
		allArgs = args;
		if (args.length == 0) {
			frameworkArgs = args;
			return;
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
				setProperty(PROP_DEBUG, ""); //$NON-NLS-1$
				debug = true;
				found = true;
			}

			// check if development mode should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -),
			// simply enable development mode.  Otherwise, assume that that the following arg is
			// actually some additional development time class path entries.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				setProperty(PROP_DEV, ""); //$NON-NLS-1$
				found = true;
			}

			// look for the initialization arg
			if (args[i].equalsIgnoreCase(INITIALIZE)) {
				initialize = true;
				found = true;
			}

			// look for the clean flag.
			if (args[i].equalsIgnoreCase(CLEAN)) {
				setProperty(PROP_CLEAN, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the consoleLog flag
			if (args[i].equalsIgnoreCase(CONSOLE_LOG)) {
				setProperty(PROP_CONSOLE_LOG, "true"); //$NON-NLS-1$
				found = true;
			}

			// look for the console with no port.
			if (args[i].equalsIgnoreCase(CONSOLE) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				setProperty(PROP_CONSOLE, ""); //$NON-NLS-1$
				found = true;
			}

			if (args[i].equalsIgnoreCase(NOEXIT)) {
				setProperty(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
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
				setProperty(PROP_CONSOLE, arg);
				found = true;
			}

			// look for the configuration location .
			if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
				setProperty(EquinoxLocations.PROP_CONFIG_AREA, arg);
				found = true;
			}

			// look for the data location for this instance.
			if (args[i - 1].equalsIgnoreCase(DATA)) {
				setProperty(EquinoxLocations.PROP_INSTANCE_AREA, arg);
				found = true;
			}

			// look for the user location for this instance.
			if (args[i - 1].equalsIgnoreCase(USER)) {
				setProperty(EquinoxLocations.PROP_USER_AREA, arg);
				found = true;
			}

			// look for the launcher location
			if (args[i - 1].equalsIgnoreCase(LAUNCHER)) {
				setProperty(EquinoxLocations.PROP_LAUNCHER, arg);
				found = true;
			}
			// look for the development mode and class path entries.
			if (args[i - 1].equalsIgnoreCase(DEV)) {
				setProperty(PROP_DEV, arg);
				found = true;
			}

			// look for the debug mode and option file location.
			if (args[i - 1].equalsIgnoreCase(DEBUG)) {
				setProperty(PROP_DEBUG, arg);
				debug = true;
				found = true;
			}

			// look for the window system.
			if (args[i - 1].equalsIgnoreCase(WS)) {
				setProperty(PROP_WS, arg);
				found = true;
			}

			// look for the operating system
			if (args[i - 1].equalsIgnoreCase(OS)) {
				setProperty(PROP_OS, arg);
				found = true;
			}

			// look for the system architecture
			if (args[i - 1].equalsIgnoreCase(ARCH)) {
				setProperty(PROP_ARCH, arg);
				found = true;
			}

			// look for the nationality/language
			if (args[i - 1].equalsIgnoreCase(NL)) {
				setProperty(PROP_NL, arg);
				found = true;
			}

			// look for the locale extensions
			if (args[i - 1].equalsIgnoreCase(NL_EXTENSIONS)) {
				setProperty(PROP_NL_EXTENSIONS, arg);
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
			frameworkArgs = new String[0];
			appArgs = args;
			return;
		}
		appArgs = new String[args.length - configArgIndex];
		frameworkArgs = new String[configArgIndex];
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
		return;
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
		String result = getProperty(PROP_SYSPATH);
		if (result != null)
			return result;
		result = getSysPathFromURL(getProperty(PROP_FRAMEWORK));
		if (result == null)
			result = getSysPathFromCodeSource();
		if (result == null)
			throw new IllegalStateException("Can not find the system path."); //$NON-NLS-1$
		if (Character.isUpperCase(result.charAt(0))) {
			char[] chars = result.toCharArray();
			chars[0] = Character.toLowerCase(chars[0]);
			result = new String(chars);
		}
		setProperty(PROP_SYSPATH, result);
		return result;
	}

	private static String getSysPathFromURL(String urlSpec) {
		if (urlSpec == null)
			return null;
		URL url = LocationHelper.buildURL(urlSpec, false);
		if (url == null)
			return null;
		File fwkFile = LocationHelper.decodePath(new File(url.getPath()));
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
		String result = url.getPath();
		if (File.separatorChar == '\\') {
			// in case on windows the \ is used
			result = result.replace('\\', '/');
		}
		if (result.endsWith(".jar")) { //$NON-NLS-1$
			result = result.substring(0, result.lastIndexOf('/'));
			if ("folder".equals(getProperty(PROP_FRAMEWORK_SHAPE))) //$NON-NLS-1$
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
		List<Bundle> initial = new ArrayList<>();
		for (Bundle bundle : installed) {
			if (bundle.getLocation().startsWith(INITIAL_LOCATION)) {
				if (includeInitial)
					initial.add(bundle);
			} else if (!includeInitial && bundle.getBundleId() != 0)
				initial.add(bundle);
		}
		return initial.toArray(new Bundle[initial.size()]);
	}

	private static Bundle getBundleByLocation(String location, Bundle[] bundles) {
		for (Bundle bundle : bundles) {
			if (location.equalsIgnoreCase(bundle.getLocation()))
				return bundle;
		}
		return null;
	}

	private static void uninstallBundles(Bundle[] curInitBundles, InitialBundle[] newInitBundles, List<Bundle> toRefresh) {
		for (Bundle curInitBundle : curInitBundles) {
			boolean found = false;
			for (InitialBundle newInitBundle : newInitBundles) {
				if (curInitBundle.getLocation().equalsIgnoreCase(newInitBundle.locationString)) {
					found = true;
					break;
				}
			}
			if (!found) {
				try {
					curInitBundle.uninstall();
					toRefresh.add(curInitBundle);
				} catch (BundleException e) {
					FrameworkLogEntry entry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_FAILED_UNINSTALL, curInitBundle.getLocation()), 0, e, null);
					log.log(entry);
				}
			}
		}
	}

	private static void installBundles(InitialBundle[] initialBundles, Bundle[] curInitBundles, List<Bundle> startBundles, List<Bundle> lazyActivationBundles, List<Bundle> toRefresh) {
		for (InitialBundle initialBundle : initialBundles) {
			Bundle osgiBundle = getBundleByLocation(initialBundle.locationString, curInitBundles);
			try {
				// don't need to install if it is already installed
				if (osgiBundle == null) {
					InputStream in = LocationHelper.getStream(initialBundle.location);
					try {
						osgiBundle = context.installBundle(initialBundle.locationString, in);
					}catch (BundleException e) {
						if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
							continue;
							// TODO should attempt to lookup the existing bundle
						}
						throw e;
					}
					// only check for lazy activation header if this is a newly installed bundle and is not marked for persistent start
					if (!initialBundle.start && hasLazyActivationPolicy(osgiBundle)) {
						lazyActivationBundles.add(osgiBundle);
					}
				}
				// always set the startlevel incase it has changed (bug 111549)
				// this is a no-op if the level is the same as previous launch.
				if ((osgiBundle.getState() & Bundle.UNINSTALLED) == 0 && initialBundle.level >= 0) {
					osgiBundle.adapt(BundleStartLevel.class).setStartLevel(initialBundle.level);
				}
				// if this bundle is supposed to be started then add it to the start list
				if (initialBundle.start) {
					startBundles.add(osgiBundle);
				}
				// include basic bundles in case they were not resolved before
				if ((osgiBundle.getState() & Bundle.INSTALLED) != 0)
					toRefresh.add(osgiBundle);
			} catch (BundleException | IOException e) {
				FrameworkLogEntry entry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_FAILED_INSTALL, initialBundle.location), 0, e, null);
				log.log(entry);
			}
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
				String eclipseLazyStart = headers.get(EquinoxModuleDataNamespace.LAZYSTART_HEADER);
				if (eclipseLazyStart == null)
					eclipseLazyStart = headers.get(EquinoxModuleDataNamespace.AUTOSTART_HEADER);
				ManifestElement[] elements = ManifestElement.parseHeader(EquinoxModuleDataNamespace.AUTOSTART_HEADER, eclipseLazyStart);
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
		for (Bundle startBundle : startBundles) {
			startBundle(startBundle, 0);
		}
		for (Bundle lazyBundle : lazyBundles) {
			startBundle(lazyBundle, Bundle.START_ACTIVATION_POLICY);
		}
	}

	private static void startBundle(Bundle bundle, int options) {
		try {
			bundle.start(options);
		} catch (BundleException e) {
			if ((bundle.getState() & Bundle.RESOLVED) != 0) {
				// only log errors if the bundle is resolved
				FrameworkLogEntry entry = new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, NLS.bind(Msg.ECLIPSE_STARTUP_FAILED_START, bundle.getLocation()), 0, e, null);
				log.log(entry);
			}
		}
	}

	/**
	 * Returns a URL which is equivalent to the given URL relative to the
	 * specified base URL. Works only for file: URLs
	 */
	private static URL makeRelative(URL base, URL location) throws MalformedURLException {
		if (base == null)
			return location;
		if (!"file".equals(base.getProtocol())) //$NON-NLS-1$
			return location;
		if (!location.getProtocol().equals(REFERENCE_PROTOCOL))
			return location; // we can only make reference urls relative
		URL nonReferenceLocation = createURL(location.getPath());
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
		URL relativeURL = createURL(base.getProtocol(), base.getHost(), base.getPort(), urlPath);
		// now make it back to a reference URL
		relativeURL = createURL(REFERENCE_SCHEME + relativeURL.toExternalForm());
		return relativeURL;
	}

	private static URL createURL(String spec) throws MalformedURLException {
		return createURL(null, spec);
	}

	private static URL createURL(URL urlContext, String spec) throws MalformedURLException {
		if (context != null && spec.startsWith(REFERENCE_SCHEME)) {
			return new URL(urlContext, spec, new Handler(context.getProperty(EquinoxLocations.PROP_INSTALL_AREA)));
		}
		return new URL(urlContext, spec);
	}

	private static URL createURL(String protocol, String host, String file) throws MalformedURLException {
		return createURL(protocol, host, -1, file);
	}

	private static URL createURL(String protocol, String host, int port, String file) throws MalformedURLException {
		if (context != null && REFERENCE_PROTOCOL.equalsIgnoreCase(protocol)) {
			return new URL(protocol, host, port, file, new Handler(context.getProperty(EquinoxLocations.PROP_INSTALL_AREA)));
		}
		return new URL(protocol, host, port, file);
	}

	private static File makeRelative(File base, File location) {
		if (!location.isAbsolute())
			return location;
		File relative = new File(new FilePath(base).makeRelative(new FilePath(location)));
		return relative;
	}

	private static void setStartLevel(final int value) throws InterruptedException {
		FrameworkStartLevel fwkStartLevel = context.getBundle().adapt(FrameworkStartLevel.class);
		final Semaphore semaphore = new Semaphore(0);
		StartupEventListener listener = new StartupEventListener(semaphore, FrameworkEvent.STARTLEVEL_CHANGED);
		context.addBundleListener(listener);
		fwkStartLevel.setStartLevel(value, listener);
		updateSplash(semaphore, listener);
	}

	static class StartupEventListener implements SynchronousBundleListener, FrameworkListener {
		private final Semaphore semaphore;
		private final int frameworkEventType;

		public StartupEventListener(Semaphore semaphore, int frameworkEventType) {
			this.semaphore = semaphore;
			this.frameworkEventType = frameworkEventType;
		}

		@Override
		public void bundleChanged(BundleEvent event) {
			if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STOPPING)
				semaphore.release();
		}

		@Override
		public void frameworkEvent(FrameworkEvent event) {
			if (event.getType() == frameworkEventType)
				semaphore.release();
		}

	}

	private static void updateSplash(Semaphore semaphore, StartupEventListener listener) throws InterruptedException {
		ServiceTracker<StartupMonitor, StartupMonitor> monitorTracker = new ServiceTracker<>(context, StartupMonitor.class.getName(), null);
		try {
			monitorTracker.open();
		} catch (IllegalStateException e) {
			// do nothing; this can happen if the framework shutdown
			return;
		}
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
				if (semaphore.tryAcquire(50, TimeUnit.MILLISECONDS))
					break; //done
				//else still working, spin another update
			}
		} finally {
			if (listener != null) {
				try {
					context.removeBundleListener(listener);
					monitorTracker.close();
				} catch (IllegalStateException e) {
					// do nothing; this can happen if the framework shutdown
				}
			}
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
			startFile = LocationHelper.decodePath(startFile);
			candidates = startFile.list();
			if (candidates != null)
				searchCandidates.put(start, candidates);
		}
		if (candidates == null)
			return null;
		String result = null;
		Object[] maxVersion = null;
		boolean resultIsFile = false;
		for (String candidateName : candidates) {
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
		Object[] result = {Integer.valueOf(-1), Integer.valueOf(-1), Integer.valueOf(-1), ""}; //$NON-NLS-1$
		StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
		String token;
		for (int i = 0; t.hasMoreTokens() && i < 4; i++) {
			token = t.nextToken();
			if (i < 3) {
				// major, minor or service ... numeric values
				try {
					result[i] = Integer.valueOf(token);
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
				setProperty(entry.getKey(), entry.getValue());
			else
				clearProperty(entry.getKey());
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
		return Boolean.valueOf(getProperty(EquinoxConfiguration.PROP_FORCED_RESTART)).booleanValue();
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
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ALREADY_RUNNING);

		if (shutdownHandlers == null)
			shutdownHandlers = new ArrayList<>();

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
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ALREADY_RUNNING);

		if (shutdownHandlers != null)
			shutdownHandlers.remove(handler);
	}

	private static void registerFrameworkShutdownHandlers() {
		if (shutdownHandlers == null)
			return;

		final Bundle systemBundle = context.getBundle();
		for (Runnable handler : shutdownHandlers) {
			BundleListener listener = event -> {
				if (event.getBundle() == systemBundle && event.getType() == BundleEvent.STOPPED) {
					handler.run();
				}
			};
			context.addBundleListener(listener);
		}
	}
}
