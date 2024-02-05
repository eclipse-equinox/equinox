/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

import org.eclipse.core.runtime.InvalidRegistryObjectException;

/**
 * A handle is the super class to all registry objects that are now served to
 * users. The handles never hold on to any "real" content of the object being
 * represented. A handle can become stale if its referenced object has been
 * removed from the registry.
 * 
 * @since 3.1.
 */
public abstract class Handle {
	protected IObjectManager objectManager;

	private int objectId;

	public int getId() {
		return objectId;
	}

	Handle(IObjectManager objectManager, int value) {
		objectId = value;
		this.objectManager = objectManager;
	}

	/**
	 * Return the actual object corresponding to this handle.
	 * 
	 * @throws InvalidRegistryObjectException when the handle is stale.
	 */
	abstract RegistryObject getObject();

	@Override
	public boolean equals(Object object) {
		if (object instanceof Handle) {
			return objectId == ((Handle) object).objectId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return objectId;
	}
}
