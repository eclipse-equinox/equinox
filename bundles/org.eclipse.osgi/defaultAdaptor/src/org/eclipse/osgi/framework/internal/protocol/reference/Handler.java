/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol.reference;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URLStreamHandler for ReferenceClassLoader resources.
 */

public class Handler extends URLStreamHandler
{
    /**
     * Constructor for a BundleClassLoader resource URLStreamHandler.
     *
     * @param entry BundleEntry this handler represents.
     */
    public Handler()
    {
    }

    /**
     * Establishes a connection to the resource specified by <code>URL</code>.
     * Since different protocols may have unique ways of connecting, it must be
     * overridden by the subclass.
     *
     * @return java.net.URLConnection
     * @param url java.net.URL
     *
     * @exception	IOException 	thrown if an IO error occurs during connection establishment
     */
    protected URLConnection openConnection(URL url) throws IOException
    {
        return new ReferenceURLConnection(url);
    }

	/** 
	 * Parse reference URL. 
	 */
	protected void parseURL(URL url, String str, int start, int end)
	{
		if (end < start) 
		{
			return;
		} 
		String reference = (start < end) ? str.substring(start, end) : url.getPath();

		setURL(url, url.getProtocol(), null, -1, null, null, reference, null, null);
	}
	
}
