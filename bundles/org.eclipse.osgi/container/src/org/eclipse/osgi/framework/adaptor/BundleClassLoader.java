/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleWiring;

/**
 * The BundleClassLoader interface is used by the Framework to load local 
 * classes and resources from a Bundle.  Classes that implement this
 * interface must extend java.lang.ClassLoader, either directly or by extending
 * a subclass of java.lang.ClassLoader.<p>
 * 
 * ClassLoaders that implement the <code>BundleClassLoader</code> interface
 * must use a <code>ClassLoaderDelegate</code> to delegate all class, resource
 * and native library lookups.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.1
 * @see org.eclipse.osgi.framework.adaptor.BundleData#createClassLoader(ClassLoaderDelegate, BundleProtectionDomain, String[])
 */
public interface BundleClassLoader /*extends ClassLoader*/extends BundleReference {

	/**
	 * Initializes the ClassLoader.  This is called after all currently resolved fragment
	 * bundles have been attached to the BundleClassLoader by the Framework.
	 */
	public void initialize();

	/**
	 * Finds a local resource in the BundleClassLoader without
	 * consulting the delegate.
	 * @param resource the resource path to find.
	 * @return a URL to the resource or null if the resource does not exist.
	 */
	public URL findLocalResource(String resource);

	/**
	 * Finds all local resources in the BundleClassLoader with the specified
	 * path without consulting the delegate.
	 * @param resource the resource path to find.
	 * @return An Enumeration of all resources found or null if the resource.
	 * does not exist.
	 */
	public Enumeration<URL> findLocalResources(String resource);

	/**
	 * Finds a local class in the BundleClassLoader without
	 * consulting the delegate.
	 * @param classname the classname to find.
	 * @return The class object found.
	 * @throws ClassNotFoundException if the classname does not exist locally.
	 */
	public Class<?> findLocalClass(String classname) throws ClassNotFoundException;

	/**
	 * This method will first search the parent class loader for the resource;
	 * That failing, this method will invoke 
	 * {@link ClassLoaderDelegate#findResource(String)} to find the resource.   
	 * @param name the resource path to get.
	 * @return a URL for the resource or <code>null</code> if the resource is not found.
	 */
	public URL getResource(String name);

	/**
	 * This method will first search the parent class loader for the resource;
	 * That failing, this method will invoke 
	 * {@link ClassLoaderDelegate#findResource(String)} to find the resource.   
	 * @param name the resource path to get.
	 * @return an Enumeration of URL objects for the resource or <code>null</code> if the resource is not found.
	 */
	public Enumeration<URL> getResources(String name) throws IOException;

	/**
	 * This method will first search the parent class loader for the class;
	 * That failing, this method will invoke 
	 * {@link ClassLoaderDelegate#findClass(String)} to find the resource.   
	 * @param name the class name to load.
	 * @return the Class.
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String name) throws ClassNotFoundException;

	/**
	 * Closes this class loader.  After this method is called
	 * loadClass will always throw ClassNotFoundException,
	 * getResource, getResourceAsStream, getResources and will
	 * return null.
	 *
	 */
	public void close();

	/**
	 * Attaches the BundleData for a fragment to this BundleClassLoader.
	 * The Fragment BundleData resources must be appended to the end of
	 * this BundleClassLoader's classpath.  Fragment BundleData resources 
	 * must be searched ordered by Bundle ID's.  
	 * @param bundledata The BundleData of the fragment.
	 * @param domain The ProtectionDomain of the resources of the fragment.
	 * Any classes loaded from the fragment's BundleData must belong to this
	 * ProtectionDomain.
	 * @param classpath An array of Bundle-ClassPath entries to
	 * use for loading classes and resources.  This is specified by the 
	 * Bundle-ClassPath manifest entry of the fragment.
	 */
	public void attachFragment(BundleData bundledata, ProtectionDomain domain, String[] classpath);

	/**
	 * Returns the ClassLoaderDelegate used by this BundleClassLoader
	 * @return the ClassLoaderDelegate used by this BundleClassLoader
	 */
	public ClassLoaderDelegate getDelegate();

	/**
	 * Returns the parent classloader used by this BundleClassLoader
	 * @return the parent classloader used by this BundleClassLoader
	 */
	public ClassLoader getParent();

	/**
	 * Returns resource entries for the bundle associated with this class loader.  
	 * This is used to answer a call to the 
	 * {@link BundleWiring#findEntries(String, String, int)} method.
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a list of resource URLs.  If no resources are found then
	 * the empty list is returned.
	 * @see {@link BundleWiring#findEntries(String, String, int)}
	 */
	List<URL> findEntries(String path, String filePattern, int options);

	/**
	 * Returns the names of resources visible to this bundle class loader.
	 * This is used to answer a call to the 
	 * {@link BundleWiring#listResources(String, String, int)} method.
	 * This method should simply return the result of calling
	 * {@link ClassLoaderDelegate#listResources(String, String, int)}
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a collection of resource names.  If no resources are found then
	 * the empty collection is returned.
	 * @see {@link BundleWiring#listResources(String, String, int)}
	 * @see {@link ClassLoaderDelegate#listResources(String, String, int)}
	 */
	Collection<String> listResources(String path, String filePattern, int options);

	/**
	 * Returns the names of local resources visible to this bundle class loader.
	 * Only the resources available on the local class path of this bundle 
	 * class loader are searched.
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a collection of resource names.  If no resources are found then
	 * the empty collection is returned.
	 * @see {@link ClassLoaderDelegate#listResources(String, String, int)}
	 */
	Collection<String> listLocalResources(String path, String filePattern, int options);
}
