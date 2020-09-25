/*******************************************************************************
 * Copyright (c) 2020 Christoph Laeubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Laeubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.Collection;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Acts as abridge between {@link IAdapterManager} services registered in the OSGi-Service-Registry and AdapterManager
 *
 */
public class AdapterFactoryBridge implements ServiceTrackerCustomizer<IAdapterFactory, AdapterFactoryBridge.LazyAdapterFactory> {
	private BundleContext bundleContext;

	public AdapterFactoryBridge(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public LazyAdapterFactory addingService(ServiceReference<IAdapterFactory> reference) {
		String[] adaptableClasses = getMultiProperty(reference, IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS);
		String[] adapterNames = getMultiProperty(reference, IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES);
		LazyAdapterFactory proxy;
		if (adapterNames.length == 0 && reference.getProperty(IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES) == null) {
			proxy = new LazyAdapterFactory(reference, bundleContext);
		} else {
			proxy = new LazyAdapterFactoryExtServiceProxy(adapterNames, reference, bundleContext);
		}
		AdapterManager manager = AdapterManager.getDefault();
		for (String adaptableClass : adaptableClasses) {
			manager.registerFactory(proxy, adaptableClass);
		}
		manager.flushLookup();
		return proxy;
	}

	private static String[] getMultiProperty(ServiceReference<?> reference, String propertyName) {
		Object property = reference.getProperty(propertyName);
		if (property instanceof String) {
			String string = (String) property;
			if (string.length() > 0) {
				return new String[] {string};
			}
		} else if (property instanceof String[]) {
			return (String[]) property;
		} else if (property instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) property;
			return collection.stream().filter(String.class::isInstance).map(String.class::cast).toArray(String[]::new);
		}
		return new String[0];
	}

	@Override
	public void modifiedService(ServiceReference<IAdapterFactory> reference, LazyAdapterFactory proxy) {
		String[] adaptableClasses = getMultiProperty(reference, IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS);
		AdapterManager manager = AdapterManager.getDefault();
		manager.unregisterAdapters(proxy);
		if (proxy instanceof LazyAdapterFactoryExtServiceProxy) {
			LazyAdapterFactoryExtServiceProxy lazy = (LazyAdapterFactoryExtServiceProxy) proxy;
			lazy.adapterNames = getMultiProperty(reference, IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES);
		}
		for (String adaptableClass : adaptableClasses) {
			manager.registerFactory(proxy, adaptableClass);
		}
		manager.flushLookup();
	}

	@Override
	public void removedService(ServiceReference<IAdapterFactory> reference, LazyAdapterFactory proxy) {
		AdapterManager manager = AdapterManager.getDefault();
		manager.unregisterAdapters(proxy);
		proxy.dispose();
	}

	public static class LazyAdapterFactory implements IAdapterFactory {

		IAdapterFactory service;
		volatile boolean disposed;
		private final ServiceReference<IAdapterFactory> reference;
		private final BundleContext bundleContext;

		LazyAdapterFactory(ServiceReference<IAdapterFactory> reference, BundleContext bundleContext) {
			this.reference = reference;
			this.bundleContext = bundleContext;
		}

		@Override
		public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
			if (!disposed) {
				IAdapterFactory factory = getFactoryService();
				if (factory != null) {
					return factory.getAdapter(adaptableObject, adapterType);
				}
			}
			return null;
		}

		@Override
		public Class<?>[] getAdapterList() {
			if (!disposed) {
				IAdapterFactory factory = getFactoryService();
				if (factory != null) {
					return factory.getAdapterList();
				}
			}
			return new Class<?>[0];
		}

		synchronized IAdapterFactory getFactoryService() {
			if (service == null && !disposed) {
				service = bundleContext.getService(reference);
			}
			return service;
		}

		synchronized void dispose() {
			disposed = true;
			if (service != null) {
				service = null;
				bundleContext.ungetService(reference);
			}
		}

	}

	private static final class LazyAdapterFactoryExtServiceProxy extends LazyAdapterFactory implements IAdapterFactoryExt {

		volatile String[] adapterNames;

		LazyAdapterFactoryExtServiceProxy(String[] adapterNames, ServiceReference<IAdapterFactory> reference, BundleContext bundleContext) {
			super(reference, bundleContext);
			this.adapterNames = adapterNames;
		}

		@Override
		public synchronized IAdapterFactory loadFactory(boolean force) {
			if (force) {
				return getFactoryService();
			}
			return service;
		}

		@Override
		public String[] getAdapterNames() {
			return adapterNames.clone();
		}

	}

}
