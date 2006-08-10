/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.BundleException;

/**
 * The System Bundle's BundleLoader.  This BundleLoader is used by ImportClassLoaders
 * to load a resource that is exported by the System Bundle.
 */
public class SystemBundleLoader extends BundleLoader {
	public static final String EQUINOX_EE = "x-equinox-ee"; //$NON-NLS-1$
	ClassLoader classLoader;
	private HashSet eePackages;

	/**
	 * @param bundle The system bundle.
	 * @param proxy The BundleLoaderProxy for the system bundle
	 * @throws BundleException On any error.
	 */
	protected SystemBundleLoader(BundleHost bundle, BundleLoaderProxy proxy) throws BundleException {
		super(bundle, proxy);
		ExportPackageDescription[] exports = proxy.getBundleDescription().getSelectedExports();
		if (exports != null && exports.length > 0) {
			eePackages = new HashSet(exports.length);
			for (int i = 0; i < exports.length; i++)
				if (((Integer) exports[i].getDirective(EQUINOX_EE)).intValue() >= 0)
					eePackages.add(exports[i].getName());
		}
		this.classLoader = getClass().getClassLoader();
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class.
	 */
	public Class findClass(String name) throws ClassNotFoundException {
		return classLoader.loadClass(name);
	}

	/**
	 * This method will always return null.
	 */
	public String findLibrary(String name) {
		return null;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class. 
	 */
	Class findLocalClass(String name) {
		Class clazz = null;
		try {
			clazz = classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			// Do nothing, will return null
		}
		return clazz;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	URL findLocalResource(String name) {
		return classLoader.getResource(name);
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	Enumeration findLocalResources(String name) {
		try {
			return classLoader.getResources(name);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	public URL findResource(String name) {
		return classLoader.getResource(name);
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	public Enumeration findResources(String name) throws IOException {
		return classLoader.getResources(name);
	}

	/**
	 * Do nothing on a close.
	 */
	protected void close() {
		// Do nothing.
	}

	public boolean isEEPackage(String pkgName) {
		return eePackages.contains(pkgName);
	}
}
