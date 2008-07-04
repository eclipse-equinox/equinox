/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked      
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public abstract class AbstractAspectJHook implements HookConfigurator, AdaptorHook, BundleFileWrapperFactoryHook , ClassLoadingHook, ClassLoadingStatsHook {

	public static boolean verbose = Boolean.getBoolean("org.aspectj.osgi.verbose");
	
	private SupplementerRegistry supplementerRegistry;
	
	public void addHooks (HookRegistry hooks) {
		if (verbose) System.err.println("[org.aspectj.osgi] info adding AspectJ hooks ...");

		supplementerRegistry = new SupplementerRegistry();

		hooks.addAdaptorHook(this);
		hooks.addClassLoadingHook(this);
		hooks.addBundleFileWrapperFactoryHook(this);
		hooks.addClassLoadingStatsHook(this);
		hooks.addStorageHook(new AspectJStorageHook(supplementerRegistry));
	}
	
	public SupplementerRegistry getSupplementerRegistry() {
		return this.supplementerRegistry;
	}

	public BundleFile wrapBundleFile(BundleFile bundleFile, Object content, BaseData data, boolean base) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean addClassPathEntry(ArrayList cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		// TODO Auto-generated method stub
		return false;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		// TODO Auto-generated method stub
		return null;
	}

	public String findLibrary(BaseData data, String libName) {
		// TODO Auto-generated method stub
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// TODO Auto-generated method stub
		
	}

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addProperties(Properties properties) {
		// TODO Auto-generated method stub
		
	}

	public FrameworkLog createFrameworkLog() {
		// TODO Auto-generated method stub
		return null;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		// TODO Auto-generated method stub
		
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		// TODO Auto-generated method stub
		
	}

	public void frameworkStopping(BundleContext context) {
		// TODO Auto-generated method stub
		
	}

	public void handleRuntimeError(Throwable error) {
		// TODO Auto-generated method stub
		
	}

	public void initialize(BaseAdaptor adaptor) {
		// TODO Auto-generated method stub
		
	}

	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean matchDNChain(String pattern, String[] dnChain) {
		// TODO Auto-generated method stub
		return false;
	}

	public void postFindLocalClass(String name, Class clazz, ClasspathManager manager) {
		// TODO Auto-generated method stub
		
	}

	public void postFindLocalResource(String name, URL resource, ClasspathManager manager) {
		// TODO Auto-generated method stub
		
	}

	public void preFindLocalClass(String name, ClasspathManager manager) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public void preFindLocalResource(String name, ClasspathManager manager) {
		// TODO Auto-generated method stub
		
	}

	public void recordClassDefine(String name, Class clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// TODO Auto-generated method stub
		
	}
	
}
