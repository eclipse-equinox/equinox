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

package org.eclipse.osgi.service.runnable;

/**
 * An ApplicationLauncher is used to launch ParameterizedRunnable objects using 
 * the main thread.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @since 3.2
 */
public interface ApplicationLauncher {
	/**
	 * Launches the specified runnable using the main thread.
	 * @param runnable a ParameterizedRunnalbe to run on the main thread.
	 * @param context the context to launch the runnable with
	 */
	void launch(ParameterizedRunnable runnable, Object context);

	/**
	 * Forces the current runnable which is running to be stopped.  
	 * This method will return after the currently running ParameterizedRunnable
	 * has completely stopped.
	 * <p>
	 * After this method returns this ApplicationLauncher will no longer allow 
	 * applications to be launched.
	 */
	void shutdown();
}
