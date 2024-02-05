/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.util.ManifestElement;
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

				@Override
				public InputStream getInputStream() throws IOException {
					return getManifestURL().openStream();
				}

				@Override
				public long getSize() {
					return 0;
				}

				@Override
				public String getName() {
					return BundleInfo.OSGI_BUNDLE_MANIFEST;
				}

				@Override
				public long getTime() {
					return 0;
				}

				@Override
				public URL getLocalURL() {
					return getManifestURL();
				}

				@Override
				public URL getFileURL() {
					return null;
				}
			};
		}
		return null;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path, boolean recurse) {
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
			Enumeration<URL> manifests = cl != null ? cl.getResources(BundleInfo.OSGI_BUNDLE_MANIFEST)
					: ClassLoader.getSystemResources(BundleInfo.OSGI_BUNDLE_MANIFEST);
			while (manifests.hasMoreElements()) {
				URL url = manifests.nextElement();
				try {
					// check each manifest until we find one with the Eclipse-SystemBundle: true
					// header
					Map<String, String> headers = ManifestElement.parseBundleManifest(url.openStream(),
							new CaseInsensitiveDictionaryMap<>());
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
