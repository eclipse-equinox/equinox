package org.eclipse.equinox.http.servlet;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import org.eclipse.equinox.http.servlet.internal.ProxyServlet;

/**
 * The HttpServiceServlet is the "public" side of a Servlet that when registered (and init() called) in a servlet container
 * will in-turn register and provide an OSGi Http Service implementation.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class HttpServiceServlet extends HttpServlet {

	private static final long serialVersionUID = -8247735945454143446L;

	private ProxyServlet delegate;

	public HttpServiceServlet() {
		delegate = new ProxyServlet();
	}

	public void destroy() {
		delegate.destroy();
	}

	public String getInitParameter(String name) {
		return delegate.getInitParameter(name);
	}

	public Enumeration getInitParameterNames() {
		return delegate.getInitParameterNames();
	}

	public ServletConfig getServletConfig() {
		return delegate.getServletConfig();
	}

	public ServletContext getServletContext() {
		return delegate.getServletContext();
	}

	public String getServletInfo() {
		return delegate.getServletInfo();
	}

	public String getServletName() {
		return delegate.getServletName();
	}

	public void init() throws ServletException {
		delegate.init();
	}

	public void init(ServletConfig config) throws ServletException {
		delegate.init(config);
	}

	public void log(String message, Throwable t) {
		delegate.log(message, t);
	}

	public void log(String msg) {
		delegate.log(msg);
	}

	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		delegate.service(req, res);
	}

}
