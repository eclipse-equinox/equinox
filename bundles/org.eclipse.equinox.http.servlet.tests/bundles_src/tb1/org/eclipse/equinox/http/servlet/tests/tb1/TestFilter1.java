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
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseFilter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestFilter1 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		ExtendedHttpService service = (ExtendedHttpService)getHttpService();
		service.registerServlet(regexAlias(), this, null, null);
		service.registerFilter(regexAlias(), f1, new Hashtable<String, String>(), null);
	}

	@Override
	public void deactivate() {
		ExtendedHttpService service = (ExtendedHttpService)getHttpService();
		service.unregister(regexAlias());
		service.unregisterFilter(f1);
	}

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		writer.print('a');
	}

	private Filter f1 = new BaseFilter('b');
}
