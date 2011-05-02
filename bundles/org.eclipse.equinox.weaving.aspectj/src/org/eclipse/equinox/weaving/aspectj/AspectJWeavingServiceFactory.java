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

package org.eclipse.equinox.weaving.aspectj;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.IWeavingServiceFactory;
import org.eclipse.equinox.weaving.aspectj.loadtime.AspectAdminImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;

/**
 * The factory to create AspectJ-based weavers.
 * 
 * @author martinlippert
 */
public class AspectJWeavingServiceFactory implements IWeavingServiceFactory {

    private final AspectAdminImpl aspectDefinitionRegistry;

    public AspectJWeavingServiceFactory(final AspectAdminImpl aspectDefinitionRegistry) {
        this.aspectDefinitionRegistry = aspectDefinitionRegistry;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingServiceFactory#createWeavingService(java.lang.ClassLoader,
     *      org.osgi.framework.Bundle, org.eclipse.osgi.service.resolver.State,
     *      org.eclipse.osgi.service.resolver.BundleDescription,
     *      org.eclipse.equinox.service.weaving.ISupplementerRegistry)
     */
    public IWeavingService createWeavingService(final ClassLoader loader,
            final Bundle bundle, final State resolverState,
            final BundleDescription bundleDesciption,
            final ISupplementerRegistry supplementerRegistry) {
        return new AspectJWeavingService(loader, bundle, resolverState,
                bundleDesciption, supplementerRegistry,
                aspectDefinitionRegistry);
    }

}
