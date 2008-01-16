/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private AdapterManager theAdapterManager;

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
	public boolean addFactories(AdapterManager adapterManager) {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(ADAPTER_POINT_ID);
		if (point == null)
			return false;

		boolean factoriesAdded = false;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(elements[j]);
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
		for (int j = 0; j < elements.length; j++) {
			AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(elements[j]);
			if (proxy != null)
				theAdapterManager.registerFactory(proxy, proxy.getAdaptableType());
		}
	}

	public synchronized void added(IExtension[] extensions) {
		for (int i = 0; i < extensions.length; i++)
			registerExtension(extensions[i]);
		theAdapterManager.flushLookup();
	}

	public synchronized void removed(IExtension[] extensions) {
		theAdapterManager.flushLookup();
		for (int i = 0; i < extensions.length; i++) {
			for (Iterator it = theAdapterManager.getFactories().values().iterator(); it.hasNext();) {
				for (Iterator it2 = ((List) it.next()).iterator(); it2.hasNext();) {
					IAdapterFactory factory = (IAdapterFactory) it2.next();
					if (!(factory instanceof AdapterFactoryProxy))
						continue;
					if (((AdapterFactoryProxy) factory).originatesFrom(extensions[i]))
						it2.remove();
				}
			}
		}
	}

	public synchronized void added(IExtensionPoint[] extensionPoints) {
		// nothing to do
	}

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
