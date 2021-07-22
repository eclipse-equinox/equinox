/*******************************************************************************
 * Copyright (c) 2006, 2021 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.osgi.internal.url;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.storage.StorageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/*
 * An abstract class for handler factory impls (Stream and Content) that can
 * handle environments running multiple osgi frameworks with the same VM.
 */
public abstract class MultiplexingFactory {
	/**
	 * As a short-term (hopefully) solution we use a special class which is defined
	 * using the Unsafe class from the VM.  This class is an implementation of
	 * Collection<AccessibleObject> simply to provide a method add(AccessibleObject)
	 * which turns around and calls AccessibleObject.setAccessible(true).
	 * <p>
	 * The reason this is needed is to hack into the VM to get deep reflective access to
	 * the java.net package for the various hacks we have to do to multiplex the
	 * URL and Content handlers.  Note that on Java 9 deep reflection is not possible
	 * by default on the java.net package.
	 * <p>
	 * The setAccessible class will be defined in the java.base module which grants
	 * it the ability to call setAccessible(true) on other types from the java.base module
	 */
	public static final Collection<AccessibleObject> setAccessible;
	static final Collection<ClassLoader> systemLoaders;
	static {
		Collection<AccessibleObject> result = null;
		try {
			// Use reflection on Unsafe to avoid having to compile against it
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe"); //$NON-NLS-1$
			Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe"); //$NON-NLS-1$

			// NOTE: deep reflection is allowed on sun.misc package for java 9.
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);

			// The SetAccessible bytes stored in a resource to avoid real loading of it (see SetAccessible.java.src for source).
			byte[] bytes = StorageUtil.getBytes(MultiplexingFactory.class.getResource("SetAccessible.bytes").openStream(), -1, 4000); //$NON-NLS-1$

			Class<Collection<AccessibleObject>> collectionClass = null;
			// using defineAnonymousClass here because it seems more simple to get what we need
			try {
				Method defineAnonymousClass = unsafeClass.getMethod("defineAnonymousClass", Class.class, byte[].class, //$NON-NLS-1$
						Object[].class);
				@SuppressWarnings("unchecked")
				Class<Collection<AccessibleObject>> unchecked = (Class<Collection<AccessibleObject>>) defineAnonymousClass
						.invoke(unsafe, URL.class, bytes, (Object[]) null);
				collectionClass = unchecked;

			} catch (NoSuchMethodException e) {
				long offset = (long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, //$NON-NLS-1$
						MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")); //$NON-NLS-1$
				MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafeClass
						.getMethod("getObject", Object.class, long.class) //$NON-NLS-1$
						.invoke(unsafe, MethodHandles.Lookup.class, offset);
				lookup = lookup.in(URL.class);
				Class<?> classOption = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption"); //$NON-NLS-1$
				Object classOptions = Array.newInstance(classOption, 0);
				Method defineHiddenClass = Lookup.class.getMethod("defineHiddenClass", byte[].class, boolean.class, //$NON-NLS-1$
						classOptions.getClass());
				lookup = (Lookup) defineHiddenClass.invoke(lookup, bytes, Boolean.FALSE, classOptions);
				@SuppressWarnings("unchecked")
				Class<Collection<AccessibleObject>> unchecked = (Class<Collection<AccessibleObject>>) lookup
						.lookupClass();
				collectionClass = unchecked;
			}

			result = collectionClass.getConstructor().newInstance();
		} catch (Throwable t) {
			t.printStackTrace();
			// ingore as if there is no Unsafe
		}
		setAccessible = result;

		Collection<ClassLoader> loaders = new ArrayList<>();
		try {
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			while (cl != null) {
				loaders.add(cl);
				cl = cl.getParent();
			}
		} catch (Throwable t) {
			// ignore as if no loaders
		}
		systemLoaders = Collections.unmodifiableCollection(loaders);
	}
	protected EquinoxContainer container;
	protected BundleContext context;
	private List<Object> factories; // list of multiplexed factories

	// used to get access to the protected SecurityManager#getClassContext method
	static class InternalSecurityManager extends SecurityManager {
		@Override
		public Class<?>[] getClassContext() {
			return super.getClassContext();
		}
	}

	private static InternalSecurityManager internalSecurityManager = new InternalSecurityManager();

	MultiplexingFactory(BundleContext context, EquinoxContainer container) {
		this.context = context;
		this.container = container;

	}

	abstract public void setParentFactory(Object parentFactory);

	abstract public Object getParentFactory();

	public boolean isMultiplexing() {
		return getFactories() != null;
	}

