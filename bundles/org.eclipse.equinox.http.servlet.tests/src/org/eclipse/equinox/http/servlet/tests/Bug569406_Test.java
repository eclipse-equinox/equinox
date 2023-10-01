package org.eclipse.equinox.http.servlet.tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Bug569406_Test extends BaseTest {

	@Test
	public void test_Bug562843_Encode_Space() throws Exception {
		final AtomicReference<String> requestURI = new AtomicReference<>();
		final AtomicReference<String> servletPath = new AtomicReference<>();
		final AtomicReference<String> pathInfo = new AtomicReference<>();
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
				requestURI.set(req.getRequestURI());
				servletPath.set(req.getServletPath());
				pathInfo.set(req.getPathInfo());
				PrintWriter writer = resp.getWriter();
				writer.write("OK");
			}
		};
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug 562843");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug 562843/this pat/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		String result = requestAdvisor.request("Bug%20562843/this%20pat/a%20b%20c/d%20e%20f");
		Assert.assertEquals("OK", result);
		Assert.assertEquals("/Bug%20562843/this%20pat/a%20b%20c/d%20e%20f", requestURI.get());
		Assert.assertEquals("/Bug 562843/this pat", servletPath.get());
		Assert.assertEquals("/a b c/d e f", pathInfo.get());
	}

	@Test
	public void test_Bug562843_Encode_Slash() throws Exception {
		final AtomicReference<String> requestURI = new AtomicReference<>();
		final AtomicReference<String> servletPath = new AtomicReference<>();
		final AtomicReference<String> pathInfo = new AtomicReference<>();
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
				requestURI.set(req.getRequestURI());
				servletPath.set(req.getServletPath());
				pathInfo.set(req.getPathInfo());
				PrintWriter writer = resp.getWriter();
				writer.write("OK");
			}
		};
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug 562843");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug 562843/this pat/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		String result = requestAdvisor.request("Bug%20562843/this%20pat/aa%2Fb%2Fc/d%2Fe%2Ff");
		Assert.assertEquals("OK", result);
		Assert.assertEquals("/Bug%20562843/this%20pat/aa%2Fb%2Fc/d%2Fe%2Ff", requestURI.get());
		Assert.assertEquals("/Bug 562843/this pat", servletPath.get());
		Assert.assertEquals("/aa/b/c/d/e/f", pathInfo.get());
	}
}
