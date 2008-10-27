
package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;

public interface IAdaptorProvider {

    public IAspectJAdaptor getAdaptor(long bundleID);

    public IAspectJAdaptor getHostBundleAdaptor(long bundleID);

}
