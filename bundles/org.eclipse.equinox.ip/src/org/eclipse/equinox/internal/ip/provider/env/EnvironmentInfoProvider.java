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
package org.eclipse.equinox.internal.ip.provider.env;

import java.util.*;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.eclipse.equinox.internal.ip.provider.BaseProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Implements ConfiguratorLoader. Reads from system properties and
 * BundleContext. Acts as provider.
 * 
 * @author Avgustin Marinov,
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class EnvironmentInfoProvider extends BaseProvider implements ProvisioningInfoProvider {

	/**
	 * This is a key for property that points if manual setting of info is
	 * allowed.
	 */
	public static final String MANUAL_SUPPORT = "equinox.provisioning.env.provider.allowed";

	/**
	 * This is a system properties key that points that the system properties
	 * starting with this key should be pushed into ProvisioningService If value
	 * for the key is not set or is "" no system properties are pushed. If "*"
	 * all are pushed. Else -> only the properties with key started with the
	 * PUSH_STARTING_WITH are pushed.
	 */
	public static final String PUSH_STARTING_WITH = "equinox.provisioning.env.provider.push.starting.with";

	/** Properties that are to be loaded into dictionary. */
	private static final String[] props = {ProvisioningService.PROVISIONING_REFERENCE};

	private BundleContext bc;

	/**
	 * If manual support is available starts it.
	 * 
	 * @see org.eclipse.equinox.internal.ip.impl.provider.BaseProvider#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		boolean manualsupport = true;
		if (bc.getProperty(MANUAL_SUPPORT) != null)
			if (bc.getProperty(MANUAL_SUPPORT).equals("false"))
				manualsupport = false;
		if (!manualsupport) {
			Log.debug(this + " is not an allowed provider.");
			return;
		}
		this.bc = bc;
		super.start(bc);
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningInfoProvider#init(org.osgi.service.provisioning.ProvisioningService)
	 */
	public Dictionary init(ProvisioningService prvSrv) {
		Dictionary info = new Hashtable();
		for (int i = props.length; i-- > 0;) {
			String value = ProvisioningAgent.bc.getProperty(props[i]);
			if (value != null) {
				info.put(props[i], value);
			}
		}
		if (ProvisioningAgent.bc.getProperty(PUSH_STARTING_WITH) != null) {
			String prefix = ProvisioningAgent.bc.getProperty(PUSH_STARTING_WITH).trim();
			if (prefix.length() != 0) {
				boolean all = "*".equals(prefix);
				Dictionary sprops = System.getProperties();
				for (Enumeration e = sprops.keys(); e.hasMoreElements();) {
					try {
						String key = (String) e.nextElement();
						if (all || key.startsWith(prefix)) {
							info.put(key, sprops.get(key));
						}
					} catch (Exception _) {
					}
				}
			}
		}
		return info;
	}

	/**
	 * Gets system property
	 * 
	 * @param key
	 *            the key.
	 * @return the value.
	 */
	public Object get(Object key) {
		Object value = null;
		if (key instanceof String) {
			value = ProvisioningAgent.bc.getProperty((String) key);
			if (value == null) {
				value = bc.getProperty((String) key);
			}
		}
		return value;
	}

	/**
	 * Returns name of this provider.
	 * 
	 * @return the name.
	 */
	public String toString() {
		return "Environment";
	}
}
