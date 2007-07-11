/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.profile.Profile;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

/**
 * PackageAdmin service for the OSGi specification.
 *
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the framework and eagerly update or uninstall bundles.
 *
 * If present, there will only be a single instance of this service
 * registered in the framework.
 *
 * <p> The term <i>exported package</i> (and the corresponding interface
 * {@link ExportedPackage}) refers to a package that has actually been
 * exported (as opposed to one that is available for export).
 *
 * <p> Note that the information about exported packages returned by this
 * service is valid only until the next time {@link #refreshPackages(org.osgi.framework.Bundle[])} is
 * called.
 * If an ExportedPackage becomes stale, (that is, the package it references
 * has been updated or removed as a result of calling
 * PackageAdmin.refreshPackages()),
 * its getName() and getSpecificationVersion() continue to return their
 * old values, isRemovalPending() returns true, and getExportingBundle()
 * and getImportingBundles() return null.
 */
public class PackageAdminImpl implements PackageAdmin {
	/** framework object */
	protected Framework framework;

	/* 
	 * We need to make sure that the GetBundleAction class loads early to prevent a ClassCircularityError when checking permissions.
	 * See bug 161561
	 */
	static {
		Class c;
		c = GetBundleAction.class;
		c.getName(); // to prevent compiler warnings
	}

	static class GetBundleAction implements PrivilegedAction {
		private Class clazz;
		private PackageAdminImpl impl;

		public GetBundleAction(PackageAdminImpl impl, Class clazz) {
			this.impl = impl;
			this.clazz = clazz;
		}

