/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import java.net.URL;

import org.eclipse.equinox.service.weaving.CacheEntry;

public interface IWeavingAdaptor {

	public CacheEntry findClass(String name, URL sourceFileURL);

	public void initialize();

	public boolean isInitialized();

	public boolean storeClass(String name, URL sourceFileURL, Class<?> clazz, byte[] classbytes);

	public byte[] weaveClass(String name, byte[] bytes);

}
