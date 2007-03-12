/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Cognos Incorporated - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import org.eclipse.core.runtime.*;
import org.osgi.service.http.*;

public class ServletManager implements ExtensionPointTracker.Listener {

	private static final String SERVLETS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.servlets"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_NAME = "httpcontext-name"; //$NON-NLS-1$

	private static final String PARAM_VALUE = "value"; //$NON-NLS-1$

	private static final String PARAM_NAME = "name"; //$NON-NLS-1$

	private static final String INIT_PARAM = "init-param"; //$NON-NLS-1$

	private static final String SERVLET = "servlet"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String LOAD_ON_STARTUP = "load-on-startup"; //$NON-NLS-1$

	private HttpService httpService;

	private ExtensionPointTracker tracker;

	private HttpContextManager httpContextManager;

	private List registered = new ArrayList();

	public ServletManager(HttpService httpService, HttpContextManager httpContextManager, IExtensionRegistry registry) {
		this.httpService = httpService;
		this.httpContextManager = httpContextManager;
		tracker = new ExtensionPointTracker(registry, SERVLETS_EXTENSION_POINT, this);
	}

	public void start() {
		tracker.open();
	}

	public void stop() {
		tracker.close();
	}

	public void added(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement servletElement = elements[i];
			if (!SERVLET.equals(servletElement.getName()))
				continue;

			ServletWrapper wrapper = new ServletWrapper(servletElement);
			String alias = servletElement.getAttribute(ALIAS);

			Dictionary initparams = new Hashtable();
			IConfigurationElement[] initParams = servletElement.getChildren(INIT_PARAM);
			for (int j = 0; j < initParams.length; ++j) {
				String paramName = initParams[j].getAttribute(PARAM_NAME);
				String paramValue = initParams[j].getAttribute(PARAM_VALUE);
				initparams.put(paramName, paramValue);
			}

			boolean loadOnStartup = new Boolean(servletElement.getAttribute(LOAD_ON_STARTUP)).booleanValue();

			String httpContextName = servletElement.getAttribute(HTTPCONTEXT_NAME);
			HttpContext context = null;
			if (httpContextName == null)
				context = httpContextManager.getDefaultHttpContext(extension.getContributor().getName());
			else
				context = httpContextManager.getHttpContext(httpContextName);

			try {
				httpService.registerServlet(alias, wrapper, initparams, context);
				registered.add(servletElement);
				if (loadOnStartup)
					wrapper.initializeDelegate();
			} catch (ServletException e) {
				// this should never happen as the init() called is the ServletWrapper implementation
				e.printStackTrace();
			} catch (NamespaceException e) {
				// TODO Should log this perhaps with the LogService?
				e.printStackTrace();
			}
		}
	}

	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement servletElement = elements[i];
			if (!SERVLET.equals(servletElement.getName()))
				continue;
			String alias = servletElement.getAttribute(ALIAS);
			if (registered.remove(servletElement))
				httpService.unregister(alias);
		}
	}

	private static class ServletWrapper implements Servlet {

		private static final String CLASS = "class"; //$NON-NLS-1$
		private IConfigurationElement element;
		private Servlet delegate;
		private ServletConfig config;

		public ServletWrapper(IConfigurationElement element) {
			this.element = element;
		}

		public void init(ServletConfig config) throws ServletException {
			this.config = config;
		}

		public ServletConfig getServletConfig() {
			return config;
		}

		public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
			initializeDelegate();
			delegate.service(arg0, arg1);
		}

		public String getServletInfo() {
			return ""; //$NON-NLS-1$
		}

		public void destroy() {
			destroyDelegate();
		}

		synchronized void initializeDelegate() throws ServletException {
			if (delegate == null) {
				try {
					Servlet newDelegate = (Servlet) element.createExecutableExtension(CLASS);
					newDelegate.init(config);
					delegate = newDelegate;
				} catch (CoreException e) {
					throw new ServletException(e);
				}
			}
		}

		private synchronized void destroyDelegate() {
			if (delegate != null) {
				Servlet doomedDelegate = delegate;
				delegate = null;
				doomedDelegate.destroy();
			}
		}
	}
}
