/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public class SupplementBundleListener implements SynchronousBundleListener {

    private final SupplementerRegistry supplementerRegistry;

    public SupplementBundleListener(
            final SupplementerRegistry supplementerRegistry) {
        this.supplementerRegistry = supplementerRegistry;
    }

    public void bundleChanged(final BundleEvent event) {
        final Bundle bundle = event.getBundle();
        if (event.getType() == BundleEvent.INSTALLED) {
            supplementerRegistry.addBundle(bundle);
        } else if (event.getType() == BundleEvent.UNINSTALLED) {
            supplementerRegistry.removeBundle(bundle);
        } else if (event.getType() == BundleEvent.UPDATED) {
            System.err.println("bundle update: "
                    + event.getBundle().getBundleId());

            supplementerRegistry.removeBundle(bundle);
            supplementerRegistry.addBundle(bundle);
            //            supplementerRegistry.getPackageAdmin().refreshPackages(null);
        }
    }

}
