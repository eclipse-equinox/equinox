/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class HttpServerManager implements ManagedServiceFactory {

	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir"; //$NON-NLS-1$
	private static final String DIR_PREFIX = "pid_"; //$NON-NLS-1$
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader"; //$NON-NLS-1$

	private Map<String, Server> servers = new HashMap<String, Server>();
	private File workDir;

	public HttpServerManager(File workDir) {
		this.workDir = workDir;
	}

	public synchronized void deleted(String pid) {
		Server server = servers.remove(pid);
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				// TODO: consider logging this, but we should still continue cleaning up 
				e.printStackTrace();
			}
			File contextWorkDir = new File(workDir, DIR_PREFIX + pid.hashCode());
			deleteDirectory(contextWorkDir);
		}
	}

	public String getName() {
		return this.getClass().getName();
	}

	@SuppressWarnings("unchecked")
	public synchronized void updated(String pid, @SuppressWarnings("rawtypes") Dictionary dictionary) throws ConfigurationException {
		deleted(pid);
		Server server = new Server();

		JettyCustomizer customizer = createJettyCustomizer(dictionary);

		Connector httpConnector = createHttpConnector(dictionary);
		if (null != customizer)
			httpConnector = (Connector) customizer.customizeHttpConnector(httpConnector, dictionary);

		if (httpConnector != null)
			server.addConnector(httpConnector);

		Connector httpsConnector = createHttpsConnector(dictionary);
		if (null != customizer)
			httpsConnector = (Connector) customizer.customizeHttpsConnector(httpsConnector, dictionary);
		if (httpsConnector != null)
			server.addConnector(httpsConnector);

		ServletHolder holder = new ServletHolder(new InternalHttpServiceServlet());
		holder.setInitOrder(0);
		holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
		holder.setInitParameter(Constants.SERVICE_DESCRIPTION, "Equinox Jetty-based Http Service"); //$NON-NLS-1$
		if (httpConnector != null) {
			int port = httpConnector.getLocalPort();
			if (port == -1)
				port = httpConnector.getPort();
			holder.setInitParameter(JettyConstants.HTTP_PORT, Integer.toString(port));
		}
		if (httpsConnector != null) {
			int port = httpsConnector.getLocalPort();
			if (port == -1)
				port = httpsConnector.getPort();
			holder.setInitParameter(JettyConstants.HTTPS_PORT, Integer.toString(port));
		}
		String otherInfo = (String) dictionary.get(JettyConstants.OTHER_INFO);
		if (otherInfo != null)
			holder.setInitParameter(JettyConstants.OTHER_INFO, otherInfo);

		ServletContextHandler httpContext = createHttpContext(dictionary);
		if (null != customizer)
			httpContext = (ServletContextHandler) customizer.customizeContext(httpContext, dictionary);

		httpContext.addServlet(holder, "/*"); //$NON-NLS-1$
		server.setHandler(httpContext);

		try {
			server.start();
		} catch (Exception e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}
		servers.put(pid, server);
	}

	public synchronized void shutdown() throws Exception {
		for (Iterator<Server> it = servers.values().iterator(); it.hasNext();) {
			Server server = it.next();
			server.stop();
		}
		servers.clear();
	}

	private Connector createHttpConnector(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Boolean httpEnabled = null;
		Object httpEnabledObj = dictionary.get(JettyConstants.HTTP_ENABLED);
		if (httpEnabledObj instanceof Boolean)
		{
			httpEnabled = (Boolean) httpEnabledObj;			
		}
		else
			if (httpEnabledObj instanceof String)
			{
				httpEnabled = Boolean.parseBoolean(httpEnabledObj.toString());
			}	
		if (httpEnabled != null && !httpEnabled.booleanValue())
			return null;

		
		Integer httpPort = null;
		Object httpPortObj = dictionary.get(JettyConstants.HTTP_PORT);
		if (httpPortObj instanceof Integer)
		{
			httpPort = (Integer) httpPortObj;
		}
		else
			if (httpPortObj instanceof String)
			{
				httpPort = Integer.valueOf(httpPortObj.toString());
			}
		if (httpPort == null)
			return null;

		Boolean nioEnabled = null;
		Object nioEnabledObj = dictionary.get(JettyConstants.HTTP_NIO);
		if (nioEnabledObj instanceof Boolean)
		{
			nioEnabled = (Boolean) nioEnabledObj;			
		}
		else
			if (nioEnabledObj instanceof String)
			{
				nioEnabled = Boolean.parseBoolean(nioEnabledObj.toString());
			}	
		if (nioEnabled == null)
			nioEnabled = getDefaultNIOEnablement();

		Connector connector;
		if (nioEnabled.booleanValue())
			connector = new SelectChannelConnector();
		else
			connector = new SocketConnector();

		connector.setPort(httpPort.intValue());

		String httpHost = (String) dictionary.get(JettyConstants.HTTP_HOST);
		if (httpHost != null) {
			connector.setHost(httpHost);
		}

		if (connector.getPort() == 0) {
			try {
				connector.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next available port 
				e.printStackTrace();
			}
		}
		return connector;
	}

	private Boolean getDefaultNIOEnablement() {
		Properties systemProperties = System.getProperties();
		String javaVendor = systemProperties.getProperty("java.vendor", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (javaVendor.equals("IBM Corporation")) { //$NON-NLS-1$
			String javaVersion = systemProperties.getProperty("java.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (javaVersion.startsWith("1.4")) //$NON-NLS-1$
				return Boolean.FALSE;
			// Note: no problems currently logged with 1.5
			if (javaVersion.equals("1.6.0")) { //$NON-NLS-1$
				String jclVersion = systemProperties.getProperty("java.jcl.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (jclVersion.startsWith("2007")) //$NON-NLS-1$
					return Boolean.FALSE;
				if (jclVersion.startsWith("2008") && !jclVersion.startsWith("200811") && !jclVersion.startsWith("200812")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	@SuppressWarnings("deprecation")
	private Connector createHttpsConnector(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Boolean httpsEnabled = null;
		Object httpsEnabledObj = dictionary.get(JettyConstants.HTTPS_ENABLED);
		if (httpsEnabledObj instanceof Boolean)
		{
			httpsEnabled = (Boolean) httpsEnabledObj;			
		}
		else
			if (httpsEnabledObj instanceof String)
			{
				httpsEnabled = Boolean.parseBoolean(httpsEnabledObj.toString());
			}	
		if (httpsEnabled == null || !httpsEnabled.booleanValue())
			return null;

		Integer httpsPort = null;
		Object httpsPortObj = dictionary.get(JettyConstants.HTTPS_PORT);
		if (httpsPortObj instanceof Integer)
		{
			httpsPort = (Integer) httpsPortObj;
		}
		else
			if (httpsPortObj instanceof String)
			{
				httpsPort = Integer.valueOf(httpsPortObj.toString());
			}
		if (httpsPort == null)
			return null;

		SslSocketConnector sslConnector = new SslSocketConnector();
		sslConnector.setPort(httpsPort.intValue());

		String httpsHost = (String) dictionary.get(JettyConstants.HTTPS_HOST);
		if (httpsHost != null) {
			sslConnector.setHost(httpsHost);
		}

		String keyStore = (String) dictionary.get(JettyConstants.SSL_KEYSTORE);
		if (keyStore != null)
			sslConnector.setKeystore(keyStore);

		String password = (String) dictionary.get(JettyConstants.SSL_PASSWORD);
		if (password != null)
			sslConnector.setPassword(password);

		String keyPassword = (String) dictionary.get(JettyConstants.SSL_KEYPASSWORD);
		if (keyPassword != null)
			sslConnector.setKeyPassword(keyPassword);

		Object needClientAuth = dictionary.get(JettyConstants.SSL_NEEDCLIENTAUTH);
		if (needClientAuth != null) {
			if (needClientAuth instanceof String)
				needClientAuth = Boolean.valueOf((String) needClientAuth);

			sslConnector.setNeedClientAuth(((Boolean) needClientAuth).booleanValue());
		}

		Object wantClientAuth = dictionary.get(JettyConstants.SSL_WANTCLIENTAUTH);
		if (wantClientAuth != null) {
			if (wantClientAuth instanceof String)
				wantClientAuth = Boolean.valueOf((String) wantClientAuth);

			sslConnector.setWantClientAuth(((Boolean) wantClientAuth).booleanValue());
		}

		String protocol = (String) dictionary.get(JettyConstants.SSL_PROTOCOL);
		if (protocol != null)
			sslConnector.setProtocol(protocol);

		String keystoreType = (String) dictionary.get(JettyConstants.SSL_KEYSTORETYPE);
		if (keystoreType != null)
			sslConnector.setKeystoreType(keystoreType);

		if (sslConnector.getPort() == 0) {
			try {
				sslConnector.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next available port 
				e.printStackTrace();
			}
		}
		return sslConnector;
	}

	private ServletContextHandler createHttpContext(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		ServletContextHandler httpContext = new ServletContextHandler();
		httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());

		String contextPathProperty = (String) dictionary.get(JettyConstants.CONTEXT_PATH);
		if (contextPathProperty == null)
			contextPathProperty = "/"; //$NON-NLS-1$
		httpContext.setContextPath(contextPathProperty);

		File contextWorkDir = new File(workDir, DIR_PREFIX + dictionary.get(Constants.SERVICE_PID).hashCode());
		contextWorkDir.mkdir();
		httpContext.setAttribute(CONTEXT_TEMPDIR, contextWorkDir);

		HashSessionManager sessionManager = new HashSessionManager();
		Integer sessionInactiveInterval = null;
		Object sessionInactiveIntervalObj = dictionary.get(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
		if (sessionInactiveIntervalObj instanceof Integer)
		{
			sessionInactiveInterval = (Integer) sessionInactiveIntervalObj;
		}
		else
			if (sessionInactiveIntervalObj instanceof String)
			{
				sessionInactiveInterval = Integer.valueOf(sessionInactiveIntervalObj.toString());
			}
		if (sessionInactiveInterval != null)
			sessionManager.setMaxInactiveInterval(sessionInactiveInterval.intValue());

		httpContext.setSessionHandler(new SessionHandler(sessionManager));

		return httpContext;
	}

	private JettyCustomizer createJettyCustomizer(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		String customizerClass = (String) dictionary.get(JettyConstants.CUSTOMIZER_CLASS);
		if (null == customizerClass)
			return null;

		try {
			return (JettyCustomizer) Class.forName(customizerClass).newInstance();
		} catch (Exception e) {
			// TODO: consider logging this, but we should still continue 
			e.printStackTrace();
			return null;
		}
	}

	public static class InternalHttpServiceServlet implements Servlet {
		//		private static final long serialVersionUID = 7477982882399972088L;
		private Servlet httpServiceServlet = new HttpServiceServlet();
		private ClassLoader contextLoader;

		public void init(ServletConfig config) throws ServletException {
			ServletContext context = config.getServletContext();
			contextLoader = (ClassLoader) context.getAttribute(INTERNAL_CONTEXT_CLASSLOADER);

			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.init(config);
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		public void destroy() {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.destroy();
			} finally {
				thread.setContextClassLoader(current);
			}
			contextLoader = null;
		}

		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.service(req, res);
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		public ServletConfig getServletConfig() {
			return httpServiceServlet.getServletConfig();
		}

		public String getServletInfo() {
			return httpServiceServlet.getServletInfo();
		}
	}

	// deleteDirectory is a convenience method to recursively delete a directory
	private static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}
}
