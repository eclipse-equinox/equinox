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
 *   Martin Lippert            extracted weaving service factory
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.io.IOException;

/**
 * The IWeavingService is the interface for weavers for individual bundles. This
 * weaver is used by the core equinox aspects runtime to weave bytecodes when a
 * class is loaded and not read from cache.
 * 
 * @author martinlippert
 */
public interface IWeavingService {

    public void flushGeneratedClasses(ClassLoader loader);

    public boolean generatedClassesExistFor(ClassLoader loader, String className);

    /**
     * The key of a concrete weaver for a bundle defines the setting in which
     * the weaver works. This key typically defines a unique key for the set of
     * aspects which are woven into this bundle. The core equinox aspects
     * runtime uses this key to feed the caching service. This means, the weaver
     * should return different keys for different set of aspects (including
     * versions), respectively when the cache should switch its context.
     * 
     * @return A unique key to define the set of aspects that are woven into the
     *         bundle to which this weaver belongs
     */
    public String getKey();

    /**
     * This method is called for each class which is loaded into the JVM and not
     * read from cache to do the actual weaving, if necessary.
     * 
     * @param name The fully qualified name of the class to be loaded
     * @param classbytes The original unmodified bytecode of the class read by
     *            the standard OSGi classloading mechanism
     * @param loader The classloader whichi s responsible for loading the class
     * @return The modified (woven) bytecode of the class or null, if no
     *         modification happened
     * @throws IOException
     */
    public byte[] preProcess(String name, byte[] classbytes, ClassLoader loader)
            throws IOException;

}
