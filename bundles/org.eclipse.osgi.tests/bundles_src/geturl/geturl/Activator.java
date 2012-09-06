/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package geturl;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		final URL url = (URL) System.getProperties().get("test.url");
		final String urlSpec = (String) System.getProperties().get("test.url.spec");
		Dictionary props = new Hashtable();
		props.put("test.url", url);
		context.registerService(PrivilegedAction.class, new PrivilegedAction() {

			public Object run() {
				try {
					throw new RuntimeException("Expected to fail to create: " + new URL(urlSpec));
				} catch (MalformedURLException e) {
					// expected; the parseURL will cause this to fail
				}
				try {
					new URL(url.getProtocol(), url.getHost(), url.getFile());
				} catch (MalformedURLException e) {
					// unexpected; the handler does not get involved and we have a multiplexor cached
					throw new RuntimeException("Could not create URL from parts: " + url);
				}
				url.toExternalForm();
				return Boolean.TRUE;

			}
		}, props);
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
