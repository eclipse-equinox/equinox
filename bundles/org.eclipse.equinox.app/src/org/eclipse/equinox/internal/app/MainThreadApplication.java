/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import org.eclipse.equinox.app.IAppContext;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.util.NLS;

public class MainThreadApplication implements ApplicationRunnable {
	private static final String PROP_ECLIPSE_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	private Object application;
	private EclipseAppHandle appContext;
	private Exception launchException;

	public MainThreadApplication(EclipseAppHandle appContext) {
		this.appContext = appContext;
		try {
			application = appContext.getConfiguration().createExecutableExtension("run"); //$NON-NLS-1$
		} catch (Exception e) {
			// had an error creating the executable extension
			// save the exception to throw on the main thread (keeping legacy behavior)
			this.launchException = e;
		}
	}

	public Object run(Object context) throws Exception {
		// if the given arg is null then pass in the left over command line args.
		if (context == null)
			context = AppPersistenceUtil.getApplicationArgs();
		Object result;
		try {
			if (launchException != null)
				// this is a dummy handle used to throw an exception on the main thread.
				throw launchException;
			if (application instanceof IApplication) {
				result = ((IApplication) application).run(context);
			} else
				result = ContainerManager.execMethod(application, "run", Object.class, context); //$NON-NLS-1$
		} finally {
			application = null;
			// The application exited itself; notify the app context
			appContext.setAppStatus(IAppContext.STOPPED);
		}
		int exitCode = result instanceof Integer ? ((Integer) result).intValue() : 0;
		// use the long way to set the property to compile against eeminimum
		System.getProperties().setProperty(PROP_ECLIPSE_EXITCODE, Integer.toString(exitCode));
		if (Activator.DEBUG)
			System.out.println(NLS.bind(Messages.application_returned, (new String[] {appContext.getApplicationDescriptor().getApplicationId(), result == null ? "null" : result.toString()}))); //$NON-NLS-1$
		return result;
	}

	public void stop() {
		// we can only handle forced stops if this application is an IApplication
		if (application instanceof IApplication)
			((IApplication) application).stop();
	}

	IApplication getApplication() {
		return (IApplication) ((application instanceof IApplication) ? application : null);
	}
}
