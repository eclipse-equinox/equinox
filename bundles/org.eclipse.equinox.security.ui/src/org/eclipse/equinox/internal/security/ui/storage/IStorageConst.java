/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.storage;

/**
 * Shared constants used by secure storage implementation.
 */
public interface IStorageConst {

	/**
	 * Name of the internal node used by the secure storage itself.
	 */
	public String STORAGE_ID = "org.eclipse.equinox.secure.storage"; //$NON-NLS-1$

	/**
	 * Path describing internal node used by the secure storage itself.
	 */
	public String PROVIDER_NODE = "/" + STORAGE_ID; //$NON-NLS-1$

}
