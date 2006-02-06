/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.*;
import org.eclipse.equinox.http.*;
import org.osgi.service.http.HttpContext;

public class ServletContextImpl implements ServletContext {
	protected Hashtable attributes = null;
	protected Http http;
	protected HttpListener listener;
	protected HttpContext httpContext;
	protected int useCount;

	public ServletContextImpl(Http http, HttpListener listener, HttpContext httpContext) {
		this.http = http;
		this.listener = listener;
		this.httpContext = httpContext;
		useCount = 0;
	}

	public int incrUseCount() {
		useCount++;

		return (useCount);
	}

	public int decrUseCount() {
		useCount--;

		return (useCount);
	}

	/**
	 * Returns the value of the named attribute of the network service,
	 * or null if the attribute does not exist.  This method allows
	 * access to additional information about the service, not already
	 * provided by the other methods in this interface. Attribute names
	 * should follow the same convention as package names.  The package
	 * names java.* and javax.* are reserved for use by Javasoft, and
	 * com.sun.* is reserved for use by Sun Microsystems.
	 *
	 * @param name the name of the attribute whose value is required
	 * @return the value of the attribute, or null if the attribute
	 * does not exist.
	 */
	public Object getAttribute(String name) {
		if (attributes != null) {
			return (attributes.get(name));
		}

		return (null);
	}

	/**
	 * Returns an enumeration of the attribute names present in this
	 * context.
	 */

	public Enumeration getAttributeNames() {
		if (attributes != null) {
			return (attributes.keys());
		}

		return ((new Vector(0).elements()));
	}

	/**
	 * Returns a <tt>ServletContext</tt> object for a particular
	 * URL path. This allows servlets to potentially gain access
	 * to the resources and to obtain <tt>RequestDispatcher</tt>
	 * objects from the target context.
	 *
	 * <p>In security concious environments, the servlet engine
	 * may always return null for any given URL path.
	 *
	 * @param uripath
	 */

	//We should be the only ServletContext out there.
	public ServletContext getContext(String uripath) {
		Registration reg = listener.getRegistration(uripath);

		if (reg == null) {
			return (null);
		}

		if (httpContext != reg.getHttpContext()) {
			return (null);
		}

		return (this);
	}

	/**
	 * Returns the major version of the servlet API that this
	 * servlet engine supports. All 2.1 compliant implementations
	 * must return the integer 2 from this method.
	 *
	 * @return 2
	 */

	public int getMajorVersion() {
		return (2);
	}

	/**
	 * Returns the mime type of the specified file, or null if not known.
	 * @param file name of the file whose mime type is required
	 */
	public String getMimeType(String file) {
		String mimeType = httpContext.getMimeType(file);
		if (mimeType != null)
			return (mimeType);
		return (http.getMimeType(file));
	}

	/**
	 * Returns the minor version of the servlet API that this
	 * servlet engine supports. All 2.1 compliant implementations
	 * must return the integer 1 from this method.
	 *
	 * @return 1
	 */

	public int getMinorVersion() {
		return (1);
	}

	/**
	 * Applies alias rules to the specified virtual path in URL path
	 * format, that is, <tt>/dir/dir/file.ext</tt>. Returns a
	 * String representing the corresponding real path in the
	 * format that is appropriate for the operating system the
	 * servlet engine is running under (including the proper path
	 * separators).
	 *
	 * <p>This method returns null if the translation could not
	 * be performed for any reason.
	 *
	 * @param path the virtual path to be translated into a real path
	 */

	public String getRealPath(String path) {
		/* We always return null because our web applications
		 * originate in bundles and there is no path on the local
		 * file system for the bundles contents.
		 */
		return (null);
	}

