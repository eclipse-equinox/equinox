/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class ContributorFactoryOSGi {

	/**
	 * Creates registry contributor object based on a Bundle. The bundle must not 
	 * be <code>null</code>.
	 * 
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

	/**
	 * Returns the OSGi bundle used to define this contributor. If a fragment
	 * was used to create the contributor, the fragment is returned. 
	 * 
	 * <p>The method may return null if the contributor is not based on a bundle, 
	 * if the bundle can't be found, or if the bundle is presently unresolved or 
	 * uninstalled.</p>
	 * 
	 * @param contributor bundle-based registry contributor
	 * @return the actual OSGi bundle associated with this contributor
	 * @since org.eclipse.equinox.registry 3.3
	 */
	public static Bundle resolve(IContributor contributor) {
		if (contributor == null)
			return null;
		if (!(contributor instanceof RegistryContributor))
			return null;
		String symbolicName = ((RegistryContributor) contributor).getActualName();
		return OSGIUtils.getDefault().getBundle(symbolicName);
	}
}
