/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.storage.url.bundleresource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.url.BundleResourceHandler;

/**
 * URLStreamHandler the bundleresource protocol.
 */

public class Handler extends BundleResourceHandler {

	public Handler(ModuleContainer container, BundleEntry bundleEntry) {
		super(container, bundleEntry);
	}

	protected BundleEntry findBundleEntry(URL url, Module module) throws IOException {
		ModuleRevision current = module.getCurrentRevision();
		ModuleWiring wiring = current == null ? null : current.getWiring();
		ModuleClassLoader classloader = (ModuleClassLoader) (current == null ? null : wiring.getClassLoader());
		if (classloader == null)
			throw new FileNotFoundException(url.getPath());
		BundleEntry entry = classloader.getClasspathManager().findLocalEntry(url.getPath(), url.getPort());
		if (entry == null) {
			// this isn't strictly needed but is kept to maintain compatibility
			entry = classloader.getClasspathManager().findLocalEntry(url.getPath());
		}
		if (entry == null) {
			throw new FileNotFoundException(url.getPath());
		}
		return entry;
	}

}
