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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;

/**
 * URLConnection for the reference protocol.
 */

public class ReferenceURLConnection extends URLConnection {
	private final String installPath;
	private volatile File reference;

	protected ReferenceURLConnection(URL url, String installPath) {
		super(url);
		this.installPath = installPath;
	}

	@Override
	public synchronized void connect() throws IOException {
		if (!connected) {
			// TODO assumes that reference URLs are always based on file: URLs.
			// There are not solid usecases to the contrary. Yet.
			// Construct the ref File carefully so as to preserve UNC paths etc.
			String path = url.getPath();
			if (!path.startsWith("file:")) { //$NON-NLS-1$
				throw new IOException(NLS.bind(Msg.ADAPTOR_URL_CREATE_EXCEPTION, path));
			}
			path = url.getPath().substring(5);
			File file = new File(path);

			if (!file.isAbsolute()) {
				if (installPath != null)
					file = makeAbsolute(installPath, file);
			}

			file = LocationHelper.decodePath(file);

			checkRead(file);

			reference = file;
			connected = true;
		}
	}

	private void checkRead(File file) throws IOException {
		if (!file.exists())
			throw new FileNotFoundException(file.toString());
		if (file.isFile()) {
			// Try to open the file to ensure that this is possible: see bug 260217
			// If access is denied, a FileNotFoundException with (access denied) message is thrown
			// Here file.canRead() cannot be used, because on Windows it does not 
			// return correct values - bug 6203387 in Sun's bug database
			InputStream is = new FileInputStream(file);
			is.close();
		} else if (file.isDirectory()) {
			// There is no straightforward way to check if a directory
			// has read permissions - same issues for File.canRead() as above; 
			// try to list the files in the directory
			File[] files = file.listFiles();
			// File.listFiles() returns null if the directory does not exist 
			// (which is not the current case, because we check that it exists and is directory),
			// or if an IO error occurred during the listing of the files, including if the
			// access is denied 
			if (files == null)
				throw new FileNotFoundException(file.toString() + " (probably access denied)"); //$NON-NLS-1$
		} else {
			// TODO not sure if we can get here.
		}
	}

	@Override
	public boolean getDoInput() {
		return true;
	}

	@Override
	public boolean getDoOutput() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}

		return new ReferenceInputStream(reference);
	}

	private static File makeAbsolute(String base, File relative) {
		if (relative.isAbsolute())
			return relative;
		return new File(new FilePath(base + relative.getPath()).toString());
	}
}
