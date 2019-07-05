/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Green - fix factories with non-standard class loading (bug 200068) 
 *     Filip Hrbek - fix thread safety problem described in bug 305863
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * This class is the standard implementation of <code>IAdapterManager</code>. It provides
 * fast lookup of property values with the following semantics:
 * <ul>
 * <li>At most one factory will be invoked per property lookup
 * <li>If multiple installed factories provide the same adapter, only the first found in
 * the search order will be invoked.
 * <li>The search order from a class with the definition <br>
 * <code>class X extends Y implements A, B</code><br> is as follows: <il>
 * <li>the target's class: X
 * <li>X's superclasses in order to <code>Object</code>
 * <li>a breadth-first traversal of each class's interfaces in the
 * order returned by <code>getInterfaces</code> (in the example, X's 
 * superinterfaces then Y's superinterfaces) </li>
 * </ul>
 * 
 * @see IAdapterFactory
 * @see IAdapterManager
 */
public final class AdapterManager implements IAdapterManager {
	/** 
	 * Cache of adapters for a given adaptable class. Maps String  -> Map
	 * (adaptable class name -> (adapter class name -> factory instance))
	 * Thread safety note: The outer map is synchronized using a synchronized
	 * map wrapper class.  The inner map is not synchronized, but it is immutable
	 * so synchronization is not necessary.
	 */
	private Map<String, Map<String, IAdapterFactory>> adapterLookup;

	/**
	 * Cache of classes for a given type name. Avoids too many loadClass calls.
	 * (factory -> (type name -> Class)).
	 * Thread safety note: Since this structure is a nested hash map, and both
	 * the inner and outer maps are mutable, access to this entire structure is
	 * controlled by the classLookupLock field.  Note the field can still be
	 * nulled concurrently without holding the lock.
	 */
	private Map<IAdapterFactory, Map<String, Class<?>>> classLookup;

	/**
	 * The lock object controlling access to the classLookup data structure.
	 */
	private final Object classLookupLock = new Object();

	/**
	 * Cache of class lookup order (Class -> Class[]). This avoids having to compute often, and
	 * provides clients with quick lookup for instanceOf checks based on type name.
	 * Thread safety note: The map is synchronized using a synchronized
	 * map wrapper class.  The arrays within the map are immutable.
	 */
	private Map<Class<?>, Class<?>[]> classSearchOrderLookup;

	/**
	 * Map of factories, keyed by <code>String</code>, fully qualified class name of
	 * the adaptable class that the factory provides adapters for. Value is a <code>List</code>
	 * of <code>IAdapterFactory</code>.
	 */
	private final HashMap<String, List<IAdapterFactory>> factories;

	private final ArrayList<IAdapterManagerProvider> lazyFactoryProviders;

	private static final AdapterManager singleton = new AdapterManager();

	public static AdapterManager getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private AdapterManager() {
		factories = new HashMap<>(5);
		lazyFactoryProviders = new ArrayList<>(1);
	}

	/**
	 * Given a type name, add all of the factories that respond to those types into
	 * the given table. Each entry will be keyed by the adapter class name (supplied in
	 * IAdapterFactory.getAdapterList).
	 */
	private void addFactoriesFor(String typeName, Map<String, IAdapterFactory> table) {
		List<IAdapterFactory> factoryList = getFactories().get(typeName);
		if (factoryList == null)
			return;
		for (int i = 0, imax = factoryList.size(); i < imax; i++) {
			IAdapterFactory factory = factoryList.get(i);
			if (factory instanceof IAdapterFactoryExt) {
				String[] adapters = ((IAdapterFactoryExt) factory).getAdapterNames();
				for (String adapter : adapters) {
					if (table.get(adapter) == null) {
						table.put(adapter, factory);
					}
				}
			} else {
				Class<?>[] adapters = factory.getAdapterList();
				for (Class<?> adapter : adapters) {
					String adapterName = adapter.getName();
					if (table.get(adapterName) == null)
						table.put(adapterName, factory);
				}
			}
		}
	}

	private void cacheClassLookup(IAdapterFactory factory, Class<?> clazz) {
		synchronized (classLookupLock) {
			//cache reference to lookup to protect against concurrent flush
			Map<IAdapterFactory, Map<String, Class<?>>> lookup = classLookup;
			if (lookup == null)
				classLookup = lookup = new HashMap<>(4);
			Map<String, Class<?>> classes = lookup.get(factory);
			if (classes == null) {
				classes = new HashMap<>(4);
				lookup.put(factory, classes);
			}
			classes.put(clazz.getName(), clazz);
		}
	}

	private Class<?> cachedClassForName(IAdapterFactory factory, String typeName) {
		synchronized (classLookupLock) {
			Class<?> clazz = null;
			//cache reference to lookup to protect against concurrent flush
			Map<IAdapterFactory, Map<String, Class<?>>> lookup = classLookup;
			if (lookup != null) {
				Map<String, Class<?>> classes = lookup.get(factory);
				if (classes != null) {
					clazz = classes.get(typeName);
				}
			}
			return clazz;
		}
	}

	/**
	 * Returns the class with the given fully qualified name, or null
	 * if that class does not exist or belongs to a plug-in that has not
	 * yet been loaded.
	 */
	private Class<?> classForName(IAdapterFactory factory, String typeName) {
		Class<?> clazz = cachedClassForName(factory, typeName);
		if (clazz == null) {
			if (factory instanceof IAdapterFactoryExt)
				factory = ((IAdapterFactoryExt) factory).loadFactory(false);
			if (factory != null) {
				try {
					clazz = factory.getClass().getClassLoader().loadClass(typeName);
				} catch (ClassNotFoundException e) {
					// it is possible that the default bundle classloader is unaware of this class
					// but the adaptor factory can load it in some other way. See bug 200068.
					if (typeName == null)
						return null;
					Class<?>[] adapterList = factory.getAdapterList();
					clazz = null;
					for (Class<?> adapter : adapterList) {
						if (typeName.equals(adapter.getName())) {
							clazz = adapter;
							break;
						}
					}
					if (clazz == null)
						return null; // class not yet loaded
				}
				cacheClassLookup(factory, clazz);
			}
		}
		return clazz;
	}

	@Override
	public String[] computeAdapterTypes(Class<? extends Object> adaptable) {
		Set<String> types = getFactories(adaptable).keySet();
		return types.toArray(new String[types.size()]);
	}

	/**
	 * Computes the adapters that the provided class can adapt to, along
	 * with the factory object that can perform that transformation. Returns 
	 * a table of adapter class name to factory object.
	 */
	private Map<String, IAdapterFactory> getFactories(Class<? extends Object> adaptable) {
		//cache reference to lookup to protect against concurrent flush
		Map<String, Map<String, IAdapterFactory>> lookup = adapterLookup;
		if (lookup == null)
			adapterLookup = lookup = Collections.synchronizedMap(new HashMap<String, Map<String, IAdapterFactory>>(30));
		Map<String, IAdapterFactory> table = lookup.get(adaptable.getName());
		if (table == null) {
			// calculate adapters for the class
			table = new HashMap<>(4);
			Class<?>[] classes = computeClassOrder(adaptable);
			for (Class<?> cl : classes) {
				addFactoriesFor(cl.getName(), table);
			}
			// cache the table
			lookup.put(adaptable.getName(), table);
		}
		return table;
	}

	/**
	 * Returns the super-type search order starting with <code>adaptable</code>. 
	 * The search order is defined in this class' comment.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<? super T>[] computeClassOrder(Class<T> adaptable) {
		Class<?>[] classes = null;
		//cache reference to lookup to protect against concurrent flush
		Map<Class<?>, Class<?>[]> lookup = classSearchOrderLookup;
		if (lookup == null)
			classSearchOrderLookup = lookup = Collections.synchronizedMap(new HashMap<Class<?>, Class<?>[]>());
		else
			classes = lookup.get(adaptable);
		// compute class order only if it hasn't been cached before
		if (classes == null) {
			classes = doComputeClassOrder(adaptable);
			lookup.put(adaptable, classes);
		}
		return (Class<? super T>[]) classes;
	}

	/**
	 * Computes the super-type search order starting with <code>adaptable</code>. 
	 * The search order is defined in this class' comment.
	 */
	private Class<?>[] doComputeClassOrder(Class<?> adaptable) {
		List<Class<?>> classes = new ArrayList<>();
		Class<?> clazz = adaptable;
		Set<Class<?>> seen = new HashSet<>(4);
		//first traverse class hierarchy
		while (clazz != null) {
			classes.add(clazz);
			clazz = clazz.getSuperclass();
		}
		//now traverse interface hierarchy for each class
		Class<?>[] classHierarchy = classes.toArray(new Class[classes.size()]);
		for (Class<?> cl : classHierarchy) {
			computeInterfaceOrder(cl.getInterfaces(), classes, seen);
		}
		return classes.toArray(new Class[classes.size()]);
	}

	private void computeInterfaceOrder(Class<?>[] interfaces, Collection<Class<?>> classes, Set<Class<?>> seen) {
		List<Class<?>> newInterfaces = new ArrayList<>(interfaces.length);
		for (Class<?> interfac : interfaces) {
			if (seen.add(interfac)) {
				//note we cannot recurse here without changing the resulting interface order
				classes.add(interfac);
				newInterfaces.add(interfac);
			}
		}
		for (Class<?> clazz : newInterfaces)
			computeInterfaceOrder(clazz.getInterfaces(), classes, seen);
	}

	/**
	 * Flushes the cache of adapter search paths. This is generally required whenever an
	 * adapter is added or removed.
	 * <p>
	 * It is likely easier to just toss the whole cache rather than trying to be smart
	 * and remove only those entries affected.
	 * </p>
	 */
	public synchronized void flushLookup() {
		adapterLookup = null;
		classLookup = null;
		classSearchOrderLookup = null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptable, Class<T> adapterType) {
		Assert.isNotNull(adaptable);
		Assert.isNotNull(adapterType);
		String adapterTypeName = adapterType.getName();
		IAdapterFactory factory = getFactories(adaptable.getClass()).get(adapterTypeName);
		T result = null;
		if (factory != null) {
			result = factory.getAdapter(adaptable, adapterType);
		}
		if (result == null && adapterType.isInstance(adaptable)) {
			return (T) adaptable;
		}
		if (result == null || adapterType.isInstance(result)) {
			return result;
		}
		// Here we have a result which does not match the expected type.
		// Throwing an exception here allows us to identify the violating factory.
		// If we don't do this, clients will just see CCE without any idea who
		// supplied the wrong adapter
		throw new AssertionFailedException("Adapter factory " //$NON-NLS-1$
				+ factory + " returned " + result.getClass().getName() //$NON-NLS-1$
				+ " that is not an instance of " + adapterTypeName); //$NON-NLS-1$
	}

	@Override
	public Object getAdapter(Object adaptable, String adapterType) {
		Assert.isNotNull(adaptable);
		Assert.isNotNull(adapterType);
		return getAdapter(adaptable, adapterType, false);
	}

	/**
	 * Returns an adapter of the given type for the provided adapter.
	 * @param adaptable the object to adapt
	 * @param adapterType the type to adapt the object to
	 * @param force <code>true</code> if the plug-in providing the
	 * factory should be activated if necessary. <code>false</code>
	 * if no plugin activations are desired.
	 */
	private Object getAdapter(Object adaptable, String adapterType, boolean force) {
		IAdapterFactory factory = getFactories(adaptable.getClass()).get(adapterType);
		if (force && factory instanceof IAdapterFactoryExt)
			factory = ((IAdapterFactoryExt) factory).loadFactory(true);
		Object result = null;
		if (factory != null) {
			Class<?> clazz = classForName(factory, adapterType);
			if (clazz != null)
				result = factory.getAdapter(adaptable, clazz);
		}
		if (result == null && adaptable.getClass().getName().equals(adapterType))
			return adaptable;
		return result;
	}

	@Override
	public boolean hasAdapter(Object adaptable, String adapterTypeName) {
		return getFactories(adaptable.getClass()).get(adapterTypeName) != null;
	}

	@Override
	public int queryAdapter(Object adaptable, String adapterTypeName) {
		IAdapterFactory factory = getFactories(adaptable.getClass()).get(adapterTypeName);
		if (factory == null)
			return NONE;
		if (factory instanceof IAdapterFactoryExt) {
			factory = ((IAdapterFactoryExt) factory).loadFactory(false); // don't force loading
			if (factory == null)
				return NOT_LOADED;
		}
		return LOADED;
	}

	@Override
	public Object loadAdapter(Object adaptable, String adapterTypeName) {
		return getAdapter(adaptable, adapterTypeName, true);
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	@Override
	public synchronized void registerAdapters(IAdapterFactory factory, Class<?> adaptable) {
		registerFactory(factory, adaptable.getName());
		flushLookup();
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	public void registerFactory(IAdapterFactory factory, String adaptableType) {
		List<IAdapterFactory> list = factories.get(adaptableType);
		if (list == null) {
			list = new ArrayList<>(5);
			factories.put(adaptableType, list);
		}
		list.add(factory);
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
	public synchronized void unregisterAdapters(IAdapterFactory factory) {
		for (List<IAdapterFactory> list : factories.values())
			list.remove(factory);
		flushLookup();
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
	public synchronized void unregisterAdapters(IAdapterFactory factory, Class<?> adaptable) {
		List<IAdapterFactory> factoryList = factories.get(adaptable.getName());
		if (factoryList == null)
			return;
		factoryList.remove(factory);
		flushLookup();
	}

	/*
	 * Shuts down the adapter manager by removing all factories
	 * and removing the registry change listener. Should only be
	 * invoked during platform shutdown.
	 */
	public synchronized void unregisterAllAdapters() {
		factories.clear();
		flushLookup();
	}

	public void registerLazyFactoryProvider(IAdapterManagerProvider factoryProvider) {
		synchronized (lazyFactoryProviders) {
			lazyFactoryProviders.add(factoryProvider);
		}
	}

	public boolean unregisterLazyFactoryProvider(IAdapterManagerProvider factoryProvider) {
		synchronized (lazyFactoryProviders) {
			return lazyFactoryProviders.remove(factoryProvider);
		}
	}

	public HashMap<String, List<IAdapterFactory>> getFactories() {
		synchronized (lazyFactoryProviders) {
			while (lazyFactoryProviders.size() > 0) {
				IAdapterManagerProvider provider = lazyFactoryProviders.remove(0);
				if (provider.addFactories(this))
					flushLookup();
			}
		}
		return factories;
	}
}
