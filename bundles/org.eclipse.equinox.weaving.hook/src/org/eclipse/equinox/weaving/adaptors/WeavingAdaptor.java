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
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.weaving.hooks.WeavingBundleFile;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;

public class WeavingAdaptor implements IWeavingAdaptor {

    private static class ThreadLocalSet extends ThreadLocal {

        public boolean contains(final Object obj) {
            final Set set = (Set) get();
            return set.contains(obj);
        }

        @Override
        protected Object initialValue() {
            return new HashSet();
        }

        public void put(final Object obj) {
            final Set set = (Set) get();
            if (set.contains(obj)) {
                throw new RuntimeException(obj.toString());
            }
            set.add(obj);
        }

        public void remove(final Object obj) {
            final Set set = (Set) get();
            if (!set.contains(obj)) {
                throw new RuntimeException(obj.toString());
            }
            set.remove(obj);
        }
    }

    private static ThreadLocalSet identifyRecursionSet = new ThreadLocalSet();

    private Bundle bundle;

    private ICachingService cachingService;

    private final WeavingAdaptorFactory factory;

    private final Generation generation;

    private boolean initialized = false;

    private final ModuleClassLoader moduleLoader;

    private final String symbolicName;

    private IWeavingService weavingService;

    public WeavingAdaptor(final Generation generation,
            final WeavingAdaptorFactory serviceFactory,
            final IWeavingService weavingService,
            final ICachingService cachingService,
            final ModuleClassLoader classLoader) {
        this.generation = generation;
        this.factory = serviceFactory;
        this.symbolicName = generation.getRevision().getSymbolicName();
        this.moduleLoader = classLoader;
        if (Debug.DEBUG_GENERAL)
            Debug.println("- WeavingAdaptor.WeavingAdaptor() bundle="
                    + symbolicName);
    }

    public CacheEntry findClass(final String name, final URL sourceFileURL) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> WeavingAdaptor.findClass() bundle=" + symbolicName
                    + ", url=" + sourceFileURL + ", name=" + name);
        CacheEntry cacheEntry = null;

        initialize();
        if (cachingService != null) {
            cacheEntry = cachingService
                    .findStoredClass("", sourceFileURL, name);
        }

        if (Debug.DEBUG_CACHE)
            Debug.println("< WeavingAdaptor.findClass() cacheEntry="
                    + cacheEntry);
        return cacheEntry;
    }

    public void initialize() {
        synchronized (this) {
            if (initialized) return;

            this.bundle = generation.getRevision().getBundle();
            if (!identifyRecursionSet.contains(this)) {
                identifyRecursionSet.put(this);

                if (Debug.DEBUG_GENERAL)
                    Debug.println("> WeavingAdaptor.initialize() bundle="
                            + symbolicName + ", moduleLoader=" + moduleLoader);

                if (symbolicName.startsWith("org.aspectj")) {
                    if (Debug.DEBUG_GENERAL)
                        Debug.println("- WeavingAdaptor.initialize() symbolicName="
                                + symbolicName
                                + ", moduleLoader="
                                + moduleLoader);
                } else if (moduleLoader != null) {
                    weavingService = factory.getWeavingService(moduleLoader);
                    cachingService = factory.getCachingService(moduleLoader,
                            bundle, weavingService);
                } else if ((generation.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {

                    final Bundle host = factory.getHost(bundle);
                    if (Debug.DEBUG_GENERAL)
                        Debug.println("- WeavingAdaptor.initialize() symbolicName="
                                + symbolicName + ", host=" + host);

                    final Generation hostGeneration = (Generation) ((ModuleRevision) host
                            .adapt(BundleRevision.class)).getRevisionInfo();
                    final BundleFile bundleFile = hostGeneration
                            .getBundleFile();
                    if (bundleFile instanceof WeavingBundleFile) {
                        final WeavingBundleFile hostFile = (WeavingBundleFile) bundleFile;
                        final WeavingAdaptor hostAdaptor = (WeavingAdaptor) hostFile
                                .getAdaptor();
                        weavingService = hostAdaptor.weavingService;
                        cachingService = factory.getCachingService(
                                hostAdaptor.moduleLoader, bundle,
                                weavingService);
                    }
                } else {
                    if (Debug.DEBUG_GENERAL)
                        Debug.println("W WeavingAdaptor.initialize() symbolicName="
                                + symbolicName + ", baseLoader=" + moduleLoader);
                }
                initialized = true;
                identifyRecursionSet.remove(this);
            }

            if (Debug.DEBUG_GENERAL)
                Debug.println("< WeavingAdaptor.initialize() weavingService="
                        + (weavingService != null) + ", cachingService="
                        + (cachingService != null));
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean storeClass(final String name, final URL sourceFileURL,
            final Class clazz, final byte[] classbytes) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> WeavingAdaptor.storeClass() bundle=" //$NON-NLS-1$
                    + symbolicName + ", url=" + sourceFileURL
                    + ", name="
                    + name + ", clazz=" + clazz);
        boolean stored = false;

        initialize();
        if (cachingService != null) {
            //have we generated a closure? 
            if (weavingService != null
                    && weavingService.generatedClassesExistFor(moduleLoader,
                            name)) {
                //If so we need to ask the cache if its capable of handling generated closures
                if (cachingService.canCacheGeneratedClasses()) {
                    final Map<String, byte[]> generatedClasses = weavingService
                            .getGeneratedClassesFor(name);

                    stored = cachingService.storeClassAndGeneratedClasses("",
                            sourceFileURL, clazz, classbytes, generatedClasses);
                } else {
                    weavingService.flushGeneratedClasses(moduleLoader);
                    if (Debug.DEBUG_CACHE)
                        Debug.println("- WeavingAdaptor.storeClass() generatedClassesExistFor=true"); //$NON-NLS-1$
                }
            } else {
                stored = cachingService.storeClass("", sourceFileURL, clazz, //$NON-NLS-1$
                        classbytes);
                if (!stored) {
                    if (Debug.DEBUG_CACHE)
                        Debug.println("E WeavingAdaptor.storeClass() bundle=" //$NON-NLS-1$
                                + symbolicName + ", name=" + name); //$NON-NLS-1$
                }
            }
        }
        if (Debug.DEBUG_CACHE)
            Debug.println("< WeavingAdaptor.storeClass() stored=" + stored); //$NON-NLS-1$
        return stored;
    }

    @Override
    public String toString() {
        return "WeavingAdaptor[" + symbolicName + "]";
    }

    public byte[] weaveClass(final String name, final byte[] bytes) {
        if (Debug.DEBUG_WEAVE)
            Debug.println("> WeavingAdaptor.weaveClass() bundle="
                    + symbolicName + ", name=" + name + ", bytes="
                    + bytes.length);
        byte[] newBytes = null;

        initialize();
        if (/* shouldWeave(bytes) && */weavingService != null) {
            try {
                newBytes = weavingService.preProcess(name, bytes, moduleLoader);
            } catch (final IOException ex) {
                throw new ClassFormatError(ex.toString());
            }
        }

        if (Debug.DEBUG_WEAVE)
            Debug.println("< WeavingAdaptor.weaveClass() newBytes=" + newBytes);
        return newBytes;
    }

}
