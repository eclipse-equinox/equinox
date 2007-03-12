/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Cognos Incorporated - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.osgi.service.http.*;

public class ResourceManager implements ExtensionPointTracker.Listener {

	private static final String RESOURCES_EXTENSION_POINT = "org.eclipse.equinox.http.registry.resources"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_NAME = "httpcontext-name"; //$NON-NLS-1$

	private static final String BASE_NAME = "base-name"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String RESOURCE = "resource"; //$NON-NLS-1$

	private HttpService httpService;

	private ExtensionPointTracker tracker;

	private HttpContextManager httpContextManager;

	private List registered = new ArrayList();

	public ResourceManager(HttpService httpService, HttpContextManager httpContextManager, IExtensionRegistry registry) {
		this.httpService = httpService;
		this.httpContextManager = httpContextManager;
		tracker = new ExtensionPointTracker(registry, RESOURCES_EXTENSION_POINT, this);
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
			IConfigurationElement resourceElement = elements[i];
			if (!RESOURCE.equals(resourceElement.getName()))
				continue;

			String alias = resourceElement.getAttribute(ALIAS);
			String baseName = resourceElement.getAttribute(BASE_NAME);
			if (baseName == null)
				baseName = ""; //$NON-NLS-1$

			String httpContextName = resourceElement.getAttribute(HTTPCONTEXT_NAME);
			HttpContext context = null;
			if (httpContextName == null)
				context = httpContextManager.getDefaultHttpContext(extension.getContributor().getName());
			else
				context = httpContextManager.getHttpContext(httpContextName);

			try {
				httpService.registerResources(alias, baseName, context);
				registered.add(resourceElement);
			} catch (NamespaceException e) {
				// TODO Should log this perhaps with the LogService?
				e.printStackTrace();
			}
		}
	}

	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement resourceElement = elements[i];
			if (!RESOURCE.equals(resourceElement.getName()))
				continue;
			String alias = resourceElement.getAttribute(ALIAS);
			if (registered.remove(resourceElement))
				httpService.unregister(alias);
		}
	}
}
