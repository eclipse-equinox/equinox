/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
 * Runs the given ISafeRunnable in a protected mode: exceptions
 * thrown in the runnable are logged and passed to the runnable's
 * exception handler.  Such exceptions are not rethrown by this method.
 * 
 * Note that this class requires presence of OSGi.
 * 
 * @since org.eclipse.equinox.common 1.0
 */
public final class SafeRunner {

	/**
	 * Runs the given runnable in a protected mode.   Exceptions
	 * thrown in the runnable are logged and passed to the runnable's
	 * exception handler.  Such exceptions are not rethrown by this method.
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
		}
	}

	private static void handleException(ISafeRunnable code, Throwable e) {
		if (!(e instanceof OperationCanceledException)) {
			// try to obtain the correct plug-in id for the bundle providing the safe runnable 
			String pluginId = CommonOSGiUtils.getDefault().getBundleId(code);
			if (pluginId == null)
				pluginId = IRuntimeConstants.NAME;
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
