/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked     
 *   Heiko Seeberger           Enhancements for service dynamics     
 *   Martin Lippert            fragment handling fixed
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

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

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.Supplementer;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

public class SupplementerRegistry implements ISupplementerRegistry {

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

    private final IAdaptorProvider adaptorProvider;

    private BundleContext context;

    private final Set<String> dontWeaveTheseBundles; // elements of type String (symbolic name of bundle)

    private PackageAdmin packageAdmin;

    private final Map<String, Supplementer> supplementers; // keys of type String (symbolic name of supplementer bundle), values of type Supplementer

    private final Map<Long, Supplementer[]> supplementersByBundle;

    public SupplementerRegistry(final IAdaptorProvider adaptorProvider) {
        this.adaptorProvider = adaptorProvider;

        this.supplementers = new HashMap<String, Supplementer>();
        this.supplementersByBundle = new HashMap<Long, Supplementer[]>();
        this.dontWeaveTheseBundles = new HashSet<String>();

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

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#addBundle(org.osgi.framework.Bundle)
     */
    public void addBundle(final Bundle bundle) {
        // First analyze which supplementers already exists for this bundle
        addSupplementedBundle(bundle);

        // Second analyze if this bundle itself is a supplementer
        addSupplementer(bundle, true);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#addSupplementedBundle(org.osgi.framework.Bundle)
     */
    public void addSupplementedBundle(final Bundle bundle) {
        try {
            final Dictionary<?, ?> manifest = bundle.getHeaders();
            final ManifestElement[] imports = ManifestElement.parseHeader(
                    Constants.IMPORT_PACKAGE, (String) manifest
                            .get(Constants.IMPORT_PACKAGE));
            final ManifestElement[] exports = ManifestElement.parseHeader(
                    Constants.EXPORT_PACKAGE, (String) manifest
                            .get(Constants.EXPORT_PACKAGE));
            final List<Supplementer> supplementers = getMatchingSupplementers(
                    bundle.getSymbolicName(), imports, exports);
            if (supplementers.size() > 0) {
                this.addSupplementedBundle(bundle, supplementers);
            }
            this.supplementersByBundle.put(bundle.getBundleId(), supplementers
                    .toArray(new Supplementer[supplementers.size()]));
        } catch (final BundleException e) {
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#addSupplementer(org.osgi.framework.Bundle,
     *      boolean)
     */
    public void addSupplementer(final Bundle bundle, final boolean updateBundles) {
        try {
            final Dictionary<?, ?> manifest = bundle.getHeaders();
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

                final Bundle[] hosts = this.packageAdmin.getHosts(bundle);
                final Bundle host = hosts != null && hosts.length == 1 ? hosts[0]
                        : null;

                final Supplementer newSupplementer = new Supplementer(bundle,
                        host, supplementBundle, supplementImporter,
                        supplementExporter);

                this.supplementers.put(bundle.getSymbolicName(),
                        newSupplementer);
                if (updateBundles) {
                    resupplementInstalledBundles(newSupplementer);
                }
            }
        } catch (final BundleException e) {
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#getMatchingSupplementers(java.lang.String,
     *      org.eclipse.osgi.util.ManifestElement[],
     *      org.eclipse.osgi.util.ManifestElement[])
     */
    public List<Supplementer> getMatchingSupplementers(
            final String symbolicName, final ManifestElement[] imports,
            final ManifestElement[] exports) {
        List<Supplementer> result = Collections.emptyList();

        if (supplementers.size() > 0
                && !this.dontWeaveTheseBundles.contains(symbolicName)) {
            result = new LinkedList<Supplementer>();
            for (final Iterator<Supplementer> i = supplementers.values()
                    .iterator(); i.hasNext();) {
                final Supplementer supplementer = i.next();
                if (isSupplementerMatching(symbolicName, imports, exports,
                        supplementer)) {
                    result.add(supplementer);
                }
            }
        }

        return result;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#getPackageAdmin()
     */
    public PackageAdmin getPackageAdmin() {
        return packageAdmin;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#getSupplementers(org.osgi.framework.Bundle)
     */
    public Supplementer[] getSupplementers(final Bundle bundle) {
        return getSupplementers(bundle.getBundleId());
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#getSupplementers(long)
     */
    public Supplementer[] getSupplementers(final long bundleID) {
        if (supplementersByBundle.containsKey(bundleID)) {
            return supplementersByBundle.get(bundleID);
        } else {
            return new Supplementer[0];
        }
    }

    /**
     * Refreshes the given bundles
     * 
     * @param bundles The bundles to refresh
     */
    public void refreshBundles(final Bundle[] bundles) {
        if (this.packageAdmin != null) {
            if (AbstractWeavingHook.verbose) {
                for (int i = 0; i < bundles.length; i++) {
                    System.out.println("refresh bundle: "
                            + bundles[i].getSymbolicName());
                }
            }

            this.packageAdmin.refreshPackages(bundles);
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#removeBundle(org.osgi.framework.Bundle)
     */
    public void removeBundle(final Bundle bundle) {
        // if this bundle is itself supplemented by others, remove the bundle from those lists
        removeSupplementedBundle(bundle);
        this.supplementersByBundle.remove(bundle.getBundleId());

        this.adaptorProvider.resetAdaptor(bundle.getBundleId());

        // if this bundle supplements other bundles, remove this supplementer and update the other bundles
        if (supplementers.containsKey(bundle.getSymbolicName())) {

            // remove the supplementer from the list of supplementers
            final Supplementer supplementer = supplementers.get(bundle
                    .getSymbolicName());
            supplementers.remove(bundle.getSymbolicName());
            if (AbstractWeavingHook.verbose)
                System.err
                        .println("[org.eclipse.equinox.weaving.hook] info removing supplementer " //$NON-NLS-1$
                                + bundle.getSymbolicName());

            // refresh bundles that where supplemented by this bundle
            final Bundle[] supplementedBundles = supplementer
                    .getSupplementedBundles();
            if (supplementedBundles != null && supplementedBundles.length > 0) {
                final List<Bundle> bundlesToRefresh = new ArrayList<Bundle>(
                        supplementedBundles.length);
                for (final Bundle bundleToRefresh : supplementedBundles) {
                    if (this.adaptorProvider.getAdaptor(bundleToRefresh
                            .getBundleId()) != null) {
                        bundlesToRefresh.add(bundleToRefresh);
                    }
                }

                if (bundlesToRefresh.size() > 0) {
                    refreshBundles(bundlesToRefresh
                            .toArray(new Bundle[bundlesToRefresh.size()]));
                }
            }

            // remove this supplementer from the list of supplementers per other bundle
            for (int i = 0; i < supplementedBundles.length; i++) {
                final long bundleId = supplementedBundles[i].getBundleId();
                final List<Supplementer> supplementerList = new ArrayList<Supplementer>(
                        Arrays.asList(this.supplementersByBundle.get(bundleId)));
                supplementerList.remove(supplementer);
                this.supplementersByBundle.put(bundleId, supplementerList
                        .toArray(new Supplementer[0]));
            }
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#setBundleContext(org.osgi.framework.BundleContext)
     */
    public void setBundleContext(final BundleContext context) {
        this.context = context;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ISupplementerRegistry#setPackageAdmin(org.osgi.service.packageadmin.PackageAdmin)
     */
    public void setPackageAdmin(final PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    private void addSupplementedBundle(final Bundle supplementedBundle,
            final List<Supplementer> supplementers) {
        for (final Iterator<Supplementer> iterator = supplementers.iterator(); iterator
                .hasNext();) {
            final Supplementer supplementer = iterator.next();
            supplementer.addSupplementedBundle(supplementedBundle);
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

    private void removeSupplementedBundle(final Bundle bundle) {
        for (final Iterator<Supplementer> iterator = this.supplementers
                .values().iterator(); iterator.hasNext();) {
            final Supplementer supplementer = iterator.next();
            supplementer.removeSupplementedBundle(bundle);
        }
    }

    private void resupplementInstalledBundles(final Supplementer supplementer) {
        final Bundle[] installedBundles = context.getBundles();

        final List<Bundle> bundlesToRefresh = new ArrayList<Bundle>();

        for (int i = 0; i < installedBundles.length; i++) {
            try {
                final Bundle bundle = installedBundles[i];

                // skip the bundle itself, just resupplement already installed bundles
                if (bundle.getSymbolicName().equals(
                        supplementer.getSymbolicName())) {
                    continue;
                }

                // skip bundles that should not be woven
                if (dontWeaveTheseBundles.contains(bundle.getSymbolicName())) {
                    continue;
                }

                // find out which of the installed bundles matches the new supplementer
                final Dictionary<?, ?> manifest = bundle.getHeaders();
                final ManifestElement[] imports = ManifestElement.parseHeader(
                        Constants.IMPORT_PACKAGE, (String) manifest
                                .get(Constants.IMPORT_PACKAGE));
                final ManifestElement[] exports = ManifestElement.parseHeader(
                        Constants.EXPORT_PACKAGE, (String) manifest
                                .get(Constants.EXPORT_PACKAGE));

                if (isSupplementerMatching(bundle.getSymbolicName(), imports,
                        exports, supplementer)) {
                    if (this.adaptorProvider.getAdaptor(bundle.getBundleId()) != null) {
                        bundlesToRefresh.add(bundle);
                    } else {
                        supplementer.addSupplementedBundle(bundle);
                        final Supplementer[] existingSupplementers = supplementersByBundle
                                .get(bundle.getBundleId());
                        List<Supplementer> enhancedSupplementerList = null;
                        if (existingSupplementers != null) {
                            enhancedSupplementerList = new ArrayList<Supplementer>(
                                    Arrays.asList(existingSupplementers));
                        } else {
                            enhancedSupplementerList = new ArrayList<Supplementer>();
                        }
                        if (!enhancedSupplementerList.contains(supplementer)) {
                            enhancedSupplementerList.add(supplementer);
                        }

                        this.supplementersByBundle.put(bundle.getBundleId(),
                                enhancedSupplementerList
                                        .toArray(new Supplementer[0]));
                    }
                }

            } catch (final BundleException e) {
                e.printStackTrace();
            }
        }

        if (bundlesToRefresh.size() > 0) {
            final Bundle[] bundles = bundlesToRefresh
                    .toArray(new Bundle[bundlesToRefresh.size()]);

            refreshBundles(bundles);
        }
    }
}
