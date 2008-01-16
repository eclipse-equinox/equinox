/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.internal.adapter;

import java.util.ArrayList;
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
	private IConfigurationElement element;
	/**
	 * The real factory. Null until the factory is loaded.
	 */
	private IAdapterFactory factory;
	private boolean factoryLoaded = false;
	/**
	 * Store Id of the declaring extension. We might need it in case 
	 * the owner goes away (in this case element becomes invalid).
	 */
	private String ownerId;

	private int internalOwnerID = -1;

	/**
	 * Creates a new factory proxy based on the given configuration element.
	 * Returns the new proxy, or null if the element could not be created.
	 */
	public static AdapterFactoryProxy createProxy(IConfigurationElement element) {
		AdapterFactoryProxy result = new AdapterFactoryProxy();
		result.element = element;
		IExtension extension = element.getDeclaringExtension();
		result.ownerId = extension.getUniqueIdentifier();
		if (extension instanceof Handle)
			result.internalOwnerID = ((Handle) extension).getId();
		if ("factory".equals(element.getName())) //$NON-NLS-1$
			return result;
		result.logError();
		return null;
	}

	public boolean originatesFrom(IExtension extension) {
		String id = extension.getUniqueIdentifier();
		if (id != null) // match by public ID declared in XML
			return id.equals(ownerId);

		if (!(extension instanceof Handle))
			return false; // should never happen

		return (internalOwnerID == ((Handle) extension).getId());
	}

	String getAdaptableType() {
		//cannot return null because it can cause startup failure
		String result = element.getAttribute("adaptableType"); //$NON-NLS-1$
		if (result != null)
			return result;
		logError();
		return ""; //$NON-NLS-1$
	}

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!factoryLoaded)
			loadFactory(false);
		return factory == null ? null : factory.getAdapter(adaptableObject, adapterType);
	}

	public Class[] getAdapterList() {
		if (!factoryLoaded)
			loadFactory(false);
		return factory == null ? null : factory.getAdapterList();
	}

	public String[] getAdapterNames() {
		IConfigurationElement[] children = element.getChildren();
		ArrayList adapters = new ArrayList(children.length);
		for (int i = 0; i < children.length; i++) {
			//ignore unknown children for forward compatibility
			if ("adapter".equals(children[i].getName())) { //$NON-NLS-1$
				String type = children[i].getAttribute("type"); //$NON-NLS-1$
				if (type != null)
					adapters.add(type);
			}
		}
		if (adapters.isEmpty())
			logError();
		return (String[]) adapters.toArray(new String[adapters.size()]);
	}

	IExtension getExtension() {
		return element.getDeclaringExtension();
	}

	/**
	 * Loads the real adapter factory, but only if its associated plug-in is
	 * already loaded. Returns the real factory if it was successfully loaded.
	 * @param force if <code>true</code> the plugin providing the 
	 * factory will be loaded if necessary, otherwise no plugin activations
	 * will occur.
	 */
	public synchronized IAdapterFactory loadFactory(boolean force) {
		if (factory != null || factoryLoaded)
			return factory;
		String contributorName = element.getContributor().getName();
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
		if (!force && !isActive)
			return null;
		//set to true to prevent repeated attempts to load a broken factory
		factoryLoaded = true;
		try {
			factory = (IAdapterFactory) element.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (CoreException e) {
			String msg = NLS.bind(RegistryMessages.adapters_cantInstansiate, getAdaptableType(), element.getContributor().getName());
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, msg, e));
		}
		return factory;
	}

	/**
	 * The factory extension was malformed. Log an appropriate exception
	 */
	private void logError() {
		String msg = NLS.bind(RegistryMessages.adapters_badAdapterFactory, element.getContributor().getName());
		RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, msg, null));
	}
}
