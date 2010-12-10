/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.KeyedHashSet;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.composite.CompositeBase;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.RequiredBundle;

/*
 * The BundleLoaderProxy proxies a BundleLoader object for a Bundle.  This
 * allows for a Bundle's depedencies to be linked without forcing the 
 * creating of the BundleLoader or BundleClassLoader objects.  This class
 * keeps track of the depedencies between the bundles installed in the 
 * Framework.
 */
public class BundleLoaderProxy implements RequiredBundle, BundleReference {
	static SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	// The BundleLoader that this BundleLoaderProxy is managing
	private BundleLoader loader;
	// The Bundle that this BundleLoaderProxy is for
	final private BundleHost bundle;
	// the BundleDescription for the Bundle
	final private BundleDescription description;
	// the BundleData for the bundle revision
	final private BundleData data;
	// Indicates if this BundleLoaderProxy is stale; 
	// this is true when the bundle is updated or uninstalled.
	private boolean stale = false;
	// cached of package sources for the bundle
	final private KeyedHashSet pkgSources;

	public BundleLoaderProxy(BundleHost bundle, BundleDescription description) {
		this.bundle = bundle;
		this.description = description;
		this.pkgSources = new KeyedHashSet(false);
		this.data = bundle.getBundleData();
	}

	public BundleLoader getBundleLoader() {
		if (System.getSecurityManager() == null)
			return getBundleLoader0();
		return AccessController.doPrivileged(new PrivilegedAction<BundleLoader>() {
			public BundleLoader run() {
				return getBundleLoader0();
			}
		});
	}

	synchronized BundleLoader getBundleLoader0() {
		if (loader != null)
			return loader;
		if (bundle.isResolved()) {
			try {
				if (bundle.getBundleId() == 0) // this is the system bundle
					loader = new SystemBundleLoader(bundle, this);
				else
					loader = new BundleLoader(bundle, this);
			} catch (BundleException e) {
				bundle.getFramework().publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
				return null;
			}
		}
		return loader;
	}

	public BundleLoader getBasicBundleLoader() {
		return loader;
	}

	public AbstractBundle getBundleHost() {
		return bundle;
	}

	void setStale() {
		stale = true;
	}

	public boolean isStale() {
		return stale;
	}

	public String toString() {
		String symbolicName = bundle.getSymbolicName();
		StringBuffer sb = new StringBuffer(symbolicName == null ? bundle.getBundleData().getLocation() : symbolicName);
		sb.append("; ").append(Constants.BUNDLE_VERSION_ATTRIBUTE); //$NON-NLS-1$
		sb.append("=\"").append(description.getVersion().toString()).append("\""); //$NON-NLS-1$//$NON-NLS-2$
		return sb.toString();
	}

	public org.osgi.framework.Bundle getBundle() {
		if (isStale())
			return null;

		return bundle;
	}

	public BundleData getBundleData() {
		return data;
	}

	public Bundle[] getRequiringBundles() {
		if (isStale())
			return null;
		// This is VERY slow; but never gets called in regular execution.
		BundleDescription[] dependents = description.getDependents();
		if (dependents == null || dependents.length == 0)
			return new Bundle[0];
		List<Bundle> result = new ArrayList<Bundle>(dependents.length);
		for (int i = 0; i < dependents.length; i++)
			addRequirers(dependents[i], result);
		return result.toArray(new org.osgi.framework.Bundle[result.size()]);
	}

	void addRequirers(BundleDescription dependent, List<Bundle> result) {
		if (dependent.getHost() != null) // don't look in fragments.
			return;
		BundleLoaderProxy dependentProxy = getBundleLoader().getLoaderProxy(dependent);
		if (dependentProxy == null)
			return; // bundle must have been uninstalled
		if (result.contains(dependentProxy.bundle))
			return; // prevent endless recusion
		BundleLoader dependentLoader = dependentProxy.getBundleLoader();
		BundleLoaderProxy[] requiredBundles = dependentLoader.requiredBundles;
		int[] reexportTable = dependentLoader.reexportTable;
		if (requiredBundles == null)
			return;
		int size = reexportTable == null ? 0 : reexportTable.length;
		int reexportIndex = 0;
		for (int i = 0; i < requiredBundles.length; i++) {
			if (requiredBundles[i] == this) {
				result.add(dependentProxy.bundle);
				if (reexportIndex < size && reexportTable[reexportIndex] == i) {
					reexportIndex++;
					BundleDescription[] dependents = dependent.getDependents();
					if (dependents == null)
						return;
					for (int j = 0; j < dependents.length; j++)
						dependentProxy.addRequirers(dependents[j], result);
				}
				return;
			}
		}
		return;
	}

