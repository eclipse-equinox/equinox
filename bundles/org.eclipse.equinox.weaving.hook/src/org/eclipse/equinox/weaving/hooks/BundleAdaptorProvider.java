/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.wiring.BundleRevision;

public class BundleAdaptorProvider {

	private final IAdaptorProvider adaptorProvider;

	private final Generation generation;

	public BundleAdaptorProvider(final Generation generation, final IAdaptorProvider adaptorProvider) {
		this.generation = generation;
		this.adaptorProvider = adaptorProvider;
	}

	public IWeavingAdaptor getAdaptor() {

		if ((generation.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			return this.adaptorProvider.getHostBundleAdaptor(this.generation.getBundleInfo().getBundleId());
		} else {
			return this.adaptorProvider.getAdaptor(this.generation.getBundleInfo().getBundleId());
		}
	}

}
