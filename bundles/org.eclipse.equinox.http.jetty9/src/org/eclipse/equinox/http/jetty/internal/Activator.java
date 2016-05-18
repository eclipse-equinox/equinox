/*******************************************************************************
 * Copyright (c) 2005, 2016 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.osgi.framework.*;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class Activator implements BundleActivator {

	private static final String JETTY_WORK_DIR = "jettywork"; //$NON-NLS-1$
	private static final String DEFAULT_PID = "default"; //$NON-NLS-1$
	private static final String MANAGED_SERVICE_FACTORY_PID = "org.eclipse.equinox.http.jetty.config"; //$NON-NLS-1$

	// OSGi Http Service suggest these properties for setting the default ports
	private static final String ORG_OSGI_SERVICE_HTTP_PORT = "org.osgi.service.http.port"; //$NON-NLS-1$
	private static final String ORG_OSGI_SERVICE_HTTP_PORT_SECURE = "org.osgi.service.http.port.secure"; //$NON-NLS-1$

	// controls whether start() should automatically start an Http Service based on BundleContext properties (default is true)
	// Note: only used if the bundle is explicitly started (e.g. not "lazy" activated)
	private static final String AUTOSTART = "org.eclipse.equinox.http.jetty.autostart"; //$NON-NLS-1$

	// Jetty will use a basic stderr logger if no other logging mechanism is provided.
	// This setting can be used to over-ride the stderr logger threshold(and only this default logger)
	// Valid values are in increasing threshold: "debug", "info", "warn", "error", and "off"
	// (default threshold is "warn")
	private static final String LOG_STDERR_THRESHOLD = "org.eclipse.equinox.http.jetty.log.stderr.threshold"; //$NON-NLS-1$

	// The staticServerManager is use by the start and stopServer methods and must be accessed in a static synchronized block
	// to ensure it is correctly handled in terms of the bundle life-cycle.
	private static HttpServerManager staticServerManager;

	private HttpServerManager httpServerManager;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration registration;

	public void start(BundleContext context) throws Exception {
		File jettyWorkDir = new File(context.getDataFile(""), JETTY_WORK_DIR); //$NON-NLS-1$
		jettyWorkDir.mkdir();
		EquinoxStdErrLog.setThresholdLogger(context.getProperty(LOG_STDERR_THRESHOLD));
		httpServerManager = new HttpServerManager(jettyWorkDir);

		boolean autostart = Details.getBoolean(context, AUTOSTART, true);
		if (autostart && !isBundleActivationPolicyUsed(context)) {
			Dictionary<String, Object> defaultSettings = createDefaultSettings(context);
			httpServerManager.updated(DEFAULT_PID, defaultSettings);
		}

		Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
		dictionary.put(Constants.SERVICE_PID, MANAGED_SERVICE_FACTORY_PID);

		registration = context.registerService(ManagedServiceFactory.class.getName(), httpServerManager, dictionary);
		setStaticServerManager(httpServerManager);
	}

	@SuppressWarnings("unchecked")
	private boolean isBundleActivationPolicyUsed(BundleContext context) {
		@SuppressWarnings("rawtypes")
		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
		StartLevel sl = ((reference != null) ? (StartLevel) context.getService(reference) : null);
		if (sl != null) {
			try {
				Bundle bundle = context.getBundle();
				Method isBundleActivationPolicyUsed = StartLevel.class.getMethod("isBundleActivationPolicyUsed", new Class[] {Bundle.class}); //$NON-NLS-1$
				Boolean result = (Boolean) isBundleActivationPolicyUsed.invoke(sl, new Object[] {bundle});
				return result.booleanValue();
			} catch (Exception e) {
				// ignore
				// Bundle Activation Policy only available in StartLevel Service 1.1
			} finally {
				context.ungetService(reference);
			}
		}
		return false;
	}

	public void stop(BundleContext context) throws Exception {
		setStaticServerManager(null);
		registration.unregister();
		registration = null;

		httpServerManager.shutdown();
		httpServerManager = null;
	}

	private Dictionary<String, Object> createDefaultSettings(BundleContext context) {
		Dictionary<String, Object> defaultSettings = new Hashtable<String, Object>();

		// PID
		defaultSettings.put(Constants.SERVICE_PID, DEFAULT_PID);

		// HTTP Enabled (default is true)
		Boolean httpEnabled = Details.getBooleanProp(context, JettyConstants.HTTP_ENABLED, true);
		defaultSettings.put(JettyConstants.HTTP_ENABLED, httpEnabled);

		// HTTP Port
		int httpPort = Details.getIntProp(context, JettyConstants.HTTP_PORT, -1);
		if (httpPort == -1)
			httpPort = Details.getInt(context, ORG_OSGI_SERVICE_HTTP_PORT, 0);
		defaultSettings.put(JettyConstants.HTTP_PORT, Integer.valueOf(httpPort));

		// HTTP Host (default is 0.0.0.0)
		String httpHost = Details.getStringProp(context, JettyConstants.HTTP_HOST, null);
		if (httpHost != null)
			defaultSettings.put(JettyConstants.HTTP_HOST, httpHost);

		// HTTPS Enabled (default is false)
		Boolean httpsEnabled = Details.getBooleanProp(context, JettyConstants.HTTPS_ENABLED, false);
		defaultSettings.put(JettyConstants.HTTPS_ENABLED, httpsEnabled);

		// minimum number of threads
		int minThreads = Details.getIntProp(context, JettyConstants.HTTP_MINTHREADS, 8);
		if (minThreads != -1) {
			defaultSettings.put(JettyConstants.HTTP_MINTHREADS, Integer.valueOf(minThreads));
		}

		// maximum number of threads
		int maxThreads = Details.getIntProp(context, JettyConstants.HTTP_MAXTHREADS, 200);
		if (maxThreads != -1) {
			defaultSettings.put(JettyConstants.HTTP_MAXTHREADS, Integer.valueOf(maxThreads));
		}

		if (httpsEnabled.booleanValue()) {
			// HTTPS Port

			int httpsPort = Details.getIntProp(context, JettyConstants.HTTP_PORT, -1);
			if (httpsPort == -1)
				httpsPort = Details.getInt(context, ORG_OSGI_SERVICE_HTTP_PORT_SECURE, 443);
			defaultSettings.put(JettyConstants.HTTPS_PORT, Integer.valueOf(httpsPort));

			// HTTPS Host (default is 0.0.0.0)
			String httpsHost = Details.getStringProp(context, JettyConstants.HTTPS_HOST, null);
			if (httpsHost != null)
				defaultSettings.put(JettyConstants.HTTPS_HOST, httpsHost);

			// SSL SETTINGS
			String keystore = Details.getStringProp(context, JettyConstants.SSL_KEYSTORE, null);
			if (keystore != null)
				defaultSettings.put(JettyConstants.SSL_KEYSTORE, keystore);

			String password = Details.getStringProp(context, JettyConstants.SSL_PASSWORD, null);
			if (password != null)
				defaultSettings.put(JettyConstants.SSL_PASSWORD, password);

			String keypassword = Details.getStringProp(context, JettyConstants.SSL_KEYPASSWORD, null);
			if (keypassword != null)
				defaultSettings.put(JettyConstants.SSL_KEYPASSWORD, keypassword);

			String needclientauth = Details.getStringProp(context, JettyConstants.SSL_NEEDCLIENTAUTH, null);
			if (needclientauth != null)
				defaultSettings.put(JettyConstants.SSL_NEEDCLIENTAUTH, Boolean.valueOf(needclientauth));

			String wantclientauth = Details.getStringProp(context, JettyConstants.SSL_WANTCLIENTAUTH, null);
			if (wantclientauth != null)
				defaultSettings.put(JettyConstants.SSL_WANTCLIENTAUTH, Boolean.valueOf(wantclientauth));

			String protocol = Details.getStringProp(context, JettyConstants.SSL_PROTOCOL, null);
			if (protocol != null)
				defaultSettings.put(JettyConstants.SSL_PROTOCOL, protocol);

			String algorithm = Details.getStringProp(context, JettyConstants.SSL_ALGORITHM, null);
			if (algorithm != null)
				defaultSettings.put(JettyConstants.SSL_ALGORITHM, algorithm);

			String keystoretype = Details.getStringProp(context, JettyConstants.SSL_KEYSTORETYPE, null);
			if (keystoretype != null)
				defaultSettings.put(JettyConstants.SSL_KEYSTORETYPE, keystoretype);
		}

		// Servlet Context Path
		String contextpath = Details.getStringProp(context, JettyConstants.CONTEXT_PATH, null);
		if (contextpath != null)
			defaultSettings.put(JettyConstants.CONTEXT_PATH, contextpath);

		// Session Inactive Interval (timeout)
		String sessionInactiveInterval = Details.getStringProp(context, JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, null);
		if (sessionInactiveInterval != null) {
			try {
				defaultSettings.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, Integer.valueOf(sessionInactiveInterval));
			} catch (NumberFormatException e) {
				//(log this) ignore
			}
		}

		// Other Info
		String otherInfo = Details.getStringProp(context, JettyConstants.OTHER_INFO, null);
		if (otherInfo != null)
			defaultSettings.put(JettyConstants.OTHER_INFO, otherInfo);

		// customizer
		String customizerClass = Details.getStringProp(context, JettyConstants.CUSTOMIZER_CLASS, null);
		if (customizerClass != null)
			defaultSettings.put(JettyConstants.CUSTOMIZER_CLASS, customizerClass);

		return defaultSettings;
	}

	public synchronized static void startServer(String pid, Dictionary<String, ?> settings) throws Exception {
		if (staticServerManager == null)
			throw new IllegalStateException("Inactive"); //$NON-NLS-1$

		staticServerManager.updated(pid, settings);
	}

	public synchronized static void stopServer(String pid) throws Exception {
		if (staticServerManager != null)
			staticServerManager.deleted(pid);
	}

	private synchronized static void setStaticServerManager(HttpServerManager httpServerManager) {
		staticServerManager = httpServerManager;
	}
}