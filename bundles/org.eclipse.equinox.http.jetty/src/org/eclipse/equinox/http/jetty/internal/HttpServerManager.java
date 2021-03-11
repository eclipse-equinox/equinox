/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
 *     Red Hat, Inc. - Jetty 9 adoption.
 *     Raymond Augé - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class HttpServerManager implements ManagedServiceFactory {

	private static final int DEFAULT_IDLE_TIMEOUT = 30000;
	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir"; //$NON-NLS-1$
	private static final String DIR_PREFIX = "pid_"; //$NON-NLS-1$
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader"; //$NON-NLS-1$

	private Map<String, Server> servers = new HashMap<>();
	private File workDir;

	public HttpServerManager(File workDir) {
		this.workDir = workDir;
	}

	@Override
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

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public synchronized void updated(String pid, Dictionary<String, ?> dictionary) throws ConfigurationException {
		deleted(pid);
		Server server = new Server(new QueuedThreadPool(Details.getInt(dictionary, JettyConstants.HTTP_MAXTHREADS, 200), Details.getInt(dictionary, JettyConstants.HTTP_MINTHREADS, 8)));

		JettyCustomizer customizer = createJettyCustomizer(dictionary);

		/**
		 * May be modified by createHttp(s)Connector.
		 */
		HttpConfiguration http_config = new HttpConfiguration();

		ServerConnector httpConnector = createHttpConnector(dictionary, server, http_config);

		ServerConnector httpsConnector = createHttpsConnector(dictionary, server, http_config);

		if (null != customizer)
			httpConnector = (ServerConnector) customizer.customizeHttpConnector(httpConnector, dictionary);

		if (httpConnector != null) {
			try {
				httpConnector.open();
			} catch (IOException e) {
				throw new ConfigurationException(pid, e.getMessage(), e);
			}
			server.addConnector(httpConnector);
		}

		if (null != customizer)
			httpsConnector = (ServerConnector) customizer.customizeHttpsConnector(httpsConnector, dictionary);

		if (httpsConnector != null) {
			try {
				httpsConnector.open();
			} catch (IOException e) {
				throw new ConfigurationException(pid, e.getMessage(), e);
			}
			server.addConnector(httpsConnector);
		}

		ServletHolder holder = new ServletHolder(new InternalHttpServiceServlet());
		holder.setInitOrder(0);
		holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
		holder.setInitParameter(Constants.SERVICE_DESCRIPTION, "Equinox Jetty-based Http Service"); //$NON-NLS-1$

		String multipartServletName = "Equinox Jetty-based Http Service - Multipart Servlet"; //$NON-NLS-1$

		holder.setInitParameter("multipart.servlet.name", multipartServletName); //$NON-NLS-1$

		if (httpConnector != null) {
			int port = httpConnector.getLocalPort();
			if (port == -1)
				port = httpConnector.getPort();
			holder.setInitParameter(JettyConstants.HTTP_PORT, Integer.toString(port));
			String host = httpConnector.getHost();
			if (host != null)
				holder.setInitParameter(JettyConstants.HTTP_HOST, host);
		}
		if (httpsConnector != null) {
			int port = httpsConnector.getLocalPort();
			if (port == -1)
				port = httpsConnector.getPort();
			holder.setInitParameter(JettyConstants.HTTPS_PORT, Integer.toString(port));
			String host = httpsConnector.getHost();
			if (host != null)
				holder.setInitParameter(JettyConstants.HTTPS_HOST, host);
		}
		String otherInfo = Details.getString(dictionary, JettyConstants.OTHER_INFO, null);
		if (otherInfo != null)
			holder.setInitParameter(JettyConstants.OTHER_INFO, otherInfo);

		ServletContextHandler httpContext = createHttpContext(dictionary);
		holder.setInitParameter(JettyConstants.CONTEXT_PATH, httpContext.getContextPath());
		httpContext.addServlet(holder, "/*"); //$NON-NLS-1$
		server.setHandler(httpContext);

		if (null != customizer)
			httpContext = (ServletContextHandler) customizer.customizeContext(httpContext, dictionary);

		try {
			server.start();
			SessionHandler sessionManager = httpContext.getSessionHandler();
			sessionManager.addEventListener((HttpSessionIdListener) holder.getServlet());
			HouseKeeper houseKeeper = server.getSessionIdManager().getSessionHouseKeeper();
			houseKeeper.setIntervalSec(Details.getLong(dictionary, JettyConstants.HOUSEKEEPER_INTERVAL, houseKeeper.getIntervalSec()));
		} catch (Exception e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}
		servers.put(pid, server);
	}

	private ServerConnector createHttpsConnector(@SuppressWarnings("rawtypes") Dictionary dictionary, Server server, HttpConfiguration http_config) {
		ServerConnector httpsConnector = null;
		if (Details.getBoolean(dictionary, JettyConstants.HTTPS_ENABLED, false)) {
			// SSL Context Factory for HTTPS and SPDY
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			//sslContextFactory.setKeyStore(KeyS)

			//Not sure if the next tree are properly migrated from jetty 8...
			sslContextFactory.setKeyStorePath(Details.getString(dictionary, JettyConstants.SSL_KEYSTORE, null));
			sslContextFactory.setKeyStorePassword(Details.getString(dictionary, JettyConstants.SSL_PASSWORD, null));
			sslContextFactory.setKeyManagerPassword(Details.getString(dictionary, JettyConstants.SSL_KEYPASSWORD, null));
			sslContextFactory.setKeyStoreType(Details.getString(dictionary, JettyConstants.SSL_KEYSTORETYPE, "JKS")); //$NON-NLS-1$
			sslContextFactory.setProtocol(Details.getString(dictionary, JettyConstants.SSL_PROTOCOL, "TLS")); //$NON-NLS-1$
			sslContextFactory.setWantClientAuth(Details.getBoolean(dictionary, JettyConstants.SSL_WANTCLIENTAUTH, false));
			sslContextFactory.setNeedClientAuth(Details.getBoolean(dictionary, JettyConstants.SSL_NEEDCLIENTAUTH, false));

			// HTTPS Configuration
			HttpConfiguration https_config = new HttpConfiguration(http_config);
			https_config.addCustomizer(new SecureRequestCustomizer());
			https_config.setHttpCompliance(HttpCompliance.RFC7230_LEGACY);

			// HTTPS connector
			httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https_config)); //$NON-NLS-1$
			httpsConnector.setPort(Details.getInt(dictionary, JettyConstants.HTTPS_PORT, 443));
			httpsConnector.setHost(Details.getString(dictionary, JettyConstants.HTTPS_HOST, null));
		}
		return httpsConnector;
	}

	private ServerConnector createHttpConnector(@SuppressWarnings("rawtypes") Dictionary dictionary, Server server, HttpConfiguration http_config) {
		ServerConnector httpConnector = null;
		if (Details.getBoolean(dictionary, JettyConstants.HTTP_ENABLED, true)) {
			// HTTP Configuration
			if (Details.getBoolean(dictionary, JettyConstants.HTTPS_ENABLED, false)) {
				http_config.setSecureScheme("https"); //$NON-NLS-1$
				http_config.setSecurePort(Details.getInt(dictionary, JettyConstants.HTTPS_PORT, 443));
			}
			http_config.setHttpCompliance(HttpCompliance.RFC7230_LEGACY);
			// HTTP connector
			httpConnector = new ServerConnector(server, new HttpConnectionFactory(http_config));
			httpConnector.setPort(Details.getInt(dictionary, JettyConstants.HTTP_PORT, 80));
			httpConnector.setHost(Details.getString(dictionary, JettyConstants.HTTP_HOST, null));
			httpConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
		}
		return httpConnector;
	}

	public synchronized void shutdown() throws Exception {
		for (Server server : servers.values()) {
			server.stop();
		}
		servers.clear();
	}

	private ServletContextHandler createHttpContext(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		ServletContextHandler httpContext = new ServletContextHandler();
		// hack in the mime type for xsd until jetty fixes it (bug 393218)
		httpContext.getMimeTypes().addMimeMapping("xsd", "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$
		httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());
		httpContext.setContextPath(Details.getString(dictionary, JettyConstants.CONTEXT_PATH, "/")); //$NON-NLS-1$

		File contextWorkDir = new File(workDir, DIR_PREFIX + dictionary.get(Constants.SERVICE_PID).hashCode());
		contextWorkDir.mkdir();
		httpContext.setAttribute(CONTEXT_TEMPDIR, contextWorkDir);
		SessionHandler handler = new SessionHandler();
		handler.setMaxInactiveInterval(Details.getInt(dictionary, JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, -1));
		httpContext.setSessionHandler(handler);

		return httpContext;
	}

	private JettyCustomizer createJettyCustomizer(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		String customizerClass = (String) dictionary.get(JettyConstants.CUSTOMIZER_CLASS);
		if (null == customizerClass)
			return null;

		try {
			return (JettyCustomizer) Class.forName(customizerClass).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			// TODO: consider logging this, but we should still continue
			e.printStackTrace();
			return null;
		}
	}

	public static class InternalHttpServiceServlet implements HttpSessionListener, HttpSessionIdListener, Servlet {
		//		private static final long serialVersionUID = 7477982882399972088L;
		private final Servlet httpServiceServlet = new HttpServiceServlet();
		private ClassLoader contextLoader;
		private final Method sessionDestroyed;
		private final Method sessionIdChanged;

		public InternalHttpServiceServlet() {
			Class<?> clazz = httpServiceServlet.getClass();

			try {
				sessionDestroyed = clazz.getMethod("sessionDestroyed", new Class<?>[] {String.class}); //$NON-NLS-1$
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			try {
				sessionIdChanged = clazz.getMethod("sessionIdChanged", new Class<?>[] {String.class}); //$NON-NLS-1$
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
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

		@Override
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

		@Override
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

		@Override
		public ServletConfig getServletConfig() {
			return httpServiceServlet.getServletConfig();
		}

		@Override
		public String getServletInfo() {
			return httpServiceServlet.getServletInfo();
		}

		@Override
		public void sessionCreated(HttpSessionEvent event) {
			// Nothing to do.
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent event) {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				sessionDestroyed.invoke(httpServiceServlet, event.getSession().getId());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// not likely
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		@Override
		public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				sessionIdChanged.invoke(httpServiceServlet, oldSessionId);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// not likely
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			} finally {
				thread.setContextClassLoader(current);
			}
		}
	}

	// deleteDirectory is a convenience method to recursively delete a directory
	private static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		return directory.delete();
	}

}
