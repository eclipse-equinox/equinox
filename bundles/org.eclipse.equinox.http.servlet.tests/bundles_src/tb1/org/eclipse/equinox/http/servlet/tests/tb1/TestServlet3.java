/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private String getSessionCookieConfigComment() {
		ServletContext context = getServletContext();
		SessionCookieConfig cookieConfig = context.getSessionCookieConfig();  // This is a Servlet 3.0 API.
		return cookieConfig.getComment();
	}

	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		String expected = "Equinox";  //$NON-NLS-1$
		setSessionCookieConfigComment(expected);
		String actual = getSessionCookieConfigComment();
		boolean ok = actual.equals(expected);
		if (ok == false) {
			writer.print(AbstractTestServlet.STATUS_ERROR);
			return;
		}
		super.handleDoGet(request, writer);
	}
	
	private void setSessionCookieConfigComment(String comment) {
		ServletContext context = getServletContext();
		SessionCookieConfig cookieConfig = context.getSessionCookieConfig();  // This is a Servlet 3.0 API.
		cookieConfig.setComment(comment);
	}
}
