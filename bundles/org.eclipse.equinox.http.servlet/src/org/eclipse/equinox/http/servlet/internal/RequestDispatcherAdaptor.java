package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import javax.servlet.*;

//This class unwraps the request so it can be processed by the underlying servlet container. 
public class RequestDispatcherAdaptor implements RequestDispatcher {

	private RequestDispatcher requestDispatcher;

	public RequestDispatcherAdaptor(RequestDispatcher requestDispatcher) {
		this.requestDispatcher = requestDispatcher;
	}

	public void forward(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
		if (req instanceof HttpServletRequestAdaptor)
			req = ((HttpServletRequestAdaptor) req).getRequest();

		requestDispatcher.forward(req, resp);
	}

	public void include(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
		if (req instanceof HttpServletRequestAdaptor)
			req = ((HttpServletRequestAdaptor) req).getRequest();

		requestDispatcher.include(req, resp);
	}
}
