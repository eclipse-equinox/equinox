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
 *   Martin Lippert            minor changes and bugfixes     
 *   Martin Lippert            splitted into different types of bundle entries
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;

public class WeavingBundleEntry extends BundleEntry {

    private final IWeavingAdaptor adaptor;

    private final URL bundleFileURL;

    private final BundleEntry delegate;

    private final boolean dontWeave;

    public WeavingBundleEntry(final IWeavingAdaptor aspectjAdaptor,
            final BundleEntry delegate, final URL url, final boolean dontWeave) {
        this.adaptor = aspectjAdaptor;
        this.bundleFileURL = url;
        this.delegate = delegate;
        this.dontWeave = dontWeave;
    }

    public boolean dontWeave() {
        return dontWeave;
    }

    public IWeavingAdaptor getAdaptor() {
        return adaptor;
    }

    public URL getBundleFileURL() {
        return bundleFileURL;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return delegate.getBytes();
    }

    @Override
    public URL getFileURL() {
        return delegate.getFileURL();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public URL getLocalURL() {
        return delegate.getLocalURL();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public long getTime() {
        return delegate.getTime();
    }

}
