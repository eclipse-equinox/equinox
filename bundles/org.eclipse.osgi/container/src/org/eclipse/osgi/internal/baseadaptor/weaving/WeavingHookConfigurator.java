/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor.weaving;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.osgi.framework.*;

public class WeavingHookConfigurator implements HookConfigurator, ClassLoadingHook, ClassLoadingStatsHook {
	private BaseAdaptor adaptor;
	// holds the map of black listed hooks.  Use weak map to avoid pinning and simplify cleanup.
	private final Map<ServiceRegistration<?>, Boolean> blackList = Collections.synchronizedMap(new WeakHashMap<ServiceRegistration<?>, Boolean>());
	// holds the stack of WovenClass objects currently being used to define classes
	private final ThreadLocal<List<WovenClassImpl>> wovenClassStack = new ThreadLocal<List<WovenClassImpl>>();

	public void addHooks(HookRegistry hookRegistry) {
		this.adaptor = hookRegistry.getAdaptor();
		hookRegistry.addClassLoadingHook(this);
		hookRegistry.addClassLoadingStatsHook(this);
	}

	private ServiceRegistry getRegistry() {
		return ((Framework) adaptor.getEventPublisher()).getServiceRegistry();
	}

	public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		ServiceRegistry registry = getRegistry();
		if (registry == null)
			return null; // no registry somehow we are loading classes before the registry has been created
		ClassLoaderDelegate delegate = manager.getBaseClassLoader().getDelegate();
		BundleLoader loader;
		if (delegate instanceof BundleLoader) {
			loader = (BundleLoader) delegate;
		} else {
			Throwable e = new IllegalStateException("Could not obtain loader"); //$NON-NLS-1$
			adaptor.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, manager.getBaseData().getBundle(), e);
			return null;
		}
		// create a woven class object and add it to the thread local stack
		WovenClassImpl wovenClass = new WovenClassImpl(name, classbytes, entry, classpathEntry.getDomain(), loader, registry, blackList);
		List<WovenClassImpl> wovenClasses = wovenClassStack.get();
		if (wovenClasses == null) {
			wovenClasses = new ArrayList<WovenClassImpl>(6);
			wovenClassStack.set(wovenClasses);
		}
		wovenClasses.add(wovenClass);
		// call the weaving hooks
		try {
			return wovenClass.callHooks();
		} catch (Throwable t) {
			ServiceRegistration<?> errorHook = wovenClass.getErrorHook();
			Bundle errorBundle = errorHook != null ? errorHook.getReference().getBundle() : manager.getBaseData().getBundle();
			adaptor.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, errorBundle, t);
			// fail hard with a class loading error
			ClassFormatError error = new ClassFormatError("Unexpected error from weaving hook."); //$NON-NLS-1$
			error.initCause(t);
			throw error;
		}
	}

	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata, ProtectionDomain sourcedomain) {
		return false;
	}

	public String findLibrary(BaseData data, String libName) {
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		return null;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain, BaseData data, String[] bundleclasspath) {
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader, BaseData data) {
		// nothing
	}

	public void preFindLocalClass(String name, ClasspathManager manager) {
		// nothing
	}

	public void postFindLocalClass(String name, Class<?> clazz, ClasspathManager manager) {
		// nothing
	}

	public void preFindLocalResource(String name, ClasspathManager manager) {
		// nothing
	}

	public void postFindLocalResource(String name, URL resource, ClasspathManager manager) {
		// nothing
	}

	public void recordClassDefine(String name, Class<?> clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// here we assume the stack contans a woven class with the same name as the class we are defining.
		List<WovenClassImpl> wovenClasses = wovenClassStack.get();
		if (wovenClasses == null || wovenClasses.size() == 0)
			return;
		WovenClassImpl wovenClass = wovenClasses.remove(wovenClasses.size() - 1);
		// inform the woven class about the class that was defined.
		wovenClass.setWeavingCompleted(clazz);
	}

}
