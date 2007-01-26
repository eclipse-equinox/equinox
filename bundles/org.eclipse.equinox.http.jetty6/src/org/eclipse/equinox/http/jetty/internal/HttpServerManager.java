/*******************************************************************************
 * Copyright (c) 2006 Eclipse.org
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.mortbay.http.*;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class HttpServerManager implements ManagedServiceFactory {

	private static final String DIR_PREFIX = "pid_"; //$NON-NLS-1$
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader"; //$NON-NLS-1$

	// Http Service properties - the autostarted service will look these up in the BundleContext by prefixing with "org.eclipse.equinox.http.jetty."
	static final String HTTP_ENABLED = "http.enabled"; //$NON-NLS-1$
	static final String HTTP_PORT = "http.port"; //$NON-NLS-1$
	static final String HTTPS_ENABLED = "https.enabled"; //$NON-NLS-1$
	static final String HTTPS_PORT = "https.port"; //$NON-NLS-1$
	static final String SSL_KEYSTORE = "ssl.keystore"; //$NON-NLS-1$
	static final String SSL_PASSWORD = "ssl.password"; //$NON-NLS-1$
	static final String SSL_KEYPASSWORD = "ssl.keypassword"; //$NON-NLS-1$
	static final String SSL_NEEDCLIENTAUTH = "ssl.needclientauth"; //$NON-NLS-1$
	static final String SSL_WANTCLIENTAUTH = "ssl.wantclientauth"; //$NON-NLS-1$
	static final String SSL_PROTOCOL = "ssl.protocol"; //$NON-NLS-1$
	static final String SSL_ALGORITHM = "ssl.algorithm"; //$NON-NLS-1$
	static final String SSL_KEYSTORETYPE = "ssl.keystoretype"; //$NON-NLS-1$
	static final String CONTEXT_PATH = "context.path"; //$NON-NLS-1$
	static final String OTHER_INFO = "other.info"; //$NON-NLS-1$

	private Map servers = new HashMap();
	private File workDir;

	public HttpServerManager(File workDir) {
		this.workDir = workDir;
	}

	public synchronized void deleted(String pid) {
		HttpServer server = (HttpServer) servers.remove(pid);
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

	public synchronized void updated(String pid, Dictionary dictionary) throws ConfigurationException {
		deleted(pid);
		HttpServer server = new HttpServer();
		SocketListener httpListener = createHttpListener(dictionary);
		if (httpListener != null)
			server.addListener(httpListener);

		SocketListener httpsListener = createHttpsListener(dictionary);
		if (httpsListener != null)
			server.addListener(httpsListener);

		ServletHandler servlets = new ServletHandler();
		servlets.setAutoInitializeServlets(true);

		ServletHolder holder = servlets.addServlet("/*", InternalHttpServiceServlet.class.getName()); //$NON-NLS-1$
		holder.setInitOrder(0);
		holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
		holder.setInitParameter(Constants.SERVICE_DESCRIPTION, "Equinox Jetty-based Http Service"); //$NON-NLS-1$
		if (httpListener != null)
			holder.setInitParameter(HTTP_PORT, new Integer(httpListener.getPort()).toString());
		if (httpsListener != null)	
			holder.setInitParameter(HTTPS_PORT, new Integer(httpsListener.getPort()).toString());
		
		String otherInfo = (String) dictionary.get(OTHER_INFO);
		if (otherInfo != null)
			holder.setInitParameter(OTHER_INFO, otherInfo);
		
		HttpContext httpContext = createHttpContext(dictionary);
		httpContext.addHandler(servlets);

		server.addContext(httpContext);
		try {
			server.start();
		} catch (Exception e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}
		servers.put(pid, server);
	}

	public synchronized void shutdown() throws Exception {
		for (Iterator it = servers.values().iterator(); it.hasNext();) {
			HttpServer server = (HttpServer) it.next();
			server.stop();
		}
		servers.clear();
	}

	private SocketListener createHttpListener(Dictionary dictionary) {
		Boolean httpEnabled = (Boolean) dictionary.get(HTTP_ENABLED);
		if (httpEnabled != null && !httpEnabled.booleanValue())
			return null;
		
		Integer httpPort = (Integer) dictionary.get(HTTP_PORT);
		if (httpPort == null)
			return null;

		SocketListener listener = new SocketListener();
		listener.setPort(httpPort.intValue());
		if (listener.getPort() == 0) {
			try {
				listener.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next available port 
				e.printStackTrace();
			}
		}		
		return listener;
	}

	private SocketListener createHttpsListener(Dictionary dictionary) {
		Boolean httpsEnabled = (Boolean) dictionary.get(HTTPS_ENABLED);
		if (httpsEnabled == null || !httpsEnabled.booleanValue())
			return null;

		Integer httpsPort = (Integer) dictionary.get(HTTPS_PORT);
		if (httpsPort == null)
			return null;

		SslListener listener = new SslListener();
		listener.setPort(httpsPort.intValue());

		String keyStore = (String) dictionary.get(SSL_KEYSTORE);
		if (keyStore != null)
			listener.setKeystore(keyStore);

		String password = (String) dictionary.get(SSL_PASSWORD);
		if (password != null)
			listener.setPassword(password);

		String keyPassword = (String) dictionary.get(SSL_KEYPASSWORD);
		if (keyPassword != null)
			listener.setKeyPassword(keyPassword);

		String needClientAuth = (String) dictionary.get(SSL_NEEDCLIENTAUTH);
		if (needClientAuth != null)
			listener.setNeedClientAuth(Boolean.valueOf(needClientAuth).booleanValue());

		String wantClientAuth = (String) dictionary.get(SSL_WANTCLIENTAUTH);
		if (wantClientAuth != null)
			listener.setWantClientAuth(Boolean.valueOf(wantClientAuth).booleanValue());

		String protocol = (String) dictionary.get(SSL_PROTOCOL);
		if (protocol != null)
			listener.setProtocol(protocol);

		String algorithm = (String) dictionary.get(SSL_ALGORITHM);
		if (algorithm != null)
			listener.setAlgorithm(algorithm);

		String keystoreType = (String) dictionary.get(SSL_KEYSTORETYPE);
		if (keystoreType != null)
			listener.setKeystoreType(keystoreType);

		if (listener.getPort() == 0) {
			try {
				listener.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next available port 
				e.printStackTrace();
			}
		}	
		return listener;
	}

	private HttpContext createHttpContext(Dictionary dictionary) {
		HttpContext httpContext = new HttpContext();
		httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());

		String contextPathProperty = (String) dictionary.get(CONTEXT_PATH);
		if (contextPathProperty == null)
			contextPathProperty = "/"; //$NON-NLS-1$
		httpContext.setContextPath(contextPathProperty);

		File contextWorkDir = new File(workDir, DIR_PREFIX + dictionary.get(Constants.SERVICE_PID).hashCode());
		contextWorkDir.mkdir();
		httpContext.setTempDirectory(contextWorkDir);

		return httpContext;
	}

	public static class InternalHttpServiceServlet implements Servlet {
		private static final long serialVersionUID = 7477982882399972088L;
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

	/**
	 * deleteDirectory is a convenience method to recursively delete a directory
	 * @param directory - the directory to delete.
	 * @return was the delete succesful
	 */
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
