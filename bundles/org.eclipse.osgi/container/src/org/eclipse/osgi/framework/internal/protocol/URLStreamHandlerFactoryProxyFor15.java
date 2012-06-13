/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLStreamHandlerService;

public class URLStreamHandlerFactoryProxyFor15 extends URLStreamHandlerProxy {

	public URLStreamHandlerFactoryProxyFor15(String protocol, ServiceReference<URLStreamHandlerService> reference, BundleContext context) {
		super(protocol, reference, context);
	}

	protected URLConnection openConnection(URL u, Proxy p) throws IOException {
		try {
			Method openConn = realHandlerService.getClass().getMethod("openConnection", new Class[] {URL.class, Proxy.class}); //$NON-NLS-1$
			return (URLConnection) openConn.invoke(realHandlerService, new Object[] {u, p});
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof IOException)
				throw (IOException) e.getTargetException();
			throw (RuntimeException) e.getTargetException();
		} catch (Exception e) {
			// expected on JRE < 1.5
			throw (UnsupportedOperationException) new UnsupportedOperationException().initCause(e);
		}
	}
}
