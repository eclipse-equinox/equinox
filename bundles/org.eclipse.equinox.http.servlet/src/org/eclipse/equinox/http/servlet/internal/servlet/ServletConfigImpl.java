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
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class ServletConfigImpl implements ServletConfig {

	private final Map<String, String> initparams;
	private final ServletContext servletContext;
	private final String servletName;

	public ServletConfigImpl(String servletName, Map<String, String> initparams, ServletContext servletContext) {

		this.servletName = servletName;

		if (initparams != null) {
			this.initparams = initparams;
		} else {
			this.initparams = Collections.emptyMap();
		}

		this.servletContext = servletContext;
	}

	@Override
	public String getServletName() {
		return servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public String getInitParameter(String name) {
		return initparams.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initparams.keySet());
	}
}
