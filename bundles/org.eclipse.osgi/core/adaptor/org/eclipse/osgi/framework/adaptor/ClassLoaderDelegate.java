/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A ClassLoaderDelegate is used by the BundleClassLoader in a similar
 * fashion that a parent ClassLoader is used.  A ClassLoaderDelegate must
 * be queried for any resource or class before it is loaded by the 
 * BundleClassLoader.
 */
public interface ClassLoaderDelegate {
	/**
	 * Finds a class for a bundle that may be outside of the actual bundle
	 * (i.e. an imported class or a fragment/host class).  The following is a
	 * list of steps that a ClassLoaderDelegate must take when trying to load
	 * a class. <p>
	 * <ul>
	 * <li>Try to load the class using the System ClassLoader.
	 * 
	 * <li>If the bundle is not a fragment then continue to the next step
	 * otherwise try to load the class from the host bundle, if the class 
	 * is not found in the host then throw a ClassNotFoundException.
	 * 
	 * <li>Try to load the class from an imported package.  If the class is
	 * not found and it belongs to a package that has been imported then throw
	 * an <code>ImportClassNotFoundException</code>.  If the class does not
	 * belong to an imported package then continue to the next step.
	 * 
	 * <li>If the bundle is not a host to any fragments then continue to the
	 * next step.  Try to load the class as an imported class from all of the
	 * fragment bundles for this host bundle. If the class is not found and 
	 * it belongs to a package that has been imported by a fragment then throw 
	 * an <code>ImportClassNotFoundException</code>.
	 * 
	 * <li>Try to load the class from the actual bundle.  This must be done
	 * by calling the findLocalClass(classname) method on the BundleClassLoader.
	 * 
	 * <li>If the bundle is a host to any fragments then try to load the class
	 * from the fragment bundles.  This must be done by calling the
	 * findLocalClass(classname) method on the fragement BundleClassLoader.
	 * 
	 * </ul>  
	 * If no class is found then a ClassNotFoundException is thrown.
	 * @param classname the class to find. 
	 * @return the Class.
	 * @throws ImportClassNotFoundException if trying to import a class from an
	 * imported package and the class is not found.
	 * @throws ClassNotFoundException if the class is not found.
	 */
	public Class findClass(String classname) throws ClassNotFoundException;

	/**
	 * Finds a resource for a bundle that may be outside of the actual bundle
	 * (i.e. an imported resource or a fragment/host resource).  The following
	 * is a list steps that a ClassLoaderDelegate must take when trying to find
	 * a resource. <p>
	 * <ul>
	 * <li>Try to load the resource using the System ClassLoader.
	 * 
	 * <li>If the bundle is not a fragment then continue to the next step
	 * otherwise try to load the resource from the host bundle, if the resource
	 * is not found in the host null is returned.
	 * 
	 * <li>Try to load the resource from an imported package.  If the resource is
	 * not found and it belongs to a package that has been imported then throw
	 * an <code>ImportResourceNotFoundException</code>.  If the resource does not
	 * belong to an imported package then continue to the next step.
	 * 
	 * <li>If the bundle is not a host to any fragments then continue to the
	 * next step.  Try to load the resource as an imported resource from all 
	 * of the fragment bundles for this host bundle. If the resource is not found 
	 * and it belongs to a package that has been imported by a fragment then 
	 * throw an <code>ImportResourceNotFoundException</code>.
	 * 
	 * <li>Try to load the resource from the actual bundle.  This must be done
	 * by calling the findLocalResource(name) method on the BundleClassLoader.
	 * 
	 * <li>If the bundle is a host to any fragments then try to load the resource
	 * from the fragment bundles.  This must be done by calling the
	 * findLocalResource(name) method on the fragement BundleClassLoader.
	 * 
	 * </ul>  
	 * If no resource is found then return null.
	 * @param resource the resource to load.
	 * @return the resource or null if resource is not found.
	 * @throws ImportResourceNotFoundException if trying to import a resource from an
	 * imported package and the resource is not found.
	 */
	public URL findResource(String resource) throws ImportResourceNotFoundException;

	/**
	 * Finds a list of resources for a bundle that may be outside of the actual 
	 * bundle (i.e. an imported resource or a fragment/host resource).  The 
	 * following is a list of steps that a ClassLoaderDelegate must take 
	 * when trying to find a resource. <p>
	 * <ul>
	 * <li>Try to find the list of resources using the System ClassLoader.
	 * 
	 * <li>If the bundle is not a fragment then continue to the next step
	 * otherwise try to find the list of resources from the host bundle, if the 
	 * resource is not found in the host then return null.
	 * 
	 * <li>Try to find the list of resources from an imported package.  If the 
	 * resource is not found and it belongs to a package that has been imported 
	 * then throw an <code>ImportResourceNotFoundException</code>.  If the 
	 * resource does not belong to an imported package then continue to the 
	 * next step.
	 * 
	 * <li>If the bundle is not a host to any fragments then continue to the
	 * next step.  Try to find the list of resources as an imported resource 
	 * from all of the fragment bundles for this host bundle. If the resource
	 * is not found and it belongs to a package that has been imported by a 
	 * fragment then throw an <code>ImportResourceNotFoundException</code>.
	 * 
	 * <li>Try to find the list of resources from the actual bundle.
	 * 
	 * <li>If the bundle is a host to any fragments then try to find the list of
	 * resources from the fragment bundles.
	 * 
	 * </ul>  
	 * If no resource is found then return null.
	 * @param resource the resource to find.
	 * @return the enumeration of resource paths found or null if the resource
	 * does not exist.
	 * @throws ImportResourceNotFoundException if trying to import a resource from an
	 * imported package and the resource is not found.
	 */
	public Enumeration findResources(String resource) throws ImportResourceNotFoundException, IOException;

	/**
	 * Returns the absolute path name of a native library.  The following is
	 * a list of steps that a ClassLoaderDelegate must take when trying to 
	 * find a library:
	 * <ul>
	 * <li>If the bundle is a fragment then try to find the library in the
	 * host bundle.
	 * <li>if the bundle is a host then try to find the library in the
	 * host bundle and then try to find the library in the fragment
	 * bundles.
	 * </ul>
	 * If no library is found return null.
	 * @param libraryname the library to find the path to.
	 * @return the path to the library or null if not found.
	 */
	public String findLibrary(String libraryname);

	public Object findObject(String object);
}
