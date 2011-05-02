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
 *   Martin Lippert            caching optimizations     
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.net.URL;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

/**
 * This is a wrapper for bundle files that allows the weaving runtime to create
 * wrapped instances of bundle entry objects.
 * 
 * Those bundle entry objects are used to return class bytes from the cache
 * instead of the bundle itself.
 */
public class WeavingBundleFile extends AbstractWeavingBundleFile {

    private final URL url;

    /**
     * Create a new wrapper for a bundle file
     * 
     * @param adaptorProvider A provider that allows this wrapper to gain access
     *            to the adaptor of this bundle
     * @param bundleFile The wrapped bundle file
     * @throws IOException
     */
    public WeavingBundleFile(final BundleAdaptorProvider adaptorProvider,
            final BundleFile bundleFile) throws IOException {
        super(adaptorProvider, bundleFile);
        this.url = delegate.getBaseFile().toURL();
    }

    @Override
    public BundleEntry getEntry(final String path) {
        if (Debug.DEBUG_BUNDLE)
            Debug.println("> AspectJBundleFile.getEntry() path=" + path
                    + ", url=" + url);
        BundleEntry entry = delegate.getEntry(path);

        if (path.endsWith(".class") && entry != null) {
            final int offset = path.lastIndexOf('.');
            final String name = path.substring(0, offset).replace('/', '.');
            final IWeavingAdaptor adaptor = getAdaptor();
            if (adaptor != null) {
                final CacheEntry cacheEntry = adaptor.findClass(name, url);
                if (cacheEntry == null) {
                    entry = new WeavingBundleEntry(adaptor, entry, url, false);
                    if (Debug.DEBUG_BUNDLE)
                        Debug.println("- AspectJBundleFile.getEntry() path="
                                + path + ", entry=" + entry);
                } else if (cacheEntry.getCachedBytes() != null) {
                    entry = new CachedClassBundleEntry(adaptor, entry, path,
                            cacheEntry.getCachedBytes(), url);
                } else {
                    entry = new WeavingBundleEntry(adaptor, entry, url,
                            cacheEntry.dontWeave());
                }
            }
        } else if (path.endsWith(".class") && entry == null) {
            final int offset = path.lastIndexOf('.');
            final String name = path.substring(0, offset).replace('/', '.');
            final IWeavingAdaptor adaptor = getAdaptor();
            if (adaptor != null) {
                final CacheEntry cacheEntry = adaptor.findClass(name, url);
                if (cacheEntry != null && cacheEntry.getCachedBytes() != null) {
                    entry = new CachedGeneratedClassBundleEntry(adaptor, path,
                            cacheEntry.getCachedBytes(), url);
                }
            }
        }

        if (Debug.DEBUG_BUNDLE)
            Debug.println("< AspectJBundleFile.getEntry() entry=" + entry);
        return entry;
    }

}
