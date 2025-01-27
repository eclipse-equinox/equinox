/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.registry.internal;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultRegistryHttpContext implements HttpContext {
	private HttpContext delegate;
	private List<ResourceMapping> resourceMappings;
	private Properties mimeMappings;

	public DefaultRegistryHttpContext(HttpContext delegate) {
		this.delegate = delegate;
	}

	public void addResourceMapping(Bundle contributingBundle, String path) {
		if (resourceMappings == null)
			resourceMappings = new ArrayList<>();

		resourceMappings.add(new ResourceMapping(contributingBundle, path));
	}

	public void addMimeMapping(String mimeExtension, String mimeType) {
		if (mimeMappings == null)
			mimeMappings = new Properties();

		mimeMappings.put(mimeExtension, mimeType);
	}

	@Override
	public String getMimeType(String name) {
		if (mimeMappings != null) {
			int dotIndex = name.lastIndexOf('.');
			if (dotIndex != -1) {
				String mimeExtension = name.substring(dotIndex + 1);
				String mimeType = mimeMappings.getProperty(mimeExtension);
				if (mimeType != null)
					return mimeType;
			}
		}
		return delegate.getMimeType(name);
	}

	@Override
	public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1) throws IOException {
		return delegate.handleSecurity(arg0, arg1);
	}

	@Override
	public URL getResource(String name) {
		if (resourceMappings == null)
			return null;

		for (ResourceMapping mapping : resourceMappings) {
			URL resourceURL = mapping.getResource(name);
			if (resourceURL != null)
				return resourceURL;
		}
		return null;
	}

	public Set<String> getResourcePaths(String path) {
		if (resourceMappings == null || path == null || !path.startsWith("/")) //$NON-NLS-1$
			return null;

		Set<String> result = null;
		for (ResourceMapping mapping : resourceMappings) {
			Set<String> resourcePaths = mapping.getResourcePaths(path);
			if (resourcePaths != null) {
				if (result == null)
					result = new HashSet<>();
				result.addAll(resourcePaths);
			}
		}
		return result;
	}

	public static class ResourceMapping {
		private Bundle bundle;
		private String bundlePath;

		public ResourceMapping(Bundle bundle, String path) {
			this.bundle = bundle;
			if (path != null) {
				if (path.endsWith("/")) //$NON-NLS-1$
					path = path.substring(0, path.length() - 1);

				if (path.length() == 0)
					path = null;
			}
			this.bundlePath = path;
		}

		public URL getResource(String resourceName) {
			if (bundlePath != null)
				resourceName = bundlePath + resourceName;

			int lastSlash = resourceName.lastIndexOf('/');
			if (lastSlash == -1)
				return null;

			String path = resourceName.substring(0, lastSlash);
			if (path.length() == 0)
				path = "/"; //$NON-NLS-1$
			String file = sanitizeEntryName(resourceName.substring(lastSlash + 1));
			Enumeration<URL> entryPaths = bundle.findEntries(path, file, false);

			if (entryPaths != null && entryPaths.hasMoreElements())
				return entryPaths.nextElement();

			return null;
		}

		private String sanitizeEntryName(String name) {
			StringBuffer buffer = null;
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				switch (c) {
				case '*':
				case '\\':
					// we need to escape '*' and '\'
					if (buffer == null) {
						buffer = new StringBuffer(name.length() + 16);
						buffer.append(name.substring(0, i));
					}
					buffer.append('\\').append(c);
					break;
				default:
					if (buffer != null)
						buffer.append(c);
					break;
				}
			}
			return (buffer == null) ? name : buffer.toString();
		}

		public Set<String> getResourcePaths(String path) {
			if (bundlePath != null)
				path = bundlePath + path;

			Enumeration<URL> entryPaths = bundle.findEntries(path, null, false);
			if (entryPaths == null)
				return null;

			Set<String> result = new HashSet<>();
			while (entryPaths.hasMoreElements()) {
				URL entryURL = entryPaths.nextElement();
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
