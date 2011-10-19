package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.*;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import org.osgi.service.http.HttpContext;

public class ServletContextAdaptor implements InvocationHandler {

	private final static Map contextToHandlerMethods;
	static {
		Map methods = new HashMap();
		Class servletContextClazz = ServletContext.class;
		Class handlerClazz = ServletContextAdaptor.class;
		Method[] handlerMethods = handlerClazz.getDeclaredMethods();
		for (int i = 0; i < handlerMethods.length; i++) {
			try {
				Method m = servletContextClazz.getMethod(handlerMethods[i].getName(), handlerMethods[i].getParameterTypes());
				methods.put(m, handlerMethods[i]);
			} catch (NoSuchMethodException e) {
				// do nothing
			}
		}
		contextToHandlerMethods = methods;
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

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Method m = (Method) contextToHandlerMethods.get(method);
		if (m != null) {
			return m.invoke(this, args);
		}
		return method.invoke(servletContext, args);
	}
}
