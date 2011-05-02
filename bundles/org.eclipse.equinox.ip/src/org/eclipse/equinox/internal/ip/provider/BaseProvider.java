/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.provider;

import java.util.Hashtable;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.osgi.framework.*;

/**
 * Base class for inner providers. Implements their registering and
 * unregistering as services, their common debug.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class BaseProvider implements BundleActivator {

	/** Service Registration of ConfigurationLoader service. */
	private ServiceRegistration reg;

	/**
	 * Invoked from ProvisioningAgent if provider is packed in provisioning
	 * agent bundle or by FW when it is packed as standalone bundle. Registers
	 * itself as ProvisioningInfoProvider service with
	 * <I>Constants.SERVICE_RANKING</I> get from manifest. Puts in provisioning
	 * dictionary entire stored data.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		Log.debug = ProvisioningAgent.getBoolean("equinox.provisioning.debug");
		String providers = (String) bc.getBundle().getHeaders().get(ProvisioningInfoProvider.PROVIDERS);

		if (providers != null) {
			String className = this.getClass().getName();
			int index = (providers = providers.trim()).indexOf(className);
			if (index != -1) {
				index += className.length();
				index = providers.indexOf(';', index);
				if (index != -1) {
					int end = providers.indexOf(',', index);
					if (end == -1) {
						end = providers.length();
					}
					Integer ranking = null;
					try {
						ranking = new Integer(providers.substring(index + 1, end).trim());
					} catch (NumberFormatException nfe) {
						throw new Exception("Bad \"" + ProvisioningInfoProvider.PROVIDERS + "\" header format in manifest! " + toString() + "'s ranking \"" + providers.substring(index + 1, end).trim() + "\" is not valid integer!");
					}

					Log.debug("Registers " + this + " provider with ranking = " + ranking + ".");
					Hashtable props = new Hashtable(1, 1.0F);
					props.put(Constants.SERVICE_RANKING, ranking);
					reg = bc.registerService(ProvisioningInfoProvider.class.getName(), this, props);
					return;
				}
			}
		}
		throw new Exception("Bad \"" + ProvisioningInfoProvider.PROVIDERS + "\" header format in manifest! Loader " + toString() + " can't be loaded!");
	}

	/**
	 * Invoked from ProvisioningAgent if provider is packed in provisioning
	 * agent bundle or by FW when it is packed as standalone bundle. Unregister
	 * service.
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) {
		if (reg != null) {
			try {
				reg.unregister();
			} catch (Exception _) {
			}
		}
	}
}
