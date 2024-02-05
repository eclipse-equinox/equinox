/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.spi;

import org.eclipse.core.runtime.IExtensionRegistry;

/**
 * Implement this interface to specify a contributed extension registry.
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * 
 * @see org.eclipse.core.runtime.RegistryFactory#getRegistry()
 * @see org.eclipse.core.runtime.RegistryFactory#setDefaultRegistryProvider(IRegistryProvider)
 * @since org.eclipse.equinox.registry 3.2
 */
public interface IRegistryProvider {

	/**
	 * Returns the extension registry contributed by this provider; must not be
	 * <code>null</code>.
	 *
	 * @return an extension registry
	 */
	public IExtensionRegistry getRegistry();
}
