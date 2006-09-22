/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.provisional.verifier.CertificateChain;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifier;

/**
 * The default implemention of <code>BaseClassLoader</code>.  This implementation extends
 * <code>ClassLoader</code>.
 * @see BaseClassLoader
 * @see ClasspathManager
 */
public class DefaultClassLoader extends ClassLoader implements BaseClassLoader {
	/**
	 * A PermissionCollection for AllPermissions; shared across all ProtectionDomains when security is disabled
	 */
	protected static final PermissionCollection ALLPERMISSIONS;
	static {
		AllPermission allPerm = new AllPermission();
		ALLPERMISSIONS = allPerm.newPermissionCollection();
		if (ALLPERMISSIONS != null)
			ALLPERMISSIONS.add(allPerm);
	}

	protected ClassLoaderDelegate delegate;
	protected ProtectionDomain domain;
	protected ClasspathManager manager;

	/**
	 * Constructs a new DefaultClassLoader.
	 * @param parent the parent classloader
	 * @param delegate the delegate for this classloader
	 * @param domain the domain for this classloader
	 * @param bundledata the bundledata for this classloader
	 * @param classpath the classpath for this classloader
	 */
	public DefaultClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, ProtectionDomain domain, BaseData bundledata, String[] classpath) {
		super(parent);
		this.delegate = delegate;
		this.domain = domain;
		this.manager = new ClasspathManager(bundledata, classpath, this);
	}

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
	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (Debug.DEBUG && Debug.DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		try {
			// Just ask the delegate.  This could result in findLocalClass(name) being called.
			Class clazz = delegate.findClass(name);
			// resolve the class if asked to.
			if (resolve)
				resolveClass(clazz);
			return (clazz);
		} catch (Error e) {
			if (Debug.DEBUG && Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		} catch (ClassNotFoundException e) {
			// If the class is not found do not try to look for it locally.
			// The delegate would have already done that for us.
			if (Debug.DEBUG && Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		}
	}

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
		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + delegate + "].getResource(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		URL url = delegate.findResource(name);
		if (url != null)
			return (url);

		if (Debug.DEBUG && Debug.DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + delegate + "].getResource(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return (null);
	}

	/**
	 * Finds all resources with the specified name.  This method must call
	 * delegate.findResources(name) to find all the resources.
	 * @param name The resource path to find.
	 * @return An Enumeration of all resources found or null if the resource.
	 * @throws IOException 
	 */
	protected Enumeration findResources(String name) throws IOException {
		return (delegate.findResources(name));
	}

	/**
	 * Finds a library for this bundle.  Simply calls 
	 * delegate.findLibrary(libname) to find the library.
	 * @param libname The library to find.
	 * @return The URL of the resource or null if it does not exist.
	 */
	protected String findLibrary(String libname) {
		// let the manager find the library for us
		return manager.findLibrary(libname);
	}

	public ProtectionDomain getDomain() {
		return domain;
	}

	public ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain) {
		return new ClasspathEntry(bundlefile, createProtectionDomain(bundlefile, cpDomain));
	}

	public Class defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry) {
		return defineClass(name, classbytes, 0, classbytes.length, classpathEntry.getDomain());
	}

	public Class publicFindLoaded(String classname) {
		return findLoadedClass(classname);
	}

	public Object publicGetPackage(String pkgname) {
		return getPackage(pkgname);
	}

	public Object publicDefinePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
		return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
	}

	public void initialize() {
		manager.initialize();
	}

	public URL findLocalResource(String resource) {
		return manager.findLocalResource(resource);
	}

	public Enumeration findLocalResources(String resource) {
		return manager.findLocalResources(resource);
	}

	public Class findLocalClass(String classname) throws ClassNotFoundException {
		return manager.findLocalClass(classname);
	}

	public void close() {
		manager.close();
	}

	public void attachFragment(BundleData sourcedata, ProtectionDomain sourcedomain, String[] sourceclasspath) {
		manager.attachFragment(sourcedata, sourcedomain, sourceclasspath);
	}

	public ClassLoaderDelegate getDelegate() {
		return delegate;
	}

	/**
	 * Creates a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain
	 * @param bundlefile The source bundlefile the domain is for.
	 * @param baseDomain The source domain.
	 * @return a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain 
	 */
	public static ProtectionDomain createProtectionDomain(BundleFile bundlefile, ProtectionDomain baseDomain) {
		// create a protection domain which knows about the codesource for this classpath entry (bug 89904)
		try {
			// use the permissions supplied by the domain passed in from the framework
			PermissionCollection permissions;
			if (baseDomain != null)
				permissions = baseDomain.getPermissions();
			else
				// no domain specified.  Better use a collection that has all permissions
				// this is done just incase someone sets the security manager later
				permissions = ALLPERMISSIONS;
			Certificate[] certs = null;
			if (bundlefile instanceof CertificateVerifier) {
				CertificateChain[] chains = ((CertificateVerifier) bundlefile).getChains();
				certs = chains == null || chains.length == 0 ? null : chains[0].getCertificates();
			}
			return new ProtectionDomain(new CodeSource(bundlefile.getBaseFile().toURL(), certs), permissions);
		} catch (MalformedURLException e) {
			// Failed to create our own domain; just return the baseDomain
			return baseDomain;
		}
	}

	public ClasspathManager getClasspathManager() {
		return manager;
	}
}
