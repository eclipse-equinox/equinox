/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.OSGi;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.eclipse.osgi.framework.tracker.ServiceTracker;

public class EclipseStarter {

	private static BundleContext context;
	private static String dataLocation = null;
	private static String configLocation = null;
	public static boolean DEBUG = false;

	// command line arguments
	private static final String CONSOLE = "-console"; //$NON-NLS-1$
	private static final String ARG_DEBUG = "-debug"; //$NON-NLS-1$
	private static final String DEV = "-dev"; //$NON-NLS-1$
	private static final String WS = "-ws"; //$NON-NLS-1$
	private static final String OS = "-os"; //$NON-NLS-1$
	private static final String ARCH = "-arch"; //$NON-NLS-1$
	private static final String NL = "-nl"; //$NON-NLS-1$	
	private static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$	
	// this is more of an Eclipse argument but this OSGi implementation stores its 
	// metadata alongside Eclipse's.
	private static final String DATA = "-data"; //$NON-NLS-1$

	public static final String INSTALL_LOCATION = "osgi.installLocation";
	
	// Constants for configuration location discovery
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_ID = "id"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_VERSION = "version"; //$NON-NLS-1$

	/** string containing the classname of the adaptor to be used in this framework instance */
	protected static String adaptorClassName = "org.eclipse.core.runtime.adaptor.EclipseAdaptor";
	
	// Console information
	protected static final String consoleClassName = "org.eclipse.osgi.framework.internal.core.FrameworkConsole";
	private static final String CONSOLE_NAME = "OSGi Console";
	private static String consolePort = "";
	private static boolean console = false;
	private static ServiceTracker applicationTracker;

	public static Object run(String[] args,Runnable endSplashHandler) throws Exception {
		processCommandLine(args);
		setInstanceLocation();
		setConfigurationLocation();
		loadConfigurationInfo();
		loadDefaultProperties();
		FrameworkAdaptor adaptor = createAdaptor();
		OSGi osgi = new OSGi(adaptor);
		if (osgi == null) 
			throw new IllegalStateException("OSGi framework could not be started");
		osgi.launch();
		try {
			if (console) 
				startConsole(osgi, new String[0]);
			context = osgi.getBundleContext();			
			publishSplashScreen(endSplashHandler);
			if (loadBundles() != null) { //if the installation of the basic did not fail
				setStartLevel(6);
				initializeApplicationTracker();
				Runnable application = (Runnable)applicationTracker.getService();
				applicationTracker.close();
				if (application == null)
					throw new IllegalStateException("Unable to acquire application service");
				application.run();
			}
		} finally {
			stopSystemBundle();
		}
		// TODO for now, if an exception is not thrown from this method, we have to do
		// the System.exit.  In the future we will update startup.jar to do the System.exit all 
		// the time.
		String exitCode = System.getProperty("eclipse.exitcode");
		if (exitCode == null)
			System.exit(0);
		try {
			System.exit(Integer.parseInt(exitCode));
		} catch (NumberFormatException e) {
			System.exit(13);
		}
		return null;
	}

	private static void publishSplashScreen(Runnable endSplashHandler) {
		// InternalPlatform now how to retrieve this later
		Dictionary properties = new Hashtable();
		properties.put("name","splashscreen");
		context.registerService(Runnable.class.getName(),endSplashHandler,properties);		
	}

	private static String searchForBundle(String name, String parent) throws MalformedURLException {
		URL url = null;
		File fileLocation = null;
		boolean reference = false;
		try {
			url = new URL(name);
		} catch (MalformedURLException e) {
			// TODO this is legacy support for non-URL names.  It should be removed eventually.
			// if name was not a URL then construct one.  
			// Assume it should be a reference and htat it is relative.  This support need not 
			// be robust as it is temporary..
			fileLocation = new File(parent, name);
			url = new URL("reference:file:"+ parent + "/" + name);
			reference = true;
		}
		// if the name was a URL then see if it is relative.  If so, insert syspath.
		if (!reference) {
			URL baseURL = url;
			// if it is a reference URL then strip off the reference: and set base to the file:...
			if (url.getProtocol().equals("reference")) {
				reference = true;
				baseURL = new URL(url.getFile());
			}
			
			fileLocation = new File(baseURL.getFile());
			// if the location is relative, prefix it with the syspath
			if (!fileLocation.isAbsolute())
				fileLocation = new File(parent, fileLocation.toString());
		}
		// If the result is a reference then search for the real result and 
		// reconstruct the answer.
		if (reference) {
			String result = searchFor(fileLocation.getName(), fileLocation.getParentFile().getAbsolutePath());
			if (result != null)
				url = new URL("reference", null, "file:" + result);
			else
				return null;
		}

		// finally we have something worth trying	
		try {
			URLConnection result = url.openConnection();
			result.connect();
			return url.toExternalForm();
		} catch (IOException e) {
//			int i = location.lastIndexOf('_');
//			return i == -1? location : location.substring(0, i);
			return null;
		}
	}

