/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public class SupplementBundleListener implements SynchronousBundleListener {

	private final ISupplementerRegistry supplementerRegistry;

	public SupplementBundleListener(final ISupplementerRegistry supplementerRegistry) {
		this.supplementerRegistry = supplementerRegistry;
	}

	@Override
	public void bundleChanged(final BundleEvent event) {
		final Bundle bundle = event.getBundle();
		if (event.getType() == BundleEvent.RESOLVED) {
			supplementerRegistry.addBundle(bundle);
		} else if (event.getType() == BundleEvent.UNRESOLVED) {
			supplementerRegistry.removeBundle(bundle);
		}
	}

}
