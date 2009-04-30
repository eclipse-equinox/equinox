/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert            initial implementation
 *   Martin Lippert            fragment handling fixed
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.Supplementer;
import org.eclipse.equinox.weaving.aspectj.AspectAdmin;
import org.eclipse.equinox.weaving.aspectj.AspectConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * The aspect resolver is responsible for finding the right connections between
 * bundles and aspects. It calculates the set of aspects that should be woven
 * into a specific bundle, depending on the resolved wires of the bundle and the
 * aspects declared by the wired bundles.
 * 
 * @author Martin Lippert
 */
public class AspectResolver {

    private final AspectAdmin aspectAdmin;

    private final State state;

    private final ISupplementerRegistry supplementerRegistry;

    private final BundleContext weavingBundleContext;

    /**
     * Creates a new instance of an aspect resolver for the given state. This
     * resolver can be used to resolve aspect configurations for bundles. It
     * does not carry any own state, it just uses the injected components to
     * retrieve the necessary informaton to determine which aspects to weave
     * into which bundles.
     * 
     * @param state The state of the OSGi framework (contains wiring
     *            information)
     * @param supplementerRegistry The supplementer registry
     * @param aspectAdmin The aspect admin, which tells us which bundle exports
     *            which aspects
     * @param bundleContext The bundle context in which the aspect resolver is
     *            used
     */
    public AspectResolver(final State state,
            final ISupplementerRegistry supplementerRegistry,
            final AspectAdmin aspectAdmin, final BundleContext bundleContext) {
        this.state = state;
        this.supplementerRegistry = supplementerRegistry;
        this.aspectAdmin = aspectAdmin;
        this.weavingBundleContext = bundleContext;
    }

    /**
     * Resolve the aspects to be woven into the given bundle
     * 
     * @param bundle The bundle in which the aspects should be woven into
     * @param bundleDescription The description of the bundle to be woven into
     * @return The configuration of aspects what should be woven into the bundle
     */
    public AspectConfiguration resolveAspectsFor(final Bundle bundle,
            final BundleDescription bundleDescription) {
        final List<String> fingerprintElements = new ArrayList<String>();

        final List<Definition> definitions = resolveAspectsForBundle(
                fingerprintElements, bundle, bundleDescription);

        final Definition[] foundDefinitions = definitions
                .toArray(new Definition[definitions.size()]);

        Collections.sort(fingerprintElements);
        final StringBuilder fingerprint = new StringBuilder();
        final Iterator<String> iterator = fingerprintElements.iterator();
        while (iterator.hasNext()) {
            final String element = iterator.next();
            fingerprint.append(element);
            fingerprint.append(';');
        }

        return new AspectConfiguration(bundle, foundDefinitions, fingerprint
                .toString());
    }

