/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * This class subclasses Bundle to provide a system Bundle
 * so that the framework can be represented as a bundle and
 * can access the services provided by other bundles.
 */

public class InternalSystemBundle extends BundleHost implements org.osgi.framework.launch.Framework {
	class SystemBundleHeaders extends Dictionary<String, String> {
		private final Dictionary<String, String> headers;

		public SystemBundleHeaders(Dictionary<String, String> headers) {
			this.headers = headers;
		}

		public Enumeration<String> elements() {
			return headers.elements();
		}

		public String get(Object key) {
			if (!(key instanceof String))
				return null;
			if (org.osgi.framework.Constants.EXPORT_PACKAGE.equalsIgnoreCase((String) key)) {
				return getExtra(org.osgi.framework.Constants.EXPORT_PACKAGE, org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES, org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
			} else if (org.osgi.framework.Constants.PROVIDE_CAPABILITY.equalsIgnoreCase((String) key)) {
				return getExtra(org.osgi.framework.Constants.PROVIDE_CAPABILITY, org.osgi.framework.Constants.FRAMEWORK_SYSTEMCAPABILITIES, org.osgi.framework.Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA);
			}
			return headers.get(key);
		}

		private String getExtra(String header, String systemProp, String systemExtraProp) {
			String systemValue = FrameworkProperties.getProperty(systemProp);
			String systemExtraValue = FrameworkProperties.getProperty(systemExtraProp);
			if (systemValue == null)
				systemValue = systemExtraValue;
			else if (systemExtraValue != null && systemExtraValue.trim().length() > 0)
				systemValue += ", " + systemExtraValue; //$NON-NLS-1$
			String result = headers.get(header);
			if (systemValue != null && systemValue.trim().length() > 0) {
				if (result != null)
					result += ", " + systemValue; //$NON-NLS-1$
				else
					result = systemValue;
			}
			return result;
		}

		public boolean isEmpty() {
			return headers.isEmpty();
		}

		public Enumeration<String> keys() {
			return headers.keys();
		}

		public String put(String key, String value) {
			return headers.put(key, value);
		}

		public String remove(Object key) {
			return headers.remove(key);
		}

		public int size() {
			return headers.size();
		}

	}

	private final FrameworkStartLevel fsl;
	ProtectionDomain systemDomain;

	/**
	 * Private SystemBundle object constructor.
	 * This method creates the SystemBundle and its BundleContext.
	 * The SystemBundle's state is set to STARTING.
	 * This method is called when the framework is constructed.
	 *
	 * @param framework Framework this bundle is running in
	 */
	protected InternalSystemBundle(Framework framework) throws BundleException {
		super(framework.adaptor.createSystemBundleData(), framework); // startlevel=0 means framework stopped
		Constants.setInternalSymbolicName(bundledata.getSymbolicName());
		state = Bundle.RESOLVED;
		context = createContext();
		fsl = new EquinoxStartLevel();
	}

	/**
	 * Load the bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 */
	protected void load() {
		SecurityManager sm = System.getSecurityManager();

		if (sm != null) {
			systemDomain = getClass().getProtectionDomain();
		}
	}

	/**
	 * Reload from a new bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 * @param newBundle
	 * @return false
	 */
	protected boolean reload(AbstractBundle newBundle) {
		return (false);
	}

	/**
	 * Refresh the bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 */
	protected void refresh() {
		// do nothing
	}

	/**
	 * Unload the bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 * @return false
	 */
	protected boolean unload() {
		return (false);
	}

	/**
	 * Close the the Bundle's file.
	 * This method closes the BundleContext for the SystemBundle.
	 *
	 */
	protected void close() {
		context.close();
		context = null;
	}

	/**
	 * This method loads a class from the bundle.
	 *
	 * @param      name     the name of the desired Class.
	 * @param      checkPermission indicates whether a permission check should be done.
	 * @return     the resulting Class
	 * @exception  java.lang.ClassNotFoundException  if the class definition was not found.
	 */
	protected Class<?> loadClass(String name, boolean checkPermission) throws ClassNotFoundException {
		if (checkPermission) {
			framework.checkAdminPermission(this, AdminPermission.CLASS);
			checkValid();
		}
		return (Class.forName(name));
	}

	/**
	 * Find the specified resource in this bundle.
	 * This methods returns null for the system bundle.
	 */
	public URL getResource(String name) {
		return (null);
	}

	/**
	 * Indicate SystemBundle is resolved.
	 *
	 */
	protected boolean isUnresolved() {
		return (false);
	}

	/**
	 * Start this bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 */
	public void start() {
		framework.checkAdminPermission(this, AdminPermission.EXECUTE);
	}

	public void start(int options) {
		framework.checkAdminPermission(this, AdminPermission.EXECUTE);
	}

	/**
	 * Start the SystemBundle.
	 * This method launches the framework.
	 *
	 */
	protected void resume() {
		/* initialize the startlevel service */
		framework.startLevelManager.initialize();

		/* Load all installed bundles */
		loadInstalledBundles(framework.startLevelManager.getInstalledBundles(framework.bundles, false));
		/* Start the system bundle */
		try {
			framework.systemBundle.state = Bundle.STARTING;
			framework.systemBundle.context.start();
			framework.publishBundleEvent(BundleEvent.STARTING, framework.systemBundle);
		} catch (BundleException be) {
			if (Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: Bundle resume exception: " + be.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(be.getNestedException() == null ? be : be.getNestedException());
			}
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, be);
			throw new RuntimeException(be.getMessage(), be);
		}

	}

