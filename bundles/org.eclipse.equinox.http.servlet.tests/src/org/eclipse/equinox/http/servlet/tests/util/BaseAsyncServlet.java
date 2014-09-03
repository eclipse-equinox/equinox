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

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Raymond Augé
 */
public class BaseAsyncServlet extends BaseServlet {

	ScheduledThreadPoolExecutor executor =
		(ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(4);

	public BaseAsyncServlet() {
		super();
	}

	public BaseAsyncServlet(String content) {
		super(content);
	}

	@Override
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

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
			PrintWriter writer = null;

			try {
				writer = asyncContext.getResponse().getWriter();

				writer.print(Thread.currentThread().getName());
				writer.print(" - ");
				writer.print(content);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			finally {
				if (writer != null) {
					writer.close();
				}

				asyncContext.complete();
			}
		}
	}

}