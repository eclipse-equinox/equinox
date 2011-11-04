/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.osgi.framework.Bundle;

/**
 * The default implementation of <code>BaseClassLoader</code>.  This implementation extends
 * <code>ClassLoader</code>.
 * @see BaseClassLoader
 * @see ClasspathManager
 */
public class DefaultClassLoader extends ClassLoader implements ParallelClassLoader {
	/**
	 * A PermissionCollection for AllPermissions; shared across all ProtectionDomains when security is disabled
	 */
	protected static final PermissionCollection ALLPERMISSIONS;
	private final static String CLASS_CERTIFICATE_SUPPORT = "osgi.support.class.certificate"; //$NON-NLS-1$
	private final static String CLASS_LOADER_TYPE = "osgi.classloader.type"; //$NON-NLS-1$
	private final static String CLASS_LOADER_TYPE_PARALLEL = "parallel"; //$NON-NLS-1$
	private static final boolean CLASS_CERTIFICATE;
	private static final boolean PARALLEL_CAPABLE;
	@SuppressWarnings("unchecked")
	private static final Enumeration<URL> EMPTY_ENUMERATION = Collections.enumeration(Collections.EMPTY_LIST);

	static {
		CLASS_CERTIFICATE = Boolean.valueOf(FrameworkProperties.getProperty(CLASS_CERTIFICATE_SUPPORT, "true")).booleanValue(); //$NON-NLS-1$
		AllPermission allPerm = new AllPermission();
		ALLPERMISSIONS = allPerm.newPermissionCollection();
		if (ALLPERMISSIONS != null)
			ALLPERMISSIONS.add(allPerm);
		boolean typeParallel = CLASS_LOADER_TYPE_PARALLEL.equals(FrameworkProperties.getProperty(CLASS_LOADER_TYPE, CLASS_LOADER_TYPE_PARALLEL));
		boolean parallelCapable = false;
		try {
			if (typeParallel) {
				Method parallelCapableMetod = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable", (Class[]) null); //$NON-NLS-1$
				parallelCapableMetod.setAccessible(true);
				parallelCapable = ((Boolean) parallelCapableMetod.invoke(null, (Object[]) null)).booleanValue();
			}
		} catch (Throwable e) {
			// must do everything to avoid failing in clinit
			parallelCapable = false;
		}
		PARALLEL_CAPABLE = parallelCapable;
	}

	protected ClassLoaderDelegate delegate;
	protected ProtectionDomain domain;
	// Note that PDE has internal dependency on this field type/name (bug 267238)
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
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (Debug.DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		try {
			// Just ask the delegate.  This could result in findLocalClass(name) being called.
			Class<?> clazz = delegate.findClass(name);
			// resolve the class if asked to.
			if (resolve)
				resolveClass(clazz);
			return (clazz);
		} catch (Error e) {
			if (Debug.DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + delegate + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		} catch (ClassNotFoundException e) {
			// If the class is not found do not try to look for it locally.
			// The delegate would have already done that for us.
			if (Debug.DEBUG_LOADER) {
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
		if (Debug.DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + delegate + "].getResource(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		URL url = delegate.findResource(name);
		if (url != null)
			return (url);

		if (Debug.DEBUG_LOADER) {
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
	protected Enumeration<URL> findResources(String name) throws IOException {
		Enumeration<URL> result = delegate.findResources(name);
		if (result == null)
			return EMPTY_ENUMERATION;
		return result;
	}

	/**
	 * Finds a library for this bundle.  Simply calls 
	 * manager.findLibrary(libname) to find the library.
	 * @param libname The library to find.
	 * @return The absolution path to the library or null if not found
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

	public Class<?> defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry) {
		return defineClass(name, classbytes, 0, classbytes.length, classpathEntry.getDomain());
	}

	public Class<?> publicFindLoaded(String classname) {
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

	public Enumeration<URL> findLocalResources(String resource) {
		return manager.findLocalResources(resource);
	}

	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
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
	@SuppressWarnings("deprecation")
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
			SignedContent signedContent = null;
			if (bundlefile instanceof BundleFileWrapperChain) {
				BundleFileWrapperChain wrapper = (BundleFileWrapperChain) bundlefile;
				while (wrapper != null && (!(wrapper.getWrapped() instanceof SignedContent)))
					wrapper = wrapper.getNext();
				signedContent = wrapper == null ? null : (SignedContent) wrapper.getWrapped();
			}
			if (CLASS_CERTIFICATE && signedContent != null && signedContent.isSigned()) {
				SignerInfo[] signers = signedContent.getSignerInfos();
				if (signers.length > 0)
					certs = signers[0].getCertificateChain();
			}
			return new BundleProtectionDomain(permissions, new CodeSource(bundlefile.getBaseFile().toURL(), certs), null);
		} catch (MalformedURLException e) {
			// Failed to create our own domain; just return the baseDomain
			return baseDomain;
		}
	}

	public ClasspathManager getClasspathManager() {
		return manager;
	}

	public Bundle getBundle() {
		return manager.getBaseData().getBundle();
	}

	public boolean isParallelCapable() {
		return PARALLEL_CAPABLE;
	}

	public List<URL> findEntries(String path, String filePattern, int options) {
		return manager.findEntries(path, filePattern, options);
	}

	public Collection<String> listResources(String path, String filePattern, int options) {
		return delegate.listResources(path, filePattern, options);
	}

	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		return manager.listLocalResources(path, filePattern, options);
	}

	public String toString() {
		Bundle b = getBundle();
		StringBuffer result = new StringBuffer(super.toString());
		if (b == null)
			return result.toString();
		return result.append('[').append(b.getSymbolicName()).append(':').append(b.getVersion()).append("(id=").append(b.getBundleId()).append(")]").toString(); //$NON-NLS-1$//$NON-NLS-2$
	}
}
