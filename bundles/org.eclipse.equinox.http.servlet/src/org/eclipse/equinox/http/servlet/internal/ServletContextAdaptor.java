/*******************************************************************************
 * Copyright (c) 2005, 2011 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import org.osgi.service.http.HttpContext;

public class ServletContextAdaptor {
	private final static Map contextToHandlerMethods;
	static {
		contextToHandlerMethods = createContextToHandlerMethods();
	}

	private static Map createContextToHandlerMethods() {
		Map methods = new HashMap();
		Method[] handlerMethods = ServletContextAdaptor.class.getDeclaredMethods();
		for (int i = 0; i < handlerMethods.length; i++) {
			Method handlerMethod = handlerMethods[i];
			String name = handlerMethod.getName();
			Class[] parameterTypes = handlerMethod.getParameterTypes();
			try {
				Method method = ServletContext.class.getMethod(name, parameterTypes);
				methods.put(method, handlerMethod);
			} catch (NoSuchMethodException e) {
				// do nothing
			}
		}
		return methods;
	}

	final private ServletContext servletContext;
	final HttpContext httpContext;
	final private AccessControlContext acc;
	final private ProxyContext proxyContext;

	public ServletContextAdaptor(ProxyContext proxyContext, ServletContext servletContext, HttpContext httpContext, AccessControlContext acc) {
		this.servletContext = servletContext;
		this.httpContext = httpContext;
		this.acc = acc;
		this.proxyContext = proxyContext;
	}

	public ServletContext createServletContext() {
		Class clazz = getClass();
		ClassLoader classLoader = clazz.getClassLoader();
		Class[] interfaces = new Class[] {ServletContext.class};
		InvocationHandler invocationHandler = createInvocationHandler();
		return (ServletContext) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
	}

	private InvocationHandler createInvocationHandler() {
		return new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return ServletContextAdaptor.this.invoke(proxy, method, args);
			}
		};
	}

	Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Method m = (Method) contextToHandlerMethods.get(method);
		if (m != null) {
			return m.invoke(this, args);
		}
		return method.invoke(servletContext, args);
	}

	/**
	 * 	@see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
	 * 
	 * This method was added in the Servlet 2.3 API however the OSGi HttpService currently does not provide
	 * support for this method in the HttpContext interface. To support "getResourcePaths(...) this
	 * implementation uses reflection to check for and then call the associated HttpContext.getResourcePaths(...)
	 * method opportunistically. Null is returned if the method is not present or fails.
	 */
	public Set getResourcePaths(String name) {
		if (name == null || !name.startsWith("/")) //$NON-NLS-1$
			return null;
		try {
			Method getResourcePathsMethod = httpContext.getClass().getMethod("getResourcePaths", new Class[] {String.class}); //$NON-NLS-1$
			if (!getResourcePathsMethod.isAccessible())
				getResourcePathsMethod.setAccessible(true);
			return (Set) getResourcePathsMethod.invoke(httpContext, new Object[] {name});
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public Object getAttribute(String attributeName) {
		Dictionary attributes = proxyContext.getContextAttributes(httpContext);
		return attributes.get(attributeName);
	}

	public Enumeration getAttributeNames() {
		Dictionary attributes = proxyContext.getContextAttributes(httpContext);
		return attributes.keys();
	}

	public void setAttribute(String attributeName, Object attributeValue) {
		Dictionary attributes = proxyContext.getContextAttributes(httpContext);
		attributes.put(attributeName, attributeValue);
	}

	public void removeAttribute(String attributeName) {
		Dictionary attributes = proxyContext.getContextAttributes(httpContext);
		attributes.remove(attributeName);
	}

	public String getMimeType(String name) {
		String mimeType = httpContext.getMimeType(name);
		return (mimeType != null) ? mimeType : servletContext.getMimeType(name);
	}

	public URL getResource(final String name) {
		try {
			return (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return httpContext.getResource(name);
				}
			}, acc);
		} catch (PrivilegedActionException e) {
			servletContext.log(e.getException().getMessage(), e.getException());
		}
		return null;
	}

	public InputStream getResourceAsStream(String name) {
		URL url = getResource(name);
		if (url != null) {
			try {
				return url.openStream();
			} catch (IOException e) {
				servletContext.log("Error opening stream for resource '" + name + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		return new RequestDispatcherAdaptor(servletContext.getNamedDispatcher(arg0));
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return new RequestDispatcherAdaptor(servletContext.getRequestDispatcher(proxyContext.getServletPath() + arg0));
	}
}
