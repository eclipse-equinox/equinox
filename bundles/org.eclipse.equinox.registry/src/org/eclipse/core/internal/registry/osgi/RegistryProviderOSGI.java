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
package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.IRegistryProvider;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public final class RegistryProviderOSGI implements IRegistryProvider {

	private ServiceTracker registryTracker = null;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.IRegistryProvider#getRegistry()
	 */
	public IExtensionRegistry getRegistry() {
		if (registryTracker == null) {
			BundleContext context = Activator.getContext();
			if (context == null) {
				RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.bundle_not_activated, null));
				return null;
			}
			registryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), null);
			registryTracker.open();
		}
		return (IExtensionRegistry) registryTracker.getService();
	}

	/**
	 * Release OSGi tracker
	 */
	public void release() {
		if (registryTracker != null) {
			registryTracker.close();
			registryTracker = null;
		}
	}
}
