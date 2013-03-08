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
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

public abstract class AbstractWeavingBundleFile extends BundleFile {

    private final BundleAdaptorProvider adaptorProvider;

    protected BundleFile delegate;

    public AbstractWeavingBundleFile(
            final BundleAdaptorProvider adaptorProvider,
            final BundleFile bundleFile) {
        super(bundleFile.getBaseFile());
        this.adaptorProvider = adaptorProvider;
        this.delegate = bundleFile;
    }

    /**
     * @see BundleFile#close()
     */
    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * @see BundleFile#containsDir(java.lang.String)
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
     * @see BundleFile#getBaseFile()
     */
    @Override
    public File getBaseFile() {
        final File baseFile = delegate.getBaseFile();
        return baseFile;
    }

    /**
     * @see BundleFile#getEntry(java.lang.String)
     */
    @Override
    public BundleEntry getEntry(final String path) {
        return delegate.getEntry(path);
    }

    /**
     * @see BundleFile#getEntryPaths(java.lang.String)
     */
    @Override
    public Enumeration<String> getEntryPaths(final String path) {
        return delegate.getEntryPaths(path);
    }

    /**
     * @see BundleFile#getFile(java.lang.String, boolean)
     */
    @Override
    public File getFile(final String path, final boolean nativeCode) {
        return delegate.getFile(path, nativeCode);
    }

    /**
     * @see BundleFile#getResourceURL(java.lang.String,
     *      org.eclipse.osgi.container.Module, int)
     */
    @Override
    public URL getResourceURL(final String path, final Module hostModule,
            final int index) {
        return delegate.getResourceURL(path, hostModule, index);
    }

    /**
     * @see BundleFile#open()
     */
    @Override
    public void open() throws IOException {
        delegate.open();
    }

}
