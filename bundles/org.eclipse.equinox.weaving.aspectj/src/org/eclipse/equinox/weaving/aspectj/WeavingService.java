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
 *   Martin Lippert            weaving context and adaptors reworked     
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingAdaptor;
import org.eclipse.equinox.weaving.aspectj.loadtime.OSGiWeavingContext;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class WeavingService implements IWeavingService {

    private List<Definition> aspectDefinitions;

    private Bundle bundle;

    private BundleDescription bundleDescription;

    private boolean enabled;

    private ClassLoader loader;

    private StringBuffer namespaceAddOn;

    private State resolverState;

    private ISupplementerRegistry supplementerRegistry;

    private OSGiWeavingAdaptor weavingAdaptor;

    private OSGiWeavingContext weavingContext;

    public WeavingService() {
        if (WeavingServicePlugin.DEBUG)
            System.out.println("- WeavingService.<init>");
    }

    public WeavingService(final ClassLoader loader, final Bundle bundle,
            final State state, final BundleDescription bundleDescription,
            final ISupplementerRegistry supplementerRegistry) {
        this.bundle = bundle;
        this.resolverState = state;
        this.supplementerRegistry = supplementerRegistry;
        this.namespaceAddOn = new StringBuffer();
        this.loader = loader;
        this.bundleDescription = bundleDescription;
        this.aspectDefinitions = parseDefinitionsForBundle();

        this.enabled = this.aspectDefinitions.size() > 0;
        if (this.enabled) {
            this.weavingContext = new OSGiWeavingContext(loader,
                    bundleDescription, aspectDefinitions);
            this.weavingAdaptor = new OSGiWeavingAdaptor(loader,
                    weavingContext, namespaceAddOn.toString());
        } else {
            System.err
                    .println("[org.eclipse.equinox.weaving.aspectj] info not weaving bundle '"
                            + bundle.getSymbolicName() + "'");
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
     * Extracts the version of the bundle to which the given url belongs to
     * 
     * @param url An URL of a bundles resource
     * @return The version of the bundle of the resource to which the URL points
     *         to
     */
    public String getBundleVersion(final Bundle bundle) {
        return resolverState.getBundle(bundle.getBundleId()).getVersion()
                .toString();
    }

    /**
     * Return an instance of this service, initalised with the specified
     * classloader
     */
    public IWeavingService getInstance(final ClassLoader loader,
            final Bundle bundle, final State resolverState,
            final BundleDescription bundleDesciption,
            final ISupplementerRegistry supplementerRegistry) {
        return new WeavingService(loader, bundle, resolverState,
                bundleDesciption, supplementerRegistry);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingService#getKey()
     */
    public String getKey() {
        if (WeavingServicePlugin.DEBUG)
            System.out.println("> WeavingService.getKey() bundle="
                    + bundleDescription.getSymbolicName());

        final String namespace = namespaceAddOn.toString();

        if (WeavingServicePlugin.DEBUG)
            System.out.println("< WeavingService.getKey() key='" + namespace
                    + "'");

        return namespace;
    }

    /**
     * See Aj.preProcess
     */
    public byte[] preProcess(final String name, final byte[] classbytes,
            final ClassLoader loader) throws IOException {
        if (enabled) {
            if (WeavingServicePlugin.DEBUG)
                System.out.println("> WeavingService.preProcess() bundle="
                        + bundleDescription.getSymbolicName() + ", name="
                        + name + ", bytes=" + classbytes.length);
            byte[] newBytes;
            ensureAdaptorInit();

            // Bug 215177: Adapt to updated (AJ 1.5.4) signature.
            newBytes = weavingAdaptor.weaveClass(name, classbytes, false);
            if (WeavingServicePlugin.DEBUG)
                System.out.println("< WeavingService.preProcess() bytes="
                        + newBytes.length);
            return newBytes;
        } else {
            return null;
        }
    }

    protected Bundle[] getBundles() {
        final Set<Bundle> bundles = new HashSet<Bundle>();

        // the bundle this context belongs to should be used
        bundles.add(this.bundle);

        final BundleContext weavingBundleContext = WeavingServicePlugin
                .getDefault() != null ? WeavingServicePlugin.getDefault()
                .getContext() : null;
        if (weavingBundleContext != null) {

            // add required bundles
            final BundleDescription[] resolvedRequires = this.bundleDescription
                    .getResolvedRequires();
            for (int i = 0; i < resolvedRequires.length; i++) {
                final Bundle requiredBundle = weavingBundleContext
                        .getBundle(resolvedRequires[i].getBundleId());
                if (requiredBundle != null) {
                    bundles.add(requiredBundle);
                }
            }

            // add fragment bundles
            final BundleDescription[] fragments = this.bundleDescription
                    .getFragments();
            for (int i = 0; i < fragments.length; i++) {
                final Bundle fragmentBundle = weavingBundleContext
                        .getBundle(fragments[i].getBundleId());
                if (fragmentBundle != null) {
                    bundles.add(fragmentBundle);
                }
            }
        }

        // add supplementers
        final Bundle[] supplementers = this.supplementerRegistry
                .getSupplementers(this.bundleDescription.getBundleId());
        bundles.addAll(Arrays.asList(supplementers));

        return bundles.toArray(new Bundle[bundles.size()]);
    }

    private void addToNamespaceAddon(final Bundle bundle) {
        namespaceAddOn.append(bundle.getSymbolicName());
        namespaceAddOn.append(":");
        namespaceAddOn.append(getBundleVersion(bundle));
        namespaceAddOn.append(";");
    }

    private void ensureAdaptorInit() {
        weavingAdaptor.initialize();
    }

    private void parseDefinitionFromRequiredBundle(final Bundle bundle,
            final List<Definition> definitions) {
        try {
            final Definition aspectDefinition = WeavingServicePlugin
                    .getDefault().getAspectDefinitionRegistry()
                    .getAspectDefinition(bundle);
            if (aspectDefinition != null) {
                definitions.add(aspectDefinition);
                addToNamespaceAddon(bundle);
            }
        } catch (final Exception e) {
            //            warn("parse definitions failed", e);
        }
    }

    /**
     * Load and cache the aop.xml/properties according to the classloader
     * visibility rules
     * 
     * @param weaver
     * @param loader
     */
    private List<Definition> parseDefinitionsForBundle() {
        final List<Definition> definitions = new ArrayList<Definition>();

        try {
            parseDefinitionsFromRequiredBundles(definitions);
            //            if (definitions.isEmpty()) {
            //                info("no configuration found. Disabling weaver for bundler "
            //                        + weavingContext.getClassLoaderName());
            //            }
        } catch (final Exception e) {
            definitions.clear();
            //            warn("parse definitions failed", e);
        }

        return definitions;
    }

    private void parseDefinitionsFromRequiredBundles(
            final List<Definition> definitions) {
        final Bundle[] bundles = getBundles();

        Arrays.sort(bundles, new Comparator() {

            public int compare(final Object arg0, final Object arg1) {
                final long bundleId1 = ((Bundle) arg0).getBundleId();
                final long bundleId2 = ((Bundle) arg1).getBundleId();
                return (int) (bundleId1 - bundleId2);
            }
        });

        for (int i = 0; i < bundles.length; i++) {
            parseDefinitionFromRequiredBundle(bundles[i], definitions);
        }
    }

}
