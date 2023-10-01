/******************************************************************************
 * Copyright (c) 2010, 2013 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *   Gunnar Wagenknecht - added support for generics
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface defining a block of code that can be run, return an Object result,
 * and throw an arbitrary Exception.
 * 
 * @param <ResultType>
 *            the type that will be returned by {@link #run(IProgressMonitor)}
 * @since 1.1
 */
public interface IProgressRunnable<ResultType> {

	/**
	 * Perform some action that returns a result or throws an exception
	 * 
	 * @param monitor
	 *            the IProgressMonitor associated with this callable
	 * @return result from the call
	 * @throws Exception
	 */
	ResultType run(IProgressMonitor monitor) throws Exception;
}
