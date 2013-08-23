/*******************************************************************************
 * Copyright (c) 2009, 2013 Martin Lippert and others.
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
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

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
    public AspectResolver(final ISupplementerRegistry supplementerRegistry,
            final AspectAdmin aspectAdmin, final BundleContext bundleContext) {
        this.supplementerRegistry = supplementerRegistry;
        this.aspectAdmin = aspectAdmin;
        this.weavingBundleContext = bundleContext;
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
        return bundle.getVersion().toString();
    }

    /**
     * Resolve the aspects to be woven into the given bundle
     * 
     * @param bundle The bundle in which the aspects should be woven into
     * @param bundlerevision The revision of the bundle to be woven into
     * @return The configuration of aspects what should be woven into the bundle
     */
    public AspectConfiguration resolveAspectsFor(final Bundle bundle,
            final BundleRevision bundleRevision) {
        final List<String> fingerprintElements = new ArrayList<String>();

        final List<Definition> definitions = resolveAspectsForBundle(
                fingerprintElements, bundle, bundleRevision);

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

        return new AspectConfiguration(bundle, foundDefinitions,
                fingerprint.toString());
    }

    private List<Definition> resolveAspectsForBundle(
            final List<String> fingerprintElements, final Bundle bundle,
            final BundleRevision bundleRevision) {
        final List<Definition> result = new ArrayList<Definition>();
        final BundleWiring wiring = bundleRevision.getWiring();

        if (wiring != null && weavingBundleContext != null) {

            Definition aspects = null;

            // fragments
            for (final BundleWire hostWire : wiring
                    .getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
                final Bundle fragmentBundle = hostWire.getRequirer()
                        .getBundle();
                if (fragmentBundle != null) {
                    aspects = aspectAdmin.getAspectDefinition(fragmentBundle);
                    if (aspects != null) {
                        result.add(aspects);
                        fingerprintElements.add(fragmentBundle
                                .getSymbolicName()
                                + ":" //$NON-NLS-1$
                                + hostWire.getRequirer().getVersion()
                                        .toString());
                    }
                }
            }

            // required bundles
            final List<BundleWire> requiredBundles = wiring
                    .getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
            ManifestElement[] requireHeaders = null;
            if (!requiredBundles.isEmpty()) {
                try {
                    requireHeaders = ManifestElement
                            .parseHeader(
                                    Constants.REQUIRE_BUNDLE,
                                    bundle.getHeaders("").get(Constants.REQUIRE_BUNDLE)); //$NON-NLS-1$
                } catch (final BundleException e) {
                }
            }
            for (final BundleWire requiredBundleWire : requiredBundles) {
                final Bundle requiredBundle = requiredBundleWire.getProvider()
                        .getBundle();
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
                                + requiredBundleWire.getProvider().getVersion()
                                        .toString());
                    }
                }
            }

            // imported packages
            final List<BundleWire> importedPackages = wiring
                    .getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
            ManifestElement[] importHeaders = null;
            if (!importedPackages.isEmpty()) {
                try {
                    importHeaders = ManifestElement
                            .parseHeader(
                                    Constants.IMPORT_PACKAGE,
                                    bundle.getHeaders("").get(Constants.IMPORT_PACKAGE)); //$NON-NLS-1$
                } catch (final BundleException e) {
                }
            }
            for (final BundleWire importPackageWire : importedPackages) {
                final Bundle exportingBundle = importPackageWire.getProvider()
                        .getBundle();
                if (exportingBundle != null) {
                    final String importedPackage = (String) importPackageWire
                            .getCapability().getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE);

                    final int applyPolicy = getApplyAspectsPolicy(
                            importHeaders, importedPackage);

                    aspects = aspectAdmin.resolveImportedPackage(
                            exportingBundle, importedPackage, applyPolicy);

                    if (aspects != null) {
                        result.add(aspects);
                        final Object v = importPackageWire
                                .getCapability()
                                .getAttributes()
                                .get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                        final String version = v == null ? Version.emptyVersion
                                .toString() : v.toString();
                        fingerprintElements.add(importedPackage + ":" //$NON-NLS-1$
                                + version);
                    }
                }
            }

            // supplementers
            final Supplementer[] supplementers = this.supplementerRegistry
                    .getSupplementers(bundleRevision.getBundle().getBundleId());

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
                final String finishedValue = bundle.getHeaders("").get( //$NON-NLS-1$
                        AspectAdmin.AOP_BUNDLE_FINISHED_HEADER);
                if (finishedValue == null
                        || !AspectAdmin.AOP_BUNDLE_FINISHED_VALUE
                                .equals(finishedValue)) {
                    result.add(aspects);
                    fingerprintElements.add(bundle.getSymbolicName() + ":" //$NON-NLS-1$
                            + bundleRevision.getVersion().toString());
                }
            }
        }

        return result;
    }

}
