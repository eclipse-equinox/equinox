/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.hooks;

import java.net.URL;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;

/**
 * A ClassLoadingStatsHook hooks into the <code>ClasspathManager</code> class.  This class allows 
 * a hook to record statistics about classloading.
 * @see ClasspathManager
 * @see HookRegistry#getClassLoadingStatsHooks()
 * @see HookRegistry#addClassLoadingStatsHook(ClassLoadingStatsHook)
 * @since 3.2
 */
public interface ClassLoadingStatsHook {
	/**
	 * Gets called by a classpath manager during {@link ClasspathManager#findLocalClass(String)} before 
	 * searching the local classloader for a class.  A classpath manager will call this method for 
	 * each configured class loading stat hook.
	 * @param name the name of the requested class
	 * @param manager the classpath manager used to find and load the requested class
	 * @throws ClassNotFoundException to prevent the requested class from loading
	 */
	void preFindLocalClass(String name, ClasspathManager manager) throws ClassNotFoundException;

	/**
	 * Gets called by a classpath manager during {@link ClasspathManager#findLocalClass(String)} after
	 * searching the local classloader for a class. A classpath manager will call this method for 
	 * each configured class loading stat hook.
	 * @param name the name of the requested class
	 * @param clazz the loaded class or null if not found
	 * @param manager the classpath manager used to find and load the requested class
	 */
	void postFindLocalClass(String name, Class<?> clazz, ClasspathManager manager) throws ClassNotFoundException;

	/**
	 * Gets called by a classpath manager during {@link ClasspathManager#findLocalResource(String)} before
	 * searching the local classloader for a resource. A classpath manager will call this method for 
	 * each configured class loading stat hook.
	 * @param name the name of the requested resource
	 * @param manager the classpath manager used to find the requested resource
	 */
	void preFindLocalResource(String name, ClasspathManager manager);

	/**
	 * Gets called by a classpath manager during {@link ClasspathManager#findLocalResource(String)} after
	 * searching the local classloader for a resource. A classpath manager will call this method for 
	 * each configured class loading stat hook.
	 * @param name the name of the requested resource
	 * @param resource the URL to the requested resource or null if not found
	 * @param manager the classpath manager used to find the requested resource
	 */
	void postFindLocalResource(String name, URL resource, ClasspathManager manager);

	/**
	 * Gets called by a classpath manager after an attempt is made to define a class.  This method allows 
	 * a class loading stat hook to record data about a class definition. 
	 * @param name the name of the class that got defined
	 * @param clazz the class object that got defined or null if an error occurred while defining a class
	 * @param classbytes the class bytes used to define the class
	 * @param classpathEntry the ClasspathEntry where the class bytes got read from
	 * @param entry the BundleEntyr source of the class bytes
	 * @param manager the classpath manager used to define the class
	 */
	void recordClassDefine(String name, Class<?> clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager);

}
