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
import java.net.URLConnection;
import org.eclipse.osgi.storage.ContentProvider;

public class ConnectInputStream extends InputStream implements ContentProvider {
	static final ConnectInputStream INSTANCE = new ConnectInputStream();
	static final URLConnection URL_CONNECTION_INSTANCE = new URLConnection(null) {
		@Override
		public void connect() throws IOException {
			connected = true;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return INSTANCE;
		}
	};

	private ConnectInputStream() {
	}

	/* This method should not be called.
	 */
	@Override
	public int read() throws IOException {
		throw new IOException();
	}

	public File getContent() {
		return null;
	}

	@Override
	public Type getType() {
		return Type.CONNECT;
	}

}