	private static String[] loadBundles() {
		long startTime = System.currentTimeMillis();
		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
		StartLevel start = null;
		if (reference != null) 
			start = (StartLevel)context.getService(reference);
		String[] bundles = getArrayFromList(System.getProperty("osgi.bundles"));			

		String syspath = getSysPath();
		Vector installed = new Vector();
		Vector ignored = new Vector();
		for (int i = 0; i < bundles.length; i++) {
			String name = bundles[i];
			if (name == null)
				continue;
			try {
				int level = -1;
				int index = name.indexOf('@');
				if (index >= 0) {
					String levelString = name.substring(index + 1, name.length());
					level = Integer.parseInt(levelString);
					name = name.substring(0, index);
			}
				String location = searchForBundle(name, syspath);
				if (location != null && !isInstalled(location)) {
					Bundle bundle = context.installBundle(location);
					if (level >= 0 && start != null)
						start.setBundleStartLevel(bundle, level);
					installed.addElement(bundle);
				} else 
					ignored.addElement(name);			
			} catch (Exception ex) {
				System.err.println("Cannot install/find: " + name);
				ex.printStackTrace();
				ignored.addElement(name);
				return null;
			}
		}
		refreshPackages((Bundle[])installed.toArray(new Bundle[installed.size()]));
			
		Enumeration e = installed.elements();
		while (e.hasMoreElements()) {
			Bundle bundle = (Bundle)e.nextElement();
			try {
				bundle.start();				
			} catch (BundleException ex) {
				System.out.println("Error starting " + bundle.getLocation());
				ex.printStackTrace();
			}
		}
		context.ungetService(reference);
		if (DEBUG)
			System.out.println("Time loadBundles in the framework: " + (System.currentTimeMillis() - startTime));
		return (String[])ignored.toArray(new String[ignored.size()]);
	}

