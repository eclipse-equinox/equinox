/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * The ServletRequestAdvisor is responsible for composing URLs and using them
 * to performing servlet requests.
 */
public class ServletRequestAdvisor extends Object {
	private final String contextPath;
	private final String port;

	public ServletRequestAdvisor(String port, String contextPath) {
		super();
		if (port == null)
		 {
			throw new IllegalArgumentException("port must not be null"); //$NON-NLS-1$
		}
		this.port = port;
		this.contextPath = contextPath;
	}

	private String createUrlSpec(String value) {
		StringBuffer buffer = new StringBuffer(100);
		String protocol = "http://"; //$NON-NLS-1$
		String host = "localhost"; //$NON-NLS-1$
		buffer.append(protocol);
		buffer.append(host);
		buffer.append(':');
		buffer.append(port);
		buffer.append(contextPath);
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

		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(150 * 1000);
		connection.setReadTimeout(150 * 1000);
		connection.connect();

		InputStream stream = connection.getInputStream();
		try {
			return drain(stream);
		} finally {
			stream.close();
		}
	}

	public Map<String, List<String>> request(String value, Map<String, List<String>> headers) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();

		if (headers != null) {
			for(Map.Entry<String, List<String>> entry : headers.entrySet()) {
				for(String entryValue : entry.getValue()) {
					conn.setRequestProperty(entry.getKey(), entryValue);
				}
			}
		}

		int responseCode = conn.getResponseCode();

		Map<String, List<String>> map = new HashMap<String, List<String>>(conn.getHeaderFields());
		map.put("responseCode", Collections.singletonList(String.valueOf(responseCode)));

		InputStream stream;

		if (responseCode >= 400) {
			stream = conn.getErrorStream();
		}
		else {
			stream = conn.getInputStream();
		}

		try {
			map.put("responseBody", Arrays.asList(drain(stream)));
			return map;
		} finally {
			stream.close();
		}
	}
}
