/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;

import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

public class BaseWeavingBundleFile extends WeavingBundleFile {

    public BaseWeavingBundleFile(final BundleAdaptorProvider adaptorProvider,
            final BundleFile bundleFile) throws IOException {
        super(adaptorProvider, bundleFile);
    }
}
