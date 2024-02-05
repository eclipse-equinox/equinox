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

package org.eclipse.equinox.http.servlet.tests.wb.t2;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractWhiteboardTestServlet;

/**
 * @author Raymond Augé
 */
public class WBServlet2 extends AbstractWhiteboardTestServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		writer.print('a');
	}

}
