/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 576660 - AdapterManager should use more modern concurrency primitives
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;

/**
 * This class is the standard implementation of <code>IAdapterManager</code>. It
 * provides fast lookup of property values with the following semantics:
 * <ul>
 * <li>If multiple installed factories provide the same adapter, iterate until
 * one of the factories return a non-<code>null</code> value. Remaining
 * factories won't be invoked.</li>
 * <li>The search order from a class with the definition <br>
 * <code>class X extends Y implements A, B</code><br>
 * is as follows:
 * <ul>
 * <li>the target's class: X
 * <li>X's superclasses in order to <code>Object</code>
 * <li>a breadth-first traversal of each class's interfaces in the order
 * returned by <code>getInterfaces</code> (in the example, X's superinterfaces
 * then Y's superinterfaces)</li>
 * </ul>
 * </ul>
 * 
 * @see IAdapterFactory
 * @see IAdapterManager
 */
public final class AdapterManager implements IAdapterManager {
	/**
	 * Cache of adapters for a given adaptable class. Maps String -> Map (adaptable
	 * class name -> (adapter class name -> factory instance)) <b>Thread safety
	 * note</b>: always use the compute methods to update the map and make sure the
	 * values (inner map) are never modified but replaced if necessary.
	 */
	private final ConcurrentMap<String, AdapterLookup> adapterLookup;

	/**
	 * Cache of classes for a given type name. Avoids too many loadClass calls.
	 * (factory -> (type name -> Class)). Thread safety note: always use the compute
	 * methods to update the map and make sure the values (inner map) are modified
	 * also this way.
	 */
	private final ConcurrentMap<IAdapterFactory, ConcurrentMap<String, Class<?>>> classLookup;

	/**
	 * Cache of class lookup order (Class -> Class[]). This avoids having to compute
	 * often, and provides clients with quick lookup for instanceOf checks based on
	 * type name. Thread safety note: always use the compute methods to update the
	 * map and make sure the values (Class array) are never modified but replaced if
	 * necessary.
	 */
	private final ConcurrentMap<Class<?>, Class<?>[]> classSearchOrderLookup;

	/**
	 * Map of factories, keyed by <code>String</code>, fully qualified class name of
	 * the adaptable class that the factory provides adapters for. Value is a
	 * <code>List</code> of <code>IAdapterFactory</code>.
	 */
	private final ConcurrentMap<String, List<IAdapterFactory>> factories;

	private final Queue<IAdapterManagerProvider> lazyFactoryProviders;

	private static final AdapterManager singleton = new AdapterManager();

	public static AdapterManager getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private AdapterManager() {
		classSearchOrderLookup = new ConcurrentHashMap<>();
		adapterLookup = new ConcurrentHashMap<>();
		lazyFactoryProviders = new ConcurrentLinkedQueue<>();
		factories = new ConcurrentHashMap<>();
		classLookup = new ConcurrentHashMap<>();
	}

	private static boolean isFactoryLoaded(IAdapterFactory adapterFactory) {
		return (!(adapterFactory instanceof IAdapterFactoryExt))
				|| ((IAdapterFactoryExt) adapterFactory).loadFactory(false) != null;
	}

	/**
	 * Given a type name, add all of the factories that respond to those types into
	 * the given table. Each entry will be keyed by the adapter class name (supplied
	 * in IAdapterFactory.getAdapterList).
	 */
	private void addFactoriesFor(String adaptableTypeName, Map<String, List<IAdapterFactory>> table) {
		List<IAdapterFactory> factoryList = getFactories().get(adaptableTypeName);
		if (factoryList == null) {
			return;
		}
		for (IAdapterFactory factory : factoryList) {
			if (factory instanceof IAdapterFactoryExt) {
				String[] adapters = ((IAdapterFactoryExt) factory).getAdapterNames();
				for (String adapter : adapters) {
					table.computeIfAbsent(adapter, any -> new ArrayList<>(1)).add(factory);
				}
			} else {
				Class<?>[] adapters = factory.getAdapterList();
				for (Class<?> adapter : adapters) {
					table.computeIfAbsent(adapter.getName(), any -> new ArrayList<>(1)).add(factory);
				}
			}
		}
	}

	/**
	 * Queries an {@link IAdapterFactory} for a given type name to return a
	 * compatible class object
	 * 
	 * @param adapterFactory the {@link IAdapterFactory} to query for the given
	 *                       classname, must not be <code>null</code>
	 * @param typeName       the name of the desired class, must not be
	 *                       <code>null</code>
	 * @return the class with the given fully qualified name, or <code>null</code>
	 *         if that class does not exist or belongs to a plug-in that has not yet
	 *         been loaded.
	 */
	private Class<?> classForName(IAdapterFactory adapterFactory, String typeName) {
		return classLookup.computeIfAbsent(adapterFactory, factory -> new ConcurrentHashMap<>())
				.computeIfAbsent(typeName, type -> {
					return loadFactory(adapterFactory, false).map(factory -> {
						try {
							return factory.getClass().getClassLoader().loadClass(typeName);
						} catch (ClassNotFoundException e) {
							// it is possible that the default bundle classloader is unaware of this class
							// but the adaptor factory can load it in some other way. See bug 200068.
							Class<?>[] adapterList = factory.getAdapterList();
							for (Class<?> adapter : adapterList) {
								if (typeName.equals(adapter.getName())) {
									return adapter;
								}
							}
						}
						return null; // class not yet loaded
					}).orElse(null); // factory not loaded yet
				});
	}

