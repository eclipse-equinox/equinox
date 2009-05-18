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

public class Supplementer {

    private final ManifestElement[] supplementBundle;

    private final Set<Bundle> supplementedBundles; // elements of type Bundle

    private final Bundle supplementer;

    private final Bundle supplementerHost;

    private final ManifestElement[] supplementExporter;

    private final ManifestElement[] supplementImporter;

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

    public void addSupplementedBundle(final Bundle supplementedBundle) {
        this.supplementedBundles.add(supplementedBundle);
    }

    public Bundle[] getSupplementedBundles() {
        return supplementedBundles.toArray(new Bundle[supplementedBundles
                .size()]);
    }

    public Bundle getSupplementerBundle() {
        return supplementer;
    }

    public Bundle getSupplementerHost() {
        return supplementerHost;
    }

    public String getSymbolicName() {
        return supplementer.getSymbolicName();
    }

    public boolean isSupplemented(final Bundle bundle) {
        return supplementedBundles.contains(bundle);
    }

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
