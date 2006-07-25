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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MainThreadRunnable implements ApplicationRunnable, IApplicationContext {
	private static final String PROP_ECLIPSE_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	private Object application;
	private EclipseAppHandle appHandle;

	public MainThreadRunnable(EclipseAppHandle appHandle) {
		this.appHandle = appHandle;
	}

	public Object run(Object context) throws Exception {
		// if the given arg is null then pass in the left over command line args.
		if (context == null)
			context = CommandLineArgs.getApplicationArgs();
		Object result;
		try {
			application = appHandle.getConfiguration().createExecutableExtension("run"); //$NON-NLS-1$
			if (application instanceof IApplication) {
				Map args = appHandle.getArguments();
				if (args == null) {
					args = new HashMap(2);
					appHandle.setArguments(args);
				}
				if (args.get(IApplicationContext.APPLICATION_ARGS) == null)
					args.put(IApplicationContext.APPLICATION_ARGS, context);
				result = ((IApplication) application).start(this);
			} else
				result = ((IPlatformRunnable) application).run(context);
		} finally {
			application = null;
			// The application exited itself; notify the app context
			appHandle.setAppStatus(EclipseAppHandle.STOPPED);
		}
		int exitCode = result instanceof Integer ? ((Integer) result).intValue() : 0;
		// use the long way to set the property to compile against eeminimum
		System.getProperties().setProperty(PROP_ECLIPSE_EXITCODE, Integer.toString(exitCode));
		if (Activator.DEBUG)
			System.out.println(NLS.bind(Messages.application_returned, (new String[] {appHandle.getApplicationDescriptor().getApplicationId(), result == null ? "null" : result.toString()}))); //$NON-NLS-1$
		return result;
	}

	public void stop() {
		appHandle.destroy();
	}

	IApplication getApplication() {
		return (IApplication) ((application instanceof IApplication) ? application : null);
	}

	public void endSplashScreen() {
		final Runnable handler = getSplashHandler();
		if (handler == null)
			return;
		SafeRunner.run(new ISafeRunnable() {
			public void handleException(Throwable e) {
				// just continue ... the exception has already been logged by
				// handleException(ISafeRunnable)
			}

			public void run() throws Exception {
				handler.run();
			}
		});
	}

	private Runnable getSplashHandler() {
		ServiceReference[] ref;
		try {
			ref = Activator.getContext().getServiceReferences(Runnable.class.getName(), "(name=splashscreen)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			return null;
		}
		if (ref == null)
			return null;
		// assumes the endInitializationHandler is available as a service
		// see EclipseStarter.publishSplashScreen
		for (int i = 0; i < ref.length; i++) {
			Runnable result = (Runnable) Activator.getContext().getService(ref[i]);
			Activator.getContext().ungetService(ref[i]); // immediately unget the service because we are not using it long
			return result;
		}
		return null;
	}

	public Map getArguments() {
		return appHandle.getArguments();
	}
}
