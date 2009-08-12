/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import java.lang.reflect.Method;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class AspectJWeavingActivator implements BundleActivator {

    private static final String CHECK_ASPECTJ_CLASS = "org.aspectj.weaver.loadtime.definition.Definition"; //$NON-NLS-1$

    private static final String REAL_ACTIVATOR_CLASS = "org.eclipse.equinox.weaving.aspectj.AspectJWeavingStarter"; //$NON-NLS-1$

    private Object starter; // to decouple the optional dependencies

    private Class<?> starterClass;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        final ClassLoader loader = this.getClass().getClassLoader();

        try {
            final Class<?> aspectjClass = loader.loadClass(CHECK_ASPECTJ_CLASS);
            if (aspectjClass != null) {
                starterClass = loader.loadClass(REAL_ACTIVATOR_CLASS);
                starter = starterClass.newInstance();

                final Method startMethod = starterClass.getMethod("start",
                        BundleContext.class);
                startMethod.invoke(starter, context);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        if (starter != null) {
            final Method stopMethod = starterClass.getMethod("stop",
                    BundleContext.class);
            stopMethod.invoke(starter, context);
        }
    }

}
