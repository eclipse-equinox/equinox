/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapperChain;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public abstract class ModuleClassLoader extends ClassLoader implements BundleReference {
	public static class GenerationProtectionDomain extends ProtectionDomain implements BundleReference {
		private final Generation generation;

		public GenerationProtectionDomain(CodeSource codesource, PermissionCollection permissions, Generation generation) {
			super(codesource, permissions);
			this.generation = generation;
		}

		public Bundle getBundle() {
			return generation.getRevision().getBundle();
		}
	}

	/**
	 * A PermissionCollection for AllPermissions; shared across all ProtectionDomains when security is disabled
	 */
	protected static final PermissionCollection ALLPERMISSIONS;
	protected static final boolean REGISTERED_AS_PARALLEL;

	static {
		AllPermission allPerm = new AllPermission();
		ALLPERMISSIONS = allPerm.newPermissionCollection();
		if (ALLPERMISSIONS != null)
			ALLPERMISSIONS.add(allPerm);
		boolean registeredAsParallel;
		try {
			Method parallelCapableMetod = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable", (Class[]) null); //$NON-NLS-1$
			parallelCapableMetod.setAccessible(true);
			registeredAsParallel = ((Boolean) parallelCapableMetod.invoke(null, (Object[]) null)).booleanValue();
		} catch (Throwable e) {
			// must do everything to avoid failing in clinit
			registeredAsParallel = false;
		}
		REGISTERED_AS_PARALLEL = registeredAsParallel;
	}

	/**
	 * Constructs a new ModuleClassLoader.
	 * @param parent the parent classloader
	 */
	public ModuleClassLoader(ClassLoader parent) {
		super(parent);
	}

	/**
	 * Returns the generation of the host revision associated with this class loader
	 * @return the generation for this class loader
	 */
	protected abstract Generation getGeneration();

	/**
	 * Returns the Debug object for the Framework instance
	 * @return the Debug object for the Framework instance
	 */
	protected abstract Debug getDebug();

	/**
	 * Returns the classpath manager for this class loader
	 * @return the classpath manager for this class loader
	 */
	public abstract ClasspathManager getClasspathManager();

	/**
	 * Returns the configuration for the Framework instance
	 * @return the configuration for the Framework instance
	 */
	protected abstract EquinoxConfiguration getConfiguration();

	/**
	 * Returns the bundle loader for this class loader
	 * @return the bundle loader for this class loader
	 */
	public abstract BundleLoader getBundleLoader();

	/**
	 * Returns true if this class loader implementation has been
	 * registered with the JVM as a parallel class loader.
	 * This requires Java 7 or later.
	 * @return true if this class loader implementation has been
	 * registered with the JVM as a parallel class loader; otherwise
	 * false is returned.
	 */
	public abstract boolean isRegisteredAsParallel();

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
		if (getDebug().DEBUG_LOADER)
			Debug.println("BundleClassLoader[" + getBundleLoader() + "].loadClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		try {
			// Just ask the delegate.  This could result in findLocalClass(name) being called.
			Class<?> clazz = getBundleLoader().findClass(name);
			// resolve the class if asked to.
			if (resolve)
				resolveClass(clazz);
			return (clazz);
		} catch (Error e) {
			if (getDebug().DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + getBundleLoader() + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		} catch (ClassNotFoundException e) {
			// If the class is not found do not try to look for it locally.
			// The delegate would have already done that for us.
			if (getDebug().DEBUG_LOADER) {
				Debug.println("BundleClassLoader[" + getBundleLoader() + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return findLocalClass(name);
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
		if (getDebug().DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + getBundleLoader() + "].getResource(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		URL url = getBundleLoader().findResource(name);
		if (url != null)
			return (url);

		if (getDebug().DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + getBundleLoader() + "].getResource(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return (null);
	}

	@Override
	protected URL findResource(String name) {
		return findLocalResource(name);
	}

	/**
	 * Gets resources for the bundle.  First delegate.findResources(name) is
	 * called. The delegate will query the system class loader, bundle imports,
	 * bundle local resources, bundle hosts and fragments.  The delegate will
	 * call BundleClassLoader.findLocalResources(name) to find a resource local
	 * to this bundle.
	 * @param name The resource path to get.
	 * @return The Enumeration of the resource URLs.
	 */
	public Enumeration<URL> getResources(String name) throws IOException {
		if (getDebug().DEBUG_LOADER) {
			Debug.println("BundleClassLoader[" + getBundleLoader() + "].getResources(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		Enumeration<URL> result = getBundleLoader().findResources(name);
		if (getDebug().DEBUG_LOADER) {
			if (result == null || !result.hasMoreElements()) {
				Debug.println("BundleClassLoader[" + getBundleLoader() + "].getResources(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		return result;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return findLocalResources(name);
	}

	/**
	 * Finds a library for this bundle.  Simply calls 
	 * manager.findLibrary(libname) to find the library.
	 * @param libname The library to find.
	 * @return The absolution path to the library or null if not found
	 */
	protected String findLibrary(String libname) {
		// let the manager find the library for us
		return getClasspathManager().findLibrary(libname);
	}

	public ClasspathEntry createClassPathEntry(BundleFile bundlefile, Generation entryGeneration) {
		return new ClasspathEntry(bundlefile, createProtectionDomain(bundlefile, entryGeneration), entryGeneration);
	}

	public Class<?> defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry) {
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

	public URL findLocalResource(String resource) {
		return getClasspathManager().findLocalResource(resource);
	}

	public Enumeration<URL> findLocalResources(String resource) {
		return getClasspathManager().findLocalResources(resource);
	}

	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
		return getClasspathManager().findLocalClass(classname);
	}

	/**
	 * Creates a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain
	 * @param bundlefile The source bundlefile the domain is for.
	 * @param domainGeneration the source generation for the domain
	 * @return a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain 
	 */
	@SuppressWarnings("deprecation")
	protected ProtectionDomain createProtectionDomain(BundleFile bundlefile, Generation domainGeneration) {
		// create a protection domain which knows about the codesource for this classpath entry (bug 89904)
		ProtectionDomain baseDomain = domainGeneration.getDomain();
		try {
			// use the permissions supplied by the domain passed in from the framework
			PermissionCollection permissions;
			if (baseDomain != null) {
				permissions = baseDomain.getPermissions();
			} else {
				// no domain specified.  Better use a collection that has all permissions
				// this is done just incase someone sets the security manager later
				permissions = ALLPERMISSIONS;
			}
			Certificate[] certs = null;
			SignedContent signedContent = null;
			if (bundlefile instanceof BundleFileWrapperChain) {
				BundleFileWrapperChain wrapper = (BundleFileWrapperChain) bundlefile;
				while (wrapper != null && (!(wrapper.getWrapped() instanceof SignedContent)))
					wrapper = wrapper.getNext();
				signedContent = wrapper == null ? null : (SignedContent) wrapper.getWrapped();
			}
			if (getConfiguration().CLASS_CERTIFICATE && signedContent != null && signedContent.isSigned()) {
				SignerInfo[] signers = signedContent.getSignerInfos();
				if (signers.length > 0)
					certs = signers[0].getCertificateChain();
			}
			return new GenerationProtectionDomain(new CodeSource(bundlefile.getBaseFile().toURL(), certs), permissions, getGeneration());
			//return new ProtectionDomain(new CodeSource(bundlefile.getBaseFile().toURL(), certs), permissions);
		} catch (MalformedURLException e) {
			// Failed to create our own domain; just return the baseDomain
			return baseDomain;
		}
	}

	public Bundle getBundle() {
		return getGeneration().getRevision().getBundle();
	}

	public List<URL> findEntries(String path, String filePattern, int options) {
		return getClasspathManager().findEntries(path, filePattern, options);
	}

	public Collection<String> listResources(String path, String filePattern, int options) {
		return getBundleLoader().listResources(path, filePattern, options);
	}

	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		return getClasspathManager().listLocalResources(path, filePattern, options);
	}

	public String toString() {
		Bundle b = getBundle();
		StringBuffer result = new StringBuffer(super.toString());
		if (b == null)
			return result.toString();
		return result.append('[').append(b.getSymbolicName()).append(':').append(b.getVersion()).append("(id=").append(b.getBundleId()).append(")]").toString(); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void loadFragments(Collection<ModuleRevision> fragments) {
		getClasspathManager().loadFragments(fragments);
	}
}
