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

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.spi.RegistryStrategy;

/**
 * Use this class to create or obtain an extension registry.
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
		return RegistryUtils.getRegistryFromProvider();
	}
}
