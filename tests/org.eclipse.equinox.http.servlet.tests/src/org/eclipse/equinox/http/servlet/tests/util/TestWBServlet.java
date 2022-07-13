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
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestWBServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String STATUS_PARAM = "status";
	
	private final AtomicReference<String> status = new AtomicReference<>("none");
	private final AtomicBoolean destroyed = new AtomicBoolean(false);

	@Override
	public void init() {
		status.set(getServletConfig().getInitParameter(STATUS_PARAM));
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try (PrintWriter writer = response.getWriter()) {
			handleDoGet(request, writer);
		}
	}

	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		if (destroyed.get()) {
			writer.print("destroyed");
		} else {
			writer.print(status.get());
		}
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	public boolean isDestroyed() {
		return destroyed.get();
	}
}