	public String getSymbolicName() {
		return description.getSymbolicName();
	}

	public Version getVersion() {
		return description.getVersion();
	}

	public boolean isRemovalPending() {
		return description.isRemovalPending();
	}

	public BundleDescription getBundleDescription() {
		return description;
	}

	PackageSource getPackageSource(String pkgName) {
		// getByKey is called outside of a synch block because we really do not
		// care too much of duplicates getting created.  Only the first one will
		// successfully get stored into pkgSources
		PackageSource pkgSource = (PackageSource) pkgSources.getByKey(pkgName);
		if (pkgSource == null) {
			pkgSource = new SingleSourcePackage(pkgName, this);
			synchronized (pkgSources) {
				pkgSources.add(pkgSource);
			}
		}
		return pkgSource;
	}

	public boolean inUse() {
		return (description.getDependents().length > 0) || ((bundle instanceof CompositeBase) && description.getResolvedImports().length > 0);
	}

	boolean forceSourceCreation(ExportPackageDescription export) {
		boolean strict = Constants.STRICT_MODE.equals(secureAction.getProperty(Constants.OSGI_RESOLVER_MODE));
		return (export.getDirective(Constants.INCLUDE_DIRECTIVE) != null) || (export.getDirective(Constants.EXCLUDE_DIRECTIVE) != null) || (strict && export.getDirective(Constants.FRIENDS_DIRECTIVE) != null);
	}

	// creates a PackageSource from an ExportPackageDescription.  This is called when initializing
	// a BundleLoader to ensure that the proper PackageSource gets created and used for
	// filtered and reexport packages.  The storeSource flag is used by initialize to indicate
	// that the source for special case package sources (filtered or re-exported should be stored 
	// in the cache.  if this flag is set then a normal SinglePackageSource will not be created
	// (i.e. it will be created lazily)
	public PackageSource createPackageSource(ExportPackageDescription export, boolean storeSource) {
		PackageSource pkgSource = null;

		// check to see if it is a filtered export
		String includes = (String) export.getDirective(Constants.INCLUDE_DIRECTIVE);
		String excludes = (String) export.getDirective(Constants.EXCLUDE_DIRECTIVE);
		String[] friends = (String[]) export.getDirective(Constants.FRIENDS_DIRECTIVE);
		if (friends != null) {
			boolean strict = Constants.STRICT_MODE.equals(secureAction.getProperty(Constants.OSGI_RESOLVER_MODE));
			if (!strict)
				friends = null; // do not pay attention to friends if not in strict mode
		}
		if (includes != null || excludes != null || friends != null) {
			pkgSource = new FilteredSourcePackage(export.getName(), this, includes, excludes, friends);
		}

		if (storeSource) {
			// if the package source is not null then store the source only if it is not already present;
			// getByKey is called outside of a synch block because we really do not
			// care too much of duplicates getting created.  Only the first one will
			// successfully get stored into pkgSources
			if (pkgSource != null && pkgSources.getByKey(export.getName()) == null)
				synchronized (pkgSources) {
					pkgSources.add(pkgSource);
				}
		} else {
			// we are not storing the special case sources, but pkgSource == null this means this
			// is a normal package source; get it and return it.
			if (pkgSource == null) {
				pkgSource = getPackageSource(export.getName());
				// the first export cached may not be a simple single source like we need.
				if (pkgSource.getClass() != SingleSourcePackage.class)
					return new SingleSourcePackage(export.getName(), this);
			}
		}

		return pkgSource;
	}
}
