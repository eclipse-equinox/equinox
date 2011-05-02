/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import org.osgi.framework.Bundle;

/**
 * This is the central interface for other bundles to implement when they would
 * like to contribute a concrete caching implementation. Bundles should
 * implement this interface and register an implementation as an OSGi service
 * under this interface.
 * 
 * @author Martin Lippert
 */
public interface ICachingServiceFactory {

    /**
     * Create concrete caching service for the given bundle. The caching service
     * is then responsible to cache woven bytecode and retrieve those bytecodes
     * from the cache.
     * 
     * @param classLoader The classloader if the given bundle
     * @param bundle The bundle the caching service should be created for
     * @param key A fingerprint that is created by the concrete weavers to
     *            indicate what the weaving configuration for this bundle is.
     *            The caching service should be able to handle different keys
     *            for the same bundle in order not the deliver the wrong cached
     *            bytes from the cache
     * @return The caching service for the given bundle
     */
    public ICachingService createCachingService(ClassLoader classLoader,
            Bundle bundle, String key);

}
