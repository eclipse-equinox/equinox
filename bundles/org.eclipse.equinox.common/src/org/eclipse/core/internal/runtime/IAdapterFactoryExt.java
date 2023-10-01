/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime;

import org.eclipse.core.runtime.IAdapterFactory;

/**
 * An internal interface that exposes portion of AdapterFactoryProxy
 * functionality without the need to import the class itself.
 */
public interface IAdapterFactoryExt {

	/**
	 * Loads the real adapter factory, but only if its associated plug-in is already
	 * loaded. Returns the real factory if it was successfully loaded.
	 * 
	 * @param force if <code>true</code> the plugin providing the factory will be
	 *              loaded if necessary, otherwise no plugin activations will occur.
	 * @return the adapter factory, or <code>null</code>
	 */
	public IAdapterFactory loadFactory(boolean force);

	public String[] getAdapterNames();
}
