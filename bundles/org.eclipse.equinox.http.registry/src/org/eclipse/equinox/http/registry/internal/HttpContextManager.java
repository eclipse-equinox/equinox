/*******************************************************************************
 * Copyright (c) 2005, 2007 Cognos Incorporated, IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.http.registry.internal.ExtensionPointTracker.Listener;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class HttpContextManager implements Listener {

	private static final String HTTPCONTEXTS_EXTENSION_POINT = "org.eclipse.equinox.http.registry.httpcontexts"; //$NON-NLS-1$
	private static final String HTTPCONTEXT = "httpcontext"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String PATH = "path"; //$NON-NLS-1$
	private static final String MIMEMAPPING = "mime-mapping"; //$NON-NLS-1$
	private static final String MIMEEXTENSION = "extension"; //$NON-NLS-1$
	private static final String MIMETYPE = "mime-type"; //$NON-NLS-1$
	private static final String RESOURCEMAPPING = "resource-mapping"; //$NON-NLS-1$
	private static final String BUNDLE = "bundle"; //$NON-NLS-1$

	private final List<IConfigurationElement> registered = new ArrayList<>();
	private final HttpRegistryManager httpRegistryManager;
	private final ExtensionPointTracker tracker;

	public HttpContextManager(HttpRegistryManager httpRegistryManager, IExtensionRegistry registry) {
		this.httpRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, HTTPCONTEXTS_EXTENSION_POINT, this);
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
		for (IConfigurationElement httpContextElement : elements) {
			if (!HTTPCONTEXT.equals(httpContextElement.getName())) {
				continue;
			}

			String httpContextId = httpContextElement.getAttribute(ID);
			if (httpContextId == null) {
				httpContextId = httpContextElement.getAttribute(NAME);
				if (httpContextId == null) {
					continue;
				}
			}

			if (httpContextId.indexOf('.') == -1) {
				httpContextId = httpContextElement.getNamespaceIdentifier() + "." + httpContextId; //$NON-NLS-1$
			}

			HttpContext context = null;
			String clazz = httpContextElement.getAttribute(CLASS);
			if (clazz != null) {
				try {
					context = (HttpContext) httpContextElement.createExecutableExtension(CLASS);
				} catch (CoreException e) {
					// log it.
					e.printStackTrace();
					continue;
				}
			} else {
				Bundle contributingBundle = httpRegistryManager.getBundle(extension.getContributor());
				DefaultRegistryHttpContext defaultContext = httpRegistryManager.createDefaultRegistryHttpContext();

				String oldPath = httpContextElement.getAttribute(PATH);
				if (oldPath != null) {
					defaultContext.addResourceMapping(contributingBundle, oldPath);
				}

				IConfigurationElement[] resourceMappingElements = httpContextElement.getChildren(RESOURCEMAPPING);
				for (IConfigurationElement resourceMappingElement : resourceMappingElements) {
					String path = resourceMappingElement.getAttribute(PATH);
					Bundle resourceBundle = contributingBundle;
					String bundleName = resourceMappingElement.getAttribute(BUNDLE);
					if (bundleName != null) {
						resourceBundle = httpRegistryManager.getBundle(bundleName);
						if (resourceBundle == null) {
							continue;
						}
						if (System.getSecurityManager() != null) {
							AdminPermission resourcePermission = new AdminPermission(resourceBundle, "resource"); //$NON-NLS-1$
							if (!contributingBundle.hasPermission(resourcePermission)) {
								continue;
							}
						}
					}
					defaultContext.addResourceMapping(resourceBundle, path);
				}

				IConfigurationElement[] mimeMappingElements = httpContextElement.getChildren(MIMEMAPPING);
				for (IConfigurationElement mimeMappingElement : mimeMappingElements) {
					String mimeExtension = mimeMappingElement.getAttribute(MIMEEXTENSION);
					String mimeType = mimeMappingElement.getAttribute(MIMETYPE);
					defaultContext.addMimeMapping(mimeExtension, mimeType);
				}
				context = defaultContext;
			}

			if (httpRegistryManager.addHttpContextContribution(httpContextId, context, extension.getContributor())) {
				registered.add(httpContextElement);
			}
		}
	}

	@Override
	public void removed(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement httpContextElement : elements) {
			if (!HTTPCONTEXT.equals(httpContextElement.getName())) {
				continue;
			}

			String httpContextId = httpContextElement.getAttribute(ID);
			if (httpContextId == null) {
				httpContextId = httpContextElement.getAttribute(NAME);
				if (httpContextId == null) {
					continue;
				}
			}
			if (httpContextId.indexOf('.') == -1) {
				httpContextId = httpContextElement.getNamespaceIdentifier() + "." + httpContextId; //$NON-NLS-1$
			}

			if (registered.remove(httpContextElement)) {
				httpRegistryManager.removeHttpContextContribution(httpContextId);
			}
		}
	}
}
