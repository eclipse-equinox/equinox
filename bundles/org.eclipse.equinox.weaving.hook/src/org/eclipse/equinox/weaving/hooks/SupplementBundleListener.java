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

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public class SupplementBundleListener implements SynchronousBundleListener {

    private final ISupplementerRegistry supplementerRegistry;

    public SupplementBundleListener(
            final ISupplementerRegistry supplementerRegistry) {
        this.supplementerRegistry = supplementerRegistry;
    }

    public void bundleChanged(final BundleEvent event) {
        final Bundle bundle = event.getBundle();
        if (event.getType() == BundleEvent.RESOLVED) {
            supplementerRegistry.addBundle(bundle);
        } else if (event.getType() == BundleEvent.UNRESOLVED) {
            supplementerRegistry.removeBundle(bundle);
        }
    }

}
