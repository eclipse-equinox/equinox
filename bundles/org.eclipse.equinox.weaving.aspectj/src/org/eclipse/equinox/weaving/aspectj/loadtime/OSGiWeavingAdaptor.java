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
 *   Heiko Seeberger           AJDT 1.5.1 changes
 *   Martin Lippert            minor changes and bugfixes
 *   Martin Lippert            reworked
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.io.IOException;

import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.eclipse.equinox.weaving.aspectj.WeavingServicePlugin;

/**
 * The weaving adaptor for AspectJs load-time weaving API that deals with the
 * OSGi specifics for load-time weaving
 */
public class OSGiWeavingAdaptor extends ClassLoaderWeavingAdaptor {

    private final ClassLoader classLoader;

    private boolean initialized;

    private boolean initializing;

    private final String namespace;

    private final OSGiWeavingContext weavingContext;

    public OSGiWeavingAdaptor(final ClassLoader loader,
            final OSGiWeavingContext context, final String namespace) {
        super();
        this.classLoader = loader;
        this.weavingContext = context;
        this.namespace = namespace;
    }

    /**
     * @see org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor#getNamespace()
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    public void initialize() {
        if (!initializing) {
            if (!initialized) {
                initializing = true;
                super.initialize(classLoader, weavingContext);
                initialized = true;
                initializing = false;

                if (WeavingServicePlugin.verbose) {
                    if (isEnabled())
                        System.err
                                .println("[org.eclipse.equinox.weaving.aspectj] info weaving bundle '"
                                        + weavingContext.getClassLoaderName()
                                        + "'");
                    else
                        System.err
                                .println("[org.eclipse.equinox.weaving.aspectj] info not weaving bundle '"
                                        + weavingContext.getClassLoaderName()
                                        + "'");
                }
            }
        }
    }

    // Bug 215177: Adapt to updated (AJ 1.5.4) super class signature:
    /**
     * @see org.aspectj.weaver.tools.WeavingAdaptor#weaveClass(java.lang.String,
     *      byte[], boolean)
     */
    @Override
    public byte[] weaveClass(final String name, byte[] bytes,
            final boolean mustWeave) throws IOException {

        /* Avoid recursion during adaptor initialization */
        if (!initializing) {
            if (!initialized) {
                initializing = true;
                initialize(classLoader, weavingContext);
                initialized = true;
                initializing = false;
            }
            // Bug 215177: Adapt to updated (AJ 1.5.4) super class signature:
            bytes = super.weaveClass(name, bytes, mustWeave);
        }
        return bytes;
    }

}
