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
 *   Martin Lippert            reworked
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.net.URL;
import java.util.List;

import org.aspectj.weaver.loadtime.DefaultWeavingContext;
import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.tools.WeavingAdaptor;
import org.eclipse.equinox.weaving.aspectj.AspectJWeavingStarter;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * The weaving context for AspectJs load-time weaving API that deals with the
 * OSGi specifics for load-time weaving
 */
public class OSGiWeavingContext extends DefaultWeavingContext {

    private final List<Definition> aspectDefinitions;

    private final BundleDescription bundleDescription;

    public OSGiWeavingContext(final ClassLoader loader,
            final BundleDescription bundleDescription,
            final List<Definition> aspectDefinitions) {
        super(loader);
        this.bundleDescription = bundleDescription;
        this.aspectDefinitions = aspectDefinitions;
        if (AspectJWeavingStarter.DEBUG)
            System.out.println("- WeavingContext.WeavingContext() locader="
                    + loader + ", bundle="
                    + bundleDescription.getSymbolicName());
    }

    /**
     * @see org.aspectj.weaver.loadtime.DefaultWeavingContext#getClassLoaderName()
     */
    @Override
    public String getClassLoaderName() {
        return bundleDescription.getSymbolicName();
    }

    /**
     * @see org.aspectj.weaver.loadtime.DefaultWeavingContext#getDefinitions(java.lang.ClassLoader,
     *      org.aspectj.weaver.tools.WeavingAdaptor)
     */
    @Override
    public List getDefinitions(final ClassLoader loader,
            final WeavingAdaptor adaptor) {
        return aspectDefinitions;
    }

    /**
     * @see org.aspectj.weaver.loadtime.DefaultWeavingContext#getFile(java.net.URL)
     */
    @Override
    public String getFile(final URL url) {
        return getBundleIdFromURL(url) + url.getFile();
    }

    /**
     * @see org.aspectj.weaver.loadtime.DefaultWeavingContext#getId()
     */
    @Override
    public String getId() {
        return bundleDescription.getSymbolicName();
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + bundleDescription.getSymbolicName()
                + "]";
    }

}
