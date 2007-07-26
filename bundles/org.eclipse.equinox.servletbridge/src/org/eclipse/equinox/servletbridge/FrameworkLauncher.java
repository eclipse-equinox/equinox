/*******************************************************************************
 * Copyright (c) 2005-2007 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.servletbridge;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * The FrameworkLauncher provides the logic to:
 * 1) init
 * 2) deploy
 * 3) start
 * 4) stop
 * 5) undeploy
 * 6) destroy
 * an instance of the OSGi framework. 
 * These 6 methods are provided to help manage the lifecycle and are called from outside this
 * class by the BridgeServlet. To create an extended FrameworkLauncher over-ride these methods to allow
 * custom behaviour.  
 */
public class FrameworkLauncher {

	private static final String WS_DELIM = " \t\n\r\f"; //$NON-NLS-1$
	protected static final String FILE_SCHEME = "file:"; //$NON-NLS-1$
	protected static final String FRAMEWORK_BUNDLE_NAME = "org.eclipse.osgi"; //$NON-NLS-1$
	protected static final String STARTER = "org.eclipse.core.runtime.adaptor.EclipseStarter"; //$NON-NLS-1$
	protected static final String FRAMEWORKPROPERTIES = "org.eclipse.osgi.framework.internal.core.FrameworkProperties"; //$NON-NLS-1$
	protected static final String NULL_IDENTIFIER = "@null"; //$NON-NLS-1$
	protected static final String OSGI_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	protected static final String OSGI_INSTANCE_AREA = "osgi.instance.area"; //$NON-NLS-1$
	protected static final String OSGI_CONFIGURATION_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	protected static final String OSGI_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	protected static final String OSGI_FORCED_RESTART = "osgi.forcedRestart"; //$NON-NLS-1$
	protected static final String RESOURCE_BASE = "/WEB-INF/eclipse/"; //$NON-NLS-1$
	protected static final String LAUNCH_INI = "launch.ini"; //$NON-NLS-1$

	private static final String MANIFEST_VERSION = "Manifest-Version"; //$NON-NLS-1$
	private static final String BUNDLE_MANIFEST_VERSION = "Bundle-ManifestVersion"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = "Bundle-Name"; //$NON-NLS-1$
	private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName"; //$NON-NLS-1$
	private static final String BUNDLE_VERSION = "Bundle-Version"; //$NON-NLS-1$
	private static final String FRAGMENT_HOST = "Fragment-Host"; //$NON-NLS-1$
	private static final String EXPORT_PACKAGE = "Export-Package"; //$NON-NLS-1$

	private static final String CONFIG_COMMANDLINE = "commandline"; //$NON-NLS-1$
	private static final String CONFIG_EXTENDED_FRAMEWORK_EXPORTS = "extendedFrameworkExports"; //$NON-NLS-1$

	static final PermissionCollection allPermissions = new PermissionCollection() {
		private static final long serialVersionUID = 482874725021998286L;
		// The AllPermission permission
		Permission allPermission = new AllPermission();

		// A simple PermissionCollection that only has AllPermission
		public void add(Permission permission) {
			// do nothing
		}

		public boolean implies(Permission permission) {
			return true;
		}

		public Enumeration elements() {
			return new Enumeration() {
				int cur = 0;

				public boolean hasMoreElements() {
					return cur < 1;
				}

				public Object nextElement() {
					if (cur == 0) {
						cur = 1;
						return allPermission;
					}
					throw new NoSuchElementException();
				}
			};
		}
	};

	static {
		// We do this to ensure the anonymous Enumeration class in allPermissions is pre-loaded 
		if (allPermissions.elements() == null)
			throw new IllegalStateException();
	}

	protected ServletConfig config;
	protected ServletContext context;
	private File platformDirectory;
	private ClassLoader frameworkContextClassLoader;
	private URLClassLoader frameworkClassLoader;

	void init(ServletConfig servletConfig) {
		config = servletConfig;
		context = servletConfig.getServletContext();
		init();
	}

	/**
	 * init is the first method called on the FrameworkLauncher and can be used for any initial setup.
	 * The default behaviour is to do nothing.
	 */
	public void init() {
		// do nothing for now
	}

	/**
	 * destory is the last method called on the FrameworkLauncher and can be used for any final cleanup.
	 * The default behaviour is to do nothing.
	 */
	public void destroy() {
		// do nothing for now
	}

