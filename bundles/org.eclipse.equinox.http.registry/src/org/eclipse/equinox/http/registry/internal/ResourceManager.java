/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated. 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Cognos Incorporated - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;

public class ResourceManager implements ExtensionPointTracker.Listener {

	private static final String RESOURCES_EXTENSION_POINT = "org.eclipse.equinox.http.registry.resources"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_NAME = "httpcontext-name"; //$NON-NLS-1$

	private static final String BASE_NAME = "base-name"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String RESOURCE = "resource"; //$NON-NLS-1$

	private static final String HTTPCONTEXT_ID = "httpcontextId"; //$NON-NLS-1$

	private ExtensionPointTracker tracker;

	private List registered = new ArrayList();

	private HttpRegistryManager httpRegistryManager;

	public ResourceManager(HttpRegistryManager httpRegistryManager, IExtensionRegistry registry) {
		this.httpRegistryManager = httpRegistryManager;
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
			if (alias == null)
				continue; // alias is mandatory - ignore this.

			String baseName = resourceElement.getAttribute(BASE_NAME);
			if (baseName == null)
				baseName = ""; //$NON-NLS-1$

			String httpContextId = resourceElement.getAttribute(HTTPCONTEXT_ID);
			if (httpContextId == null) {
				httpContextId = resourceElement.getAttribute(HTTPCONTEXT_NAME);
			}

			if (httpContextId != null && httpContextId.indexOf('.') == -1)
				httpContextId = resourceElement.getNamespaceIdentifier() + "." + httpContextId; //$NON-NLS-1$

			if (httpRegistryManager.addResourcesContribution(alias, baseName, httpContextId, extension.getContributor()))
				registered.add(resourceElement);
		}
	}

	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement resourceElement = elements[i];
			if (registered.remove(resourceElement)) {
				String alias = resourceElement.getAttribute(ALIAS);
				httpRegistryManager.removeContribution(alias);
			}
		}
	}
}
