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
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.DebugOptions;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.service.resolver.*;
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

	/** BundleLoaders that are pending removal. Value is BundleLoader */
	protected Vector removalPending;

	protected KeyedHashSet exportedPackages;
	protected KeyedHashSet exportedBundles;

	private long cumulativeTime;

	private static boolean checkServiceClassSource = true;
	private static boolean restrictServiceClasses = false;

	/**
	 * Constructor.
	 *
	 * @param framework Framework object.
	 */
	protected PackageAdminImpl(Framework framework) {
		this.framework = framework;
	}

	protected void initialize() {
		checkServiceClassSource = Boolean.valueOf(System.getProperty(Constants.OSGI_CHECKSERVICECLASSSOURCE,"true")).booleanValue(); //$NON-NLS-1$
		restrictServiceClasses = Boolean.valueOf(System.getProperty(Constants.OSGI_RESTRICTSERVICECLASSES,"false")).booleanValue(); //$NON-NLS-1$
		removalPending = new Vector(10, 10);

		State state = framework.adaptor.getState();
		if (!state.isResolved()) {
			state.resolve(false);
		}
		exportedPackages = new KeyedHashSet(false);
		exportedBundles = new KeyedHashSet(false);
	}

	private KeyedHashSet getExportedPackages(KeyedHashSet packageSet) {
		State state = framework.adaptor.getState();
		PackageSpecification[] packageSpecs = state.getExportedPackages();

		for (int i = 0; i < packageSpecs.length; i++) {
			BundleDescription bundleSpec = packageSpecs[i].getSupplier();
			if (bundleSpec == null)
				continue;
			AbstractBundle bundle = framework.getBundle(bundleSpec.getBundleId());
			if (bundle == null) {
				BundleException be = new BundleException(Msg.formatter.getString("BUNDLE_NOT_IN_FRAMEWORK", bundleSpec)); //$NON-NLS-1$
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, be);
				continue;
			}
			// check export permissions before getting the host;
			// we want to check the permissions of the fragment
			if (!bundle.checkExportPackagePermission(packageSpecs[i].getName()))
				continue;
			// if we have a host then get the host bundle
			HostSpecification hostSpec = bundleSpec.getHost();
			if (hostSpec != null) {
				bundleSpec = hostSpec.getSupplier();
				if (bundleSpec == null)
					continue;
				bundle = framework.getBundle(bundleSpec.getBundleId());
				if (bundle == null) {
					BundleException be = new BundleException(Msg.formatter.getString("BUNDLE_NOT_IN_FRAMEWORK", bundleSpec)); //$NON-NLS-1$
					framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, be);
					continue;
				}
			}

			if (bundle.isResolved() && bundle instanceof BundleHost) {
				ExportedPackageImpl packagesource = new ExportedPackageImpl(packageSpecs[i], ((BundleHost) bundle).getLoaderProxy());
				packageSet.add(packagesource);
			}
		}
		return packageSet;
	}

	private KeyedHashSet getExportedBundles(KeyedHashSet bundleSet) {
		State state = framework.adaptor.getState();
		BundleDescription[] bundleDescripts = state.getResolvedBundles();
		for (int i = 0; i < bundleDescripts.length; i++) {
			BundleDescription bundledes = bundleDescripts[i];
			AbstractBundle bundle = framework.getBundle(bundledes.getBundleId());
			if (bundle != null && bundle.isResolved() && bundle.getSymbolicName() != null && bundle instanceof BundleHost && bundle.checkProvideBundlePermission(bundle.getSymbolicName())) {
				BundleLoaderProxy loaderProxy = ((BundleHost) bundle).getLoaderProxy();
				bundleSet.add(loaderProxy);
			}
		}
		return bundleSet;
	}

	protected void cleanup() { //This is only called when the framework is shutting down
		removalPending = null;
		exportedPackages = null;
		exportedBundles = null;
	}

	protected void addRemovalPending(BundleLoaderProxy loaderProxy) {
		removalPending.addElement(loaderProxy);
	}

	protected void deleteRemovalPending(BundleLoaderProxy loaderProxy) throws BundleException {

		boolean exporting = loaderProxy.inUse();
		if (exporting) {
			/* Reaching here is an internal error */
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("BundleLoader.unexportPackager returned true! " + loaderProxy); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
			throw new BundleException(Msg.formatter.getString("OSGI_INTERNAL_ERROR")); //$NON-NLS-1$
		}
		unexportResources(loaderProxy);
		BundleLoader loader = loaderProxy.getBundleLoader();
		loader.clear();
		loader.close();
		removalPending.remove(loaderProxy);
	}

	/**
	 * Gets the packages exported by the specified bundle.
	 *
	 * @param bundle The bundle whose exported packages are to be returned,
	 *               or <tt>null</tt> if all the packages currently
	 *               exported in the framework are to be returned.  If the
	 *               specified bundle is the system bundle (that is, the
	 *               bundle with id 0), this method returns all the packages
	 *               on the system classpath whose name does not start with
	 *               "java.".  In an environment where the exhaustive list
	 *               of packages on the system classpath is not known in
	 *               advance, this method will return all currently known
	 *               packages on the system classpath, that is, all packages
	 *               on the system classpath that contains one or more classes
	 *               that have been loaded.
	 *
	 * @return The array of packages exported by the specified bundle,
	 * or <tt>null</tt> if the specified bundle has not exported any packages.
	 */
	public org.osgi.service.packageadmin.ExportedPackage[] getExportedPackages(org.osgi.framework.Bundle bundle) {
		// need to make sure the dependacies are marked before this call.
		synchronized (framework.bundles) {
			framework.bundles.markDependancies();

			KeyedElement[] elements = exportedPackages.elements();
			if (bundle != null) {
				Vector result = new Vector();
				for (int i = 0; i < elements.length; i++) {
					ExportedPackageImpl pkgElement = (ExportedPackageImpl) elements[i];
					if (pkgElement.supplier.getBundleHost() == bundle) {
						result.add(pkgElement);
					}
				}
				if (result.size() == 0) {
					return null;
				}
				ExportedPackageImpl[] pkgElements = new ExportedPackageImpl[result.size()];
				return (ExportedPackage[]) result.toArray(pkgElements);
			} else {
				if (elements.length == 0) {
					return null;
				}
				ExportedPackageImpl[] pkgElements = new ExportedPackageImpl[elements.length];
				System.arraycopy(elements, 0, pkgElements, 0, pkgElements.length);
				return pkgElements;
			}
		}
	}

	/**
	 * Gets the ExportedPackage with the specified package name.  All exported
	 * packages
	 * will be checked for the specified name.  In an environment where the
	 * exhaustive list of packages on the system classpath is not known in
	 * advance, this method attempts to see if the named package is on the
	 * system classpath.
	 * This
	 * means that this method may discover an ExportedPackage that was
	 * not present in the list returned by <tt>getExportedPackages()</tt>.
	 *
	 * @param packageName The name of the exported package to be returned.
	 *
	 * @return The exported package with the specified name, or <tt>null</tt>
	 *         if no expored package with that name exists.
	 */
	public org.osgi.service.packageadmin.ExportedPackage getExportedPackage(String packageName) {
		// need to make sure the dependacies are marked before this call.
		synchronized (framework.bundles) {
			framework.bundles.markDependancies();
			return (ExportedPackageImpl) exportedPackages.getByKey(packageName);
		}
	}

	/**
	 * Forces the update (replacement) or removal of packages exported by
	 * the specified bundles.
	 *
	 * <p> If no bundles are specified, this method will update or remove any
	 * packages exported by any bundles that were previously updated or
	 * uninstalled. The technique by which this is accomplished
	 * may vary among different framework implementations. One permissible
	 * implementation is to stop and restart the Framework.
	 *
	 * <p> This method returns to the caller immediately and then performs the
	 * following steps in its own thread:
	 *
	 * <ol>
	 * <p>
	 * <li> Compute a graph of bundles starting with the specified ones. If no
	 * bundles are specified, compute a graph of bundles starting with
	 * previously updated or uninstalled ones.
	 * Any bundle that imports a package that is currently exported
	 * by a bundle in the graph is added to the graph. The graph is fully
	 * constructed when there is no bundle outside the graph that imports a
	 * package from a bundle in the graph. The graph may contain
	 * <tt>UNINSTALLED</tt> bundles that are currently still
	 * exporting packages.
	 *
	 * <p>
	 * <li> Each bundle in the graph will be stopped as described in the
	 * <tt>Bundle.stop</tt> method.
	 *
	 * <p>
	 * <li> Each bundle in the graph that is in the
	 * <tt>RESOLVED</tt> state is moved
	 * to the <tt>INSTALLED</tt> state.
	 * The effect of this step is that bundles in the graph are no longer
	 * <tt>RESOLVED</tt>.
	 *
	 * <p>
	 * <li> Each bundle in the graph that is in the UNINSTALLED state is
	 * removed from the graph and is now completely removed from the framework.
	 *
	 * <p>
	 * <li> Each bundle in the graph that was in the
	 * <tt>ACTIVE</tt> state prior to Step 2 is started as
	 * described in the <tt>Bundle.start</tt> method, causing all
	 * bundles required for the restart to be resolved.
	 * It is possible that, as a
	 * result of the previous steps, packages that were
	 * previously exported no longer are. Therefore, some bundles
	 * may be unresolvable until another bundle
	 * offering a compatible package for export has been installed in the
	 * framework.
	 * </ol>
	 *
	 * <p> For any exceptions that are thrown during any of these steps, a
	 * <tt>FrameworkEvent</tt> of type <tt>ERROR</tt> is
	 * broadcast, containing the exception.
	 *
	 * @param input the bundles whose exported packages are to be updated or
	 * removed,
	 * or <tt>null</tt> for all previously updated or uninstalled bundles.
	 *
	 * @exception SecurityException if the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
	public void refreshPackages(org.osgi.framework.Bundle[] input) {
		framework.checkAdminPermission();

		AbstractBundle[] copy = null;
		if (input != null) {
			synchronized (input) {
				int size = input.length;

				copy = new AbstractBundle[size];

				System.arraycopy(input, 0, copy, 0, size);
			}
		}

		final AbstractBundle[] bundles = copy;
		Thread refresh = SecureAction.createThread(new Runnable() {
			public void run() {
				refreshPackages(bundles);
			}
		}, "Refresh Packages"); //$NON-NLS-1$

		refresh.start();
	}

	/**
	 * Worker routine called on a seperate thread to perform the actual work.
	 *
	 * @param refresh the list of bundles to refresh 
	 */
	protected void refreshPackages(AbstractBundle[] refresh) {
		try {
			Vector graph = null;
			synchronized (framework.bundles) {
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: Initialize graph"); //$NON-NLS-1$
				}
				// make sure the dependencies are marked first
				framework.bundles.markDependancies();

				graph = computeAffectedBundles(refresh);

				// get the state.
				State state = framework.adaptor.getState();
				// resolve the state.
				state.resolve(false);

				// process the delta.  This will set the state of all
				// the bundles in the graph.
				processDelta(graph);
			}
			/*
			 * Resume the suspended bundles outside of the synchronization block.
			 * This will cause the bundles to be resolved using the most up-to-date
			 * generations of the bundles.
			 */
			resumeBundles(graph);

		} finally {
			framework.publishFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, framework.systemBundle, null);
		}

	}

	private void resumeBundles(Vector graph) {

		AbstractBundle[] refresh = new AbstractBundle[graph.size()];
		boolean[] previouslyResolved = new boolean[graph.size()];
		graph.copyInto(refresh);
		Util.sort(refresh, 0, graph.size());
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			Debug.println("refreshPackages: restart the bundles"); //$NON-NLS-1$
		}
		for (int i = 0; i < refresh.length; i++) {
			AbstractBundle bundle = (AbstractBundle) refresh[i];
			if (bundle.isResolved())
				framework.resumeBundle(bundle);
		}
	}

	private void processDelta(Vector graph) {
		AbstractBundle[] refresh = new AbstractBundle[graph.size()];
		boolean[] previouslyResolved = new boolean[graph.size()];
		graph.copyInto(refresh);
		Util.sort(refresh, 0, graph.size());

		Vector notify = new Vector();
		try {
			try {
				/*
				 * Suspend each bundle and grab its state change lock.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: Suspend each bundle and acquire its state change lock"); //$NON-NLS-1$
				}
				for (int i = refresh.length - 1; i >= 0; i--) {
					AbstractBundle changedBundle = refresh[i];
					previouslyResolved[i] = changedBundle.isResolved();
					if (changedBundle.isActive() && !changedBundle.isFragment()) {
						boolean suspended = framework.suspendBundle(changedBundle, true);
						if (!suspended) {
							throw new BundleException(Msg.formatter.getString("BUNDLE_STATE_CHANGE_EXCEPTION")); //$NON-NLS-1$
						}
					} else {
						changedBundle.beginStateChange();
					}

					if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
						if (changedBundle.stateChanging == null) {
							Debug.println("Bundle state change lock is clear! " + changedBundle); //$NON-NLS-1$
							Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
						}
					}
				}
				/*
				 * Refresh the bundles which will unexport the packages.
				 * This will move RESOLVED bundles to the INSTALLED state.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: refresh the bundles"); //$NON-NLS-1$
				}
				/*
				 * Unimport detached BundleLoaders for bundles in the graph.
				 */
				for (int i = removalPending.size() - 1; i >= 0; i--) {
					BundleLoaderProxy loaderProxy = (BundleLoaderProxy) removalPending.elementAt(i);

					if (graph.contains(loaderProxy.getBundleHost())) {
						framework.bundles.unMarkDependancies(loaderProxy);
					}
				}

				for (int i = 0; i < refresh.length; i++) {
					AbstractBundle changedBundle = refresh[i];
					changedBundle.refresh();
					// send out unresolved events
					if (previouslyResolved[i])
						framework.publishBundleEvent(BundleEvent.UNRESOLVED, changedBundle);
				}

				/*
				 * Cleanup detached BundleLoaders for bundles in the graph.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: unexport the removal pending packages"); //$NON-NLS-1$
				}
				for (int i = removalPending.size() - 1; i >= 0; i--) {
					BundleLoaderProxy loaderProxy = (BundleLoaderProxy) removalPending.elementAt(i);
					AbstractBundle removedBundle = loaderProxy.getBundleHost();

					if (graph.contains(removedBundle)) {
						deleteRemovalPending(loaderProxy);
					}
				}

				// set the resolved bundles state
				List allBundles = framework.bundles.getBundles();
				int size = allBundles.size();
				for (int i = 0; i < size; i++) {
					AbstractBundle bundle = (AbstractBundle) allBundles.get(i);
					if (bundle.isResolved())
						continue;
					BundleDescription bundleDes = bundle.getBundleDescription();
					if (bundleDes != null) {
						if (bundleDes.isResolved()) {
							if (bundle.isFragment()) {
								BundleHost host = (BundleHost) framework.getBundle(bundleDes.getHost().getSupplier().getBundleId());
								if (((BundleFragment) bundle).setHost(host)) {
									bundle.resolve(bundleDes.isSingleton());
								}
							} else {
								bundle.resolve(bundleDes.isSingleton());
							}
							if (bundle.isResolved()) {
								notify.addElement(bundle);
							}
						}
					}
				}

				// update the exported package and bundle lists.
				exportedPackages = getExportedPackages(exportedPackages);
				exportedBundles = getExportedBundles(exportedBundles);
			} finally {
				/*
				 * Release the state change locks.
				 */
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages: release the state change locks"); //$NON-NLS-1$
				}
				for (int i = 0; i < refresh.length; i++) {
					AbstractBundle changedBundle = refresh[i];
					changedBundle.completeStateChange();
				}
			}
			/*
			 * Take this opportunity to clean up the adaptor storage.
			 */
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("refreshPackages: clean up adaptor storage"); //$NON-NLS-1$
			}
			try {
				framework.adaptor.compactStorage();
			} catch (IOException e) {
				if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
					Debug.println("refreshPackages exception: " + e.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(e);
				}
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, new BundleException(Msg.formatter.getString("BUNDLE_REFRESH_FAILURE"), e)); //$NON-NLS-1$
			}
		} catch (BundleException e) {
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("refreshPackages exception: " + e.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(e.getNestedException());
			}
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, new BundleException(Msg.formatter.getString("BUNDLE_REFRESH_FAILURE"), e)); //$NON-NLS-1$
		}

		// send out any resolved/unresolved events
		for (int i = 0; i < notify.size(); i++) {
			AbstractBundle changedBundle = (AbstractBundle) notify.elementAt(i);
			framework.publishBundleEvent(changedBundle.isResolved() ? BundleEvent.RESOLVED : BundleEvent.UNRESOLVED, changedBundle);
		}

	}

	private void unresolvePermissions(Vector bundles, Hashtable packages) {
		/*
		 * All bundles must be notified of the unexported packages so that
		 * they may unresolve permissions if necessary.
		 * This done after resolve bundles so that unresolved permissions
		 * can be immediately resolved.
		 */
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			Debug.println("refreshPackages: unresolve permissions"); //$NON-NLS-1$
		}
		int size = bundles.size();
		for (int i = 0; i < size; i++) {
			AbstractBundle bundle = (AbstractBundle) bundles.elementAt(i);
			bundle.unresolvePermissions(packages);
		}
	}

	private Vector computeAffectedBundles(AbstractBundle[] refresh) {
		Vector graph = new Vector();

		if (refresh == null) {
			int size = removalPending.size();
			for (int i = 0; i < size; i++) {
				BundleLoaderProxy loaderProxy = (BundleLoaderProxy) removalPending.elementAt(i);
				AbstractBundle bundle = loaderProxy.getBundleHost();
				if (!graph.contains(bundle)) {
					if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
						Debug.println(" refresh: " + bundle); //$NON-NLS-1$
					}
					graph.addElement(bundle);

					// add in any dependents of the removal pending loader.
					AbstractBundle[] dependents = loaderProxy.getDependentBundles();
					for (int j = 0; j < dependents.length; j++) {
						if (!graph.contains(dependents[j])) {
							graph.addElement(dependents[j]);
						}
					}
				}
			}
		} else {
			for (int i = 0; i < refresh.length; i++) {
				AbstractBundle bundle = refresh[i];
				if (bundle == framework.systemBundle) {
					continue;
				}
				if (bundle.isFragment()) {
					// if it is a fragment then put the host in the graph
					BundleHost host = (BundleHost) bundle.getHost();
					if (host != null) {
						if (!graph.contains(host)) {
							graph.addElement(host);
						}
					}
				}

				if (!graph.contains(bundle)) {
					if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
						Debug.println(" refresh: " + bundle); //$NON-NLS-1$
					}
					graph.addElement(bundle);
				}
			}
		}

		/*
		 * If there is nothing to do, then return.
		 */
		if (graph.size() == 0) {
			if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
				Debug.println("refreshPackages: Empty graph"); //$NON-NLS-1$
			}

			return graph;
		}

		/*
		 * Complete graph.
		 */
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			Debug.println("refreshPackages: Complete graph"); //$NON-NLS-1$
		}

		boolean changed;
		do {
			changed = false;
			int size = graph.size();
			for (int i = size - 1; i >= 0; i--) {
				AbstractBundle bundle = (AbstractBundle) graph.elementAt(i);
				if (!bundle.isFragment()) {
					BundleLoaderProxy loaderProxy = ((BundleHost) bundle).getLoaderProxy();
					if (loaderProxy != null) {
						// add any dependents
						AbstractBundle[] dependents = loaderProxy.getDependentBundles();
						for (int j = 0; j < dependents.length; j++) {
							if (!graph.contains(dependents[j])) {
								graph.addElement(dependents[j]);
								changed = true;
							}
						}
					}
					// add in any fragments
					org.osgi.framework.Bundle[] frags = bundle.getFragments();
					if (frags != null) {
						for (int j = 0; j < frags.length; j++) {
							if (!graph.contains(frags[j])) {
								graph.addElement(frags[j]);
								changed = true;
							}
						}
					}
				} else {
					// add in the host.
					AbstractBundle host = (AbstractBundle) bundle.getHost();
					if (host != null) {
						if (!graph.contains(host)) {
							graph.addElement(host);
							changed = true;
						}
					}
				}
				// add in any singleton bundles if needed
				AbstractBundle[] sameNames = framework.bundles.getBundles(bundle.getSymbolicName());
				if (sameNames != null && sameNames.length > 1) {
					for (int j = 0; j < sameNames.length; j++)
						if (sameNames[j] != bundle && sameNames[j].isSingleton() && !graph.contains(sameNames[j])) {
							graph.addElement(sameNames[j]);
							changed = true;
						}
				}
			}

			// look for the bundles in removalPending list
			// we always add removalPending dependants here even if
			// the removalpending bundle was not in the original list
			for (int i = removalPending.size() - 1; i >= 0; i--) {
				BundleLoaderProxy removedLoaderProxy = (BundleLoaderProxy) removalPending.elementAt(i);
				AbstractBundle removedBundle = removedLoaderProxy.getBundleHost();
				if (!graph.contains(removedBundle)) {
					graph.addElement(removedBundle);
					changed = true;
				}
				AbstractBundle[] dependents = removedLoaderProxy.getDependentBundles();
				for (int k = 0; k < dependents.length; k++) {
					if (!graph.contains(dependents[k])) {
						graph.addElement(dependents[k]);
						changed = true;
					}
				}
			}
		} while (changed);

		return graph;
	}

	/**
	 * Sets all the bundles in the state that are resolved to the resolved
	 * state.  This should only be called when the framework is launching.
	 *
	 */
	protected void setResolvedBundles() {
		State state = framework.adaptor.getState();
		BundleDescription[] descriptions = state.getBundles();
		for (int i = 0; i < descriptions.length; i++) {
			long bundleId = descriptions[i].getBundleId();
			AbstractBundle bundle = framework.getBundle(bundleId);
			if (bundle == null) {
				BundleException be = new BundleException(Msg.formatter.getString("BUNDLE_NOT_IN_FRAMEWORK", descriptions[i])); //$NON-NLS-1$
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, be);
			}
			if (bundle != framework.systemBundle) {
				if (descriptions[i].isResolved()) {
					if (bundle.isFragment()) {
						BundleHost host = (BundleHost) framework.getBundle(descriptions[i].getHost().getSupplier().getBundleId());
						if (((BundleFragment) bundle).setHost(host)) {
							bundle.resolve(descriptions[i].isSingleton());
						}
					} else {
						bundle.resolve(descriptions[i].isSingleton());
					}
				}
			}
		}
		exportedPackages = getExportedPackages(exportedPackages);
		exportedBundles = getExportedBundles(exportedBundles);
	}

	/**
	 * A permissible implementation of this method is to attempt to 
	 * resolve all unresolved bundles installed in the framework.
	 * That is what this method does.
	 * @param bundles the set of bundles to attempt to resolve.
	 */
	public boolean resolveBundles(org.osgi.framework.Bundle[] bundles) {
		resolveBundles();
		if (bundles == null)
			synchronized (framework.bundles) {
				List bundleList = framework.bundles.getBundles();
				bundles = (Bundle[]) bundleList.toArray(new AbstractBundle[bundleList.size()]);
			}
		for (int i = 0; i < bundles.length; i++)
			if (!((AbstractBundle) bundles[i]).isResolved())
				return false;

		return true;
	}

	/**
	 * Attempt to resolve all unresolved bundles. When this method returns
	 * all bundles are resolved that can be resolved. A resolved bundle
	 * has exported and imported packages. An unresolved bundle neither
	 * exports nor imports packages.
	 *
	 */
	protected void resolveBundles() {
		long start = 0;
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN_TIMING)
			start = System.currentTimeMillis();
		/*
		 * Resolve the bundles. This will make there exported packages available.
		 */
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN) {
			Debug.println("refreshBundles: resolve bundles"); //$NON-NLS-1$
		}

		Vector notify = new Vector();
		synchronized (framework.bundles) {
			boolean resolveNeeded = false;
			List allBundles = framework.bundles.getBundles();
			int size = allBundles.size();

			// first check to see if there is anything to resolve
			for (int i = 0; i < size; i++) {
				if (!((AbstractBundle) allBundles.get(i)).isResolved())
					resolveNeeded = true;
			}
			if (!resolveNeeded)
				return;

			// get the state and resolve it.
			framework.adaptor.getState().resolve(false);
			for (int i = 0; i < size; i++) {
				AbstractBundle bundle = (AbstractBundle) allBundles.get(i);
				if (bundle.isResolved() || bundle == framework.systemBundle)
					continue;

				BundleDescription changedBundleDes = bundle.getBundleDescription();
				if (changedBundleDes == null) {
					framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, new BundleException(Msg.formatter.getString("BUNDLE_NOT_IN_STATE", bundle.getLocation()))); //$NON-NLS-1$
				}

				if (changedBundleDes.isResolved()) {
					if (bundle.isFragment()) {
						BundleHost host = (BundleHost) framework.getBundle(changedBundleDes.getHost().getSupplier().getBundleId());
						if (((BundleFragment) bundle).setHost(host)) {
							bundle.resolve(changedBundleDes.isSingleton());
						}
					} else {
						bundle.resolve(changedBundleDes.isSingleton());
					}
					if (bundle.isResolved()) {
						notify.add(bundle);
					}
				}
			}

			// update the exported package and bundle lists.
			exportedPackages = getExportedPackages(exportedPackages);
			exportedBundles = getExportedBundles(exportedBundles);
		}
		for (int i = 0; i < notify.size(); i++) {
			AbstractBundle bundle = (AbstractBundle) notify.elementAt(i);
			if (bundle != null) {
				framework.publishBundleEvent(bundle.isResolved() ? BundleEvent.RESOLVED : BundleEvent.UNRESOLVED, bundle);
			}
		}
		if (Debug.DEBUG && Debug.DEBUG_PACKAGEADMIN_TIMING) {
			cumulativeTime = cumulativeTime + System.currentTimeMillis() - start;
			DebugOptions.getDefault().setOption(Debug.OPTION_DEBUG_PACKAGEADMIN_TIMING + "/value", Long.toString(cumulativeTime)); //$NON-NLS-1$
		}
	}

	protected void unexportResources(BundleLoaderProxy proxy) {
		KeyedElement[] bundles = exportedBundles.elements();
		for (int i = 0; i < bundles.length; i++) {
			BundleLoaderProxy loaderProxy = (BundleLoaderProxy) bundles[i];
			if (loaderProxy == proxy) {
				exportedBundles.remove(proxy);
			}
		}

		KeyedElement[] packages = exportedPackages.elements();
		for (int i = 0; i < packages.length; i++) {
			PackageSource source = (PackageSource) packages[i];
			BundleLoaderProxy sourceProxy = source.getSupplier();
			if (sourceProxy == proxy) {
				exportedPackages.remove(source);
			}
		}
		// make the proxy stale
		proxy.setStale();
	}

	protected BundleDescription[] getBundleDescriptions(Vector graph) {
		ArrayList result = new ArrayList();
		int size = graph.size();
		for (int i = 0; i < size; i++) {
			AbstractBundle bundle = (AbstractBundle) graph.elementAt(i);
			BundleDescription bundleDes = bundle.getBundleDescription();
			if (bundleDes != null) {
				result.add(bundleDes);
			}
		}
		return (BundleDescription[]) result.toArray(new BundleDescription[result.size()]);
	}

	public ProvidingBundle[] getProvidingBundles(String symbolicName) {
		if (exportedBundles == null || exportedBundles.size() == 0)
			return null;

		// need to make sure the dependacies are marked before this call.
		framework.bundles.markDependancies();

		KeyedElement[] allSymbolicBundles = exportedBundles.elements();
		if (symbolicName == null) {
			if (allSymbolicBundles.length == 0) {
				return null;
			}
			ProvidingBundle[] result = new ProvidingBundle[allSymbolicBundles.length];
			System.arraycopy(allSymbolicBundles, 0, result, 0, result.length);
			return result;
		} else {
			ArrayList result = new ArrayList();
			for (int i = 0; i < allSymbolicBundles.length; i++) {
				ProvidingBundle symBundle = (ProvidingBundle) allSymbolicBundles[i];
				if (symBundle.getSymbolicName().equals(symbolicName))
					result.add(symBundle);
			}
			return (ProvidingBundle[]) result.toArray(new ProvidingBundle[result.size()]);
		}
	}

	public org.osgi.framework.Bundle[] getBundles(String symbolicName, String versionRange) {
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
		else
			return (AbstractBundle[]) result.toArray(new AbstractBundle[result.size()]);

	}

	public org.osgi.framework.Bundle[] getFragments(org.osgi.framework.Bundle bundle) {
		return ((AbstractBundle) bundle).getFragments();
	}

	public org.osgi.framework.Bundle[] getHosts(org.osgi.framework.Bundle bundle) {
		org.osgi.framework.Bundle host = ((AbstractBundle) bundle).getHost();
		if (host == null)
			return null;
		else
			return new org.osgi.framework.Bundle[] {host};
	}

	public int getBundleType(org.osgi.framework.Bundle bundle) {
		return ((AbstractBundle) bundle).isFragment() ? PackageAdminImpl.BUNDLE_TYPE_FRAGMENT : 0;
	}

	protected Class loadServiceClass(String className, AbstractBundle bundle) {
		try {
			// first try the PARENT class space
			return framework.adaptor.getBundleClassLoaderParent().loadClass(className);
		} catch (ClassNotFoundException e) {
			// do nothing; try exported packages
		}
		// try exported packages; SERVICE class space
		String pkgname = BundleLoader.getPackageName(className);
		if (pkgname != null) {
			PackageSource exporter = (PackageSource) exportedPackages.getByKey(pkgname);
			if (exporter != null) {
				Class serviceClass = exporter.getSupplier().getBundleLoader().findLocalClass(className);
				if (serviceClass != null)
					return serviceClass;
			}
		}
		// try bundle's PRIVATE class space
		if (bundle == null)
			return null;
		BundleLoader loader = bundle.getBundleLoader();
		if (!checkServiceClassSource) {
			try {
				return loader.findClass(className);
			} catch (ClassNotFoundException e1) {
				return null;
			}
		}
		if (restrictServiceClasses) {
			// cannot have a service class from a package that a bundle 
			// provides to a NAMED class space (unless it is exported also).
			if (pkgname == null)
				pkgname = BundleLoader.DEFAULT_PACKAGE;
			if (loader.getProvidedPackage(pkgname) != null)
				return null;
		}
		Class  serviceClass = loader.findLocalClass(className);
		if (serviceClass == null || bundle.getBundleId() == 0)
			return serviceClass;
		return serviceClass.getClassLoader() == loader.createClassLoader() ? serviceClass : null;
	}
}