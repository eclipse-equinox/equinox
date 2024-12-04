/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

/**
 * Safe runnables represent blocks of code and associated exception handlers.
 * They are typically used when a plug-in needs to call some untrusted code
 * (e.g., code contributed by another plug-in via an extension).
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see SafeRunner#run(ISafeRunnable)
 */
@FunctionalInterface
public interface ISafeRunnable {
	/**
	 * Runs this runnable. Any exceptions thrown from this method will be logged by
	 * the caller and passed to this runnable's {@link #handleException} method.
	 *
	 * @exception Exception if a problem occurred while running this method
	 * @see SafeRunner#run(ISafeRunnable)
	 */
	public void run() throws Exception;

	/**
	 * Handles an exception thrown by this runnable's {@link #run} method. The
	 * processing done here should be specific to the particular use case for this
	 * runnable. Generalized exception processing, including logging in the
	 * Platform's log, is done by the {@link SafeRunner}.
	 * <p>
	 * All exceptions from the {@link #run} method are passed to this method, along
	 * with certain {@link Error} types that are typically caused by programming
	 * errors in the untrusted code being run.
	 * </p>
	 *
	 * @param exception an exception which occurred during processing the body of
	 *                  this runnable (i.e., in {@link #run})
	 * @see SafeRunner#run(ISafeRunnable)
	 */
	public default void handleException(Throwable exception) {
		// The exception has already been logged by the caller.
	}
}
