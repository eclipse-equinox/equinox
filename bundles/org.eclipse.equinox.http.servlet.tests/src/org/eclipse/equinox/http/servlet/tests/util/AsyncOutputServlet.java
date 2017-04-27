/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AsyncOutputServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/plain");
		resp.setBufferSize(16);
		resp.flushBuffer();
		AsyncContext async = req.startAsync(req, resp);
		ServletOutputStream out = resp.getOutputStream();
		out.setWriteListener(new AsyncWriter(async, Boolean.parseBoolean(req.getParameter("bytes"))));
	}

	private class AsyncWriter implements WriteListener {

		private final AsyncContext async;

		private final boolean writeBytes;

		private boolean eof;

		public AsyncWriter(AsyncContext async, boolean writeBytes) {
			this.async = async;
			this.writeBytes = writeBytes;
		}

		@Override
		public void onWritePossible() throws IOException {
			HttpServletResponse resp = (HttpServletResponse) async.getResponse();
			ServletOutputStream out = resp.getOutputStream();
			if (eof) {
				out.close();
				async.complete();
				return;
			}

			if (writeBytes) {
				byte[] buf = new byte[10];
				for (int i = 0; i < buf.length; ++i) {
					buf[i] = (byte) ('0' + i);
				}

				do {
					out.write(buf);
				} while (out.isReady());
			} else {
				int i = -1;
				do {
					out.write('0' + (++i % 10));
				} while (out.isReady());
			}

			eof = true;
		}

		@Override
		public void onError(Throwable t) {
			try {
				async.complete();
			} finally {
				getServletContext().log("Error writing response.", t);
			}
		}
	}
}
