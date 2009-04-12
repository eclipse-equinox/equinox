/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
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

import java.util.Map;

/**
 * A CacheItem contains the necessary information to store the cached bytecode
 * of a class to the cache disk storage.
 * 
 * @author Martin Lippert
 */
public class CacheItem {

    private final byte[] cachedBytes;

    private final String directory;

    private final Map<String, byte[]> generatedClasses;

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
        this(cachedBytes, directory, name, null);
    }

    /**
     * Create a new item to be cached
     * 
     * @param cachedBytes The bytes to be written to the cache
     * @param directory The directory to where the bytes should be stored
     * @param name The name of the file to store the bytes in
     * @param generatedClasses The generated classes that should be stored
     *            together with this item (className -> bytecode)
     */
    public CacheItem(final byte[] cachedBytes, final String directory,
            final String name, final Map<String, byte[]> generatedClasses) {
        this.cachedBytes = cachedBytes;
        this.directory = directory;
        this.name = name;
        this.generatedClasses = generatedClasses;
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
     * @return A map containing the generated classes (name -> bytecode) for
     *         this item or null, if there are no generated classes with this
     *         one
     */
    public Map<String, byte[]> getGeneratedClasses() {
        return generatedClasses;
    }

    /**
     * @return The name of the file to be written to the cache
     */
    public String getName() {
        return name;
    }

}
