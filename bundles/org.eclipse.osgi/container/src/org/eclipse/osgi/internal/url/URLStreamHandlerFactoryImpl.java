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

import java.net.URLStreamHandler;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.equinox.plurl.PlurlStreamHandlerFactory;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * This class contains the URL stream handler factory for the OSGi framework.
 */
public class URLStreamHandlerFactoryImpl implements PlurlStreamHandlerFactory {
	protected static final String URLSTREAMHANDLERCLASS = "org.osgi.service.url.URLStreamHandlerService"; //$NON-NLS-1$
	public static final String PROTOCOL_REFERENCE = "reference"; //$NON-NLS-1$
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private final BundleContext context;
	private final EquinoxContainer container;
	private final Map<String, URLStreamHandlerProxy> proxies;

	/**
	 * Create the factory.
	 *
	 * @param context BundleContext for the system bundle
	 */
	public URLStreamHandlerFactoryImpl(BundleContext context, EquinoxContainer container) {
		this.context = context;
		this.container = container;
		proxies = new ConcurrentHashMap<>();
	}

	/**
	 * Creates a new URLStreamHandler instance for the specified protocol.
	 *
	 * @param protocol The desired protocol
	 * @return a URLStreamHandler for the specific protocol.
	 */
	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		try {
			return createInternalURLStreamHandler(protocol);
		} catch (Throwable t) {
			container.getLogServices().log(URLStreamHandlerFactoryImpl.class.getName(), FrameworkLogEntry.ERROR,
					"Unexpected error in factory.", t); //$NON-NLS-1$
		}
		return null;
	}

	private URLStreamHandler getFrameworkHandler(String protocol) {
		if (BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.bundleentry.Handler(container.getStorage().getModuleContainer(),
					null);
		} else if (BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.bundleresource.Handler(container.getStorage().getModuleContainer(),
					null);
		} else if (PROTOCOL_REFERENCE.equals(protocol)) {
			return new org.eclipse.osgi.storage.url.reference.Handler(
					container.getConfiguration().getConfiguration(EquinoxLocations.PROP_INSTALL_AREA));
		}
		return null;
	}

	private URLStreamHandler createInternalURLStreamHandler(String protocol) {
		// internal protocol handlers
		URLStreamHandler frameworkHandler = getFrameworkHandler(protocol);
		if (frameworkHandler != null) {
			return frameworkHandler;
		}
		// Now we check the service registry
		URLStreamHandlerProxy handler = proxies.computeIfAbsent(protocol, p -> new URLStreamHandlerProxy(p, context));
		if (handler.isActive()) {
			return handler;
		}
		return null;
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
