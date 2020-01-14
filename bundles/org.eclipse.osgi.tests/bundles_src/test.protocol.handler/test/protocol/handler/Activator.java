/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package test.protocol.handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.*;

public class Activator extends AbstractURLStreamHandlerService implements BundleActivator {
	BundleContext bc;

	@Override
	public void start(BundleContext context) throws Exception {
		bc = context;
		Dictionary<String, String> props = new Hashtable<>();
		props.put(URLConstants.URL_HANDLER_PROTOCOL, "testing1");
		context.registerService(URLStreamHandlerService.class, this, props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing
	}

	@Override
	public URLConnection openConnection(URL u) {
		return new URLConnection(u) {

			@Override
			public void connect() throws IOException {
				try {
					bc.getBundle();
				} catch (IllegalStateException e) {
					throw new IOException(e);
				}
				return;
			}
		};
	}
}
