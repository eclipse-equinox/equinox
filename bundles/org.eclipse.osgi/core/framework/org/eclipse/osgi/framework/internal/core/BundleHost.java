/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
import org.eclipse.osgi.framework.adaptor.BundleWatcher;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.util.SecureAction;
import org.osgi.framework.*;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;

public class BundleHost extends Bundle {

	/** Loaded state object */
	protected BundleLoader loader;

	/** 
	 * The BundleLoader proxy; a lightweight object that acts as a proxy
	 * to the BundleLoader and allows lazy creation of the BundleLoader object
	 */
	private BundleLoaderProxy proxy;	

	/** The BundleContext that represents this Bundle and all of its fragments */
	protected BundleContext context;

	/** The List of BundleFragments */
	protected BundleFragment[] fragments;

	public BundleHost(BundleData bundledata, Framework framework) throws BundleException {
		super(bundledata, framework);
		context = null;
		loader = null;
		fragments = null;
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
			if (loader != null) {
				Debug.println("Bundle.load called when loader != null: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		if (framework.isActive()) {
			SecurityManager sm = System.getSecurityManager();

			if (sm != null) {
				PermissionCollection collection = framework.permissionAdmin.createPermissionCollection(this);

				domain = new ProtectionDomain(null, collection);
			}

		}
		loader = null;

	}

	/**
	 * Changes the state from ACTIVE | RESOVED to INSTALLED.  This is called when a 
	 * fragment gets reloaded or unloaded.
	 * This method must be called while holding the bundles lock.
	 *
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 * @exception org.osgi.framework.BundleException
	 */
	protected boolean unresolve() throws BundleException {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		boolean exporting = false;

		if (framework.isActive()) {

			if (state == RESOLVED) {
				// suspend and acquire the state change lock
				if (isActive()) {
					boolean suspended = framework.suspendBundle(this, true);
					if (!suspended) {
						throw new BundleException(Msg.formatter.getString("BUNDLE_STATE_CHANGE_EXCEPTION"));
					}
				} else {
					beginStateChange();
				}

				BundleLoaderProxy curProxy = getLoaderProxy();
				exporting = curProxy.inUse();
				if (exporting) {
					// make sure the BundleLoader is created so it can
					// be added to the removalPending list.
					curProxy.getBundleLoader();
					framework.packageAdmin.addRemovalPending(curProxy);
				} else {
					framework.packageAdmin.unexportResources(curProxy);
					if (loader != null) {
						loader.clear();
						loader.close();
					}
					framework.bundles.unMarkDependancies(curProxy);
				}

				unresolveFragments();
				loader = null;
				fragments = null;
				proxy = null;
				state = INSTALLED;
				// publish UNRESOLVED event here
				framework.publishBundleEvent(BundleEvent.UNRESOLVED, this);
				completeStateChange();
			}
		} else {
			/* close the outgoing jarfile */
			try {
				this.bundledata.close();
			} catch (IOException e) {
				// Do Nothing
			}
		}

		return (exporting);
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
			if (state == RESOLVED) {
				BundleLoaderProxy curProxy = getLoaderProxy();
				exporting = curProxy.inUse();
				if (exporting) {
					// make sure the BundleLoader is created so it can
					// be added to the removalPending list.
					curProxy.getBundleLoader();
					framework.packageAdmin.addRemovalPending(curProxy);
				} else {
					framework.packageAdmin.unexportResources(curProxy);
					if (loader != null) {
						loader.clear();
						loader.close();
					}
					framework.bundles.unMarkDependancies(curProxy);
				}
				state = INSTALLED;
				// publish UNRESOLVED event here
				framework.publishBundleEvent(BundleEvent.UNRESOLVED, this);
				loader = null;
				proxy = null;
				unresolveFragments();
				fragments = null;
			}

		} else {
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
	 * Unresolves all attached fragments.  This is called any time the host
	 * bundle is reloaded, unloaded or is unresovled.
	 *
	 */
	protected void unresolveFragments() {
		if (fragments != null) {
			for (int i = 0; i < fragments.length; i++) {
				fragments[i].unresolve();
			}
		}
	}

	/**
	 * Refresh the bundle. This is called by Framework.refreshPackages.
	 * This method must be called while holding the bundles lock.
	 *
	 * @exception org.osgi.framework.BundleException if an exported package is "in use". i.e. it has been imported by a bundle
	 */
	protected void refresh() throws BundleException {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (UNINSTALLED | INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != UNINSTALLED | INSTALLED | RESOLVED: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}
		if (state == RESOLVED) {
			BundleLoaderProxy curProxy = getLoaderProxy();
			framework.packageAdmin.unexportResources(curProxy);
			if (loader != null) {
				loader.clear();
				loader.close();
			}
			framework.bundles.unMarkDependancies(curProxy);
			loader = null;
			proxy = null;
			fragments = null;
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
			if (state == RESOLVED) {
				BundleLoaderProxy curProxy = getLoaderProxy();
				exporting = curProxy.inUse();
				if (exporting) {
					// make sure the BundleLoader is created so it can
					// be added to the removalPending list.
					curProxy.getBundleLoader();
					framework.packageAdmin.addRemovalPending(curProxy);
				} else {
					framework.packageAdmin.unexportResources(curProxy);
					if (loader != null) {
						loader.clear();
						loader.close();
					}
					framework.bundles.unMarkDependancies(curProxy);
				}

				state = INSTALLED;
				// publish UNRESOLVED event here
				framework.publishBundleEvent(BundleEvent.UNRESOLVED, this);
				loader = null;
				proxy = null;
				unresolveFragments();
				fragments = null;
				domain = null;
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
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if ((state & (STARTING | ACTIVE | STOPPING)) == 0) {
				Debug.println("Bundle.loadClass(" + name + ") called when state != STARTING | ACTIVE | STOPPING: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		getBundleLoader();
		if (loader == null) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Bundle.loadClass(" + name + ") called when loader == null: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
			throw new ClassNotFoundException(name);
		}

		return (loader.loadClass(name));
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

		getBundleLoader();
		if (loader == null) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Bundle.getResource(" + name + ") called when loader == null: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}

			return (null);
		}

		return (loader.getResource(name));
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

			//STARTUP TIMING Start here
			if (Debug.DEBUG && Debug.DEBUG_MONITOR_BUNDLES) {
				BundleWatcher bundleStats = framework.adaptor.getBundleStats();
				if (bundleStats != null)
					bundleStats.startActivation(this);
			}

			try {
				if (state == INSTALLED) {
					framework.packageAdmin.resolveBundles();

					if (state != RESOLVED) {
						throw new BundleException(getResolutionFailureMessage());
					}
				}

				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Bundle: Active sl = " + framework.startLevelImpl.getStartLevel() + "; Bundle " + getBundleId() + " sl = " + getStartLevel());
				}

				if (getStartLevel() <= framework.startLevelImpl.getStartLevel()) {
					state = STARTING;

					context = createContext();
					try {
						context.start();

						if (framework.active) {
							state = ACTIVE;

							if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
								Debug.println("->started " + this);
							}

							framework.publishBundleEvent(BundleEvent.STARTED, this);
						}

					} catch (BundleException e) {
						context.close();
						context = null;

						state = RESOLVED;

						throw e;
					}

					if (state == UNINSTALLED) {
						context.close();
						context = null;
						throw new BundleException(Msg.formatter.getString("BUNDLE_UNINSTALLED_EXCEPTION", getLocation())); //$NON-NLS-1$
					}
				}
			} finally {
				if (Debug.DEBUG && Debug.DEBUG_MONITOR_BUNDLES) {
					BundleWatcher bundleStats = framework.adaptor.getBundleStats();
					if (bundleStats != null)
						bundleStats.endActivation(this);
				}
			}
		}

		if (persistent) {
			setStatus(Constants.BUNDLE_STARTED, true);
		}
	}

