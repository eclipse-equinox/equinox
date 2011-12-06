/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/*
 * The ServletRequestAdvisor is responsible for composing URLs and using them
 * to performing servlet requests. 
 */
public class ServletRequestAdvisor extends Object {
	private final String port;

	public ServletRequestAdvisor(String port) {
		super();
		if (port == null)
			throw new IllegalArgumentException("port must not be null"); //$NON-NLS-1$
		this.port = port;
	}

	private String createUrlSpec(String value) {
		StringBuffer buffer = new StringBuffer(100);
		String protocol = "http://"; //$NON-NLS-1$
		String host = "localhost"; //$NON-NLS-1$
		buffer.append(protocol);
		buffer.append(host);
		buffer.append(':');
		buffer.append(port);
		if (value != null) {
			buffer.append('/');
			buffer.append(value);
		}
		return buffer.toString();
	}

	private String drain(InputStream stream) throws IOException {
		byte[] bytes = new byte[100];
		StringBuffer buffer = new StringBuffer(500);
		int length;
		while ((length = stream.read(bytes)) != -1) {
			String chunk = new String(bytes, 0, length);
			buffer.append(chunk);
		}
		return buffer.toString();
	}

	private void log(String message) {
		String value = this + ": " + message; //$NON-NLS-1$
		System.out.println(value);
	}
	
	public String request(String value) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		InputStream stream = url.openStream();
		try {
			return drain(stream);
		} finally {
			stream.close();
		}
	}
}
