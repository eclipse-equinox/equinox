package org.eclipse.equinox.http.servlet;

import org.eclipse.equinox.http.servlet.internal.ProxyServlet;

/**
 * The HttpServiceServlet is the "public" side of a Servlet that when registered (and init() called) in a servlet container
 * will in-turn register and provide an OSGi Http Service implementation.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class HttpServiceServlet extends ProxyServlet {
	private static final long serialVersionUID = -3647550992964861187L; 
}
