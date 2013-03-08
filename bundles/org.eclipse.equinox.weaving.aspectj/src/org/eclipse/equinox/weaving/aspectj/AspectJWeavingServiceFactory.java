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
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;

/**
 * The factory to create AspectJ-based weavers.
 * 
 * @author martinlippert
 */
public class AspectJWeavingServiceFactory implements IWeavingServiceFactory {

    private final AspectAdminImpl aspectDefinitionRegistry;

    public AspectJWeavingServiceFactory(
            final AspectAdminImpl aspectDefinitionRegistry) {
        this.aspectDefinitionRegistry = aspectDefinitionRegistry;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.IWeavingServiceFactory#createWeavingService(ClassLoader,
     *      Bundle, BundleRevision, ISupplementerRegistry)
     */
    public IWeavingService createWeavingService(final ClassLoader loader,
            final Bundle bundle, final BundleRevision bundleRevision,
            final ISupplementerRegistry supplementerRegistry) {
        return new AspectJWeavingService(loader, bundle, bundleRevision,
                supplementerRegistry, aspectDefinitionRegistry);
    }

}
