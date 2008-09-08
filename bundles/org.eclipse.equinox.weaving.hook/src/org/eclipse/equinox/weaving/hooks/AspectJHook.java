/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes 
 *   Martin Lippert            supplementing mechanism reworked     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.net.URL;

import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.equinox.weaving.adaptors.AspectJAdaptor;
import org.eclipse.equinox.weaving.adaptors.AspectJAdaptorFactory;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class AspectJHook extends AbstractAspectJHook {

    private final AspectJAdaptorFactory adaptorFactory;

    private BundleContext bundleContext;

    public AspectJHook() {
        if (Debug.DEBUG_GENERAL) Debug.println("- AspectJHook.<init>()");
        adaptorFactory = new AspectJAdaptorFactory();
    }

    public void frameworkStart(final BundleContext context)
            throws BundleException {
        //		Debug.println("? AspectJHook.frameworkStart() context=" + context + ", fdo=" + FrameworkDebugOptions.getDefault());
        initialize(context);
    }

    public void frameworkStop(final BundleContext context)
            throws BundleException {
        adaptorFactory.dispose(context);
    }

    public void initializedClassLoader(final BaseClassLoader baseClassLoader,
            final BaseData data) {
        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("> AspectJHook.initializedClassLoader() bundle="
                            + data.getSymbolicName() + ", loader="
                            + baseClassLoader + ", data=" + data
                            + ", bundleFile=" + data.getBundleFile());
        IAspectJAdaptor adaptor = null;
        final BundleFile bundleFile = data.getBundleFile();
        if (bundleFile instanceof BaseAjBundleFile) {
            final BaseAjBundleFile baseBundleFile = (BaseAjBundleFile) bundleFile;
            adaptor = baseBundleFile.getAdaptor();
            adaptor.setBaseClassLoader(baseClassLoader);
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AspectJHook.initializedClassLoader() adaptor="
                    + adaptor);
    }

    public byte[] processClass(final String name, final byte[] classbytes,
            final ClasspathEntry classpathEntry, final BundleEntry entry,
            final ClasspathManager manager) {
        byte[] newClassytes = null;
        if (entry instanceof AspectJBundleEntry) {
            final AspectJBundleEntry ajBundleEntry = (AspectJBundleEntry) entry;
            if (!ajBundleEntry.dontWeave()) {
                final IAspectJAdaptor adaptor = ajBundleEntry.getAdaptor();
                newClassytes = adaptor.weaveClass(name, classbytes);
            }
        }
        return newClassytes;
    }

    public void recordClassDefine(final String name, final Class clazz,
            final byte[] classbytes, final ClasspathEntry classpathEntry,
            final BundleEntry entry, final ClasspathManager manager) {
        if (entry instanceof AspectJBundleEntry) {
            final AspectJBundleEntry ajBundleEntry = (AspectJBundleEntry) entry;
            if (!ajBundleEntry.dontWeave()) {
                final IAspectJAdaptor adaptor = ajBundleEntry.getAdaptor();
                final URL sourceFileURL = ajBundleEntry.getBundleFileURL();
                adaptor.storeClass(name, sourceFileURL, clazz, classbytes);
            }
        }
    }

    public BundleFile wrapBundleFile(final BundleFile bundleFile,
            final Object content, final BaseData data, final boolean base)
            throws IOException {
        BundleFile wrapped = null;
        if (Debug.DEBUG_BUNDLE)
            Debug
                    .println("> AspectJBundleFileWrapperFactoryHook.wrapBundleFile() bundle="
                            + data.getSymbolicName()
                            + " bundleFile="
                            + bundleFile
                            + ", content="
                            + content
                            + ", data="
                            + data
                            + ", base="
                            + base
                            + ", baseFile="
                            + bundleFile.getBaseFile());

        if (base) {
            final IAspectJAdaptor adaptor = createAspectJAdaptor(data);
            if (adaptor != null) {
                wrapped = new BaseAjBundleFile(adaptor, bundleFile);
            }
        } else {
            IAspectJAdaptor adaptor = null;
            if (bundleContext != null) {
                adaptor = getAspectJAdaptor(data);
                if (Debug.DEBUG_BUNDLE)
                    Debug
                            .println("- AspectJBundleFileWrapperFactoryHook.wrapBundleFile() adaptor="
                                    + adaptor);
                //				if (adaptor == null) throw new RuntimeException(data.getSymbolicName());
            }
            if (adaptor != null) {
                wrapped = new AspectJBundleFile(adaptor, bundleFile);
            }
        }
        if (Debug.DEBUG_BUNDLE)
            Debug
                    .println("< AspectJBundleFileWrapperFactoryHook.wrapBundleFile() wrapped="
                            + wrapped);
        return wrapped;
    }

    private IAspectJAdaptor createAspectJAdaptor(final BaseData baseData) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJHook.createAspectJAdaptor() location="
                    + baseData.getLocation());
        IAspectJAdaptor adaptor = null;

        if (adaptorFactory != null) {
            adaptor = new AspectJAdaptor(baseData, adaptorFactory, null, null,
                    null);
        } else {
            if (Debug.DEBUG_GENERAL)
                Debug.println("- AspectJHook.createAspectJAdaptor() factory="
                        + adaptorFactory);
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AspectJHook.createAspectJAdaptor() adaptor="
                    + adaptor);
        return adaptor;
    }

    private IAspectJAdaptor getAspectJAdaptor(final BaseData data) {
        IAspectJAdaptor adaptor = null;

        final BundleFile bundleFile = data.getBundleFile();
        if (bundleFile instanceof BaseAjBundleFile) {
            final BaseAjBundleFile baseBundleFile = (BaseAjBundleFile) bundleFile;
            adaptor = baseBundleFile.getAdaptor();
        }

        return adaptor;
    }

    private void initialize(final BundleContext context) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJHook.initialize() context=" + context);

        this.bundleContext = context;

        final SupplementerRegistry supplementerRegistry = getSupplementerRegistry();
        adaptorFactory.initialize(context, supplementerRegistry);

        final ServiceReference serviceReference = context
                .getServiceReference(PackageAdmin.class.getName());
        final PackageAdmin packageAdmin = (PackageAdmin) context
                .getService(serviceReference);

        supplementerRegistry.setBundleContext(context);
        supplementerRegistry.setPackageAdmin(packageAdmin);
        context.addBundleListener(new SupplementBundleListener(
                supplementerRegistry));

        // final re-build supplementer final registry state for final installed bundles
        final Bundle[] installedBundles = context.getBundles();
        for (int i = 0; i < installedBundles.length; i++) {
            supplementerRegistry.addSupplementer(installedBundles[i], false);
        }
        for (int i = 0; i < installedBundles.length; i++) {
            supplementerRegistry.addSupplementedBundle(installedBundles[i]);
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AspectJHook.initialize() adaptorFactory="
                    + adaptorFactory);
    }
}
