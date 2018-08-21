/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
