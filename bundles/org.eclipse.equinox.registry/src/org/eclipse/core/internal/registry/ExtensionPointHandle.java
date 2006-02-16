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