	/**
	 * deploy is used to move the OSGi framework libraries into a location suitable for execution.
	 * The default behaviour is to copy the contents of the webapps WEB-INF/eclipse directory
	 * to the webapps temp directory.
	 */
	public synchronized void deploy() {
		if (platformDirectory != null) {
			context.log("Framework is already deployed"); //$NON-NLS-1$
			return;
		}

		File servletTemp = (File) context.getAttribute("javax.servlet.context.tempdir"); //$NON-NLS-1$
		platformDirectory = new File(servletTemp, "eclipse"); //$NON-NLS-1$
		if (!platformDirectory.exists()) {
			platformDirectory.mkdirs();
		}

		copyResource(RESOURCE_BASE + "configuration/", new File(platformDirectory, "configuration")); //$NON-NLS-1$ //$NON-NLS-2$
		copyResource(RESOURCE_BASE + "features/", new File(platformDirectory, "features")); //$NON-NLS-1$ //$NON-NLS-2$
		File plugins = new File(platformDirectory, "plugins"); //$NON-NLS-1$
		copyResource(RESOURCE_BASE + "plugins/", plugins); //$NON-NLS-1$
		deployExtensionBundle(plugins);
		copyResource(RESOURCE_BASE + ".eclipseproduct", new File(platformDirectory, ".eclipseproduct")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * deployExtensionBundle will generate the Servletbridge extensionbundle if it is not already present in the platform's
	 * plugin directory. By default it exports "org.eclipse.equinox.servletbridge" and a versioned export of the Servlet API.
	 * Additional exports can be added by using the "extendedFrameworkExports" initial-param in the ServletConfig
	 */
	private void deployExtensionBundle(File plugins) {
		File extensionBundle = new File(plugins, "org.eclipse.equinox.servletbridge.extensionbundle_1.0.0.jar"); //$NON-NLS-1$
		File extensionBundleDir = new File(plugins, "org.eclipse.equinox.servletbridge.extensionbundle_1.0.0"); //$NON-NLS-1$
		if (extensionBundle.exists() || (extensionBundleDir.exists() && extensionBundleDir.isDirectory()))
			return;

		Manifest mf = new Manifest();
		Attributes attribs = mf.getMainAttributes();
		attribs.putValue(MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		attribs.putValue(BUNDLE_MANIFEST_VERSION, "2"); //$NON-NLS-1$
		attribs.putValue(BUNDLE_NAME, "Servletbridge Extension Bundle"); //$NON-NLS-1$
		attribs.putValue(BUNDLE_SYMBOLIC_NAME, "org.eclipse.equinox.servletbridge.extensionbundle"); //$NON-NLS-1$
		attribs.putValue(BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		attribs.putValue(FRAGMENT_HOST, "system.bundle; extension:=framework"); //$NON-NLS-1$

		String servletVersion = context.getMajorVersion() + "." + context.getMinorVersion(); //$NON-NLS-1$
		String packageExports = "org.eclipse.equinox.servletbridge; version=1.0" + //$NON-NLS-1$
				", javax.servlet; version=" + servletVersion + //$NON-NLS-1$
				", javax.servlet.http; version=" + servletVersion + //$NON-NLS-1$
				", javax.servlet.resources; version=" + servletVersion; //$NON-NLS-1$

		String extendedExports = config.getInitParameter(CONFIG_EXTENDED_FRAMEWORK_EXPORTS);
		if (extendedExports != null && extendedExports.trim().length() != 0)
			packageExports += ", " + extendedExports; //$NON-NLS-1$

		attribs.putValue(EXPORT_PACKAGE, packageExports);

		try {
			JarOutputStream jos = null;
			try {
				jos = new JarOutputStream(new FileOutputStream(extensionBundle), mf);
				jos.finish();
			} finally {
				if (jos != null)
					jos.close();
			}
		} catch (IOException e) {
			context.log("Error generating extension bundle", e); //$NON-NLS-1$
		}
	}

	/** undeploy is the reverse operation of deploy and removes the OSGi framework libraries from their
	 * execution location. Typically this method will only be called if a manual undeploy is requested in the 
	 * ServletBridge.
	 * By default, this method removes the OSGi install and also removes the workspace.
	 */
	public synchronized void undeploy() {
		if (platformDirectory == null) {
			context.log("Undeploy unnecessary. - (not deployed)"); //$NON-NLS-1$
			return;
		}

		if (frameworkClassLoader != null) {
			throw new IllegalStateException("Could not undeploy Framework - (not stopped)"); //$NON-NLS-1$
		}

		deleteDirectory(new File(platformDirectory, "configuration")); //$NON-NLS-1$
		deleteDirectory(new File(platformDirectory, "features")); //$NON-NLS-1$
		deleteDirectory(new File(platformDirectory, "plugins")); //$NON-NLS-1$
		deleteDirectory(new File(platformDirectory, "workspace")); //$NON-NLS-1$

		new File(platformDirectory, ".eclipseproduct").delete(); //$NON-NLS-1$
		platformDirectory = null;
	}

	/** start is used to "start" a previously deployed OSGi framework
	 * The default behaviour will read launcher.ini to create a set of initial properties and
	 * use the "commandline" configuration parameter to create the equivalent command line arguments
	 * available when starting Eclipse. 
	 */
	public synchronized void start() {
		if (platformDirectory == null)
			throw new IllegalStateException("Could not start the Framework - (not deployed)"); //$NON-NLS-1$

		if (frameworkClassLoader != null) {
			context.log("Framework is already started"); //$NON-NLS-1$
			return;
		}

		Map initalPropertyMap = buildInitialPropertyMap();
		String[] args = buildCommandLineArguments();

		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			System.setProperty("osgi.framework.useSystemProperties", "false"); //$NON-NLS-1$ //$NON-NLS-2$

			URL[] osgiURLArray = {new URL((String) initalPropertyMap.get(OSGI_FRAMEWORK))};
			frameworkClassLoader = new ChildFirstURLClassLoader(osgiURLArray, this.getClass().getClassLoader());
			Class clazz = frameworkClassLoader.loadClass(STARTER);

			Method setInitialProperties = clazz.getMethod("setInitialProperties", new Class[] {Map.class}); //$NON-NLS-1$
			setInitialProperties.invoke(null, new Object[] {initalPropertyMap});

			registerRestartHandler(clazz);

			Method runMethod = clazz.getMethod("startup", new Class[] {String[].class, Runnable.class}); //$NON-NLS-1$
			runMethod.invoke(null, new Object[] {args, null});

			frameworkContextClassLoader = Thread.currentThread().getContextClassLoader();
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getTargetException();
			if (t == null)
				t = ite;
			context.log("Error while starting Framework", t); //$NON-NLS-1$
			throw new RuntimeException(t.getMessage());
		} catch (Exception e) {
			context.log("Error while starting Framework", e); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage());
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	private void registerRestartHandler(Class starterClazz) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
		Method registerFrameworkShutdownHandler = null;
		try {
			registerFrameworkShutdownHandler = starterClazz.getDeclaredMethod("internalAddFrameworkShutdownHandler", new Class[] {Runnable.class}); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
			// Ok. However we will not support restart events. Log this as info
			context.log(starterClazz.getName() + " does not support setting a shutdown handler. Restart handling is disabled."); //$NON-NLS-1$
			return;
		}
		if (!registerFrameworkShutdownHandler.isAccessible())
			registerFrameworkShutdownHandler.setAccessible(true);
		Runnable restartHandler = createRestartHandler();
		registerFrameworkShutdownHandler.invoke(null, new Object[] {restartHandler});
	}

	private Runnable createRestartHandler() throws ClassNotFoundException, NoSuchMethodException {
		Class frameworkPropertiesClazz = frameworkClassLoader.loadClass(FRAMEWORKPROPERTIES);
		final Method getProperty = frameworkPropertiesClazz.getMethod("getProperty", new Class[] {String.class}); //$NON-NLS-1$
		Runnable restartHandler = new Runnable() {
			public void run() {
				try {
					String forcedRestart = (String) getProperty.invoke(null, new Object[] {OSGI_FORCED_RESTART});
					if (Boolean.valueOf(forcedRestart).booleanValue()) {
						stop();
						start();
					}
				} catch (InvocationTargetException ite) {
					Throwable t = ite.getTargetException();
					if (t == null)
						t = ite;
					throw new RuntimeException(t.getMessage());
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage());
				}
			}
		};
		return restartHandler;
	}

	/** buildInitialPropertyMap create the initial set of properties from the contents of launch.ini
	 * and for a few other properties necessary to launch defaults are supplied if not provided.
	 * The value '@null' will set the map value to null.
	 * @return a map containing the initial properties
	 */
	protected Map buildInitialPropertyMap() {
		Map initialPropertyMap = new HashMap();
		Properties launchProperties = loadProperties(RESOURCE_BASE + LAUNCH_INI);
		for (Iterator it = launchProperties.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (key.endsWith("*")) { //$NON-NLS-1$
				if (value.equals(NULL_IDENTIFIER)) {
					clearPrefixedSystemProperties(key.substring(0, key.length() - 1), initialPropertyMap);
				}
			} else if (value.equals(NULL_IDENTIFIER))
				initialPropertyMap.put(key, null);
			else
				initialPropertyMap.put(entry.getKey(), entry.getValue());
		}

		try {
			// install.area if not specified
			if (initialPropertyMap.get(OSGI_INSTALL_AREA) == null)
				initialPropertyMap.put(OSGI_INSTALL_AREA, platformDirectory.toURL().toExternalForm());

			// configuration.area if not specified
			if (initialPropertyMap.get(OSGI_CONFIGURATION_AREA) == null) {
				File configurationDirectory = new File(platformDirectory, "configuration"); //$NON-NLS-1$
				if (!configurationDirectory.exists()) {
					configurationDirectory.mkdirs();
				}
				initialPropertyMap.put(OSGI_CONFIGURATION_AREA, configurationDirectory.toURL().toExternalForm());
			}

			// instance.area if not specified
			if (initialPropertyMap.get(OSGI_INSTANCE_AREA) == null) {
				File workspaceDirectory = new File(platformDirectory, "workspace"); //$NON-NLS-1$
				if (!workspaceDirectory.exists()) {
					workspaceDirectory.mkdirs();
				}
				initialPropertyMap.put(OSGI_INSTANCE_AREA, workspaceDirectory.toURL().toExternalForm());
			}

			// osgi.framework if not specified
			if (initialPropertyMap.get(OSGI_FRAMEWORK) == null) {
				// search for osgi.framework in osgi.install.area
				String installArea = (String) initialPropertyMap.get(OSGI_INSTALL_AREA);

				// only support file type URLs for install area
				if (installArea.startsWith(FILE_SCHEME))
					installArea = installArea.substring(FILE_SCHEME.length());

				String path = new File(installArea, "plugins").toString(); //$NON-NLS-1$
				path = searchFor(FRAMEWORK_BUNDLE_NAME, path);
				if (path == null)
					throw new RuntimeException("Could not find framework"); //$NON-NLS-1$

				initialPropertyMap.put(OSGI_FRAMEWORK, new File(path).toURL().toExternalForm());
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error establishing location"); //$NON-NLS-1$
		}

		return initialPropertyMap;
	}

	/**
	 * clearPrefixedSystemProperties clears System Properties by writing null properties in the targetPropertyMap that match a prefix
	 */
	private static void clearPrefixedSystemProperties(String prefix, Map targetPropertyMap) {
		for (Iterator it = System.getProperties().keySet().iterator(); it.hasNext();) {
			String propertyName = (String) it.next();
			if (propertyName.startsWith(prefix) && !targetPropertyMap.containsKey(propertyName)) {
				targetPropertyMap.put(propertyName, null);
			}
		}
	}

	/**
	 * buildCommandLineArguments parses the commandline config parameter into a set of arguments 
	 * @return an array of String containing the commandline arguments
	 */
	protected String[] buildCommandLineArguments() {
		List args = new ArrayList();

		String commandLine = config.getInitParameter(CONFIG_COMMANDLINE);
		if (commandLine != null) {
			StringTokenizer tokenizer = new StringTokenizer(commandLine, WS_DELIM);
			while (tokenizer.hasMoreTokens()) {
				String arg = tokenizer.nextToken();
				if (arg.startsWith("\"")) { //$NON-NLS-1$
					if (arg.endsWith("\"")) { //$NON-NLS-1$ 
		 				if (arg.length() >= 2) {
		 					// strip the beginning and ending quotes 
		 					arg = arg.substring(1, arg.length() - 1);
		 				}
		 			} else {
						String remainingArg = tokenizer.nextToken("\""); //$NON-NLS-1$
						arg = arg.substring(1) + remainingArg;
						// skip to next whitespace separated token
						tokenizer.nextToken(WS_DELIM);
		 			}
				} else if (arg.startsWith("'")) { //$NON-NLS-1$
		 			if (arg.endsWith("'")) { //$NON-NLS-1$ 
		 				if (arg.length() >= 2) {
		 					// strip the beginning and ending quotes 
		 					arg = arg.substring(1, arg.length() - 1);
		 				}
		 			} else {
						String remainingArg = tokenizer.nextToken("'"); //$NON-NLS-1$
						arg = arg.substring(1) + remainingArg;
						// skip to next whitespace separated token
						tokenizer.nextToken(WS_DELIM);
		 			}
				}
				args.add(arg);
			}
		}
		return (String[]) args.toArray(new String[] {});
	}

	/**
	 * stop is used to "shutdown" the framework and make it avialable for garbage collection.
	 * The default implementation also has special handling for Apache Commons Logging to "release" any
	 * resources associated with the frameworkContextClassLoader.
	 */
	public synchronized void stop() {
		if (platformDirectory == null) {
			context.log("Shutdown unnecessary. (not deployed)"); //$NON-NLS-1$
			return;
		}

		if (frameworkClassLoader == null) {
			context.log("Framework is already shutdown"); //$NON-NLS-1$
			return;
		}

		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Class clazz = frameworkClassLoader.loadClass(STARTER);
			Method method = clazz.getDeclaredMethod("shutdown", (Class[]) null); //$NON-NLS-1$
			Thread.currentThread().setContextClassLoader(frameworkContextClassLoader);
			method.invoke(clazz, (Object[]) null);

			// ACL keys its loggers off of the ContextClassLoader which prevents GC without calling release. 
			// This section explicitly calls release if ACL is used.
			try {
				clazz = this.getClass().getClassLoader().loadClass("org.apache.commons.logging.LogFactory"); //$NON-NLS-1$
				method = clazz.getDeclaredMethod("release", new Class[] {ClassLoader.class}); //$NON-NLS-1$
				method.invoke(clazz, new Object[] {frameworkContextClassLoader});
			} catch (ClassNotFoundException e) {
				// ignore, ACL is not being used
			}
		} catch (Exception e) {
			context.log("Error while stopping Framework", e); //$NON-NLS-1$
			return;
		} finally {
			frameworkClassLoader = null;
			frameworkContextClassLoader = null;
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	/**
	 * copyResource is a convenience method to recursively copy resources from the ServletContext to
	 * an installation target. The default behaviour will create a directory if the resourcepath ends
	 * in '/' and a file otherwise.
	 * @param resourcePath - The resource root path
	 * @param target - The root location where resources are to be copied
	 */
	protected void copyResource(String resourcePath, File target) {
		if (resourcePath.endsWith("/")) { //$NON-NLS-1$
			target.mkdir();
			Set paths = context.getResourcePaths(resourcePath);
			if (paths == null)
				return;
			for (Iterator it = paths.iterator(); it.hasNext();) {
				String path = (String) it.next();
				File newFile = new File(target, path.substring(resourcePath.length()));
				copyResource(path, newFile);
			}
		} else {
			try {
				if (target.createNewFile()) {
					InputStream is = null;
					OutputStream os = null;
					try {
						is = context.getResourceAsStream(resourcePath);
						if (is == null)
							return;
						os = new FileOutputStream(target);
						byte[] buffer = new byte[8192];
						int bytesRead = is.read(buffer);
						while (bytesRead != -1) {
							os.write(buffer, 0, bytesRead);
							bytesRead = is.read(buffer);
						}
					} finally {
						if (is != null)
							is.close();

						if (os != null)
							os.close();
					}
				}
			} catch (IOException e) {
				context.log("Error copying resources", e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * deleteDirectory is a convenience method to recursively delete a directory
	 * @param directory - the directory to delete.
	 * @return was the delete succesful
	 */
	protected static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}

	/**
	 * Used when to set the ContextClassLoader when the BridgeServlet delegates to a Servlet
	 * inside the framework
	 * @return a Classloader with the OSGi framework's context classloader.
	 */
	public synchronized ClassLoader getFrameworkContextClassLoader() {
		return frameworkContextClassLoader;
	}

	/**
	 * Platfom Directory is where the OSGi software is installed
	 * @return the framework install location
	 */
	protected synchronized File getPlatformDirectory() {
		return platformDirectory;
	}

	/**
	 * loadProperties is a convenience method to load properties from a servlet context resource
	 * @param resource - The target to read properties from
	 * @return the properties
	 */
	protected Properties loadProperties(String resource) {
		Properties result = new Properties();
		InputStream in = null;
		try {
			URL location = context.getResource(resource);
			if (location != null) {
				in = location.openStream();
				result.load(in);
			}
		} catch (MalformedURLException e) {
			// no url to load from
		} catch (IOException e) {
			// its ok if there is no file
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	/***************************************************************************
	 * See org.eclipse.core.launcher [copy of searchFor, findMax,
	 * compareVersion, getVersionElements] TODO: If these methods were made
	 * public and static we could use them directly
	 **************************************************************************/

	/**
	 * Searches for the given target directory starting in the "plugins" subdirectory
	 * of the given location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * @param target 
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
	protected String searchFor(final String target, String start) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File candidate) {
				return candidate.getName().equals(target) || candidate.getName().startsWith(target + "_"); //$NON-NLS-1$
			}
		};
		File[] candidates = new File(start).listFiles(filter);
		if (candidates == null)
			return null;
		String[] arrays = new String[candidates.length];
		for (int i = 0; i < arrays.length; i++) {
			arrays[i] = candidates[i].getName();
		}
		int result = findMax(arrays);
		if (result == -1)
			return null;
		return candidates[result].getAbsolutePath().replace(File.separatorChar, '/') + (candidates[result].isDirectory() ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
	}

	protected int findMax(String[] candidates) {
		int result = -1;
		Object maxVersion = null;
		for (int i = 0; i < candidates.length; i++) {
			String name = candidates[i];
			String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
			int index = name.indexOf('_');
			if (index != -1)
				version = name.substring(index + 1);
			Object currentVersion = getVersionElements(version);
			if (maxVersion == null) {
				result = i;
				maxVersion = currentVersion;
			} else {
				if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
					result = i;
					maxVersion = currentVersion;
				}
			}
		}
		return result;
	}

	/**
	 * Compares version strings. 
	 * @param left 
	 * @param right 
	 * @return result of comparison, as integer;
	 * <code><0</code> if left < right;
	 * <code>0</code> if left == right;
	 * <code>>0</code> if left > right;
	 */
	private int compareVersion(Object[] left, Object[] right) {

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

	/**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @param version 
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier). Note, that returning anything else will cause exceptions in the caller.
	 */
	private Object[] getVersionElements(String version) {
		if (version.endsWith(".jar")) //$NON-NLS-1$
			version = version.substring(0, version.length() - 4);
		Object[] result = {new Integer(0), new Integer(0), new Integer(0), ""}; //$NON-NLS-1$
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
	 * The ChildFirstURLClassLoader alters regular ClassLoader delegation and will check the URLs
	 * used in its initialization for matching classes before delegating to it's parent.
	 * Sometimes also referred to as a ParentLastClassLoader
	 */
	protected class ChildFirstURLClassLoader extends URLClassLoader {

		public ChildFirstURLClassLoader(URL[] urls) {
			super(urls);
		}

		public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
			super(urls, parent, factory);
		}

		public URL getResource(String name) {
			URL resource = findResource(name);
			if (resource == null) {
				ClassLoader parent = getParent();
				if (parent != null)
					resource = parent.getResource(name);
			}
			return resource;
		}

		protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
			Class clazz = findLoadedClass(name);
			if (clazz == null) {
				try {
					clazz = findClass(name);
				} catch (ClassNotFoundException e) {
					ClassLoader parent = getParent();
					if (parent != null)
						clazz = parent.loadClass(name);
					else
						clazz = getSystemClassLoader().loadClass(name);
				}
			}

			if (resolve)
				resolveClass(clazz);

			return clazz;
		}

		// we want to ensure that the framework has AllPermissions
		protected PermissionCollection getPermissions(CodeSource codesource) {
			return allPermissions;
		}
	}

}
