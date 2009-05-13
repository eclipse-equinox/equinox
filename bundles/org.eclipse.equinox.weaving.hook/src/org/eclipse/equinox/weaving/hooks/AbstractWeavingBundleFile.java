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
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

public abstract class AbstractWeavingBundleFile extends BundleFile {

    protected BundleFile delegate;

    private final BundleAdaptorProvider adaptorProvider;

    public AbstractWeavingBundleFile(final BundleAdaptorProvider adaptorProvider,
            final BundleFile bundleFile) {
        super(bundleFile.getBaseFile());
        this.adaptorProvider = adaptorProvider;
        this.delegate = bundleFile;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#close()
     */
    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#containsDir(java.lang.String)
     */
    @Override
    public boolean containsDir(final String dir) {
        return delegate.containsDir(dir);
    }

    /**
     * @return
     */
    public IWeavingAdaptor getAdaptor() {
        return this.adaptorProvider.getAdaptor();
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getBaseFile()
     */
    @Override
    public File getBaseFile() {
        final File baseFile = delegate.getBaseFile();
        return baseFile;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getEntry(java.lang.String)
     */
    @Override
    public BundleEntry getEntry(final String path) {
        return delegate.getEntry(path);
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getEntryPaths(java.lang.String)
     */
    @Override
    public Enumeration getEntryPaths(final String path) {
        return delegate.getEntryPaths(path);
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#getFile(java.lang.String,
     *      boolean)
     */
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

    /**
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleFile#open()
     */
    @Override
    public void open() throws IOException {
        delegate.open();
    }

}
