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

import java.util.ArrayList;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.PackageSpecification;
import org.eclipse.osgi.service.resolver.Version;

import org.osgi.service.packageadmin.NamedClassSpace;

/**
 * The BundleLoaderProxy proxies a BundleLoader object for a Bundle.  This
 * allows for a Bundle's depedencies to be linked without forcing the 
 * creating of the BundleLoader or BundleClassLoader objects.  This class
 * keeps track of the depedencies between the bundles installed in the 
 * Framework.
 */
public class BundleLoaderProxy implements KeyedElement, NamedClassSpace {
	/** The BundleLoader that this BundleLoaderProxy is managing */
	private BundleLoader loader;
	/** The Bundle that this BundleLoaderProxy is for*/
	private BundleHost bundle;
	/** The Symbolic Name of the Bundle; this must be cached incase the Bundle is updated */
	private String symbolicName;
	/** The Version of the Bundle; this must be cached incase the Bundle is updated */
	private Version version;
	/** The unique hash key for this KeyedElemetn */
	private String key;
	/** 
	 * Indicates if this BundleLoaderProxy is stale; 
	 * this is true when the bundle is updated or uninstalled.
	 */
	private boolean stale = false;
	/**
	 * The set of users that are dependant on this BundleLoaderProxy
	 */
	private KeyedHashSet users;
	/**
	 * Indicates if the dependencies of this BundleLoaderProxy have been marked
	 */
	protected boolean markedUsedDependencies = false;

	public BundleLoaderProxy(BundleHost bundle) {
		this.bundle = bundle;
		this.symbolicName = bundle.getSymbolicName();
		if (this.symbolicName == null) {
			this.symbolicName = new StringBuffer().append(bundle.getBundleId()).append("NOSYMBOLICNAME").toString(); //$NON-NLS-1$
		}
		this.version = bundle.getVersion();
		this.key = new StringBuffer(symbolicName).append("_").append(this.version.toString()).toString(); //$NON-NLS-1$
		this.users = new KeyedHashSet(false);
	}

	public BundleLoader getBundleLoader() {
		if (loader == null)
			loader = bundle.getBundleLoader();
		return loader;
	}

	public AbstractBundle getBundle() {
		return bundle;
	}

	public void setBundleLoader(BundleLoader value) {
		loader = value;
	}

	public void markUsed(BundleLoaderProxy user) {
		// only mark as used if the user is not our own bundle.
		if (user.getBundle() != bundle) {
			users.add(user);
		}
	}

	public void unMarkUsed(BundleLoaderProxy user) {
		users.removeByKey(user.getKey());
	}

	public int getKeyHashCode() {
		return key.hashCode();
	}

	public boolean compare(KeyedElement other) {
		if (!(other instanceof BundleLoaderProxy))
			return false;
		BundleLoaderProxy otherLoaderProxy = (BundleLoaderProxy) other;
		return (symbolicName.equals(otherLoaderProxy.symbolicName) && version.matchQualifier(otherLoaderProxy.version));
	}

	public Object getKey() {
		return key;
	}

	public void setStale() {
		stale = true;
	}

	public boolean isStale() {
		return stale;
	}

	public boolean inUse() {
		return (users.size() > 0);
	}

	public AbstractBundle[] getDependentBundles() {
		KeyedElement[] proxyLoaders = users.elements();
		KeyedHashSet bundles = new KeyedHashSet(proxyLoaders.length, false);
		for (int i = 0; i < proxyLoaders.length; i++) {
			BundleLoaderProxy loaderProxy = (BundleLoaderProxy) proxyLoaders[i];
			bundles.add(loaderProxy.getBundle());
		}

		KeyedElement[] elements = bundles.elements();
		AbstractBundle[] result = new AbstractBundle[elements.length];
		System.arraycopy(elements, 0, result, 0, elements.length);

		return result;
	}

	public String toString() {
		String symbolicName = bundle.getSymbolicName();
		StringBuffer sb = new StringBuffer(symbolicName == null ? bundle.getLocation() : symbolicName);
		sb.append("; ").append(Constants.BUNDLE_VERSION_ATTRIBUTE); //$NON-NLS-1$
		sb.append("=\"").append(version.toString()).append("\""); //$NON-NLS-1$//$NON-NLS-2$
		return sb.toString();
	}

	protected void markDependencies() {
		if (markedUsedDependencies || !bundle.isResolved()) { //TODO Can we get a bundleLoaderProxy is we are unresolved. If we can get one when the bundle is unresolved is it correct?
			return;
		}
		markedUsedDependencies = true;

		BundleDescription bundleDes = bundle.getBundleDescription();
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if (bundleDes == null) {
				Debug.println("Bundle.resolved called and getBundleDescription returned null: " + this); //$NON-NLS-1$
				Debug.printStackTrace(new Exception("Stack trace")); //$NON-NLS-1$
			}
		}

		PackageSpecification[] packages = bundleDes.getPackages();
		BundleSpecification[] requiredBundles = bundleDes.getRequiredBundles();
		BundleDescription[] fragDescriptions = bundleDes.getFragments();

		markUsedPackages(packages);
		markUsedBundles(requiredBundles);

		for (int i = 0; i < fragDescriptions.length; i++) {
			if (fragDescriptions[i].isResolved()) {
				markUsedPackages(fragDescriptions[i].getPackages());
				markUsedBundles(fragDescriptions[i].getRequiredBundles());
			}
		}

		// besure to create the BundleLoader;
		getBundleLoader();
	}

	private void markUsedPackages(PackageSpecification[] packages) {
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				SingleSourcePackage packagesource = (SingleSourcePackage) bundle.framework.packageAdmin.exportedPackages.getByKey(packages[i].getName());
				if (packagesource != null) {
					packagesource.getSupplier().markUsed(this);
				}
			}
		}
	}

	private void markUsedBundles(BundleSpecification[] requiredBundles) {
		if (requiredBundles != null) {
			for (int i = 0; i < requiredBundles.length; i++) {
				if (requiredBundles[i].isResolved()) {
					String bundleKey = new StringBuffer(requiredBundles[i].getName()).append("_").append(requiredBundles[i].getActualVersion().toString()).toString(); //$NON-NLS-1$

					BundleLoaderProxy loaderProxy = (BundleLoaderProxy) bundle.framework.packageAdmin.exportedBundles.getByKey(bundleKey);
					if (loaderProxy != null) {
						loaderProxy.markUsed(this);
					}
				}
			}
		}
	}

	public org.osgi.framework.Bundle getProvidingBundle() {
		if (isStale())
			return null;

		return bundle;
	}

	public org.osgi.framework.Bundle[] getRequiringBundles() {
		if (isStale())
			return null;

		KeyedElement[] requiringProxies = users.elements();
		ArrayList requiringBundles = new ArrayList();
		for (int i = 0; i < requiringProxies.length; i++) {
			BundleLoaderProxy requiringProxy = (BundleLoaderProxy) requiringProxies[i];
			BundleLoader requiringLoader = requiringProxy.getBundleLoader();
			BundleLoaderProxy[] reqBundles = requiringLoader.requiredBundles;
			if (reqBundles != null)
				for (int j = 0; j < reqBundles.length; j++)
					if (reqBundles[j] == this)
						requiringBundles.add(requiringProxy.getBundle());
		}

		return (AbstractBundle[]) requiringBundles.toArray(new AbstractBundle[requiringBundles.size()]);
	}

	public String getName() {
		return symbolicName;
	}

	public String getVersion() {
		return version.toString();
	}

	public boolean isRemovalPending() {
		return bundle.framework.packageAdmin.removalPending.contains(this);
	}
}