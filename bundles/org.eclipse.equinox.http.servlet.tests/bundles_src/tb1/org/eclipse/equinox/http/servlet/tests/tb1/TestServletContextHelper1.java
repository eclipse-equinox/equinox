package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.context.ServletContextHelper;

public class TestServletContextHelper1 extends ServletContextHelper {

	public void activate(ComponentContext componentContext) {
		delegate = new ServletContextHelper(componentContext.getBundleContext().getBundle()) {};
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return delegate.handleSecurity(request, response);
	}

	public URL getResource(String name) {
		return delegate.getResource(name);
	}

	public String toString() {
		return delegate.toString();
	}

	public String getMimeType(String name) {
		return delegate.getMimeType(name);
	}

	public Set<String> getResourcePaths(String path) {
		return delegate.getResourcePaths(path);
	}

	public String getRealPath(String path) {
		return delegate.getRealPath(path);
	}

	private ServletContextHelper delegate;

}