/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol.bundleentry;

import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.framework.internal.core.Bundle;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;
import org.eclipse.osgi.framework.internal.defaultadaptor.BundleEntry;
import org.eclipse.osgi.framework.internal.defaultadaptor.DefaultBundleData;
import org.osgi.framework.BundleContext;

/**
 * URLStreamHandler the bundleentry protocol.
 */

public class Handler extends BundleResourceHandler
{

    /**
     * Constructor for a bundle protocol resource URLStreamHandler.
     */
    public Handler()
    {
    	super();
    }

    public Handler(BundleEntry bundleEntry, BundleContext context) {
    	super(bundleEntry,context);
    }

    protected BundleEntry findBundleEntry(URL url, Bundle bundle) throws IOException
    {
		DefaultBundleData bundleData = (DefaultBundleData) bundle.getBundleData();
		return bundleData.getBaseBundleFile().getEntry(url.getPath());

	}

}
