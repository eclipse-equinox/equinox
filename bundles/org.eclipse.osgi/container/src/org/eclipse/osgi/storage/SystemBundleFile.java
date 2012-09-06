/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.framework.BundleException;

public class SystemBundleFile extends BundleFile {

	public SystemBundleFile() {
		super(null);
	}

	@Override
	public File getFile(String path, boolean nativeCode) {
		return null;
	}

	@Override
	public BundleEntry getEntry(String path) {
		if (BundleInfo.OSGI_BUNDLE_MANIFEST.equals(path)) {
			return new BundleEntry() {

				public InputStream getInputStream() throws IOException {
					return getManifestURL().openStream();
				}

				public long getSize() {
					return 0;
				}

				public String getName() {
					return BundleInfo.OSGI_BUNDLE_MANIFEST;
				}

				public long getTime() {
					return 0;
				}

				public URL getLocalURL() {
					return getManifestURL();
				}

				public URL getFileURL() {
					return null;
				}
			};
		}
		return null;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return null;
	}

	@Override
	public void close() throws IOException {
		// nothing
	}

	@Override
	public void open() throws IOException {
		// nothing
	}

	@Override
	public boolean containsDir(String dir) {
		return false;
	}

	URL getManifestURL() {
		ClassLoader cl = getClass().getClassLoader();
		try {
			// get all manifests in your classloader delegation
			Enumeration<URL> manifests = cl != null ? cl.getResources(BundleInfo.OSGI_BUNDLE_MANIFEST) : ClassLoader.getSystemResources(BundleInfo.OSGI_BUNDLE_MANIFEST);
			while (manifests.hasMoreElements()) {
				URL url = manifests.nextElement();
				try {
					// check each manifest until we find one with the Eclipse-SystemBundle: true header
					Headers<String, String> headers = Headers.parseManifest(url.openStream());
					if ("true".equals(headers.get(Storage.ECLIPSE_SYSTEMBUNDLE))) //$NON-NLS-1$
						return url;
				} catch (BundleException e) {
					// ignore and continue to next URL
				}
			}
		} catch (IOException e) {
			// ignore and return null
		}
		return null;
	}
}
