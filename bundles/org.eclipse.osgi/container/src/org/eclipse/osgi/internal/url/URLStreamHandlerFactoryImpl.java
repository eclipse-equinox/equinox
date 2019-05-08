/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
import java.net.*;
import java.security.AccessController;
import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class contains the URL stream handler factory for the OSGi framework.
 */
public class URLStreamHandlerFactoryImpl extends MultiplexingFactory implements URLStreamHandlerFactory {
	protected static final String URLSTREAMHANDLERCLASS = "org.osgi.service.url.URLStreamHandlerService"; //$NON-NLS-1$
	protected static final String PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs"; //$NON-NLS-1$
	public static final String PROTOCOL_REFERENCE = "reference"; //$NON-NLS-1$
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private ServiceTracker<URLStreamHandlerService, URLStreamHandlerService> handlerTracker;

	private static final List<Class<?>> ignoredClasses = Arrays.asList(new Class<?>[] {MultiplexingURLStreamHandler.class, URLStreamHandlerFactoryImpl.class, URL.class});
	private Map<String, URLStreamHandler> proxies;
	private URLStreamHandlerFactory parentFactory;
	private ThreadLocal<List<String>> creatingProtocols = new ThreadLocal<>();

	/**
	 * Create the factory.
	 *
	 * @param context BundleContext for the system bundle
	 */
	public URLStreamHandlerFactoryImpl(BundleContext context, EquinoxContainer container) {
		super(context, container);

		proxies = new Hashtable<>(15);
		handlerTracker = new ServiceTracker<>(context, URLSTREAMHANDLERCLASS, null);
		handlerTracker.open();
	}

	private Class<?> getBuiltIn(String protocol, String builtInHandlers) {
		if (builtInHandlers == null)
			return null;
		Class<?> clazz;
		StringTokenizer tok = new StringTokenizer(builtInHandlers, "|"); //$NON-NLS-1$
		while (tok.hasMoreElements()) {
			StringBuffer name = new StringBuffer();
			name.append(tok.nextToken());
			name.append("."); //$NON-NLS-1$
			name.append(protocol);
			name.append(".Handler"); //$NON-NLS-1$
			try {
				clazz = secureAction.loadSystemClass(name.toString());
				if (clazz != null)
					return clazz; //this class exists, it is a built in handler	
			} catch (ClassNotFoundException ex) {
				// keep looking
			}
		}
		return null;
	}

	/**
	 * Creates a new URLStreamHandler instance for the specified
	 * protocol.
	 *
	 * @param protocol The desired protocol
	 * @return a URLStreamHandler for the specific protocol.
	 */
	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		// Check if we are recursing
		if (isRecursive(protocol))
			return null;
		try {
			//first check for built in handlers
			String builtInHandlers = secureAction.getProperty(PROTOCOL_HANDLER_PKGS);
			Class<?> clazz = getBuiltIn(protocol, builtInHandlers);
			if (clazz != null)
				return null; // let the VM handle it
			URLStreamHandler result = null;
			if (isMultiplexing()) {
				URLStreamHandler authorized = findAuthorizedURLStreamHandler(protocol);
				if (authorized != null)
					result = new MultiplexingURLStreamHandler(protocol, this, authorized);
			} else {
				result = createInternalURLStreamHandler(protocol);
			}
			// if parent is present do parent lookup
			if (result == null && parentFactory != null)
				result = parentFactory.createURLStreamHandler(protocol);
			return result; //result may be null; let the VM handle it (consider sun.net.protocol.www.*)
		} catch (Throwable t) {
			container.getLogServices().log(URLStreamHandlerFactoryImpl.class.getName(), FrameworkLogEntry.ERROR, "Unexpected error in factory.", t); //$NON-NLS-1$
			return null;
		} finally {
			releaseRecursive(protocol);
		}
	}

	private boolean isRecursive(String protocol) {
		List<String> protocols = creatingProtocols.get();
		if (protocols == null) {
			protocols = new ArrayList<>(1);
			creatingProtocols.set(protocols);
		}
		if (protocols.contains(protocol))
			return true;
		protocols.add(protocol);
		return false;
	}

	private void releaseRecursive(String protocol) {
		List<String> protocols = creatingProtocols.get();
		protocols.remove(protocol);
	}

	private URLStreamHandler getFrameworkHandler(String protocol) {
		if (BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.bundleentry.Handler(container.getStorage().getModuleContainer(), null);
		} else if (BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.bundleresource.Handler(container.getStorage().getModuleContainer(), null);
		} else if (PROTOCOL_REFERENCE.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.reference.Handler(container.getConfiguration().getConfiguration(EquinoxLocations.PROP_INSTALL_AREA));
		}
		return null;
	}

	public URLStreamHandler createInternalURLStreamHandler(String protocol) {
		//internal protocol handlers
		URLStreamHandler frameworkHandler = getFrameworkHandler(protocol);
		if (frameworkHandler != null) {
			return frameworkHandler;
		}

		//Now we check the service registry
		//first check to see if the handler is in the cache
		URLStreamHandlerProxy handler = (URLStreamHandlerProxy) proxies.get(protocol);
		if (handler != null)
			return (handler);
		//look through the service registry for a URLStramHandler registered for this protocol
		ServiceReference<URLStreamHandlerService>[] serviceReferences = handlerTracker.getServiceReferences();
		if (serviceReferences == null)
			return null;
		for (int i = 0; i < serviceReferences.length; i++) {
			Object prop = serviceReferences[i].getProperty(URLConstants.URL_HANDLER_PROTOCOL);
			if (prop instanceof String)
				prop = new String[] {(String) prop}; // TODO should this be a warning?
			if (!(prop instanceof String[])) {
				String message = NLS.bind(Msg.URL_HANDLER_INCORRECT_TYPE, new Object[] {URLConstants.URL_HANDLER_PROTOCOL, URLSTREAMHANDLERCLASS, serviceReferences[i].getBundle()});
				container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, message, null);
				continue;
			}
			String[] protocols = (String[]) prop;
			for (int j = 0; j < protocols.length; j++) {
				if (protocols[j].equals(protocol)) {
					handler = new URLStreamHandlerProxy(protocol, serviceReferences[i], context);
					proxies.put(protocol, handler);
					return (handler);
				}
			}
		}
		return null;
	}

	protected URLStreamHandler findAuthorizedURLStreamHandler(String protocol) {
		Object factory = findAuthorizedFactory(ignoredClasses);
		if (factory == null)
			return null;

		if (factory == this)
			return createInternalURLStreamHandler(protocol);

		try {
			Method createInternalURLStreamHandlerMethod = factory.getClass().getMethod("createInternalURLStreamHandler", new Class[] {String.class}); //$NON-NLS-1$
			return (URLStreamHandler) createInternalURLStreamHandlerMethod.invoke(factory, new Object[] {protocol});
		} catch (Exception e) {
			container.getLogServices().log(URLStreamHandlerFactoryImpl.class.getName(), FrameworkLogEntry.ERROR, "findAuthorizedURLStreamHandler-loop", e); //$NON-NLS-1$
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
			this.parentFactory = (URLStreamHandlerFactory) parentFactory;
	}
}
