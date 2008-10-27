
package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.internal.core.BundleFragment;

public class BundleAdaptorProvider {

    private final IAdaptorProvider adaptorProvider;

    private final BaseData baseData;

    public BundleAdaptorProvider(final BaseData data,
            final IAdaptorProvider adaptorProvider) {
        this.baseData = data;
        this.adaptorProvider = adaptorProvider;
    }

    public IAspectJAdaptor getAdaptor() {
        if (this.baseData.getBundle() instanceof BundleFragment) {
            return this.adaptorProvider.getHostBundleAdaptor(this.baseData
                    .getBundleID());
        } else {
            return this.adaptorProvider.getAdaptor(this.baseData.getBundleID());
        }
    }

}