	/**
	 * Returns a <tt>RequestDispatcher</tt> object for the specified
	 * URL path if the context knows of an active source (such as
	 * a servlet, JSP page, CGI script, etc) of content for the
	 * particular path. This format of the URL path must be of the
	 * form <tt>/dir/dir/file.ext</tt>. The servlet engine is responsible
	 * for implementing whatever functionality is required to
	 * wrap the target source with an implementation of the
	 * <tt>RequestDispatcher</tt> interface.
	 *
	 * <p>This method will return null if the context cannot provide
	 * a dispatcher for the path provided.
	 *
	 * @param urlpath Path to use to look up the target server resource
	 * @see RequestDispatcher
	 */
	public RequestDispatcher getRequestDispatcher(String urlpath) {
		// BUGBUG need to support query string in the urlpath
		// Servlet 2.2 Section 8.1.1

		if (Http.DEBUG) {
			http.logDebug(" getRequestDispatcher: " + urlpath); //$NON-NLS-1$
		}

		Registration reg = listener.getRegistration(urlpath);

		if (reg == null) {
			return (null);
		}

		if (httpContext != reg.getHttpContext()) {
			return (null);
		}

		return (new RequestDispatcherImpl(reg, urlpath));
	}

	/**
	 * Returns a URL object of a resource that is mapped to a
	 * corresponding URL path. The URL path must be of the form
	 * <tt>/dir/dir/file.ext</tt>. This method allows a servlet
	 * to access content to be served from the servlet engines
	 * document space in a system independent manner. Resources
	 * could be located on the local file system, a remote
	 * file system, a database, or a remote network site.
	 *
	 * <p>This method may return null if there is no resource
	 * mapped to the given URL path.
	 *
	 * <p>The servlet engine must implement whatever URL handlers
	 * and <tt>URLConnection</tt> objects are necessary to access
	 * the given content.
	 *
	 * <p>This method does not fill the same purpose as the
	 * <tt>getResource</tt> method of <tt>java.lang.Class</tt>.
	 * The method in <tt>java.lang.Class</tt> looks up resources
	 * based on class loader. This method allows servlet engines
	 * to make resources avaialble to a servlet from any source
	 * without regards to class loaders, location, etc.
	 *
	 * @param path Path of the content resource
	 * @exception MalformedURLException if the resource path is
	 * not properly formed.
	 */

	public URL getResource(String path) throws MalformedURLException {
		return (httpContext.getResource(path));

	}

	/**
	 * Returns an <tt>InputStream</tt> object allowing access to
	 * a resource that is mapped to a corresponding URL path. The
	 * URL path must be of the form <tt>/dir/dir/file.ext</tt>.
	 *
	 * <p>Note that meta-information such as content length and
	 * content type that are available when using the
	 * <tt>getResource</tt> method of this class are lost when
	 * using this method.
	 *
	 * <p>This method may return null if there is no resource
	 * mapped to the given URL path.
	 * <p>The servlet engine must implement whatever URL handlers
	 * and <tt>URLConnection</tt> objects are necessary to access
	 * the given content.
	 *
	 * <p>This method does not fill the same purpose as the
	 * <tt>getResourceAsStream</tt> method of <tt>java.lang.Class</tt>.
	 * The method in <tt>java.lang.Class</tt> looks up resources
	 * based on class loader. This method allows servlet engines
	 * to make resources avaialble to a servlet from any source
	 * without regards to class loaders, location, etc.
	 *
	 * @param name
	 */

	public InputStream getResourceAsStream(String path) {
		try {
			URL url = httpContext.getResource(path);
			if (url != null)
				return url.openStream();
		} catch (IOException ex) {
			// TODO: consider logging
		}
		return null;
	}

	/**
	 * Returns the name and version of the network service under which
	 * the servlet is running. The form of this string must begin with
	 * <tt>&lt;servername&gt;/&lt;versionnumber&gt;</tt>. For example
	 * the Java Web Server could return a string of the form
	 * <tt>Java Web Server/1.1.3</tt>. Other optional information
	 * can be returned in parenthesis after the primary string. For
	 * example, <tt>Java Web Server/1.1.3 (JDK 1.1.6; Windows NT 4.0 x86)
	 * </tt>.
	 */
	public String getServerInfo() {
		return ("IBM Service Management Framework HttpService/1.0"); //$NON-NLS-1$
	}

