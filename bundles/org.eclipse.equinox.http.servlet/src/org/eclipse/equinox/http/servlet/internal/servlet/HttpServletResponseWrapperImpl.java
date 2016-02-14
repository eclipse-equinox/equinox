/*******************************************************************************
 * Copyright (c) 2014, 2016 Raymond Augé and others.
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
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
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
	public void sendError(int status) {
		this.status = status;
	}

	@Override
	public void sendError(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (outputStream == null) {
			outputStream = new InternalOutputStream(super.getOutputStream());
		}
		return outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			writer = new InternalWriter(super.getWriter());
		}
		return writer;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public static HttpServletResponseWrapperImpl findHttpRuntimeResponse(
		HttpServletResponse response) {

		while (response instanceof HttpServletResponseWrapper) {
			if (response instanceof HttpServletResponseWrapperImpl) {
				return (HttpServletResponseWrapperImpl)response;
			}

			response = (HttpServletResponse)((HttpServletResponseWrapper)response).getResponse();
		}

		return null;
	}

	private int status = -1;
	private String message;
	private boolean completed;
	private InternalOutputStream outputStream;
	private InternalWriter writer;

	private class InternalOutputStream extends ServletOutputStream {

		public InternalOutputStream(ServletOutputStream originalOutputStream) {
			this.originalOutputStream = originalOutputStream;
		}

		@Override
		public boolean isReady() {
			return originalOutputStream.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			originalOutputStream.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			if (isCompleted()) {
				return;
			}
			originalOutputStream.write(b);
		}

		private final ServletOutputStream originalOutputStream;

	}

	private class InternalWriter extends PrintWriter {

		public InternalWriter(PrintWriter originalWriter) {
			super(originalWriter);
		}

		@Override
		public PrintWriter format(Locale l, String format, Object... args) {
			if (!isCompleted()) {
				super.format(l, format, args);
			}
			return this;
		}

		@Override
		public PrintWriter format(String format, Object... args) {
			if (!isCompleted()) {
				super.format(format, args);
			}
			return this;
		}

		@Override
		public void println() {
			if (isCompleted()) {
				return;
			}
			super.println();
		}

		@Override
		public void write(int c) {
			if (isCompleted()) {
				return;
			}
			super.write(c);
		}

		@Override
		public void write(char[] buf, int off, int len) {
			if (isCompleted()) {
				return;
			}
			super.write(buf, off, len);
		}

		@Override
		public void write(char[] buf) {
			if (isCompleted()) {
				return;
			}
			super.write(buf);
		}

		@Override
		public void write(String s, int off, int len) {
			if (isCompleted()) {
				return;
			}
			super.write(s, off, len);
		}

		@Override
		public void write(String s) {
			if (isCompleted()) {
				return;
			}
			super.write(s);
		}

	}

}