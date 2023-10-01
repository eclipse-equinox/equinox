/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 *   Martin Lippert            extracted caching service factory
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching.j9;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.osgi.framework.Bundle;

/**
 * Factory implementation to create concrete J9 caching services for individual
 * bundles
 * 
 * @author martinlippert
 */
public class CachingServiceFactory implements ICachingServiceFactory {

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingServiceFactory#createCachingService(java.lang.ClassLoader,
	 *      org.osgi.framework.Bundle, java.lang.String)
	 */
	public ICachingService createCachingService(final ClassLoader classLoader, final Bundle bundle, final String key) {
		return new CachingService(classLoader, bundle, key);
	}

}
