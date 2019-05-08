/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.osgi.tests.appadmin;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class ExitValueApp implements IApplication, Runnable {
	public static final String returnNullResult = "return.null"; //$NON-NLS-1$
	public static final String returnAsyncResult = "return.async";
	public static final String setAsyncEarly = "set.async.early";
	public static final String setAsyncWrongApp = "set.async.wrongApp";
	public static final String exitValue = "Exit Value"; //$NON-NLS-1$

	private boolean active = true;
	private boolean stopped = false;
	private boolean useAsync = false;
	private boolean returnNull = false;
	private boolean setWrongApp = false;
	private IApplicationContext appContext;
	final Object guardObj = new Object();

	public synchronized Object start(IApplicationContext context) {
		appContext = context;
		context.applicationRunning();
		Boolean nullValue = (Boolean) context.getArguments().get(returnNullResult);
		returnNull = nullValue == null ? false : nullValue.booleanValue();
		Boolean asyncValue = (Boolean) context.getArguments().get(returnAsyncResult);
		useAsync = asyncValue == null ? false : asyncValue.booleanValue();

		Boolean asyncEarlyValue = (Boolean) context.getArguments().get(setAsyncEarly);
		boolean setEarly = asyncEarlyValue == null ? false : asyncEarlyValue.booleanValue();
		Boolean wrongAppValue = (Boolean) context.getArguments().get(setAsyncWrongApp);
		setWrongApp = wrongAppValue == null ? false : wrongAppValue.booleanValue();
		if (setEarly) {
			try {
				context.setResult("failed", this);
				// failed
			} catch (IllegalStateException e) {
				// passed
				return exitValue;
			}
		}
		if (useAsync) {
			System.out.println("async result");
			new Thread(this, "ExitValueApp Test").start();
			return IApplicationContext.EXIT_ASYNC_RESULT;
		}
		run();
		return returnNull ? null : exitValue;
	}

	public synchronized void stop() {
		active = false;
		notifyAll();
		while (!stopped)
			try {
				wait(100);
			} catch (InterruptedException e) {
				// do nothing
			}
	}

	@Override
	public synchronized void run() {
		if (active) {
			try {
				wait(5000); // only run for 5 seconds at most
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		stopped = true;
		if (useAsync) {
			IApplication app = this;
			Object result = returnNull ? null : exitValue;
			if (setWrongApp) {
				result = "failed";
				app = new IApplication() {

					public void stop() {
						// nothing
					}

					public Object start(IApplicationContext context) throws Exception {
						return null;
					}
				};
			}
			try {
				appContext.setResult(result, app);
				// failed
			} catch (IllegalArgumentException e) {
				// passed
				appContext.setResult(returnNull ? null : exitValue, this);
			}
		}
		notifyAll();
	}

}
