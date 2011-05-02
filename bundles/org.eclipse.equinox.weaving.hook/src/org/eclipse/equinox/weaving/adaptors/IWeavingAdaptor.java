/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import java.net.URL;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;

public interface IWeavingAdaptor {

    public CacheEntry findClass(String name, URL sourceFileURL);

    public void initialize();

    public void setBaseClassLoader(BaseClassLoader baseClassLoader);

    public boolean storeClass(String name, URL sourceFileURL, Class clazz,
            byte[] classbytes);

    public byte[] weaveClass(String name, byte[] bytes);

}
