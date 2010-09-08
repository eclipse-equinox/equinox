/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.HookRegistry;

/**
 * A ClassLoaderDelegateHook hooks into the <code>ClassLoaderDelegate</code>.
 * @see ClassLoaderDelegate
 * @see HookRegistry#getClassLoaderDelegateHooks()
 * @see HookRegistry#addClassLoaderDelegateHook(ClassLoaderDelegateHook)
 * @since 3.4
 */
public interface ClassLoaderDelegateHook {
	/**
	 * Called by a {@link ClassLoaderDelegate#findClass(String)} method before delegating to the resolved constraints and 
	 * local bundle for a class load.  If this method returns null then normal delegation is done.  If this method 
	 * returns a non-null value then the rest of the delegation process is skipped and the returned value is used.
	 * If this method throws a <code>ClassNotFoundException</code> then the calling 
	 * {@link ClassLoaderDelegate#findClass(String)} method re-throws the exception.
	 * @param name the name of the class to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the class found by this hook or null if normal delegation should continue
	 * @throws ClassNotFoundException to terminate the delegation and throw an exception
	 */
	public Class<?> preFindClass(String name, BundleClassLoader classLoader, BundleData data) throws ClassNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate#findClass(String)} method after delegating to the resolved constraints and 
	 * local bundle for a class load.  This method will only be called if no class was found
	 * from the normal delegation.
	 * @param name the name of the class to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the class found by this hook or null if normal delegation should continue
	 * @throws ClassNotFoundException to terminate the delegation and throw an exception
	 */
	public Class<?> postFindClass(String name, BundleClassLoader classLoader, BundleData data) throws ClassNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate #findResource(String)} before delegating to the resolved constraints and 
	 * local bundle for a resource load.  If this method returns null then normal delegation is done.  
	 * If this method returns a non-null value then the rest of the delegation process is skipped and the returned value is used.
	 * If this method throws an <code>FileNotFoundException</code> then the delegation is terminated.
	 * @param name the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the resource found by this hook or null if normal delegation should continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public URL preFindResource(String name, BundleClassLoader classLoader, BundleData data) throws FileNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate} after delegating to the resolved constraints and 
	 * local bundle for a resource load.  This method will only be called if no resource was found
	 * from the normal delegation.
	 * @param name the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the resource found by this hook or null if normal delegation should continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public URL postFindResource(String name, BundleClassLoader classLoader, BundleData data) throws FileNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate} before delegating to the resolved constraints and 
	 * local bundle for a resource load.  If this method returns null then normal delegation is done.  
	 * If this method returns  a non-null value then the rest of the delegation process is skipped and the returned value is used.
	 * If this method throws an <code>FileNotFoundException</code> then the delegation is terminated
	 * @param name the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the resources found by this hook or null if normal delegation should continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public Enumeration<URL> preFindResources(String name, BundleClassLoader classLoader, BundleData data) throws FileNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate} after delegating to the resolved constraints and 
	 * local bundle for a resource load.  This method will only be called if no resources were found
	 * from the normal delegation.
	 * @param name the name of the resource to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the resources found by this hook or null if normal delegation should continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public Enumeration<URL> postFindResources(String name, BundleClassLoader classLoader, BundleData data) throws FileNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate} before normal delegation.  If this method returns 
	 * a non-null value then the rest of the delegation process is skipped and the returned value
	 * is used.
	 * @param name the name of the library to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the library found by this hook or null if normal delegation should continue
	 * @throws FileNotFoundException to terminate the delegation
	 */
	public String preFindLibrary(String name, BundleClassLoader classLoader, BundleData data) throws FileNotFoundException;

	/**
	 * Called by a {@link ClassLoaderDelegate} after normal delegation.  This method will only be called
	 * if no library was found from the normal delegation.
	 * @param name the name of the library to find
	 * @param classLoader the bundle class loader
	 * @param data the bundle data
	 * @return the library found by this hook or null if normal delegation should continue
	 */
	public String postFindLibrary(String name, BundleClassLoader classLoader, BundleData data);
}
