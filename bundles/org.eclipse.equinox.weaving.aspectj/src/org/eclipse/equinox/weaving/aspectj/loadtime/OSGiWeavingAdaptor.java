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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.aspectj.bridge.Constants;
import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.eclipse.equinox.weaving.aspectj.WeavingServicePlugin;
import org.osgi.framework.Bundle;

public class OSGiWeavingAdaptor extends ClassLoaderWeavingAdaptor {

    private final static String AOP_XML = Constants.AOP_USER_XML + ";"
            + Constants.AOP_AJC_XML + ";" + Constants.AOP_OSGI_XML;

    private static final String AOP_CONTEXT_LOCATION_HEADER = "Eclipse-AspectContext";

    private static final String DEFAULT_AOP_CONTEXT_LOCATION = "META-INF/aop.xml";

    private boolean initialized;

    private boolean initializing;

    private ClassLoader classLoader;

    private OSGiWeavingContext weavingContext;

    private StringBuffer namespaceAddon;

    public OSGiWeavingAdaptor(ClassLoader loader, OSGiWeavingContext context) {
        super();
        this.classLoader = loader;
        this.weavingContext = context;
        this.namespaceAddon = new StringBuffer();
    }

    // Bug 215177: Adapt to updated (AJ 1.5.4) super class signature:
    public byte[] weaveClass(String name, byte[] bytes, boolean mustWeave)
            throws IOException {

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

    public void initialize() {
        if (!initializing) {
            if (!initialized) {
                initializing = true;
                super.initialize(classLoader, weavingContext);
                initialized = true;
                initializing = false;
            }
        }

        if (WeavingServicePlugin.verbose) {
            if (isEnabled())
                System.err
                        .println("[org.aspectj.osgi.service.weaving] info weaving bundle '"
                                + weavingContext.getClassLoaderName() + "'");
            else
                System.err
                        .println("[org.aspectj.osgi.service.weaving] info not weaving bundle '"
                                + weavingContext.getClassLoaderName() + "'");
        }
    }

    /**
     * Load and cache the aop.xml/properties according to the classloader
     * visibility rules
     * 
     * @param weaver
     * @param loader
     */
    public List parseDefinitionsForBundle() {
        List definitions = new ArrayList();
        Set seenBefore = new HashSet();

        try {
            parseDefinitionsFromVisibility(definitions, seenBefore);
            parseDefinitionsFromRequiredBundles(definitions, seenBefore);
            if (definitions.isEmpty()) {
                info("no configuration found. Disabling weaver for bundler "
                        + weavingContext.getClassLoaderName());
            }
        } catch (Exception e) {
            definitions.clear();
            warn("parse definitions failed", e);
        }

        return definitions;
    }

    private void parseDefinitionsFromVisibility(List definitions, Set seenBefore) {
        String resourcePath = System.getProperty(
                "org.aspectj.weaver.loadtime.configuration", AOP_XML);
        StringTokenizer st = new StringTokenizer(resourcePath, ";");

        while (st.hasMoreTokens()) {
            try {
                Enumeration xmls = weavingContext.getResources(st.nextToken());

                while (xmls.hasMoreElements()) {
                    URL xml = (URL) xmls.nextElement();
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
            } catch (Exception e) {
                warn("parse definitions failed", e);
            }
        }
    }

    private void addToNamespaceAddon(URL xml) {
        String bundleName = weavingContext.getBundleIdFromURL(xml);
        String bundleVersion = weavingContext.getBundleVersionFromURL(xml);

        namespaceAddon.append(bundleName);
        namespaceAddon.append(":");
        namespaceAddon.append(bundleVersion);
        namespaceAddon.append(";");
    }

    private void parseDefinitionsFromRequiredBundles(List definitions,
            Set seenBefore) {
        Bundle[] bundles = weavingContext.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            parseDefinitionFromRequiredBundle(bundles[i], definitions,
                    seenBefore);
        }
    }

    private void parseDefinitionFromRequiredBundle(Bundle bundle,
            List definitions, Set seenBefore) {
        try {
            URL aopXmlDef = bundle.getEntry(getDefinitionLocation(bundle));
            if (aopXmlDef != null) {
                if (!seenBefore.contains(aopXmlDef)) {
                    definitions.add(DocumentParser.parse(aopXmlDef));
                    seenBefore.add(aopXmlDef);

                    addToNamespaceAddon(aopXmlDef);
                } else {
                    warn("ignoring duplicate definition: " + aopXmlDef);
                }
            }
        } catch (Exception e) {
            warn("parse definitions failed", e);
        }
    }

    private String getDefinitionLocation(Bundle bundle) {
        String aopContextHeader = (String) bundle.getHeaders().get(
                AOP_CONTEXT_LOCATION_HEADER);
        if (aopContextHeader != null) {
            aopContextHeader = aopContextHeader.trim();
            return aopContextHeader;
        }

        return DEFAULT_AOP_CONTEXT_LOCATION;
    }

    /**
     * @see org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor#getNamespace()
     */
    public String getNamespace() {
        String namespace = super.getNamespace();
        if (namespace != null && namespace.length() > 0
                && namespaceAddon.length() > 0) {
            return namespace + " - " + namespaceAddon.toString(); //$NON-NLS-1$
        } else {
            return namespace;
        }
    }

}
