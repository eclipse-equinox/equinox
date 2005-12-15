/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import org.osgi.framework.Bundle;

/**
 * Utility class to execute common privileged code.
 */
public class SecureAction {
	// make sure we use the correct controlContext;
	private AccessControlContext controlContext;

	/**
	 * Constructs a new SecureAction object.  The constructed SecureAction object 
	 * uses the caller's AccessControlContext to perform security checks 
	 */
	public SecureAction() {
		// save the control context to be used.
		this.controlContext = AccessController.getContext();
	}

	/**
	 * Gets a resource from a bundle.
	 * @param bundle the bundle to get the resource from
	 * @param name The name of the resource
	 * @return The URL of the resource
	 */
	
	public URL getBundleResource(final Bundle bundle, final String name) {
		if (System.getSecurityManager() == null)
		     return bundle.getResource(name);
		return (URL) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return bundle.getResource(name);
			}
		}, controlContext);
	}
	
	public URLConnection openURL(final URL url) throws IOException {
		if (System.getSecurityManager() == null)
		    return open(url);
		try {	
		return (URLConnection) AccessController.doPrivileged(new PrivilegedExceptionAction() {
			public Object run() throws IOException{
				return open(url);
			}
		}, controlContext);  
		} catch(PrivilegedActionException ex) {
			throw (IOException) ex.getException();
		}
	}  
	
	private URLConnection open(final URL url)throws IOException
	{
		URLConnection connection = url.openConnection();
        connection.connect(); /* establish connection to check permissions */
        return connection;
	}
	
	/**
	 * Returns a system property.  Same as calling
	 * System.getProperty(String,String).
	 * @param property the property key.
	 * @param def the default value if the property key does not exist.
	 * @return the value of the property or the def value if the property
	 * does not exist.
	 */
	public String getProperty(final String property, final String def) {
		if (System.getSecurityManager() == null)
			return System.getProperty(property, def);
		return (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return System.getProperty(property, def);
			}
		}, controlContext);
	}

}
