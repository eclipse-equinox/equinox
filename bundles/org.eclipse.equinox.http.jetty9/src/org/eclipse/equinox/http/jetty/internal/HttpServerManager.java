/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - Jetty 9 adoption.
 *     Raymond Augé - bug fixes
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.session.HashSessionManager;
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

	private Map<String, Server> servers = new HashMap<String, Server>();
	private File workDir;
	private boolean servlet3multipart = false;

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
		Server server = new Server(new QueuedThreadPool(getMaxThreads(dictionary), getMinThreads(dictionary)));

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
		setupMultiPartConfig(dictionary, holder);

		httpContext.addServlet(holder, "/*"); //$NON-NLS-1$
		server.setHandler(httpContext);

		SessionManager sessionManager = httpContext.getSessionHandler().getSessionManager();
		try {
			sessionManager.addEventListener((HttpSessionIdListener) holder.getServlet());
		} catch (ServletException e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}

		try {
			server.start();
		} catch (Exception e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}
		servers.put(pid, server);
	}

	public void setServlet3multipart(boolean servlet3multipart) {
		this.servlet3multipart = servlet3multipart;
	}

	private ServerConnector createHttpsConnector(@SuppressWarnings("rawtypes") Dictionary dictionary, Server server, HttpConfiguration http_config) {
		ServerConnector httpsConnector = null;
		if (isHttpsEnabled(dictionary)) {
			// SSL Context Factory for HTTPS and SPDY
			SslContextFactory sslContextFactory = new SslContextFactory();
			//sslContextFactory.setKeyStore(KeyS)

			//Not sure if the next tree are properly migrated from jetty 8...
			sslContextFactory.setKeyStorePath((String) dictionary.get(JettyConstants.SSL_KEYSTORE));
			sslContextFactory.setKeyStorePassword((String) dictionary.get(JettyConstants.SSL_PASSWORD));
			sslContextFactory.setKeyManagerPassword((String) dictionary.get(JettyConstants.SSL_KEYPASSWORD));

			String keystoreType = (String) dictionary.get(JettyConstants.SSL_KEYSTORETYPE);
			if (keystoreType != null) {
				sslContextFactory.setKeyStoreType(keystoreType);
			}

			String protocol = (String) dictionary.get(JettyConstants.SSL_PROTOCOL);
			if (protocol != null) {
				sslContextFactory.setProtocol(protocol);
			}

			Object wantClientAuth = dictionary.get(JettyConstants.SSL_WANTCLIENTAUTH);
			if (wantClientAuth != null) {
				if (wantClientAuth instanceof String)
					wantClientAuth = Boolean.valueOf((String) wantClientAuth);

				sslContextFactory.setWantClientAuth((Boolean) wantClientAuth);
			}

			Object needClientAuth = dictionary.get(JettyConstants.SSL_NEEDCLIENTAUTH);
			if (needClientAuth != null) {
				if (needClientAuth instanceof String)
					needClientAuth = Boolean.valueOf((String) needClientAuth);

				sslContextFactory.setNeedClientAuth(((Boolean) needClientAuth));
			}

			// HTTPS Configuration
			HttpConfiguration https_config = new HttpConfiguration(http_config);
			https_config.addCustomizer(new SecureRequestCustomizer());

			// HTTPS connector
			httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https_config)); //$NON-NLS-1$
			httpsConnector.setPort(getIntProperty(dictionary, JettyConstants.HTTPS_PORT));
		}
		return httpsConnector;
	}

	private ServerConnector createHttpConnector(@SuppressWarnings("rawtypes") Dictionary dictionary, Server server, HttpConfiguration http_config) {
		ServerConnector httpConnector = null;
		if (isHttpEnabled(dictionary)) {
			// HTTP Configuration
			if (isHttpsEnabled(dictionary)) {
				http_config.setSecureScheme("https"); //$NON-NLS-1$
				http_config.setSecurePort(getIntProperty(dictionary, JettyConstants.HTTPS_PORT));
			}
			// HTTP connector
			httpConnector = new ServerConnector(server, new HttpConnectionFactory(http_config));
			httpConnector.setPort(getIntProperty(dictionary, JettyConstants.HTTP_PORT));
			httpConnector.setHost((String) dictionary.get(JettyConstants.HTTP_HOST));
			httpConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
		}
		return httpConnector;
	}

	public synchronized void shutdown() throws Exception {
		for (Iterator<Server> it = servers.values().iterator(); it.hasNext();) {
			Server server = it.next();
			server.stop();
		}
		servers.clear();
	}

	private Integer getIntProperty(@SuppressWarnings("rawtypes") Dictionary dictionary, String property) {
		Integer httpPort = null;
		Object httpPortObj = dictionary.get(property);
		if (httpPortObj instanceof Integer) {
			httpPort = (Integer) httpPortObj;
		} else if (httpPortObj instanceof String) {
			httpPort = Integer.valueOf(httpPortObj.toString());
		}
		if (httpPort == null) {
			throw new IllegalArgumentException("Expected " + property + "property, but it is not set."); //$NON-NLS-1$//$NON-NLS-2$
		}
		return httpPort;
	}

	/**
	 * If not configured -> enable
	 */
	private boolean isHttpEnabled(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Boolean httpEnabled = true;
		Object httpEnabledObj = dictionary.get(JettyConstants.HTTP_ENABLED);
		if (httpEnabledObj instanceof Boolean) {
			httpEnabled = (Boolean) httpEnabledObj;
		} else if (httpEnabledObj instanceof String) {
			httpEnabled = Boolean.parseBoolean(httpEnabledObj.toString());
		}
		return httpEnabled;
	}

	/**
	 * If not configured -> disable.
	 */
	private boolean isHttpsEnabled(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Boolean httpsEnabled = false;
		Object httpsEnabledObj = dictionary.get(JettyConstants.HTTPS_ENABLED);
		if (httpsEnabledObj instanceof Boolean) {
			httpsEnabled = (Boolean) httpsEnabledObj;
		} else if (httpsEnabledObj instanceof String) {
			httpsEnabled = Boolean.parseBoolean(httpsEnabledObj.toString());
		}
		return httpsEnabled;
	}

	private int getMaxThreads(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Integer maxThreads = 200;
		Object maxThreadsObj = dictionary.get(JettyConstants.HTTP_MAXTHREADS);
		if (maxThreadsObj instanceof Integer) {
			maxThreads = (Integer) maxThreadsObj;
		} else if (maxThreadsObj instanceof String) {
			maxThreads = Integer.parseInt(maxThreadsObj.toString());
		}
		return maxThreads;
	}

	private int getMinThreads(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		Integer minThreads = 8;
		Object minThreadsObj = dictionary.get(JettyConstants.HTTP_MINTHREADS);
		if (minThreadsObj instanceof Integer) {
			minThreads = (Integer) minThreadsObj;
		} else if (minThreadsObj instanceof String) {
			minThreads = Integer.parseInt(minThreadsObj.toString());
		}
		return minThreads;
	}

	private ServletContextHandler createHttpContext(@SuppressWarnings("rawtypes") Dictionary dictionary) {
		ServletContextHandler httpContext = new ServletContextHandler();
		// hack in the mime type for xsd until jetty fixes it (bug 393218)
		httpContext.getMimeTypes().addMimeMapping("xsd", "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (sessionInactiveIntervalObj instanceof Integer) {
			sessionInactiveInterval = (Integer) sessionInactiveIntervalObj;
		} else if (sessionInactiveIntervalObj instanceof String) {
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

	public static class InternalHttpServiceServlet implements HttpSessionIdListener, Servlet {
		//		private static final long serialVersionUID = 7477982882399972088L;
		private Servlet httpServiceServlet = new HttpServiceServlet();
		private ClassLoader contextLoader;
		private Method method;

		public void init(ServletConfig config) throws ServletException {
			ServletContext context = config.getServletContext();
			contextLoader = (ClassLoader) context.getAttribute(INTERNAL_CONTEXT_CLASSLOADER);

			Class<?> clazz = httpServiceServlet.getClass();
			try {
				method = clazz.getMethod("sessionIdChanged", new Class<?>[] {String.class});
			} catch (Exception e) {
				throw new ServletException(e);
			}

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

		public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				method.invoke(httpServiceServlet, oldSessionId);
			} catch (IllegalAccessException e) {
				// not likely
			} catch (IllegalArgumentException e) {
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

	private void setupMultiPartConfig(@SuppressWarnings("rawtypes") Dictionary dictionary, ServletHolder holder) {
		if (!servlet3multipart) {
			return;
		}

		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		holder.getRegistration().setMultipartConfig(multipartConfigElement);
	}

}
