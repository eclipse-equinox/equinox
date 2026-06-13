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

package org.eclipse.equinox.internal.spi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

public class ServiceLoaderMediatorHook extends ClassLoaderHook {
	// TODO: Extend the class that writes the config.ini to automatically add this
	// framework extension when it is installed?! Check if this is then used in
	// Tycho and PDE.
	// Respectively add first-class support for adding framework extensions to the
	// product, like for auto-started bundles

	private static final String SERVICE_NAME_PREFIX = "META-INF/services/"; //$NON-NLS-1$

	private static final Class<?> SERVICE_LOADER_GET_RESOURCES_CALLER;
	private static final Class<?> SERVICE_LOADER_LOAD_CLASS_CALLERS;

	static {
		ClassLoader classLoader = ServiceLoaderMediatorHook.class.getClassLoader();
		URL tracingDummy = classLoader.getResource(SERVICE_NAME_PREFIX + ServiceLoaderMediatorHook.class.getName());
		@SuppressWarnings("unchecked")
		Optional<Class<?>>[] resourcesCallers = new Optional[1];
		@SuppressWarnings("unchecked")
		Optional<Class<?>>[] classCallers = new Optional[1];
		try {
			ClassLoader tracker = new ClassLoader() {
				@Override
				public Enumeration<URL> getResources(String name) throws IOException {
					resourcesCallers[0] = Callers.walk(callerClasses -> callerClasses
							.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
							.findFirst());
					return Collections.enumeration(Arrays.asList(tracingDummy));
				}

				@Override
				public Class<?> loadClass(String name) throws ClassNotFoundException {
					if ("a.caller.tracing.Dummy".equals(name)) { //$NON-NLS-1$
						classCallers[0] = Callers.walk(callerClasses -> callerClasses
								.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class)
								.findFirst());
					}
					throw new ClassNotFoundException();
				}
			};
			ServiceLoader.load(ServiceLoaderMediatorHook.class, tracker).findFirst();
		} catch (ServiceConfigurationError e) { // expected
		}
		SERVICE_LOADER_GET_RESOURCES_CALLER = resourcesCallers[0].get();
		SERVICE_LOADER_LOAD_CLASS_CALLERS = classCallers[0].get();
	}

	private static final String OSGI_SERVICELOADER_NAMESPACE = "osgi.serviceloader"; //$NON-NLS-1$

	private static final class BundleServices {
		private static final Pattern SERVICE_LOADER_FILTER = Pattern
				.compile("\\(" + OSGI_SERVICELOADER_NAMESPACE + "=([\\w\\$\\.]+)\\)"); //$NON-NLS-1$ //$NON-NLS-2$
		private static final int SERVICE_NAME = 1;

		final Set<String> consumedServices;
		final Map<String, List<String>> providedServices;

		public BundleServices(Bundle bundle, BundleWiring wiring) {
			this.consumedServices = getConsumedServices(wiring);
			this.providedServices = getProvidedServices(bundle, wiring);
		}

		private static Set<String> getConsumedServices(BundleWiring wiring) {
			// TODO: Or just check if wired to this fragment's host for the
			// registrar/processor
			if (hasOSGiExtenderRequirement(wiring, "(osgi.extender=osgi.serviceloader.processor)")) { //$NON-NLS-1$
				// TODO: Or operate on the resource's requirements to also consider those that
				// are not wired? But if not wired, it means there's no provider anyways and we
				// can't do anything...
				return wiring.getRequirements(OSGI_SERVICELOADER_NAMESPACE).stream()
						.map(r -> r.getDirectives().getOrDefault(Constants.FILTER_DIRECTIVE, "")) //$NON-NLS-1$
						.map(SERVICE_LOADER_FILTER::matcher).filter(Matcher::matches).map(m -> m.group(SERVICE_NAME))
						.filter(BundleServices::isFullyQualifiedJavaClassName).collect(Collectors.toSet());
			}
			return Collections.emptySet();
		}

		private static Map<String, List<String>> getProvidedServices(Bundle bundle, BundleWiring wiring) {
			// TODO: Or just check if wired to this fragment's host for the
			// registrar/processor
			if (hasOSGiExtenderRequirement(wiring, "(osgi.extender=osgi.serviceloader.registrar)")) { //$NON-NLS-1$
				Map<String, List<String>> provided = new HashMap<>();
				// TODO: how to treat fragments. They should be considered as part of the host?
				// But arn't they already considered in the wiring?
				// If lists are merged, handle fragment removal. Or merge on demand to simplify
				// tracking?
				Enumeration<String> serviceProviderEntries = bundle.getEntryPaths(SERVICE_NAME_PREFIX);
				while (serviceProviderEntries.hasMoreElements()) {
					String path = serviceProviderEntries.nextElement();
					int lastSeparator = path.lastIndexOf('/');
					if (lastSeparator + 1 != SERVICE_NAME_PREFIX.length()) {
						continue;
					}
					String service = path.substring(lastSeparator + 1);
					// TODO: Check JDT's issue with this?!
					URL entry = (URL) (Object) bundle.getEntry(path);

					try (InputStream is = entry.openStream();
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(is, StandardCharsets.UTF_8));) {
						List<String> providedServices = provided.computeIfAbsent(service, n -> new ArrayList<>());
						reader.lines().map(BundleServices::getServiceImplementorName).filter(n -> !n.isEmpty())
						.forEach(providedServices::add);
					} catch (IOException e) { // TODO: ignore, log?
					}

					//					try {
					//						ClassLoader bundleClassLoader = wiring.getClassLoader();
					//						Class<?> serviceClass = bundleClassLoader.loadClass(service);
					//						List<Class<?>> collect = ServiceLoader.load(serviceClass, bundleClassLoader).stream()
					//								.map(Provider::type).collect(Collectors.toList());
					//						collect.stream().map(Object::getClass).map(Class::getName).collect(Collectors.toList());
					//					} catch (Exception e) { // TODO: Handle? Rethrow? Log?
					//						e.printStackTrace();
					//					}
				}
				provided.values().removeIf(List::isEmpty); // Service file might have been empty
				return provided;
			}
			return Collections.emptyMap();
		}

		private static boolean hasOSGiExtenderRequirement(BundleWiring wiring, String filter) {
			List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.extender"); //$NON-NLS-1$
			if (requiredWires.isEmpty()) {
				return false;
			}
			return requiredWires.stream().anyMatch(w -> {
				Map<String, String> directives = w.getRequirement().getDirectives();
				return directives.getOrDefault(Constants.FILTER_DIRECTIVE, "").contains(filter) //$NON-NLS-1$
						// TODO: Why system-bundle? to check this framework-extension SPI is used?
						&& w.getProvider().getBundle().getBundleId() == Constants.SYSTEM_BUNDLE_ID;
			});
		}

		private static String getServiceImplementorName(String providerName) {
			int commentIndex = providerName.indexOf('#');
			if (commentIndex >= 0) {
				providerName = providerName.substring(0, commentIndex);
			}
			return providerName.trim();
		}

		private static boolean isFullyQualifiedJavaClassName(String service) {
			return Character.isJavaIdentifierStart(service.codePointAt(0)) && IntStream.range(1, service.length())
					.allMatch(i -> Character.isJavaIdentifierPart(service.codePointAt(i)) || service.charAt(i) == '.');
		}
	}

	private static class ServiceBundleTracker extends BundleTracker<BundleServices> {
		private static final int SERVICES_ACTIVE_STATES = Bundle.RESOLVED | Bundle.STARTING
				| Bundle.START_ACTIVATION_POLICY | Bundle.ACTIVE | Bundle.STOPPING | Bundle.START_TRANSIENT
				| Bundle.STOP_TRANSIENT;

		// TODO: Check consistency among all implementations of any service in the
		// runtime, e.g. that multiple versions of one class implement the same set of
		// services. Of course this could change over time, but would also cause
		// problems in a flat classpath.
		final Map<String, String> allProvidedServices = new ConcurrentHashMap<>();

		public ServiceBundleTracker(BundleContext context) {
			super(context, SERVICES_ACTIVE_STATES, null);
		}

		@Override
		public BundleServices addingBundle(Bundle bundle, BundleEvent event) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring != null) {
				BundleServices services = new BundleServices(bundle, wiring);
				if (!services.consumedServices.isEmpty() || !services.providedServices.isEmpty()) {
					services.providedServices.forEach((service, providers) -> {
						for (String serviceClassname : providers) {
							if (allProvidedServices.putIfAbsent(serviceClassname, service) != null) {
								// TODO: or handle this? Could e.g. happen with multiple versions of a bundle
								// installed or if two bundles provide the same service impl name for any reason
								throw new IllegalStateException("Service provider class available more than once:" //$NON-NLS-1$
										+ serviceClassname);
							}
						}
					});
					return services;
				}
			}
			return null; // Not interesting, don't track
		}

		@Override
		public void modifiedBundle(Bundle bundle, BundleEvent event, BundleServices object) {
			// TODO Just remove and add again?
			super.modifiedBundle(bundle, event, object);
		}

		@Override
		public void removedBundle(Bundle bundle, BundleEvent event, BundleServices services) {
			for (List<String> providers : services.providedServices.values()) {
				for (String classname : providers) {
					allProvidedServices.remove(classname);
				}
			}
		}

		boolean consumesService(Bundle bundle, String serviceName) {
			BundleServices bundleServices = getObject(bundle);
			return bundleServices != null && bundleServices.consumedServices.contains(serviceName);
		}
	}

	private volatile ServiceBundleTracker serviceBundleTracker;

	@Override
	public ModuleClassLoader createClassLoader(ClassLoader parent, EquinoxConfiguration configuration,
			BundleLoader delegate, Generation generation) {
		if (serviceBundleTracker == null) {
			synchronized (this) {
				if (serviceBundleTracker == null) {
					Bundle aBundle = delegate.getWiring().getBundle();
					Bundle systemBundle = aBundle.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_ID);
					BundleContext systemBundleContext = systemBundle.getBundleContext();
					ServiceBundleTracker tracker = new ServiceBundleTracker(systemBundleContext);
					// TODO: what is suitable if the framework is restarted. Is this called again?
					systemBundleContext.addBundleListener(event -> {
						if (event.getType() == Framework.STOPPING) {
							tracker.close();
						}
					});
					systemBundleContext.addFrameworkListener(event -> {
						if (event.getType() == FrameworkEvent.STOPPED) {
							tracker.close();
						}
					});
					tracker.open();
					serviceBundleTracker = tracker;
				}
			}
		}
		return null;
	}

	@Override
	public Enumeration<URL> preFindResources(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		if (name.startsWith(SERVICE_NAME_PREFIX)) {
			String serviceName = name.substring(SERVICE_NAME_PREFIX.length());
			Bundle bundle = classLoader.getBundle();
			if (serviceBundleTracker.consumesService(bundle, serviceName)
					&& Callers.walk(s -> s.anyMatch(c -> c == SERVICE_LOADER_GET_RESOURCES_CALLER))) {
				List<URL> services = searchProviderClassLoaders(serviceName, bundle, classLoader).flatMap(cl -> {
					try {
						Enumeration<URL> resources = cl.getResources(name);
						return resources.hasMoreElements() ? Collections.list(resources).stream() : Stream.empty();
					} catch (IOException e) {
						return Stream.empty();
					}
				}).collect(Collectors.toList());
				return Collections.enumeration(services);
			}
		}
		return null;
	}

	@Override
	public Class<?> preFindClass(String name, ModuleClassLoader classLoader) throws ClassNotFoundException {
		String serviceName = serviceBundleTracker.allProvidedServices.get(name);
		if (serviceName != null) {
			Bundle bundle = classLoader.getBundle();
			if (serviceBundleTracker.consumesService(bundle, serviceName)
					&& Callers.walk(s -> s.anyMatch(c -> c == SERVICE_LOADER_LOAD_CLASS_CALLERS))) {
				// TODO: index and parse the found service files per bundle and only continue
				// here if the class to load is a registered service of the bundle.
				// IF possible use the ServiceLoader class itself to parse those files.
				// See LazyClassPath LazyClassPathLookupIterator.parseLine():
				// All non empty line and everything behind a # is a comment and ignored.
				// No need to check for the correct syntax. The ServiceLoader will do the
				// failure for us.
				return searchProviderClassLoaders(serviceName, bundle, classLoader).map(cl -> {
					try {
						return cl.loadClass(name);
					} catch (ClassNotFoundException | NoClassDefFoundError e) { // ignore
						return null;
					}
				}).filter(Objects::nonNull).findFirst().orElse(null);
			}
		}
		return null;
	}

	private Stream<ClassLoader> searchProviderClassLoaders(String serviceName, Bundle bundle,
			ModuleClassLoader classLoader) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		return Stream.concat(Stream.of(classLoader), wiredProviderClassLoaders(serviceName, wiring));
	}

	private static Stream<ClassLoader> wiredProviderClassLoaders(String serviceName, BundleWiring wiring) {
		List<BundleWire> requiredWires = wiring.getRequiredWires(OSGI_SERVICELOADER_NAMESPACE);
		if (!requiredWires.isEmpty()) {
			return requiredWires.stream().filter(wire -> {
				Object serviceLoaderAttribute = wire.getCapability().getAttributes().get(OSGI_SERVICELOADER_NAMESPACE);
				return serviceName.equals(serviceLoaderAttribute);
			}).map(BundleWire::getProviderWiring).map(BundleWiring::getClassLoader);

		}
		return Stream.empty();
	}

}
