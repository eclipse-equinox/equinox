/*******************************************************************************
 * Copyright (c) 2007 Cognos Incorporated, IBM Corporation.
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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.IExtension;
import org.osgi.service.http.HttpContext;

public class NamedHttpContextImpl implements HttpContext {

	private Map httpContexts = new HashMap();
	private List snapshot = null;

	public String getMimeType(String name) {
		List contexts = getSnapshot();
		for (Iterator it = contexts.iterator(); it.hasNext();) {
			HttpContext context = (HttpContext) it.next();
			String mimeType = context.getMimeType(name);
			if (mimeType != null)
				return mimeType;
		}
		return null;
	}

	public URL getResource(String name) {
		List contexts = getSnapshot();
		for (Iterator it = contexts.iterator(); it.hasNext();) {
			HttpContext context = (HttpContext) it.next();
			URL resourceURL = context.getResource(name);
			if (resourceURL != null)
				return resourceURL;
		}
		return null;
	}

	public boolean handleSecurity(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		List contexts = getSnapshot();
		for (Iterator it = contexts.iterator(); it.hasNext();) {
			HttpContext context = (HttpContext) it.next();
			if (context.handleSecurity(req, resp) == false)
				return false;
		}
		return true;
	}

	public Set getResourcePaths(String name) {
		if (name == null || !name.startsWith("/")) //$NON-NLS-1$
			return null;

		Set result = null;
		List contexts = getSnapshot();
		for (Iterator it = contexts.iterator(); it.hasNext();) {
			HttpContext context = (HttpContext) it.next();
			try {
				Method getResourcePathsMethod = context.getClass().getMethod("getResourcePaths", new Class[] {String.class}); //$NON-NLS-1$
				if (!getResourcePathsMethod.isAccessible())
					getResourcePathsMethod.setAccessible(true);
				Set resourcePaths = (Set) getResourcePathsMethod.invoke(context, new Object[] {name});
				if (resourcePaths != null) {
					if (result == null)
						result = new HashSet();
					result.addAll(resourcePaths);
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return result;
	}

	public synchronized void addHttpContext(IExtension extension, HttpContext context) {
		httpContexts.put(extension, context);
		snapshot = null;
	}

	public synchronized void removeHttpContext(IExtension extension) {
		httpContexts.remove(extension);
		snapshot = null;
	}

	private synchronized List getSnapshot() {
		if (snapshot == null)
			snapshot = new ArrayList(httpContexts.values());

		return snapshot;
	}

}
