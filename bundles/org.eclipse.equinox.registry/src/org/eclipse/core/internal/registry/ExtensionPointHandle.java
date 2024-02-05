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

/**
 * The code (minus the getDeclaringPluginDescriptor() was moved into the
 * BaseExtensionPointHandle to avoid duplicating code in the compatibility
 * fragment.
 *
 * Modifications to the code should be done in the BaseExtensionPointHandle.
 *
 * @since 3.1
 */
public class ExtensionPointHandle extends BaseExtensionPointHandle {

	static final ExtensionPointHandle[] EMPTY_ARRAY = new ExtensionPointHandle[0];

	public ExtensionPointHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}
}
