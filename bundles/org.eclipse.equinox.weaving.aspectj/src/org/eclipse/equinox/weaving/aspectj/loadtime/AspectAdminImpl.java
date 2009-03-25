/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.eclipse.equinox.weaving.aspectj.AspectAdmin;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;

/**
 * The AspectAdmin takes care of resolving aspect definitions of resolved
 * bundles and provides information which bundle should be woven with which
 * aspects.
 * 
 * The AspectAdmin takes the aop.xml files into account as well as the aspect
 * definitions in the bundle manifests.
 * 
 * All the information parsing and resolving is done at bundle resolve time, the
 * removal from the cache is done at unresolved events. The initial state is
 * re-created by the initialize method.
 * 
 * @author Martin Lippert
 */
public class AspectAdminImpl implements AspectAdmin, SynchronousBundleListener {

    // directive to declare the aspect policy. possible values are "opt-in" or "opt-out"
    private static final String ASPECT_POLICY_DIRECTIVE = "aspect-policy"; //$NON-NLS-1$

    // policy directive value to tell the weaver that clients have explicitly to ask for those aspects to be applied
    private static final String ASPECT_POLICY_DIRECTIVE_OPT_IN = "opt-in"; //$NON-NLS-1$

    // policy directive value to tell the weaver that clients will get those aspects applied automatically unless they ask for not applying them
    private static final String ASPECT_POLICY_DIRECTIVE_OPT_OUT = "opt-out"; //$NON-NLS-1$

    // directive to declare the exported aspects. The values should list the aspect class names without the package
    private static final String ASPECTS_ATTRIBUTE = "aspects"; //$NON-NLS-1$

    // remember all aspect definitions for the given bundle (regardless of the way they are declared)
    private final Map<Bundle, Definition> aspectDefinitions;

    // remember only the exported aspect definitions for the given bundle (regardless of the way they are declared)
    private final Map<Bundle, Definition> aspectDefinitionsExported;

    // remember the aspect policies per exported package per bundle
    private final Map<Bundle, Map<String, Integer>> aspectPolicies;

    /**
     * Create a registry to manage aspect definition files
     */
    public AspectAdminImpl() {
        this.aspectDefinitions = new ConcurrentHashMap<Bundle, Definition>();
        this.aspectDefinitionsExported = new ConcurrentHashMap<Bundle, Definition>();
        this.aspectPolicies = new ConcurrentHashMap<Bundle, Map<String, Integer>>();
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            bundleResolved(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED) {
            bundleUnresolved(event.getBundle());
        }
    }

    /**
     * Do the parsing when a bundle is resolved
     * 
     * @param bundle The bundle that is resolved (should not be null)
     */
    public void bundleResolved(final Bundle bundle) {
        if (!this.aspectDefinitions.containsKey(bundle)
                && !this.aspectDefinitionsExported.containsKey(bundle)
                && !this.aspectPolicies.containsKey(bundle)) {
            parseDefinitions(bundle);
        }
    }

    /**
     * Remove the cached aspect definitions from the aspect definition registry
     * 
     * @param bundle The bundle that got unresolved (should not be null)
     */
    public void bundleUnresolved(final Bundle bundle) {
        this.aspectDefinitions.remove(bundle);
        this.aspectDefinitionsExported.remove(bundle);
        this.aspectPolicies.remove(bundle);
    }

    /**
     * @see org.eclipse.equinox.weaving.aspectj.AspectAdmin#getAspectDefinition(org.osgi.framework.Bundle)
     */
    public Definition getAspectDefinition(final Bundle bundle) {
        return this.aspectDefinitions.get(bundle);
    }

    /**
     * @see org.eclipse.equinox.weaving.aspectj.AspectAdmin#getAspectPolicy(org.osgi.framework.Bundle,
     *      java.lang.String)
     */
    public int getAspectPolicy(final Bundle bundle, final String packageName) {
        final Map<String, Integer> policies = this.aspectPolicies.get(bundle);
        if (policies != null) {
            final Integer policy = policies.get(packageName);
            if (policy != null) {
                return policy;
            }
        }

        return AspectAdmin.OPT_OUT_POLICY;
    }

    /**
     * Finds the location of the aspect definition within the given bundle. The
     * default location is "META-INF/aop.xml", but if the bundles manifest
     * contains an entry for "Eclipse-AspectContext", that value is used to
     * search for the aop.xml file.
     * 
     * @param bundle The bundle for which to calculate the location of the
     *            aspect definition file
     * @return The path to the aspect definition relately to the given bundle
     */
    public String getDefinitionLocation(final Bundle bundle) {
        String aopContextHeader = (String) bundle.getHeaders().get(
                AOP_CONTEXT_LOCATION_HEADER);
        if (aopContextHeader != null) {
            aopContextHeader = aopContextHeader.trim();
            return aopContextHeader;
        }

        return DEFAULT_AOP_CONTEXT_LOCATION;
    }

