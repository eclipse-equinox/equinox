/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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

package org.eclipse.osgi.internal.url;

import java.net.URL;

public class URLStreamHandlerSetter implements org.osgi.service.url.URLStreamHandlerSetter {

	protected URLStreamHandlerProxy handlerProxy;

	public URLStreamHandlerSetter(URLStreamHandlerProxy handler) {
		this.handlerProxy = handler;
	}

	/**
	 * @see org.osgi.service.url.URLStreamHandlerSetter#setURL(URL, String, String,
	 *      int, String, String)
	 * @deprecated
	 */
	@Override
	public void setURL(URL url, String protocol, String host, int port, String file, String ref) {
		handlerProxy.setURL(url, protocol, host, port, file, ref);
	}

	/**
	 * @see org.osgi.service.url.URLStreamHandlerSetter#setURL(URL, String, String,
	 *      int, String, String, String, String, String)
	 */
	@Override
	public void setURL(URL url, String protocol, String host, int port, String authority, String userInfo, String path,
			String query, String ref) {
		handlerProxy.setURL(url, protocol, host, port, authority, userInfo, path, query, ref);
	}

}
