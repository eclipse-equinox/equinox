/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol;

import java.net.URLStreamHandler;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLConstants;

/**
 * This class contains the URL stream handler factory for the OSGi framework.
 */
public class StreamHandlerFactory implements java.net.URLStreamHandlerFactory {
	/** BundleContext to system bundle */
	protected BundleContext context;

	/** internal adaptor object */
	protected FrameworkAdaptor adaptor;

	private ServiceTracker urlStreamHandlerTracker;

	protected static final String urlStreamHandlerClazz = "org.osgi.service.url.URLStreamHandlerService";

	private Hashtable proxies;

	/**
	 * Create the factory.
	 *
	 * @param context BundleContext for the system bundle
	 */
	public StreamHandlerFactory(BundleContext context, FrameworkAdaptor adaptor) {
		this.context = context;
		this.adaptor = adaptor;

		proxies = new Hashtable(15);
		urlStreamHandlerTracker = new ServiceTracker(context, urlStreamHandlerClazz, null);
		urlStreamHandlerTracker.open();
	}

	/**
	 * Creates a new URLStreamHandler instance for the specified
	 * protocol.
	 *
	 * @param protocol The desired protocol
	 * @return a URLStreamHandler for the specific protocol.
	 */
	public URLStreamHandler createURLStreamHandler(String protocol) {

		//first check for built in handlers
		String builtInHandlers = System.getProperty("java.protocol.handler.pkgs");
		Class clazz = null;
		if (builtInHandlers != null) {
			StringTokenizer tok = new StringTokenizer(builtInHandlers, "|");
			while (tok.hasMoreElements()) {
				StringBuffer name = new StringBuffer();
				name.append(tok.nextToken());
				name.append(".");
				name.append(protocol);
				name.append(".Handler");
				try {
					clazz = Class.forName(name.toString());
					if (clazz != null) {
						return (null); //this class exists, it is a built in handler, let the JVM handle it	
					}
				} catch (ClassNotFoundException ex) {
				} //keep looking 
			}
		}

		//internal protocol handlers
		String name = "org.eclipse.osgi.framework.internal.protocol." + protocol + ".Handler";

		try {
			clazz = Class.forName(name);
		}

		//Now we checdk the service registry
		catch (ClassNotFoundException e) {

			//first check to see if the handler is in the cache
			URLStreamHandlerProxy handler = (URLStreamHandlerProxy) proxies.get(protocol);
			if (handler != null) {
				return (handler);
			}

			//look through the service registry for a URLStramHandler registered for this protocol
			org.osgi.framework.ServiceReference[] serviceReferences = urlStreamHandlerTracker.getServiceReferences();
			if (serviceReferences != null) {
				for (int i = 0; i < serviceReferences.length; i++) {
					String[] protocols = (String[]) (serviceReferences[i].getProperty(URLConstants.URL_HANDLER_PROTOCOL));
					if (protocols != null) {
						for (int j = 0; j < protocols.length; j++) {
							if (protocols[j].equals(protocol)) {
								handler = new URLStreamHandlerProxy(protocol, serviceReferences[i], context);
								proxies.put(protocol, handler);
								return (handler);
							}
						}
					}
				}
			}
			return (null);
		}

		if (clazz == null) {
			return null;
		}

		try {
			URLStreamHandler handler = (URLStreamHandler) clazz.newInstance();

			if (handler instanceof ProtocolActivator) {
				((ProtocolActivator) handler).start(context, adaptor);
			}

			return handler;
		} catch (Exception e) {
			return null;
		}
	}
}