    private int getApplyAspectsPolicy(final ManifestElement[] headers,
            final String manifestValue) {
        int result = AspectAdmin.ASPECT_APPLY_POLICY_NOT_DEFINED;

        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].getValue().equals(manifestValue)) {
                    final String directive = headers[i]
                            .getDirective(AspectAdmin.ASPECT_APPLY_POLICY_DIRECTIVE);
                    if ("true".equals(directive)) { //$NON-NLS-1$
                        result = AspectAdmin.ASPECT_APPLY_POLICY_TRUE;
                    } else if ("false".equals(directive)) { //$NON-NLS-1$
                        result = AspectAdmin.ASPECT_APPLY_POLICY_FALSE;
                    }
                }
            }
        }
        return result;
    }

    private String getBundleVersion(final Bundle bundle) {
        return state.getBundle(bundle.getBundleId()).getVersion().toString();
    }

    private List<Definition> resolveAspectsForBundle(
            final List<String> fingerprintElements, final Bundle bundle,
            final BundleDescription bundleDescription) {
        final List<Definition> result = new ArrayList<Definition>();

        if (weavingBundleContext != null) {

            Definition aspects = null;

            // fragments
            final BundleDescription[] fragments = bundleDescription
                    .getFragments();
            for (int i = 0; i < fragments.length; i++) {
                final Bundle fragmentBundle = weavingBundleContext
                        .getBundle(fragments[i].getBundleId());
                if (fragmentBundle != null) {
                    aspects = aspectAdmin.getAspectDefinition(fragmentBundle);
                    if (aspects != null) {
                        result.add(aspects);
                        fingerprintElements.add(fragmentBundle
                                .getSymbolicName()
                                + ":" //$NON-NLS-1$
                                + fragments[i].getVersion().toString());
                    }
                }
            }

            // required bundles
            final BundleDescription[] resolvedRequires = bundleDescription
                    .getResolvedRequires();
            ManifestElement[] requireHeaders = null;
            if (resolvedRequires.length > 0) {
                try {
                    requireHeaders = ManifestElement
                            .parseHeader(Constants.REQUIRE_BUNDLE,
                                    (String) bundle.getHeaders().get(
                                            Constants.REQUIRE_BUNDLE));
                } catch (final BundleException e) {
                }
            }
            for (int i = 0; i < resolvedRequires.length; i++) {
                final Bundle requiredBundle = weavingBundleContext
                        .getBundle(resolvedRequires[i].getBundleId());
                if (requiredBundle != null) {
                    final int applyPolicy = getApplyAspectsPolicy(
                            requireHeaders, requiredBundle.getSymbolicName());

                    aspects = aspectAdmin.resolveRequiredBundle(requiredBundle,
                            applyPolicy);

                    if (aspects != null) {
                        result.add(aspects);
                        fingerprintElements.add(requiredBundle
                                .getSymbolicName()
                                + ":" //$NON-NLS-1$
                                + resolvedRequires[i].getVersion().toString());
                    }
                }
            }

            // imported packages
            final ExportPackageDescription[] resolvedImports = bundleDescription
                    .getResolvedImports();
            ManifestElement[] importHeaders = null;
            if (resolvedImports.length > 0) {
                try {
                    importHeaders = ManifestElement
                            .parseHeader(Constants.IMPORT_PACKAGE,
                                    (String) bundle.getHeaders().get(
                                            Constants.IMPORT_PACKAGE));
                } catch (final BundleException e) {
                }
            }
            for (int i = 0; i < resolvedImports.length; i++) {
                final Bundle exportingBundle = weavingBundleContext
                        .getBundle(resolvedImports[i].getExporter()
                                .getBundleId());
                if (exportingBundle != null) {
                    final String importedPackage = resolvedImports[i].getName();

                    final int applyPolicy = getApplyAspectsPolicy(
                            importHeaders, importedPackage);

                    aspects = aspectAdmin.resolveImportedPackage(
                            exportingBundle, importedPackage, applyPolicy);

                    if (aspects != null) {
                        result.add(aspects);
                        fingerprintElements.add(importedPackage + ":" //$NON-NLS-1$
                                + resolvedImports[i].getVersion().toString());
                    }
                }
            }

            // supplementers
            final Supplementer[] supplementers = this.supplementerRegistry
                    .getSupplementers(bundleDescription.getBundleId());

            for (int i = 0; i < supplementers.length; i++) {
                aspects = aspectAdmin
                        .getExportedAspectDefinitions(supplementers[i]
                                .getSupplementerBundle());
                if (aspects != null) {
                    result.add(aspects);
                    fingerprintElements.add(supplementers[i].getSymbolicName()
                            + ":" //$NON-NLS-1$
                            + getBundleVersion(supplementers[i]
                                    .getSupplementerBundle()));
                }
            }

            // this bundle
            aspects = aspectAdmin.getAspectDefinition(bundle);
            if (aspects != null) {
                final String finishedValue = (String) bundle.getHeaders().get(
                        AspectAdmin.AOP_BUNDLE_FINISHED_HEADER);
                if (finishedValue == null
                        || !AspectAdmin.AOP_BUNDLE_FINISHED_VALUE
                                .equals(finishedValue)) {
                    result.add(aspects);
                    fingerprintElements.add(bundle.getSymbolicName() + ":" //$NON-NLS-1$
                            + bundleDescription.getVersion().toString());
                }
            }
        }

        return result;
    }

}
