/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol.reference;

import java.io.*;
import java.net.*;
import org.eclipse.osgi.framework.adaptor.core.ReferenceInputStream;

/**
 * URLConnection for the reference protocol.
 */

public class ReferenceURLConnection extends URLConnection {
	protected URL reference;

	protected ReferenceURLConnection(URL url) {
		super(url);
	}

	public synchronized void connect() throws IOException {
		if (!connected) {
			URL ref = new URL(url.getPath());
			if (!new File(ref.getFile()).exists())
				throw new FileNotFoundException();
			reference = ref;
		}
	}

	public boolean getDoInput() {
		return true;
	}

	public boolean getDoOutput() {
		return false;
	}

	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}

		return new ReferenceInputStream(reference);
	}

}