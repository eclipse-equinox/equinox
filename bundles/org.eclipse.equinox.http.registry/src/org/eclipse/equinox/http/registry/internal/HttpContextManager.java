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
import org.eclipse.equinox.http.registry.internal.ExtensionPointTracker.Listener;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.PackageAdmin;

public class HttpContextManager implements Listener {

	private static final String HTTPCONTEXTS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.httpcontexts"; //$NON-NLS-1$
	private static final String HTTPCONTEXT = "httpcontext"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String PATH = "path"; //$NON-NLS-1$

	private Map contextsMap = new HashMap();
	private PackageAdmin packageAdmin;
	HttpService httpService;
	private ExtensionPointTracker tracker;

	public HttpContextManager(HttpService httpService, PackageAdmin packageAdmin, IExtensionRegistry registry) {
		this.httpService = httpService;
		this.packageAdmin = packageAdmin;
		tracker = new ExtensionPointTracker(registry, HTTPCONTEXTS_EXTENSION_POINT, this);
	}

	public HttpContext getDefaultHttpContext(String bundleName) {
		return new DefaultHttpContextImpl(getBundle(bundleName));
	}

	public synchronized HttpContext getHttpContext(String httpContextName) {
		HttpContext context = (HttpContext) contextsMap.get(httpContextName);
		if (context == null) {
			context = new NamedHttpContextImpl();
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
	
	public void added(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length != 1 || !HTTPCONTEXT.equals(elements[0].getName()))
			return;

		IConfigurationElement httpContextElement = elements[0];
		String httpContextName = httpContextElement.getAttribute(NAME);
		if (httpContextName == null)
			return;

		HttpContext context = null;
		String clazz = httpContextElement.getAttribute(CLASS);
		if (clazz != null) {
			try {
				context = (HttpContext) httpContextElement.createExecutableExtension(CLASS);
			} catch (CoreException e) {
				// log it.
				e.printStackTrace();
			}
		} else {
			Bundle b = getBundle(extension.getContributor().getName());
			String path = httpContextElement.getAttribute(PATH);
			context = new DefaultHttpContextImpl(b, path);
		}

		NamedHttpContextImpl namedContext = (NamedHttpContextImpl) getHttpContext(httpContextName);
		namedContext.addHttpContext(extension, context);
	}

	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length != 1 || !HTTPCONTEXT.equals(elements[0].getName()))
			return;

		IConfigurationElement httpContextElement = elements[0];
		String httpContextName = httpContextElement.getAttribute(NAME);
		if (httpContextName == null)
			return;
		
		NamedHttpContextImpl namedContext = (NamedHttpContextImpl) getHttpContext(httpContextName);
		namedContext.removeHttpContext(extension);
	}

	private class DefaultHttpContextImpl implements HttpContext {
		private Bundle bundle;
		private HttpContext delegate;
		private String bundlePath;

		public DefaultHttpContextImpl(Bundle bundle) {
			this.bundle = bundle;
			delegate = httpService.createDefaultHttpContext();
		}

		public DefaultHttpContextImpl(Bundle b, String bundlePath) {
			this(b);
			if (bundlePath != null) {
				if (bundlePath.endsWith("/")) //$NON-NLS-1$
					bundlePath = bundlePath.substring(0, bundlePath.length() - 1);

				if (bundlePath.length() == 0)
					bundlePath = null;
			}
			this.bundlePath = bundlePath;
		}

		public String getMimeType(String arg0) {
			return delegate.getMimeType(arg0);
		}

		public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1) throws IOException {
			return delegate.handleSecurity(arg0, arg1);
		}
		
		public URL getResource(String resourceName) {
			Enumeration entryPaths;
			if (bundlePath == null)
				entryPaths = bundle.findEntries(resourceName, null, false);
			else
				entryPaths = bundle.findEntries(bundlePath + resourceName, null, false);
			
			if (entryPaths != null && entryPaths.hasMoreElements())
				return (URL) entryPaths.nextElement();
			
			return null;
		}

		public Set getResourcePaths(String path) {
			Enumeration entryPaths;
			if (bundlePath == null)
				entryPaths = bundle.findEntries(path, null, false);
			else
				entryPaths = bundle.findEntries(path, null, false);
			
			if (entryPaths == null)
				return null;

			Set result = new HashSet();
			while (entryPaths.hasMoreElements()) {
				URL entryURL = (URL) entryPaths.nextElement();
				String entryPath = entryURL.getFile();

				if (bundlePath == null)	
					result.add(entryPath);
				else
					result.add(entryPath.substring(bundlePath.length()));
			}
			return result;
		}
	}
}
