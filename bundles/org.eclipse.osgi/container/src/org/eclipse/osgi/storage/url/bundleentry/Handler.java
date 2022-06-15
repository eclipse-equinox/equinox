/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.storage.url.bundleentry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.url.BundleResourceHandler;

/**
 * URLStreamHandler the bundleentry protocol.
 */

public class Handler extends BundleResourceHandler {

	public Handler(ModuleContainer container, BundleEntry bundleEntry) {
		super(container, bundleEntry);
	}

	@Override
	protected BundleEntry findBundleEntry(URL url, Module module) throws IOException {
		ModuleRevision revision = module.getCurrentRevision();
		BundleInfo.Generation revisionInfo = (BundleInfo.Generation) revision.getRevisionInfo();
		BundleEntry entry = revisionInfo == null ? null : revisionInfo.getBundleFile().getEntry(url.getPath());
		if (entry == null) {
			String path = url.getPath();
			if (revisionInfo != null && (path.indexOf('%') >= 0 || path.indexOf('+') >= 0)) {
				entry = revisionInfo.getBundleFile().getEntry(LocationHelper.decode(path, true));
				if (entry != null) {
					return entry;
				}
				entry = revisionInfo.getBundleFile().getEntry(LocationHelper.decode(path, false));
				if (entry != null) {
					return entry;
				}
			}
			throw new FileNotFoundException(url.getPath());
		}
		return entry;
	}
}