	/**
	 * Originally defined to return a servlet from the context
	 * with the specified name. This method has been deprecated and
	 * only remains to preserve binary compatibility.
	 * This method will always return null.
	 *
	 * @deprecated This method has been deprecated for
	 * servlet lifecycle reasons. This method will be permanently
	 * removed in a future version of the Servlet API.
	 */
	public Servlet getServlet(String name) {
		return (null);
	}

	/**
	 * Originally defined to return an <tt>Enumeration</tt> of
	 * <tt>String</tt> objects containing all the servlet names
	 * known to this context.
	 * This method has been deprecated and only remains to preserve
	 * binary compatibility. This methd must always return an
	 * empty enumeration.
	 *
	 * @deprecated This method has been deprecated for
	 * servlet lifecycle reasons. This method will be permanently
	 * removed in a future version of the Servlet API.
	 */
	public Enumeration getServletNames() {
		return ((new Vector(0).elements()));
	}

	/**
	 * Originally defined to return an <tt>Enumeration</tt> of
	 * <tt>Servlet</tt> objects containing all the servlets
	 * known to this context.
	 * This method has been deprecated and only remains to preserve
	 * binary compatibility. This method must always return an empty
	 * enumeration.
	 *
	 * @deprecated This method has been deprecated for
	 * servlet lifecycle reasons. This method will be permanently
	 * removed in a future version of the Servlet API.
	 */
	public Enumeration getServlets() {
		return ((new Vector(0).elements()));
	}

	/**
	 * Logs the specified message and a stack trace of the given
	 * exception to the context's log. The
	 * name and type of the servlet log is servlet engine specific,
	 * but is normally an event log.
	 *
	 * @param exception the exception to be written
	 * @param msg the message to be written
	 *
	 * @deprecated Use log(String message, Throwable t) instead
	 */
	public void log(Exception exception, String msg) {
		log(msg, exception);
	}

	/**
	 * Logs the specified message to the context's log. The
	 * name and type of the servlet log is servlet engine specific,
	 * but is normally an event log.
	 *
	 * @param msg the message to be written
	 */
	public void log(String msg) {
		http.logInfo(msg);
	}

	/**
	 * Logs the specified message and a stack trace of the given
	 * <tt>Throwable</tt> object to the context's log. The
	 * name and type of the servlet log is servlet engine specific,
	 * but is normally an event log.
	 *
	 * @param msg the message to be written
	 * @param throwable the exception to be written
	 */

	public void log(String message, Throwable throwable) {
		http.logError(message, throwable);
	}

	/**
	 * Removes the attribute from the context that is bound to a particular
	 * name.
	 *
	 * @param name the name of the attribute to remove from the context
	 */

	public void removeAttribute(String name) {
		if (attributes != null) {
			attributes.remove(name);
		}
	}

	/**
	 * Binds an object to a given name in this context. If an object
	 * is allready bound into the context with the given name,
	 * it will be replaced.
	 *
	 * Attribute names should follow the same convention as package names.
	 * Names matching java.*, javax.*, and sun.* are reserved for
	 * definition by this specification or by the reference implementation.
	 *
	 * @param name the name of the attribute to store
	 * @param value the value of the attribute
	 */

	public void setAttribute(String name, Object object) {
		if (attributes == null) {
			synchronized (this) {
				if (attributes == null) {
					attributes = new Hashtable(31);
				}
			}
		}

		attributes.put(name, object);
	}

	/**
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 */
	public String getInitParameter(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 */
	public Enumeration getInitParameterNames() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletContext#getNamedDispatcher(String)
	 */
	public RequestDispatcher getNamedDispatcher(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletContext#getResourcePaths(String)
	 */
	public Set getResourcePaths(String path) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletContext#getServletContextName()
	 */
	public String getServletContextName() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

}