	@Override
	public String[] computeAdapterTypes(Class<? extends Object> adaptable) {
		Set<String> types = getFactories(adaptable).keySet();
		return types.toArray(new String[types.size()]);
	}

	/**
	 * Computes the adapters that the provided class can adapt to, along with the
	 * factory object that can perform that transformation. Returns a table of
	 * adapter class name to factory object.
	 */
	private Map<String, List<IAdapterFactory>> getFactories(Class<? extends Object> adaptable) {
		// cache reference to lookup to protect against concurrent flush
		return adapterLookup.computeIfAbsent(adaptable.getName(), adaptableType -> new AdapterLookup(adaptable, this))
				.getMap();
	}

	/**
	 * Returns the super-type search order starting with <code>adaptable</code>. The
	 * search order is defined in this class' comment.
	 */
	@Override
	public <T> Class<? super T>[] computeClassOrder(Class<T> adaptable) {
		Class<? super T>[] classOrder = getClassOrder(adaptable);
		return Arrays.copyOf(classOrder, classOrder.length);
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? super T>[] getClassOrder(Class<T> adaptable) {
		return (Class<? super T>[]) classSearchOrderLookup.computeIfAbsent(adaptable,
				AdapterManager::doComputeClassOrder);
	}

	/**
	 * Computes the super-type search order starting with <code>adaptable</code>.
	 * The search order is defined in this class' comment.
	 */
	private static Class<?>[] doComputeClassOrder(Class<?> adaptable) {
		List<Class<?>> classes = new ArrayList<>();
		Class<?> clazz = adaptable;
		Set<Class<?>> seen = new HashSet<>(4);
		// first traverse class hierarchy
		while (clazz != null) {
			classes.add(clazz);
			clazz = clazz.getSuperclass();
		}
		// now traverse interface hierarchy for each class
		Class<?>[] classHierarchy = classes.toArray(new Class[classes.size()]);
		for (Class<?> cl : classHierarchy) {
			computeInterfaceOrder(cl.getInterfaces(), classes, seen);
		}
		return classes.toArray(new Class[classes.size()]);
	}

	private static void computeInterfaceOrder(Class<?>[] interfaces, Collection<Class<?>> classes, Set<Class<?>> seen) {
		List<Class<?>> newInterfaces = new ArrayList<>(interfaces.length);
		for (Class<?> interfac : interfaces) {
			if (seen.add(interfac)) {
				// note we cannot recurse here without changing the resulting interface order
				classes.add(interfac);
				newInterfaces.add(interfac);
			}
		}
		for (Class<?> clazz : newInterfaces) {
			computeInterfaceOrder(clazz.getInterfaces(), classes, seen);
		}
	}

	/**
	 * Flushes the cache of adapter search paths. This is generally required
	 * whenever an adapter is added or removed.
	 * <p>
	 * It is likely easier to just toss the whole cache rather than trying to be
	 * smart and remove only those entries affected.
	 * </p>
	 */
	public void flushLookup() {
		adapterLookup.clear();
		classLookup.clear();
		classSearchOrderLookup.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptable, Class<T> adapterType) {
		Assert.isNotNull(adaptable);
		Assert.isNotNull(adapterType);
		List<Entry<IAdapterFactory, Class<?>>> incorrectAdapters = new ArrayList<>();
		T adapterObject = getFactories(adaptable.getClass())
				.getOrDefault(adapterType.getName(), Collections.emptyList()) //
				.stream() //
				.map(factory -> new SimpleEntry<>(factory, factory.getAdapter(adaptable, adapterType))) //
				.filter(entry -> {
					Object adapter = entry.getValue();
					if (adapter == null) {
						return false;
					}
					boolean res = adapterType.isInstance(adapter);
					if (!res) {
						IAdapterFactory factory = entry.getKey();
						incorrectAdapters.add(new SimpleEntry<>(factory, adapter.getClass()));
					}
					return res;
				}).map(Entry::getValue) //
				.findFirst() //
				.orElse(null);
		if (adapterObject == null) {
			if (!incorrectAdapters.isEmpty()) {
				throw new AssertionFailedException(incorrectAdapters.stream().map(entry -> "Adapter factory " //$NON-NLS-1$
						+ entry.getKey() + " returned " + entry.getValue().getName() //$NON-NLS-1$
						+ " that is not an instance of " + adapterType.getName()).collect(Collectors.joining("\n"))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (adapterType.isInstance(adaptable)) {
				return (T) adaptable;
			}
		}
		return adapterObject;
	}

	@Override
	public Object getAdapter(Object adaptable, String adapterType) {
		Assert.isNotNull(adaptable);
		Assert.isNotNull(adapterType);
		return getAdapter(adaptable, adapterType, false);
	}

	/**
	 * Returns an adapter of the given type for the provided adapter.
	 * 
	 * @param adaptable   the object to adapt
	 * @param adapterType the type to adapt the object to
	 * @param force       <code>true</code> if the plug-in providing the factory
	 *                    should be activated if necessary. <code>false</code> if no
	 *                    plugin activations are desired.
	 */
	private Object getAdapter(Object adaptable, String adapterType, boolean force) {
		Assert.isNotNull(adaptable);
		Assert.isNotNull(adapterType);
		return getFactories(adaptable.getClass()).getOrDefault(adapterType, Collections.emptyList()) //
				.stream() //
				.map(factory -> force && factory instanceof IAdapterFactoryExt
						? ((IAdapterFactoryExt) factory).loadFactory(true)
						: factory) //
				.filter(Objects::nonNull).map(factory -> {
					Class<?> adapterClass = classForName(factory, adapterType);
					if (adapterClass == null) {
						return null;
					}
					return factory.getAdapter(adaptable, adapterClass); //
				}).filter(Objects::nonNull) //
				.findFirst() //
				.map(Object.class::cast) // casting to object seems necessary here; compiler issue?
				.orElseGet(() -> adapterType.equals(adaptable.getClass().getName()) ? adaptable : null);
	}

	@Override
	public boolean hasAdapter(Object adaptable, String adapterTypeName) {
		return getFactories(adaptable.getClass()).get(adapterTypeName) != null;
	}

	@Override
	public int queryAdapter(Object adaptable, String adapterTypeName) {
		List<IAdapterFactory> eligibleFactories = getFactories(adaptable.getClass()).get(adapterTypeName);
		if (eligibleFactories == null || eligibleFactories.isEmpty()) {
			return NONE;
		}
		if (eligibleFactories.stream().anyMatch(AdapterManager::isFactoryLoaded)) {
			return LOADED;
		}
		return NOT_LOADED;
	}

	@Override
	public Object loadAdapter(Object adaptable, String adapterTypeName) {
		return getAdapter(adaptable, adapterTypeName, true);
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	@Override
	public void registerAdapters(IAdapterFactory factory, Class<?> adaptable) {
		registerFactory(factory, adaptable.getName());
		flushLookup();
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	public void registerFactory(IAdapterFactory factory, String adaptableType) {
		factories.computeIfAbsent(adaptableType, any -> new CopyOnWriteArrayList<>()).add(factory);
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
	public void unregisterAdapters(IAdapterFactory factory) {
		for (List<IAdapterFactory> list : factories.values()) {
			if (list.remove(factory)) {
				flushLookup();
			}
		}
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
	public void unregisterAdapters(IAdapterFactory factory, Class<?> adaptable) {
		List<IAdapterFactory> factoryList = factories.get(adaptable.getName());
		if (factoryList == null || factoryList.isEmpty()) {
			return;
		}
		if (factoryList.remove(factory)) {
			flushLookup();
		}
	}

	/*
	 * Shuts down the adapter manager by removing all factories and removing the
	 * registry change listener. Should only be invoked during platform shutdown.
	 */
	public void unregisterAllAdapters() {
		lazyFactoryProviders.clear();
		factories.clear();
		flushLookup();
	}

	public void registerLazyFactoryProvider(IAdapterManagerProvider factoryProvider) {
		lazyFactoryProviders.add(factoryProvider);
	}

	public boolean unregisterLazyFactoryProvider(IAdapterManagerProvider factoryProvider) {
		return lazyFactoryProviders.remove(factoryProvider);
	}

	public Map<String, List<IAdapterFactory>> getFactories() {
		IAdapterManagerProvider provider;
		while ((provider = lazyFactoryProviders.poll()) != null) {
			if (provider.addFactories(this)) {
				flushLookup();
			}
		}
		return factories;
	}

	/**
	 * Try to load the given factory according to the force parameter
	 * 
	 * @param factory the factory to load
	 * @param force   if loading should be forced
	 * @return an {@link Optional} describing the loaded factory
	 */
	private static Optional<IAdapterFactory> loadFactory(IAdapterFactory factory, boolean force) {
		if (factory instanceof IAdapterFactoryExt) {
			return Optional.ofNullable(((IAdapterFactoryExt) factory).loadFactory(force));
		}
		return Optional.ofNullable(factory);
	}

	private static final class AdapterLookup {

		private final Class<?> adaptable;
		private final AdapterManager manager;
		private Map<String, List<IAdapterFactory>> map;

		AdapterLookup(Class<?> adaptable, AdapterManager manager) {
			this.adaptable = adaptable;
			this.manager = manager;
		}

		synchronized Map<String, List<IAdapterFactory>> getMap() {
			if (map == null) {
				// calculate adapters for the class
				Map<String, List<IAdapterFactory>> table = new HashMap<>(4);
				for (Class<?> cl : manager.computeClassOrder(adaptable)) {
					manager.addFactoriesFor(cl.getName(), table);
				}
				map = Collections.unmodifiableMap(table);
			}
			return map;
		}
	}
}
