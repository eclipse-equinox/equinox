/*******************************************************************************
 * Copyright (c) 2005, 2009 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

public class ServletManager implements ExtensionPointTracker.Listener {

	private static final String SERVLETS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.servlets"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_NAME = "httpcontext-name"; //$NON-NLS-1$

	private static final String PARAM_VALUE = "value"; //$NON-NLS-1$

	private static final String PARAM_NAME = "name"; //$NON-NLS-1$

	private static final String INIT_PARAM = "init-param"; //$NON-NLS-1$

	private static final String SERVLET = "servlet"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String LOAD_ON_STARTUP = "load-on-startup"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_ID = "httpcontextId"; //$NON-NLS-1$

	private static final String SERVICESELECTOR = "serviceSelector"; //$NON-NLS-1$

	private static final String CLASS = "class"; //$NON-NLS-1$

	private static final String FILTER = "filter"; //$NON-NLS-1$

	private final ExtensionPointTracker tracker;

	private final HttpRegistryManager httpRegistryManager;

	private final List<IConfigurationElement> registered = new ArrayList<>();

	private final ServiceReference<?> reference;

	public ServletManager(HttpRegistryManager httpRegistryManager, ServiceReference<?> reference,
			IExtensionRegistry registry) {
		this.httpRegistryManager = httpRegistryManager;
		this.reference = reference;
		tracker = new ExtensionPointTracker(registry, SERVLETS_EXTENSION_POINT, this);
	}

	public void start() {
		tracker.open();
	}

	public void stop() {
		tracker.close();
	}

	@Override
	public void added(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement serviceSelectorElement : elements) {
			if (!SERVICESELECTOR.equals(serviceSelectorElement.getName())) {
				continue;
			}

			org.osgi.framework.Filter serviceSelector = null;
			String clazz = serviceSelectorElement.getAttribute(CLASS);
			if (clazz != null) {
				try {
					serviceSelector = (org.osgi.framework.Filter) serviceSelectorElement
							.createExecutableExtension(CLASS);
				} catch (CoreException e) {
					// log it.
					e.printStackTrace();
					return;
				}
			} else {
				String filter = serviceSelectorElement.getAttribute(FILTER);
				if (filter == null) {
					return;
				}

				try {
					serviceSelector = FrameworkUtil.createFilter(filter);
				} catch (InvalidSyntaxException e) {
					// log it.
					e.printStackTrace();
					return;
				}
			}

			if (!serviceSelector.match(reference)) {
				return;
			}

			break;
		}

		for (IConfigurationElement servletElement : elements) {
			if (!SERVLET.equals(servletElement.getName())) {
				continue;
			}

			ServletWrapper wrapper = new ServletWrapper(servletElement);
			String alias = servletElement.getAttribute(ALIAS);
			if (alias == null) {
				continue; // alias is mandatory - ignore this.
			}

			Dictionary<String, String> initparams = new Hashtable<>();
			IConfigurationElement[] initParams = servletElement.getChildren(INIT_PARAM);
			for (IConfigurationElement initParam : initParams) {
				String paramName = initParam.getAttribute(PARAM_NAME);
				String paramValue = initParam.getAttribute(PARAM_VALUE);
				initparams.put(paramName, paramValue);
			}

			boolean loadOnStartup = Boolean.valueOf(servletElement.getAttribute(LOAD_ON_STARTUP)).booleanValue();
			if (loadOnStartup) {
				wrapper.setLoadOnStartup();
			}

			String httpContextId = servletElement.getAttribute(HTTPCONTEXT_ID);
			if (httpContextId == null) {
				httpContextId = servletElement.getAttribute(HTTPCONTEXT_NAME);
			}

			if (httpContextId != null && httpContextId.indexOf('.') == -1) {
				httpContextId = servletElement.getNamespaceIdentifier() + "." + httpContextId; //$NON-NLS-1$
			}

			if (httpRegistryManager.addServletContribution(alias, wrapper, initparams, httpContextId,
					extension.getContributor())) {
				registered.add(servletElement);
			}
		}
	}

	@Override
	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement servletElement : elements) {
			if (registered.remove(servletElement)) {
				String alias = servletElement.getAttribute(ALIAS);
				httpRegistryManager.removeContribution(alias);
			}
		}
	}

	private static class ServletWrapper implements Servlet {
		private final IConfigurationElement element;
		private Servlet delegate;
		private ServletConfig config;
		private boolean loadOnStartup = false;

		public ServletWrapper(IConfigurationElement element) {
			this.element = element;
		}

		public void setLoadOnStartup() {
			this.loadOnStartup = true;
		}

		@Override
		public void init(ServletConfig servletConfig) throws ServletException {
			this.config = servletConfig;
			if (loadOnStartup) {
				initializeDelegate();
			}
		}

		@Override
		public ServletConfig getServletConfig() {
			return config;
		}

		@Override
		public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
			initializeDelegate();
			delegate.service(arg0, arg1);
		}

		@Override
		public String getServletInfo() {
			return ""; //$NON-NLS-1$
		}

		@Override
		public void destroy() {
			destroyDelegate();
		}

		private synchronized void initializeDelegate() throws ServletException {
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
