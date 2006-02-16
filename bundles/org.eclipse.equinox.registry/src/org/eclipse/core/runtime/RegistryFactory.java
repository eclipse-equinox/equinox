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
import org.eclipse.core.internal.registry.*;
import org.eclipse.core.internal.registry.osgi.RegistryStrategyOSGI;
import org.eclipse.core.runtime.spi.*;

/**
 * Use this class to create or obtain an extension registry.
 * <p>
 * This class is not intended to be subclassed or instantiated.
 * </p>
 * @since org.eclipse.equinox.registry 3.2
 */
public final class RegistryFactory {

	private static IRegistryProvider defaultRegistryProvider;

	/**
	 * Creates an extension registry.
	 *  
	 * @param strategy - optional strategies that modify registry functionality; might be null
	 * @param masterToken - master control token for the registry. Keep it to access controlled methods of the
	 * registry. Contributions made with this token are marked as non-dynamic.
	 * @param userToken - user access token for the registry. Contributions made with this token are marked 
	 * as dynamic. 
	 * @return - new extension registry
	 * @throws CoreException in case if registry start conditions are not met. The exception's status 
	 * message provides additional details.
	 */
	public static IExtensionRegistry createRegistry(RegistryStrategy strategy, Object masterToken, Object userToken) {
		return new ExtensionRegistry(strategy, masterToken, userToken);
	}

	/**
	 * Returns the existing extension registry specified by the registry provider.
	 * May return null is the provider has not been set or registry was not created.
	 * 
	 * @return existing extension registry or null
	 */
	public static IExtensionRegistry getRegistry() {
		if (defaultRegistryProvider == null)
			return null;
		return defaultRegistryProvider.getRegistry();
	}

	/**
	 * Creates registry strategy that can be used in an OSGi container. It uses OSGi contributions and contributors
	 * for the registry processing and takes advantage of additional mechanisms available through
	 * the OSGi library.  
	 * <p>
	 * <b>Note:</b> This class/interface is part of an interim API that is still under 
	 * development and expected to change significantly before reaching stability. 
	 * It is being made available at this early stage to solicit feedback from pioneering 
	 * adopters on the understanding that any code that uses this API will almost certainly 
	 * be broken (repeatedly) as the API evolves.
	 * </p>
	 * @param storageDir - file system directory to store cache files; might be null
	 * @param cacheReadOnly - true: cache is read only; false: cache is read/write
	 * @param token - control token for the registry
	 */
	public static RegistryStrategy createOSGiStrategy(File storageDir, boolean cacheReadOnly, Object token) {
		return new RegistryStrategyOSGI(storageDir, cacheReadOnly, token);
	}

	/**
	 * Use this method to specify the default registry provider. The default registry provider
	 * is immutable in the sense that it can be set only once during the application runtime.
	 * Attempts to change the default registry provider will cause CoreException.
	 * <p>
	 * <b>Note:</b> This class/interface is part of an interim API that is still under 
	 * development and expected to change significantly before reaching stability. 
	 * It is being made available at this early stage to solicit feedback from pioneering 
	 * adopters on the understanding that any code that uses this API will almost certainly 
	 * be broken (repeatedly) as the API evolves.
	 * </p>
	 * @see RegistryFactory#getRegistry()
	 * @param provider - extension registry provider
	 * @throws CoreException - default registry provider was already set for this application
	 */
	public static void setDefaultRegistryProvider(IRegistryProvider provider) throws CoreException {
		if (defaultRegistryProvider != null) {
			Status status = new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, RegistryMessages.registry_default_exists, null);
			throw new CoreException(status);
		}
		defaultRegistryProvider = provider;
	}
}
