/*******************************************************************************
 * Copyright (c) 2005, 2010 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.Filter;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

public class FilterManager implements ExtensionPointTracker.Listener {

	private static final String FILTERS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.filters"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_NAME = "httpcontext-name"; //$NON-NLS-1$

	private static final String PARAM_VALUE = "value"; //$NON-NLS-1$

	private static final String PARAM_NAME = "name"; //$NON-NLS-1$

	private static final String INIT_PARAM = "init-param"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String LOAD_ON_STARTUP = "load-on-startup"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_ID = "httpcontextId"; //$NON-NLS-1$

	private static final String SERVICESELECTOR = "serviceSelector"; //$NON-NLS-1$

	private static final String CLASS = "class"; //$NON-NLS-1$

	private static final String FILTER = "filter"; //$NON-NLS-1$

	private ExtensionPointTracker tracker;

	private HttpRegistryManager httpRegistryManager;

	private Map registered = new HashMap();

	private ServiceReference reference;

	public FilterManager(HttpRegistryManager httpRegistryManager, ServiceReference reference, IExtensionRegistry registry) {
		this.httpRegistryManager = httpRegistryManager;
		this.reference = reference;
		tracker = new ExtensionPointTracker(registry, FILTERS_EXTENSION_POINT, this);
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
			IConfigurationElement serviceSelectorElement = elements[i];
			if (!SERVICESELECTOR.equals(serviceSelectorElement.getName()))
				continue;

			org.osgi.framework.Filter serviceSelector = null;
			String clazz = serviceSelectorElement.getAttribute(CLASS);
			if (clazz != null) {
				try {
					serviceSelector = (org.osgi.framework.Filter) serviceSelectorElement.createExecutableExtension(CLASS);
				} catch (CoreException e) {
					// log it.
					e.printStackTrace();
					return;
				}
			} else {
				String filter = serviceSelectorElement.getAttribute(FILTER);
				if (filter == null)
					return;

				try {
					serviceSelector = FrameworkUtil.createFilter(filter);
				} catch (InvalidSyntaxException e) {
					// log it.
					e.printStackTrace();
					return;
				}
			}

			if (!serviceSelector.match(reference))
				return;

			break;
		}

		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement filterElement = elements[i];
			if (!FILTER.equals(filterElement.getName()))
				continue;

			FilterWrapper wrapper = new FilterWrapper(filterElement);
			String alias = filterElement.getAttribute(ALIAS);
			if (alias == null)
				continue; // alias is mandatory - ignore this.

			Dictionary initparams = new Hashtable();
			IConfigurationElement[] initParams = filterElement.getChildren(INIT_PARAM);
			for (int j = 0; j < initParams.length; ++j) {
				String paramName = initParams[j].getAttribute(PARAM_NAME);
				String paramValue = initParams[j].getAttribute(PARAM_VALUE);
				initparams.put(paramName, paramValue);
			}

			boolean loadOnStartup = new Boolean(filterElement.getAttribute(LOAD_ON_STARTUP)).booleanValue();
			if (loadOnStartup)
				wrapper.setLoadOnStartup();

			String httpContextId = filterElement.getAttribute(HTTPCONTEXT_ID);
			if (httpContextId == null) {
				httpContextId = filterElement.getAttribute(HTTPCONTEXT_NAME);
			}

			if (httpContextId != null && httpContextId.indexOf('.') == -1)
				httpContextId = filterElement.getNamespaceIdentifier() + "." + httpContextId; //$NON-NLS-1$

			if (httpRegistryManager.addFilterContribution(alias, wrapper, initparams, httpContextId, extension.getContributor()))
				registered.put(filterElement, wrapper);
		}
	}

	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement filterElement = elements[i];
			Filter filter = (Filter) registered.remove(filterElement);
			if (filter != null)
				httpRegistryManager.removeFilterContribution(filter);
		}
	}

	private static class FilterWrapper implements Filter {

		private IConfigurationElement element;
		private Filter delegate;
		private FilterConfig config;
		private boolean loadOnStartup = false;

		public FilterWrapper(IConfigurationElement element) {
			this.element = element;
		}

		public void setLoadOnStartup() {
			this.loadOnStartup = true;
		}

		public void init(FilterConfig filterConfig) throws ServletException {
			this.config = filterConfig;
			if (loadOnStartup)
				initializeDelegate();
		}

		public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain chain) throws ServletException, IOException {
			initializeDelegate();
			delegate.doFilter(arg0, arg1, chain);
		}

		public void destroy() {
			destroyDelegate();
		}

		private synchronized void initializeDelegate() throws ServletException {
			if (delegate == null) {
				try {
					Filter newDelegate = (Filter) element.createExecutableExtension(CLASS);
					newDelegate.init(config);
					delegate = newDelegate;
				} catch (CoreException e) {
					throw new ServletException(e);
				}
			}
		}

		private synchronized void destroyDelegate() {
			if (delegate != null) {
				Filter doomedDelegate = delegate;
				delegate = null;
				doomedDelegate.destroy();
			}
		}
	}
}
