/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * @since 3.1
 */
public class ThirdLevelConfigurationElementHandle extends ConfigurationElementHandle {

	public ThirdLevelConfigurationElementHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}

	@Override
	protected ConfigurationElement getConfigurationElement() {
		return (ConfigurationElement) objectManager.getObject(getId(),
				RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
	}

	@Override
	public IConfigurationElement[] getChildren() {
		return (IConfigurationElement[]) objectManager.getHandles(getConfigurationElement().getRawChildren(),
				RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
	}

}
