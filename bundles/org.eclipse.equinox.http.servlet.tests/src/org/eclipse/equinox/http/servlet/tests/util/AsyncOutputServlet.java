/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AsyncOutputServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/plain");
		resp.setBufferSize(16);
		resp.flushBuffer();
		AsyncContext async = req.startAsync(req, resp);
		ServletOutputStream out = resp.getOutputStream();
		out.setWriteListener(new AsyncWriter(async,
				Integer.parseInt(req.getParameter("iterations") == null ? "1" : req.getParameter("iterations")),
				Boolean.parseBoolean(req.getParameter("bytes"))));
	}

	private class AsyncWriter implements WriteListener {

		private final AsyncContext async;
		private final boolean writeBytes;
		private final int iterations;

		private boolean eof;

		public AsyncWriter(AsyncContext async, int iterations, boolean writeBytes) {
			this.async = async;
			this.iterations = iterations;
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

			int count = 0;

			if (writeBytes) {
				byte[] buf = new byte[10];
				for (int i = 0; i < buf.length; ++i) {
					buf[i] = (byte) ('0' + i);
				}

				do {
					out.write(buf);
				} while (out.isReady() && (++count < iterations));
			} else {
				int i = -1;
				do {
					int b = '0' + (++i % 10);
					out.write(b);
				} while (out.isReady() && (++count < (iterations * 10)));
			}

			out.close();
			async.complete();

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
