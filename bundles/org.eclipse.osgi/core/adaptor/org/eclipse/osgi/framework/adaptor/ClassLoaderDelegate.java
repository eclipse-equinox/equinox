/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Enumeration;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A ClassLoaderDelegate is used by the BundleClassLoader in a similar
 * fashion that a parent ClassLoader is used.  A ClassLoaderDelegate must
 * be queried for any resource or class before it is loaded by the 
 * BundleClassLoader.  The Framework implements the ClassLoaderDelegate
 * and supplies it to the BundleClassLoader.  FrameworkAdaptor implementations
 * are not responsible for suppling an implementation for ClassLoaderDelegate.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.1
 */
public interface ClassLoaderDelegate {
	/**
	 * Finds a class for a bundle that may be outside of the actual bundle
	 * (i.e. a class from an imported package or required bundle).<p>
	 * 
	 * If the class does not belong to an imported package or is not 
	 * found in a required bundle then the ClassloaderDelegate will call 
	 * BundleClassLoader.findLocalClass(). <p>
	 *   
	 * If no class is found then a ClassNotFoundException is thrown.
	 * @param classname the class to find. 
	 * @return the Class.
	 * @throws ClassNotFoundException if the class is not found.
	 */
	public Class<?> findClass(String classname) throws ClassNotFoundException;

	/**
	 * Finds a resource for a bundle that may be outside of the actual bundle
	 * (i.e. a resource from an imported package or required bundle).<p>
	 * 
	 * If the resource does not belong to an imported package or is not 
	 * found in a required bundle then the ClassloaderDelegate will call 
	 * BundleClassLoader.findLocalResource(). <p>
	 * 
	 * If no resource is found then return null.
	 * @param resource the resource to load.
	 * @return the resource or null if resource is not found.
	 */
	public URL findResource(String resource);

	/**
	 * Finds an enumeration of resources for a bundle that may be outside of 
	 * the actual bundle (i.e. a resource from an imported package or required 
	 * bundle).<p>
	 * 
	 * If the resource does not belong to an imported package or is not 
	 * found in a required bundle then the ClassloaderDelegate will call 
	 * BundleClassLoader.findLocalResource(). <p>
	 * If no resource is found then return null.
	 * @param resource the resource to find.
	 * @return the enumeration of resources found or null if the resource
	 * does not exist.
	 */
	public Enumeration<URL> findResources(String resource) throws IOException;

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

	/**
	 * Returns true if the lazy trigger has been set for this 
	 * delegate.  The lazy trigger is set when a bundle has been 
	 * marked for lazy activation due to a successful class load.
	 * @return true if the lazy trigger has been set
	 * @since 3.6
	 */
	public boolean isLazyTriggerSet();

	/**
	 * Sets the lazy trigger for this delegate.  This will activate
	 * the bundle if the bundle has been started with the activation
	 * policy and the bundle's start level is met.
	 * @throws BundleException if an error occurred while activating the bundle
	 * @see ClassLoaderDelegate#isLazyTriggerSet()
	 * @since 3.6
	 */
	public void setLazyTrigger() throws BundleException;

	/**
	 * Returns the names of resources visible to this delegate.
	 * This is used to answer a call to the 
	 * {@link BundleWiring#listResources(String, String, int)} method.
	 * First a search is done on the packages imported by the bundle associated
	 * with this delegate.  Next a search is done on the the bundles required by 
	 * the bundle associated with this delegate.  Finally a local search of 
	 * the bundle associated with this delegate is done by calling
	 * {@link BundleClassLoader#listLocalResources(String, String, int)}.  Note
	 * that for imported packages the search stops at the source for the import.
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a collection of resource names.  If no resources are found then
	 * the empty collection is returned.
	 * @see BundleWiring#listResources(String, String, int)
	 */
	public Collection<String> listResources(String path, String filePattern, int options);
}
