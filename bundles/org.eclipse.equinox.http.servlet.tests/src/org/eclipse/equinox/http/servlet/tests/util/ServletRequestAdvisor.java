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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(150 * 1000);
		connection.setReadTimeout(150 * 1000);

		if (headers != null) {
			for(Map.Entry<String, List<String>> entry : headers.entrySet()) {
				for(String entryValue : entry.getValue()) {
					connection.setRequestProperty(entry.getKey(), entryValue);
				}
			}
		}

		int responseCode = connection.getResponseCode();

		Map<String, List<String>> map = new HashMap<String, List<String>>(connection.getHeaderFields());
		map.put("responseCode", Collections.singletonList(String.valueOf(responseCode)));

		InputStream stream;

		if (responseCode >= 400) {
			stream = connection.getErrorStream();
		}
		else {
			stream = connection.getInputStream();
		}

		try {
			map.put("responseBody", Arrays.asList(drain(stream)));
			return map;
		} finally {
			stream.close();
		}
	}

	public Map<String, List<String>> upload(String value, Map<String, List<Object>> headers) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(150 * 1000);
		connection.setReadTimeout(150 * 1000);

		if (headers != null) {
			for(Map.Entry<String, List<Object>> entry : headers.entrySet()) {
				for(Object entryValue : entry.getValue()) {
					if (entryValue instanceof String) {
						connection.setRequestProperty(entry.getKey(), (String)entryValue);
					}
					else if (entryValue instanceof URL) {
						uploadFileConnection(connection, entry.getKey(), (URL)entryValue);
					}
					else {
						throw new IllegalArgumentException("only supports strings and files");
					}
				}
			}
		}

		int responseCode = connection.getResponseCode();

		Map<String, List<String>> map = new HashMap<String, List<String>>(connection.getHeaderFields());
		map.put("responseCode", Collections.singletonList(String.valueOf(responseCode)));

		InputStream stream;

		if (responseCode >= 400) {
			stream = connection.getErrorStream();
		}
		else {
			stream = connection.getInputStream();
		}

		try {
			map.put("responseBody", Arrays.asList(drain(stream)));
			return map;
		} finally {
			stream.close();
		}
	}

	private void uploadFileConnection(HttpURLConnection connection, String param, URL file)
		throws IOException {

		String fileName = file.getPath();
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		connection.setDoOutput(true);

		String boundary = Long.toHexString(System.currentTimeMillis());
		String CRLF = "\r\n";
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		InputStream input = null;
		OutputStream output = null;
		PrintWriter writer = null;

		try {
			output = connection.getOutputStream();
			writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

			writer.append("--" + boundary);
			writer.append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
			writer.append(fileName);
			writer.append("\"");
			writer.append(CRLF);
			writer.append("Content-Type: ");
			String contentType = URLConnection.guessContentTypeFromName(fileName);
			writer.append(contentType);
			writer.append(CRLF);
			if (!contentType.startsWith("text/")) {
				writer.append("Content-Transfer-Encoding: binary");
				writer.append(CRLF);
			}
			writer.append(CRLF);
			writer.flush();

			byte[] buf = new byte[64];
			input = file.openStream();
			int c = 0;
			while ((c = input.read(buf, 0, buf.length)) > 0) {
				output.write(buf, 0, c);
				output.flush();
			}

			output.flush(); // Important before continuing with writer!
			writer.append(CRLF); // CRLF is important! It indicates end of boundary.
			writer.flush();

			// End of multipart/form-data.
			writer.append("--" + boundary + "--");
			writer.append(CRLF);
			writer.flush();
		}
		finally {
			if (input != null) {
				input.close();
			}
			if (output != null) {
				output.close();
			}
			if (writer != null) {
				writer.close();
			}
		}
	}

}
