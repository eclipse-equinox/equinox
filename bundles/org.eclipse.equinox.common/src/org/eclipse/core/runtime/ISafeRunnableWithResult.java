/*******************************************************************************
 * Copyright (c) 2020 Marcus Hoepfner and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Marcus Hoepfner - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * Safe runnables represent blocks of code and associated exception
 * handlers.  They are typically used when a plug-in needs to call some
 * untrusted code (e.g., code contributed by another plug-in via an
 * extension). In contradiction to {@link ISafeRunnable} this runnable is able to return a result.
 * <p>
 * This interface can be used without OSGi running.
 * </p><p>
 * Clients may implement this interface.
 * </p>
 * @param <T> the type of the result
 * @see SafeRunner#run(ISafeRunnableWithResult)
 * 
 * @since 3.11
 */
@FunctionalInterface
public interface ISafeRunnableWithResult<T> extends ISafeRunnable {
	@Override
	default void run() throws Exception {
		runWithResult();
	}

	/**
	 * Runs this runnable and returns the result. Any exceptions thrown from this method will
	 * be logged by the caller and passed to this runnable's
	 * {@link #handleException} method.
	 * @return the result
	 *
	 * @exception Exception if a problem occurred while running this method
	 * @see SafeRunner#run(ISafeRunnable)
	 */
	public T runWithResult() throws Exception;
}
