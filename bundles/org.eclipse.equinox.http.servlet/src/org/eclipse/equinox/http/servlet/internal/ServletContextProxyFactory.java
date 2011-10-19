/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.lang.reflect.*;
import javax.servlet.ServletContext;

class ServletContextProxyFactory extends Object {
	private static final String PROXY_METHOD_TRACING_PROPERTY = "org.eclipse.equinox.http.servlet.internal.proxy.method.tracing"; //$NON-NLS-1$
	private static final boolean PROXY_METHOD_TRACING = Boolean.getBoolean(ServletContextProxyFactory.PROXY_METHOD_TRACING_PROPERTY);

	private MethodAdvisor methodAdvisor;

	static ServletContext create(ServletContextAdaptor adapter) {
		ServletContextProxyFactory factory = new ServletContextProxyFactory();
		return factory.createServletContext(adapter);
	}

	private ServletContextProxyFactory() {
		super();
		this.methodAdvisor = new MethodAdvisor(ServletContextAdaptor.class);
	}

	private ServletContext createServletContext(ServletContextAdaptor adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("adapter must not be null"); //$NON-NLS-1$
		ClassLoader loader = ServletContextAdaptor.class.getClassLoader();
		InvocationHandler handler = createServletContextInvocationHandler(adapter);
		Class[] interfaces = new Class[] {ServletContext.class};
		return (ServletContext) Proxy.newProxyInstance(loader, interfaces, handler);
	}

	private InvocationHandler createServletContextInvocationHandler(final ServletContextAdaptor adapter) {
		return new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return ServletContextProxyFactory.this.invoke(adapter, proxy, method, args);
			}
		};
	}

	private Object invoke(ServletContextAdaptor adapter, Object proxy, Method method, Object[] args) throws Throwable {
		if (ServletContextProxyFactory.PROXY_METHOD_TRACING) {
			System.out.println("TRACE-invoking: " + method.getName()); //$NON-NLS-1$
		}
		boolean match = methodAdvisor.isImplemented(method);
		Object object = match ? adapter : adapter.getSubject();
		return method.invoke(object, args);
	}
}
