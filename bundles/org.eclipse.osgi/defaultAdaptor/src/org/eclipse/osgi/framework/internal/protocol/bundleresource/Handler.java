/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol.bundleresource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.BundleResourceHandler;

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

	public Handler(BundleEntry bundleEntry) {
		super(bundleEntry);
	}

	protected BundleEntry findBundleEntry(URL url, AbstractBundle bundle) throws IOException {
		BaseClassLoader classloader = getBundleClassLoader(bundle);
		if (classloader == null)
			throw new FileNotFoundException(url.getPath());
		ClasspathManager cpManager = classloader.getClasspathManager();
		int index = url.getPort();
		BundleEntry entry = null;
		if (index == 0) {
			entry = cpManager.findLocalEntry(url.getPath());
		} else {
			Enumeration entries = cpManager.findLocalEntries(url.getPath());
			if (entries != null)
				for (int i = 0; entries.hasMoreElements() && i <= index; i++)
					entry = (BundleEntry) entries.nextElement();
		}
		if (entry == null)
			throw new FileNotFoundException(url.getPath());
		return entry;
	}

}
