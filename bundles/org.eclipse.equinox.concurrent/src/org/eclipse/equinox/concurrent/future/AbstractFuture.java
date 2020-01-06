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
 * Abstract implementation of {@link IFuture} and {@link ISafeProgressRunner}.
 * @since 1.1
 */
public abstract class AbstractFuture<ResultType> implements
		IFuture<ResultType>, ISafeProgressRunner {

	/**
	 * Returns <code>true</code> if this future has been previously canceled,
	 * <code>false</code> otherwise. Subclasses must override.
	 * 
	 * @return <code>true</code> if this future has been previously canceled,
	 *         <code>false</code> otherwise
	 */
	public abstract boolean isCanceled();

	/**
	 * Return a progress monitor for this future. Subclasses must override.
	 * 
	 * @return the progress monitor for this future.
	 */
	public abstract IProgressMonitor getProgressMonitor();

}
