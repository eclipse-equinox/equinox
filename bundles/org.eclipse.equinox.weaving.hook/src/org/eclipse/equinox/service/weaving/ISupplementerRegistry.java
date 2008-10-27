
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

    public PackageAdmin getPackageAdmin();

    public Bundle[] getSupplementers(final Bundle bundle);

    public Bundle[] getSupplementers(final long bundleID);

    public List getSupplementers(final String symbolicName,
            final ManifestElement[] imports, final ManifestElement[] exports);

    public void removeBundle(final Bundle bundle);

    public void setBundleContext(final BundleContext context);

    public void setPackageAdmin(final PackageAdmin packageAdmin);

    /**
     * Refreshes the given bundles
     * 
     * @param bundles The bundles to refresh
     */
    public void refreshBundles(final Bundle[] bundles);

}
