/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.lang.reflect.Proxy;
import javax.servlet.ServletContext;

class ServletContextProxyFactory extends Object {
	private static ClassLoader cl = ServletContextProxyFactory.class.getClassLoader();

	static ServletContext create(ServletContextAdaptor handler) {
		if (handler == null)
			throw new IllegalArgumentException("adapter must not be null"); //$NON-NLS-1$
		Class[] interfaces = new Class[] {ServletContext.class};
		return (ServletContext) Proxy.newProxyInstance(cl, interfaces, handler);
	}

}
