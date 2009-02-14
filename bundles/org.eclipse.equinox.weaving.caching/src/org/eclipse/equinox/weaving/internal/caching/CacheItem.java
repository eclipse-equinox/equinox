/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Martin Lippert - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

/**
 * A CacheItem contains the necessary information to store the cached bytecode
 * of a class to the cache disk storage.
 * 
 * @author Martin Lippert
 */
public class CacheItem {

    private final byte[] cachedBytes;

    private final String directory;

    private final String name;

    /**
     * Create a new item to be cached
     * 
     * @param cachedBytes The bytes to be written to the cache
     * @param directory The directory to where the bytes should be stored
     * @param name The name of the file to store the bytes in
     */
    public CacheItem(final byte[] cachedBytes, final String directory,
            final String name) {
        super();
        this.cachedBytes = cachedBytes;
        this.directory = directory;
        this.name = name;
    }

    /**
     * @return The bytes to be written to the cache under the given name
     */
    public byte[] getCachedBytes() {
        return cachedBytes;
    }

    /**
     * @return The directory in which the item should be stored
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @return The name of the file to be written to the cache
     */
    public String getName() {
        return name;
    }

}
