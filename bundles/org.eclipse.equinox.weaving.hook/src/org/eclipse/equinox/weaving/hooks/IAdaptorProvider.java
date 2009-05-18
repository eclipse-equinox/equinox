
package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;

public interface IAdaptorProvider {

    public IWeavingAdaptor getAdaptor(long bundleID);

    public IWeavingAdaptor getHostBundleAdaptor(long bundleID);

    public void resetAdaptor(long bundleID);

}
