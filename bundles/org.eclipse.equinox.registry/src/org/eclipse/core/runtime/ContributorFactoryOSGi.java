/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.util.List;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.framework.util.Wirings;
import org.osgi.framework.Bundle;

/**
 * The contributor factory creates new registry contributors for use in OSGi-based
 * registries.
 * <p>
 * This class can not be extended or instantiated by clients.
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
		if (Wirings.isFragment(contributor)) {
			List<Bundle> hosts = Wirings.getHosts(contributor);
			if (!hosts.isEmpty()) {
				Bundle hostBundle = hosts.get(0);
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
		return Wirings.getAtLeastResolvedBundle(symbolicName).orElse(null);
	}
}
