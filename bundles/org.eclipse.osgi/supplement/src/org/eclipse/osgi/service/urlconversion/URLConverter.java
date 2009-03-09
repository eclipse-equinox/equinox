/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.urlconversion;

import java.io.IOException;
import java.net.URL;

/**
 * The interface of the service that allows client-defined protocol
 * URLs to be converted to native file URLs on the local file system.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.1
 */
public interface URLConverter {

	/**
	 * Converts a URL that uses a user-defined protocol into a URL that uses the file
	 * protocol. The contents of the URL may be extracted into a cache on the file-system
	 * in order to get a file URL. 
	 * <p>
	 * If the protocol for the given URL is not recognized by this converter, the original
	 * URL is returned as-is.
	 * </p>
	 * @param url the original URL
	 * @return the converted file URL or the original URL passed in if it is 
	 * 	not recognized by this converter
	 * @throws IOException if an error occurs during the conversion
	 * @since 3.2
	 */
	public URL toFileURL(URL url) throws IOException;

	/**
	 * Converts a URL that uses a client-defined protocol into a URL that uses a
	 * protocol which is native to the Java class library (file, jar, http, etc).
	 * <p>
	 * Note however that users of this API should not assume too much about the
	 * results of this method.  While it may consistently return a file: URL in certain
	 * installation configurations, others may result in jar: or http: URLs.
	 * </p>
	 * <p>
	 * If the protocol is not recognized by this converter, then the original URL is
	 * returned as-is.
	 * </p>
	 * @param url the original URL
	 * @return the resolved URL or the original if the protocol is unknown to this converter
	 * @exception IOException if unable to resolve URL
	 * @throws IOException if an error occurs during the resolution
	 * @since 3.2
	 */
	public URL resolve(URL url) throws IOException;
}
