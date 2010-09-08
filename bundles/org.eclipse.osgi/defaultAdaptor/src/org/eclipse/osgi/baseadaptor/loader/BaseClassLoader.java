/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.loader;

import java.net.URL;
import java.security.ProtectionDomain;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;

/**
 * The actual class loader object used to define classes for a classpath manager.
 * This interface provides public versions of a few methods on class loader.
 * @see ClasspathManager
 * @since 3.2
 */
public interface BaseClassLoader extends BundleClassLoader {
	/**
	 * Returns the domain for the host bundle of this class loader
	 * @return the domain for the host bundle of this class loader
	 */
	ProtectionDomain getDomain();

	/**
	 * Creates a classpath entry with the given bundle file and domain
	 * @param bundlefile the source bundle file for a classpath entry
	 * @param cpDomain the source domain for a classpath entry
	 * @return a classpath entry with the given bundle file and domain
	 */
	ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain);

	/**
	 * Defines a Class.
	 * @param name the name of the class to define
	 * @param classbytes the bytes of the class to define
	 * @param classpathEntry the classpath entry used to load the class bytes
	 * @param entry the bundle entry used to load the class bytes
	 * @return a defined Class
	 */
	Class<?> defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry);

	/**
	 * A public version of the ClassLoader.findLoadedClass(java.lang.String) method.
	 * @param classname the class name to find.
	 * @return a loaded class
	 */
	Class<?> publicFindLoaded(String classname);

	/**
	 * A public version of the ClassLoader#getPackage(java.lang.String) method.
	 * @param pkgname the package name to get.
	 * @return the package or null if it does not exist
	 */
	Object publicGetPackage(String pkgname);

	/**
	 * A public version of the ClassLoader#definePackage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.net.URL) method.
	 * @return a defined Package
	 */
	Object publicDefinePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase);

	/**
	 * Returns the ClasspathManager for this BaseClassLoader
	 * @return the ClasspathManager
	 */
	ClasspathManager getClasspathManager();
}