		public Object run() {
			return impl.getBundlePriv(clazz);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param framework Framework object.
	 */
	protected PackageAdminImpl(Framework framework) {
		this.framework = framework;
	}

	public ExportedPackage[] getExportedPackages(Bundle bundle) {
		ArrayList allExports = new ArrayList();
		synchronized (framework.bundles) {
			ExportPackageDescription[] allDescriptions = framework.adaptor.getState().getExportedPackages();
			for (int i = 0; i < allDescriptions.length; i++) {
				if (!allDescriptions[i].isRoot())
					continue;
				ExportedPackageImpl exportedPackage = createExportedPackage(allDescriptions[i]);
				if (exportedPackage == null)
					continue;
				if (bundle == null || exportedPackage.supplier.getBundle() == bundle)
					allExports.add(exportedPackage);
			}
		}
		return (ExportedPackage[]) (allExports.size() == 0 ? null : allExports.toArray(new ExportedPackage[allExports.size()]));
	}

	private ExportedPackageImpl createExportedPackage(ExportPackageDescription description) {
		BundleDescription exporter = description.getExporter();
		if (exporter == null || exporter.getHost() != null)
			return null;
		BundleLoaderProxy proxy = (BundleLoaderProxy) exporter.getUserObject();
		if (proxy == null) {
			BundleHost bundle = (BundleHost) framework.getBundle(exporter.getBundleId());
			if (bundle == null)
				return null;
			proxy = bundle.getLoaderProxy();
		}
		return new ExportedPackageImpl(description, proxy);
	}

	public ExportedPackage getExportedPackage(String name) {
		ExportedPackage[] allExports = getExportedPackages((Bundle) null);
		if (allExports == null)
			return null;
		ExportedPackage result = null;
		for (int i = 0; i < allExports.length; i++) {
			if (name.equals(allExports[i].getName())) {
				if (result == null) {
					result = allExports[i];
				} else {
					// TODO not efficient but this is not called very often
					Version curVersion = Version.parseVersion(result.getSpecificationVersion());
					Version newVersion = Version.parseVersion(allExports[i].getSpecificationVersion());
					if (newVersion.compareTo(curVersion) >= 0)
						result = allExports[i];
				}
			}
		}
		return result;
	}

	public ExportedPackage[] getExportedPackages(String name) {
		ExportedPackage[] allExports = getExportedPackages((Bundle) null);
		if (allExports == null)
			return null;
		ArrayList result = new ArrayList(1); // rare to have more than one
		for (int i = 0; i < allExports.length; i++)
			if (name.equals(allExports[i].getName()))
				result.add(allExports[i]);
		return (ExportedPackage[]) (result.size() == 0 ? null : result.toArray(new ExportedPackage[result.size()]));
	}

	public void refreshPackages(Bundle[] input) {
		framework.checkAdminPermission(framework.systemBundle, AdminPermission.RESOLVE);

		AbstractBundle[] copy = null;
		if (input != null) {
			synchronized (input) {
				copy = new AbstractBundle[input.length];
				System.arraycopy(input, 0, copy, 0, input.length);
			}
		}

		final AbstractBundle[] bundles = copy;
		Thread refresh = framework.secureAction.createThread(new Runnable() {
			public void run() {
				doResolveBundles(bundles, true);
				if ("true".equals(FrameworkProperties.getProperty("osgi.forcedRestart"))) //$NON-NLS-1$ //$NON-NLS-2$
					framework.shutdown();
			}
		}, "Refresh Packages"); //$NON-NLS-1$

		refresh.start();
	}

	public boolean resolveBundles(Bundle[] bundles) {
		framework.checkAdminPermission(framework.systemBundle, AdminPermission.RESOLVE);
		doResolveBundles(null, false);
		if (bundles == null)
			bundles = framework.getAllBundles();
		for (int i = 0; i < bundles.length; i++)
			if (!((AbstractBundle) bundles[i]).isResolved())
				return false;

		return true;
	}

	synchronized void doResolveBundles(AbstractBundle[] bundles, boolean refreshPackages) {
		try {
			if (Profile.PROFILE && Profile.STARTUP)
				Profile.logEnter("resolve bundles"); //$NON-NLS-1$
			framework.publishBundleEvent(Framework.BATCHEVENT_BEGIN, framework.systemBundle);
			AbstractBundle[] refreshedBundles = null;
			BundleDescription[] descriptions = null;
			synchronized (framework.bundles) {
				int numBundles = bundles == null ? 0 : bundles.length;
				if (!refreshPackages)
					// in this case we must make descriptions non-null so we do
					// not force the removal pendings to be processed when resolving
					// the state.
					descriptions = new BundleDescription[0];
				else if (numBundles > 0) {
					ArrayList results = new ArrayList(numBundles);
					for (int i = 0; i < numBundles; i++) {
						BundleDescription description = bundles[i].getBundleDescription();
						if (description != null && description.getBundleId() != 0 && !results.contains(description))
							results.add(description);
						// add in any bundles that have the same symbolic name see bug (169593)
						AbstractBundle[] sameNames = framework.bundles.getBundles(bundles[i].getSymbolicName());
						if (sameNames != null && sameNames.length > 1) {
							for (int j = 0; j < sameNames.length; j++)
								if (sameNames[j] != bundles[i]) {
									BundleDescription sameName = sameNames[j].getBundleDescription();
									if (sameName != null && sameName.getBundleId() != 0 && !results.contains(sameName))
										results.add(sameName);
								}
						}
					}
					descriptions = (BundleDescription[]) (results.size() == 0 ? null : results.toArray(new BundleDescription[results.size()]));
				}
			}
			State systemState = framework.adaptor.getState();
			BundleDelta[] delta = systemState.resolve(descriptions).getChanges();
			refreshedBundles = processDelta(delta, refreshPackages);
			// if we end up refreshing the system bundle or one of its fragments the framework will be shutdown and 
			// should be re-started. This call should return without doing further work.
			if (!framework.isActive())
				return;
			if (refreshPackages) {
				AbstractBundle[] allBundles = framework.getAllBundles();
				for (int i = 0; i < allBundles.length; i++)
					allBundles[i].unresolvePermissions();
				// increment the system state timestamp if we are refreshing packages.
				// this is needed incase we suspended a bundle from processing the delta (bug 167483)
				if (delta.length > 0)
					systemState.setTimeStamp(systemState.getTimeStamp() == Long.MAX_VALUE ? 0 : systemState.getTimeStamp() + 1);
			}
			// always resume bundles incase we have lazy-start bundles
			resumeBundles(refreshedBundles, refreshPackages);
		} catch (Throwable t) {
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("PackageAdminImpl.doResolveBundles: Error occured :"); //$NON-NLS-1$
				Debug.printStackTrace(t);
			}
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			if (t instanceof Error)
				throw (Error) t;
		} finally {
			if (Profile.PROFILE && Profile.STARTUP)
				Profile.logExit("resolve bundles"); //$NON-NLS-1$
			if (framework.isActive()) {
				framework.publishBundleEvent(Framework.BATCHEVENT_END, framework.systemBundle);
				if (refreshPackages)
					framework.publishFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, framework.systemBundle, null);
			}
		}
	}

