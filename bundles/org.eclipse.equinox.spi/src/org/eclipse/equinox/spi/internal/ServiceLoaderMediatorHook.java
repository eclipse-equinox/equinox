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

import static org.eclipse.equinox.spi.internal.Utilities.setOf;
import static org.eclipse.equinox.spi.internal.Utilities.walkCallerClasses;

import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.HostNamespace;
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
				resourcesCaller = walkCallerClasses(callerClasses -> callerClasses
						.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
						.findFirst());
				return Collections.enumeration(Arrays.asList(tracingDummy));
			}

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if ("org.eclipse.equinox.spi.caller.tracing.Dummy".equals(name)) { //$NON-NLS-1$
					classCaller = walkCallerClasses(callerClasses -> callerClasses
							.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
							.findFirst());
				}
				throw new ClassNotFoundException();
			}
		}
		ResoruceTrackingClassLoader tracker = new ResoruceTrackingClassLoader();
		try {
			ServiceLoader.load(ServiceLoaderMediatorHook.class, tracker).iterator().hasNext();
		} catch (ServiceConfigurationError e) { // expected
		}
		SERVICE_LOADER_GET_RESOURCES_CALLER = tracker.resourcesCaller.get();
		SERVICE_LOADER_LOAD_CLASS_CALLERS = tracker.classCaller.get();
	}

	private static final int SERVICES_ACTIVE_STATES = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE
			| Bundle.STOPPING;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	// --- tracking of service loader related bundles ---

	private final BundleTracker<Bundle> serviceBundleTracker;
	private final Map<Bundle, BundleServices> trackedBundles = new ConcurrentHashMap<>();
	private final Map<String, Set<BundleServices>> allServiceTypes = new HashMap<>();
	private final Map<String, Set<BundleServices>> allProvidedServices = new HashMap<>();
	private final Set<Bundle> stoppedProviders = ConcurrentHashMap.newKeySet();
	private final ServiceLoaderMediatorHookConfigurator.Tracing tracing;

	public ServiceLoaderMediatorHook(BundleContext systemBundleContext,
			ServiceLoaderMediatorHookConfigurator.Tracing tracing) {
		this.tracing = tracing;
		serviceBundleTracker = new BundleTracker<>(systemBundleContext, SERVICES_ACTIVE_STATES, this);
		serviceBundleTracker.open();
	}

	void stop() {
		try (RuntimeCloseable locked = lock(lock.writeLock())) {
			this.serviceBundleTracker.close();
			this.trackedBundles.clear();
			this.allServiceTypes.clear();
			this.allProvidedServices.clear();
			this.stoppedProviders.clear();
		}
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
		BundleServices services = BundleServices.of(bundleRevision,
				// A host bundle without own service providers/consumers still needs to be
				// tracked if it has an attached fragment providing OSGi services: only the
				// host's own life-cycle events (STARTING/STOPPING) can trigger the
				// (un)registration of the fragment's published OSGi services.
				() -> bundleAndFragmentsServices(bundle).findAny().isPresent());
		if (services == null) {
			return null;
		}
		try (RuntimeCloseable locked = lock(lock.writeLock())) {
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
				tracing.debugRegistrations("Registered", providedServices, bundle); //$NON-NLS-1$
			}
			if (BundleServices.isFragment(bundleRevision)) {
				BundleWiring hostingWiring = BundleServices.getHostingWiring(bundleRevision);
				if (hostingWiring != null) {
					Bundle hostBundle = hostingWiring.getBundle();
					if (!trackedBundles.containsKey(hostBundle)) {
						addingBundle(hostBundle, null);
					}
				}
			}
			// An 'empty' bundle is tracked, only if any of its fragments provides services.
			boolean hasProvidingFragments = services.isEmpty();
			if (hasProvidingFragments) {
				// This host bundle is tracked for the first time: if it is already
				// starting/active, perform the registration that a STARTING event would
				// otherwise have triggered (that event won't be re-delivered, since this
				// bundle wasn't tracked yet when it occurred).
				BundleContext bundleContext = bundle.getBundleContext();
				if (bundleContext != null) {
					bundleAndFragmentsServices(bundle).forEach(s -> s.registerOSGiServices(bundleContext));
				}
			}
		}
		return bundle;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle trackedBundle) {
		try (RuntimeCloseable locked = lock(lock.writeLock())) {
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
			stoppedProviders.remove(bundle);

			tracing.debugRegistrations("Unregistered", bundleServices.providedServices(), bundle); //$NON-NLS-1$
		}
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle trackedBundle) {
		try (RuntimeCloseable locked = lock(lock.writeLock())) {
			// The transition from resolved to starting is relevant to perform the
			// registration as OSGi service as part of the re-registration.
			// Likewise the transition from active to resolved in a stop process is relevant
			// to unregister the OSGi services.
			switch (event.getType()) {
			// A bundle awaiting its lazy activation trigger enters STARTING and fires
			// BundleEvent.LAZY_ACTIVATION instead of BundleEvent.STARTING, but must
			// nonetheless have its Service Providers registered without waiting for the
			// trigger class load, see the Service Loader Mediator specification
			// clarification proposed in https://github.com/osgi/osgi/pull/973
			case BundleEvent.STARTING:
			case BundleEvent.LAZY_ACTIVATION:
				stoppedProviders.remove(bundle);
				BundleContext bundleContext = bundle.getBundleContext();
				bundleAndFragmentsServices(bundle).forEach(s -> s.registerOSGiServices(bundleContext));
				break;
			case BundleEvent.STOPPING:
				stoppedProviders.add(bundle);
				bundleAndFragmentsServices(bundle).forEach(BundleServices::unregisterOSGiServices);
				break;
			case BundleEvent.STARTED:
			case BundleEvent.STOPPED:
				// The completion of the start and stop process is irrelevant.
			case BundleEvent.RESOLVED: // An update of the wiring is irrelevant.
				break;
			case BundleEvent.UPDATED:
				removedBundle(bundle, event, trackedBundle);
				addingBundle(bundle, event);
				break;
			default:
				throw new IllegalStateException("Unsupported event type " + event.getType() + " for bundle " + bundle); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private Stream<BundleServices> bundleAndFragmentsServices(Bundle bundle) {
		return bundleAndFragments(bundle).map(trackedBundles::get).filter(Objects::nonNull);
	}

	private static Stream<Bundle> bundleAndFragments(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> providedHostWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		if (providedHostWires == null) {
			return Stream.of(bundle);
		}
		return Stream.concat(Stream.of(bundle),
				providedHostWires.stream().map(w -> w.getRequirer().getBundle()).filter(Objects::nonNull));
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
				try (RuntimeCloseable locked = lock(lock.readLock())) {
					Set<BundleServices> services = allServiceTypes.get(serviceName);
					if (services != null
							&& walkCallerClasses(s -> s.anyMatch(c -> c == SERVICE_LOADER_GET_RESOURCES_CALLER))) {
						List<URL> configFiles = providerHostBundle(classLoader, services, setOf(serviceName))
								.flatMap(ServiceLoaderMediatorHook::bundleAndFragments).map(b -> {
									// Search service files (in that bundle and its fragments) without using the
									// bundle's classloader to prevent recursive invocations of this hook
									// (which should be handled, but have a overhead).
									try {
										return b.getEntry(name);
									} catch (IllegalStateException e) { // Happens if bundle is uninstalled concurrently
										return null; // -> ignore as provider
									}
								}).filter(Objects::nonNull).collect(Collectors.toList());
						return Collections.enumeration(configFiles);
					}
				}
			}
		}
		return null;
	}

	private boolean isConsumer(Bundle bundle) {
		BundleServices bundleServices = trackedBundles.get(bundle);
		return bundleServices != null && bundleServices.isProcessedConsumer();
	}

	@Override
	public Class<?> preFindClass(String classname, ModuleClassLoader classLoader) throws ClassNotFoundException {
		Set<String> potentialServiceTypes = new HashSet<>(3);
		if (!isConsumer(classLoader.getBundle())) {
			return null;
		}
		List<Bundle> providerBundles = List.of();
		try (RuntimeCloseable locked = lock(lock.readLock())) {
			Set<BundleServices> services = allProvidedServices.get(classname); // set is mutable
			if (services != null) {
				for (BundleServices bundleServices : services) {
					bundleServices.providedServices().forEach((type, providers) -> {
						if (providers.contains(classname)) {
							potentialServiceTypes.add(type);
						}
					});
				}
				if (walkCallerClasses(s -> s.anyMatch(c -> c == SERVICE_LOADER_LOAD_CLASS_CALLERS))) {
					// If a service loader call reached this state, everything should be fine.
					// No need to check again if the callers consumes the service type.
					providerBundles = providerHostBundle(classLoader, services, potentialServiceTypes)
							.collect(Collectors.toList());
				}
			}
		}
		// Load outside of the lock: loading may lazily start a bundle, which fires
		// events that require the write lock (cannot be upgraded from the read lock).
		for (Bundle bundle : providerBundles) {
			try {
				Class<?> clazz = bundle.loadClass(classname);
				if (clazz != null) {
					return clazz;
				}
			} catch (ClassNotFoundException | NoClassDefFoundError | IllegalStateException e) { // ignore
			}
		}
		return null;
	}

	private Stream<Bundle> providerHostBundle(ModuleClassLoader classLoader, Set<BundleServices> services,
			Set<String> serviceNames) {

		Stream<BundleWiring> providerHostWirings = services.stream().map(BundleServices::bundle).map(b -> {
			BundleRevision bundleRevision = b.adapt(BundleRevision.class);
			return BundleServices.getHostingWiring(bundleRevision);
		}).filter(Objects::nonNull).filter(hostingWiring -> !stoppedProviders.contains(hostingWiring.getBundle()));
		// For host bundles that were started and have since been stopped again (without
		// being uninstalled) provided services must no longer be found via
		// ServiceLoader, see chapter 133.3.4 Life Cycle Impedance Mismatch.

		// See chapter 133.3 -- Consumers
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#d0e80411

		Bundle bundle = classLoader.getBundle();
		BundleWiring wiring = bundle.adapt(BundleWiring.class); // never a fragment
		List<BundleWire> requiredWires = wiring.getRequiredWires(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);
		if (requiredWires != null && !requiredWires.isEmpty()) {
			// Consider section 133.3.3 Restricting Visibility and only consider providers
			// wired to the consumer via the required capability or all if the consumer
			// doesn't have the visibility restricted (i.e. has no requirements)
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
			providerHostWirings = providerHostWirings.filter(wiredWirings::contains);
		}
		return Stream.concat(Stream.of(bundle), providerHostWirings.map(BundleWiring::getBundle)).distinct();
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
