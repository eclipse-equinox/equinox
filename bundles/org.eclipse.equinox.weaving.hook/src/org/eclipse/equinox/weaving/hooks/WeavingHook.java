/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.equinox.weaving.adaptors.WeavingAdaptor;
import org.eclipse.equinox.weaving.adaptors.WeavingAdaptorFactory;
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

public class WeavingHook extends AbstractWeavingHook {

    private final WeavingAdaptorFactory adaptorFactory;

    private final Map<Long, IWeavingAdaptor> adaptors;

    private BundleContext bundleContext;

    public WeavingHook() {
        if (Debug.DEBUG_GENERAL) Debug.println("- AspectJHook.<init>()");
        this.adaptorFactory = new WeavingAdaptorFactory();
        this.adaptors = new HashMap<Long, IWeavingAdaptor>();
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#frameworkStart(org.osgi.framework.BundleContext)
     */
    @Override
    public void frameworkStart(final BundleContext context)
            throws BundleException {
        //		Debug.println("? AspectJHook.frameworkStart() context=" + context + ", fdo=" + FrameworkDebugOptions.getDefault());
        initialize(context);
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#frameworkStop(org.osgi.framework.BundleContext)
     */
    @Override
    public void frameworkStop(final BundleContext context)
            throws BundleException {
        adaptorFactory.dispose(context);
    }

    public IWeavingAdaptor getAdaptor(final long bundleID) {
        return this.adaptors.get(bundleID);
    }

    public IWeavingAdaptor getHostBundleAdaptor(final long bundleID) {
        final Bundle bundle = this.bundleContext.getBundle(bundleID);
        if (bundle != null) {
            final Bundle host = adaptorFactory.getHost(bundle);
            if (host != null) {
                final long hostBundleID = host.getBundleId();
                return this.adaptors.get(hostBundleID);
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#initializedClassLoader(org.eclipse.osgi.baseadaptor.loader.BaseClassLoader,
     *      org.eclipse.osgi.baseadaptor.BaseData)
     */
    @Override
    public void initializedClassLoader(final BaseClassLoader baseClassLoader,
            final BaseData data) {
        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("> AspectJHook.initializedClassLoader() bundle="
                            + data.getSymbolicName() + ", loader="
                            + baseClassLoader + ", data=" + data
                            + ", bundleFile=" + data.getBundleFile());

        final IWeavingAdaptor adaptor = createAspectJAdaptor(data);
        adaptor.setBaseClassLoader(baseClassLoader);
        adaptor.initialize();
        this.adaptors.put(data.getBundleID(), adaptor);

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AspectJHook.initializedClassLoader() adaptor="
                    + adaptor);
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#processClass(java.lang.String,
     *      byte[], org.eclipse.osgi.baseadaptor.loader.ClasspathEntry,
     *      org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    @Override
    public byte[] processClass(final String name, final byte[] classbytes,
            final ClasspathEntry classpathEntry, final BundleEntry entry,
            final ClasspathManager manager) {
        byte[] newClassytes = null;
        if (entry instanceof WeavingBundleEntry) {
            final WeavingBundleEntry ajBundleEntry = (WeavingBundleEntry) entry;
            if (!ajBundleEntry.dontWeave()) {
                final IWeavingAdaptor adaptor = ajBundleEntry.getAdaptor();
                newClassytes = adaptor.weaveClass(name, classbytes);
            }
        }
        return newClassytes;
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#recordClassDefine(java.lang.String,
     *      java.lang.Class, byte[],
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathEntry,
     *      org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    @Override
    public void recordClassDefine(final String name, final Class clazz,
            final byte[] classbytes, final ClasspathEntry classpathEntry,
            final BundleEntry entry, final ClasspathManager manager) {
        if (clazz == null) {
            if (Debug.DEBUG_GENERAL) {
                Debug.println("Error in defining class: " + name); //$NON-NLS-1$
            }
            return;
        }
        if (entry instanceof WeavingBundleEntry) {
            final WeavingBundleEntry ajBundleEntry = (WeavingBundleEntry) entry;
            if (!ajBundleEntry.dontWeave()) {
                final IWeavingAdaptor adaptor = ajBundleEntry.getAdaptor();
                final URL sourceFileURL = ajBundleEntry.getBundleFileURL();
                adaptor.storeClass(name, sourceFileURL, clazz, classbytes);
            }
        }
    }

    public void resetAdaptor(final long bundleID) {
        this.adaptors.remove(bundleID);
    }

    @Override
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
            wrapped = new BaseWeavingBundleFile(new BundleAdaptorProvider(data,
                    this), bundleFile);
        } else {
            wrapped = new WeavingBundleFile(new BundleAdaptorProvider(data,
                    this), bundleFile);
        }
        if (Debug.DEBUG_BUNDLE)
            Debug
                    .println("< AspectJBundleFileWrapperFactoryHook.wrapBundleFile() wrapped="
                            + wrapped);
        return wrapped;
    }

    private IWeavingAdaptor createAspectJAdaptor(final BaseData baseData) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJHook.createAspectJAdaptor() location="
                    + baseData.getLocation());
        IWeavingAdaptor adaptor = null;

        if (adaptorFactory != null) {
            adaptor = new WeavingAdaptor(baseData, adaptorFactory, null, null,
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

    private void initialize(final BundleContext context) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJHook.initialize() context=" + context);

        this.bundleContext = context;

        final ISupplementerRegistry supplementerRegistry = getSupplementerRegistry();
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
