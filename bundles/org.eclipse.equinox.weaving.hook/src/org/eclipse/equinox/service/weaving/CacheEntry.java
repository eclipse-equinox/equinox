/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert              initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

/**
 * A CacheEntry represents an item that is read (or should have been read) from
 * the cache.
 * 
 * A cache entry is the primary communication item between the basic hook
 * mechanism and the cache implementation. The cache can tell the hook to skip
 * any weaving for the class (in the case the cache knows that the class don't
 * need any weaving, e.g. no aspects affect this class) or to use the bytes that
 * are read from the cache to define the class in the VM.
 * 
 * @author Martin Lippert
 */
public class CacheEntry {

    private final byte[] cachedBytes;

    private final boolean dontWeave;

    /**
     * Creates a new cache entry. This item can tell the basic hook mechanism to
     * use the given cached bytes for the class definition or if the original
     * class bytes needs weaving or not
     * 
     * @param dontWeave A flag that indicates whether this item needs to be
     *            woven or not
     * @param cachedBytes The bytes for the class read from the cache
     */
    public CacheEntry(final boolean dontWeave, final byte[] cachedBytes) {
        this.dontWeave = dontWeave;
        this.cachedBytes = cachedBytes;
    }

    /**
     * Tell the hook mechanism to weave a class or not to weave a class
     * 
     * @return true, if the class doesn't need any weaving, otherwise false
     */
    public boolean dontWeave() {
        return dontWeave;
    }

    /**
     * Returns the bytes that are read from the cache. These bytes should be
     * used for defining the class instead of the original ones.
     * 
     * @return The cached bytes for the class
     */
    public byte[] getCachedBytes() {
        return cachedBytes;
    }

}
