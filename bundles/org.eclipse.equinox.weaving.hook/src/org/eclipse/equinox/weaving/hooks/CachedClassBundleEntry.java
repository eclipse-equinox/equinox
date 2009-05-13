/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;

public class CachedClassBundleEntry extends BundleEntry {

    private final IWeavingAdaptor adaptor;

    private final URL bundleFileURL;

    private final byte[] bytes;

    private final BundleEntry delegate;

    private final String name;

    public CachedClassBundleEntry(final IWeavingAdaptor aspectjAdaptor,
            final BundleEntry delegate, final String name, final byte[] bytes,
            final URL url) {
        this.adaptor = aspectjAdaptor;
        this.bundleFileURL = url;
        this.delegate = delegate;
        this.name = name;
        this.bytes = bytes;
    }

    public boolean dontWeave() {
        return true;
    }

    public IWeavingAdaptor getAdaptor() {
        return adaptor;
    }

    public URL getBundleFileURL() {
        return bundleFileURL;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return bytes;
    }

    @Override
    public URL getFileURL() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (delegate == null) {
            System.err.println("error in: " + name);
        }
        return delegate.getInputStream();
    }

    @Override
    public URL getLocalURL() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public long getTime() {
        return 0;
    }

}
