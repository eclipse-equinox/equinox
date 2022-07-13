/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestServletContext1 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		ServletContext servletContext = request.getServletContext();
		Set<String> resourcePaths = servletContext.getResourcePaths("/org/eclipse/equinox/http/servlet/tests/tb1");

		for (String resourcePath : resourcePaths) {
			if (resourcePath.endsWith("/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt")) {
				writer.print(resourcePath);
			}
		}
	}

}
