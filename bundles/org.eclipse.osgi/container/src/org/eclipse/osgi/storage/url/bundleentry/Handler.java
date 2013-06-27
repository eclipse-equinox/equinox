/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.storage.url.bundleentry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.container.*;
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

	protected BundleEntry findBundleEntry(URL url, Module module) throws IOException {
		ModuleRevision revision = module.getCurrentRevision();
		BundleInfo.Generation revisionInfo = (BundleInfo.Generation) revision.getRevisionInfo();
		BundleEntry entry = revisionInfo == null ? null : revisionInfo.getBundleFile().getEntry(url.getPath());
		if (entry == null)
			throw new FileNotFoundException(url.getPath());
		return entry;
	}
}
