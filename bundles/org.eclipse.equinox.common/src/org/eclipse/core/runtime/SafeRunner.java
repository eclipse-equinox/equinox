/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	 * Runs the given runnable in a protected mode.   Exceptions
	 * thrown in the runnable are logged and passed to the runnable's
	 * exception handler.  Such exceptions are not rethrown by this method.
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
		} catch (Exception e) {
			handleException(code, e);
		} catch (LinkageError e) {
			handleException(code, e);
		} catch (AssertionError e) {
			handleException(code, e);
		}
	}

	private static void handleException(ISafeRunnable code, Throwable e) {
		if (!(e instanceof OperationCanceledException)) {
			// try to obtain the correct plug-in id for the bundle providing the safe runnable 
			Activator activator = Activator.getDefault();
			String pluginId = null;
			if (activator != null)
				pluginId = activator.getBundleId(code);
			if (pluginId == null)
				pluginId = IRuntimeConstants.PI_COMMON;
			String message = NLS.bind(CommonMessages.meta_pluginProblems, pluginId);
			IStatus status;
			if (e instanceof CoreException) {
				status = new MultiStatus(pluginId, IRuntimeConstants.PLUGIN_ERROR, message, e);
				((MultiStatus) status).merge(((CoreException) e).getStatus());
			} else {
				status = new Status(IStatus.ERROR, pluginId, IRuntimeConstants.PLUGIN_ERROR, message, e);
			}
			// Make sure user sees the exception: if the log is empty, log the exceptions on stderr 
			if (!RuntimeLog.isEmpty())
				RuntimeLog.log(status);
			else
				e.printStackTrace();
		}
		code.handleException(e);
	}
}