	private void loadInstalledBundles(AbstractBundle[] installedBundles) {

		for (int i = 0; i < installedBundles.length; i++) {
			AbstractBundle bundle = installedBundles[i];
			if (Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: Trying to load bundle " + bundle); //$NON-NLS-1$
			}
			bundle.load();
		}
	}

	/**
	 * Stop the framework.
	 * This method spawns a thread which will call framework.shutdown.
	 *
	 */
	public void stop() {
		framework.checkAdminPermission(this, AdminPermission.EXECUTE);

		if ((state & (ACTIVE | STARTING)) != 0) {
			Thread shutdown = framework.secureAction.createThread(new Runnable() {
				public void run() {
					try {
						framework.close();
					} catch (Throwable t) {
						// allow the adaptor to handle this unexpected error
						framework.adaptor.handleRuntimeError(t);
					}
				}
			}, "System Bundle Shutdown", framework.getContextFinder()); //$NON-NLS-1$

			shutdown.start();
		}
	}

	public void stop(int options) {
		stop();
	}

	/**
	 * Stop the SystemBundle.
	 * This method shuts down the framework.
	 *
	 */
	protected void suspend() {

		framework.startLevelManager.shutdown();
		framework.startLevelManager.cleanup();

		/* clean up the exporting loaders */
		framework.packageAdmin.cleanup();

		if (Debug.DEBUG_GENERAL) {
			Debug.println("->Framework shutdown"); //$NON-NLS-1$
		}
		// fire the STOPPED event here.
		// All bundles have been unloaded, but there may be a boot strap listener that is interested (bug 182742)
		framework.publishBundleEvent(BundleEvent.STOPPED, this);
	}

	protected void suspend(boolean lock) {
		// do nothing
	}

	/**
	 * Update this bundle.
	 * This method spawns a thread which will call framework.shutdown
	 * followed by framework.launch.
	 *
	 */
	public void update() {
		framework.checkAdminPermission(this, AdminPermission.LIFECYCLE);

		if ((state & (ACTIVE | STARTING)) != 0) {
			Thread restart = framework.secureAction.createThread(new Runnable() {
				public void run() {
					int sl = framework.startLevelManager.getStartLevel();
					FrameworkProperties.setProperty(Constants.PROP_OSGI_RELAUNCH, ""); //$NON-NLS-1$
					framework.shutdown(FrameworkEvent.STOPPED_UPDATE);
					framework.launch();
					if (sl > 0)
						framework.startLevelManager.doSetStartLevel(sl);
					FrameworkProperties.clearProperty(Constants.PROP_OSGI_RELAUNCH);
				}
			}, "System Bundle Update", framework.getContextFinder()); //$NON-NLS-1$

			restart.start();
		}
	}

	/**
	 * Update this bundle from an InputStream.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 * @param in The InputStream from which to read the new bundle.
	 */
	public void update(InputStream in) {
		update();

		try {
			in.close();
		} catch (IOException e) {
			// do nothing
		}
	}

	/**
	 * Uninstall this bundle.
	 * This methods overrides the Bundle method and throws an exception.
	 *
	 */
	public void uninstall() throws BundleException {
		framework.checkAdminPermission(this, AdminPermission.LIFECYCLE);

		throw new BundleException(Msg.BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION, BundleException.INVALID_OPERATION);
	}

	/**
	 * Determine whether the bundle has the requested
	 * permission.
	 * This methods overrides the Bundle method and returns <code>true</code>.
	 *
	 * @param permission The requested permission.
	 * @return <code>true</code>
	 */
	public boolean hasPermission(Object permission) {
		if (systemDomain != null) {
			if (permission instanceof Permission) {
				return systemDomain.implies((Permission) permission);
			}

			return false;
		}

		return true;
	}

	/**
	 * No work to do for the SystemBundle.
	 *
	 * @param refreshedBundles
	 *            A list of bundles which have been refreshed as a result
	 *            of a packageRefresh
	 */
	protected void unresolvePermissions(AbstractBundle[] refreshedBundles) {
		// Do nothing
	}

	public Dictionary<String, String> getHeaders(String localeString) {
		return new SystemBundleHeaders(super.getHeaders(localeString));
	}

	public void init() {
		// no op for internal representation
	}

	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		return framework.waitForStop(timeout);
	}

	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <A> A adapt0(Class<A> adapterType) {
		if (FrameworkStartLevel.class.equals(adapterType))
			return (A) fsl;
		else if (FrameworkWiring.class.equals(adapterType))
			return (A) framework.getPackageAdmin();
		return super.adapt0(adapterType);
	}

	class EquinoxStartLevel implements FrameworkStartLevel {
		public void setStartLevel(int startlevel, FrameworkListener... listeners) {
			framework.startLevelManager.setStartLevel(startlevel, InternalSystemBundle.this, listeners);
		}

		public int getInitialBundleStartLevel() {
			return framework.startLevelManager.getInitialBundleStartLevel();
		}

		public void setInitialBundleStartLevel(int startlevel) {
			framework.startLevelManager.setInitialBundleStartLevel(startlevel);
		}

		public Bundle getBundle() {
			return InternalSystemBundle.this;
		}

		public int getStartLevel() {
			return framework.startLevelManager.getStartLevel();
		}
	}
}
