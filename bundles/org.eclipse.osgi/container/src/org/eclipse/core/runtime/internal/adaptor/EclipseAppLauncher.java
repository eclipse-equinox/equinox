/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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

package org.eclipse.core.runtime.internal.adaptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.service.runnable.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

public class EclipseAppLauncher implements ApplicationLauncher {
	volatile private ParameterizedRunnable runnable = null;
	private Object appContext = null;
	private final java.util.concurrent.Semaphore runningLock = new java.util.concurrent.Semaphore(1);
	private final java.util.concurrent.Semaphore waitForAppLock = new java.util.concurrent.Semaphore(0);
	private final BundleContext context;
	private boolean relaunch = false;
	private final boolean failOnNoDefault;
	private final FrameworkLog log;
	private final EquinoxConfiguration equinoxConfig;

	public EclipseAppLauncher(BundleContext context, boolean relaunch, boolean failOnNoDefault, FrameworkLog log, EquinoxConfiguration equinoxConfig) {
		this.context = context;
		this.relaunch = relaunch;
		this.failOnNoDefault = failOnNoDefault;
		this.log = log;
		this.equinoxConfig = equinoxConfig;
		findRunnableService();
	}

	/*
	 * Used for backwards compatibility with < 3.2 runtime
	 */
	private void findRunnableService() {
		// look for a ParameterizedRunnable registered as a service by runtimes (3.0, 3.1)
		String appClass = ParameterizedRunnable.class.getName();
		ServiceReference<?>[] runRefs = null;
		try {
			runRefs = context.getServiceReferences(ParameterizedRunnable.class.getName(), "(&(objectClass=" + appClass + ")(eclipse.application=*))"); //$NON-NLS-1$//$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		if (runRefs != null && runRefs.length > 0) {
			// found the service use it as the application.
			runnable = (ParameterizedRunnable) context.getService(runRefs[0]);
			// we will never be able to relaunch with a pre 3.2 runtime
			relaunch = false;
			waitForAppLock.release();
		}
	}

	/*
	 * Starts this application launcher on the current thread.  This method
	 * should be called by the main thread to ensure that applications are
	 * launched in the main thread.
	 */
	public Object start(Object defaultContext) throws Exception {
		// here we assume that launch has been called by runtime before we started
		// TODO this may be a bad assumption but it works for now because we register the app launcher as a service and runtime synchronously calls launch on the service
		if (failOnNoDefault && runnable == null)
			throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ERROR_NO_APPLICATION);
		Object result = null;
		boolean doRelaunch;
		Bundle b = context.getBundle();
		do {
			try {
				if (relaunch) {
					// need a thread to kick the main thread when the framework stops
					final Thread mainThread = Thread.currentThread();
					final BundleContext mainContext = context;
					new Thread((Runnable) () -> {
						Framework framework = (Framework) mainContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
						try {
							framework.waitForStop(0);
							// framework is done; tell the main thread to stop
							mainThread.interrupt();
						} catch (InterruptedException e) {
							Thread.interrupted();
							// just exiting now
						}
					}, "Framework watcher").start(); //$NON-NLS-1$

				}
				result = runApplication(defaultContext);
			} catch (Exception e) {
				if (!relaunch || (b.getState() & Bundle.ACTIVE) == 0)
					throw e;
				if (log != null)
					log.log(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, Msg.ECLIPSE_STARTUP_APP_ERROR, 1, e, null));
			}
			doRelaunch = (relaunch && (b.getState() & Bundle.ACTIVE) != 0);
		} while (doRelaunch);
		return result;
	}

	/*
	 * Waits for an application to be launched and the runs the application on the
	 * current thread (main).
	 */
	private Object runApplication(Object defaultContext) throws Exception {
		try {
			// wait for an application to be launched.
			waitForAppLock.acquire();
			// an application is ready; acquire the running lock.
			// this must happen after we have acquired an application (by acquiring waitForAppLock above).
			runningLock.acquire();
			if (EclipseStarter.debug) {
				String timeString = equinoxConfig.getConfiguration("eclipse.startTime"); //$NON-NLS-1$
				long time = timeString == null ? 0L : Long.parseLong(timeString);
				Debug.println("Starting application: " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			}
			try {
				// run the actual application on the current thread (main).
				return runnable.run(appContext != null ? appContext : defaultContext);
			} finally {
				// free the runnable application and release the lock to allow another app to be launched.
				runnable = null;
				appContext = null;
				runningLock.release();
			}
		} catch (InterruptedException e) {
			// ignore; but mark interrupted for others
			Thread.interrupted();
		}
		return null;
	}

	@Override
	public void launch(ParameterizedRunnable app, Object applicationContext) {
		waitForAppLock.tryAcquire(); // clear out any pending apps notifications
		if (!runningLock.tryAcquire()) // check to see if an application is currently running
			throw new IllegalStateException("An application is aready running."); //$NON-NLS-1$
		this.runnable = app;
		this.appContext = applicationContext;
		waitForAppLock.release(); // notify the main thread to launch an application.
		runningLock.release(); // release the running lock
	}

	@Override
	public void shutdown() {
		// this method will aquire and keep the runningLock to prevent
		// all future application launches.
		if (runningLock.tryAcquire())
			return; // no application is currently running.
		ParameterizedRunnable currentRunnable = runnable;
		if (currentRunnable instanceof ApplicationRunnable) {
			((ApplicationRunnable) currentRunnable).stop();
			try {
				runningLock.tryAcquire(1, TimeUnit.MINUTES); // timeout after 1 minute.
			} catch (InterruptedException e) {
				// ignore
				Thread.interrupted();
			}
		}
	}

	/*
	 * Similar to the start method this method will restart the default method on current thread.
	 * This method assumes that the default application was launched at least once and that an ApplicationDescriptor
	 * exists that can be used to relaunch the default application.
	 */
	public Object reStart(Object argument) throws Exception {
		ServiceReference<?> ref[] = null;
		ref = context.getServiceReferences("org.osgi.service.application.ApplicationDescriptor", "(eclipse.application.default=true)"); //$NON-NLS-1$//$NON-NLS-2$
		if (ref != null && ref.length > 0) {
			Object defaultApp = context.getService(ref[0]);
			Method launch = defaultApp.getClass().getMethod("launch", new Class[] {Map.class}); //$NON-NLS-1$
			launch.invoke(defaultApp, new Object[] {null});
			return start(argument);
		}
		throw new IllegalStateException(Msg.ECLIPSE_STARTUP_ERROR_NO_APPLICATION);
	}
}
