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
package org.eclipse.osgi.internal.composite;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.osgi.framework.Bundle;

public class CompositeClassLoader extends ClassLoader implements BaseClassLoader {

	private final ClassLoaderDelegate delegate;
	private final ClasspathManager manager;
	private final ClassLoaderDelegate companionDelegate;
	//Support to cut class / resource loading cycles in the context of one thread. The contained object is a set of classname
	private final ThreadLocal beingLoaded = new ThreadLocal();

	public CompositeClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, ClassLoaderDelegate companionDelegate, BaseData data) {
		super(parent);
		this.delegate = delegate;
		this.manager = new ClasspathManager(data, new String[0], this);
		this.companionDelegate = companionDelegate;
	}

	public ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain) {
		// nothing
		return null;
	}

	public Class defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry) {
		// nothing
		return null;
	}

	public ClasspathManager getClasspathManager() {
		return manager;
	}

	public ProtectionDomain getDomain() {
		// no domain
		return null;
	}

	public Object publicDefinePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
		return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
	}

	public Class publicFindLoaded(String classname) {
		return findLoadedClass(classname);
	}

	public Object publicGetPackage(String pkgname) {
		return getPackage(pkgname);
	}

	public void attachFragment(BundleData bundledata, ProtectionDomain domain, String[] classpath) {
		// nothing
	}

	public void close() {
		// nothing
	}

	public Class findLocalClass(String classname) throws ClassNotFoundException {
		if (!startLoading(classname))
			throw new ClassNotFoundException(classname);
		try {
			return companionDelegate.findClass(classname);
		} finally {
			stopLoading(classname);
		}
	}

	public URL findLocalResource(String resource) {
		if (!startLoading(resource))
			return null;
		try {
			return companionDelegate.findResource(resource);
		} finally {
			stopLoading(resource);
		}
	}

	public Enumeration findLocalResources(String resource) {
		if (!startLoading(resource))
			return null;
		try {
			return companionDelegate.findResources(resource);
		} catch (IOException e) {
			return null;
		} finally {
			stopLoading(resource);
		}
	}

	public ClassLoaderDelegate getDelegate() {
		return delegate;
	}

	public URL getResource(String name) {
		return delegate.findResource(name);
	}

	public void initialize() {
		manager.initialize();
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		return delegate.findClass(name);
	}

	private boolean startLoading(String name) {
		Set classesAndResources = (Set) beingLoaded.get();
		if (classesAndResources != null && classesAndResources.contains(name))
			return false;

		if (classesAndResources == null) {
			classesAndResources = new HashSet(3);
			beingLoaded.set(classesAndResources);
		}
		classesAndResources.add(name);
		return true;
	}

	private void stopLoading(String name) {
		((Set) beingLoaded.get()).remove(name);
	}

	public Bundle getBundle() {
		return manager.getBaseData().getBundle();
	}

	@SuppressWarnings("unchecked")
	public List<URL> findEntries(String path, String filePattern, int options) {
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> listResources(String path, String filePattern, int options) {
		return Collections.EMPTY_LIST;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		return Collections.EMPTY_LIST;
	}
}
