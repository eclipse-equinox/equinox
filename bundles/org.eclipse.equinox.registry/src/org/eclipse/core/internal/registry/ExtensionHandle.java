/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * The code (minus the getDeclaringPluginDescriptor() was moved into 
 * the  BaseExtensionPointHandle to avoid duplicating code in the 
 * compatibility fragment.
 * 
 * Modifications to the code should be done in the BaseExtensionHandle.
 * 
 * @since 3.1 
 */
public class ExtensionHandle extends BaseExtensionHandle {

	static final ExtensionHandle[] EMPTY_ARRAY = new ExtensionHandle[0];

	public ExtensionHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}
}
