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
package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.IRegistryProvider;

public final class RegistryProviderOSGI implements IRegistryProvider {

	private final IExtensionRegistry registry;

	public RegistryProviderOSGI(IExtensionRegistry registry) {
		this.registry = registry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.registry.IRegistryProvider#getRegistry()
	 */
	@Override
	public IExtensionRegistry getRegistry() {
		return registry;
	}
}
