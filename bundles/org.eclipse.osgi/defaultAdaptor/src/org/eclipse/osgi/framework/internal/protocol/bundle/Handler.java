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

package org.eclipse.osgi.framework.internal.protocol.bundle;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.osgi.framework.internal.defaultadaptor.BundleEntry;

/**
 * URLStreamHandler for BundleClassLoader resources.
 */

public class Handler extends URLStreamHandler
{
    /**
     * BundleEntry this handler is associated with.
     */
    protected final BundleEntry entry;

    /**
     * Constructor for a BundleClassLoader resource URLStreamHandler.
     *
     * @param entry BundleEntry this handler represents.
     */
    public Handler(BundleEntry entry)
    {
        this.entry = entry;
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
        return(new BundleURLConnection(url, entry));
    }

    /**
     * Converts a bundle URL to a String.
     *
     * @param   url   the URL.
     * @return  a string representation of the URL.
     */
    protected String toExternalForm(URL url)
    {
        StringBuffer result = new StringBuffer(url.getProtocol());
        result.append(":");

        String host = url.getHost();
        if ((host != null) && (host.length() > 0))
        {
            result.append("//");
            result.append(host);
        }

        int port = url.getPort();

        if (port != -1)
        {
            result.append(":");
            result.append(port);
        }

        String file = url.getFile();
        if (file != null)
        {
            if ((file.length() > 0) && (file.charAt(0) != '/'))  /* if name doesn't have a leading slash */
            {
                result.append("/");
            }

            result.append(file);
        }

        String ref = url.getRef();
        if (ref != null)
        {
            result.append("#");
            result.append(ref);
        }

        return (result.toString());
    }
}
