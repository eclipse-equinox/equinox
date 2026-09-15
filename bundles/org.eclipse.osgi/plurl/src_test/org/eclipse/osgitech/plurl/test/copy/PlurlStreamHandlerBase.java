/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package org.eclipse.osgitech.plurl.test.copy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public abstract class PlurlStreamHandlerBase extends URLStreamHandler implements PlurlStreamHandler {
	private volatile PlurlSetter plurlSetter;

	@Override
	public abstract URLConnection openConnection(URL u) throws IOException;

	@Override
	public void parseURL(PlurlSetter setter, URL u, String spec, int start, int limit) {
		this.plurlSetter = setter;
		parseURL(u, spec, start, limit);
	}

	@Override
	public URLConnection openConnection(URL u, Proxy p) throws IOException {
		return super.openConnection(u, p);
	}

	@Override
	public String toExternalForm(URL u) {
		return super.toExternalForm(u);
	}

	@Override
	public boolean equals(URL u1, URL u2) {
		return super.equals(u1, u2);
	}

	@Override
	public int getDefaultPort() {
		return super.getDefaultPort();
	}

	@Override
	public InetAddress getHostAddress(URL u) {
		return super.getHostAddress(u);
	}

	@Override
	public int hashCode(URL u) {
		return super.hashCode(u);
	}

	@Override
	public boolean hostsEqual(URL u1, URL u2) {
		return super.hostsEqual(u1, u2);
	}

	@Override
	public boolean sameFile(URL u1, URL u2) {
		return super.sameFile(u1, u2);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setURL(URL u, String proto, String host, int port, String file, String ref) {
		PlurlSetter current = plurlSetter;
		if (current == null) {
			// something is calling the handler directly, probably passed it to URL directly
			super.setURL(u, proto, host, port, null, null, file, null, ref);
		} else {
			current.setURL(u, proto, host, port, null, null, file, null, ref);
		}
	}

	@Override
	public void setURL(URL u, String proto, String host, int port, String auth, String user, String path,
			String query, String ref) {
		PlurlSetter current = plurlSetter;
		if (current == null) {
			// something is calling the handler directly, probably passed it to URL directly
			super.setURL(u, proto, host, port, auth, user, path, query, ref);
		} else {
			current.setURL(u, proto, host, port, auth, user, path, query, ref);
		}
	}

}
