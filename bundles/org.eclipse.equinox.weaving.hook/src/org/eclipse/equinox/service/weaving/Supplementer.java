/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Martin Lippert            supplementing mechanism reworked     
 *   Martin Lippert            fragment handling fixed
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;

/**
 * A supplementer object is created for every bundle that contains one or many
 * of the supplementer headers in its header.
 * 
 * The corresponding supplementer object contains the information which headers
 * the bundle defines and which bundles it supplements in the running system.
 * 
 * @author Martin Lippert
 */
public class Supplementer {

    private final ManifestElement[] supplementBundle;

    private final Set<Bundle> supplementedBundles; // elements of type Bundle

    private final Bundle supplementer;

    private final Bundle supplementerHost;

    private final ManifestElement[] supplementExporter;

    private final ManifestElement[] supplementImporter;

    /**
     * Creates a supplementer object for the given bundle.
     * 
     * @param bundle The bundle that defines the supplementer headers
     * @param bundleHost The host bundle of the supplementer bundle, if the
     *            bundle is a fragment, otherwise null
     * @param supplementBundle The parsed manifest headers defined for
     *            Eclipse-SupplementBundle
     * @param supplementImporter The parsed manifest headers defined for
     *            Eclipse-SupplementImporter
     * @param supplementExporter The parsed manifest headers defined for
     *            Eclipse-SupplementExporter
     */
    public Supplementer(final Bundle bundle, final Bundle bundleHost,
            final ManifestElement[] supplementBundle,
            final ManifestElement[] supplementImporter,
            final ManifestElement[] supplementExporter) {
        this.supplementer = bundle;
        this.supplementerHost = bundleHost != null ? bundleHost : bundle;
        this.supplementBundle = supplementBundle;
        this.supplementImporter = supplementImporter;
        this.supplementExporter = supplementExporter;
        this.supplementedBundles = new HashSet<Bundle>();
    }

    /**
     * Add a bundle to the list of supplemented bundles
     * 
     * @param supplementedBundle The bundle that is supplemented by this
     *            supplementer
     */
    public void addSupplementedBundle(final Bundle supplementedBundle) {
        this.supplementedBundles.add(supplementedBundle);
    }

    /**
     * Gives information about which bundles are currently supplemented by this
     * supplementer
     * 
     * @return The currently supplemented bundles
     */
    public Bundle[] getSupplementedBundles() {
        return supplementedBundles.toArray(new Bundle[supplementedBundles
                .size()]);
    }

    /**
     * Returns the bundle that defines the supplementer headers (this
     * supplementer object belongs to)
     * 
     * @return The bundle object this supplementer belongs to
     */
    public Bundle getSupplementerBundle() {
        return supplementer;
    }

    /**
     * Returns the host of the supplementer bundle, if it is a fragment,
     * otherwise this returns the same as getSupplementerBundle()
     * 
     * @return The host bundle this supplementer belongs to
     */
    public Bundle getSupplementerHost() {
        return supplementerHost;
    }

    /**
     * The symbolic name of the supplementer bundle
     * 
     * @return The symbolic name of the supplementer bundle
     */
    public String getSymbolicName() {
        return supplementer.getSymbolicName();
    }

    /**
     * Provides information about whether a given bundle is supplemented by this
     * supplementer or not
     * 
     * @param bundle The bundle that might be supplemented by this supplementer
     * @return true, if the bundle is supplemented by this supplementer,
     *         otherwise false
     */
    public boolean isSupplemented(final Bundle bundle) {
        return supplementedBundles.contains(bundle);
    }

    /**
     * Checks if the given export-package header definitions matches the
     * supplement-exporter definitions of this supplementer
     * 
     * @param exports The headers to check for matching against this
     *            supplementer
     * @return true, if this supplementer matches against the given
     *         export-package headers
     */
    public boolean matchesSupplementExporter(final ManifestElement[] exports) {
        boolean matches = false;

        if (supplementExporter != null)
            for (int i = 0; !matches && i < supplementExporter.length; i++) {
                final ManifestElement supplementExport = supplementExporter[i];
                for (int j = 0; !matches && j < exports.length; j++) {
                    final ManifestElement exportPackage = exports[j];
                    if (supplementExport.getValue().equals(
                            exportPackage.getValue())) matches = true;
                }
            }

        return matches;
    }

    /**
     * Checks if the given import-package header definitions matches the
     * supplement-importer definitions of this supplementer
     * 
     * @param imports The headers to check for matching against this
     *            supplementer
     * @return true, if this supplementer matches against the given
     *         import-package headers
     */
    public boolean matchesSupplementImporter(final ManifestElement[] imports) {
        boolean matches = false;

        if (supplementImporter != null)
            for (int i = 0; !matches && i < supplementImporter.length; i++) {
                final ManifestElement supplementImport = supplementImporter[i];
                for (int j = 0; !matches && j < imports.length; j++) {
                    final ManifestElement importPackage = imports[j];
                    if (supplementImport.getValue().equals(
                            importPackage.getValue())) matches = true;
                }
            }

        return matches;
    }

    /**
     * Checks if the given bundle symbolic name definition matches the
     * supplement-bundle definition of this supplementer
     * 
     * @param symbolicName The symbolic name of the bundle that shoudl be
     *            checked
     * @return true, if this supplementer matches against the given bundle
     *         symbolic name
     */
    public boolean matchSupplementer(final String symbolicName) {
        boolean matches = false;

        if (supplementBundle != null)
            for (int i = 0; !matches && i < supplementBundle.length; i++) {
                final ManifestElement bundle = supplementBundle[i];
                if (equals_wild(bundle.getValue(), symbolicName))
                    matches = true;
            }

        return matches;
    }

    /**
     * Removes the given bundle from the set of supplemented bundles (that are
     * supplemented by this supplementer)
     * 
     * @param supplementedBundle The bundle that is no longer supplemented by
     *            this supplementer
     */
    public void removeSupplementedBundle(final Bundle supplementedBundle) {
        this.supplementedBundles.remove(supplementedBundle);
    }

    //knibb
    //test if two Strings are equal
    //with wild card support - only supports strings ending in *
    private boolean equals_wild(final String input, final String match) {
        if (input.equals(match)) {
            //its a match so just return true
            return true;
        }
        if (input.endsWith("*") == false) {
            //no wildcards are being used here
            return false;
        }
        final String wild_in = input.substring(0, input.length() - 1);
        if (match.startsWith(wild_in)) return true;

        return false;
    }

}
