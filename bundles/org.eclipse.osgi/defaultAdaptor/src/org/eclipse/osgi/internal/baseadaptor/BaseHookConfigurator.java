/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;

/**
 * Add the hooks necessary to support the OSGi Framework specification.  
 */
public class BaseHookConfigurator implements HookConfigurator {

	public void addHooks(HookRegistry registry) {
		// always add the BaseStorageHook and BaseClassLoadingHook; it is required for the storage implementation
		BaseStorageHook hook = new BaseStorageHook(new BaseStorage());
		registry.addStorageHook(hook);
		registry.addAdaptorHook(hook);
		registry.addClassLoadingHook(new BaseClassLoadingHook());
	}

}