	private static void refreshPackages(Bundle[] bundles) {
		if (bundles.length == 0)
			return;
		ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null) {
			packageAdmin = (PackageAdmin)context.getService(packageAdminRef);
			if (packageAdmin == null)
				return;
		}
		// TODO this is such a hack it is silly.  There are still cases for race conditions etc
		// but this should allow for some progress...
		final Semaphore semaphore = new Semaphore(0);
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
					semaphore.release();
			}
		};
		context.addFrameworkListener(listener);
		packageAdmin.refreshPackages(bundles);
		semaphore.acquire();
		context.removeFrameworkListener(listener);
		context.ungetService(packageAdminRef);
	}
	
	/**
	 *  Invokes the OSGi Console on another thread
	 *
	 * @param osgi The current OSGi instance for the console to attach to
	 * @param consoleArgs An String array containing commands from the command line
	 * for the console to execute
	 */
	private static void startConsole(OSGi osgi, String[] consoleArgs) {
		try {
			Class consoleClass = Class.forName(consoleClassName);
			Class[] parameterTypes;
			Object[] parameters;
			if (consolePort.length() == 0) {
				parameterTypes = new Class[] { OSGi.class, String[].class };
				parameters = new Object[] { osgi, consoleArgs };
			} else {
				parameterTypes = new Class[] { OSGi.class, int.class, String[].class };
				parameters = new Object[] { osgi, new Integer(consolePort), consoleArgs };
			}
			Constructor constructor = consoleClass.getConstructor(parameterTypes);
			Object console = constructor.newInstance(parameters);
			Thread t = new Thread(((Runnable) console), CONSOLE_NAME);
			t.start();
		} catch (NumberFormatException nfe) {
			System.err.println("Invalid console port: " + consolePort);
		} catch (Exception ex) {
			System.out.println("Failed to find/start: " + CONSOLE_NAME);
		}

	}

	/**
	 *  Creates and returns the adaptor
	 *
	 *  @return a FrameworkAdaptor object
	 */
	private static FrameworkAdaptor createAdaptor() throws Exception {
		Class adaptorClass = Class.forName(adaptorClassName);
		Class[] constructorArgs = new Class[] { String[].class };
		Constructor constructor = adaptorClass.getConstructor(constructorArgs);
		return (FrameworkAdaptor) constructor.newInstance(new Object[] { new String[0] });
	}

	private static String[] processCommandLine(String[] args) throws Exception {
		EnvironmentInfo.allArgs = args;
		int[] configArgs = new int[100];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)
	
			// check if development mode should be enabled for the entire platform
			// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
			// simply enable development mode.  Otherwise, assume that that the following arg is
			// actually some additional development time class path entries.  This will be processed below.
			if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				System.getProperties().put("osgi.dev", "");
				found = true;
				continue;
			}
	
			// look for the console with no port.  
			if (args[i].equalsIgnoreCase(CONSOLE) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
				console = true;
				found = true;
				continue;
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
				console = true;
				consolePort = arg;
				found = true;
				continue;
			}
	
			// look for the configuraiton location .  
			if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
				configLocation = arg;
				continue;
			}
	
			// look for the development mode and class path entries.  
			if (args[i - 1].equalsIgnoreCase(DEV)) {
				System.getProperties().put("osgi.dev", arg);
				found = true;
				continue;
			}
	
			// look for the data location for this instance.  
			if (args[i - 1].equalsIgnoreCase(DATA)) {
				dataLocation = arg;
				continue;
			}
	
			// look for the debug mode and option file location.  
			if (args[i - 1].equalsIgnoreCase(ARG_DEBUG)) {
				System.getProperties().put("osgi.debug", arg);
				DEBUG = true;
				found = true;
				continue;
			}
	
			// look for the window system.  
			if (args[i - 1].equalsIgnoreCase(WS)) {
				found = true;
				System.getProperties().put("osgi.ws", arg);
			}
	
			// look for the operating system
			if (args[i - 1].equalsIgnoreCase(OS)) {
				found = true;
				System.getProperties().put("osgi.os", arg);
			}
	
			// look for the system architecture
			if (args[i - 1].equalsIgnoreCase(ARCH)) {
				found = true;
				System.getProperties().put("osgi.arch", arg);
			}
	
			// look for the nationality/language
			if (args[i - 1].equalsIgnoreCase(NL)) {
				found = true;
				System.getProperties().put("osgi.nl", arg);
			}
	
			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}
	
		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0) {
			EnvironmentInfo.frameworkArgs = new String[0];
			EnvironmentInfo.appArgs = args;
			return args;
		}
		EnvironmentInfo.appArgs = new String[args.length - configArgIndex];
		EnvironmentInfo.frameworkArgs = new String[configArgIndex];
		configArgIndex = 0;
		int j = 0;
		int k = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex]) {
				EnvironmentInfo.frameworkArgs[k++] = args[i];
				configArgIndex++;
			} else
				EnvironmentInfo.appArgs[j++] = args[i];
		}
		return EnvironmentInfo.appArgs;
	}
	
	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	private static String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	protected static String getSysPath() {
		String result = System.getProperty("osgi.syspath");
		if (result != null) 
			return result;

		URL url = EclipseStarter.class.getProtectionDomain().getCodeSource().getLocation();
		result = url.getFile();
		if (result.endsWith("/"))
			result = result.substring(0, result.length() - 1);
		result = result.substring(0, result.lastIndexOf('/'));
		result = result.substring(0, result.lastIndexOf('/'));
		if (Character.isUpperCase(result.charAt(0))) {
			char[] chars = result.toCharArray();
			chars[0] = Character.toLowerCase(chars[0]);
			result = new String(chars);
		}
		return result;
	}

	private static void setInstanceLocation() {
		File result = null;
		String location = System.getProperty("osgi.instance.area");
		// if the instance location is not set, predict where the workspace will be and 
		// put the instance area inside the workspace meta area.
		if (location == null) {
			if (dataLocation == null) 
				result = new File(System.getProperty("user.dir"), "workspace");//$NON-NLS-1$ //$NON-NLS-2$
			else
				result = new File(dataLocation);
			result = new File(result, ".metadata/bundles");
		} else {
			result = new File(location);
		}
		System.getProperties().put("osgi.instance.area", result.getAbsolutePath());	
		System.getProperties().put("org.eclipse.osgi.framework.defaultadaptor.bundledir", result.getAbsolutePath());	
	}

	private static void setConfigurationLocation() {
		String location = System.getProperty("osgi.configuration.area");
		if (location != null) {
			configLocation = location;
			if (System.getProperty("osgi.manifest.cache") == null)
				System.getProperties().put("osgi.manifest.cache", configLocation + "/manifests");
			return;
		}
		// -configuration was not specified so compute a configLocation based on the
		// install location.  If it is read/write then use it.  Otherwise use the user.home
		if (configLocation == null) {
			configLocation = getDefaultConfigurationLocation() + "/.config";
		} else {
			// if -configuration was specified, then interpret the config location from the 
			// value given.  Allow for the specification of a config file (.cfg) or a dir.
			try {
				configLocation = new URL(configLocation).getFile();
			} catch (MalformedURLException e) {
				// TODO do something in the error case
			}
			configLocation = configLocation.replace('\\', '/');
			int index = configLocation.lastIndexOf('/');
			if (configLocation.endsWith(".cfg") || configLocation.endsWith("/")) 
				configLocation = configLocation.substring(0, index);
		} 
		System.getProperties().put("osgi.configuration.area", configLocation);
		if (System.getProperty("osgi.manifest.cache") == null) {
			System.getProperties().put("osgi.manifest.cache", configLocation + "/manifests");
		}
	}
	
	private static boolean isInstalled(String location) {
		Bundle[] installed = context.getBundles();
		for (int i = 0; i < installed.length; i++) {
			Bundle bundle = installed[i];
			if (location.equalsIgnoreCase(bundle.getLocation()))
				return true;
		}
		return false;
	}

	private static String getDefaultConfigurationLocation() {
		// 1) We store the config state relative to the 'eclipse' directory if possible
		// 2) If this directory is read-only 
		//    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
		//    is unique for each local user, and <application-id> is the one 
		//    defined in .eclipseproduct marker file. If .eclipseproduct does not
		//    exist, use "eclipse" as the application-id.
		
		String installProperty = System.getProperty(INSTALL_LOCATION);
		URL installURL = null;
		try {
			installURL = new URL(installProperty);
		} catch (MalformedURLException e) {
			// do nothgin here since it is basically impossible to get a bogus url 
		}
		File installDir = new File(installURL.getFile());
		if ("file".equals(installURL.getProtocol()) && installDir.canWrite()) { //$NON-NLS-1$
//			if (DEBUG)
//				debug("Using the installation directory."); //$NON-NLS-1$
			return installDir.getAbsolutePath();
		}

		// We can't write in the eclipse install dir so try for some place in the user's home dir
//		if (DEBUG)
//			debug("Using the user.home location."); //$NON-NLS-1$
		String appName = "." + ECLIPSE; //$NON-NLS-1$
		File eclipseProduct = new File(installDir, PRODUCT_SITE_MARKER );
		if (eclipseProduct.exists()) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(eclipseProduct));
				String appId = props.getProperty(PRODUCT_SITE_ID);
				if (appId == null || appId.trim().length() == 0)
					appId = ECLIPSE;
				String appVersion = props.getProperty(PRODUCT_SITE_VERSION);
				if (appVersion == null || appVersion.trim().length() == 0)
					appVersion = ""; //$NON-NLS-1$
				appName += File.separator + appId + "_" + appVersion; //$NON-NLS-1$
			} catch (IOException e) {
				// Do nothing if we get an exception.  We will default to a standard location 
				// in the user's home dir.
			}
		}

		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		File configDir = new File(userHome, appName);
		configDir.mkdirs();
		return configDir.getAbsolutePath();
	}
	
	private static void initializeApplicationTracker() {
		Filter filter = null;
		try {
			filter = context.createFilter("(&(objectClass=java.lang.Runnable)(eclipse.application=*))");
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		applicationTracker = new ServiceTracker(context, filter, null);
		applicationTracker.open();
	}
	
	private static void loadConfigurationInfo() {
		String configArea = System.getProperty("osgi.configuration.area");
		if (configArea == null)
			return;
		File location = new File(configArea, "config.ini");
		mergeProperties(System.getProperties(), loadProperties(location));
	}
	
	private static void loadDefaultProperties() {
		URL codeLocation = EclipseStarter.class.getProtectionDomain().getCodeSource().getLocation();
		if (codeLocation == null)
			return;
		String frameworkLocation = codeLocation.getFile();
		File location = new File(new File(frameworkLocation).getParentFile(), "eclipse.properties");
		mergeProperties(System.getProperties(), loadProperties(location));
	}
	
	private static Properties loadProperties(File location) {
		Properties result = new Properties();
		try {
			InputStream in = new FileInputStream(location);
			try {
				result.load(in);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			// its ok if there is no config.ini file.  We'll just use the defaults for everything
			return result;
		} catch (IOException e) {
			// but it is not so good if the file is there and has errors.
			// TODO log an error here and exit?
			e.printStackTrace();
		}
		return result;
	}
	
	private static void mergeProperties(Properties destination, Properties source) {
		for (Enumeration e = source.keys(); e.hasMoreElements(); ) {
			String key = (String)e.nextElement();
			String value = source.getProperty(key);
			if (destination.getProperty(key) == null)
				destination.put(key, value);
		}
	}
	
	private static void stopSystemBundle() throws BundleException{
		Bundle systemBundle = context.getBundle(0);
		if (systemBundle.getState() == Bundle.ACTIVE) {
			final Semaphore semaphore = new Semaphore(0);
			FrameworkListener listener = new FrameworkListener() {
				public void frameworkEvent(FrameworkEvent event) {
					if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED)
						semaphore.release();
				}
				
			};
			context.addFrameworkListener(listener);
			systemBundle.stop();
			semaphore.acquire();
			context.removeFrameworkListener(listener);
		}
	}
	private static void setStartLevel(final int value) {
		ServiceTracker tracker = new ServiceTracker(context, StartLevel.class.getName(), null);
		tracker.open();
		final StartLevel startLevel = (StartLevel)tracker.getService();
		final Semaphore semaphore = new Semaphore(0);
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED && startLevel.getStartLevel() == value)
					semaphore.release();
			}
		};
		context.addFrameworkListener(listener);
		startLevel.setStartLevel(value);
		semaphore.acquire();
		context.removeFrameworkListener(listener);
		tracker.close();
	}
	/**
	 * Searches for the given target directory starting in the "plugins" subdirectory
	 * of the given location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
	private static String searchFor(final String target, String start) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File candidate) {
				return candidate.isDirectory() && (candidate.getName().equals(target) || candidate.getName().startsWith(target + "_")); //$NON-NLS-1$
			}
		};
		File[] candidates = new File(start).listFiles(filter); //$NON-NLS-1$
		if (candidates == null)
			return null;
		String result = null;
		Object maxVersion = null;
		for (int i = 0; i < candidates.length; i++) {
			String name = candidates[i].getName();
			String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
			int index = name.indexOf('_');
			if (index != -1)
				version = name.substring(index + 1);
			Object currentVersion = getVersionElements(version);
			if (maxVersion == null) {
				result = candidates[i].getAbsolutePath();
				maxVersion = currentVersion;
			} else {
				if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
					result = candidates[i].getAbsolutePath();
					maxVersion = currentVersion;
				}
			}
		}
		if (result == null)
			return null;
		return result.replace(File.separatorChar, '/') + "/"; //$NON-NLS-1$
	}
	/**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier). Note, that returning anything else will cause exceptions in the caller.
	 */
	private static Object[] getVersionElements(String version) {
		Object[] result = { new Integer(0), new Integer(0), new Integer(0), "" }; //$NON-NLS-1$
		StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
		String token;
		int i = 0;
		while (t.hasMoreTokens() && i < 4) {
			token = t.nextToken();
			if (i < 3) {
				// major, minor or service ... numeric values
				try {
					result[i++] = new Integer(token);
				} catch (Exception e) {
					// invalid number format - use default numbers (0) for the rest
					break;
				}
			} else {
				// qualifier ... string value
				result[i++] = token;
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
	
}