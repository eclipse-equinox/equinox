/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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

package org.eclipse.osgi.storage.url.reference;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URLStreamHandler for reference protocol. A reference URL is used to hold a
 * reference to a local file URL. A reference URL allows bundles to be installed
 * by reference. This means the content of the bundle will not be copied.
 * Instead the content of the bundle will be loaded from the reference location
 * specified by the reference URL. The Framework only supports reference URLs
 * that refer to a local file URL. For example:
 * 
 * <pre>
 *     reference:file:/eclipse/plugins/org.eclipse.myplugin_1.0.0/
 *     reference:file:/eclispe/plugins/org.eclipse.mybundle_1.0.0.jar
 * </pre>
 */
public class Handler extends URLStreamHandler {

	private final String installPath;

	public Handler(String installURL) {
		super();
		if (installURL != null && installURL.startsWith("file:")) { //$NON-NLS-1$
			// this is the safest way to create a File object off a file: URL
			this.installPath = installURL.substring(5);
		} else {
			this.installPath = null;
		}
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new ReferenceURLConnection(url, installPath);
	}

	@Override
	protected void parseURL(URL url, String str, int start, int end) {
		if (end < start) {
			return;
		}
		String reference = (start < end) ? str.substring(start, end) : url.getPath();

		setURL(url, url.getProtocol(), null, -1, null, null, reference, null, null);
	}

}