    /**
     * @see org.eclipse.equinox.weaving.aspectj.AspectAdmin#getExportedAspectDefinitions(org.osgi.framework.Bundle)
     */
    public Definition getExportedAspectDefinitions(final Bundle bundle) {
        return this.aspectDefinitionsExported.get(bundle);
    }

    /**
     * Initialize the state of the aspect definition registry for the given
     * bundles. This should typically be called when the weaving service bundle
     * is started to set up the aspect definitions for all resolved bundles
     * 
     * @param bundles All bundles that should be taken into account and searched
     *            for aspect definitions
     * 
     */
    public void initialize(final Bundle[] bundles) {
        for (final Bundle bundle : bundles) {
            final int state = bundle.getState();
            if (state != Bundle.INSTALLED && state != Bundle.UNINSTALLED) {
                parseDefinitions(bundle);
            }
        }
    }

    /**
     * Parse the aspect definition for the given bundle, if there is one.
     * 
     * @param bundle The bundle for which the aspect definition should be parsed
     * @return The parsed definition or null, if the bundle does not provide an
     *         aspect definition
     */
    protected void parseDefinitions(final Bundle bundle) {
        try {
            Definition allAspectsDefinition = null;
            final Set<String> exportedAspects = new LinkedHashSet<String>();
            final Map<String, Integer> policies = new HashMap<String, Integer>();
            final Set<String> exportedPackages = new HashSet<String>();

            // try to find aop.xml file
            final String aopXmlLocation = getDefinitionLocation(bundle);
            final URL aopXmlDef = bundle.getEntry(aopXmlLocation);
            if (aopXmlDef != null) {
                allAspectsDefinition = DocumentParser.parse(aopXmlDef);
            }

            // parse export package headers
            final Dictionary<?, ?> manifest = bundle.getHeaders();
            final ManifestElement[] exports = ManifestElement.parseHeader(
                    Constants.EXPORT_PACKAGE, (String) manifest
                            .get(Constants.EXPORT_PACKAGE));

            for (int i = 0; exports != null && i < exports.length; i++) {
                final String packageName = exports[i].getValue();
                exportedPackages.add(packageName);

                // policies
                final String policy = exports[i]
                        .getDirective(ASPECT_POLICY_DIRECTIVE);
                if (policy != null
                        && policy.trim().toLowerCase().equals(
                                ASPECT_POLICY_DIRECTIVE_OPT_OUT)) {
                    policies.put(packageName, AspectAdmin.OPT_OUT_POLICY);
                }
                if (policy != null
                        && policy.trim().toLowerCase().equals(
                                ASPECT_POLICY_DIRECTIVE_OPT_IN)) {
                    policies.put(packageName, AspectAdmin.OPT_IN_POLICY);
                }

                // aspects
                final String allaspects = exports[i]
                        .getAttribute(ASPECTS_ATTRIBUTE);
                if (allaspects != null) {
                    final String[] aspects = ManifestElement
                            .getArrayFromList(allaspects);
                    if (aspects != null) {
                        for (int j = 0; j < aspects.length; j++) {
                            exportedAspects.add(packageName + "." + aspects[j]); //$NON-NLS-1$
                        }
                    }
                }
            }

            // add aop.xml declared aspects to the list of exported aspects if there packages are exported
            if (allAspectsDefinition != null
                    && allAspectsDefinition.getAspectClassNames() != null) {
                final Iterator<?> iterator = allAspectsDefinition
                        .getAspectClassNames().iterator();
                while (iterator.hasNext()) {
                    final String aspect = (String) iterator.next();
                    final String packageName = getPackage(aspect);
                    if (exportedPackages.contains(packageName)) {
                        exportedAspects.add(aspect);
                    }
                }
            }

            if (allAspectsDefinition != null) {
                this.aspectDefinitions.put(bundle, allAspectsDefinition);
            }

            if (exportedAspects.size() > 0) {
                final Definition exportedAspectsDefinition = new Definition();
                exportedAspectsDefinition.getAspectClassNames().addAll(
                        exportedAspects);
                this.aspectDefinitionsExported.put(bundle,
                        exportedAspectsDefinition);
            }

            if (policies.size() > 0) {
                this.aspectPolicies.put(bundle, policies);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private String getPackage(final String aspect) {
        final int index = aspect.lastIndexOf('.');
        if (index >= 0) {
            return aspect.substring(0, index);
        } else {
            return ""; //$NON-NLS-1$
        }
    }
}
