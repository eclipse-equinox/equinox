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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.aspectj.weaver.loadtime.definition.Definition.ConcreteAspect;
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

        return AspectAdmin.ASPECT_POLICY_NOT_DEFINED;
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

        return AOP_CONTEXT_DEFAULT_LOCATION;
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
     * @see org.eclipse.equinox.weaving.aspectj.AspectAdmin#resolveImportedPackage(org.osgi.framework.Bundle,
     *      java.lang.String, int)
     */
    public Definition resolveImportedPackage(final Bundle bundle,
            final String packageName, final int applyAspectsPolicy) {
        if (AspectAdmin.ASPECT_APPLY_POLICY_TRUE == applyAspectsPolicy) {
            final Definition exportedAspectDefinitions = getExportedAspectDefinitions(bundle);
            final Definition result = new Definition();
            if (exportedAspectDefinitions != null) {
                final List<?> aspectClassNames = exportedAspectDefinitions
                        .getAspectClassNames();
                for (final Iterator<?> iterator = aspectClassNames.iterator(); iterator
                        .hasNext();) {
                    final String aspectName = (String) iterator.next();
                    final String aspectPackageName = getPackage(aspectName);
                    if (aspectPackageName.equals(packageName)) {
                        result.getAspectClassNames().add(aspectName);
                    }
                }

                final Iterator<?> concreteAspects = exportedAspectDefinitions
                        .getConcreteAspects().iterator();
                while (concreteAspects.hasNext()) {
                    final Definition.ConcreteAspect concreteAspect = (ConcreteAspect) concreteAspects
                            .next();
                    if (concreteAspect.name != null
                            && getPackage(concreteAspect.name).equals(
                                    packageName)) {
                        result.getConcreteAspects().add(concreteAspect);
                    }
                }

                if (exportedAspectDefinitions.getWeaverOptions().trim()
                        .length() > 0) {
                    result.appendWeaverOptions(exportedAspectDefinitions
                            .getWeaverOptions());
                }
            }
            if (result.getAspectClassNames().size() > 0
                    || result.getConcreteAspects().size() > 0
                    || result.getWeaverOptions().length() > 0) {
                return result;
            } else {
                return null;
            }
        } else if (AspectAdmin.ASPECT_APPLY_POLICY_FALSE == applyAspectsPolicy) {
            return null;
        } else {
            final Definition exportedAspectDefinitions = getExportedAspectDefinitions(bundle);
            final Definition result = new Definition();
            if (exportedAspectDefinitions != null) {
                final List<?> aspectClassNames = exportedAspectDefinitions
                        .getAspectClassNames();
                for (final Iterator<?> iterator = aspectClassNames.iterator(); iterator
                        .hasNext();) {
                    final String aspectName = (String) iterator.next();
                    final String aspectPackageName = getPackage(aspectName);
                    final int aspectPolicy = getAspectPolicy(bundle,
                            aspectPackageName);
                    if (aspectPackageName.equals(packageName)
                            && (AspectAdmin.ASPECT_POLICY_NOT_DEFINED == aspectPolicy || AspectAdmin.ASPECT_POLICY_OPT_OUT == aspectPolicy)) {
                        result.getAspectClassNames().add(aspectName);
                    }
                }

                final Iterator<?> concreteAspects = exportedAspectDefinitions
                        .getConcreteAspects().iterator();
                while (concreteAspects.hasNext()) {
                    final Definition.ConcreteAspect concreteAspect = (ConcreteAspect) concreteAspects
                            .next();

                    final String aspectPackageName = getPackage(concreteAspect.name);
                    final int aspectPolicy = getAspectPolicy(bundle,
                            aspectPackageName);

                    if (aspectPackageName.equals(packageName)
                            && (AspectAdmin.ASPECT_POLICY_NOT_DEFINED == aspectPolicy || AspectAdmin.ASPECT_POLICY_OPT_OUT == aspectPolicy)) {
                        result.getConcreteAspects().add(concreteAspect);
                    }
                }

                if (exportedAspectDefinitions.getWeaverOptions().trim()
                        .length() > 0) {
                    result.appendWeaverOptions(exportedAspectDefinitions
                            .getWeaverOptions());
                }
            }

            if (result.getAspectClassNames().size() > 0
                    || result.getConcreteAspects().size() > 0
                    || result.getWeaverOptions().length() > 0) {
                return result;
            } else {
                return null;
            }
        }
    }

    /**
     * @see org.eclipse.equinox.weaving.aspectj.AspectAdmin#resolveRequiredBundle(org.osgi.framework.Bundle,
     *      int)
     */
    public Definition resolveRequiredBundle(final Bundle bundle,
            final int applyAspectsPolicy) {
        if (AspectAdmin.ASPECT_APPLY_POLICY_TRUE == applyAspectsPolicy) {
            return getExportedAspectDefinitions(bundle);
        } else if (AspectAdmin.ASPECT_APPLY_POLICY_FALSE == applyAspectsPolicy) {
            return null;
        } else {
            final Definition exportedAspectDefinitions = getExportedAspectDefinitions(bundle);
            final Definition result = new Definition();

            if (exportedAspectDefinitions != null) {
                final Iterator<?> aspects = exportedAspectDefinitions
                        .getAspectClassNames().iterator();
                while (aspects.hasNext()) {
                    final String aspect = (String) aspects.next();
                    final String aspectPackage = getPackage(aspect);
                    final int aspectPolicy = getAspectPolicy(bundle,
                            aspectPackage);

                    if (aspectPolicy == AspectAdmin.ASPECT_POLICY_NOT_DEFINED
                            || aspectPolicy == AspectAdmin.ASPECT_POLICY_OPT_OUT) {
                        result.getAspectClassNames().add(aspect);
                    }
                }

                final Iterator<?> concreteAspects = exportedAspectDefinitions
                        .getConcreteAspects().iterator();
                while (concreteAspects.hasNext()) {
                    final Definition.ConcreteAspect concreteAspect = (Definition.ConcreteAspect) concreteAspects
                            .next();
                    final String aspectPackage = getPackage(concreteAspect.name);
                    final int aspectPolicy = getAspectPolicy(bundle,
                            aspectPackage);

                    if (aspectPolicy == AspectAdmin.ASPECT_POLICY_NOT_DEFINED
                            || aspectPolicy == AspectAdmin.ASPECT_POLICY_OPT_OUT) {
                        result.getConcreteAspects().add(concreteAspect);
                    }
                }

                if (exportedAspectDefinitions.getWeaverOptions().trim()
                        .length() > 0) {
                    result.appendWeaverOptions(exportedAspectDefinitions
                            .getWeaverOptions());
                }
            }

            if (result.getAspectClassNames().size() > 0
                    || result.getConcreteAspects().size() > 0
                    || result.getWeaverOptions().length() > 0) {
                return result;
            } else {
                return null;
            }
        }
    }

    /**
     * Parse the aspect definition for the given bundle, if there is one.
     * 
     * @param bundle The bundle for which the aspect definition should be parsed
     */
    protected void parseDefinitions(final Bundle bundle) {
        try {
            Definition allAspectsDefinition = null;
            final Set<String> exportedAspects = new LinkedHashSet<String>();
            final Set<Definition.ConcreteAspect> exportedConcreteAspects = new HashSet<Definition.ConcreteAspect>();
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
                    policies
                            .put(packageName, AspectAdmin.ASPECT_POLICY_OPT_OUT);
                }
                if (policy != null
                        && policy.trim().toLowerCase().equals(
                                ASPECT_POLICY_DIRECTIVE_OPT_IN)) {
                    policies.put(packageName, AspectAdmin.ASPECT_POLICY_OPT_IN);
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

            // add aop.xml declared aspects to the list of exported aspects if their packages are exported
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

            if (allAspectsDefinition != null
                    && allAspectsDefinition.getConcreteAspects().size() > 0) {
                final Iterator<?> iterator = allAspectsDefinition
                        .getConcreteAspects().iterator();
                while (iterator.hasNext()) {
                    final Definition.ConcreteAspect concreteAspect = (Definition.ConcreteAspect) iterator
                            .next();
                    if (concreteAspect.name != null
                            && exportedPackages
                                    .contains(getPackage(concreteAspect.name))) {
                        exportedConcreteAspects.add(concreteAspect);
                    }
                }
            }

            if (allAspectsDefinition != null) {
                this.aspectDefinitions.put(bundle, allAspectsDefinition);
            }

            if (exportedAspects.size() > 0
                    || exportedConcreteAspects.size() > 0
                    || (allAspectsDefinition != null && allAspectsDefinition
                            .getWeaverOptions().length() > 0)) {
                final Definition exportedAspectsDefinition = new Definition();
                exportedAspectsDefinition.getAspectClassNames().addAll(
                        exportedAspects);
                exportedAspectsDefinition.getConcreteAspects().addAll(
                        exportedConcreteAspects);

                if (allAspectsDefinition != null
                        && allAspectsDefinition.getWeaverOptions().trim()
                                .length() > 0) {
                    exportedAspectsDefinition
                            .appendWeaverOptions(allAspectsDefinition
                                    .getWeaverOptions());
                }

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
