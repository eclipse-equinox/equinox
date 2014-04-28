/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.IRegistryProvider;

public final class RegistryProviderOSGI implements IRegistryProvider {

	private final IExtensionRegistry registry;

	public RegistryProviderOSGI(IExtensionRegistry registry) {
		this.registry = registry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.IRegistryProvider#getRegistry()
	 */
	public IExtensionRegistry getRegistry() {
		return registry;
	}
}
