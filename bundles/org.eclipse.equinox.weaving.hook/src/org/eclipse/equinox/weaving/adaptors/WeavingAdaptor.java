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
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.framework.internal.core.BundleFragment;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.osgi.framework.Bundle;

public class WeavingAdaptor implements IWeavingAdaptor {

    private static class ThreadLocalSet extends ThreadLocal {

        public boolean contains(final Object obj) {
            final Set set = (Set) get();
            return set.contains(obj);
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

        @Override
        protected Object initialValue() {
            return new HashSet();
        }
    }

    private static ThreadLocalSet identifyRecursionSet = new ThreadLocalSet();

    private BaseClassLoader baseLoader;

    private Bundle bundle;

    private ICachingService cachingService;

    private final BaseData data;

    private final WeavingAdaptorFactory factory;

    private boolean initialized = false;

    private String symbolicName;

    private IWeavingService weavingService;

    public WeavingAdaptor(final BaseData baseData,
            final WeavingAdaptorFactory serviceFactory,
            final BaseClassLoader baseClassLoader,
            final IWeavingService weavingService,
            final ICachingService cachingService) {
        this.data = baseData;
        this.factory = serviceFactory;
        this.symbolicName = baseData.getLocation();
        if (Debug.DEBUG_GENERAL)
            Debug.println("- AspectJAdaptor.AspectJAdaptor() bundle="
                    + symbolicName);
    }

    public CacheEntry findClass(final String name, final URL sourceFileURL) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> AspectJAdaptor.findClass() bundle=" + symbolicName
                    + ", url=" + sourceFileURL + ", name=" + name);
        CacheEntry cacheEntry = null;

        initialize();
        if (cachingService != null) {
            cacheEntry = cachingService
                    .findStoredClass("", sourceFileURL, name);
        }

        if (Debug.DEBUG_CACHE)
            Debug.println("< AspectJAdaptor.findClass() cacheEntry="
                    + cacheEntry);
        return cacheEntry;
    }

    public void initialize() {
        synchronized (this) {
            if (initialized) return;

            this.bundle = data.getBundle();
            this.symbolicName = data.getSymbolicName();
            if (!identifyRecursionSet.contains(this)) {
                identifyRecursionSet.put(this);

                if (Debug.DEBUG_GENERAL)
                    Debug.println("> AspectJAdaptor.initialize() bundle="
                            + symbolicName + ", baseLoader=" + baseLoader);

                if (symbolicName.startsWith("org.aspectj")) {
                    if (Debug.DEBUG_GENERAL)
                        Debug
                                .println("- AspectJAdaptor.initialize() symbolicName="
                                        + symbolicName
                                        + ", baseLoader="
                                        + baseLoader);
                } else if (baseLoader != null) {
                    weavingService = factory.getWeavingService(baseLoader);
                    cachingService = factory.getCachingService(baseLoader,
                            bundle, weavingService);
                } else if (bundle instanceof BundleFragment) {
                    final BundleFragment fragment = (BundleFragment) bundle;
                    final BundleHost host = (BundleHost) factory
                            .getHost(fragment);
                    if (Debug.DEBUG_GENERAL)
                        Debug
                                .println("- AspectJAdaptor.initialize() symbolicName="
                                        + symbolicName + ", host=" + host);

                    final BaseData hostData = (BaseData) host.getBundleData();
                    //				System.err.println("? AspectJAdaptor.initialize() bundleData=" + hostData);
                    final BundleFile bundleFile = hostData.getBundleFile();
                    if (bundleFile instanceof WeavingBundleFile) {
                        final WeavingBundleFile hostFile = (WeavingBundleFile) bundleFile;
                        //					System.err.println("? AspectJAdaptor.initialize() bundleFile=" + hostFile);
                        final WeavingAdaptor hostAdaptor = (WeavingAdaptor) hostFile
                                .getAdaptor();
                        //					System.err.println("? AspectJAdaptor.initialize() bundleFile=" + hostAdaptor);
                        weavingService = hostAdaptor.weavingService;
                        cachingService = factory.getCachingService(
                                hostAdaptor.baseLoader, bundle, weavingService);
                    }
                } else {
                    if (Debug.DEBUG_GENERAL)
                        Debug
                                .println("W AspectJAdaptor.initialize() symbolicName="
                                        + symbolicName
                                        + ", baseLoader="
                                        + baseLoader);
                }
                initialized = true;
                identifyRecursionSet.remove(this);
            }

            if (Debug.DEBUG_GENERAL)
                Debug.println("< AspectJAdaptor.initialize() weavingService="
                        + (weavingService != null) + ", cachingService="
                        + (cachingService != null));
        }
    }

    public void setBaseClassLoader(final BaseClassLoader baseClassLoader) {
        this.baseLoader = baseClassLoader;

        if (Debug.DEBUG_GENERAL)
            Debug.println("- AspectJAdaptor.setBaseClassLoader() bundle="
                    + symbolicName + ", baseLoader=" + baseLoader);
    }

    public boolean storeClass(final String name, final URL sourceFileURL,
            final Class clazz, final byte[] classbytes) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> AspectJAdaptor.storeClass() bundle="
                    + symbolicName + ", url=" + sourceFileURL + ", name="
                    + name + ", clazz=" + clazz);
        boolean stored = false;

        initialize();
        if (cachingService != null) {
            //have we generated a closure? 
            if (weavingService != null
                    && weavingService.generatedClassesExistFor(
                            (ClassLoader) baseLoader, name)) {
                //If so we need to ask the cache if its capable of handling generated closures
                if (cachingService.canCacheGeneratedClasses()) {
                    final Map<String, byte[]> generatedClasses = weavingService
                            .getGeneratedClassesFor(name);

                    stored = cachingService.storeClassAndGeneratedClasses("",
                            sourceFileURL, clazz, classbytes, generatedClasses);
                } else {
                    weavingService
                            .flushGeneratedClasses((ClassLoader) baseLoader);
                    if (Debug.DEBUG_CACHE)
                        Debug
                                .println("- AspectJAdaptor.storeClass() generatedClassesExistFor=true");
                }
            } else {
                stored = cachingService.storeClass("", sourceFileURL, clazz,
                        classbytes);
                if (!stored) {
                    if (Debug.DEBUG_CACHE)
                        Debug.println("E AspectJHook.storeClass() bundle="
                                + symbolicName + ", name=" + name);
                }
            }
        }
        if (Debug.DEBUG_CACHE)
            Debug.println("< AspectJAdaptor.storeClass() stored=" + stored);
        return stored;
    }

    @Override
    public String toString() {
        return "AspectJAdaptor[" + symbolicName + "]";
    }

    public byte[] weaveClass(final String name, final byte[] bytes) {
        if (Debug.DEBUG_WEAVE)
            Debug.println("> AspectJAdaptor.weaveClass() bundle="
                    + symbolicName + ", name=" + name + ", bytes="
                    + bytes.length);
        byte[] newBytes = null;

        initialize();
        if (/* shouldWeave(bytes) && */weavingService != null) {
            try {
                newBytes = weavingService.preProcess(name, bytes,
                        (ClassLoader) baseLoader);
            } catch (final IOException ex) {
                throw new ClassFormatError(ex.toString());
            }
        }

        if (Debug.DEBUG_WEAVE)
            Debug.println("< AspectJAdaptor.weaveClass() newBytes=" + newBytes);
        return newBytes;
    }

}
