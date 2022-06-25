/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    private static class ThreadLocalSet extends ThreadLocal<Set<Object>> {

        public boolean contains(final Object obj) {
            final Set<Object> set = get();
            return set.contains(obj);
        }

        @Override
        protected Set<Object> initialValue() {
            return new HashSet<>();
        }

        public void put(final Object obj) {
            final Set<Object> set = get();
            if (set.contains(obj)) {
                throw new RuntimeException(obj.toString());
            }
            set.add(obj);
        }

        public void remove(final Object obj) {
            final Set<?> set = get();
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
            Debug.println("- WeavingAdaptor.WeavingAdaptor() bundle=" //$NON-NLS-1$
                    + symbolicName);
    }

    @Override
    public CacheEntry findClass(final String name, final URL sourceFileURL) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> WeavingAdaptor.findClass() bundle=" + symbolicName //$NON-NLS-1$
                    + ", url=" + sourceFileURL + ", name=" + name); //$NON-NLS-1$ //$NON-NLS-2$
        CacheEntry cacheEntry = null;

        initialize();
        if (cachingService != null) {
            cacheEntry = cachingService
                    .findStoredClass("", sourceFileURL, name); //$NON-NLS-1$
        }

        if (Debug.DEBUG_CACHE)
            Debug.println("< WeavingAdaptor.findClass() cacheEntry=" //$NON-NLS-1$
                    + cacheEntry);
        return cacheEntry;
    }

    @Override
    public void initialize() {
        synchronized (this) {
            if (initialized) return;

            this.bundle = generation.getRevision().getBundle();
            if (!identifyRecursionSet.contains(this)) {
                identifyRecursionSet.put(this);

                if (Debug.DEBUG_GENERAL)
                    Debug.println("> WeavingAdaptor.initialize() bundle=" //$NON-NLS-1$
                            + symbolicName + ", moduleLoader=" + moduleLoader); //$NON-NLS-1$

                if (symbolicName != null && symbolicName.startsWith("org.aspectj")) { //$NON-NLS-1$
                    if (Debug.DEBUG_GENERAL)
                        Debug.println("- WeavingAdaptor.initialize() symbolicName=" //$NON-NLS-1$
                                + symbolicName + ", moduleLoader=" //$NON-NLS-1$
                                + moduleLoader);
                } else if (moduleLoader != null) {
                    weavingService = factory.getWeavingService(moduleLoader);
                    cachingService = factory.getCachingService(moduleLoader,
                            bundle, weavingService);
                } else if ((generation.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {

                    final Bundle host = factory.getHost(bundle);
                    if (Debug.DEBUG_GENERAL)
                        Debug.println("- WeavingAdaptor.initialize() symbolicName=" //$NON-NLS-1$
                                + symbolicName + ", host=" + host); //$NON-NLS-1$

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
                        Debug.println("W WeavingAdaptor.initialize() symbolicName=" //$NON-NLS-1$
                                + symbolicName + ", baseLoader=" + moduleLoader); //$NON-NLS-1$
                }
                initialized = true;
                identifyRecursionSet.remove(this);
            }

            if (Debug.DEBUG_GENERAL)
                Debug.println("< WeavingAdaptor.initialize() weavingService=" //$NON-NLS-1$
                        + (weavingService != null) + ", cachingService=" //$NON-NLS-1$
                        + (cachingService != null));
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean storeClass(final String name, final URL sourceFileURL,
            final Class<?> clazz, final byte[] classbytes) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> WeavingAdaptor.storeClass() bundle=" //$NON-NLS-1$
                    + symbolicName + ", url=" + sourceFileURL //$NON-NLS-1$
                    + ", name=" //$NON-NLS-1$
                    + name + ", clazz=" + clazz); //$NON-NLS-1$
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

                    stored = cachingService.storeClassAndGeneratedClasses("", //$NON-NLS-1$
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
        return "WeavingAdaptor[" + symbolicName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public byte[] weaveClass(final String name, final byte[] bytes) {
        if (Debug.DEBUG_WEAVE)
            Debug.println("> WeavingAdaptor.weaveClass() bundle=" //$NON-NLS-1$
                    + symbolicName + ", name=" + name + ", bytes=" //$NON-NLS-1$ //$NON-NLS-2$
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
            Debug.println("< WeavingAdaptor.weaveClass() newBytes=" + newBytes); //$NON-NLS-1$
        return newBytes;
    }

}
