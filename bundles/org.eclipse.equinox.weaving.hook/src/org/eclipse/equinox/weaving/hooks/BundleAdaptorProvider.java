/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
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

    public IWeavingAdaptor getAdaptor() {
        if (this.baseData.getBundle() instanceof BundleFragment) {
            return this.adaptorProvider.getHostBundleAdaptor(this.baseData
                    .getBundleID());
        } else {
            return this.adaptorProvider.getAdaptor(this.baseData.getBundleID());
        }
    }

}