	/**
	 * Create a BundleContext for this bundle.
	 *
	 * @return BundleContext for this bundle.
	 */
	protected BundleContext createContext() {
		return (new BundleContext(this));
	}

	/**
	 * Return the current context for this bundle.
	 *
	 * @return BundleContext for this bundle.
	 */
	protected BundleContext getContext() {
		return (context);
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

			state = STOPPING;

			try {
				context.stop();
			} finally {
				context.close();
				context = null;

				checkValid();

				state = RESOLVED;

				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("->stopped " + this);
				}

				framework.publishBundleEvent(BundleEvent.STOPPED, this);
			}
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
	public org.osgi.framework.ServiceReference[] getRegisteredServices() {
		checkValid();

		if (context == null) {
			return (null);
		}

		return (context.getRegisteredServices());
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
	public org.osgi.framework.ServiceReference[] getServicesInUse() {
		checkValid();

		if (context == null) {
			return (null);
		}

		return (context.getServicesInUse());
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.Bundle#getFragments()
	 */
	public org.osgi.framework.Bundle[] getFragments() {
		synchronized (framework.bundles) {
			if (fragments == null)
				return null;
			org.osgi.framework.Bundle[] result = new org.osgi.framework.Bundle[fragments.length];
			System.arraycopy(fragments,0,result,0,result.length);
			return result;
		}
	}

	/**
	 * Attaches a fragment to this BundleHost.  Fragments must be attached to
	 * the host by ID order.  If the ClassLoader of the host is already created
	 * then the fragment must be attached to the host ClassLoader
	 * @param fragment The fragment bundle to attach
	 * return true if the fragment successfully attached; false if the fragment
	 * could not be logically inserted at the end of the fragment chain.
	 */
	protected void attachFragment(BundleFragment fragment) throws BundleException {
		if (fragments == null) {
			fragments = new BundleFragment[] {fragment};
		} else {
			boolean inserted = false;
			// We must keep our fragments ordered by bundle ID; or 
			// install order.
			BundleFragment[] newFragments = new BundleFragment[fragments.length+1];
			for (int i = 0; i < fragments.length; i++) {
				if (!inserted && fragment.getBundleId() < fragments[i].getBundleId()) {
					// if the loader has already been created
					// then we cannot attach a fragment into the middle
					// of the fragment chain.
					if (loader != null) {
						throw new BundleException(Msg.formatter.getString("BUNDLE_LOADER_ATTACHMENT_ERROR", fragments[i].getSymbolicName(),getSymbolicName())); //$NON-NLS-1$
					}
					newFragments[i] = fragment;
					inserted = true;
				}
				newFragments[inserted ? i+1 : i] = fragments[i]; 
			}
		}

		// If the Host ClassLoader has exists then we must attach
		// the fragment to the ClassLoader.
		if (loader != null) {
			loader.attachFragment(fragment, SecureAction.getProperties());
		}

	}

	public BundleLoader getBundleLoader() {
		if (loader == null) {
			synchronized (this) {
				if (loader == null)
					try {
						loader = new BundleLoader(this, getBundleDescription());
						getLoaderProxy().setBundleLoader(loader);
					} catch (BundleException e) {
						framework.publishFrameworkEvent(FrameworkEvent.ERROR,this,e);
						return null;
					}
			}
		}
		return loader;
	}

	protected BundleLoaderProxy getLoaderProxy() {
		if (proxy == null) {
			synchronized (this) {
				if (proxy == null) {
					proxy = new BundleLoaderProxy(this);
				}
			}
		}
		return proxy;
	}

}
