/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Martin Lippert - initial implementation
 *     Martin Lippert - caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.net.URL;
import java.util.Map;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;

/**
 * This implementation of the caching service is responsible for those bundles
 * that are not affected by the weaving. This is the case if no aspects are
 * being found for this bundle.
 * 
 * This caching service indicates for the runtime system that classes of the
 * bundle this caching service belongs to dont need to be passed to the weaving
 * service. Instead the original code from the bundle can be used.
 * 
 * @author Martin Lippert
 */
public class UnchangedCachingService implements ICachingService {

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#canCacheGeneratedClasses()
     */
    public boolean canCacheGeneratedClasses() {
        return false;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
     *      java.net.URL, java.lang.String)
     */
    public CacheEntry findStoredClass(final String namespace,
            final URL sourceFileURL, final String name) {
        return new CacheEntry(true, null);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#stop()
     */
    public void stop() {
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[])
     */
    public boolean storeClass(final String namespace, final URL sourceFileURL,
            final Class<?> clazz, final byte[] classbytes) {
        return false;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClassAndGeneratedClasses(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[], java.util.Map)
     */
    public boolean storeClassAndGeneratedClasses(final String namespace,
            final URL sourceFileUrl, final Class<?> clazz,
            final byte[] classbytes, final Map<String, byte[]> generatedClasses) {
        return false;
    }
}
