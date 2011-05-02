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
 *   Martin Lippert            extracted caching service factory
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.net.URL;
import java.util.Map;

public interface ICachingService {

    public boolean canCacheGeneratedClasses();

    public CacheEntry findStoredClass(String namespace, URL sourceFileURL,
            String name);

    public void stop();

    public boolean storeClass(String namespace, URL sourceFileURL,
            Class<?> clazz, byte[] classbytes);

    public boolean storeClassAndGeneratedClasses(String namespace,
            URL sourceFileURL, Class<?> clazz, byte[] classbytes,
            Map<String, byte[]> generatedClasses);

}
