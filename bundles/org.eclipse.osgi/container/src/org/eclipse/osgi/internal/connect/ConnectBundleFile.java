/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.osgi.internal.connect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.CloseableBundleFile;
import org.eclipse.osgi.storage.bundlefile.MRUBundleFileList;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectContent.ConnectEntry;
import org.osgi.framework.connect.ConnectModule;

public class ConnectBundleFile extends CloseableBundleFile<ConnectEntry> {
	public class ConnectBundleEntry extends BundleEntry {
		private final ConnectEntry connectEntry;

		public ConnectBundleEntry(ConnectEntry entry) {
			this.connectEntry = entry;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return ConnectBundleFile.this.getInputStream(connectEntry);
		}

		@Override
		public byte[] getBytes() throws IOException {
			return connectEntry.getBytes();
		}

		@Override
		public long getSize() {
			return connectEntry.getContentLength();
		}

		@Override
		public String getName() {
			return connectEntry.getName();
		}

		@Override
		public long getTime() {
			return connectEntry.getLastModified();
		}

		@Override
		public URL getFileURL() {
			File file = ConnectBundleFile.this.getFile(getName(), false);
			if (file != null) {
				try {
					return file.toURI().toURL();
				} catch (MalformedURLException e) {
					// should never happen
				}
			}
			return null;
		}

		@Override
		public URL getLocalURL() {
			return ConnectBundleFile.this.getClassLoader().map(cl -> cl.getResource(getName())).orElse(null);
		}
	}

	private final ConnectContent content;

	public ConnectBundleFile(ConnectModule module, File basefile, BundleInfo.Generation generation, MRUBundleFileList mruList, Debug debug) throws IOException {
		super(basefile, generation, mruList, debug);
		this.content = module.getContent();
	}

	@Override
	protected void doOpen() throws IOException {
		content.open();
	}

	@Override
	protected Iterable<String> getPaths() {
		try {
			return content.getEntries();
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Override
	protected BundleEntry findEntry(String path) {
		if (path.length() > 0 && path.charAt(0) == '/') {
			path = path.substring(1);
		}
		return content.getEntry(path).map(ConnectBundleEntry::new).orElse(null);
	}

	@Override
	protected void doClose() throws IOException {
		content.close();
	}

	@Override
	protected void postClose() {
		// do nothing
	}

	@Override
	protected InputStream doGetInputStream(ConnectEntry entry) throws IOException {
		return entry.getInputStream();
	}

	public Map<String, String> getConnectHeaders() {
		if (!lockOpen()) {
			return null;
		}
		try {
			return content.getHeaders().orElse(null);
		} finally {
			releaseOpen();
		}
	}

	Optional<ClassLoader> getClassLoader() {
		return content.getClassLoader();
	}

	@Override
	public String toString() {
		return content.toString();
	}
}