	private void resumeBundles(AbstractBundle[] bundles, boolean refreshPackages) {
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			Debug.println("PackageAdminImpl: restart the bundles"); //$NON-NLS-1$
		}
		if (bundles == null)
			return;
		for (int i = 0; i < bundles.length; i++) {
			if (!bundles[i].isResolved() || (!refreshPackages && ((bundles[i].getBundleData().getStatus() & Constants.BUNDLE_LAZY_START) == 0 || bundles[i].testStateChanging(Thread.currentThread()))))
				// skip bundles that are not resolved or
				// if we are doing resolveBundles then skip non-lazy start bundles and bundles currently changing state by this thread
				continue;
			framework.resumeBundle(bundles[i]);
		}
	}

	private void suspendBundle(AbstractBundle bundle) {
		// attempt to suspend the bundle or obtain the state change lock
		// Note that this may fail but we cannot quit the
		// refreshPackages operation because of it. (bug 84169)
		if (bundle.isActive() && !bundle.isFragment()) {
			framework.suspendBundle(bundle, true);
		} else {
			if (bundle.getStateChanging() != Thread.currentThread())
				try {
					bundle.beginStateChange();
				} catch (BundleException e) {
					framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
				}
		}

		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			if (bundle.stateChanging == null) {
				Debug.println("Bundle state change lock is clear! " + bundle); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}
	}

	private void applyRemovalPending(BundleDelta bundleDelta) throws BundleException {
		if ((bundleDelta.getType() & BundleDelta.REMOVAL_COMPLETE) != 0) {
			BundleDescription bundle = bundleDelta.getBundle();
			if (bundle.getDependents() != null && bundle.getDependents().length > 0) {
				/* Reaching here is an internal error */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("Bundles still depend on removed bundle! " + bundle); //$NON-NLS-1$
					Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
				}
				throw new BundleException(Msg.OSGI_INTERNAL_ERROR);
			}
			BundleLoaderProxy proxy = (BundleLoaderProxy) bundle.getUserObject();
			if (proxy != null) {
				BundleHost.closeBundleLoader(proxy);
				try {
					proxy.getBundleHost().getBundleData().close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private AbstractBundle setResolved(BundleDescription bundleDescription) {
		if (!bundleDescription.isResolved())
			return null;
		AbstractBundle bundle = framework.getBundle(bundleDescription.getBundleId());
		if (bundle == null) {
			BundleException be = new BundleException(NLS.bind(Msg.BUNDLE_NOT_IN_FRAMEWORK, bundleDescription));
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, be);
			return null;
		}
		boolean resolve = true;
		if (bundle.isFragment()) {
			BundleDescription[] hosts = bundleDescription.getHost().getHosts();
			for (int i = 0; i < hosts.length; i++) {
				BundleHost host = (BundleHost) framework.getBundle(hosts[i].getBundleId());
				resolve = ((BundleFragment) bundle).addHost(host.getLoaderProxy());
			}
		}
		if (resolve)
			bundle.resolve();
		return bundle;
	}

	private AbstractBundle[] applyDeltas(BundleDelta[] bundleDeltas) throws BundleException {
		ArrayList results = new ArrayList(bundleDeltas.length);
		for (int i = 0; i < bundleDeltas.length; i++) {
			int type = bundleDeltas[i].getType();
			if ((type & (BundleDelta.REMOVAL_PENDING | BundleDelta.REMOVAL_COMPLETE)) != 0)
				applyRemovalPending(bundleDeltas[i]);
			if ((type & BundleDelta.RESOLVED) != 0) {
				AbstractBundle bundle = setResolved(bundleDeltas[i].getBundle());
				if (bundle != null && bundle.isResolved())
					results.add(bundle);
			}
		}
		return (AbstractBundle[]) (results.size() == 0 ? null : results.toArray(new AbstractBundle[results.size()]));
	}

	private AbstractBundle[] processDelta(BundleDelta[] bundleDeltas, boolean refreshPackages) {
		ArrayList bundlesList = new ArrayList(bundleDeltas.length);
		// get all the bundles that are going to be refreshed
		for (int i = 0; i < bundleDeltas.length; i++) {
			if ((bundleDeltas[i].getType() & BundleDelta.REMOVAL_COMPLETE) != 0 && (bundleDeltas[i].getType() & BundleDelta.REMOVED) == 0)
				// this means the bundle was previously pending removal; do not add to list because it was already removed from before.
				continue;
			AbstractBundle changedBundle = framework.getBundle(bundleDeltas[i].getBundle().getBundleId());
			if (changedBundle != null && !bundlesList.contains(changedBundle))
				bundlesList.add(changedBundle);
		}
		AbstractBundle[] refresh = (AbstractBundle[]) bundlesList.toArray(new AbstractBundle[bundlesList.size()]);
		// first sort by id/start-level order
		Util.sort(refresh);
		// then sort by dependency order
		StartLevelManager.sortByDependency(refresh);
		boolean[] previouslyResolved = new boolean[refresh.length];
		AbstractBundle[] resolved = null;
		try {
			try {
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: Suspend each bundle and acquire its state change lock"); //$NON-NLS-1$
				}
				// find which bundles were previously resolved and handle extension bundles
				boolean restart = false;
				for (int i = refresh.length - 1; i >= 0; i--) {
					previouslyResolved[i] = refresh[i].isResolved();
					if (refresh[i] == framework.systemBundle)
						restart = true;
					else if (((refresh[i].bundledata.getType() & BundleData.TYPE_FRAMEWORK_EXTENSION) != 0) && previouslyResolved[i])
						restart = true;
					else if ((refresh[i].bundledata.getType() & BundleData.TYPE_BOOTCLASSPATH_EXTENSION) != 0)
						restart = true;
				}
				if (restart) {
					FrameworkProperties.setProperty("osgi.forcedRestart", "true"); //$NON-NLS-1$ //$NON-NLS-2$
					// do not shutdown the framework while holding the PackageAdmin lock (bug 194149)
					return null;
				}
				// now suspend each bundle and grab its state change lock.
				if (refreshPackages)
					for (int i = refresh.length - 1; i >= 0; i--)
						suspendBundle(refresh[i]);
				/*
				 * Refresh the bundles which will unexport the packages.
				 * This will move RESOLVED bundles to the INSTALLED state.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: refresh the bundles"); //$NON-NLS-1$
				}

				synchronized (framework.bundles) {
					for (int i = 0; i < refresh.length; i++)
						refresh[i].refresh();
				}
				// send out unresolved events outside synch block (defect #80610)
				for (int i = 0; i < refresh.length; i++) {
					// send out unresolved events
					if (previouslyResolved[i])
						framework.publishBundleEvent(BundleEvent.UNRESOLVED, refresh[i]);
				}

				/*
				 * apply Deltas.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: applying deltas to bundles"); //$NON-NLS-1$
				}
				synchronized (framework.bundles) {
					resolved = applyDeltas(bundleDeltas);
				}

			} finally {
				/*
				 * Release the state change locks.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: release the state change locks"); //$NON-NLS-1$
				}
				if (refreshPackages)
					for (int i = 0; i < refresh.length; i++) {
						AbstractBundle changedBundle = refresh[i];
						changedBundle.completeStateChange();
					}
			}
			/*
			 * Take this opportunity to clean up the adaptor storage.
			 */
			if (refreshPackages) {
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN)
					Debug.println("refreshPackages: clean up adaptor storage"); //$NON-NLS-1$
				try {
					framework.adaptor.compactStorage();
				} catch (IOException e) {
					if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
						Debug.println("refreshPackages exception: " + e.getMessage()); //$NON-NLS-1$
						Debug.printStackTrace(e);
					}
					framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, new BundleException(Msg.BUNDLE_REFRESH_FAILURE, e));
				}
			}
		} catch (BundleException e) {
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("refreshPackages exception: " + e.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(e.getNestedException() == null ? e : e.getNestedException());
			}
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, new BundleException(Msg.BUNDLE_REFRESH_FAILURE, e));
		}

		// send out any resolved.  This must be done after the state change locks have been release.
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN)
			Debug.println("refreshPackages: send out RESOLVED events"); //$NON-NLS-1$
		if (resolved != null)
			for (int i = 0; i < resolved.length; i++)
				framework.publishBundleEvent(BundleEvent.RESOLVED, resolved[i]);

		return refresh;
	}

	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		AbstractBundle[] bundles;
		if (symbolicName == null)
			bundles = framework.getAllBundles();
		else
			bundles = framework.getBundleBySymbolicName(symbolicName);
		if (bundles == null || bundles.length == 0)
			return null;

		ArrayList result = new ArrayList(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].isFragment() || !bundles[i].isResolved() || bundles[i].getSymbolicName() == null)
				continue;
			result.add(((BundleHost) bundles[i]).getLoaderProxy());
		}
		return result.size() == 0 ? null : (RequiredBundle[]) result.toArray(new RequiredBundle[result.size()]);
	}

	public Bundle[] getBundles(String symbolicName, String versionRange) {
		if (symbolicName == null) {
			throw new IllegalArgumentException();
		}
		AbstractBundle bundles[] = framework.getBundleBySymbolicName(symbolicName);
		if (bundles == null)
			return null;

		if (versionRange == null) {
			AbstractBundle[] result = new AbstractBundle[bundles.length];
			System.arraycopy(bundles, 0, result, 0, result.length);
			return result;
		}

		// This code depends on the array of bundles being in descending
		// version order.
		ArrayList result = new ArrayList(bundles.length);
		VersionRange range = new VersionRange(versionRange);
		for (int i = 0; i < bundles.length; i++) {
			if (range.isIncluded(bundles[i].getVersion())) {
				result.add(bundles[i]);
			}
		}

		if (result.size() == 0)
			return null;
		return (AbstractBundle[]) result.toArray(new AbstractBundle[result.size()]);
	}

	public Bundle[] getFragments(Bundle bundle) {
		return ((AbstractBundle) bundle).getFragments();
	}

	public Bundle[] getHosts(Bundle bundle) {
		BundleLoaderProxy[] hosts = ((AbstractBundle) bundle).getHosts();
		if (hosts == null)
			return null;
		Bundle[] result = new Bundle[hosts.length];
		for (int i = 0; i < hosts.length; i++)
			result[i] = hosts[i].getBundleHost();
		return result;
	}

	Bundle getBundlePriv(Class clazz) {
		ClassLoader cl = clazz.getClassLoader();
		if (cl instanceof BundleClassLoader)
			return ((BundleLoader) ((BundleClassLoader) cl).getDelegate()).bundle;
		if (cl == getClass().getClassLoader())
			return framework.systemBundle;
		return null;
	}

	public Bundle getBundle(final Class clazz) {
		if (System.getSecurityManager() == null)
			return getBundlePriv(clazz);
		return (Bundle) AccessController.doPrivileged(new GetBundleAction(this, clazz));
	}

	public int getBundleType(Bundle bundle) {
		return ((AbstractBundle) bundle).isFragment() ? PackageAdmin.BUNDLE_TYPE_FRAGMENT : 0;
	}

	protected void cleanup() { //This is only called when the framework is shutting down
	}

	protected void setResolvedBundles(SystemBundle systemBundle) {
		checkSystemBundle(systemBundle);
		// Now set the actual state of the bundles from the persisted state.
		State state = framework.adaptor.getState();
		BundleDescription[] descriptions = state.getBundles();
		for (int i = 0; i < descriptions.length; i++) {
			if (descriptions[i].getBundleId() == 0)
				setFrameworkVersion(descriptions[i]);
			else
				setResolved(descriptions[i]);
		}
	}

	private void checkSystemBundle(SystemBundle systemBundle) {
		try {
			// first check that the system bundle has not changed since last saved state.
			State state = framework.adaptor.getState();
			BundleDescription oldSystemBundle = state.getBundle(0);
			boolean different = false;
			if (oldSystemBundle == null || !systemBundle.getBundleData().getVersion().equals(oldSystemBundle.getVersion()))
				different = true;
			if (!different && FrameworkProperties.getProperty("osgi.dev") == null) //$NON-NLS-1$
				return; // return quick if not in dev mode; system bundle version changes with each build
			BundleDescription newSystemBundle = state.getFactory().createBundleDescription(state, systemBundle.getHeaders(""), systemBundle.getLocation(), 0); //$NON-NLS-1$
			if (newSystemBundle == null)
				throw new BundleException(Msg.OSGI_SYSTEMBUNDLE_DESCRIPTION_ERROR);
			if (!different) {
				// need to check to make sure the system bundle description is up to date in the state.
				ExportPackageDescription[] oldPackages = oldSystemBundle.getExportPackages();
				ExportPackageDescription[] newPackages = newSystemBundle.getExportPackages();
				if (oldPackages.length >= newPackages.length) {
					for (int i = 0; i < newPackages.length && !different; i++) {
						if (oldPackages[i].getName().equals(newPackages[i].getName())) {
							Object oldVersion = oldPackages[i].getVersion();
							Object newVersion = newPackages[i].getVersion();
							different = oldVersion == null ? newVersion != null : !oldVersion.equals(newVersion);
						} else {
							different = true;
						}
					}
				} else {
					different = true;
				}
			}
			if (different) {
				state.removeBundle(0);
				state.addBundle(newSystemBundle);
				// force resolution so packages are properly linked
				state.resolve(false);
			}
		} catch (BundleException e) /* fatal error */{
			e.printStackTrace();
			throw new RuntimeException(NLS.bind(Msg.OSGI_SYSTEMBUNDLE_CREATE_EXCEPTION, e.getMessage()));
		}
	}

	private void setFrameworkVersion(BundleDescription systemBundle) {
		ExportPackageDescription[] packages = systemBundle.getExportPackages();
		for (int i = 0; i < packages.length; i++)
			if (packages[i].getName().equals(Constants.OSGI_FRAMEWORK_PACKAGE)) {
				FrameworkProperties.setProperty(Constants.FRAMEWORK_VERSION, packages[i].getVersion().toString());
				break;
			}
		FrameworkProperties.setProperty(Constants.OSGI_IMPL_VERSION_KEY, systemBundle.getVersion().toString());
	}
}
