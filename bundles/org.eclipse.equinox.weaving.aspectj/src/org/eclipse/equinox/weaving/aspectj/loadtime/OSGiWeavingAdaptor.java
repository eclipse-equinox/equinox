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
 *   Martin Lippert            minor changes and bugfixes
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.eclipse.equinox.weaving.aspectj.WeavingServicePlugin;
import org.osgi.framework.Bundle;

/**
 * The weaving adaptor for AspectJs load-time weaving API that deals with the
 * OSGi specifics for load-time weaving
 */
public class OSGiWeavingAdaptor extends ClassLoaderWeavingAdaptor {

    private static final String AOP_CONTEXT_LOCATION_HEADER = "Eclipse-AspectContext";

    private static final String DEFAULT_AOP_CONTEXT_LOCATION = "META-INF/aop.xml";

    private final List aspectDefinitions;

    private final ClassLoader classLoader;

    private boolean initialized;

    private boolean initializing;

    private final StringBuffer namespaceAddon;

    private final OSGiWeavingContext weavingContext;

    public OSGiWeavingAdaptor(final ClassLoader loader,
            final OSGiWeavingContext context) {
        super();
        this.classLoader = loader;
        this.weavingContext = context;
        this.namespaceAddon = new StringBuffer();
        this.aspectDefinitions = parseDefinitionsForBundle();
    }

    /**
     * returns the found aspect definitions
     */
    public List getAspectDefinitions() {
        return this.aspectDefinitions;
    }

    /**
     * @see org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor#getNamespace()
     */
    public String getNamespace() {
        //        final String namespace = super.getNamespace();
        return namespaceAddon.toString();

        //        if (namespace != null && namespace.length() > 0
        //                && namespaceAddon.length() > 0) {
        //            return namespace + " - " + namespaceAddon.toString(); 
        //        } else {
        //            return namespace;
        //        }
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

    private void addToNamespaceAddon(final URL xml) {
        final String bundleName = weavingContext.getBundleIdFromURL(xml);
        final String bundleVersion = weavingContext
                .getBundleVersionFromURL(xml);

        namespaceAddon.append(bundleName);
        namespaceAddon.append(":");
        namespaceAddon.append(bundleVersion);
        namespaceAddon.append(";");
    }

    private String getDefinitionLocation(final Bundle bundle) {
        String aopContextHeader = (String) bundle.getHeaders().get(
                AOP_CONTEXT_LOCATION_HEADER);
        if (aopContextHeader != null) {
            aopContextHeader = aopContextHeader.trim();
            return aopContextHeader;
        }

        return DEFAULT_AOP_CONTEXT_LOCATION;
    }

    private void parseDefinitionFromRequiredBundle(final Bundle bundle,
            final List definitions, final Set seenBefore) {
        try {
            final URL aopXmlDef = bundle
                    .getEntry(getDefinitionLocation(bundle));
            if (aopXmlDef != null) {
                if (!seenBefore.contains(aopXmlDef)) {
                    definitions.add(DocumentParser.parse(aopXmlDef));
                    seenBefore.add(aopXmlDef);

                    addToNamespaceAddon(aopXmlDef);
                } else {
                    warn("ignoring duplicate definition: " + aopXmlDef);
                }
            }
        } catch (final Exception e) {
            warn("parse definitions failed", e);
        }
    }

    /**
     * Load and cache the aop.xml/properties according to the classloader
     * visibility rules
     * 
     * @param weaver
     * @param loader
     */
    private List parseDefinitionsForBundle() {
        final List definitions = new ArrayList();
        final Set seenBefore = new HashSet();

        try {
            parseDefinitionsFromVisibility(definitions, seenBefore);
            parseDefinitionsFromRequiredBundles(definitions, seenBefore);
            if (definitions.isEmpty()) {
                info("no configuration found. Disabling weaver for bundler "
                        + weavingContext.getClassLoaderName());
            }
        } catch (final Exception e) {
            definitions.clear();
            warn("parse definitions failed", e);
        }

        return definitions;
    }

    private void parseDefinitionsFromRequiredBundles(final List definitions,
            final Set seenBefore) {
        final Bundle[] bundles = weavingContext.getBundles();

        Arrays.sort(bundles, new Comparator() {

            public int compare(final Object arg0, final Object arg1) {
                final long bundleId1 = ((Bundle) arg0).getBundleId();
                final long bundleId2 = ((Bundle) arg1).getBundleId();
                return (int) (bundleId1 - bundleId2);
            }
        });

        for (int i = 0; i < bundles.length; i++) {
            parseDefinitionFromRequiredBundle(bundles[i], definitions,
                    seenBefore);
        }
    }

    private void parseDefinitionsFromVisibility(final List definitions,
            final Set seenBefore) {
        final String resourcePath = System.getProperty(
                "org.aspectj.weaver.loadtime.configuration", "");
        final StringTokenizer st = new StringTokenizer(resourcePath, ";");

        while (st.hasMoreTokens()) {
            try {
                final Enumeration xmls = weavingContext.getResources(st
                        .nextToken());

                while (xmls.hasMoreElements()) {
                    final URL xml = (URL) xmls.nextElement();
                    if (!seenBefore.contains(xml)) {
                        info("using configuration "
                                + weavingContext.getFile(xml));
                        definitions.add(DocumentParser.parse(xml));
                        seenBefore.add(xml);

                        addToNamespaceAddon(xml);
                    } else {
                        warn("ignoring duplicate definition: " + xml);
                    }
                }
            } catch (final Exception e) {
                warn("parse definitions failed", e);
            }
        }
    }

}
