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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import org.eclipse.osgi.framework.debug.Debug;

/**
 * The framework needs access to some protected methods in order to
 * load local resources and classes.  The BundleClassLoader simply exposes
 * some new public methods that call protected methods for the framework.
 */
public abstract class BundleClassLoader extends ClassLoader {

	/**
	 * The delegate used to get classes and resources from.  The delegate
	 * must always be queried first before the local ClassLoader is searched for
	 * a class or resource.
	 */
	protected ClassLoaderDelegate delegate;

	/**
	 * The host ProtectionDomain to use to define classes.
	 */
	protected ProtectionDomain hostdomain;

	/**
	 * The host classpath entries for this classloader
	 */
	protected String[] hostclasspath;

	/**
	 * Indicates this class loader is closed.
	 */
	protected boolean closed = false;

	/**
	 * The default parent classloader to use when one is not specified.
	 * The behavior of the default parent classloader will be to load classes
	 * from the boot strap classloader.
	 */
	protected static ParentClassLoader defaultParentClassLoader = new ParentClassLoader();

	/**
	 * BundleClassLoader constructor.
	 * @param delegate The ClassLoaderDelegate for this bundle.
	 * @param domain The ProtectionDomain for this bundle.
	 */
	public BundleClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] classpath) {
		this(delegate, domain, classpath, null);
	}

	/**
	 * BundleClassLoader constructor.
	 * @param delegate The ClassLoaderDelegate for this bundle.
	 * @param domain The ProtectionDomain for this bundle.
	 * @param parent The parent classloader to use.
	 */
	public BundleClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] classpath, ClassLoader parent) {
		// use the defaultParentClassLoader if a parent is not specified.
		super(parent == null ? defaultParentClassLoader : parent);
		this.delegate = delegate;
		this.hostdomain = domain;
		this.hostclasspath = classpath;
	}

	/**
	 * Initializes the ClassLoader.  This is called after all currently resolved fragment
	 * bundles have been attached to the BundleClassLoader by the Framework.
	 * @throws BundleException
	 */
	public abstract void initialize();

	/**
	 * Loads a class for the bundle.  First delegate.findClass(name) is called.
	 * The delegate will query the system class loader, bundle imports, bundle
	 * local classes, bundle hosts and fragments.  The delegate will call 
	 * BundleClassLoader.findLocalClass(name) to find a class local to this 
	 * bundle.  
	 * @param name the name of the class to load.
	 * @param resolve indicates whether to resolve the loaded class or not.
	 * @return The Class object.
	 * @throws ClassNotFoundException if the class is not found.
	 */
	public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (closed)
			throw new ClassNotFoundException(name);

		if (Debug.DEBUG && Debug.DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ")");

		try {
			// First check the parent classloader for system classes.
			ClassLoader parent = getParentPrivileged();
			if (parent != null)
				try {
					return parent.loadClass(name);
				} catch (ClassNotFoundException e) {
					// Do nothing. continue to delegate.
				}

			// Just ask the delegate.  This could result in findLocalClass(name) being called.
			Class clazz = delegate.findClass(name);
			// resolve the class if asked to.
			if (resolve)
				resolveClass(clazz);
			return (clazz);
		} catch (Error e) {
			if (Debug.DEBUG && Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ") failed.");
				Debug.printStackTrace(e);
			}
			throw e;
		} catch (ClassNotFoundException e) {
			// If the class is not found do not try to look for it locally.
			// The delegate would have already done that for us.
			if (Debug.DEBUG && Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ") failed.");
				Debug.printStackTrace(e);
			}
			throw e;
		}
	}

	/**
	 * Finds a class local to this bundle.  The bundle class path is used
	 * to search for the class.  The delegate must not be used.  This method
	 * is abstract to force extending classes to implement this method instead
	 * of using the ClassLoader.findClass(String) method.
	 * @param name The classname of the class to find
	 * @return The Class object.
	 * @throws ClassNotFoundException if the class is not found.
	 */
	abstract protected Class findClass(String name) throws ClassNotFoundException;

	/**
	 * Gets a resource for the bundle.  First delegate.findResource(name) is 
	 * called. The delegate will query the system class loader, bundle imports,
	 * bundle local resources, bundle hosts and fragments.  The delegate will 
	 * call BundleClassLoader.findLocalResource(name) to find a resource local 
	 * to this bundle.  
	 * @param name The resource path to get.
	 * @return The URL of the resource or null if it does not exist.
	 */
	public URL getResource(String name) {
		if (closed) {
			return null;
		}
		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + delegate + "].getResource(" + name + ")");
		}

		try {
			URL url = null;
			// First check the parent classloader for system resources.
			ClassLoader parent = getParentPrivileged();
			if (parent != null)
				url = parent.getResource(name);
			if (url != null) {
				return (url);
			}
			url = delegate.findResource(name);
			if (url != null) {
				return (url);
			}
		} catch (ImportResourceNotFoundException e) {
		}

		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + delegate + "].getResource(" + name + ") failed.");
		}

		return (null);
	}

	/**
	 * Finds a resource local to this bundle.  Simply calls 
	 * findResourceImpl(name) to find the resource.
	 * @param name The resource path to find.
	 * @return The URL of the resource or null if it does not exist.
	 */
	abstract protected URL findResource(String name);

	/**
	 * Finds all resources with the specified name.  This method must call
	 * delegate.findResources(name) to find all the resources.
	 * @param name The resource path to find.
	 * @return An Enumeration of all resources found or null if the resource.
	 */
	protected Enumeration findResources(String name) throws IOException {
		/* Note: this class cannot be constructed with super(parent).
		 * In order to properly search the imported packages,
		 * ClassLoader.getResources cannot call parent.findResources
		 * which will happen if this class is constructed with super(parent).
		 * This is necessary because ClassLoader.getResources is final. Otherwise
		 * we could override it to better implement the desired behavior.
		 * So instead, we do not have a parent and call delegate.findResources(name). 
		 * The delegate may call findLocalResources(name) to find the resources
		 * locally if they are not found outside the bundle.
		 */
		try {
			return (delegate.findResources(name));
		} catch (Exception e) {
			if (Debug.DEBUG && Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].findResources(" + name + ") failed.");
				Debug.printStackTrace(e);
			}
			return null;
		}
	}

	/**
	 * Finds a library for this bundle.  Simply calls 
	 * delegate.findLibrary(libname) to find the library.
	 * @param libname The library to find.
	 * @return The URL of the resource or null if it does not exist.
	 */
	protected String findLibrary(String libname) {
		return delegate.findLibrary(libname);
	}

	/**
	 * Finds a local resource in the BundleClassLoader without
	 * consulting the delegate.
	 * @param resource the resource path to find.
	 * @return a URL to the resource or null if the resource does not exist.
	 */
	public URL findLocalResource(String resource) {
		return this.findResource(resource);
	}

	/**
	 * Finds all local resources in the BundleClassLoader with the specified
	 * path without consulting the delegate.
	 * @param classname
	 * @return An Enumeration of all resources found or null if the resource.
	 * does not exist.
	 */
	abstract public Enumeration findLocalResources(String resource);

	/**
	 * Finds a local class in the BundleClassLoader without
	 * consulting the delegate.
	 * @param classname the classname to find.
	 * @return The class object found.
	 * @throws ClassNotFoundException if the classname does not exist locally.
	 */
	public Class findLocalClass(String classname) throws ClassNotFoundException {
		return findClass(classname);
	}

	/**
	 * Finds a local object in the BundleClassLoader without
	 * consulting the delegate.
	 * @param object the object name to fined.
	 * @return the object found or null if the object does not exist.
	 */
	abstract public Object findLocalObject(String object);

	/**
	 * Closes this class loader.  After this method is called
	 * loadClass will always throw ClassNotFoundException,
	 * getResource, getResourceAsStream, and getResources will
	 * return null.
	 *
	 */
	public void close() {
		closed = false;
	}

	/**
	 * Empty parent classloader.  This is used by default as our parentClassLoader
	 * The BundleClassLoader constructor may assign a different parentClassLoader
	 * if desired.
	 */
	protected static class ParentClassLoader extends ClassLoader {
		protected ParentClassLoader() {
			super(null);
		}
	}

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
	abstract public void attachFragment(BundleData bundledata, ProtectionDomain domain, String[] classpath);

	protected ClassLoader getParentPrivileged(){
		if (System.getSecurityManager() == null)
			return getParent();
		
		return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return getParent();
			}
		});
	}
}
