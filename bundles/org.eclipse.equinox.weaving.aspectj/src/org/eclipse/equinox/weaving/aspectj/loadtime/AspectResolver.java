/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.weaving.aspectj.AspectConfiguration;
import org.eclipse.equinox.weaving.aspectj.WeavingServicePlugin;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The aspect resolver is responsible for finding the right connections between
 * bundles and aspects. It calculates the set of aspects that should be woven
 * into a specific bundle, depending on the resolved wires of the bundle and the
 * aspects declared by the wired bundles.
 * 
 * @author Martin Lippert
 */
public class AspectResolver {

    private final State state;

    private final ISupplementerRegistry supplementerRegistry;

    public AspectResolver(final State state,
            final ISupplementerRegistry supplementerRegistry) {
        this.state = state;
        this.supplementerRegistry = supplementerRegistry;
    }

    /**
     * Resolve the aspects to be woven into the given bundle
     * 
     * @param bundle
     * @param bundleDescription
     * @return
     */
    public AspectConfiguration resolveAspectsFor(final Bundle bundle,
            final BundleDescription bundleDescription) {
        final StringBuilder fingerprint = new StringBuilder();
        final List<Definition> definitions = parseDefinitionsForBundle(
                fingerprint, bundle, bundleDescription);

        final Definition[] foundDefinitions = definitions
                .toArray(new Definition[definitions.size()]);
        return new AspectConfiguration(bundle, foundDefinitions, fingerprint
                .toString());
    }

    private void addToNamespaceAddon(final Bundle bundle,
            final StringBuilder fingerprintBuilder) {
        fingerprintBuilder.append(bundle.getSymbolicName());
        fingerprintBuilder.append(':');
        fingerprintBuilder.append(getBundleVersion(bundle));
        fingerprintBuilder.append(';');
    }

    private Bundle[] getBundles(final Bundle bundle,
            final BundleDescription bundleDescription) {
        final Set<Bundle> bundles = new HashSet<Bundle>();

        // the bundle this context belongs to should be used
        bundles.add(bundle);

        final BundleContext weavingBundleContext = WeavingServicePlugin
                .getDefault() != null ? WeavingServicePlugin.getDefault()
                .getContext() : null;
        if (weavingBundleContext != null) {

            // add required bundles
            final BundleDescription[] resolvedRequires = bundleDescription
                    .getResolvedRequires();
            for (int i = 0; i < resolvedRequires.length; i++) {
                final Bundle requiredBundle = weavingBundleContext
                        .getBundle(resolvedRequires[i].getBundleId());
                if (requiredBundle != null) {
                    bundles.add(requiredBundle);
                }
            }

            // add fragment bundles
            final BundleDescription[] fragments = bundleDescription
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
                .getSupplementers(bundleDescription.getBundleId());
        bundles.addAll(Arrays.asList(supplementers));

        return bundles.toArray(new Bundle[bundles.size()]);
    }

    /**
     * Identifies the version of the given bundle
     * 
     * @param bundle The bundle for which the version should be identified
     * @return The version of the bundle
     */
    private String getBundleVersion(final Bundle bundle) {
        return state.getBundle(bundle.getBundleId()).getVersion().toString();
    }

    private void parseDefinitionFromRequiredBundle(final Bundle bundle,
            final List<Definition> definitions,
            final StringBuilder fingerprintBuilder) {
        try {
            final Definition aspectDefinition = WeavingServicePlugin
                    .getDefault().getAspectDefinitionRegistry()
                    .getAspectDefinition(bundle);
            if (aspectDefinition != null) {
                definitions.add(aspectDefinition);
                addToNamespaceAddon(bundle, fingerprintBuilder);
            }
        } catch (final Exception e) {
            //            warn("parse definitions failed", e);
        }
    }

    /**
     * Load and cache the aop.xml/properties according to the classloader
     * visibility rules
     * 
     * @param fingerprintBuilder
     * @param bundle
     * @param bundleDescription
     */
    private List<Definition> parseDefinitionsForBundle(
            final StringBuilder fingerprintBuilder, final Bundle bundle,
            final BundleDescription bundleDescription) {
        final List<Definition> definitions = new ArrayList<Definition>();

        try {
            parseDefinitionsFromRequiredBundles(definitions,
                    fingerprintBuilder, bundle, bundleDescription);
        } catch (final Exception e) {
            definitions.clear();
        }

        return definitions;
    }

    private void parseDefinitionsFromRequiredBundles(
            final List<Definition> definitions,
            final StringBuilder fingerprintBuilder, final Bundle bundle,
            final BundleDescription bundleDescription) {
        final Bundle[] bundles = getBundles(bundle, bundleDescription);

        Arrays.sort(bundles, new Comparator() {

            public int compare(final Object arg0, final Object arg1) {
                final long bundleId1 = ((Bundle) arg0).getBundleId();
                final long bundleId2 = ((Bundle) arg1).getBundleId();
                return (int) (bundleId1 - bundleId2);
            }
        });

        for (int i = 0; i < bundles.length; i++) {
            parseDefinitionFromRequiredBundle(bundles[i], definitions,
                    fingerprintBuilder);
        }
    }

}
