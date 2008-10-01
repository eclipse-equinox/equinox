/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes
 *   Heiko Seeberger           AJDT 1.5.1 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import java.io.IOException;

import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingAdaptor;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingContext;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;

public class WeavingService implements IWeavingService {

    private OSGiWeavingAdaptor adaptor1;

    private BundleDescription bundle;

    private ClassLoader loader;

    private OSGiWeavingContext weavingContext;

    public WeavingService() {
        if (WeavingServicePlugin.DEBUG)
            System.out.println("- WeavingService.<init>");
    }

    public WeavingService(final ClassLoader loader, final Bundle bundle,
            final State state, final BundleDescription bundleDescription,
            final SupplementerRegistry supplementerRegistry) {
        this.loader = loader;
        this.bundle = bundleDescription;
        this.weavingContext = new OSGiWeavingContext(loader, bundle, state,
                bundleDescription, supplementerRegistry);
    }

    public void flushGeneratedClasses(final ClassLoader loader) {
        ensureAdaptorInit();
        adaptor1.flushGeneratedClasses();
    }

    public boolean generatedClassesExistFor(final ClassLoader loader,
            final String className) {
        ensureAdaptorInit();
        return adaptor1.generatedClassesExistFor(className);
    }

    /**
     * Return an instance of this service, initalised with the specified
     * classloader
     */
    public IWeavingService getInstance(final ClassLoader loader,
            final Bundle bundle, final State resolverState,
            final BundleDescription bundleDesciption,
            final SupplementerRegistry supplementerRegistry) {
        return new WeavingService(loader, bundle, resolverState,
                bundleDesciption, supplementerRegistry);
    }

    public String getKey() {
        if (WeavingServicePlugin.DEBUG)
            System.out.println("> WeavingService.getKey() bundle="
                    + bundle.getSymbolicName());
        String key;
        ensureAdaptorCreated();
        key = adaptor1.getNamespace();
        if (WeavingServicePlugin.DEBUG)
            System.out.println("< WeavingService.getKey() key='" + key + "'");
        return key;
    }

    /**
     * See Aj.preProcess
     */
    public byte[] preProcess(final String name, final byte[] classbytes,
            final ClassLoader loader) throws IOException {
        if (WeavingServicePlugin.DEBUG)
            System.out.println("> WeavingService.preProcess() bundle="
                    + bundle.getSymbolicName() + ", name=" + name + ", bytes="
                    + classbytes.length);
        byte[] newBytes;
        ensureAdaptorInit();

        // Bug 215177: Adapt to updated (AJ 1.5.4) signature.
        newBytes = adaptor1.weaveClass(name, classbytes, false);
        if (WeavingServicePlugin.DEBUG)
            System.out.println("< WeavingService.preProcess() bytes="
                    + newBytes.length);
        return newBytes;
    }

    /**
     * Initialise Aj
     */
    private void ensureAdaptorCreated() {
        synchronized (this) {
            if (adaptor1 == null) {
                adaptor1 = new OSGiWeavingAdaptor(loader, weavingContext);
            }
        }
    }

    private void ensureAdaptorInit() {
        ensureAdaptorCreated();
        adaptor1.initialize();
    }

}
