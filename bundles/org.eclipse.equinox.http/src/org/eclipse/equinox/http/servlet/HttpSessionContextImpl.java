/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.util.Enumeration;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * A HttpSessionContext is a grouping of HttpSessions associated with a single
 * entity. This interface gives servlets access to
 * methods for listing the IDs and for retrieving a session based on its ID.
 *
 * <p>Servlets get the HttpSessionContext object by calling the
 * getSessionContext()
 * method of HttpSession.
 *
 * @see HttpSession
 * @depracated
 */
public class HttpSessionContextImpl implements HttpSessionContext {

	public HttpSessionContextImpl() {
	}

	/**
	 * Returns an enumeration of all of the session IDs in this context.
	 *
	 * @return an enumeration of all session IDs in this context
	 * @deprecated
	 */
	public Enumeration getIds() {
		return ((new Vector(0)).elements());
	}

	/**
	 * Returns the session bound to the specified session ID.
	 *
	 * @param sessionID the ID of a particular session object
	 * @return the session name. Returns null if the session ID does not refer
	 * to a valid session.
	 * @depracated
	 */
	public HttpSession getSession(String sessionId) {
		return (null);
	}
}
