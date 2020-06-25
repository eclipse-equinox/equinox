/*******************************************************************************
 * Copyright (c) 2014, 2016 Raymond Augé and others.
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
	public void sendError(int theStatus) {
		this.status = theStatus;
	}

	@Override
	public void sendError(int theStatus, String theMessage) {
		this.status = theStatus;
		this.message = theMessage;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int getStatus() {
		if (status == -1) {
			return super.getStatus();
		}
		return status;
	}

	public int getInternalStatus() {
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

	@Override
	public void flushBuffer() throws IOException {
		if (status != -1) {
			HttpServletResponse wrappedResponse = (HttpServletResponse)this.getResponse();
			wrappedResponse.sendError(status, getMessage());
		}
		super.flushBuffer();
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
		public void close() throws IOException {
			originalOutputStream.close();
		}

		@Override
		public void flush() throws IOException {
			if (getInternalStatus() != -1) {
				HttpServletResponse wrappedResponse = (HttpServletResponse) HttpServletResponseWrapperImpl.this.getResponse();
				wrappedResponse.sendError(getInternalStatus(), getMessage());
			}
			originalOutputStream.flush();
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

		@Override
		public void write(byte[] b) throws IOException {
			if (isCompleted()) {
				return;
			}
			originalOutputStream.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (isCompleted()) {
				return;
			}
			originalOutputStream.write(b, off, len);
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