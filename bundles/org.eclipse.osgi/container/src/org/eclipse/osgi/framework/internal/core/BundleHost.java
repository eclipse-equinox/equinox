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
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverHookException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class BundleHost extends AbstractBundle {
	public static final int LAZY_TRIGGER = 0x40000000;
	/** 
	 * The BundleLoader proxy; a lightweight object that acts as a proxy
	 * to the BundleLoader and allows lazy creation of the BundleLoader object
	 */
	private BundleLoaderProxy proxy;

	/** The BundleContext that represents this Bundle and all of its fragments */
	protected BundleContextImpl context;

	/** The List of BundleFragments */
	protected BundleFragment[] fragments;

	public BundleHost(BundleData bundledata, Framework framework) {
		super(bundledata, framework);
		context = null;
		fragments = null;
	}

	/**
	 * Load the bundle.
	 */
	protected void load() {
		if (Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED)) == 0) {
				Debug.println("Bundle.load called when state != INSTALLED: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
			if (proxy != null) {
				Debug.println("Bundle.load called when proxy != null: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}

		if (framework.isActive()) {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null && framework.securityAdmin != null) {
				domain = framework.securityAdmin.createProtectionDomain(this);
			}
		}
		proxy = null;
	}

	/**
	 * Reload from a new bundle.
	 * This method must be called while holding the bundles lock.
	 *
	 * @param newBundle Dummy Bundle which contains new data.
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 */
	protected boolean reload(AbstractBundle newBundle) {
		if (Debug.DEBUG_GENERAL) {
			if ((state & (INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != INSTALLED | RESOLVED: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}

		boolean exporting = false;

		if (framework.isActive()) {
			if (state == RESOLVED) {
				BundleLoaderProxy curProxy = getLoaderProxy();
				exporting = curProxy.inUse();
				if (exporting) {
					// make sure the BundleLoader is created.
					curProxy.getBundleLoader().createClassLoader();
				} else
					BundleLoader.closeBundleLoader(proxy);
				state = INSTALLED;
				proxy = null;
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
		// create a new domain for the bundle because its signers/symbolic-name may have changed
		if (framework.isActive() && System.getSecurityManager() != null && framework.securityAdmin != null)
			domain = framework.securityAdmin.createProtectionDomain(this);
		return (exporting);
	}

	/**
	 * Refresh the bundle. This is called by Framework.refreshPackages.
	 * This method must be called while holding the bundles lock.
	 */
	protected void refresh() {
		if (Debug.DEBUG_GENERAL) {
			if ((state & (UNINSTALLED | INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.reload called when state != UNINSTALLED | INSTALLED | RESOLVED: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}
		if (state == RESOLVED) {
			BundleLoader.closeBundleLoader(proxy);
			proxy = null;
			fragments = null;
			state = INSTALLED;
			// Do not publish UNRESOLVED event here.  This is done by caller 
			// to resolve if appropriate.
		}
		manifestLocalization = null;
	}

	/**
	 * Unload the bundle.
	 * This method must be called while holding the bundles lock.
	 *
	 * @return  true if an exported package is "in use". i.e. it has been imported by a bundle
	 */
	protected boolean unload() {
		if (Debug.DEBUG_GENERAL) {
			if ((state & (UNINSTALLED | INSTALLED | RESOLVED)) == 0) {
				Debug.println("Bundle.unload called when state != UNINSTALLED | INSTALLED | RESOLVED: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}

		boolean exporting = false;

		if (framework.isActive()) {
			if (state == RESOLVED) {
				BundleLoaderProxy curProxy = getLoaderProxy();
				exporting = curProxy.inUse();
				if (exporting) {
					// make sure the BundleLoader is created.
					curProxy.getBundleLoader().createClassLoader();
				} else
					BundleLoader.closeBundleLoader(proxy);

				state = INSTALLED;
				proxy = null;
				fragments = null;
				domain = null;
			}
		}
		if (!exporting) {
			try {
				this.bundledata.close();
			} catch (IOException e) { // Do Nothing.
			}
		}

		return (exporting);
	}

	private BundleLoader checkLoader() {
		checkValid();

		// check to see if the bundle is resolved
		if (!isResolved()) {
			if (!framework.packageAdmin.resolveBundles(new Bundle[] {this})) {
				return null;
			}
		}
		if (Debug.DEBUG_GENERAL) {
			if ((state & (STARTING | ACTIVE | STOPPING | RESOLVED)) == 0) {
				Debug.println("Bundle.checkLoader() called when state != STARTING | ACTIVE | STOPPING | RESOLVED: " + this); //$NON-NLS-1$ 
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}

		BundleLoader loader = getBundleLoader();
		if (loader == null) {
			if (Debug.DEBUG_GENERAL) {
				Debug.println("Bundle.checkLoader() called when loader == null: " + this); //$NON-NLS-1$ 
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
			return null;
		}
		return loader;
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
			try {
				framework.checkAdminPermission(this, AdminPermission.CLASS);
			} catch (SecurityException e) {
				throw new ClassNotFoundException(name, e);
			}
		}
		BundleLoader loader = checkLoader();
		if (loader == null)
			throw new ClassNotFoundException(NLS.bind(Msg.BUNDLE_CNFE_NOT_RESOLVED, name, getBundleData().getLocation()));
		try {
			return (loader.loadClass(name));
		} catch (ClassNotFoundException e) {
			// this is to support backward compatibility in eclipse
			// we always attempted to start a bundle even if the class was not found
			if (!(e instanceof StatusException) && (bundledata.getStatus() & Constants.BUNDLE_LAZY_START) != 0 && !testStateChanging(Thread.currentThread()))
				try {
					// only start the bundle if this is a simple CNFE
					loader.setLazyTrigger();
				} catch (BundleException be) {
					framework.adaptor.getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, be.getMessage(), 0, be, null));
				}
			throw e;
		}
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
		BundleLoader loader = null;
		try {
			framework.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException ee) {
			return null;
		}
		loader = checkLoader();
		if (loader == null) {
			Enumeration<URL> result = bundledata.findLocalResources(name);
			if (result != null && result.hasMoreElements())
				return result.nextElement();
			return null;
		}
		return loader.findResource(name);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		BundleLoader loader = null;
		try {
			framework.checkAdminPermission(this, AdminPermission.RESOURCE);
		} catch (SecurityException ee) {
			return null;
		}
		Enumeration<URL> result;
		loader = checkLoader();
		if (loader == null)
			result = bundledata.findLocalResources(name);
		else
			result = loader.getResources(name);
		if (result != null && result.hasMoreElements())
			return result;
		return null;
	}

	/**
	 * Internal worker to start a bundle.
	 *
	 * @param options the start options
	 */
	protected void startWorker(int options) throws BundleException {
		if ((options & START_TRANSIENT) == 0) {
			setStatus(Constants.BUNDLE_STARTED, true);
			setStatus(Constants.BUNDLE_ACTIVATION_POLICY, (options & START_ACTIVATION_POLICY) != 0);
			if (Debug.MONITOR_ACTIVATION)
				new Exception("A persistent start has been called on bundle: " + getBundleData()).printStackTrace(); //$NON-NLS-1$
		}
		if (!framework.active || (state & ACTIVE) != 0)
			return;
		if (getInternalStartLevel() > framework.startLevelManager.getStartLevel()) {
			if ((options & LAZY_TRIGGER) == 0 && (options & START_TRANSIENT) != 0) {
				// throw exception if this is a transient start
				String msg = NLS.bind(Msg.BUNDLE_TRANSIENT_START_ERROR, this);
				// Use a StatusException to indicate to the lazy starter that this should result in a warning
				throw new BundleException(msg, BundleException.INVALID_OPERATION, new BundleStatusException(msg, StatusException.CODE_WARNING, this));
			}
			return;
		}

		if (state == INSTALLED) {
			try {
				if (!framework.packageAdmin.resolveBundles(new Bundle[] {this}, true))
					throw getResolutionFailureException();
			} catch (IllegalStateException e) {
				// Can happen if the resolver detects a nested resolve process
				throw new BundleException("Unexpected resolution exception.", BundleException.RESOLVE_ERROR, e); //$NON-NLS-1$
			} catch (ResolverHookException e) {
				throw new BundleException("Unexpected resolution exception.", BundleException.REJECTED_BY_HOOK, e.getCause()); //$NON-NLS-1$
			}

		}

		if ((options & START_ACTIVATION_POLICY) != 0 && (bundledata.getStatus() & Constants.BUNDLE_LAZY_START) != 0) {
			// the bundle must use the activation policy here.
			if ((state & RESOLVED) != 0) {
				// now we must publish the LAZY_ACTIVATION event and return
				state = STARTING;
				// release the state change lock before sending lazy activation event (bug 258659)
				completeStateChange();
				framework.publishBundleEvent(BundleEvent.LAZY_ACTIVATION, this);
			}
			return;
		}

		if (Debug.DEBUG_GENERAL) {
			Debug.println("Bundle: Active sl = " + framework.startLevelManager.getStartLevel() + "; Bundle " + getBundleId() + " sl = " + getInternalStartLevel()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		if ((options & LAZY_TRIGGER) != 0) {
			if ((state & RESOLVED) != 0) {
				// Should publish the lazy activation event here before the starting event
				// This can happen if another bundle in the same start-level causes a class load from the lazy start bundle.
				state = STARTING;
				// release the state change lock before sending lazy activation event (bug 258659)
				completeStateChange();
				framework.publishBundleEvent(BundleEvent.LAZY_ACTIVATION, this);
				beginStateChange();
				if (state != STARTING) {
					// while firing the LAZY_ACTIVATION event some one else caused the bundle to transition
					// out of STARTING.  This could have happened because some listener called start on the bundle
					// or another class load could have caused the start trigger to get fired again.
					return;
				}
			}
		}
		state = STARTING;
		framework.publishBundleEvent(BundleEvent.STARTING, this);
		context = getContext();
		//STARTUP TIMING Start here		
		long start = 0;

		BundleWatcher bundleStats = framework.adaptor.getBundleWatcher();
		if (bundleStats != null)
			bundleStats.watchBundle(this, BundleWatcher.START_ACTIVATION);
		if (Debug.DEBUG_BUNDLE_TIME) {
			start = System.currentTimeMillis();
			System.out.println("Starting " + getSymbolicName()); //$NON-NLS-1$
		}

		try {
			context.start();
			startHook();
			if (framework.active) {
				state = ACTIVE;

				if (Debug.DEBUG_GENERAL) {
					Debug.println("->started " + this); //$NON-NLS-1$
				}
				// release the state change lock before sending lazy activation event (bug 258659)
				completeStateChange();
				framework.publishBundleEvent(BundleEvent.STARTED, this);
			}

		} catch (BundleException e) {
			// we must fire the stopping event
			state = STOPPING;
			framework.publishBundleEvent(BundleEvent.STOPPING, this);

			stopHook();
			context.close();
			context = null;

			state = RESOLVED;
			// if this is a lazy start bundle that fails to start then
			// we must fire the stopped event
			framework.publishBundleEvent(BundleEvent.STOPPED, this);
			throw e;
		} finally {
			if (bundleStats != null)
				bundleStats.watchBundle(this, BundleWatcher.END_ACTIVATION);
			if (Debug.DEBUG_BUNDLE_TIME)
				System.out.println("End starting " + getSymbolicName() + " " + (System.currentTimeMillis() - start)); //$NON-NLS-1$ //$NON-NLS-2$

		}

		if (state == UNINSTALLED) {
			context.close();
			context = null;
			throw new BundleException(NLS.bind(Msg.BUNDLE_UNINSTALLED_EXCEPTION, getBundleData().getLocation()), BundleException.STATECHANGE_ERROR);
		}
	}

	/**
	 * @throws BundleException  
	 */
	protected void startHook() throws BundleException {
		// do nothing by default
	}

	protected boolean readyToResume() {
		// Return false if the bundle is not at the correct start-level
		if (getInternalStartLevel() > framework.startLevelManager.getStartLevel())
			return false;
		int status = bundledata.getStatus();
		// Return false if the bundle is not persistently marked for start
		if ((status & Constants.BUNDLE_STARTED) == 0)
			return false;
		if ((status & Constants.BUNDLE_ACTIVATION_POLICY) == 0 || (status & Constants.BUNDLE_LAZY_START) == 0 || isLazyTriggerSet())
			return true;
		if (!isResolved()) {
			if (framework.getAdaptor().getState().isResolved() || !framework.packageAdmin.resolveBundles(new Bundle[] {this}))
				// should never transition from UNRESOLVED -> STARTING
				return false;
		}
		// now we can publish the LAZY_ACTIVATION event
		state = STARTING;
		// release the state change lock before sending lazy activation event (bug 258659)
		completeStateChange();
		framework.publishBundleEvent(BundleEvent.LAZY_ACTIVATION, this);
		return false;
	}

	private synchronized boolean isLazyTriggerSet() {
		if (proxy == null)
			return false;
		BundleLoader loader = proxy.getBasicBundleLoader();
		return loader != null ? loader.isLazyTriggerSet() : false;
	}

	/**
	 * Create a BundleContext for this bundle.
	 *
	 * @return BundleContext for this bundle.
	 */
	protected BundleContextImpl createContext() {
		return (new BundleContextImpl(this));
	}

	/**
	 * Return the current context for this bundle.
	 *
	 * @return BundleContext for this bundle.
	 */
	protected synchronized BundleContextImpl getContext() {
		if (context == null) {
			// only create the context if we are starting, active or stopping
			// this is so that SCR can get the context for lazy-start bundles
			if ((state & (STARTING | ACTIVE | STOPPING)) != 0)
				context = createContext();
		}
		return (context);
	}

	/**
	 * Internal worker to stop a bundle.
	 *
	 * @param options the stop options
	 */
	protected void stopWorker(int options) throws BundleException {
		if ((options & STOP_TRANSIENT) == 0) {
			setStatus(Constants.BUNDLE_STARTED, false);
			setStatus(Constants.BUNDLE_ACTIVATION_POLICY, false);
			if (Debug.MONITOR_ACTIVATION)
				new Exception("A persistent start has been called on bundle: " + getBundleData()).printStackTrace(); //$NON-NLS-1$
		}
		if (framework.active) {
			if ((state & (STOPPING | RESOLVED | INSTALLED)) != 0) {
				return;
			}

			BundleWatcher bundleStats = framework.adaptor.getBundleWatcher();
			if (bundleStats != null)
				bundleStats.watchBundle(this, BundleWatcher.START_DEACTIVATION);

			state = STOPPING;
			framework.publishBundleEvent(BundleEvent.STOPPING, this);
			try {
				// context may be null if a lazy-start bundle is STARTING
				if (context != null)
					context.stop();
			} finally {
				stopHook();
				if (context != null) {
					context.close();
					context = null;
				}

				checkValid();

				state = RESOLVED;

				if (Debug.DEBUG_GENERAL) {
					Debug.println("->stopped " + this); //$NON-NLS-1$
				}

				framework.publishBundleEvent(BundleEvent.STOPPED, this);
				if (bundleStats != null)
					bundleStats.watchBundle(this, BundleWatcher.END_DEACTIVATION);

			}
		}
	}

	/**
	 * @throws BundleException  
	 */
	protected void stopHook() throws BundleException {
		// do nothing
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
	public ServiceReference<?>[] getRegisteredServices() {
		checkValid();

		if (context == null) {
			return null;
		}

		return context.getFramework().getServiceRegistry().getRegisteredServices(context);
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
	public ServiceReference<?>[] getServicesInUse() {
		checkValid();

		if (context == null) {
			return null;
		}

		return context.getFramework().getServiceRegistry().getServicesInUse(context);
	}

	public BundleFragment[] getFragments() {
		synchronized (framework.bundles) {
			if (fragments == null)
				return null;
			BundleFragment[] result = new BundleFragment[fragments.length];
			System.arraycopy(fragments, 0, result, 0, result.length);
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
		// do not force the creation of the bundle loader here
		BundleLoader loader = getLoaderProxy().getBasicBundleLoader();
		// If the Host ClassLoader exists then we must attach
		// the fragment to the ClassLoader.
		if (loader != null)
			loader.attachFragment(fragment);

		if (fragments == null) {
			fragments = new BundleFragment[] {fragment};
		} else {
			boolean inserted = false;
			// We must keep our fragments ordered by bundle ID; or 
			// install order.
			BundleFragment[] newFragments = new BundleFragment[fragments.length + 1];
			for (int i = 0; i < fragments.length; i++) {
				if (fragment == fragments[i])
					return; // this fragment is already attached
				// need to flush the other attached fragment manifest caches in case the attaching fragment provides translations (bug 339211)
				fragments[i].manifestLocalization = null;
				if (!inserted && fragment.getBundleId() < fragments[i].getBundleId()) {
					// if the loader has already been created
					// then we cannot attach a fragment into the middle
					// of the fragment chain.
					if (loader != null) {
						throw new BundleException(NLS.bind(Msg.BUNDLE_LOADER_ATTACHMENT_ERROR, fragments[i].getSymbolicName(), getSymbolicName()), BundleException.INVALID_OPERATION);
					}
					newFragments[i] = fragment;
					inserted = true;
				}
				newFragments[inserted ? i + 1 : i] = fragments[i];
			}
			if (!inserted)
				newFragments[newFragments.length - 1] = fragment;
			fragments = newFragments;
		}
		// need to flush the manifest cache in case the attaching fragment provides translations
		manifestLocalization = null;
	}

	protected BundleLoader getBundleLoader() {
		BundleLoaderProxy curProxy = getLoaderProxy();
		return curProxy == null ? null : curProxy.getBundleLoader();
	}

	public synchronized BundleLoaderProxy getLoaderProxy() {
		if (proxy != null)
			return proxy;
		BundleDescription bundleDescription = getBundleDescription();
		if (bundleDescription == null)
			return null;
		proxy = new BundleLoaderProxy(this, bundleDescription);
		// Note that BundleLoaderProxy is a BundleReference
		// this is necessary to ensure the resolver can continue
		// to provide BundleRevision objects to resolver hooks.
		bundleDescription.setUserObject(proxy);
		return proxy;
	}

	/**
	 * Gets the class loader for the host bundle.  This may end up 
	 * creating the bundle class loader if it was not already created.
	 * A null value may be returned if the bundle is not resolved.
	 * @return the bundle class loader or null if the bundle is not resolved.
	 */
	public ClassLoader getClassLoader() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new RuntimePermission("getClassLoader")); //$NON-NLS-1$
		BundleLoaderProxy curProxy = getLoaderProxy();
		BundleLoader loader = curProxy == null ? null : curProxy.getBundleLoader();
		BundleClassLoader bcl = loader == null ? null : loader.createClassLoader();
		return (bcl instanceof ClassLoader) ? (ClassLoader) bcl : null;
	}

}
