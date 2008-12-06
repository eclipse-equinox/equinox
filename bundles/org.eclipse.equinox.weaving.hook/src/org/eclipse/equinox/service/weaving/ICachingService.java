/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *   Martin Lippert            extracted caching service factory
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.net.URL;

public interface ICachingService {

    public CacheEntry findStoredClass(String namespace, URL sourceFileURL,
            String name);

    public void stop();

    public boolean storeClass(String namespace, URL sourceFileURL, Class clazz,
            byte[] classbytes);

}
