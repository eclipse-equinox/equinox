/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked     
 *   Heiko Seeberger           Enhancements for service dynamics     
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.weaving.hooks.AbstractAspectJHook;
import org.eclipse.equinox.weaving.hooks.Supplementer;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

public class SupplementerRegistry {

    //knibb
    /**
     * Manifest header (named &quot;Supplement-Bundle&quot;) identifying the
     * names (and optionally, version numbers) of any bundles supplemented by
     * this bundle. All supplemented bundles will have all the exported packages
     * of this bundle added to their imports list
     * 
     * <p>
     * The attribute value may be retrieved from the <code>Dictionary</code>
     * object returned by the <code>Bundle.getHeaders</code> method.
     */
    public static final String SUPPLEMENT_BUNDLE = "Eclipse-SupplementBundle"; //$NON-NLS-1$

    /**
     * Manifest header (named &quot;Supplement-Exporter&quot;) identifying the
     * names (and optionally, version numbers) of the packages that the bundle
     * supplements. All exporters of one of these packages will have the
     * exported packages of this bundle added to their imports list.
     * 
     * <p>
     * The attribute value may be retrieved from the <code>Dictionary</code>
     * object returned by the <code>Bundle.getHeaders</code> method.
     */
    public static final String SUPPLEMENT_EXPORTER = "Eclipse-SupplementExporter"; //$NON-NLS-1$

    /**
     * Manifest header (named &quot;Supplement-Importer&quot;) identifying the
     * names (and optionally, version numbers) of the packages that the bundle
     * supplements. All importers of one of these packages will have the
     * exported packages of this bundle added to their imports in addition.
     * 
     * <p>
     * The attribute value may be retrieved from the <code>Dictionary</code>
     * object returned by the <code>Bundle.getHeaders</code> method.
     */
    public static final String SUPPLEMENT_IMPORTER = "Eclipse-SupplementImporter"; //$NON-NLS-1$

    private BundleContext context;

    private final Set dontWeaveTheseBundles; // elements of type String (symbolic name of bundle)

    private PackageAdmin packageAdmin;

    private final Map supplementers; // keys of type String (symbolic name of supplementer bundle), values of type Supplementer

    public SupplementerRegistry() {
        this.supplementers = new HashMap();
        this.dontWeaveTheseBundles = new HashSet();

        this.dontWeaveTheseBundles.add("org.eclipse.osgi");
        this.dontWeaveTheseBundles.add("org.eclipse.update.configurator");
        this.dontWeaveTheseBundles.add("org.eclipse.core.runtime");
        this.dontWeaveTheseBundles.add("org.eclipse.equinox.common");
        this.dontWeaveTheseBundles.add("org.eclipse.equinox.weaving.hook");
        this.dontWeaveTheseBundles.add("org.eclipse.equinox.weaving.aspectj");
        this.dontWeaveTheseBundles.add("org.eclipse.equinox.weaving.caching");
        this.dontWeaveTheseBundles
                .add("org.eclipse.equinox.weaving.caching.j9");
        this.dontWeaveTheseBundles.add("org.aspectj.runtime");
        this.dontWeaveTheseBundles.add("org.aspectj.weaver");
        this.dontWeaveTheseBundles
                .add("org.eclipse.equinox.simpleconfigurator");
    }

    public void addSupplementedBundle(final Bundle supplementedBundle,
            final List supplementers) {
        for (final Iterator iterator = supplementers.iterator(); iterator
                .hasNext();) {
            final String supplementersName = (String) iterator.next();
            if (this.supplementers.containsKey(supplementersName)) {
                final Supplementer supplementer = (Supplementer) this.supplementers
                        .get(supplementersName);
                supplementer.addSupplementedBundle(supplementedBundle);
            }
        }
    }

    public void addSupplementer(final Bundle bundle) {
        final Dictionary manifest = bundle.getHeaders();
        try {
            // First analyze which supplementers already exists for this bundle
            final ManifestElement[] imports = ManifestElement.parseHeader(
                    Constants.IMPORT_PACKAGE, (String) manifest
                            .get(Constants.IMPORT_PACKAGE));
            final ManifestElement[] exports = ManifestElement.parseHeader(
                    Constants.EXPORT_PACKAGE, (String) manifest
                            .get(Constants.EXPORT_PACKAGE));
            final List supplementers = getSupplementers(bundle
                    .getSymbolicName(), imports, exports);
            if (supplementers.size() > 0) {
                this.addSupplementedBundle(bundle, supplementers);
            }

            // Second analyze if this bundle itself is a supplementer
            final ManifestElement[] supplementBundle = ManifestElement
                    .parseHeader(SUPPLEMENT_BUNDLE, (String) manifest
                            .get(SUPPLEMENT_BUNDLE));
            final ManifestElement[] supplementImporter = ManifestElement
                    .parseHeader(SUPPLEMENT_IMPORTER, (String) manifest
                            .get(SUPPLEMENT_IMPORTER));
            final ManifestElement[] supplementExporter = ManifestElement
                    .parseHeader(SUPPLEMENT_EXPORTER, (String) manifest
                            .get(SUPPLEMENT_EXPORTER));

            if (supplementBundle != null || supplementImporter != null
                    || supplementExporter != null) {
                final Supplementer newSupplementer = new Supplementer(bundle,
                        supplementBundle, supplementImporter,
                        supplementExporter);

                this.supplementers.put(bundle.getSymbolicName(),
                        newSupplementer);
                resupplementInstalledBundles(newSupplementer);
            }
        } catch (final BundleException e) {
        }
    }