	public void register(Object factory) {
		// set parent for each factory so they can do proper delegation
		try {
			Class<?> clazz = factory.getClass();
			Method setParentFactory = clazz.getMethod("setParentFactory", new Class[] {Object.class}); //$NON-NLS-1$
			setParentFactory.invoke(factory, new Object[] {getParentFactory()});
		} catch (Exception e) {
			container.getLogServices().log(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, "register", e); //$NON-NLS-1$
			// just return and not have it registered
			return;
		}
		addFactory(factory);
	}

	public void unregister(Object factory) {
		removeFactory(factory);
		// close the service tracker
		try {
			// this is brittle; if class does not directly extend MultplexingFactory then this method will not exist, but we do not want a public method here
			Method closeTracker = factory.getClass().getSuperclass().getDeclaredMethod("closePackageAdminTracker", (Class[]) null); //$NON-NLS-1$
			closeTracker.setAccessible(true); // its a private method
			closeTracker.invoke(factory, (Object[]) null);
		} catch (Exception e) {
			container.getLogServices().log(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, "unregister", e); //$NON-NLS-1$
			// just return without blowing up here
		}
	}

	public Object designateSuccessor() {
		List<Object> released = releaseFactories();
		// Note that we do this outside of the sync block above.
		// This is only possible because we do additional locking outside of
		// this class to ensure no other threads are trying to manipulate the
		// list of registered factories.  See Framework class the following methods:
		// Framework.installURLStreamHandlerFactory(BundleContext, FrameworkAdaptor)
		// Framework.installContentHandlerFactory(BundleContext, FrameworkAdaptor)
		// Framework.uninstallURLStreamHandlerFactory
		// Framework.uninstallContentHandlerFactory()
		if (released == null || released.isEmpty())
			return getParentFactory();
		Object successor = released.remove(0);
		try {
			Class<?> clazz = successor.getClass();
			Method register = clazz.getMethod("register", new Class[] {Object.class}); //$NON-NLS-1$
			for (Object r : released) {
				register.invoke(successor, new Object[] {r});
			}
		} catch (Exception e) {
			container.getLogServices().log(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, "designateSuccessor", e); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
		closePackageAdminTracker(); // close tracker
		return successor;
	}

	private void closePackageAdminTracker() {
		// Do nothing, just here for posterity
	}

	public Object findAuthorizedFactory(List<Class<?>> ignoredClasses) {
		List<Object> current = getFactories();
		Class<?>[] classStack = internalSecurityManager.getClassContext();
		for (Class<?> clazz : classStack) {
			if (clazz == InternalSecurityManager.class || clazz == MultiplexingFactory.class || ignoredClasses.contains(clazz) || isSystemClass(clazz))
				continue;
			if (hasAuthority(clazz))
				return this;
			if (current == null)
				continue;
			for (Object factory : current) {
				try {
					Method hasAuthorityMethod = factory.getClass().getMethod("hasAuthority", new Class[] {Class.class}); //$NON-NLS-1$
					if (((Boolean) hasAuthorityMethod.invoke(factory, new Object[] {clazz})).booleanValue()) {
						return factory;
					}
				} catch (Exception e) {
					container.getLogServices().log(MultiplexingFactory.class.getName(), FrameworkLogEntry.ERROR, "findAuthorizedURLStreamHandler-loop", e); //$NON-NLS-1$
					// we continue to the next factory here instead of failing
				}
			}
		}
		// Instead of returning null here, this factory is returned;
		// This means the root factory may provide protocol handlers for call stacks
		// that have no classes loaded by an bundle class loader.
		return this;
	}

	private boolean isSystemClass(final Class<?> clazz) {
		// we want to ignore classes from the system
		ClassLoader cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> clazz.getClassLoader());
		return cl == null || systemLoaders.contains(cl);
	}

	public boolean hasAuthority(Class<?> clazz) {
		Bundle b = FrameworkUtil.getBundle(clazz);
		if (!(b instanceof EquinoxBundle)) {
			return false;
		}
		return (container.getStorage().getModuleContainer() == ((EquinoxBundle) b).getModule().getContainer());
	}

	private synchronized List<Object> getFactories() {
		return factories;
	}

	private synchronized List<Object> releaseFactories() {
		if (factories == null)
			return null;

		List<Object> released = new LinkedList<>(factories);
		factories = null;
		return released;
	}

	private synchronized void addFactory(Object factory) {
		List<Object> updated = (factories == null) ? new LinkedList<>() : new LinkedList<>(factories);
		updated.add(factory);
		factories = updated;
	}

	private synchronized void removeFactory(Object factory) {
		List<Object> updated = new LinkedList<>(factories);
		updated.remove(factory);
		factories = updated.isEmpty() ? null : updated;
	}

	static void setAccessible(AccessibleObject o) {
		if (setAccessible != null) {
			setAccessible.add(o);
		} else {
			o.setAccessible(true);
		}
	}
}
