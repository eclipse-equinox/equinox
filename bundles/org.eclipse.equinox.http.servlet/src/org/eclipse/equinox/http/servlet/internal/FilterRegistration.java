package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

//This class wraps the servlet object registered in the HttpService.registerServlet call, to manage the context classloader when handleRequests are being asked.
public class FilterRegistration extends Registration {

	private Filter filter; //The actual filter object registered against the http service. All filter requests will eventually be delegated to it.
	private HttpContext httpContext; //The context used during the registration of the filter
	private ClassLoader registeredContextClassLoader;
	private String alias;

	public FilterRegistration(Filter filter, HttpContext context, String alias) {
		this.filter = filter;
		this.httpContext = context;
		this.alias = alias;
		registeredContextClassLoader = Thread.currentThread().getContextClassLoader();
	}

	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			super.destroy();
			filter.destroy();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	//Delegate the init call to the actual servlet
	public void init(FilterConfig filterConfig) throws ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			filter.init(filterConfig);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	//Delegate the handling of the request to the actual servlet
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			if (httpContext.handleSecurity(request, response))
				filter.doFilter(request, response, chain);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	public Filter getFilter() {
		return filter;
	}

	public HttpContext getHttpContext() {
		return httpContext;
	}

	public boolean matches(String dispatchPathInfo) {
		return dispatchPathInfo.startsWith(alias);
	}
}
