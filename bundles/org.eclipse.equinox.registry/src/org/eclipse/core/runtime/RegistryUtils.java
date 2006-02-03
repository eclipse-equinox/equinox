/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.File;
import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.registry.osgi.RegistryStrategyOSGI;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.osgi.framework.Bundle;

/**
 * Collection of utility methods related to the extension registry
 * functionality.
 * 
 * This class is not intended to be subclassed or instantiated.
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public final class RegistryUtils {

	private static IRegistryProvider defaultRegistryProvider;

	/**
	 * Creates registry strategy that can be used in OSGi world. It provides the following functionality:
	 *  - Event scheduling is done using Eclipse job scheduling mechanism
	 *  - Translation is done with Equinox ResourceTranslator
	 *  - Uses OSGi bundle model for namespace resolution
	 *  - Uses bunlde-based class loaders to create executable extensions
	 *  - Registry is filled with information stored in plugin.xml / fragment.xml files of OSGi bundles
	 *    with the XML parser is obtained via an OSGi service
	 *  - Performs registry validation based on the time stamps of the plugin.xml / fragment.xml files
	 * 
	 * @param storageDir - file system directory to store cache files; might be null
	 * @param cacheReadOnly - true: cache is read only; false: cache is read/write
	 * @param token - control token for the registry
	 */
	public static RegistryStrategy createOSGiStrategy(File storageDir, boolean cacheReadOnly, Object token) {
		return new RegistryStrategyOSGI(storageDir, cacheReadOnly, token);
	}

	/**
	 * Returns Id corresponding to the OSGi bundle.
	 * @see IExtensionRegistry#addContribution
	 * @param bundle - OSGi bundle
	 * @return Id used for the OSGi bundle
	 */
	public static String getContributorId(Bundle bundle) {
		return RegistryStrategyOSGI.getId(bundle);
	}

	/**
	 * Returns the existing extension registry specified by the registry provider.
	 * May return null is the provider has not been set or registry was not created.
	 * 
	 * @return existing extension registry or null
	 */
	public static IExtensionRegistry getRegistryFromProvider() {
		if (defaultRegistryProvider == null)
			return null;
		return defaultRegistryProvider.getRegistry();
	}

	/**
	 * Use this method to specify the default registry provider. The default registry provider
	 * is immutable in the sense that it can be set only once during the application runtime.
	 * Attempts to change the default registry provider will cause CoreException.
	 * 
	 * @see RegistryFactory#getRegistry()
	 * 
	 * <b>This is an experimental API. It might change in future.</b>
	 * 
	 * @param provider - extension registry provider
	 * @throws CoreException - default registry provider was already set for this application
	 */
	public static void setRegistryProvider(IRegistryProvider provider) throws CoreException {
		if (defaultRegistryProvider != null) {
			Status status = new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, RegistryMessages.registry_default_exists, null);
			throw new CoreException(status);
		}
		defaultRegistryProvider = provider;
	}
}
