/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
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

import static org.eclipse.equinox.spi.internal.ServiceLoaderMediatorHookConfigurator.spiExtensionBundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.serviceloader.ServiceLoaderNamespace;

record BundleServices(Bundle bundle, boolean isProcessedConsumer, Map<String, List<String>> providedServices,
		Map<String, List<ServicePublication>> publishedServices, List<ServiceRegistration<?>> registeredServices) {

	static BundleServices of(Bundle bundle) {
		BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
		if (bundleRevision != null) {
			BundleWiring hostingWiring = getHostingWiring(bundleRevision);

			boolean isProcessed = requiresServiceLoaderProcessor(hostingWiring);

			// See chapter 133.4 -- Service Provider Bundles
			// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#d0e80557

			Map<String, List<String>> provided = getAdvertisedServiceProviders(bundleRevision);
			Map<String, List<ServicePublication>> published = getPublishedServices(bundleRevision);
			provided.keySet().retainAll(published.keySet());

			if (!hasOSGiExtenderRequirement(hostingWiring, "(osgi.extender=osgi.serviceloader.registrar)")) { //$NON-NLS-1$
				published = Map.of(); // Skip registration as OSGi services
			}

			if (isProcessed || !provided.isEmpty()) {
				BundleServices services = new BundleServices(bundle, isProcessed, Map.copyOf(provided), published,
						new ArrayList<>());

				BundleContext hostContext = hostingWiring.getBundle().getBundleContext();
				if (hostContext != null) {
					services.registerOSGiServices(hostContext);
				}
				return services;
			}
		}
		return null;
	}

	private static boolean requiresServiceLoaderProcessor(BundleWiring wiring) {
		// See chapter 133.3.2 -- Opting In
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#d0e80429
		return hasOSGiExtenderRequirement(wiring, "(osgi.extender=osgi.serviceloader.processor)"); //$NON-NLS-1$
	}

	private static Map<String, List<ServicePublication>> getPublishedServices(BundleRevision resource) {
		List<Capability> capabilities = resource.getCapabilities(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);
		if (capabilities == null) {
			return Map.of();
		}
		return capabilities.stream().map(ServicePublication::from).collect(Collectors.groupingBy(c -> c.serviceType));
	}

	private static Map<String, List<String>> getAdvertisedServiceProviders(BundleRevision resource) {
		// See chapter 133.4.1 -- Advertising
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#d0e80542
		Map<String, List<String>> advertisedServiceProviders = new HashMap<>();
		Bundle bundle = resource.getBundle();
		Enumeration<String> providerConfigurationFiles = bundle
				.getEntryPaths(ServiceLoaderMediatorHook.SERVICE_NAME_PREFIX);
		if (providerConfigurationFiles == null) {
			return Map.of();
		}
		while (providerConfigurationFiles.hasMoreElements()) {
			String path = providerConfigurationFiles.nextElement();
			String service = ServiceLoaderMediatorHook.serviceNameFromPath(path);
			if (service == null) {
				continue; // Not a service provider-configuration file in services directory
			}
			List<String> serviceProviders = loadServiceFile(bundle, path);
			if (!serviceProviders.isEmpty() && advertisedServiceProviders.put(service, serviceProviders) != null) {
				// Should be impossible
				throw new IllegalStateException("Multiple configuration files found for service " + service); //$NON-NLS-1$
			}
		}
		return advertisedServiceProviders;
	}

	private static List<String> loadServiceFile(Bundle bundle, String path) {
		URL entry = bundle.getEntry(path);
		try (InputStream is = entry.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));) {
			return reader.lines().map(BundleServices::getServiceImplementorName).filter(n -> !n.isEmpty()).toList();
		} catch (IOException e) { // ignore
			return List.of();
		}
	}

	private static String getServiceImplementorName(String providerName) {
		// All non empty line and everything behind a # is a comment and ignored.
		// No need to check for the correct syntax. The ServiceLoader will do the
		// failure for us.
		int commentIndex = providerName.indexOf('#');
		if (commentIndex >= 0) {
			providerName = providerName.substring(0, commentIndex);
		}
		return providerName.trim();
	}

	private record ServicePublication(String serviceType, String register, Map<String, Object> serviceAttributes) {

		private static final Set<String> SPECIFIED_CAPABILITY_ATTRIBUTES = Set
				.of(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);

		// See chapters 133.4.3 -- OSGi Services and 133.5.2 -- OSGi Service Factory
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#i3285744

		static ServicePublication from(Capability capability) {
			Map<String, Object> attributes = capability.getAttributes();
			String serviceType = (String) attributes.get(ServiceLoaderNamespace.SERVICELOADER_NAMESPACE);
			String register = capability.getDirectives().get(ServiceLoaderNamespace.CAPABILITY_REGISTER_DIRECTIVE);
			attributes = new HashMap<>(attributes);
			attributes.keySet().removeIf(a -> a.startsWith(".") || SPECIFIED_CAPABILITY_ATTRIBUTES.contains(a)); //$NON-NLS-1$
			attributes.put("serviceloader.mediator", spiExtensionBundle.getBundleId()); //$NON-NLS-1$
			return new BundleServices.ServicePublication(serviceType, register, attributes);
		}
	}

	public void registerOSGiServices(BundleContext bundleContext) {
		// See chapter 133.5.1 -- Registering Services
		// https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.loader.html#i3280881
		publishedServices.forEach((serviceType, publications) -> {
			List<String> providerTypes = providedServices.getOrDefault(serviceType, List.of());
			for (ServicePublication publication : publications) {
				String register = publication.register();
				if (register != null) {
					providerTypes = providerTypes.contains(register) ? List.of(register) : List.of();
				}
				if (providerTypes.isEmpty()) {
					continue;
				}
				for (String providerType : providerTypes) {
					var registration = registerService(serviceType, providerType, publication, bundleContext);
					if (registration != null) {
						registeredServices.add(registration);
					}
				}
			}
		});
	}

	public void unregisterOSGiServices() {
		registeredServices().forEach(ServiceRegistration::unregister);
		registeredServices.clear();
	}
	// TODO: Register services of all fragments upon activation of the host

	/**
	 * @param bundleContext - a bundle's context of the BundleContext of it's host,
	 *                      if the bundle is a fragment.
	 */
	static ServiceRegistration<?> registerService(String serviceType, String providerType,
			ServicePublication publication, BundleContext bundleContext) {
		Class<?> serviceProviderType;
		try {
			serviceProviderType = bundleContext.getBundle().loadClass(providerType);
		} catch (Exception | NoClassDefFoundError e) {
			return null;
		}
		ServiceFactory<?> factory;
		// "The OSGi service factory must be implemented such that it creates a new
		// instance for each bundle that gets the service."
		if (Constants.SCOPE_PROTOTYPE
				.equalsIgnoreCase((String) publication.serviceAttributes().getOrDefault(Constants.SERVICE_SCOPE, ""))) { //$NON-NLS-1$
			factory = new PrototypeServiceFactory<>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					return createInstance(serviceProviderType);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
					// nothing to do
				}
			};
		} else {
			factory = new ServiceFactory<>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					return createInstance(serviceProviderType);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
					// nothing to do
				}
			};
		}
		return bundleContext.registerService(serviceType, factory,
				FrameworkUtil.asDictionary(publication.serviceAttributes()));
	}

	private static Object createInstance(Class<?> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to create instance of class " + cls); //$NON-NLS-1$
		}
	}

	private static boolean hasOSGiExtenderRequirement(BundleWiring wiring, String filter) {
		if (wiring == null) {
			return false;
		}
		List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.extender"); //$NON-NLS-1$
		if (requiredWires.isEmpty()) {
			return false;
		}
		return requiredWires.stream().anyMatch(w -> {
			Requirement requirement = w.getRequirement();
			// Ensure this Mediator extension is resolved as provider
			return getFilterDirective(requirement).contains(filter)
					&& w.getCapability().getResource().getBundle() == spiExtensionBundle;
		});
	}

	static BundleWiring getHostingWiring(BundleRevision bundleRevision) {
		BundleWiring wiring = bundleRevision.getWiring();
		if ((bundleRevision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			List<BundleWire> requiredWires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
			return requiredWires == null ? null : requiredWires.get(0).getProviderWiring();
		} else {
			return wiring;
		}
	}

	private static String getFilterDirective(Requirement r) {
		return r.getDirectives().getOrDefault(ServiceLoaderNamespace.REQUIREMENT_FILTER_DIRECTIVE, ""); //$NON-NLS-1$
	}

}