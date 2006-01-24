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

/**
 * Implement this interface to specify the default extension registry.
 * 
 * @see RegistryFactory#getRegistry()
 * @see RegistryUtils#setRegistryProvider(IRegistryProvider)
 * 
 * <b>This is an experimental API. It might change in future.</b>
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public interface IRegistryProvider {

	/**
	 * Returns the "default" extension registry. 
	 * @return an extension registry 
	 */
	public IExtensionRegistry getRegistry();
}
