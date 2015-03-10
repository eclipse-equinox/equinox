/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author Raymond Augé
 */
public class HttpServletResponseWrapperImpl extends HttpServletResponseWrapper {

	public HttpServletResponseWrapperImpl(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void flushBuffer() throws IOException {
		if (this.status > -1) {
			return;
		}

		super.flushBuffer();
	}

	@Override
	public void sendError(int status) {
		this.status = status;
	}

	@Override
	public void sendError(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public void setStatus(int status, String message) {
		sendError(status, message);
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int getStatus() {
		return status;
	}

	private int status = -1;
	private String message;

}