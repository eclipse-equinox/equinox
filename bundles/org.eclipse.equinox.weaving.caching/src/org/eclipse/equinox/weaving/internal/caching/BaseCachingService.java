/*******************************************************************************
 * Copyright (c) 2008 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.net.URL;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;

/**
 * Abstract superclass for {@link ICachingService}. Throws
 * {@link UnsupportedOperationException}s for every method.
 * 
 * @author Heiko Seeberger
 */
public abstract class BaseCachingService implements ICachingService {

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
     *      java.net.URL, java.lang.String)
     */
    public byte[] findStoredClass(final String namespace,
            final URL sourceFileURL, final String name) {
        throw new UnsupportedOperationException("Illegal call semantics!");
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#getInstance(java.lang.ClassLoader,
     *      org.osgi.framework.Bundle, java.lang.String)
     */
    public ICachingService getInstance(final ClassLoader classLoader,
            final Bundle bundle, final String key) {
        throw new UnsupportedOperationException("Illegal call semantics!");
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[])
     */
    public boolean storeClass(final String namespace, final URL sourceFileURL,
            final Class clazz, final byte[] classbytes) {
        throw new UnsupportedOperationException("Illegal call semantics!");
    }
}
