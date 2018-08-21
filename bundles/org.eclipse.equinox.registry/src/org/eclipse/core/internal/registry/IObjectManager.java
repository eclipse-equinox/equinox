/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

/**
 * @since 3.1
 */
public interface IObjectManager {
	Handle getHandle(int id, byte type);

	Handle[] getHandles(int[] ids, byte type);

	Object getObject(int id, byte type);

	RegistryObject[] getObjects(int[] values, byte type);

	void close();
}
