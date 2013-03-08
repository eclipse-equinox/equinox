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

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.osgi.framework.BundleActivator;

/**
 * Default implementation of all the necessary adaptor hooks. Used to keep the
 * real hook implementation focused on only the necessary methods.
 * 
 * @author Matthew Webster
 * @author Martin Lippert
 */
public abstract class AbstractWeavingHook extends ClassLoaderHook implements
        HookConfigurator, BundleFileWrapperFactoryHook, IAdaptorProvider,
        ActivatorHookFactory, BundleActivator {

    /**
     * flag to indicate whether to print out detailed information or not
     */
    public static boolean verbose = Boolean
            .getBoolean("org.aspectj.osgi.verbose"); //$NON-NLS-1$

    private ISupplementerRegistry supplementerRegistry;

    /**
     * @see org.eclipse.osgi.baseadaptor.HookConfigurator#addHooks(org.eclipse.osgi.baseadaptor.HookRegistry)
     */
    public void addHooks(final HookRegistry hooks) {
        if (verbose)
            System.err
                    .println("[org.eclipse.equinox.weaving.hook] info adding AspectJ hooks ..."); //$NON-NLS-1$

        Debug.init(hooks.getConfiguration().getDebugOptions());
        supplementerRegistry = new SupplementerRegistry(this);

        hooks.addClassLoaderHook(this);
        hooks.addBundleFileWrapperFactoryHook(this);
        hooks.addActivatorHookFactory(this);
        hooks.addClassLoaderHook(new WeavingLoaderDelegateHook(
                supplementerRegistry));
    }

    /**
     * @see org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory#createActivator()
     */
    public BundleActivator createActivator() {
        return this;
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

}
