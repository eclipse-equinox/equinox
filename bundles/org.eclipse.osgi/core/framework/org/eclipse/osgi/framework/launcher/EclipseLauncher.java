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
package org.eclipse.osgi.framework.launcher;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.OSGi;
import org.eclipse.osgi.framework.util.Tokenizer;

//import org.eclipse.osgi.platform.OsgiPlatform;
//import org.eclipse.osgi.framework.Framework;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class EclipseLauncher implements EnvironmentInfo {

	/** default console port */
	protected String consolePort = "";

	/** flag to indicate whether or not to start the console */
	protected boolean console = false;

	/** string containing the classname of the adaptor to be used in this framework instance */
	protected String adaptorClassName =
		"org.eclipse.osgi.framework.internal.defaultadaptor.DefaultAdaptor";

	protected final String IdeAdentClassName =
		"org.eclipse.osgi.framework.internal.core.ide.IdeAgent";

	protected final String osgiConsoleClazz =
		"org.eclipse.osgi.framework.internal.core.FrameworkConsole";

	/** array of adaptor arguments to be passed to FrameworkAdaptor.initialize() */
	String[] adaptorArgs = null;

	/** array of application arguments to be passed to Eclipse applications */
	String[] applicationArgs = null;

	/* Components that can be installed and activated optionally. */
	private static final String OSGI_CONSOLE_COMPONENT_NAME = "OSGi Console";
	private static final String OSGI_CONSOLE_COMPONENT = "osgiconsole.jar";

	// command line arguments
	private static final String INSTALL_ARG = "-install"; //$NON-NLS-1$
	private static final String META_AREA = ".metadata"; //$NON-NLS-1$
    

	Properties bootOptions;
	String installLocation; // install
	String debugOptionsFilename;
	boolean debugMode;

	Runnable handler;
	String allArgs[];
	String passThru[];

	private FrameworkAdaptor platform;
	private OSGi osgi;
	private ServiceRegistration bootloaderRegistration;
	private BundleContext context;

	public static void main(String args[]) {
		try {
			new EclipseLauncher().doIt(args);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void doIt(String[] args) throws Exception {
		allArgs = args;
		passThru = parseArgs(args);
		
		String osgiInstance = System.getProperty("osgi.instance");
		platform = doAdaptor();
	
		osgi = doOSGi(platform);
		osgi.launch();
		if (console) {
			doConsole(osgi, passThru);
		}
		context = osgi.getBundleContext();			

		// initialize eclipse URL handling
		String metaPath = osgiInstance + META_AREA;
		String nl,ws,os;
		nl = context.getProperty(Constants.FRAMEWORK_LANGUAGE);
		os = context.getProperty(Constants.FRAMEWORK_OS_NAME);
		ws = context.getProperty(Constants.FRAMEWORK_WINDOWING_SYSTEM);

		registerEclipseableService();
		loadBundles();
	}

	/**
	 *  Processes the -adaptor command line argument.
	 * 
	 *  Parses the arguments to get the adaptor class name, the adaptor dir or filename,
	 *  and the adaptor file size.
	 *
	 *  @return a FrameworkAdaptor object
	 */
	protected FrameworkAdaptor doAdaptor() throws Exception {

		Class adaptorClass = Class.forName(adaptorClassName);
		Class[] constructorArgs = new Class[] { String[].class };
		Constructor constructor = adaptorClass.getConstructor(constructorArgs);
		return (FrameworkAdaptor) constructor.newInstance(
			new Object[] { adaptorArgs });
	}

	/**
	 * Creates the OSGi framework object.
	 *
	 * @param The FrameworkAdaptor object
	 */
	protected OSGi doOSGi(FrameworkAdaptor adaptor) {
		return new OSGi(adaptor);
	}

	/*
		public void startConsole() {
			console = new Console(null);
			consoleThread = new Thread(console, "Console Thread");
			consoleThread.start();
		}
		*/
	/*
	private static void installRuntimeBundle() throws BundleException {
		Bundle runtimeBundle = context.installBundle(System.getProperty("eclipse.pluginbase") + System.getProperty("eclipse.runtime"));
		runtimeBundle.start();	//TODO This should not be here, because we are not sure that the bundle is resolved. We should have a listener or something like that
	}
	*/

	public Properties getBootOptions() {
		return bootOptions;
	}
/*
	public String[] getCommandLineArgs() {
		// TODO: what are we suppose to return here? Versus getFrameworkArgs()/getFrameworkArgs()
		return appArgs; //This is a temporary hack
	}
*/
	public Runnable getSplashHandler() {
		return handler;
	}

	private void registerEclipseableService() {
		Hashtable properties = new Hashtable(3);
		bootloaderRegistration =
			context.registerService(
				EnvironmentInfo.class.getName(),
				this,
				properties);
	}
/*
	public boolean inDevelopmentMode() {
		return platform.inDevelopmentMode();
	}
*/
	public String getOSArch() {
		return context.getProperty(Constants.FRAMEWORK_PROCESSOR);
	}
	public String getNL() {
		return context.getProperty(Constants.FRAMEWORK_LANGUAGE);
	}
	public String getOS() {
		return context.getProperty(Constants.FRAMEWORK_OS_NAME);
	}
	public String getWS() {
		return context.getProperty(Constants.FRAMEWORK_WINDOWING_SYSTEM);
	}

	public String[] getAllArgs() {
		return allArgs;
	}

	public String[] getFrameworkArgs() {
		// TODO: what are we suppose to return here? Versus getCommandLineArgs()/getApplicationArgs()
		return this.adaptorArgs;
	}

	public String[] getApplicationArgs() {
		// TODO: what are we suppose to return here? Versus getCommandLineArgs()/getFrameworkArgs()
		return applicationArgs;
	}
	public boolean inDebugMode() {
		return debugMode;
	}

	/**
	 *  Invokes the OSGi Console on another thread
	 *
	 * @param osgi The current OSGi instance for the console to attach to
	 * @param consoleArgs An String array containing commands from the command line
	 * for the console to execute
	 */
	protected void doConsole(OSGi osgi, String[] consoleArgs) {

		Constructor consoleConstructor;
		Object osgiconsole;
		Class[] parameterTypes;
		Object[] parameters;

		try {
			Class osgiconsoleClass = Class.forName(osgiConsoleClazz);
			if (consolePort.length() == 0) {
				parameterTypes = new Class[] { OSGi.class, String[].class };
				parameters = new Object[] { osgi, consoleArgs };
			} else {
				parameterTypes = new Class[] { OSGi.class, int.class, String[].class };
				parameters = new Object[] { osgi, new Integer(consolePort), consoleArgs };
			}
			consoleConstructor = osgiconsoleClass.getConstructor(parameterTypes);
			osgiconsole = consoleConstructor.newInstance(parameters);

			Thread t = new Thread(((Runnable) osgiconsole), OSGI_CONSOLE_COMPONENT_NAME);
			t.start();
		} catch (NumberFormatException nfe) {
			System.err.println("Invalid console port: " + consolePort);
		} catch (Exception ex) {
			informAboutMissingComponent(OSGI_CONSOLE_COMPONENT_NAME, OSGI_CONSOLE_COMPONENT);
		}

	}
	
	/**
	 * Informs the user about a missing component.
	 *
	 * @param component The name of the component
	 * @param jar The jar file that contains the component
	 */
	void informAboutMissingComponent(String component, String jar) {
		System.out.println();
		System.out.print("Warning: The requested component '" + component + "' is not included in this runtime.");
		System.out.println(" Add '" + jar + "' to the classpath or rebuild the jxe with it.");
		System.out.println();
	}

/*
	private String[] processCommandLine(String[] args)
		throws Exception {
		int[] configArgs = new int[100];
		configArgs[0] = -1;

		if (args == null) {
			return null;
		}
		// need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)

			// None at this level so far...

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

			// look for the install location. 
			if (args[i - 1].equalsIgnoreCase(INSTALL_ARG)) {
				found = true;
				installLocation = arg;
			}

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0)
			return args;
		String[] passThruArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int i, j;
		for (j = 0, i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex]) {
				configArgIndex++;
				args[i] = null;
			} else
				passThruArgs[j++] = args[i];
		}
		for (j = 0, i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			args[j++] = args[i];
		}
		processedArgs = args;
		return passThruArgs;
	}
*/
	private static final String OPTIONS = ".options"; //$NON-NLS-1$
	private static Properties debugOptions;

	private void loadDebugOptions() {

		// if no debug option was specified, don't even bother to try.
		// Must ensure that the options slot is null as this is the signal to the
		// platform that debugging is not enabled.
		if (debugMode) {
			debugOptions = null;
			return;
		}
		debugOptions = new Properties();
		URL optionsFile;
		if (debugOptionsFilename == null) {
			// default options location is user.dir (install location may be r/o so
			// is not a good candidate for a trace options that need to be updatable by
			// by the user)
			String userDir = System.getProperty("user.dir").replace(File.separatorChar, '/'); //$NON-NLS-1$
			if (!userDir.endsWith("/")) //$NON-NLS-1$
				userDir += "/"; //$NON-NLS-1$
			debugOptionsFilename = "file:" + userDir + OPTIONS; //$NON-NLS-1$
		}
		try {
			optionsFile = getURL(debugOptionsFilename);
		} catch (MalformedURLException e) {
			System.out.println("Unable to construct URL for options file: " + debugOptionsFilename); //$NON-NLS-1$
			e.printStackTrace(System.out);
			return;
		}
		System.out.println("Debug-Options:\n    " + debugOptionsFilename); //$NON-NLS-1$
		try {
			InputStream input = optionsFile.openStream();
			try {
				debugOptions.load(input);
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			//	Its not an error to not find the options file
		} catch (IOException e) {
			System.out.println("Could not parse the options file: " + optionsFile); //$NON-NLS-1$
			e.printStackTrace(System.out);
		}
		// trim off all the blanks since properties files don't do that.
		for (Iterator i = debugOptions.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			debugOptions.put(key, ((String) debugOptions.get(key)).trim());
		}
		setupOptions();
	}
	/**
	 * Setup the debug flags for the given debug options.  This method will likely
	 * be called twice.  Once when loading the options file from the command
	 * line or install dir and then again when we have loaded options from the
	 * specific platform metaarea. 
	 */
	public static void setupOptions() {
		// TODO: need to fix this and decide how/which options we want.
		// TODO: see InternalBootLoader::setupOptions();
		/*
		options.put(OPTION_STARTTIME, Long.toString(System.currentTimeMillis()));
		DelegatingURLClassLoader.DEBUG = getBooleanOption(OPTION_LOADER_DEBUG, false);
		DelegatingURLClassLoader.DEBUG_SHOW_CREATE = getBooleanOption(OPTION_LOADER_SHOW_CREATE, true);
		DelegatingURLClassLoader.DEBUG_SHOW_ACTIVATE = getBooleanOption(OPTION_LOADER_SHOW_ACTIVATE, true);
		DelegatingURLClassLoader.DEBUG_SHOW_ACTIONS = getBooleanOption(OPTION_LOADER_SHOW_ACTIONS, true);
		DelegatingURLClassLoader.DEBUG_SHOW_SUCCESS = getBooleanOption(OPTION_LOADER_SHOW_SUCCESS, true);
		DelegatingURLClassLoader.DEBUG_SHOW_FAILURE = getBooleanOption(OPTION_LOADER_SHOW_FAILURE, true);
		DelegatingURLClassLoader.DEBUG_FILTER_CLASS = getListOption(OPTION_LOADER_FILTER_CLASS);
		DelegatingURLClassLoader.DEBUG_FILTER_LOADER = getListOption(OPTION_LOADER_FILTER_LOADER);
		DelegatingURLClassLoader.DEBUG_FILTER_RESOURCE = getListOption(OPTION_LOADER_FILTER_RESOURCE);
		DelegatingURLClassLoader.DEBUG_FILTER_NATIVE = getListOption(OPTION_LOADER_FILTER_NATIVE);
		PlatformURLConnection.DEBUG = getBooleanOption(OPTION_URL_DEBUG, false);
		PlatformURLConnection.DEBUG_CONNECT = getBooleanOption(OPTION_URL_DEBUG_CONNECT, true);
		PlatformURLConnection.DEBUG_CACHE_LOOKUP = getBooleanOption(OPTION_URL_DEBUG_CACHE_LOOKUP, true);
		PlatformURLConnection.DEBUG_CACHE_COPY = getBooleanOption(OPTION_URL_DEBUG_CACHE_COPY, true);
		BootLoader.CONFIGURATION_DEBUG = getBooleanOption(OPTION_CONFIGURATION_DEBUG, false);
		
		DelegatingURLClassLoader.MONITOR_PLUGINS = getBooleanOption(OPTION_MONITOR_PLUGINS, DelegatingURLClassLoader.MONITOR_PLUGINS);
		DelegatingURLClassLoader.MONITOR_CLASSES = getBooleanOption(OPTION_MONITOR_CLASSES, DelegatingURLClassLoader.MONITOR_CLASSES);
		DelegatingURLClassLoader.MONITOR_BUNDLES = getBooleanOption(OPTION_MONITOR_BUNDLES, DelegatingURLClassLoader.MONITOR_BUNDLES);
		
		DelegatingURLClassLoader.TRACE_FILENAME = options.getProperty(OPTION_TRACE_FILENAME, DelegatingURLClassLoader.TRACE_FILENAME); 
		DelegatingURLClassLoader.TRACE_FILTERS = options.getProperty(OPTION_TRACE_FILTERS, DelegatingURLClassLoader.TRACE_FILTERS);
		DelegatingURLClassLoader.TRACE_CLASSES = getBooleanOption(OPTION_TRACE_CLASSES, DelegatingURLClassLoader.TRACE_CLASSES);		
		DelegatingURLClassLoader.TRACE_PLUGINS = getBooleanOption(OPTION_TRACE_PLUGINS, DelegatingURLClassLoader.TRACE_PLUGINS);
		
		DelegatingURLClassLoader.DEBUG_PROPERTIES = getBooleanOption(OPTION_LOADER_PROPERTIES, DelegatingURLClassLoader.DEBUG_PROPERTIES);
		DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES = getBooleanOption(OPTION_LOADER_PACKAGE_PREFIXES, DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES);
		DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES_SUCCESS = getBooleanOption(OPTION_LOADER_PACKAGE_PREFIXES_SUCCESS, DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES_SUCCESS);
		DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES_FAILURE = getBooleanOption(OPTION_LOADER_PACKAGE_PREFIXES_FAILURE, DelegatingURLClassLoader.DEBUG_PACKAGE_PREFIXES_FAILURE);
		*/
	}

	/**
	 * Helper method that creates an URL object from the given string
	 * representation. The string must correspond to a valid URL or file system
	 * path.
	 */
	private static URL getURL(String urlString) throws MalformedURLException {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			// if it is not a well formed URL, tries to create a "file:" URL
			try {
				return new File(urlString).toURL();
			} catch (MalformedURLException ex) {
				// re-throw the original exception if nothing works
				throw e;
			}
		}
	}
	/**
	 *  Parses the command line arguments and remembers them so they can be processed later.
	 *
	 *  @param args The command line arguments
	 *  @return String [] Any arguments that should be passed to the console
	 */
	private String[] parseArgs(String[] args) {
		Vector consoleArgsVector = new Vector();
		for (int i = 0; i < args.length; i++) {
			boolean match = false;

			// Have to check for args that may be contained in double quotes but broken up by spaces - for example
			// -adaptor::"bundledir=c:/my bundle dir":reset should all be one arg, but java breaks it into 3 args, 
			// ignoring the quotes.  Must put it back together into one arg.
			String fullarg = args[i];
			int quoteidx = fullarg.indexOf("\"");
			if (quoteidx > 0) {
				if (quoteidx == fullarg.lastIndexOf("\"")) {
					boolean stillparsing = true;
					i++;
					while (i < args.length && stillparsing) {
						fullarg = fullarg + " " + args[i];
						i++;
						if (quoteidx < fullarg.lastIndexOf("\"")) {
							stillparsing = false;
						}
					}
				}
			} else {
				// IDE can't pass double quotes due to known eclipse bug (see Bugzilla 93201).  Allowing for use of single quotes.
				quoteidx = fullarg.indexOf("'");
				if (quoteidx > 0) {
					if (quoteidx == fullarg.lastIndexOf("'")) {
						boolean stillparsing = true;
						i++;
						while (i < args.length && stillparsing) {
							fullarg = fullarg + " " + args[i];
							i++;
							if (quoteidx < fullarg.lastIndexOf("'")) {
								stillparsing = false;
							}
						}
					}
					fullarg = fullarg.replace('\'', '\"');
				}
			}

			Tokenizer tok = new Tokenizer(fullarg);
			if (tok.hasMoreTokens()) {
				String command = tok.getString(" ");
				StringTokenizer subtok = new StringTokenizer(command, ":");
				String subcommand = subtok.nextToken().toLowerCase();

				if (matchCommand("-console", subcommand, 4)) {
					_console(command);
					match = true;
				}
				if (matchCommand("-adaptor", subcommand, 2)) {
					_adaptor(command);
					match = true;
				}
				if (matchCommand("-install", subcommand, 2)) {
					_install(command);
					match = true;
				}
				if (matchCommand("-debug", subcommand, 2)) {
					_debug(command);
					match = true;
				}
				if (matchCommand("-application", subcommand, 3)) {
					_application(command);
					match = true;
				}

				if (match == false) {
					// if the command doesn't match any of the known commands, save it to pass
					// to the console
					consoleArgsVector.addElement(fullarg);
				}
			}
		}
		// convert arguments to be passed to console into a string array for the Console
		String[] consoleArgsArray = new String[consoleArgsVector.size()];
		Enumeration e = consoleArgsVector.elements();
		for (int i = 0; i < consoleArgsArray.length; i++) {
			consoleArgsArray[i] = (String) e.nextElement();
		}
		return consoleArgsArray;
	}

	public static boolean matchCommand(
		String command,
		String input,
		int minLength) {
		if (minLength <= 0) {
			minLength = command.length();
		}

		int length = input.length();

		if (minLength > length) {
			length = minLength;
		}

		return (command.regionMatches(0, input, 0, length));
	}

	protected void _debug(String command) {
		StringTokenizer tok = new StringTokenizer(command, ":");
		// first token is always "-console"
		String cmd = tok.nextToken();
		if (tok.hasMoreTokens()) {
			debugOptionsFilename = tok.nextToken();
		}
		debugMode = true;
	}

	protected void _install(String command) {
		StringTokenizer tok = new StringTokenizer(command, ":");
		// first token is always "-console"
		String cmd = tok.nextToken();
		if (tok.hasMoreTokens()) {
			installLocation = tok.nextToken();
		}
	}
	/**
	 *  Remembers that the -console option has been requested.
	 */
	protected void _console(String command) {
		console = true;
		StringTokenizer tok = new StringTokenizer(command, ":");
		// first token is always "-console"
		String cmd = tok.nextToken();
		if (tok.hasMoreTokens()) {
			consolePort = tok.nextToken();
		}
	}

	/**
	 *  Remembers that the -adaptor option has been requested.  Parses off the adaptor class
	 *  file name, the adaptor file name, and the size if they are there.
	 *
	 * @param tok The rest of the -adaptor parameter string that contains the class file name,
	 * and possibly the adaptor file and file size.
	 */
	protected void _adaptor(String command) {
		Tokenizer tok = new Tokenizer(command);
		// first token is always "-adaptor"
		String cmd = tok.getToken(":");
		tok.getChar(); // advance to next token
		// and next token is either adaptor class name or ":" if we should use the default adaptor
		String adp = tok.getToken(":");
		if (adp!=null && adp.length() > 0) {
			adaptorClassName = adp;
		}

		// following tokens are arguments to be processed by the adaptor implementation class
		// they may be enclosed in quotes
		// store them in a vector until we know how many there are
		Vector v = new Vector();
		parseloop : while (true) {
			tok.getChar(); // advance to next token
			String arg = tok.getString(":");
			if (arg == null) {
				break parseloop;
			} else {
				v.addElement(arg);
			}
		}
		// now that we know how many args there are, move args from vector to String []
		if (v != null) {
			int numArgs = v.size();
			adaptorArgs = new String[numArgs];
			Enumeration e = v.elements();
			for (int i = 0; i < numArgs; i++) {
				adaptorArgs[i] = (String) e.nextElement();
			}
		}
	}

	/**
		 *  Remembers that the -application option has been requested.  Parses off the application parameters
		 *  into a String []
		 *
		 * @param tok The rest of the -application parameter string that contains the application arguments
		 */
	protected void _application(String command) {
		Tokenizer tok = new Tokenizer(command);
		// first token is always "-adaptor"
		String cmd = tok.getToken(":");
		// following tokens are arguments to be processed by the adaptor implementation class
		// they may be enclosed in quotes
		// store them in a vector until we know how many there are
		Vector v = new Vector();
		parseloop : while (true) {
			tok.getChar(); // advance to next token
			String arg = tok.getToken(";");
			if (arg == null) {
				break parseloop;
			} else {
				v.addElement(arg);
			}
		}
		// now that we know how many args there are, move args from vector to String []
		if (v != null) {
			int numArgs = v.size();
			applicationArgs = new String[numArgs];
			Enumeration e = v.elements();
			for (int i = 0; i < numArgs; i++) {
				applicationArgs[i] = (String) e.nextElement();
			}
		}
	}
	
	public String[] loadBundles() {
		String name;
		File dir;
		ServiceReference packageAdminRef;
		PackageAdmin packageAdmin=null;
		String location;

		String tmp1 = System.getProperty("osgi.bundles");
		StringTokenizer tokenizer = new StringTokenizer(tmp1, ",");
		Vector list = new Vector();
		while (tokenizer.hasMoreTokens()) {
			list.addElement(tokenizer.nextToken());
		}
		
		String[] bundles = new String[list.size()];
		list.toArray(bundles);
		
		packageAdminRef = context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin");
		if (packageAdminRef != null) {
			packageAdmin = (org.osgi.service.packageadmin.PackageAdmin)context.getService(packageAdminRef);
			if (packageAdmin == null) return bundles;
		}

		String syspath = getSysPath();
		Bundle bundle;
		Vector installed = new Vector();
		Vector ignored = new Vector();
		for (int i = 0; i < bundles.length; i++) {
			name = bundles[i];
			if (name == null)
				continue;
			try {
				location = "reference:file:/"+syspath+"/"+name;
				bundle = context.installBundle(location);
				installed.addElement(bundle);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("Ignoring " + name);
				ignored.addElement(name);
				continue;
			}
		}
		Bundle a[] = new Bundle[installed.size()];
		installed.toArray(a);
		packageAdmin.refreshPackages(a);
		
		Enumeration e;
		e = installed.elements();
		while (e.hasMoreElements()) {
			bundle = (Bundle)e.nextElement();
			try {
				bundle.start();				
			} catch (BundleException ex) {
				ex.printStackTrace();
				System.err.println("Starting " + bundle.getLocation());
			}
		}
		String tmp[] = new String[ignored.size()];
		ignored.toArray(tmp);
		return tmp;
	}

	private String getSysPath() {
		String syspath;
		syspath = System.getProperty("osgi.syspath");
		if (syspath==null) 
			throw new RuntimeException("Missing osgi.syspath properties.");
		if (!syspath.equals("workspace")) 
			return syspath;

		Class clazz = EclipseLauncher.class;
		ClassLoader cl = clazz.getClassLoader();
		String thisname = clazz.getName();
		String classname = thisname.replace('.', '/') + ".class";
		URL url = cl.getResource(classname);
		int idx;
		String pathname = url.getFile();
		idx = pathname.indexOf(classname);
		pathname = pathname.substring(0, idx);
		
		syspath = pathname;
		idx = syspath.indexOf("org.eclipse.osgi.framework.core");
		syspath = syspath.substring(0, idx);
		return syspath;
	}
}
