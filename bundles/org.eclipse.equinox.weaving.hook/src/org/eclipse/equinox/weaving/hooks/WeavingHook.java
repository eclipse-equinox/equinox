/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *   Martin Lippert            supplementing mechanism reworked
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.equinox.weaving.adaptors.WeavingAdaptor;
import org.eclipse.equinox.weaving.adaptors.WeavingAdaptorFactory;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
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
        if (Debug.DEBUG_GENERAL) Debug.println("- WeavingHook.<init>()");

        this.adaptorFactory = new WeavingAdaptorFactory();
        this.adaptors = new HashMap<Long, IWeavingAdaptor>();
    }

    @Override
    public void classLoaderCreated(final ModuleClassLoader classLoader) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> WeavingHook.initializedClassLoader() bundle="
                    + classLoader.getBundle().getSymbolicName()
                    + ", loader="
                    + classLoader
                    + ", bundleFile="
                    + classLoader.getClasspathManager().getGeneration()
                            .getBundleFile());

        final IWeavingAdaptor adaptor = createWeavingAdaptor(classLoader
                .getClasspathManager().getGeneration(), classLoader);
        this.adaptors.put(classLoader.getBundle().getBundleId(), adaptor);

        if (Debug.DEBUG_GENERAL)
            Debug.println("< WeavingHook.initializedClassLoader() adaptor="
                    + adaptor);
    }

    private IWeavingAdaptor createWeavingAdaptor(final Generation generation,
            final ModuleClassLoader classLoader) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> WeavingHook.createAspectJAdaptor() location="
                    + generation.getRevision().getRevisions().getModule()
                            .getLocation());
        IWeavingAdaptor adaptor = null;

        if (adaptorFactory != null) {
            adaptor = new WeavingAdaptor(generation, adaptorFactory, null,
                    null, classLoader);
        } else {
            if (Debug.DEBUG_GENERAL)
                Debug.println("- WeavingHook.createAspectJAdaptor() factory="
                        + adaptorFactory);
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< WeavingHook.createAspectJAdaptor() adaptor="
                    + adaptor);
        return adaptor;
    }

    @Override
    public IWeavingAdaptor getAdaptor(final long bundleID) {
        return this.adaptors.get(bundleID);
    }

    @Override
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

    private void initialize(final BundleContext context) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> WeavingHook.initialize() context=" + context);

        this.bundleContext = context;

        final ISupplementerRegistry supplementerRegistry = getSupplementerRegistry();
        adaptorFactory.initialize(context, supplementerRegistry);

        final ServiceReference<PackageAdmin> serviceReference = context
                .getServiceReference(PackageAdmin.class);
        final PackageAdmin packageAdmin = context.getService(serviceReference);

        supplementerRegistry.setBundleContext(context);
        supplementerRegistry.setPackageAdmin(packageAdmin);
        context.addBundleListener(new SupplementBundleListener(
                supplementerRegistry));

        // re-build supplementer registry state for installed bundles
        final Bundle[] installedBundles = context.getBundles();
        for (Bundle installedBundle : installedBundles) {
            if (installedBundle.getState() != Bundle.UNINSTALLED) {
                supplementerRegistry.addSupplementer(installedBundle, false);
            }
        }
        for (Bundle installedBundle : installedBundles) {
            if (installedBundle.getState() != Bundle.UNINSTALLED) {
                supplementerRegistry.addSupplementedBundle(installedBundle);
            }
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< WeavingHook.initialize() adaptorFactory="
                    + adaptorFactory);
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
    public void recordClassDefine(final String name, final Class<?> clazz,
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

    @Override
    public void resetAdaptor(final long bundleID) {
        this.adaptors.remove(bundleID);
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#frameworkStart(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws BundleException {
        initialize(context);
    }

    /**
     * @see org.eclipse.equinox.weaving.hooks.AbstractWeavingHook#frameworkStop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws BundleException {
        adaptorFactory.dispose(context);
    }

    /**
     * @see org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook#wrapBundleFile(org.eclipse.osgi.storage.bundlefile.BundleFile,
     *      org.eclipse.osgi.storage.BundleInfo.Generation, boolean)
     */
    public BundleFileWrapper wrapBundleFile(final BundleFile bundleFile,
            final Generation generation, final boolean base) {
        if (bundleFile.getBaseFile() == null) {
            // must have a base file to work
            return null;
        }
        BundleFileWrapper wrapped = null;
        if (Debug.DEBUG_BUNDLE)
            Debug.println("> WeavingHook.wrapBundleFile() bundle="
                    + (generation.getRevision() != null ? generation
                            .getRevision().getSymbolicName() : generation
                            .getBundleInfo().getBundleId()) + " bundleFile="
                    + bundleFile + ", generation=" + generation + ", base="
                    + base + ", baseFile=" + bundleFile.getBaseFile());

        if (base) {
            wrapped = new BaseWeavingBundleFile(new BundleAdaptorProvider(
                    generation, this), bundleFile);
        } else {
            wrapped = new WeavingBundleFile(new BundleAdaptorProvider(
                    generation, this), bundleFile);
        }
        if (Debug.DEBUG_BUNDLE)
            Debug.println("< WeavingHook.wrapBundleFile() wrapped=" + wrapped);
        return wrapped;
    }

}
