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

public class Bug562843_Test extends BaseTest {

	@Test
	public void test_Bug562843() throws Exception {
		final AtomicReference<String> requestURI = new AtomicReference<>();
		final AtomicReference<String> servletPath = new AtomicReference<>();
		final AtomicReference<String> pathInfo = new AtomicReference<>();
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void doGet(
				final HttpServletRequest req, final HttpServletResponse resp)
				throws IOException {
				requestURI.set(req.getRequestURI());
				servletPath.set(req.getServletPath());
				pathInfo.set(req.getPathInfo());
				PrintWriter writer = resp.getWriter();
				writer.write("OK");
			}
		};
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug 562843");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug 562843/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		String result = requestAdvisor.request("Bug%20562843/a%20b%20c");
		Assert.assertEquals("OK", result);
		Assert.assertEquals("/Bug%20562843/a%20b%20c", requestURI.get());
		Assert.assertEquals("/Bug 562843", servletPath.get());
		Assert.assertEquals("/a b c", pathInfo.get());
	}

}
