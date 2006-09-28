/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.PackageAdmin;

public class HttpContextManager {

	private static final String HTTPCONTEXTS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.httpcontexts"; //$NON-NLS-1$

	private Map contextsMap = new HashMap();

	HttpService httpService;

	ExtensionPointTracker tracker;

	PackageAdmin packageAdmin;

	public HttpContextManager(HttpService httpService, PackageAdmin packageAdmin, IExtensionRegistry registry) {
		this.httpService = httpService;
		this.packageAdmin = packageAdmin;
		tracker = new ExtensionPointTracker(registry, HTTPCONTEXTS_EXTENSION_POINT, null);
	}

	public HttpContext getDefaultHttpContext(String bundleName) {
		return new DefaultHttpContextImpl(getBundle(bundleName));
	}

	public synchronized HttpContext getHttpContext(String httpContextName) {
		HttpContext context = (HttpContext) contextsMap.get(httpContextName);
		if (context == null) {
			context = new HttpContextImpl(httpContextName);
			contextsMap.put(httpContextName, context);
		}
		return context;
	}

	public void start() {
		tracker.open();
	}

	public void stop() {
		tracker.close();
		synchronized (this) {
			contextsMap.clear();
		}
	}

	private Bundle getBundle(String symbolicName) {
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private class DefaultHttpContextImpl implements HttpContext {
		private Bundle bundle;
		private HttpContext delegate;

		public DefaultHttpContextImpl(Bundle bundle) {
			this.bundle = bundle;
			delegate = httpService.createDefaultHttpContext();
		}

		public String getMimeType(String arg0) {
			return delegate.getMimeType(arg0);
		}

		public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1) throws IOException {
			return delegate.handleSecurity(arg0, arg1);
		}

		public URL getResource(String resourceName) {
			return bundle.getEntry(resourceName);
		}

		public Set getResourcePaths(String path) {
			Enumeration entryPaths = bundle.getEntryPaths(path);
			if (entryPaths == null)
				return null;

			Set result = new HashSet();
			while (entryPaths.hasMoreElements())
				result.add(entryPaths.nextElement());
			return result;
		}
	}

	private class HttpContextImpl implements HttpContext {

		private static final String PATH = "path"; //$NON-NLS-1$

		private static final String HTTPCONTEXT = "httpcontext"; //$NON-NLS-1$

		private static final String NAME = "name"; //$NON-NLS-1$

		private HttpContext delegate;

		private String contextName;

		public HttpContextImpl(String contextName) {
			this.contextName = contextName;
			delegate = httpService.createDefaultHttpContext();
		}

		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			return delegate.handleSecurity(request, response);
		}

		public URL getResource(String resourceName) {
			IExtension[] extensions = tracker.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				if (elements.length != 1 || !HTTPCONTEXT.equals(elements[0].getName()))
					continue;

				IConfigurationElement httpContextElement = elements[0];
				String httpContextName = httpContextElement.getAttribute(NAME);
				String path = httpContextElement.getAttribute(PATH);

				if (httpContextName.equals(contextName)) {
					Bundle b = getBundle(extensions[i].getContributor().getName());
					if (path.endsWith("/")) { //$NON-NLS-1$
						path = path.substring(0, path.length() - 1);
					}

					URL url = b.getEntry(path + resourceName);
					if (url != null) {
						return url;
					}
				}
			}
			return null;
		}

		public Set getResourcePaths(String resourcePath) {
			Set result = null;
			IExtension[] extensions = tracker.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				if (elements.length != 1 || !HTTPCONTEXT.equals(elements[0].getName()))
					continue;

				IConfigurationElement httpContextElement = elements[0];
				String httpContextName = httpContextElement.getAttribute(NAME);
				String path = httpContextElement.getAttribute(PATH);

				if (httpContextName.equals(contextName)) {
					Bundle b = getBundle(extensions[i].getContributor().getName());
					if (path.endsWith("/")) { //$NON-NLS-1$
						path = path.substring(0, path.length() - 1);
					}

					Enumeration entryPaths = b.getEntryPaths(path + resourcePath);
					if (entryPaths != null) {
						if (result == null)
							result = new HashSet();
						while (entryPaths.hasMoreElements())
							result.add(entryPaths.nextElement());
					}
				}
			}
			return result;
		}

		public String getMimeType(String name) {
			return delegate.getMimeType(name);
		}
	}
}
