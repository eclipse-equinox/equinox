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
import java.net.URL;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;

public class BundleFragment extends Bundle {

	/** The resolved host that this fragment is attached to */
	protected BundleHost host;

	/**
	 * @param bundledata
	 * @param location
	 * @param framework
	 * @param startLevel
	 * @throws BundleException
	 */
	public BundleFragment(BundleData bundledata, String location, Framework framework, int startLevel) throws BundleException {
		super(bundledata, location, framework, startLevel);
		host = null;
	}

	/**
	 * Load the bundle.
	 * @exception org.osgi.framework.BundleException
	 */
	protected void load() throws BundleException {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED)) == 0) {
				Debug.println("Bundle.load called when state != INSTALLED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		if (framework.isActive()) {
			SecurityManager sm = System.getSecurityManager();

			if (sm != null) {
				PermissionCollection collection = framework.permissionAdmin.createPermissionCollection(this);

				domain = new ProtectionDomain(null, collection);
			}

			try {
				bundledata.open(); /* make sure the BundleData is open */
			} catch (IOException e) {
				throw new BundleException(Msg.formatter.getString("BUNDLE_READ_EXCEPTION"), e);
			}
		}
	}

	/**
	 * Changes the state from ACTIVE | RESOVED to INSTALLED.  This is called when a 
	 * host gets reloaded or unloaded.
	 * This method must be called while holding the bundles lock.
	 *
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 * @exception org.osgi.framework.BundleException
	 */
	protected boolean unresolve() {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		if (framework.isActive()) {
			if (host != null) {
				if (state == RESOLVED) {
					state = INSTALLED;
					// publish UNRESOLVED event here.
					framework.publishBundleEvent(BundleEvent.UNRESOLVED, this);
					host = null;
				}
			}
		} else {
			/* close the outgoing jarfile */
			try {
				this.bundledata.close();
			} catch (IOException e) {
				// Do Nothing
			}
		}

		return (false);
	}

	/**
	 * Reload from a new bundle.
	 * This method must be called while holding the bundles lock.
	 *
	 * @param newBundle Dummy Bundle which contains new data.
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 * @exception org.osgi.framework.BundleException
	 */
	protected boolean reload(Bundle newBundle) throws BundleException {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		boolean exporting = false;

		if (framework.isActive()) {
			if (host != null) {
				if (state == RESOLVED) {
					// Unresolving the host will cause the fragment to unresolve
					exporting = host.unresolve();
				}
			}

		}

		if (!exporting) {
			/* close the outgoing jarfile */
			try {
				this.bundledata.close();
			} catch (IOException e) {
				// Do Nothing
			}
		}

		this.bundledata = newBundle.bundledata;
		this.bundledata.setBundle(this);
		return (exporting);
	}

	/**
	 * Refresh the bundle. This is called by Framework.refreshPackages.
	 * This method must be called while holding the bundles lock.
	 * this.loader.unimportPackages must have already been called before calling
	 * this method!
	 *
	 * @exception org.osgi.framework.BundleException if an exported package is "in use". i.e. it has been imported by a bundle
	 */
	protected void refresh() throws BundleException {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (UNINSTALLED | INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.refresh called when state != UNINSTALLED | INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		if (state == RESOLVED) {
			host = null;
			state = INSTALLED;
			// Do not publish UNRESOLVED event here.  This is done by caller 
			// to resolve if appropriate.
		}
	}

	/**
	 * Unload the bundle.
	 * This method must be called while holding the bundles lock.
	 *
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 */
	protected boolean unload() {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (UNINSTALLED | INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.unload called when state != UNINSTALLED | INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		boolean exporting = false;

		if (framework.isActive()) {
			if (host != null) {
				BundleHost resumeHost = host;
				if (state == RESOLVED) {
					// Unresolving the host will cause the fragment to unresolve
					try {
						exporting = host.unresolve();
					} catch (BundleException be) {
						framework.publishFrameworkEvent(FrameworkEvent.ERROR, this, be);
					}
				}
				if (!exporting) {
					domain = null;
					try {
						this.bundledata.close();
					} catch (IOException e) { // Do Nothing.
					}
				}
				// We must resume the host now that we are unloaded.
				framework.resumeBundle(resumeHost);
			}
		} else {
			try {
				this.bundledata.close();
			} catch (IOException e) { // Do Nothing.
			}
		}

		return (exporting);
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
		// cannot load a class from a fragment because there is no classloader
		// associated with fragments.
		throw new ClassNotFoundException(Msg.formatter.getString("BUNDLE_FRAGMENT_CNFE",name));
	}

	/**
	 * Find the specified resource in this bundle.
	 *
	 * This bundle's class loader is called to search for the named resource.
	 * If this bundle's state is <tt>INSTALLED</tt>, then only this bundle will
	 * be searched for the specified resource. Imported packages cannot be searched
	 * when a bundle has not been resolved.
	 *
	 * @param name The name of the resource.
	 * See <tt>java.lang.ClassLoader.getResource</tt> for a description of
	 * the format of a resource name.
	 * @return a URL to the named resource, or <tt>null</tt> if the resource could
	 * not be found or if the caller does not have
	 * the <tt>AdminPermission</tt>, and the Java Runtime Environment supports permissions.
	 * 
	 * @exception java.lang.IllegalStateException If this bundle has been uninstalled.
	 */
	public URL getResource(String name) {
		checkValid();
		// cannot get a resource for a fragment because there is no classloader
		// associated with fragments.
		return (null);

	}

	/**
	 * Internal worker to start a bundle.
	 *
	 * @param persistent if true persistently record the bundle was started.
	 */
	protected void startWorker(boolean persistent) throws BundleException {
		if (framework.active) {
			if ((state & (STARTING | ACTIVE)) != 0) {
				return;
			}

			if (state == INSTALLED) {
				framework.packageAdmin.resolveBundles();

				if (state != RESOLVED) {
					throw new BundleException(getResolutionFailureMessage());
				}
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Bundle: Active sl = " + framework.startLevelImpl.getStartLevel() + "; Bundle " + id + " sl = " + this.startLevel);
			}

			if (this.startLevel <= framework.startLevelImpl.getStartLevel()) {
				if (state == UNINSTALLED) {
					throw new BundleException(Msg.formatter.getString("BUNDLE_UNINSTALLED_EXCEPTION"));
				}
				if (framework.active) {
					state = ACTIVE;

					if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
						Debug.println("->started " + this);
					}

					framework.publishBundleEvent(BundleEvent.STARTED, this);
				}
			}
		}

		if (persistent) {
			setStatus(Constants.BUNDLE_STARTED, true);
		}
	}

	/**
	 * Internal worker to stop a bundle.
	 *
	 * @param persistent if true persistently record the bundle was stopped.
	 */
	protected void stopWorker(boolean persistent) throws BundleException {
		if (persistent) {
			setStatus(Constants.BUNDLE_STARTED, false);
		}

		if (framework.active) {
			if ((state & (STOPPING | RESOLVED | INSTALLED)) != 0) {
				return;
			}

			state = RESOLVED;

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("->stopped " + this);
			}

			framework.publishBundleEvent(BundleEvent.STOPPED, this);
		}
	}

	/**
	 * Provides a list of {@link ServiceReference}s for the services
	 * registered by this bundle
	 * or <code>null</code> if the bundle has no registered
	 * services.
	 *
	 * <p>The list is valid at the time
	 * of the call to this method, but the framework is a very dynamic
	 * environment and services can be modified or unregistered at anytime.
	 *
	 * @return An array of {@link ServiceReference} or <code>null</code>.
	 * @exception java.lang.IllegalStateException If the
	 * bundle has been uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 */
	public ServiceReference[] getRegisteredServices() {
		checkValid();
		// Fragments cannot have a BundleContext and therefore
		// cannot have any services registered.
		return null;
	}

	/**
	 * Provides a list of {@link ServiceReference}s for the
	 * services this bundle is using,
	 * or <code>null</code> if the bundle is not using any services.
	 * A bundle is considered to be using a service if the bundle's
	 * use count for the service is greater than zero.
	 *
	 * <p>The list is valid at the time
	 * of the call to this method, but the framework is a very dynamic
	 * environment and services can be modified or unregistered at anytime.
	 *
	 * @return An array of {@link ServiceReference} or <code>null</code>.
	 * @exception java.lang.IllegalStateException If the
	 * bundle has been uninstalled.
	 * @see ServiceReference
	 */
	public ServiceReference[] getServicesInUse() {
		checkValid();
		// Fragments cannot have a BundleContext and therefore
		// cannot have any services in use.
		return null;
	}

	public org.osgi.framework.Bundle getHost() {
		return host;
	}

	public boolean isFragment() {
		return true;
	}

	/**
	 * Sets the host for this fragment from the list of available
	 * BundleExporters.  If a matching host cannot be found then a
	 * resolve Exception is logged.
	 * @param exporters The available BundleExporters to resolve the host from.
	 */
	protected boolean setHost(BundleHost value) {
		host = value;
		if (host != null) {
			try {
				host.attachFragment(this);
			} catch (BundleException be) {
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, host, be);
				return false;
			}
		}
		return true;
		// TODO detach the fragment if the host is null???
	}

	public BundleLoader getBundleLoader() {
		// Fragments cannot have a BundleLoader.
		return null;
	}

	/**
	 * Return the current context for this bundle.
	 *
	 * @return BundleContext for this bundle.
	 */
	protected BundleContext getContext() {
		// Fragments cannot have a BundleContext.
		return null;
	}
}
