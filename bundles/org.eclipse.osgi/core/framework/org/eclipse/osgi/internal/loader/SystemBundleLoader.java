/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.BundleFragment;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.BundleException;

/**
 * The System Bundle's BundleLoader.  This BundleLoader is used by ImportClassLoaders
 * to load a resource that is exported by the System Bundle.
 */
public class SystemBundleLoader extends BundleLoader {
	public static final String EQUINOX_EE = "x-equinox-ee"; //$NON-NLS-1$
	private final ClassLoader classLoader;
	private final HashSet eePackages;
	private final HashSet extPackages;
	private final ClassLoader extClassLoader;

	/**
	 * @param bundle The system bundle.
	 * @param proxy The BundleLoaderProxy for the system bundle
	 * @throws BundleException On any error.
	 */
	protected SystemBundleLoader(BundleHost bundle, BundleLoaderProxy proxy) throws BundleException {
		super(bundle, proxy);
		ExportPackageDescription[] exports = proxy.getBundleDescription().getSelectedExports();
		if (exports == null || exports.length == 0)
			eePackages = null;
		else {
			eePackages = new HashSet(exports.length);
			for (int i = 0; i < exports.length; i++)
				if (((Integer) exports[i].getDirective(EQUINOX_EE)).intValue() >= 0)
					eePackages.add(exports[i].getName());
		}
		this.classLoader = getClass().getClassLoader();
		extPackages = new HashSet(0); // not common; start with 0
		BundleFragment[] fragments = bundle.getFragments();
		if (fragments != null)
			for (int i = 0; i < fragments.length; i++)
				addExtPackages(fragments[i]);
		ClassLoader extCL = ClassLoader.getSystemClassLoader();
		if (extCL == null)
			extClassLoader = null;
		else {
			while (extCL.getParent() != null)
				extCL = extCL.getParent();
			// make sure extCL is not already on the parent chain of the system classloader
			boolean found = false;
			ClassLoader systemExtCL = this.classLoader;
			while (systemExtCL.getParent() != null && !found) {
				if (systemExtCL.getParent() == extCL)
					found = true;
				else
					systemExtCL = systemExtCL.getParent();
			}
			extClassLoader = found ? null : extCL;
		}
	}

	private void addExtPackages(BundleFragment fragment) {
		if ((fragment.getBundleData().getType() & BundleData.TYPE_EXTCLASSPATH_EXTENSION) == 0)
			return;
		ExportPackageDescription[] extExports = fragment.getBundleDescription().getExportPackages();
		for (int j = 0; j < extExports.length; j++)
			extPackages.add(extExports[j].getName());
	}

	synchronized public void attachFragment(BundleFragment fragment) throws BundleException {
		super.attachFragment(fragment);
		synchronized (extPackages) {
			addExtPackages(fragment);
		}
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class.
	 * This method never gets called because there is no BundleClassLoader for the framework.
	 */
	public Class findClass(String name) throws ClassNotFoundException {
		Class result = findLocalClass(name);
		if (result == null)
			throw new ClassNotFoundException(name);
		return result;
	}

	/**
	 * This method will always return null.
	 * This method never gets called because there is no BundleClassLoader for the framework.
	 */
	public String findLibrary(String name) {
		return null;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class. 
	 */
	Class findLocalClass(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			if (extClassLoader != null)
				synchronized (extPackages) {
					if (extPackages.size() > 0 && extPackages.contains(BundleLoader.getPackageName(name)))
						try {
							return extClassLoader.loadClass(name);
						} catch (ClassNotFoundException e2) {
							return null;
						}
				}
		}
		return null;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	URL findLocalResource(String name) {
		URL result = classLoader.getResource(name);
		if (result == null && extClassLoader != null)
			synchronized (extPackages) {
				if (extPackages.size() > 0 && extPackages.contains(BundleLoader.getResourcePackageName(name)))
					result = extClassLoader.getResource(name);
			}
		return result;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	Enumeration findLocalResources(String name) {
		Enumeration result = null;
		try {
			result = classLoader.getResources(name);
		} catch (IOException e) {
			// do nothing
		}
		if ((result == null || !result.hasMoreElements()) && extClassLoader != null)
			synchronized (extPackages) {
				if (extPackages.size() > 0 && extPackages.contains(BundleLoader.getResourcePackageName(name)))
					try {
						result = extClassLoader.getResources(name);
					} catch (IOException e) {
						// do nothing
					}
			}
		return result;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 * This method never gets called because there is no BundleClassLoader for the framework.
	 */
	public URL findResource(String name) {
		return findLocalResource(name);
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 * This method never gets called because there is no BundleClassLoader for the framework.
	 */
	public Enumeration findResources(String name) throws IOException {
		return findLocalResources(name);
	}

	/**
	 * Do nothing on a close.
	 */
	protected synchronized void close() {
		// Do nothing.
	}

	public boolean isEEPackage(String pkgName) {
		return eePackages.contains(pkgName);
	}
}
