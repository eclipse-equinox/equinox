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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.osgi.framework.Constants;
import org.osgi.service.http.runtime.HttpServiceRuntime;

/**
 * @author Raymond Augé
 */
public class TestServlet5 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private Map<String, Object> properties;

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		writer.print(properties.get(Constants.SERVICE_DESCRIPTION));
	}

	public void setHttpServiceRuntime(HttpServiceRuntime httpServiceRuntime, Map<String, Object> properties) {
		this.properties = properties;
	}

}
