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
 *   Heiko Seeberger           AJDT 1.5.1 changes     
 *   Martin Lippert            weaving context and adaptors reworked     
 *   Martin Lippert            extracted weaving service factory
 *   Martin Lippert            advanced aspect resolving implemented
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.weaving.aspectj.loadtime.AspectResolver;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingAdaptor;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingContext;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;

public class AspectJWeavingService implements IWeavingService {

    private List<Definition> aspectDefinitions;

    private BundleDescription bundleDescription;

    private boolean enabled;

    private String namespaceAddOn;

    private OSGiWeavingAdaptor weavingAdaptor;

    private OSGiWeavingContext weavingContext;

    public AspectJWeavingService() {
        if (AspectJWeavingStarter.DEBUG)
            System.out.println("- WeavingService.<init>");
    }

    public AspectJWeavingService(final ClassLoader loader, final Bundle bundle,
            final State state, final BundleDescription bundleDescription,
            final ISupplementerRegistry supplementerRegistry,
            final AspectAdmin aspectAdmin) {
        this.bundleDescription = bundleDescription;

        final AspectResolver aspectResolver = new AspectResolver(state,
                supplementerRegistry, aspectAdmin, AspectJWeavingStarter
                        .getDefault().getContext());
        final AspectConfiguration aspectConfig = aspectResolver
                .resolveAspectsFor(bundle, bundleDescription);
        this.namespaceAddOn = aspectConfig.getFingerprint();
        this.aspectDefinitions = aspectConfig.getAspectDefinitions();

        this.enabled = this.aspectDefinitions.size() > 0;
        if (this.enabled) {
            this.weavingContext = new OSGiWeavingContext(loader,
                    bundleDescription, aspectDefinitions);
            this.weavingAdaptor = new OSGiWeavingAdaptor(loader,
                    weavingContext, namespaceAddOn.toString());
        } else {
            if (AspectJWeavingStarter.DEBUG) {
                System.err
                        .println("[org.eclipse.equinox.weaving.aspectj] info not weaving bundle '"
                                + bundle.getSymbolicName() + "'");
            }
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#flushGeneratedClasses(java.lang.ClassLoader)
     */
    public void flushGeneratedClasses(final ClassLoader loader) {
        if (enabled) {
            ensureAdaptorInit();
            weavingAdaptor.flushGeneratedClasses();
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#generatedClassesExistFor(java.lang.ClassLoader,
     *      java.lang.String)
     */
    public boolean generatedClassesExistFor(final ClassLoader loader,
            final String className) {
        if (enabled) {
            ensureAdaptorInit();
            return weavingAdaptor.generatedClassesExistFor(className);
        } else {
            return false;
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#getGeneratedClassesFor(java.lang.String)
     */
    public Map<String, byte[]> getGeneratedClassesFor(final String className) {
        if (enabled) {
            ensureAdaptorInit();
            return weavingAdaptor.getGeneratedClassesFor(className);
        } else {
            return new HashMap<String, byte[]>();
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#getKey()
     */
    public String getKey() {
        if (AspectJWeavingStarter.DEBUG)
            System.out.println("> WeavingService.getKey() bundle="
                    + bundleDescription.getSymbolicName());

        final String namespace = namespaceAddOn.toString();

        if (AspectJWeavingStarter.DEBUG)
            System.out.println("< WeavingService.getKey() key='" + namespace
                    + "'");

        return namespace;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#preProcess(java.lang.String,
     *      byte[], java.lang.ClassLoader)
     */
    public byte[] preProcess(final String name, final byte[] classbytes,
            final ClassLoader loader) throws IOException {
        if (enabled) {
            if (AspectJWeavingStarter.DEBUG)
                System.out.println("> WeavingService.preProcess() bundle="
                        + bundleDescription.getSymbolicName() + ", name="
                        + name + ", bytes=" + classbytes.length);
            byte[] newBytes;
            ensureAdaptorInit();

            // Bug 215177: Adapt to updated (AJ 1.5.4) signature.
            newBytes = weavingAdaptor.weaveClass(name, classbytes, false);
            if (AspectJWeavingStarter.DEBUG)
                System.out.println("< WeavingService.preProcess() bytes="
                        + newBytes.length);
            return newBytes;
        } else {
            return null;
        }
    }

    private void ensureAdaptorInit() {
        weavingAdaptor.initialize();
    }

}
