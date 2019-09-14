/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import static org.junit.Assert.assertEquals;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/*
 * The ServletRequestAdvisor is responsible for composing URLs and using them
 * to performing servlet requests.
 */
public class ServletRequestAdvisor extends Object {
	private final String contextPath;
	private final String port;
	private final String ksPath;
	private final String ksPassword;
	private final int timeout;

	public ServletRequestAdvisor(String port, String contextPath) {
		this(port, contextPath, null, null, 150);
	}

	public ServletRequestAdvisor(String port, String contextPath, int timeout) {
		this(port, contextPath, null, null, timeout);
	}

	public ServletRequestAdvisor(String port, String contextPath, String ksPath, String ksPassword) {
		this(port, contextPath, ksPath, ksPassword, 150);
	}

	public ServletRequestAdvisor(String port, String contextPath, String ksPath, String ksPassword, int timeout) {
		super();
		if (port == null)
		{
			throw new IllegalArgumentException("port must not be null"); //$NON-NLS-1$
		}
		this.port = port;
		this.contextPath = contextPath;
		this.ksPath = ksPath;
		this.ksPassword = ksPassword;
		this.timeout = timeout * 100000;
	}

	private String createUrlSpec(String value, boolean isHttps) {
		StringBuilder buffer = new StringBuilder(100);
		String protocol = "http://"; //$NON-NLS-1$
		if (isHttps) {
			protocol = "https://";
		}
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

	private String createUrlSpec(String value) {
		return createUrlSpec(value, false);
	}

	private String drain(InputStream stream) throws IOException {
		byte[] bytes = new byte[100];
		StringBuilder buffer = new StringBuilder(500);
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
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.connect();

		InputStream stream = connection.getInputStream();
		try {
			return drain(stream);
		} finally {
			stream.close();
		}
	}

	public String requestHttps(String value) throws Exception {
		String spec = createUrlSpec(value, true);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		SSLContext sslContext = SSLContext.getInstance("SSL");
		initializeSSLContext(sslContext, ksPath, ksPassword);

		HttpsURLConnection httpsConn = (HttpsURLConnection)url.openConnection();
		httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
		httpsConn.setRequestMethod("GET");
		httpsConn.setDoOutput(false);
		httpsConn.setDoInput(true);
		httpsConn.setConnectTimeout(timeout);
		httpsConn.setReadTimeout(timeout);
		httpsConn.connect();

		assertEquals("Request to the url " + spec + " was not successful", 200 , httpsConn.getResponseCode());
		InputStream stream = httpsConn.getInputStream();
		try {
			return drain(stream);
		} finally {
			stream.close();
		}
	}

	private void initializeSSLContext(SSLContext sslContext, String ksPath, String ksPassword) throws Exception {
		KeyManager keyManagers[] = null;
		if (ksPath != null) {
			KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			File ksFile = new File(ksPath);
			KeyStore keyStore = KeyStore.getInstance("JKS");

			try(InputStream ksStream = new FileInputStream(ksFile)){
				keyStore.load(ksStream, ksPassword.toCharArray());
				kmFactory.init(keyStore, ksPassword.toCharArray());
				keyManagers = kmFactory.getKeyManagers();
			}
		}

		TrustManager[] trustManagers = getTrustManager();

		sslContext.init(keyManagers, trustManagers, null);

	}

	private TrustManager[] getTrustManager() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(
										java.security.cert.X509Certificate[] certs, String authType) {}

			@Override
			public void checkServerTrusted(
										java.security.cert.X509Certificate[] certs, String authType) {}
		} };

		return trustAllCerts;
	}

	public Map<String, List<String>> request(String value, Map<String, List<String>> headers) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

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

	public Map<String, List<String>> eventSource(String value, Map<String, List<String>> headers, final EventHandler handler) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setChunkedStreamingMode(0);
		connection.setDoOutput(true);
		//connection.setRequestProperty("Connection", "Close");
		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

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

		handler.open(stream);

		return map;
	}

	public Map<String, List<String>> upload(String value, Map<String, List<Object>> headers) throws IOException {
		String spec = createUrlSpec(value);
		log("Requesting " + spec); //$NON-NLS-1$
		URL url = new URL(spec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setUseCaches(false);

		if (headers != null) {
			if (headers.containsKey("method")) {
				String method = (String)headers.remove("method").get(0);
				connection.setRequestMethod(method);
			}

			for(Map.Entry<String, List<Object>> entry : headers.entrySet()) {
				for(Object entryValue : entry.getValue()) {
					if (entryValue instanceof String) {
						if (entry.getKey().equals("x-www-form-urlencoded")) {
							postFormURLEncoded(connection, (String)entryValue);
						}
						else {
							connection.setRequestProperty(entry.getKey(), (String)entryValue);
						}
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

	private void postFormURLEncoded(HttpURLConnection connection, String param)
		throws IOException {

		byte[] bytes = param.getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		//connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

		try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
			wr.write(bytes);
			wr.flush();
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
			writer.append("Content-Disposition: form-data; name=\"");
			writer.append(param);
			writer.append("\"; filename=\"");
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
