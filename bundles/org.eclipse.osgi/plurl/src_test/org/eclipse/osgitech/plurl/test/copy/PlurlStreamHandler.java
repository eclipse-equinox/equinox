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


public interface PlurlStreamHandler {

	public interface PlurlSetter {

		public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo,
				String path, String query, String ref);
	}

	public boolean equals(URL u1, URL u2);

	public int hashCode(URL u);

	public boolean hostsEqual(URL u1, URL u2);

	public int getDefaultPort();

	public InetAddress getHostAddress(URL u);

	public URLConnection openConnection(URL u) throws IOException;

	public URLConnection openConnection(URL u, Proxy p) throws IOException;

	public boolean sameFile(URL u1, URL u2);

	public String toExternalForm(URL u);

	public void parseURL(PlurlSetter plurlSetter, URL u, String spec, int start, int limit);

	public void setURL(URL u, String proto, String host, int port, String file, String ref);

	public void setURL(URL u, String proto, String host, int port, String auth, String user, String path,
			String query, String ref);
}
