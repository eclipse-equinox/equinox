/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.io.util;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class URI {

	/*
	 * <scheme>://<net_loc>/<path>;<params>?<query>#<fragment>
	 */
	private String scheme;
	private String schemeSpec;
	private String auth;
	private String userInfo;
	private String host;
	private String port;
	private String path;
	private String query;
	private String fragment;
	private boolean isOpaque;

	private Hashtable params;

	public URI(String uri) {
		if (uri == null) {
			throw new IllegalArgumentException("URL cannot be NULL!");
		}

		byte[] buf = uri.getBytes();
		int sPos = indexOf(buf, ':', 0);

		scheme = trim(buf, 0, sPos);

		sPos++;

		int ePos = indexOf(buf, '#', sPos);

		if (ePos != -1) {
			fragment = trim(buf, ePos + 1, buf.length);
		} else {
			ePos = buf.length;
		}

		schemeSpec = trim(buf, sPos, ePos);

		int pos = indexOf(buf, '?', sPos, ePos);

		if (pos != -1) {
			query = trim(buf, pos + 1, ePos);
			ePos = pos;
		}

		pos = indexOf(buf, ';', sPos, ePos);

		if (pos != -1) {
			parseParams(buf, pos + 1, ePos);
			ePos = pos;
		}

		isOpaque = true;

		if (buf[sPos] == '/' && buf[sPos + 1] == '/') {
			sPos += 2;
			isOpaque = false;
		}

		pos = indexOf(buf, '/', sPos, ePos);

		if (pos != -1) {
			path = trim(buf, pos, ePos);
			ePos = pos;
		}

		auth = trim(buf, sPos, ePos);

		if (!isOpaque) {
			pos = indexOf(buf, '@', sPos);

			if (pos != -1) {
				userInfo = trim(buf, sPos, pos);
				sPos = pos + 1;
			}

			pos = indexOf(buf, ']', sPos);
			if (pos != -1) {
				host = trim(buf, sPos, pos + 1);
				sPos = pos + 1;
			}

			pos = indexOf(buf, ':', sPos);

			if (pos != -1) {
				port = trim(buf, pos + 1, ePos);
				ePos = pos;
			}
			if (host == null) {
				host = trim(buf, sPos, ePos);
			}
		}
	}

	public String getScheme() {
		return scheme;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	public int getPortNumber() {
		return port == null ? -1 : Integer.parseInt(port);
	}

	public String getFragment() {
		return fragment;
	}

	public String getQuery() {
		return query;
	}

	public String getUserInfo() {
		return userInfo;
	}

	public String getPath() {
		return path;
	}

	public String getSchemeSpecificPart() {
		return schemeSpec;
	}

	public String getAuthority() {
		return auth;
	}

	public boolean isOpaque() {
		return isOpaque;
	}

	public String get(String param) {
		return params == null ? null : (String) params.get(param);
	}

	public String getParams() {
		if (params == null || params.isEmpty()) {
			return null;
		}

		StringBuffer sb = new StringBuffer();

		for (Enumeration en = params.keys(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			sb.append(";");
			sb.append(key);
			sb.append("=");
			sb.append(((String) params.get(key)));
		}

		if (sb.length() == 0) {
			return null;
		}

		return sb.toString();
	}

	public static String getHost(String uri) {
		if (uri == null) {
			throw new IllegalArgumentException("URL cannot be NULL!");
		}

		int pos = uri.indexOf("://");

		if (pos == -1) {
			throw new IllegalArgumentException("Does not have scheme");
		}

		int sPos = pos + 3;

		pos = uri.indexOf('@', sPos);
		if (pos != -1) {
			sPos = pos + 1;
		}

		int ePos = endOfHostPort(uri, sPos);

		pos = uri.indexOf(']', sPos);
		if (pos != -1) {
			return uri.substring(sPos, pos + 1);
		}

		pos = uri.indexOf(':', sPos);

		if (sPos != -1 && pos < ePos) {
			return uri.substring(sPos, pos);
		}

		return uri.substring(sPos, ePos);
	}

	public static int getPort(String uri) {
		if (uri == null) {
			throw new IllegalArgumentException("URL cannot be NULL!");
		}

		int pos = uri.indexOf("://");

		if (pos == -1) {
			throw new IllegalArgumentException("Does not have scheme");
		}

		int sPos = pos + 3;

		pos = uri.indexOf('@', sPos);
		if (pos != -1) {
			sPos = pos + 1;
		}

		pos = uri.indexOf(']', sPos);
		if (pos != -1) {
			sPos = pos + 1;
		}

		sPos = uri.indexOf(":", sPos);

		if (sPos == -1) {
			// return -1;
			throw new IllegalArgumentException("Port is missing " + uri);
		}

		sPos++;

		int ePos = endOfHostPort(uri, sPos);

		return Integer.parseInt(uri.substring(sPos, ePos));
	}

	static int endOfHostPort(String str, int spos) {
		int pos = str.indexOf("/", spos);
		int tmp = str.indexOf(";", spos);

		if (tmp != -1 && tmp < pos || pos == -1) {
			pos = tmp;
		}

		if (pos != -1) {
			return pos;
		}

		return str.length();
	}

	static int indexOf(byte[] ba, char b, int startPos) {
		return indexOf(ba, b, startPos, ba.length);
	}

	static int indexOf(byte[] ba, char b, int sPos, int ePos) {
		if (sPos > ba.length || sPos < 0 || ePos > ba.length || ePos < 0 || sPos >= ePos) {
			return -1;// throw new IllegalArgumentException();
		}

		for (int i = sPos; i < ePos; i++) {
			if (ba[i] == b) {
				return i;
			}
		}

		return -1;
	}

	static String trim(byte[] buf, int s, int e) {
		if (s >= e) {
			return null;
		}

		while (s < buf.length && s < e && buf[s] == ' ') {
			s++;
		}

		while (e >= 0 && e > s && buf[e - 1] == ' ') {
			e--;
		}

		if (s == e) {
			return null;
		}

		return new String(buf, s, e - s);
	}

	private void parseParams(byte[] buf, int sPos, int ePos) {
		int pos;

		while (sPos < ePos && (pos = indexOf(buf, ';', sPos)) != -1) {
			parseParam(buf, sPos, pos);
			sPos = pos + 1;
		}

		if (sPos < ePos) {
			parseParam(buf, sPos, ePos);
		}
	}

	private void parseParam(byte[] buf, int sPos, int ePos) {
		int rpos = indexOf(buf, '=', sPos, ePos);

		if (rpos != -1) {
			String key = trim(buf, sPos, rpos);

			if (key == null) {
				return;
			}

			String str = trim(buf, rpos + 1, ePos);

			if (str != null) {
				if (params == null) {
					params = new Hashtable();
				}

				params.put(key, str);
			}
		}
	}

}
