/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	IBM - Initial API and implementation
 * 	Oracle - Fix for bug 408506
 ******************************************************************************/
package org.eclipse.core.internal.adapter;

import java.util.*;
import java.util.concurrent.Callable;
import org.eclipse.core.internal.registry.Handle;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.IAdapterFactoryExt;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Instances of this class represent adapter factories that have been
 * contributed via the adapters extension point. The concrete factory is not
 * loaded until the factory's plugin is loaded, AND until the factory is
 * requested to supply an adapter.
 */
class AdapterFactoryProxy implements IAdapterFactory, IAdapterFactoryExt {

	private String adaptableType;
	private String[] adapterNames;
	private String contributorName;
	private IExtension declaringExtension;
	/**
	 * The real factory. Null until the factory is loaded.
	 */
	private Optional<IAdapterFactory> factory;
	private Callable<IAdapterFactory> factoryLoader;
	/**
	 * Store Id of the declaring extension. We might need it in case
	 * the owner goes away (in this case element becomes invalid).
	 */
	private String ownerId;
	private int internalOwnerId = -1;

	/**
	 * Creates a new factory proxy based on the given configuration element.
	 * Returns the new proxy, or null if the element could not be created.
	 */
	public static AdapterFactoryProxy createProxy(IConfigurationElement element) {
		AdapterFactoryProxy result = new AdapterFactoryProxy();
		result.contributorName = element.getContributor().getName();
		if (!"factory".equals(element.getName())) { //$NON-NLS-1$
			result.logError();
			return null;
		}

		result.adaptableType = element.getAttribute("adaptableType"); //$NON-NLS-1$
		if (result.adaptableType == null) {
			result.logError();
			return null;
		}

		result.adapterNames = Arrays.stream(element.getChildren())
				// ignore unknown children for forward compatibility
				.filter(child -> "adapter".equals(child.getName())) //$NON-NLS-1$
				.map(child -> child.getAttribute("type")) //$NON-NLS-1$
				.filter(Objects::nonNull).toArray(String[]::new);
		if (result.adapterNames.length == 0) {
			result.logError();
			return null;
		}

		result.declaringExtension = element.getDeclaringExtension();
		result.ownerId = result.declaringExtension.getUniqueIdentifier();
		if (result.declaringExtension instanceof Handle) {
			result.internalOwnerId = ((Handle) result.declaringExtension).getId();
		}
		result.factoryLoader = () -> {
			return (IAdapterFactory) element.createExecutableExtension("class"); //$NON-NLS-1$
		};

		return result;
	}

	public boolean originatesFrom(IExtension extension) {
		String id = extension.getUniqueIdentifier();
		if (id != null) { // match by public ID declared in XML
			return id.equals(ownerId);
		}
		if (!(extension instanceof Handle)) {
			return false; // should never happen
		}
		return (internalOwnerId == ((Handle) extension).getId());
	}

	String getAdaptableType() {
		return adaptableType;
	}

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		Optional<IAdapterFactory> adapterFactory = factory;
		if (adapterFactory == null) {
			adapterFactory = Optional.ofNullable(loadFactory(false));
		}
		return adapterFactory.map(f -> f.getAdapter(adaptableObject, adapterType)).orElse(null);
	}

	@Override
	public Class<?>[] getAdapterList() {
		Optional<IAdapterFactory> adapterFactory = factory;
		if (adapterFactory == null) {
			adapterFactory = Optional.ofNullable(loadFactory(false));
		}
		return adapterFactory.map(f -> f.getAdapterList()).orElse(null);
	}

	@Override
	public String[] getAdapterNames() {
		return adapterNames;
	}

	IExtension getExtension() {
		return declaringExtension;
	}

	/**
	 * Loads the real adapter factory, but only if its associated plug-in is
	 * already loaded. Returns the real factory if it was successfully loaded.
	 * @param force if <code>true</code> the plugin providing the
	 * factory will be loaded if necessary, otherwise no plugin activations
	 * will occur.
	 */
	@Override
	public synchronized IAdapterFactory loadFactory(boolean force) {
		if (factory == null) {
			boolean isActive;
			// Different VMs have different degrees of "laziness" for the class loading.
			// To make sure that VM won't try to load EquinoxUtils before getting into
			// this try-catch block, the fully qualified name is used (removing entry for
			// the EquinoxUtils from the import list).
			try {
				isActive = org.eclipse.core.internal.registry.osgi.EquinoxUtils.isActive(contributorName);
			} catch (NoClassDefFoundError noClass) {
				// This block will only be triggered if VM loads classes in a very "eager" way.
				isActive = true;
			}
			if (!force && !isActive) {
				return null;
			}
			try {
				factory = Optional.of(factoryLoader.call());
			} catch (Exception e) {
				String msg = NLS.bind(RegistryMessages.adapters_cantInstansiate, adaptableType, contributorName);
				RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, msg, e));
				// prevent repeated attempts to load a broken factory
				factory = Optional.empty();
			}
		}
		return factory.orElse(null);
	}

	/**
	 * The factory extension was malformed. Log an appropriate exception.
	 */
	private void logError() {
		String msg = NLS.bind(RegistryMessages.adapters_badAdapterFactory, contributorName);
		RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, msg, null));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AdapterFactoryProxy [contributor: "); //$NON-NLS-1$
		sb.append(contributorName);
		sb.append(", adaptableType: "); //$NON-NLS-1$
		sb.append(adaptableType);
		if (factory != null) {
			sb.append(", factory: "); //$NON-NLS-1$
			sb.append(factory);
		}
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}
}
