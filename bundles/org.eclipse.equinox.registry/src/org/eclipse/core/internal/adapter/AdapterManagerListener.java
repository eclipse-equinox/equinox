/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.adapter;

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.internal.runtime.IAdapterManagerProvider;
import org.eclipse.core.runtime.*;

/**
 * Portions of the AdapterManager that deal with the Eclipse extension registry
 * were moved into this class.
 */
public final class AdapterManagerListener implements IRegistryEventListener, IAdapterManagerProvider {
	public static final String ADAPTER_POINT_ID = "org.eclipse.core.runtime.adapters"; //$NON-NLS-1$

	private final AdapterManager theAdapterManager;

	/**
	 * Constructs a new adapter manager.
	 */
	public AdapterManagerListener() {
		theAdapterManager = AdapterManager.getDefault();
		theAdapterManager.registerLazyFactoryProvider(this);
	}

	/**
	 * Loads adapters registered with the adapters extension point from
	 * the plug-in registry.  Note that the actual factory implementations
	 * are loaded lazily as they are needed.
	 */
	@Override
	public boolean addFactories(AdapterManager adapterManager) {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(ADAPTER_POINT_ID);
		if (point == null)
			return false;

		boolean factoriesAdded = false;
		IExtension[] extensions = point.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(element);
				if (proxy != null) {
					adapterManager.registerFactory(proxy, proxy.getAdaptableType());
					factoriesAdded = true;
				}
			}
		}
		RegistryFactory.getRegistry().addListener(this, ADAPTER_POINT_ID);
		return factoriesAdded;
	}

	private void registerExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(element);
			if (proxy != null)
				theAdapterManager.registerFactory(proxy, proxy.getAdaptableType());
		}
	}

	@Override
	public synchronized void added(IExtension[] extensions) {
		for (IExtension extension : extensions) {
			registerExtension(extension);
		}
		theAdapterManager.flushLookup();
	}

	@Override
	public synchronized void removed(IExtension[] extensions) {
		theAdapterManager.flushLookup();
		for (IExtension extension : extensions) {
			for (List<IAdapterFactory> adapterFactories : theAdapterManager.getFactories().values()) {
				for (Iterator<IAdapterFactory> it2 = (adapterFactories).iterator(); it2.hasNext();) {
					IAdapterFactory factory = it2.next();
					if (!(factory instanceof AdapterFactoryProxy))
						continue;
					if (((AdapterFactoryProxy) factory).originatesFrom(extension)) {
						it2.remove();
					}
				}
			}
		}
	}

	@Override
	public synchronized void added(IExtensionPoint[] extensionPoints) {
		// nothing to do
	}

	@Override
	public synchronized void removed(IExtensionPoint[] extensionPoints) {
		// all extensions should have been removed by this point by #removed(IExtension[] extensions)
		// nothing to do
	}

	/*
	 * Shuts down the listener by removing the registry change listener. Should only be
	 * invoked during platform shutdown.
	 */
	public synchronized void stop() {
		RegistryFactory.getRegistry().removeListener(this);
	}
}
