/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

package org.eclipse.equinox.internal.transforms;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

/**
 * This class is capable of providing a transformed version of an entry
 * contained within a base bundle entity.
 */
public class TransformedBundleEntry extends BundleEntry {

	long timestamp;
	private InputStream stream;
	private BundleEntry original;
	private TransformedBundleFile bundleFile;

	/**
	 * Create a wrapped bundle entry. Calls to obtain the content of this entry will
	 * be resolved via the provided input stream rather than the original.
	 * 
	 * @param bundleFile    the host bundle file
	 * @param original      the original entry
	 * @param wrappedStream the override stream
	 */
	public TransformedBundleEntry(TransformedBundleFile bundleFile, BundleEntry original, InputStream wrappedStream) {
		this.stream = wrappedStream;
		this.bundleFile = bundleFile;
		this.original = original;
		timestamp = System.currentTimeMillis();
	}

	@SuppressWarnings("deprecation")
	public URL getFileURL() {
		try {
			File file = bundleFile.getFile(getName(), false);
			if (file != null)
				return file.toURL();
		} catch (MalformedURLException e) {
			// This can not happen.
		}
		return null;
	}

	public InputStream getInputStream() {
		return stream;
	}

	public URL getLocalURL() {
		return getFileURL();
	}

	public String getName() {
		return original.getName();
	}

	/**
	 * Obtaining the size means inspecting the transformed stream. If this stream
	 * does not support marks the stream is drained and a copy is retained for later
	 * use.
	 */
	public long getSize() {
		ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream(1024);
		byte[] buffer = new byte[1024];
		int i = 0;
		try {
			while ((i = stream.read(buffer)) > -1) {
				tempBuffer.write(buffer, 0, i);
			}
			if (stream.markSupported()) {
				try {
					stream.reset();
				} catch (IOException e) {
					stream = new ByteArrayInputStream(tempBuffer.toByteArray());
				}
			} else {
				stream = new ByteArrayInputStream(tempBuffer.toByteArray());
			}
		} catch (IOException e) {
			bundleFile.getGeneration().getBundleInfo().getStorage().getLogServices().log(EquinoxContainer.NAME,
					FrameworkLogEntry.ERROR,
					"Problem calculating size of stream for file.  Stream may now be corrupted : " //$NON-NLS-1$
							+ getName(),
					e);
		}
		return tempBuffer.size();

	}

	public long getTime() {
		return timestamp;
	}
}
