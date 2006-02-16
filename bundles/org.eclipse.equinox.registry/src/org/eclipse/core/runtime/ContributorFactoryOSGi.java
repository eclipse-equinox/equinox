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
package org.eclipse.core.runtime;

import org.eclipse.core.internal.registry.osgi.OSGIUtils;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.Bundle;

/**
 * The contributor factory creates new registry contributors for use in OSGi-based 
 * registries. 
 * <p>
 * This class can not be extended or instantiated by clients.
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost certainly 
 * be broken (repeatedly) as the API evolves.
 * </p>
 * @since org.eclipse.equinox.registry 3.2
 */
public final class ContributorFactoryOSGi {

	/**
	 * Creates registry contributor object based on a Bundle.
	 * @param contributor bundle associated with the contribution
	 * @return new registry contributor based on the Bundle
	 */
	public static IContributor createContributor(Bundle contributor) {
		String id = Long.toString(contributor.getBundleId());
		String name = contributor.getSymbolicName();
		String hostId = null;
		String hostName = null;

		// determine host properties, if any
		if (OSGIUtils.getDefault().isFragment(contributor)) {
			Bundle[] hosts = OSGIUtils.getDefault().getHosts(contributor);
			if (hosts != null) {
				Bundle hostBundle = hosts[0];
				hostId = Long.toString(hostBundle.getBundleId());
				hostName = hostBundle.getSymbolicName();
			}
		}

		return new RegistryContributor(id, name, hostId, hostName);
	}
}
