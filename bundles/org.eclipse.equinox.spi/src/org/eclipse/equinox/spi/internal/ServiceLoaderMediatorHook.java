/*******************************************************************************
 * Copyright (c) 2023, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.spi.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.serviceloader.ServiceLoaderNamespace;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ServiceLoaderMediatorHook extends ClassLoaderHook implements BundleTrackerCustomizer<Bundle> {

	static final String SERVICE_NAME_PREFIX = "META-INF/services/"; //$NON-NLS-1$

	static String serviceNameFromPath(String path) {
		int lastSeparator = path.lastIndexOf('/');
		if (lastSeparator + 1 != SERVICE_NAME_PREFIX.length()) {
			return null; // Not a service provider-configuration file in services directory
		}
		return path.substring(lastSeparator + 1);
	}

	private static final Class<?> SERVICE_LOADER_GET_RESOURCES_CALLER;
	private static final Class<?> SERVICE_LOADER_LOAD_CLASS_CALLERS;

	static {
		ClassLoader classLoader = ServiceLoaderMediatorHook.class.getClassLoader();
		URL tracingDummy = classLoader.getResource(SERVICE_NAME_PREFIX + ServiceLoaderMediatorHook.class.getName());
		class ResoruceTrackingClassLoader extends ClassLoader {
			Optional<Class<?>> resourcesCaller;
			Optional<Class<?>> classCaller;

			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				resourcesCaller = Callers.walk(callerClasses -> callerClasses
						.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
						.findFirst());
				return Collections.enumeration(Arrays.asList(tracingDummy));
			}

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if ("org.eclipse.equinox.spi.caller.tracing.Dummy".equals(name)) { //$NON-NLS-1$
					classCaller = Callers.walk(callerClasses -> callerClasses
							.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
							.findFirst());
				}
				throw new ClassNotFoundException();
			}
		}
		ResoruceTrackingClassLoader tracker = new ResoruceTrackingClassLoader();
		try {
			ServiceLoader.load(ServiceLoaderMediatorHook.class, tracker).findFirst();
		} catch (ServiceConfigurationError e) { // expected
		}
		SERVICE_LOADER_GET_RESOURCES_CALLER = tracker.resourcesCaller.get();
		SERVICE_LOADER_LOAD_CLASS_CALLERS = tracker.classCaller.get();
	}

	private static final Logger LOGGER = Logger.getLogger(ServiceLoaderMediatorHook.class.getName());
	// TODO: Or just use System.out with a flag? to reduce requirements?

	private static final int SERVICES_ACTIVE_STATES = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE
			| Bundle.STOPPING;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	// --- tracking of service loader related bundles ---

	private volatile BundleTracker<Bundle> serviceBundleTracker;
	private final Map<Bundle, BundleServices> trackedBundles = new ConcurrentHashMap<>();
	private final Map<String, Set<BundleServices>> allServiceTypes = new HashMap<>();
	private final Map<String, Set<BundleServices>> allProvidedServices = new HashMap<>();

	void start(BundleContext systemBundleContext) {
		try (var locked = lock(lock.writeLock())) {
			serviceBundleTracker = new BundleTracker<>(systemBundleContext, SERVICES_ACTIVE_STATES, this);
			serviceBundleTracker.open();
		}
	}

	void stop() {
		try (var locked = lock(lock.writeLock())) {
			this.serviceBundleTracker.close();
			this.serviceBundleTracker = null;
			this.trackedBundles.clear();
			this.allServiceTypes.clear();
			this.allProvidedServices.clear();
		}
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		BundleServices services = BundleServices.of(bundle);
		if (services == null) {
			return null;
		}
		try (var locked = lock(lock.writeLock())) {
			trackedBundles.put(bundle, services);
			Map<String, List<String>> providedServices = services.providedServices();
			if (!providedServices.isEmpty()) {
				providedServices.forEach((serviceType, providers) -> {
					allServiceTypes.computeIfAbsent(serviceType, s -> createIdentityHashSet(3)).add(services);
					for (String providerClass : providers) {
						// A provider can implement multiple service types
						allProvidedServices.computeIfAbsent(providerClass, p -> createIdentityHashSet(3)).add(services);
					}
				});
				if (LOGGER.isLoggable(Level.FINE)) {
					providedServices.forEach((serviceType, providers) -> {
						LOGGER.fine("Registered services providers for service '" + serviceType + "' (in bundle " //$NON-NLS-1$ //$NON-NLS-2$
								+ bundle + "): " + providers); //$NON-NLS-1$
					});
				}
			}
		}
		return bundle;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle bundle2) {
		try (var locked = lock(lock.writeLock())) {
			BundleServices bundleServices = trackedBundles.remove(bundle);
			BiFunction<String, Set<BundleServices>, Set<BundleServices>> removeBundle = (s, bundles) -> {
				bundles.remove(bundleServices);
				return bundles.isEmpty() ? null : bundles;
			};
			bundleServices.providedServices().forEach((serviceType, providers) -> {
				allServiceTypes.computeIfPresent(serviceType, removeBundle);
				for (String providerClass : providers) {
					allProvidedServices.computeIfPresent(providerClass, removeBundle);
				}
			});
			for (ServiceRegistration<?> registration : bundleServices.registeredServices()) {
				registration.unregister();
			}
			if (LOGGER.isLoggable(Level.FINE)) {
				bundleServices.providedServices().forEach((serviceType, providers) -> {
					LOGGER.fine("Unregistered services providers for service '" + serviceType + "' (in bundle " + bundle //$NON-NLS-1$ //$NON-NLS-2$
							+ "): " + providers); //$NON-NLS-1$
				});
			}
		}
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle bundle2) {
		try (var locked = lock(lock.writeLock())) {
			// The transition from resolved to starting is relevant to perform the
			// registration as OSGi service as part of the re-registration.
			// Likewise the transition from active to resolved in a stop process is relevant
			// to unregister the OSGi services.

			switch (event.getType()) {
			// TODO: LAZY_ACTIVATION??
			case BundleEvent.STARTING -> {
				BundleServices services = trackedBundles.get(bundle);
				if (services != null) {
					services.registerOSGiServices(bundle.getBundleContext());
				}
			}
			case BundleEvent.STOPPING -> {
				BundleServices services = trackedBundles.get(bundle);
				if (services != null) {
					services.unregisterOSGiServices();
				}
			}
			case BundleEvent.STARTED, BundleEvent.STOPPED -> {
				// The completion of the start and stop process is irrelevant.
			}
			default -> {
				removedBundle(bundle, event, bundle2);
				addingBundle(bundle, event);
			}
			}
		}
	}

	@Override
	public boolean isProcessClassRecursionSupported() {
		return true;
	}

	// --- interception and handling of service loader calls ---

	@Override
	public Enumeration<URL> preFindResources(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		if (name.startsWith(SERVICE_NAME_PREFIX)) {
			String serviceName = serviceNameFromPath(name);
			if (serviceName != null) {
				if (!isConsumer(classLoader.getBundle())) {
					return null;
				}
				try (var locked = lock(lock.readLock())) {
					Set<BundleServices> services = allServiceTypes.get(serviceName);
					if (services != null
							&& Callers.walk(s -> s.anyMatch(c -> c == SERVICE_LOADER_GET_RESOURCES_CALLER))) {
						List<URL> configFiles = providerClassLoaders(classLoader, services, Set.of(serviceName))
								.flatMap(cl -> {
									try {
										return cl.resources(name);
									} catch (UncheckedIOException e) {
										return Stream.empty();
									}
								}).toList();
						return Collections.enumeration(configFiles);
					}
				}
			}
		}
		return null;
	}

	boolean isConsumer(Bundle bundle) {
		BundleServices bundleServices = trackedBundles.get(bundle);
		return bundleServices != null && bundleServices.isProcessedConsumer();
	}

	@Override
	public Class<?> preFindClass(String classname, ModuleClassLoader classLoader) throws ClassNotFoundException {
		Set<String> potentialServiceTypes = new HashSet<>(3);
		if (!isConsumer(classLoader.getBundle())) {
			return null;
		}
		try (var locked = lock(lock.readLock())) {
			Set<BundleServices> services = allProvidedServices.get(classname); // set is mutable
			if (services != null) {
				for (BundleServices bundleServices : services) {
					bundleServices.providedServices().forEach((type, providers) -> {
						if (providers.contains(classname)) {
							potentialServiceTypes.add(type);
						}
					});
				}
				if (Callers.walk(s -> s.takeWhile(c -> c != BundleServices.class)
						.anyMatch(c -> c == SERVICE_LOADER_LOAD_CLASS_CALLERS))) {
					// If a service loader call reached this state, everything should be fine.
					// No need to check again if the callers consumes the service type.
					return providerClassLoaders(classLoader, services, potentialServiceTypes).map(cl -> {
						try {
							return cl.loadClass(classname);
						} catch (ClassNotFoundException | NoClassDefFoundError e) { // ignore
							return null;
						}
					}).filter(Objects::nonNull).findFirst().orElse(null);
				}
			}
		}
		return null;
	}

	private static Stream<ClassLoader> providerClassLoaders(ModuleClassLoader classLoader, Set<BundleServices> services,
			Set<String> serviceNames) {

		Stream<BundleWiring> providerWirings = services.stream().map(BundleServices::bundle).map(b -> {
			BundleRevision bundleRevision = b.adapt(BundleRevision.class);
			return BundleServices.getHostingWiring(bundleRevision);
		});

		// See chapter 133.3 -- Consumers
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#d0e80411

		BundleWiring wiring = classLoader.getBundle().adapt(BundleWiring.class); // never a fragment
		List<BundleWire> requiredWires = wiring.getRequiredWires(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);
		if (requiredWires != null) {
			// Consider section 133.3.3 Restricting Visibility and only consider providers
			// wired to the consumer via the required capability or all if the consumer
			// doesn't have the visibility restricted
			Stream<BundleWire> wires = requiredWires.stream();
			if (serviceNames != null) {
				wires = wires.filter(wire -> {
					Map<String, Object> providerAttributes = wire.getCapability().getAttributes();
					Object publishedService = providerAttributes.get(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);
					return serviceNames.contains(publishedService);
				});
			}
			Set<BundleWiring> wiredWirings = wires.map(BundleWire::getProviderWiring).collect(Collectors.toSet());
			// the wired wirings are always hosts (never from fragments)
			providerWirings = providerWirings.filter(wiredWirings::contains);
		}
		return Stream.concat(Stream.of(classLoader), providerWirings.map(BundleWiring::getClassLoader));
	}

	private interface RuntimeCloseable extends AutoCloseable {
		@Override
		void close();
	}

	private RuntimeCloseable lock(Lock lock) {
		lock.lock();
		return lock::unlock;
	}

	private static <T> Set<T> createIdentityHashSet(int expectedMaxSize) {
		return Collections.newSetFromMap(new IdentityHashMap<>(expectedMaxSize));
	}

}
