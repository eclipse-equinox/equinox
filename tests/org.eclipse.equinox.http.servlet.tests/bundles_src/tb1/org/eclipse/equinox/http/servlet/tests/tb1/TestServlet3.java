/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet3_component.xml.
 */
public class TestServlet3 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		String expected = "Equinox";  //$NON-NLS-1$
		try {
			// Not a terribly useful test, but previous test assumed we could call setComment on a SessionCookieConfig
			// after ServletContext initialization.  This is not allowed by the servlet specification.
			// Just verifying that an ISE is thrown now.
			setSessionCookieConfigComment(expected);
			writer.print(AbstractTestServlet.STATUS_ERROR);
		} catch (IllegalStateException e) {
			super.handleDoGet(request, writer);
		}
	}
	
	private void setSessionCookieConfigComment(String comment) {
		ServletContext context = getServletContext();
		SessionCookieConfig cookieConfig = context.getSessionCookieConfig();  // This is a Servlet 3.0 API.
		cookieConfig.setComment(comment);
	}
}
