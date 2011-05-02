/*******************************************************************************
 * Copyright (c) 2008, 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;

/**
 * This is the central interface for other bundles to implement when they would
 * like to contribute a concrete weaving mechanism. Bundles should implement
 * this interface and register an implementation as an OSGi service under this
 * interface.
 * 
 * @author Martin Lippert
 */
public interface IWeavingServiceFactory {

    /**
     * Create a concrete weaving implementation for the given bundle. This is
     * called by the basic equinox aspects weaving hook mechanism lazily when
     * the classloader for the bundle is created.
     * 
     * @param loader The classloader of the bundle for which to create a weaver
     * @param bundle The bundle for which to create the weaver
     * @param resolverState The state service of the OSGi runtime
     * @param bundleDesciption The description of the bundle for which to create
     *            a weaver
     * @param supplementerRegistry The supplementer registry which is used by
     *            the core equinox aspects hook
     * @return The created weaver for the given bundle
     */
    public IWeavingService createWeavingService(ClassLoader loader,
            Bundle bundle, State resolverState,
            BundleDescription bundleDesciption,
            ISupplementerRegistry supplementerRegistry);

}
