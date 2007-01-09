/*******************************************************************************
 * Copyright (c) 2006 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;
import org.osgi.service.cm.ManagedServiceFactory;

public class Activator implements BundleActivator {

	private static final String JETTY_WORK_DIR = "jettywork"; //$NON-NLS-1$
	private static final String DEFAULT_PID = "default"; //$NON-NLS-1$
	private static final String MANAGED_SERVICE_FACTORY_PID = "org.eclipse.equinox.http.jetty.config"; //$NON-NLS-1$

	// OSGi Http Service suggest these properties for setting the default ports
	private static final String ORG_OSGI_SERVICE_HTTP_PORT = "org.osgi.service.http.port"; //$NON-NLS-1$
	private static final String ORG_OSGI_SERVICE_HTTP_PORT_SECURE = "org.osgi.service.http.port.secure"; //$NON-NLS-1$

	// controls whether start() should automatically start an Http Service based on BundleContext properties (default is true)
	private static final String AUTOSTART = "org.eclipse.equinox.http.jetty.autostart"; //$NON-NLS-1$

	private HttpServerManager httpServerManager;
	private ServiceRegistration registration;

	public void start(BundleContext context) throws Exception {
		File jettyWorkDir = new File(context.getDataFile(""), JETTY_WORK_DIR); //$NON-NLS-1$ 
		jettyWorkDir.mkdir();
		httpServerManager = new HttpServerManager(jettyWorkDir);

		String autostart = context.getProperty(AUTOSTART);
		if (autostart == null || Boolean.valueOf(autostart).booleanValue()) {
			Dictionary defaultSettings = createDefaultSettings(context);
			httpServerManager.updated(DEFAULT_PID, defaultSettings);
		}
		
		Dictionary dictionary = new Hashtable();
		dictionary.put(Constants.SERVICE_PID, MANAGED_SERVICE_FACTORY_PID);
		
		registration = context.registerService(ManagedServiceFactory.class.getName(), httpServerManager, dictionary);
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		registration = null;

		httpServerManager.shutdown();
		httpServerManager = null;
	}

	private Dictionary createDefaultSettings(BundleContext context) {
		final String PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty."; //$NON-NLS-1$
		Dictionary defaultSettings = new Hashtable();

		// PID
		defaultSettings.put(Constants.SERVICE_PID, DEFAULT_PID);

		// HTTP Enabled (default is true)
		String httpEnabledProperty = context.getProperty(PROPERTY_PREFIX + HttpServerManager.HTTP_ENABLED);
		Boolean httpEnabled = (httpEnabledProperty == null) ? Boolean.TRUE : new Boolean(httpEnabledProperty);
		defaultSettings.put(HttpServerManager.HTTP_ENABLED, httpEnabled);

		
		// HTTP Port
		String httpPortProperty = context.getProperty(PROPERTY_PREFIX + HttpServerManager.HTTP_PORT);
		if (httpPortProperty == null)
			httpPortProperty = context.getProperty(ORG_OSGI_SERVICE_HTTP_PORT);

		int httpPort = 80;
		if (httpPortProperty != null) {
			try {
				httpPort = Integer.parseInt(httpPortProperty);
			} catch (NumberFormatException e) {
				//(log this) ignore and use default
			}
		}
		defaultSettings.put(HttpServerManager.HTTP_PORT, new Integer(httpPort));

		// HTTPS Enabled (default is false)
		Boolean httpsEnabled = new Boolean(context.getProperty(PROPERTY_PREFIX + HttpServerManager.HTTPS_ENABLED));
		defaultSettings.put(HttpServerManager.HTTPS_ENABLED, httpsEnabled);

		if (httpsEnabled.booleanValue()) {
			// HTTPS Port
			String httpsPortProperty = context.getProperty(PROPERTY_PREFIX + HttpServerManager.HTTPS_PORT);
			if (httpPortProperty == null)
				httpPortProperty = context.getProperty(ORG_OSGI_SERVICE_HTTP_PORT_SECURE);

			int httpsPort = 443;
			if (httpsPortProperty != null) {
				try {
					httpsPort = Integer.parseInt(httpsPortProperty);
				} catch (NumberFormatException e) {
					//(log this) ignore and use default
				}
			}
			defaultSettings.put(HttpServerManager.HTTPS_PORT, new Integer(httpsPort));

			// SSL SETTINGS
			String keystore = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_KEYSTORE);
			if (keystore != null)
				defaultSettings.put(HttpServerManager.SSL_KEYSTORE, keystore);

			String password = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_PASSWORD);
			if (password != null)
				defaultSettings.put(HttpServerManager.SSL_PASSWORD, password);

			String keypassword = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_KEYPASSWORD);
			if (keypassword != null)
				defaultSettings.put(HttpServerManager.SSL_KEYPASSWORD, keypassword);

			String needclientauth = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_NEEDCLIENTAUTH);
			if (needclientauth != null)
				defaultSettings.put(HttpServerManager.SSL_NEEDCLIENTAUTH, new Boolean(needclientauth));

			String wantclientauth = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_WANTCLIENTAUTH);
			if (wantclientauth != null)
				defaultSettings.put(HttpServerManager.SSL_WANTCLIENTAUTH, new Boolean(wantclientauth));

			String protocol = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_PROTOCOL);
			if (protocol != null)
				defaultSettings.put(HttpServerManager.SSL_PROTOCOL, protocol);

			String algorithm = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_ALGORITHM);
			if (algorithm != null)
				defaultSettings.put(HttpServerManager.SSL_ALGORITHM, algorithm);

			String keystoretype = context.getProperty(PROPERTY_PREFIX + HttpServerManager.SSL_KEYSTORETYPE);
			if (keystoretype != null)
				defaultSettings.put(HttpServerManager.SSL_KEYSTORETYPE, keystoretype);
		}

		// Servlet Context Path
		String contextpath = context.getProperty(PROPERTY_PREFIX + HttpServerManager.CONTEXT_PATH);
		if (contextpath != null)
			defaultSettings.put(HttpServerManager.CONTEXT_PATH, contextpath);

		return defaultSettings;
	}
}
