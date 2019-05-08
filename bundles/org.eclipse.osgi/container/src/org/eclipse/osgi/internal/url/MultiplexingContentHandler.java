/*******************************************************************************
 * Copyright (c) 2006, 2012 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 *******************************************************************************/
package org.eclipse.osgi.internal.url;

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLConnection;

public class MultiplexingContentHandler extends ContentHandler {

	private String contentType;
	private ContentHandlerFactoryImpl factory;

	public MultiplexingContentHandler(String contentType, ContentHandlerFactoryImpl factory) {
		this.contentType = contentType;
		this.factory = factory;
	}

	@Override
	public Object getContent(URLConnection uConn) throws IOException {
		ContentHandler handler = factory.findAuthorizedContentHandler(contentType);
		if (handler != null)
			return handler.getContent(uConn);

		return uConn.getInputStream();
	}

}
