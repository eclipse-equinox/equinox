/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked      
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Default implementation of all the necessary adaptor hooks. Used to keep the
 * real hook implementation focused on only the necessary methods.
 * 
 * @author Matthew Webster
 * @author Martin Lippert
 */
public abstract class AbstractWeavingHook implements HookConfigurator,
        AdaptorHook, BundleFileWrapperFactoryHook, ClassLoadingHook,
        ClassLoadingStatsHook, IAdaptorProvider {

    /**
     * flag to indicate whether to print out detailed information or not
     */
    public static boolean verbose = Boolean
            .getBoolean("org.aspectj.osgi.verbose"); //$NON-NLS-1$

    private ISupplementerRegistry supplementerRegistry;

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#addClassPathEntry(java.util.ArrayList,
     *      java.lang.String,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager,
     *      org.eclipse.osgi.baseadaptor.BaseData,
     *      java.security.ProtectionDomain)
     */
    public boolean addClassPathEntry(final ArrayList cpEntries,
            final String cp, final ClasspathManager hostmanager,
            final BaseData sourcedata, final ProtectionDomain sourcedomain) {
        return false;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.HookConfigurator#addHooks(org.eclipse.osgi.baseadaptor.HookRegistry)
     */
    public void addHooks(final HookRegistry hooks) {
        if (verbose)
            System.err
                    .println("[org.eclipse.equinox.weaving.hook] info adding AspectJ hooks ..."); //$NON-NLS-1$

        supplementerRegistry = new SupplementerRegistry(this);

        hooks.addAdaptorHook(this);
        hooks.addClassLoadingHook(this);
        hooks.addBundleFileWrapperFactoryHook(this);
        hooks.addClassLoadingStatsHook(this);
        hooks.addClassLoaderDelegateHook(new WeavingLoaderDelegateHook(
                supplementerRegistry));
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#addProperties(java.util.Properties)
     */
    public void addProperties(final Properties properties) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#createClassLoader(java.lang.ClassLoader,
     *      org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate,
     *      org.eclipse.osgi.framework.adaptor.BundleProtectionDomain,
     *      org.eclipse.osgi.baseadaptor.BaseData, java.lang.String[])
     */
    public BaseClassLoader createClassLoader(final ClassLoader parent,
            final ClassLoaderDelegate delegate,
            final BundleProtectionDomain domain, final BaseData data,
            final String[] bundleclasspath) {
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#createFrameworkLog()
     */
    public FrameworkLog createFrameworkLog() {
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#findLibrary(org.eclipse.osgi.baseadaptor.BaseData,
     *      java.lang.String)
     */
    public String findLibrary(final BaseData data, final String libName) {
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#frameworkStart(org.osgi.framework.BundleContext)
     */
    public void frameworkStart(final BundleContext context)
            throws BundleException {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#frameworkStop(org.osgi.framework.BundleContext)
     */
    public void frameworkStop(final BundleContext context)
            throws BundleException {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#frameworkStopping(org.osgi.framework.BundleContext)
     */
    public void frameworkStopping(final BundleContext context) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#getBundleClassLoaderParent()
     */
    public ClassLoader getBundleClassLoaderParent() {
        return null;
    }

    /**
     * Their is only one registry for dealing with supplementers available via
     * this accessor method.
     * 
     * @return The supplementer registry, guaranteed to be not null
     */
    public ISupplementerRegistry getSupplementerRegistry() {
        return this.supplementerRegistry;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#handleRuntimeError(java.lang.Throwable)
     */
    public void handleRuntimeError(final Throwable error) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#initialize(org.eclipse.osgi.baseadaptor.BaseAdaptor)
     */
    public void initialize(final BaseAdaptor adaptor) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#initializedClassLoader(org.eclipse.osgi.baseadaptor.loader.BaseClassLoader,
     *      org.eclipse.osgi.baseadaptor.BaseData)
     */
    public void initializedClassLoader(final BaseClassLoader baseClassLoader,
            final BaseData data) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#mapLocationToURLConnection(java.lang.String)
     */
    public URLConnection mapLocationToURLConnection(final String location)
            throws IOException {
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.AdaptorHook#matchDNChain(java.lang.String,
     *      java.lang.String[])
     */
    public boolean matchDNChain(final String pattern, final String[] dnChain) {
        return false;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook#postFindLocalClass(java.lang.String,
     *      java.lang.Class,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public void postFindLocalClass(final String name, final Class clazz,
            final ClasspathManager manager) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook#postFindLocalResource(java.lang.String,
     *      java.net.URL, org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public void postFindLocalResource(final String name, final URL resource,
            final ClasspathManager manager) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook#preFindLocalClass(java.lang.String,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public void preFindLocalClass(final String name,
            final ClasspathManager manager) throws ClassNotFoundException {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook#preFindLocalResource(java.lang.String,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public void preFindLocalResource(final String name,
            final ClasspathManager manager) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#processClass(java.lang.String,
     *      byte[], org.eclipse.osgi.baseadaptor.loader.ClasspathEntry,
     *      org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public byte[] processClass(final String name, final byte[] classbytes,
            final ClasspathEntry classpathEntry, final BundleEntry entry,
            final ClasspathManager manager) {
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook#recordClassDefine(java.lang.String,
     *      java.lang.Class, byte[],
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathEntry,
     *      org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public void recordClassDefine(final String name, final Class clazz,
            final byte[] classbytes, final ClasspathEntry classpathEntry,
            final BundleEntry entry, final ClasspathManager manager) {
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook#wrapBundleFile(org.eclipse.osgi.baseadaptor.bundlefile.BundleFile,
     *      java.lang.Object, org.eclipse.osgi.baseadaptor.BaseData, boolean)
     */
    public BundleFile wrapBundleFile(final BundleFile bundleFile,
            final Object content, final BaseData data, final boolean base)
            throws IOException {
        return null;
    }

}
