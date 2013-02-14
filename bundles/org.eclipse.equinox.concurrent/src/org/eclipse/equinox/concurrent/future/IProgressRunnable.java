/******************************************************************************
 * Copyright (c) 2010, 2013 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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