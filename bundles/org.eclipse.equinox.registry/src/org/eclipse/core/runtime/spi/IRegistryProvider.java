/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.spi;

import org.eclipse.core.runtime.IExtensionRegistry;

/**
 * Implement this interface to specify the default extension registry.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost certainly 
 * be broken (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.core.runtime.RegistryFactory#getRegistry()
 * @see org.eclipse.core.runtime.RegistryFactory#setRegistryProvider(IRegistryProvider)
 * @since org.eclipse.equinox.registry 3.2
 */
public interface IRegistryProvider {

	/**
	 * Returns the "default" extension registry.
	 * 
	 * @return an extension registry 
	 */
	public IExtensionRegistry getRegistry();
}
