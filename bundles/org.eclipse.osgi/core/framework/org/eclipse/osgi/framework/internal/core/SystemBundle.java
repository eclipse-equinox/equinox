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

package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.util.Hashtable;
import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * This class subclasses Bundle to provide a system Bundle
 * so that the framework can be represented as a bundle and
 * can access the services provided by other bundles.
 */

public class SystemBundle extends BundleHost {

	/**
	 * Private SystemBundle object constructor.
	 * This method creates the SystemBundle and its BundleContext.
	 * The SystemBundle's state is set to STARTING.
	 * This method is called when the framework is constructed.
	 *
	 * @param bundledata the bundle's BundleData
	 * @param location identity string for the bundle
	 * @param framework Framework this bundle is running in
	 */
	protected SystemBundle(Framework framework) throws BundleException {
		super(framework.adaptor.createSystemBundleData(), framework); // startlevel=0 means framework stopped
		state = STARTING;
		context = createContext();
	}

	public BundleLoader getBundleLoader() {
		if (loader == null) {
			synchronized (this) {
				if (loader == null)
					try {
						loader = new SystemBundleLoader(this, getBundleDescription());
					} catch (BundleException e) {
						framework.publishFrameworkEvent(FrameworkEvent.ERROR,this,e);
						return null;
					}
			}
		}
		return loader;
	}
	/**
	 * Load the bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 */
	protected void load() throws BundleException {
		SecurityManager sm = System.getSecurityManager();

		if (sm != null) {
			domain = getClass().getProtectionDomain();
		}
	}

	/**
	 * Reload from a new bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 * @param newBundle
	 * @return false
	 */
	protected boolean reload(Bundle newBundle) throws BundleException {
		return (false);
	}

	/**
	 * Refresh the bundle.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 */
	protected void refresh() throws BundleException {
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
	 * This method closes the BundleContext for the SystemBundle
	 * and sets the SystemBundle's state to UNINSTALLED.
	 *
	 */
	protected void close() {
		context.close();
		context = null;

		state = UNINSTALLED;
	}

	/**
	 * This method loads a class from the bundle.
	 *
	 * @param      name     the name of the desired Class.
	 * @param      checkPermission indicates whether a permission check should be done.
	 * @return     the resulting Class
	 * @exception  java.lang.ClassNotFoundException  if the class definition was not found.
	 */
	protected Class loadClass(String name, boolean checkPermission) throws ClassNotFoundException {
		if (checkPermission) {
			framework.checkAdminPermission();
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
	public void start() throws BundleException {
		framework.checkAdminPermission();
	}

	/**
	 * Start the SystemBundle.
	 * This method launches the framework.
	 *
	 */
	protected void resume() throws BundleException {
		/* initialize the startlevel service */
		framework.startLevelImpl.initialize();

		framework.startLevelImpl.launch(framework.startLevelImpl.getFrameworkStartLevel());

	}

	/**
	 * Stop the framework.
	 * This method spawns a thread which will call framework.shutdown.
	 *
	 */
	public void stop() throws BundleException {
		framework.checkAdminPermission();

		if (state == ACTIVE) {
			Thread shutdown = framework.createThread(new Runnable() {
				public void run() {
					framework.shutdown();
				}
			}, "System Bundle Shutdown");

			shutdown.start();
		}
	}

	/**
	 * Stop the SystemBundle.
	 * This method shuts down the framework.
	 *
	 */
	protected void suspend() throws BundleException {

		framework.startLevelImpl.shutdown();
		framework.startLevelImpl.cleanup();

		/* clean up the exporting loaders */
		framework.packageAdmin.cleanup();

		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			Debug.println("->Framework shutdown");
		}
	}

	/**
	 * Update this bundle.
	 * This method spawns a thread which will call framework.shutdown
	 * followed by framework.launch.
	 *
	 */
	public void update() throws BundleException {
		framework.checkAdminPermission();

		if (state == ACTIVE) {
			Thread restart = framework.createThread(new Runnable() {
				public void run() {
					framework.shutdown();

					framework.launch();
				}
			}, "System Bundle Update");

			restart.start();
		}
	}

	/**
	 * Update this bundle from an InputStream.
	 * This methods overrides the Bundle method and does nothing.
	 *
	 * @param in The InputStream from which to read the new bundle.
	 */
	public void update(InputStream in) throws BundleException {
		update();

		try {
			in.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Uninstall this bundle.
	 * This methods overrides the Bundle method and throws an exception.
	 *
	 */
	public void uninstall() throws BundleException {
		framework.checkAdminPermission();

		throw new BundleException(Msg.formatter.getString("BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION"));
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
		if (domain != null) {
			if (permission instanceof Permission) {
				return domain.implies((Permission) permission);
			}

			return false;
		}

		return true;
	}

	/**
	 * No work to do for the SystemBundle.
	 *
	 * @param unresolvedPackages A list of the package which have been unresolved
	 * as a result of a packageRefresh
	 */
	protected void unresolvePermissions(Hashtable unresolvedPackages) {
	}

	/*
	 * The System Bundle always has permission to do anything;
	 * override the check*Permission methods to always return true.
	 */
	protected boolean checkExportPackagePermission(String pkgName) {
		return true;
	}
	protected boolean checkFragmentBundlePermission(String symbolicName) {
		return true;
	}
	protected boolean checkFragmentHostPermission(String symbolicName) {
		return true;
	}
	protected boolean checkImportPackagePermission(String pkgName) {
		return true;
	}
	protected boolean checkPermissions() {
		return true;
	}
	protected boolean checkProvideBundlePermission(String symbolicName) {
		return true;
	}
	protected boolean checkRequireBundlePermission(String symbolicName) {
		return true;
	}
}
