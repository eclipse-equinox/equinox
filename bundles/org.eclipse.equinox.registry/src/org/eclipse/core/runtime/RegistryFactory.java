/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.internal.registry.RegistryProviderFactory;
import org.eclipse.core.internal.registry.osgi.RegistryStrategyOSGI;
import org.eclipse.core.runtime.spi.IRegistryProvider;
import org.eclipse.core.runtime.spi.RegistryStrategy;

/**
 * Use this class to create or obtain an extension registry.
 * <p>
 * The following methods can be used without OSGi running:
 * </p><p><ul>
 * <li>{@link #createRegistry(RegistryStrategy, Object, Object)}</li>
 * <li>{@link #getRegistry()}</li>
 * <li>{@link #setDefaultRegistryProvider(IRegistryProvider)}</li>
 * </ul></p><p>
 * This class is not intended to be subclassed or instantiated.
 * </p>
 * @since org.eclipse.equinox.registry 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class RegistryFactory {

	/**
	 * Creates a new extension registry based on the given set of parameters.
	 * <p>
	 * The strategy is an optional collection of methods that supply additional registry 
	 * functionality. Users may pass in <code>null</code> for the strategy if default 
	 * behavior is sufficient.
	 * </p><p>
	 * The master token is stored by the registry and later used as an identifier of callers 
	 * who are allowed full control over the registry functionality. Users may pass in 
	 * <code>null</code> as a master token.
	 * </p><p>
	 * The user token is stored by the registry and later used as an identifier of callers
	 * who are allowed to control registry at the user level. For instance, users attempting to 
	 * modify dynamic contributions to the registry have to use the user token. Users may pass 
	 * in <code>null</code> as a user token.
	 * </p>
	 * @param strategy registry strategy or <code>null</code>
	 * @param masterToken the token used for master control of the registry or <code>null</code>
	 * @param userToken the token used for user control of the registry or <code>null</code>
	 * @return the new extension registry
	 */
	public static IExtensionRegistry createRegistry(RegistryStrategy strategy, Object masterToken, Object userToken) {
		return new ExtensionRegistry(strategy, masterToken, userToken);
	}

	/**
	 * Returns the default extension registry specified by the registry provider.
	 * May return <code>null</code> if the provider has not been set or if the 
	 * registry has not been created.
	 * 
	 * @return existing extension registry or <code>null</code>
	 */
	public static IExtensionRegistry getRegistry() {
		IRegistryProvider defaultRegistryProvider = RegistryProviderFactory.getDefault();
		if (defaultRegistryProvider == null)
			return null;
		return defaultRegistryProvider.getRegistry();
	}

	/**
	 * Creates a registry strategy that can be used in an OSGi container. The strategy uses 
	 * OSGi contributions and contributors for the registry processing and takes advantage of 
	 * additional mechanisms available through the OSGi library.
	 * <p>
	 * The OSGi registry strategy sequentially checks the array of storage directories to 
	 * discover the location of the registry cache formed by previous invocations of the extension
	 * registry. Once found, the location is used to store registry cache. If this value 
	 * is <code>null</code> then caching of the registry content is disabled.
	 * </p><p>
	 * The cache read-only array is an array the same length as the storage directory array. 
	 * It contains boolean values indicating whether or not each storage directory is read-only. 
	 * If the value at an index is <code>true</code> then the location at the corresponding index 
	 * in the storage directories array is read-only; if <code>false</code> then the cache location 
	 * is read-write. The array can be <code>null</code> if the <code>storageDirs</code> parameter 
	 * is <code>null</code>.
	 * </p><p>
	 * The master token should be passed to the OSGi registry strategy to permit it to perform 
	 * contributions to the registry.
	 * </p><p>
	 * <b>Note:</b> This class/interface is part of an interim API that is still under 
	 * development and expected to change significantly before reaching stability. 
	 * It is being made available at this early stage to solicit feedback from pioneering 
	 * adopters on the understanding that any code that uses this API will almost certainly 
	 * be broken (repeatedly) as the API evolves.
	 * </p>
	 * @param storageDirs array of file system directories or <code>null</code>
	 * @param cacheReadOnly array of read only attributes or <code>null</code>
	 * @param token control token for the registry
	 * @return registry strategy that can be used in an OSGi container
	 * @see #createRegistry(RegistryStrategy, Object, Object)
	 */
	public static RegistryStrategy createOSGiStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object token) {
		return new RegistryStrategyOSGI(storageDirs, cacheReadOnly, token);
	}

	/**
	 * Use this method to specify the default registry provider. The default registry provider
	 * is immutable in the sense that it can be set only once during the application runtime.
	 * Attempts to change the default registry provider will cause an exception to be thrown.
	 * <p>
	 * The given registry provider must not be <code>null</code>.
	 * </p><p>
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
		RegistryProviderFactory.setDefault(provider);
	}
}
