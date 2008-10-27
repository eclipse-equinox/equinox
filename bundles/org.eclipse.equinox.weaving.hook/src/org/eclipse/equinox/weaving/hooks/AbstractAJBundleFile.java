/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

public abstract class AbstractAJBundleFile extends BundleFile {

    protected BundleFile delegate;

    private final BundleAdaptorProvider adaptorProvider;

    public AbstractAJBundleFile(final BundleAdaptorProvider adaptorProvider,
            final BundleFile bundleFile) {
        super(bundleFile.getBaseFile());
        this.adaptorProvider = adaptorProvider;
        this.delegate = bundleFile;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean containsDir(final String dir) {
        return delegate.containsDir(dir);
    }

    public IAspectJAdaptor getAdaptor() {
        return this.adaptorProvider.getAdaptor();
    }

    @Override
    public File getBaseFile() {
        final File baseFile = delegate.getBaseFile();
        return baseFile;
    }

    @Override
    public BundleEntry getEntry(final String path) {
        return delegate.getEntry(path);
    }

    @Override
    public Enumeration getEntryPaths(final String path) {
        return delegate.getEntryPaths(path);
    }

    @Override
    public File getFile(final String path, final boolean nativeCode) {
        return delegate.getFile(path, nativeCode);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public URL getResourceURL(final String path, final long hostBundleID) {
        return delegate.getResourceURL(path, hostBundleID);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public URL getResourceURL(final String path, final long hostBundleID,
            final int index) {
        return delegate.getResourceURL(path, hostBundleID, index);
    }

    @Override
    public void open() throws IOException {
        delegate.open();
    }

}
