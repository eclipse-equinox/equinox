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

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Raymond Augé
 */
public class BaseAsyncServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4);

	public BaseAsyncServlet() {
		super();
	}

	public BaseAsyncServlet(String content) {
		super(content);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) {

		AsyncContext asyncContext = request.startAsync(request, response);

		executor.execute(new AsyncOperation(asyncContext));
	}

	public class AsyncOperation implements Runnable {
		AsyncContext asyncContext;

		public AsyncOperation(AsyncContext asyncContext) {
			this.asyncContext = asyncContext;
		}

		@Override
		public void run() {
			try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
				writer.print(Thread.currentThread().getName());
				writer.print(" - ");
				writer.print(content);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				asyncContext.complete();
			}
		}
	}

}
