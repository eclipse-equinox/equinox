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

package org.eclipse.osgi.framework.internal.protocol.bundleresource;

import org.eclipse.osgi.storage.url.BundleResourceHandler;

import org.eclipse.osgi.internal.loader.classpath.BaseClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;

/**
 * URLStreamHandler the bundleresource protocol.
 */

public class Handler extends BundleResourceHandler {

	/**
	 * Constructor for a bundle protocol resource URLStreamHandler.
	 */
	public Handler() {
		super();
	}

	public Handler(BundleEntry bundleEntry, BaseAdaptor adaptor) {
		super(bundleEntry, adaptor);
	}

	protected BundleEntry findBundleEntry(URL url, AbstractBundle bundle) throws IOException {
		BaseClassLoader classloader = getBundleClassLoader(bundle);
		if (classloader == null)
			throw new FileNotFoundException(url.getPath());
		ClasspathManager cpManager = classloader.getClasspathManager();
		BundleEntry entry = cpManager.findLocalEntry(url.getPath(), url.getPort());
		if (entry == null)
			// this isn't strictly needed but is kept to maintain compatibility
			entry = cpManager.findLocalEntry(url.getPath());
		if (entry == null)
			throw new FileNotFoundException(url.getPath());
		return entry;
	}

}
