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

package org.eclipse.osgi.baseadaptor.hooks;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.*;

/**
 * A ClassLoadingHook hooks into the <code>ClasspathManager</code> class.
 * @see ClasspathManager
 * @see HookRegistry#getClassLoadingHooks()
 * @see HookRegistry#addClassLoadingHook(ClassLoadingHook)
 * @since 3.2
 */
public interface ClassLoadingHook {
	/**
	 * Gets called by a classpath manager before defining a class.  This method allows a class loading hook 
	 * to process the bytes of a class that is about to be defined.
	 * @param name the name of the class being defined
	 * @param classbytes the bytes of the class being defined
	 * @param classpathEntry the ClasspathEntry where the class bytes have been read from.
	 * @param entry the BundleEntry source of the class bytes
	 * @param manager the class path manager used to define the requested class
	 * @return a modified array of classbytes or null if the original bytes should be used.
	 */
	byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager);

	/**
	 * Gets called by a classpath manager when looking for ClasspathEntry objects.  This method allows 
	 * a classloading hook to add additional ClasspathEntry objects
	 * @param cpEntries the list of ClasspathEntry objects currently available for the requested classpath
	 * @param cp the name of the requested classpath
	 * @param hostmanager the classpath manager the requested ClasspathEntry is for
	 * @param sourcedata the source bundle data of the requested ClasspathEntry
	 * @param sourcedomain the source domain of the requested ClasspathEntry
	 * @return true if a ClasspathEntry has been added to cpEntries
	 */
	boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain);

	/**
	 * Gets called by a base data during {@link BundleData#findLibrary(String)}.
	 * A base data will call this method for each configured class loading hook until one 
	 * class loading hook returns a non-null value.  If no class loading hook returns 
	 * a non-null value then the base data will return null.
	 * @param data the base data to find a native library for.
	 * @param libName the name of the native library.
	 * @return The absolute path name of the native library or null.
	 */
	String findLibrary(BaseData data, String libName);

	/**
	 * Gets called by the adaptor during {@link FrameworkAdaptor#getBundleClassLoaderParent()}.
	 * The adaptor will call this method for each configured class loading hook until one 
	 * class loading hook returns a non-null value.  If no class loading hook returns 
	 * a non-null value then the adaptor will perform the default behavior.
	 * @return the parent classloader to be used by all bundle classloaders or null.
	 */
	public ClassLoader getBundleClassLoaderParent();

	/**
	 * Gets called by a base data during 
	 * {@link BundleData#createClassLoader(ClassLoaderDelegate, BundleProtectionDomain, String[])}.
	 * The BaseData will call this method for each configured class loading hook until one data
	 * hook returns a non-null value.  If no class loading hook returns a non-null value then a 
	 * default implemenation of BundleClassLoader will be created.
	 * @param parent the parent classloader for the BundleClassLoader
	 * @param delegate the delegate for the bundle classloader
	 * @param domain the domian for the bundle classloader
	 * @param data the BundleData for the BundleClassLoader
	 * @param bundleclasspath the classpath for the bundle classloader
	 * @return a newly created bundle classloader
	 */
	BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath);

	/**
	 * Gets called by a classpath manager at the end of 
	 * {@link ClasspathManager#initialize()}.
	 * The classpath manager will call this method for each configured class loading hook after it 
	 * has been initialized.
	 * @param baseClassLoader the newly created bundle classloader
	 * @param data the BundleData associated with the bundle classloader
	 */
	void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data);
}
