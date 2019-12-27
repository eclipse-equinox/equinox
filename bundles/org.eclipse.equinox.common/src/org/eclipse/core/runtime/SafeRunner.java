/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.core.runtime;

import org.eclipse.core.internal.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Runs the given ISafeRunnable in a protected mode: exceptions and certain
 * errors thrown in the runnable are logged and passed to the runnable's
 * exception handler.  Such exceptions are not rethrown by this method.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * @since org.eclipse.equinox.common 3.2
 */
public final class SafeRunner {

	/**
	 * Runs the given runnable in a protected mode. Exceptions
	 * thrown in the runnable are logged and passed to the runnable's
	 * exception handler. Such exceptions are not rethrown by this method.
	 * <p>
	 * In addition to catching all {@link Exception} types, this method also catches certain {@link Error} 
	 * types that typically result from programming errors in the code being executed. 
	 * Severe errors that are not generally safe to catch are not caught by this method.
	 * </p>
	 *
	 * @param code the runnable to run
	 */
	public static void run(ISafeRunnable code) {
		Assert.isNotNull(code);
		try {
			code.run();
		} catch (Exception | LinkageError | AssertionError e) {
			handleException(code, e);
		}
	}

	/**
	 * Runs the given runnable in a protected mode and returns the result given by the runnable. Exceptions
	 * thrown in the runnable are logged and passed to the runnable's
	 * exception handler. Such exceptions are not rethrown by this method, instead null is returned.
	 * <p>
	 * In addition to catching all {@link Exception} types, this method also catches certain {@link Error}
	 * types that typically result from programming errors in the code being executed. 
	 * Severe errors that are not generally safe to catch are not caught by this method.
	 * </p>
	 * @param <T> the result type
	 *
	 * @param code the runnable to run
	 * @return the result
	 *
	 * @since 3.11
	 */
	public static <T> T run(ISafeRunnableWithResult<T> code) {
		Assert.isNotNull(code);
		try {
			return code.runWithResult();
		} catch (Exception | LinkageError | AssertionError e) {
			handleException(code, e);
			return null;
		}
	}

	private static void handleException(ISafeRunnable code, Throwable exception) {
		if (!(exception instanceof OperationCanceledException)) {
			String pluginId = getBundleIdOfSafeRunnable(code);
			IStatus status = convertToStatus(exception, pluginId);
			makeSureUserSeesException(exception, status);
		}
		code.handleException(exception);
	}

	private static void makeSureUserSeesException(Throwable exception, IStatus status) {
		if (RuntimeLog.isEmpty()) {
			exception.printStackTrace();
		} else {
			RuntimeLog.log(status);
		}
	}

	private static String getBundleIdOfSafeRunnable(ISafeRunnable code) {
		Activator activator = Activator.getDefault();
		String pluginId = null;
		if (activator != null)
			pluginId = activator.getBundleId(code);
		if (pluginId == null)
			return IRuntimeConstants.PI_COMMON;
		return pluginId;
	}

	private static IStatus convertToStatus(Throwable exception, String pluginId) {
		String message = NLS.bind(CommonMessages.meta_pluginProblems, pluginId);
		if (exception instanceof CoreException) {
			MultiStatus status = new MultiStatus(pluginId, IRuntimeConstants.PLUGIN_ERROR, message, exception);
			status.merge(((CoreException) exception).getStatus());
			return status;
		}
		return new Status(IStatus.ERROR, pluginId, IRuntimeConstants.PLUGIN_ERROR, message, exception);
	}
}