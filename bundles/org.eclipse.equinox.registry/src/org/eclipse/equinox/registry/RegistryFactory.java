/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.registry;

import java.io.File;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.internal.registry.osgi.RegistryStrategyOSGI;
import org.eclipse.equinox.registry.spi.RegistryStrategy;

/**
 * Use this class to create an extension registry.
 * 
 * This class is not intended to be subclassed or instantiated.
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public final class RegistryFactory {

	/**
	 * Creates an extension registry.
	 *  
	 * @param strategy - optional strategies that modify registry functionality; might be null
	 * @param token - control token for the registry. Keep it to access controlled methods of the
	 * registry
	 * @return - new extension registry
	 */
	public static IExtensionRegistry createExtensionRegistry(RegistryStrategy strategy, Object token) {
		return new ExtensionRegistry(strategy, token);
	}
	
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
}
