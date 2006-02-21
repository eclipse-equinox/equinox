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
	 * Creates a new extension registry based on the given set of parameters.
	 * <p>
	 * The strategy is an optional strategy that modify registry functionality. Users
	 * may pass in <code>null</code> for the strategy if default behavior is desired.
	 * </p><p>
	 * The master token is used for master control for the registry. Clients may hold
	 * onto it to access controlled methods of the registry. Contributions made with 
	 * this token are marked as non-dynamic.
	 * </p><p>
	 * The user token is used for user access for the registry. Contributions made with 
	 * this token are marked as dynamic. 
	 * </p>
	 *  
	 * @param strategy optional registry strategy or <code>null</code>
	 * @param masterToken the token used for master control of the registry
	 * @param userToken the token used for user control of the registry
	 * @return the new extension registry
	 */
	public static IExtensionRegistry createRegistry(RegistryStrategy strategy, Object masterToken, Object userToken) {
		return new ExtensionRegistry(strategy, masterToken, userToken);
	}

	/**
	 * Returns the existing extension registry specified by the registry provider.
	 * May return <code>null</code> if the provider has not been set or if the 
	 * registry has not yet been created.
	 * 
	 * @return existing extension registry or <code>null</code>
	 */
	public static IExtensionRegistry getRegistry() {
		if (defaultRegistryProvider == null)
			return null;
		return defaultRegistryProvider.getRegistry();
	}

	/**
	 * Creates a registry strategy that can be used in an OSGi container. It uses OSGi contributions and contributors
	 * for the registry processing and takes advantage of additional mechanisms available through
	 * the OSGi library.  
	 * <p>
	 * The storage directory array is a list of file system directories to store cache files. 
	 * TODO: If this value is <code>null</code> then ___ .
	 * </p><p>
	 * The cache read-only array is an array the same length as the storage directory array and it contains
	 * boolean values indicating whether or not each storage directory is read-only. If the value at
	 * an index is <code>true</code> then the location at the corresponding index in the storage
	 * array is read-only. If <code>false</code> then the cache location is read-write. The array
	 * can be <code>null</code> if the <code>storageDir</code> parameter is <code>null</code>.
	 * </p><p>
	 * TODO describe the token here
	 * </p><p>
	 * <b>Note:</b> This class/interface is part of an interim API that is still under 
	 * development and expected to change significantly before reaching stability. 
	 * It is being made available at this early stage to solicit feedback from pioneering 
	 * adopters on the understanding that any code that uses this API will almost certainly 
	 * be broken (repeatedly) as the API evolves.
	 * </p>
	 * @param storageDir array of file system directories or <code>null</code>
	 * @param cacheReadOnly array of read only attributes
	 * @param token control token for the registry
	 */
	public static RegistryStrategy createOSGiStrategy(File[] storageDir, boolean[] cacheReadOnly, Object token) {
		return new RegistryStrategyOSGI(storageDir, cacheReadOnly, token);
	}

	/**
	 * Use this method to specify the default registry provider. The default registry provider
	 * is immutable in the sense that it can be set only once during the application runtime.
	 * Attempts to change the default registry provider will cause an exception to be thrown.
	 * <p>
	 * <b>Note:</b> This class/interface is part of an interim API that is still under 
	 * development and expected to change significantly before reaching stability. 
	 * It is being made available at this early stage to solicit feedback from pioneering 
	 * adopters on the understanding that any code that uses this API will almost certainly 
	 * be broken (repeatedly) as the API evolves.
	 * </p>
	 * @see RegistryFactory#getRegistry()
	 * @param provider extension registry provider
	 * @throws CoreException if a default registry provider was already set for this application
	 */
	public static void setDefaultRegistryProvider(IRegistryProvider provider) throws CoreException {
		if (defaultRegistryProvider != null) {
			Status status = new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, RegistryMessages.registry_default_exists, null);
			throw new CoreException(status);
		}
		defaultRegistryProvider = provider;
	}
}
