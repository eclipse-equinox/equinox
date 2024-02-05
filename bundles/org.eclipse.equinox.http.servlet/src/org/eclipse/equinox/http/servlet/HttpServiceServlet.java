/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import org.eclipse.equinox.http.servlet.internal.servlet.ProxyServlet;

/**
 * The HttpServiceServlet is the "public" side of a Servlet that when registered
 * (and init() called) in a servlet container will in-turn register and provide
 * an OSGi Http Service implementation. This class is not meant for extending or
 * even using directly and is purely meant for registering in a servlet
 * container.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class HttpServiceServlet extends ProxyServlet {
	private static final long serialVersionUID = -3647550992964861187L;
}
