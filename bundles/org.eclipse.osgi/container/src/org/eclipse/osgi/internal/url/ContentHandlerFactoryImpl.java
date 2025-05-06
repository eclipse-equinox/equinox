/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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

import java.net.ContentHandler;
import java.util.Hashtable;
import java.util.Map;
import org.eclipse.equinox.plurl.PlurlContentHandlerFactory;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The ContentHandlerFactory is registered with the JVM to provide content
 * handlers to requestors. The ContentHandlerFactory will first look for
 * built-in content handlers. If a built in handler exists, this factory will
 * return null. Otherwise, this ContentHandlerFactory will search the service
 * registry for a maching Content-Handler and, if found, return a proxy for that
 * content handler.
 */
public class ContentHandlerFactoryImpl implements PlurlContentHandlerFactory {
	private static final String contentHandlerClazz = "java.net.ContentHandler"; //$NON-NLS-1$

	private final EquinoxContainer container;
	private final BundleContext context;
	private final ServiceTracker<ContentHandler, ContentHandler> contentHandlerTracker;
	private final Map<String, ContentHandlerProxy> proxies;

	public ContentHandlerFactoryImpl(BundleContext context, EquinoxContainer container) {
		this.container = container;
		this.context = context;
		proxies = new Hashtable<>(5);

		// We need to track content handler registrations
		contentHandlerTracker = new ServiceTracker<>(context, contentHandlerClazz, null);
		contentHandlerTracker.open();
	}

	/**
	 * @see java.net.ContentHandlerFactory#createContentHandler(String)
	 */
	@Override
	public ContentHandler createContentHandler(String contentType) {
		// first check to see if the handler is in the cache
		ContentHandlerProxy proxy = proxies.get(contentType);
		if (proxy != null) {
			return (proxy);
		}
		ServiceReference<ContentHandler>[] serviceReferences = contentHandlerTracker.getServiceReferences();
		if (serviceReferences != null) {
			for (ServiceReference<ContentHandler> serviceReference : serviceReferences) {
				Object prop = serviceReference.getProperty(URLConstants.URL_CONTENT_MIMETYPE);
				if (prop instanceof String) {
					prop = new String[] { (String) prop };
				}
				if (!(prop instanceof String[])) {
					String message = NLS.bind(Msg.URL_HANDLER_INCORRECT_TYPE, URLConstants.URL_CONTENT_MIMETYPE,
							contentHandlerClazz, serviceReference.getBundle());
					container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, message, null);
					continue;
				}
				String[] contentHandler = (String[]) prop;
				for (String typename : contentHandler) {
					if (typename.equals(contentType)) {
						proxy = new ContentHandlerProxy(contentType, serviceReference, context);
						proxies.put(contentType, proxy);
						return (proxy);
					}
				}
			}
		}

		// If we can't find the content handler in the service registry, return Proxy
		// with DefaultContentHandler set.
		// We need to do this because if we return null, we won't get called again for
		// this content type.
		proxy = new ContentHandlerProxy(contentType, null, context);
		proxies.put(contentType, proxy);
		return (proxy);
	}

	@Override
	public boolean shouldHandle(Class<?> clazz) {
		Bundle b = FrameworkUtil.getBundle(clazz);
		if (!(b instanceof EquinoxBundle)) {
			return false;
		}
		return (container.getStorage().getModuleContainer() == ((EquinoxBundle) b).getModule().getContainer());
	}

}
