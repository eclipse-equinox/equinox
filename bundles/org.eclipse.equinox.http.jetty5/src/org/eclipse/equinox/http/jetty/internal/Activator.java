/*******************************************************************************
 * Copyright (c) 2006 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.internal;

import java.io.File;
import java.io.IOException;
import javax.servlet.*;
import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.mortbay.http.*;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private static final String CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.ContextClassLoader"; //$NON-NLS-1$
	private HttpServer server;

	public void start(BundleContext context) throws Exception {

		server = new HttpServer();

		SocketListener httpListener = createHttpListener(context);
		if (httpListener != null)
			server.addListener(httpListener);

		SocketListener httpsListener = createHttpsListener(context);
		if (httpsListener != null)
			server.addListener(httpsListener);

		ServletHandler servlets = new ServletHandler();
		servlets.setAutoInitializeServlets(true);

		ServletHolder holder = servlets.addServlet("/*", InternalHttpServiceServlet.class.getName()); //$NON-NLS-1$
		holder.setInitOrder(0);

		HttpContext httpContext = createHttpContext(context);
		httpContext.addHandler(servlets);

		server.addContext(httpContext);
		server.start();
	}

	public void stop(BundleContext context) throws Exception {
		server.stop();
		server = null;
	}

	private SocketListener createHttpListener(BundleContext context) {
		int httpPort = 80;
		String httpPortProperty = context.getProperty("org.osgi.service.http.port"); //$NON-NLS-1$
		if (httpPortProperty != null) {
			try {
				httpPort = Integer.parseInt(httpPortProperty);
			} catch (NumberFormatException e) {
				//(log this) ignore and use default
			}
		}

		if (httpPort < 1)
			return null;

		SocketListener listener = new SocketListener();
		listener.setPort(httpPort);
		return listener;
	}

	private SocketListener createHttpsListener(BundleContext context) {

		String sslEnabled = context.getProperty("org.eclipse.equinox.http.jetty.ssl.enabled"); //$NON-NLS-1$
		if (!Boolean.valueOf(sslEnabled).booleanValue())
			return null;

		int httpsPort = 443;
		String httpsPortProperty = context.getProperty("org.osgi.service.http.port.secure"); //$NON-NLS-1$
		if (httpsPortProperty != null) {
			try {
				httpsPort = Integer.parseInt(httpsPortProperty);
			} catch (NumberFormatException e) {
				//(log this) ignore and use default
			}
		}
		if (httpsPort < 1)
			return null;

		SslListener listener = new SslListener();
		listener.setPort(httpsPort);

		String keyStore = context.getProperty("org.eclipse.equinox.http.jetty.ssl.keystore"); //$NON-NLS-1$
		if (keyStore != null)
			listener.setKeystore(keyStore);

		String password = context.getProperty("org.eclipse.equinox.http.jetty.ssl.password"); //$NON-NLS-1$
		if (password != null)
			listener.setPassword(password);

		String keyPassword = context.getProperty("org.eclipse.equinox.http.jetty.ssl.keypassword"); //$NON-NLS-1$
		if (keyPassword != null)
			listener.setKeyPassword(keyPassword);

		String needClientAuth = context.getProperty("org.eclipse.equinox.http.jetty.ssl.needclientauth"); //$NON-NLS-1$
		if (needClientAuth != null)
			listener.setNeedClientAuth(Boolean.valueOf(needClientAuth).booleanValue());

		String wantClientAuth = context.getProperty("org.eclipse.equinox.http.jetty.ssl.wantclientauth"); //$NON-NLS-1$
		if (wantClientAuth != null)
			listener.setWantClientAuth(Boolean.valueOf(wantClientAuth).booleanValue());

		String protocol = context.getProperty("org.eclipse.equinox.http.jetty.ssl.protocol"); //$NON-NLS-1$
		if (protocol != null)
			listener.setProtocol(protocol);

		String algorithm = context.getProperty("org.eclipse.equinox.http.jetty.ssl.algorithm"); //$NON-NLS-1$
		if (algorithm != null)
			listener.setAlgorithm(algorithm);

		String keystoreType = context.getProperty("org.eclipse.equinox.http.jetty.ssl.keystoretype"); //$NON-NLS-1$
		if (keystoreType != null)
			listener.setKeystoreType(keystoreType);

		return listener;
	}

	private HttpContext createHttpContext(BundleContext context) {
		String contextPathProperty = context.getProperty("org.eclipse.equinox.http.jetty.contextPath"); //$NON-NLS-1$
		if (contextPathProperty == null)
			contextPathProperty = "/"; //$NON-NLS-1$
		HttpContext httpContext = new HttpContext();
		httpContext.setAttribute(CONTEXT_CLASSLOADER, Thread.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());
		httpContext.setContextPath(contextPathProperty);
		File jettyWorkDir = new File(context.getDataFile(""), "jettywork"); //$NON-NLS-1$ //$NON-NLS-2$
		jettyWorkDir.mkdir();
		httpContext.setTempDirectory(jettyWorkDir);

		return httpContext;
	}
	
	public static class InternalHttpServiceServlet implements Servlet {
		private static final long serialVersionUID = 7477982882399972088L;
		private Servlet httpServiceServlet = new HttpServiceServlet();
		private ClassLoader contextLoader;

		public void init(ServletConfig config) throws ServletException {
			ServletContext context = config.getServletContext();
			contextLoader = (ClassLoader) context.getAttribute(CONTEXT_CLASSLOADER);
			
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
}
