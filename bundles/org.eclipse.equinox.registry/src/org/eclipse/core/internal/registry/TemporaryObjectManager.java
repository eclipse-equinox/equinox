/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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

import java.util.Map;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

/**
 * @since 3.1
 */
public class TemporaryObjectManager implements IObjectManager {
	private Map<?, ?> actualObjects; // id --> registry objects
	private final RegistryObjectManager parent; // the main object manager (should be equals to
												// extensionRegistry.getObjectManager)

	public TemporaryObjectManager(Map<?, ?> actualObjects, RegistryObjectManager parent) {
		this.actualObjects = actualObjects;
		this.parent = parent;
	}

	@Override
	public Handle getHandle(int id, byte type) {
		switch (type) {
		case RegistryObjectManager.EXTENSION_POINT:
			return new ExtensionPointHandle(this, id);

		case RegistryObjectManager.EXTENSION:
			return new ExtensionHandle(this, id);

		case RegistryObjectManager.CONFIGURATION_ELEMENT:
			return new ConfigurationElementHandle(this, id);

		case RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT:
		default: // avoid compiler error, type should always be known
			return new ThirdLevelConfigurationElementHandle(this, id);
		}
	}

	@Override
	public Handle[] getHandles(int[] ids, byte type) {
		Handle[] results = null;
		int nbrId = ids.length;
		switch (type) {
		case RegistryObjectManager.EXTENSION_POINT:
			if (nbrId == 0) {
				return ExtensionPointHandle.EMPTY_ARRAY;
			}
			results = new ExtensionPointHandle[nbrId];
			for (int i = 0; i < nbrId; i++) {
				results[i] = new ExtensionPointHandle(this, ids[i]);
			}
			break;

		case RegistryObjectManager.EXTENSION:
			if (nbrId == 0) {
				return ExtensionHandle.EMPTY_ARRAY;
			}
			results = new ExtensionHandle[nbrId];
			for (int i = 0; i < nbrId; i++) {
				results[i] = new ExtensionHandle(this, ids[i]);
			}
			break;

		case RegistryObjectManager.CONFIGURATION_ELEMENT:
			if (nbrId == 0) {
				return ConfigurationElementHandle.EMPTY_ARRAY;
			}
			results = new ConfigurationElementHandle[nbrId];
			for (int i = 0; i < nbrId; i++) {
				results[i] = new ConfigurationElementHandle(this, ids[i]);
			}
			break;

		case RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT:
			if (nbrId == 0) {
				return ConfigurationElementHandle.EMPTY_ARRAY;
			}
			results = new ThirdLevelConfigurationElementHandle[nbrId];
			for (int i = 0; i < nbrId; i++) {
				results[i] = new ThirdLevelConfigurationElementHandle(this, ids[i]);
			}
			break;
		}
		return results;
	}

	@Override
	synchronized public Object getObject(int id, byte type) {
		Object result = null;
		try {
			result = parent.getObject(id, type);
		} catch (InvalidRegistryObjectException e) {
			if (actualObjects != null) {
				result = actualObjects.get(Integer.valueOf(id));
			}
		}
		if (result == null) {
			throw new InvalidRegistryObjectException();
		}
		return result;
	}

	@Override
	synchronized public RegistryObject[] getObjects(int[] values, byte type) {
		if (values.length == 0) {
			switch (type) {
			case RegistryObjectManager.EXTENSION_POINT:
				return ExtensionPoint.EMPTY_ARRAY;
			case RegistryObjectManager.EXTENSION:
				return Extension.EMPTY_ARRAY;
			case RegistryObjectManager.CONFIGURATION_ELEMENT:
			case RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT:
				return ConfigurationElement.EMPTY_ARRAY;
			}
		}

		RegistryObject[] results = null;
		switch (type) {
		case RegistryObjectManager.EXTENSION_POINT:
			results = new ExtensionPoint[values.length];
			break;
		case RegistryObjectManager.EXTENSION:
			results = new Extension[values.length];
			break;
		case RegistryObjectManager.CONFIGURATION_ELEMENT:
		case RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT:
			results = new ConfigurationElement[values.length];
			break;
		}
		for (int i = 0; i < values.length; i++) {
			results[i] = (RegistryObject) getObject(values[i], type);
		}
		return results;
	}

	@Override
	public synchronized void close() {
		actualObjects = null;
	}
}
