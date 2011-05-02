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
 *   Martin Lippert            extracted weaving service factory
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.io.IOException;
import java.util.Map;

/**
 * The IWeavingService is the interface for weavers for individual bundles. This
 * weaver is used by the core equinox aspects runtime to weave bytecodes when a
 * class is loaded and not read from cache.
 * 
 * @author Martin Lippert
 */
public interface IWeavingService {

    /**
     * Flush all generated classes from the weaving service so that memory kept
     * by the weaving service for additional classes can be freed.
     * 
     * @param loader The class loader the weaving service belongs to
     */
    public void flushGeneratedClasses(ClassLoader loader);

    /**
     * Has the weaving service generated new classes on the fly for the given
     * class?
     * 
     * @param loader The class loader of the woven class
     * @param className The name of the woven class
     * @return true, if the weaving service has generated additional classes for
     *         the woven class (closures, for example)
     */
    public boolean generatedClassesExistFor(ClassLoader loader, String className);

    /**
     * Returns a map that contains all generated classes for the given class.
     * Implementations of this method should remove those classes from internal
     * lists (to free memory). This means also that calling this method a second
     * time will return an emptry map.
     * 
     * @param className The name of the class for which additional classes were
     *            generated
     * @return The generated classes (key: generated class name, value:
     *         generated class bytecode)
     */
    public Map<String, byte[]> getGeneratedClassesFor(String className);

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