    public Set getSupplementedBundles() {
        final Set supplementedBundles = new HashSet();
        final Iterator supplementersIterator = supplementers.values()
                .iterator();
        while (supplementersIterator.hasNext()) {
            final Supplementer supplementer = (Supplementer) supplementersIterator
                    .next();
            supplementedBundles.addAll(Arrays.asList(supplementer
                    .getSupplementedBundles()));
        }
        return supplementedBundles;
    }

    public Bundle[] getSupplementers(final Bundle bundle) {
        List result = Collections.EMPTY_LIST;

        if (supplementers.size() > 0) {
            result = new ArrayList();
            for (final Iterator i = supplementers.values().iterator(); i
                    .hasNext();) {
                final Supplementer supplementer = (Supplementer) i.next();
                if (supplementer.isSupplemented(bundle)) {
                    result.add(supplementer.getSupplementerBundle());
                }
            }
        }

        return (Bundle[]) result.toArray(new Bundle[result.size()]);
    }

    public List getSupplementers(final String symbolicName,
            final ManifestElement[] imports, final ManifestElement[] exports) {
        List result = Collections.EMPTY_LIST;

        if (supplementers.size() > 0
                && !this.dontWeaveTheseBundles.contains(symbolicName)) {
            result = new LinkedList();
            for (final Iterator i = supplementers.values().iterator(); i
                    .hasNext();) {
                final Supplementer supplementer = (Supplementer) i.next();
                if (isSupplementerMatching(symbolicName, imports, exports,
                        supplementer)) {
                    result.add(supplementer.getSymbolicName());
                }
            }
        }

        return result;
    }

    public void removeSupplementedBundle(final Bundle bundle) {
        for (final Iterator iterator = this.supplementers.values().iterator(); iterator
                .hasNext();) {
            final Supplementer supplementer = (Supplementer) iterator.next();
            supplementer.removeSupplementedBundle(bundle);
        }
    }

    public void removeSupplementer(final Bundle bundle) {
        // if this bundle is itself supplemented by others, remove the bundle from those lists
        removeSupplementedBundle(bundle);

        // if this bundle supplements other bundles, remove this supplementer and update the other bundles
        if (supplementers.containsKey(bundle.getSymbolicName())) {
            final Supplementer supplementer = (Supplementer) supplementers
                    .get(bundle.getSymbolicName());
            supplementers.remove(bundle.getSymbolicName());
            if (AbstractAspectJHook.verbose)
                System.err
                        .println("[org.aspectj.osgi] info removing supplementer "
                                + bundle.getSymbolicName());

            final Bundle[] supplementedBundles = supplementer
                    .getSupplementedBundles();
            for (int i = 0; i < supplementedBundles.length; i++) {
                final Bundle supplementedBundle = supplementedBundles[i];
                if (supplementedBundle != null) {
                    updateInstalledBundle(supplementedBundle);
                }
            }
        }
    }

    public void setBundleContext(final BundleContext context) {
        this.context = context;
    }

    public void setPackageAdmin(final PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    public void updateInstalledBundle(final Bundle bundle) {
        if (AbstractAspectJHook.verbose)
            System.err
                    .println("[org.aspectj.osgi] info triggering update for re-supplementing "
                            + bundle.getSymbolicName());

        try {
            bundle.update();
        } catch (final BundleException e) {
            e.printStackTrace();
        }
    }

    private boolean isSupplementerMatching(final String symbolicName,
            final ManifestElement[] imports, final ManifestElement[] exports,
            final Supplementer supplementer) {
        final String supplementerName = supplementer.getSymbolicName();
        if (!supplementerName.equals(symbolicName)) {
            if (supplementer.matchSupplementer(symbolicName)
                    || (imports != null && supplementer
                            .matchesSupplementImporter(imports))
                    || (exports != null && supplementer
                            .matchesSupplementExporter(exports))) {
                return true;
            }
        }
        return false;
    }

    private void resupplementInstalledBundles(final Supplementer supplementer) {
        final Bundle[] installedBundles = context.getBundles();

        for (int i = 0; i < installedBundles.length; i++) {
            try {
                final Bundle bundle = installedBundles[i];

                if (bundle.getSymbolicName().equals(
                        supplementer.getSymbolicName())) {
                    continue;
                }

                if (dontWeaveTheseBundles.contains(bundle.getSymbolicName())) {
                    continue;
                }

                final Dictionary manifest = bundle.getHeaders();
                final ManifestElement[] imports = ManifestElement.parseHeader(
                        Constants.IMPORT_PACKAGE, (String) manifest
                                .get(Constants.IMPORT_PACKAGE));
                final ManifestElement[] exports = ManifestElement.parseHeader(
                        Constants.EXPORT_PACKAGE, (String) manifest
                                .get(Constants.EXPORT_PACKAGE));

                if (isSupplementerMatching(bundle.getSymbolicName(), imports,
                        exports, supplementer)) {
                    boolean alreadyRequired = false;
                    final ManifestElement[] requires = ManifestElement
                            .parseHeader(Constants.REQUIRE_BUNDLE,
                                    (String) manifest
                                            .get(Constants.REQUIRE_BUNDLE));
                    if (requires != null) {
                        for (int j = 0; j < requires.length; j++) {
                            if (requires[j].getValue().equals(
                                    supplementer.getSymbolicName())) {
                                alreadyRequired = true;
                            }
                        }
                    }

                    if (!alreadyRequired) {
                        updateInstalledBundle(bundle);
                    }
                }

            } catch (final BundleException e) {
                e.printStackTrace();
            }
        }
    }

}
