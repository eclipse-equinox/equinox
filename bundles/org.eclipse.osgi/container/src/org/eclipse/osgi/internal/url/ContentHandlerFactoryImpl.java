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

import java.lang.reflect.Method;
import java.net.ContentHandler;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The ContentHandlerFactory is registered with the JVM to provide content handlers
 * to requestors.  The ContentHandlerFactory will first look for built-in content handlers.
 * If a built in handler exists, this factory will return null.  Otherwise, this ContentHandlerFactory
 * will search the service registry for a maching Content-Handler and, if found, return a 
 * proxy for that content handler.
 */
public class ContentHandlerFactoryImpl extends MultiplexingFactory implements java.net.ContentHandlerFactory {
	private ServiceTracker<ContentHandler, ContentHandler> contentHandlerTracker;

	private static final String contentHandlerClazz = "java.net.ContentHandler"; //$NON-NLS-1$
	private static final String CONTENT_HANDLER_PKGS = "java.content.handler.pkgs"; //$NON-NLS-1$
	private static final String DEFAULT_VM_CONTENT_HANDLERS = "sun.net.www.content|sun.awt.www.content"; //$NON-NLS-1$

	private static final List<Class<?>> ignoredClasses = Arrays.asList(new Class<?>[] {MultiplexingContentHandler.class, ContentHandlerFactoryImpl.class, URLConnection.class});

	private Map<String, ContentHandlerProxy> proxies;
	private java.net.ContentHandlerFactory parentFactory;

	public ContentHandlerFactoryImpl(BundleContext context, EquinoxContainer container) {
		super(context, container);

		proxies = new Hashtable<>(5);

		//We need to track content handler registrations
		contentHandlerTracker = new ServiceTracker<>(context, contentHandlerClazz, null);
		contentHandlerTracker.open();
	}

	/**
	 * @see java.net.ContentHandlerFactory#createContentHandler(String)
	 */
	//TODO method is too long... consider reducing indentation (returning quickly) and moving complex steps to private methods
	@Override
	public ContentHandler createContentHandler(String contentType) {
		//first, we check to see if there exists a built in content handler for
		//this content type.  we can not overwrite built in ContentHandlers
		String builtInHandlers = URLStreamHandlerFactoryImpl.secureAction.getProperty(CONTENT_HANDLER_PKGS);
		builtInHandlers = builtInHandlers == null ? DEFAULT_VM_CONTENT_HANDLERS : DEFAULT_VM_CONTENT_HANDLERS + '|' + builtInHandlers;
		Class<?> clazz = null;
		if (builtInHandlers != null) {
			//replace '/' with a '.' and all characters not allowed in a java class name
			//with a '_'.

			// find all characters not allowed in java names
			String convertedContentType = contentType.replace('.', '_');
			convertedContentType = convertedContentType.replace('/', '.');
			convertedContentType = convertedContentType.replace('-', '_');
			StringTokenizer tok = new StringTokenizer(builtInHandlers, "|"); //$NON-NLS-1$
			while (tok.hasMoreElements()) {
				StringBuffer name = new StringBuffer();
				name.append(tok.nextToken());
				name.append("."); //$NON-NLS-1$
				name.append(convertedContentType);
				try {
					clazz = URLStreamHandlerFactoryImpl.secureAction.loadSystemClass(name.toString());
					if (clazz != null) {
						return (null); //this class exists, it is a built in handler, let the JVM handle it	
					}
				} catch (ClassNotFoundException ex) {
					//keep looking
				}
			}
		}

		if (isMultiplexing())
			return new MultiplexingContentHandler(contentType, this);

		return createInternalContentHandler(contentType);
	}

	public ContentHandler createInternalContentHandler(String contentType) {
		//first check to see if the handler is in the cache
		ContentHandlerProxy proxy = proxies.get(contentType);
		if (proxy != null) {
			return (proxy);
		}
		ServiceReference<ContentHandler>[] serviceReferences = contentHandlerTracker.getServiceReferences();
		if (serviceReferences != null) {
			for (ServiceReference<ContentHandler> serviceReference : serviceReferences) {
				Object prop = serviceReference.getProperty(URLConstants.URL_CONTENT_MIMETYPE);
				if (prop instanceof String)
					prop = new String[] {(String) prop}; // TODO should this be a warning?
				if (!(prop instanceof String[])) {
					String message = NLS.bind(Msg.URL_HANDLER_INCORRECT_TYPE, new Object[]{URLConstants.URL_CONTENT_MIMETYPE, contentHandlerClazz, serviceReference.getBundle()});
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
		// if parent is present do parent lookup before returning a proxy
		if (parentFactory != null) {
			ContentHandler parentHandler = parentFactory.createContentHandler(contentType);
			if (parentHandler != null)
				return parentHandler;
		}
		//If we can't find the content handler in the service registry, return Proxy with DefaultContentHandler set.
		//We need to do this because if we return null, we won't get called again for this content type.
		proxy = new ContentHandlerProxy(contentType, null, context);
		proxies.put(contentType, proxy);
		return (proxy);
	}

	public synchronized ContentHandler findAuthorizedContentHandler(String contentType) {
		Object factory = findAuthorizedFactory(ignoredClasses);
		if (factory == null)
			return null;

		if (factory == this)
			return createInternalContentHandler(contentType);

		try {
			Method createInternalContentHandlerMethod = factory.getClass().getMethod("createInternalContentHandler", new Class[] {String.class}); //$NON-NLS-1$
			return (ContentHandler) createInternalContentHandlerMethod.invoke(factory, new Object[] {contentType});
		} catch (Exception e) {
			container.getLogServices().log(ContentHandlerFactoryImpl.class.getName(), FrameworkLogEntry.ERROR, "findAuthorizedContentHandler-loop", e); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public Object getParentFactory() {
		return parentFactory;
	}

	@Override
	public void setParentFactory(Object parentFactory) {
		if (this.parentFactory == null) // only allow it to be set once
			this.parentFactory = (java.net.ContentHandlerFactory) parentFactory;
	}
}
