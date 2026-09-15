/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Supplier;
import org.eclipse.equinox.plurl.PlurlStreamHandlerBase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The URLStreamHandlerProxy is a URLStreamHandler that acts as a proxy for
 * registered URLStreamHandlerServices. When a URLStreamHandler is requested
 * from the URLStreamHandlerFactory and it exists in the service registry, a
 * URLStreamHandlerProxy is created which will pass all the requests from the
 * requestor to the real URLStreamHandlerService. We can't return the real
 * URLStreamHandlerService from the URLStreamHandlerFactory because the JVM
 * caches URLStreamHandlers and therefore would not support a dynamic
 * environment of URLStreamHandlerServices being registered and unregistered.
 */

public class URLStreamHandlerProxy extends PlurlStreamHandlerBase {

	private static final URLStreamHandlerService NO_HANDLER = new NullURLStreamHandlerService();

	protected URLStreamHandlerService realHandlerService;

	protected final URLStreamHandlerSetter urlSetter;

	protected final ServiceTracker<URLStreamHandlerService, LazyURLStreamHandlerService> urlStreamHandlerServiceTracker;

	public URLStreamHandlerProxy(String protocol, BundleContext context) {

		urlSetter = new URLStreamHandlerSetter(this);

		Filter filter;
		try {
			filter = context.createFilter(String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, //$NON-NLS-1$
					URLStreamHandlerFactoryImpl.URLSTREAMHANDLERCLASS, URLConstants.URL_HANDLER_PROTOCOL, protocol));
		} catch (InvalidSyntaxException e) {
			throw new AssertionError("should never happen!", e); //$NON-NLS-1$
		}

		urlStreamHandlerServiceTracker = new ServiceTracker<>(context, filter,
				new ServiceTrackerCustomizer<URLStreamHandlerService, LazyURLStreamHandlerService>() {

					@Override
					public LazyURLStreamHandlerService addingService(
							ServiceReference<URLStreamHandlerService> reference) {
						return new LazyURLStreamHandlerService(context, reference);
					}

					@Override
					public void modifiedService(ServiceReference<URLStreamHandlerService> reference,
							LazyURLStreamHandlerService service) {
						// nothing to do here...
					}

					@Override
					public void removedService(ServiceReference<URLStreamHandlerService> reference,
							LazyURLStreamHandlerService service) {
						service.dispose();
					}
				});
		URLStreamHandlerFactoryImpl.secureAction.open(urlStreamHandlerServiceTracker);
	}

	/**
	 * @see java.net.URLStreamHandler#equals(URL, URL)
	 */
	@Override
	public boolean equals(URL url1, URL url2) {
		return getRealHandlerService().equals(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#getDefaultPort()
	 */
	@Override
	public int getDefaultPort() {
		return getRealHandlerService().getDefaultPort();
	}

	/**
	 * @see java.net.URLStreamHandler#getHostAddress(URL)
	 */
	@Override
	public InetAddress getHostAddress(URL url) {
		return getRealHandlerService().getHostAddress(url);
	}

	/**
	 * @see java.net.URLStreamHandler#hashCode(URL)
	 */
	@Override
	public int hashCode(URL url) {
		return getRealHandlerService().hashCode(url);
	}

	/**
	 * @see java.net.URLStreamHandler#hostsEqual(URL, URL)
	 */
	@Override
	public boolean hostsEqual(URL url1, URL url2) {
		return getRealHandlerService().hostsEqual(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#openConnection(URL)
	 */
	@Override
	public URLConnection openConnection(URL url) throws IOException {
		return getRealHandlerService().openConnection(url);
	}

	/**
	 * @see java.net.URLStreamHandler#parseURL(URL, String, int, int)
	 */
	@Override
	protected void parseURL(URL url, String str, int start, int end) {
		getRealHandlerService().parseURL(urlSetter, url, str, start, end);
	}

	/**
	 * @see java.net.URLStreamHandler#sameFile(URL, URL)
	 */
	@Override
	public boolean sameFile(URL url1, URL url2) {
		return getRealHandlerService().sameFile(url1, url2);
	}

	/**
	 * @see java.net.URLStreamHandler#toExternalForm(URL)
	 */
	@Override
	public String toExternalForm(URL url) {
		return getRealHandlerService().toExternalForm(url);
	}

	@Override
	public URLConnection openConnection(URL u, Proxy p) throws IOException {
		try {
			URLStreamHandlerService service = getRealHandlerService();
			Method openConn = service.getClass().getMethod("openConnection", //$NON-NLS-1$
								URL.class, Proxy.class );
			openConn.setAccessible(true);
			return (URLConnection) openConn.invoke(service, u, p);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof IOException) {
				throw (IOException) e.getTargetException();
			}
			throw (RuntimeException) e.getTargetException();
		} catch (Exception e) {
			// expected on JRE < 1.5
			throw new UnsupportedOperationException(e);
		}
	}

	void setURLInternal(URL u, String proto, String host, int port, String auth, String user, String path, String query,
			String ref) {
		setURL(u, proto, host, port, auth, user, path, query, ref);
	}

	void setURLInternal(URL u, String proto, String host, int port, String file, String ref) {
		setURL(u, proto, host, port, file, ref);
	}

	public boolean isActive() {
		return urlStreamHandlerServiceTracker.getService() != null;
	}

	public URLStreamHandlerService getRealHandlerService() {
		LazyURLStreamHandlerService service = urlStreamHandlerServiceTracker.getService();
		if (service != null) {
			return service.get();
		}
		return NO_HANDLER;
	}

	private static final class LazyURLStreamHandlerService implements Supplier<URLStreamHandlerService> {

		private final BundleContext bundleContext;
		private final ServiceReference<URLStreamHandlerService> reference;
		private URLStreamHandlerService service;
		private boolean disposed;

		LazyURLStreamHandlerService(BundleContext bundleContext, ServiceReference<URLStreamHandlerService> reference) {
			this.bundleContext = bundleContext;
			this.reference = reference;
		}

		synchronized void dispose() {
			disposed = true;
			if (service != null) {
				service = null;
				bundleContext.ungetService(reference);
			}
		}

		@Override
		public synchronized URLStreamHandlerService get() {
			if (service == null && !disposed) {
				service = URLStreamHandlerFactoryImpl.secureAction.getService(reference, bundleContext);
			}
			return service;
		}

	}
}
