/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

/**
 * The callback interface for the elements desiring to lazily supply information
 * to the adapter manager.
 * 
 * @since org.eclipse.core.runtime 3.2
 */
public interface IAdapterManagerProvider {

	/**
	 * Add factories. The method called before the AdapterManager starts using
	 * factories.
	 * 
	 * @param adapterManager the adapter manager that is about to be used
	 * @return <code>true</code> if factories were added; <code>false</code> if no
	 *         factories were added in this method call.
	 */
	public boolean addFactories(AdapterManager adapterManager);
}
