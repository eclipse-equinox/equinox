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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;

public class CachedGeneratedClassBundleEntry extends BundleEntry {

    private final IWeavingAdaptor adaptor;

    private final URL bundleFileURL;

    private final byte[] bytes;

    private final String name;

    public CachedGeneratedClassBundleEntry(final IWeavingAdaptor adaptor,
            final String path, final byte[] cachedBytes, final URL url) {
        this.adaptor = adaptor;
        this.bundleFileURL = url;
        this.bytes = cachedBytes;
        this.name = path;
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
        final ByteArrayInputStream result = new ByteArrayInputStream(bytes);
        return result;
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
