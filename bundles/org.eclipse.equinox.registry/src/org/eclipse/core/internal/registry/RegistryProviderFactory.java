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

package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.IRegistryProvider;

/**
 * @since org.eclipse.equinox.registry 3.2
 */
public final class RegistryProviderFactory {

	private static IRegistryProvider defaultRegistryProvider;

	public static IRegistryProvider getDefault() {
		return defaultRegistryProvider;
	}

	public static void setDefault(IRegistryProvider provider) throws CoreException {
		if (defaultRegistryProvider != null) {
			Status status = new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, RegistryMessages.registry_default_exists, null);
			throw new CoreException(status);
		}
		defaultRegistryProvider = provider;
	}

	public static void releaseDefault() {
		defaultRegistryProvider = null;
	}
}
