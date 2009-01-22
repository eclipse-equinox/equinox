/******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Abstract implementation of {@link IFuture} and {@link ISafeProgressRunner}.
 *
 */
public abstract class AbstractFuture implements IFuture, ISafeProgressRunner {

	/**
	 * Returns <code>true</code> if this future has been previously canceled, <code>false</code>
	 * otherwise.  Subclasses must override.
	 * @return <code>true</code> if this future has been previously canceled, <code>false</code>
	 * otherwise
	 */
	public abstract boolean isCanceled();

	/**
	 * Return a progress monitor for this future.  Subclasses must override.
	 * @return the progress monitor for this future.
	 */
	public abstract IProgressMonitor getProgressMonitor();

}
