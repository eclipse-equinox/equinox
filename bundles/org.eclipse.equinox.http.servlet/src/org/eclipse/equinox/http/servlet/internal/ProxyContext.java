package org.eclipse.equinox.http.servlet.internal;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.osgi.service.http.HttpContext;

/**
 * The ProxyContext provides something similar to a ServletContext for all servlets and resources under a particular ProxyServlet.
 * In particular it holds and represent the concept of "context path" through the Proxy Servlets servlet path.
 * The Http Service also requires a ServletContext namespaced by eachindividual HttpContext. The ProxyContext provides support for the
 * attribute map of a ServletContext again namespaced by HttpContext as specified in the Http Service specification. A WeakHashMap is
 * used to hold the various attributes so that when the HttpContext is no longer referenced the associated context attributes can be
 * garbage collected.
 */
public class ProxyContext {
	private String servletPath;
	private WeakHashMap contextAttributes = new WeakHashMap();

	synchronized void initializeServletPath(HttpServletRequest req) {
		if (servletPath == null)
			servletPath = HttpServletRequestAdaptor.getDispatchServletPath(req);
	}

	synchronized String getServletPath() {
		return servletPath;
	}

	synchronized void createContextAttributes(HttpContext httpContext) {
		if (!contextAttributes.containsKey(httpContext))
			contextAttributes.put(httpContext, new Hashtable());
	}

	synchronized Dictionary getContextAttributes(HttpContext httpContext) {
		return (Dictionary) contextAttributes.get(httpContext);
	}
}
