/*******************************************************************************
 * Copyright (c) 2015, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - adapt to equinox code base
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * Basic implementation of IETF RFC 2397, the "data" URL scheme
 * (http://tools.ietf.org/html/rfc2397).
 */
class DataURLStreamHandler extends AbstractURLStreamHandlerService {

	final static String PROTOCOL = "data"; //$NON-NLS-1$
	private static final String CHARSET_TOKEN = "charset="; //$NON-NLS-1$

	@Override
	public URLConnection openConnection(URL u) throws IOException {
		if (!PROTOCOL.equals(u.getProtocol())) {
			throw new MalformedURLException("Unsupported protocol"); //$NON-NLS-1$
		}
		return new DataURLConnection(u);
	}

	static class ParseResult {
		String mediaType = "text/plain"; //$NON-NLS-1$
		Charset charset;
		byte[]	data;
	}

	static final class DataURLConnection extends URLConnection {

		private ParseResult parsed;

		DataURLConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
			parsed = parse(url.getPath());
		}

		static ParseResult parse(String ssp) throws IOException {
			int commaIndex = ssp.indexOf(',');
			if (commaIndex < 0) {
				throw new MalformedURLException("missing comma"); //$NON-NLS-1$
			}

			String paramSegment = ssp.substring(0, commaIndex);
			String dataSegment = ssp.substring(commaIndex + 1);

			String mediaType = null;
			boolean base64 = false;
			Charset charset = null;

			StringTokenizer tokenizer = new StringTokenizer(paramSegment, ";"); //$NON-NLS-1$
			boolean first = true;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (first) {
					mediaType = token;
				} else if ("base64".equals(token)) { //$NON-NLS-1$
					base64 = true;
				} else if (token.startsWith(CHARSET_TOKEN)) {
					charset = Charset.forName(token.substring(CHARSET_TOKEN.length()));
				}
				first = false;
			}

			byte[] bytes;
			if (base64) {
				bytes = Base64.getDecoder().decode(dataSegment);
			} else {
				String decoded = URLDecoder.decode(dataSegment,
						Objects.requireNonNullElse(charset, StandardCharsets.US_ASCII));
				bytes = decoded.getBytes(StandardCharsets.UTF_8);
			}

			ParseResult parsed = new ParseResult();
			parsed.data = bytes;
			if (mediaType != null && !mediaType.isEmpty()) {
				parsed.mediaType = mediaType;
			}
			parsed.charset = charset;
			return parsed;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (parsed == null) {
				connect();
			}

			return new ByteArrayInputStream(parsed.data);
		}

		@Override
		public String getContentType() {
			return parsed != null ? parsed.mediaType : null;
		}

		@Override
		public String getContentEncoding() {
			if (parsed != null && parsed.charset != null) {
				return parsed.charset.name();
			}
			return null;
		}

	}

}
