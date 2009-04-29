/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert            initial implementation     
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.util.List;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

public interface ISupplementerRegistry {

    public void addBundle(final Bundle bundle);

    public void addSupplementedBundle(final Bundle bundle);

    public void addSupplementer(final Bundle bundle, final boolean updateBundles);

    public List<Supplementer> getMatchingSupplementers(
            final String symbolicName, final ManifestElement[] imports,
            final ManifestElement[] exports);

    public PackageAdmin getPackageAdmin();

    public Supplementer[] getSupplementers(final Bundle bundle);

    public Supplementer[] getSupplementers(final long bundleID);

    /**
     * Refreshes the given bundles
     * 
     * @param bundles The bundles to refresh
     */
    public void refreshBundles(final Bundle[] bundles);

    public void removeBundle(final Bundle bundle);

    public void setBundleContext(final BundleContext context);

    public void setPackageAdmin(final PackageAdmin packageAdmin);

}
