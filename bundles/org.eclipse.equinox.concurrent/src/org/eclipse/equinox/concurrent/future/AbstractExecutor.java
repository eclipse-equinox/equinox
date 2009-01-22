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
 * Abstract implementation of {@link IExecutor} and {@link IRunnableExecutor}.
 *
 */
public abstract class AbstractExecutor implements IRunnableExecutor, IExecutor {

	public void execute(final Runnable runnable) {
		execute(new IProgressRunnable() {
			public Object run(IProgressMonitor monitor) throws Exception {
				runnable.run();
				return null;
			}
		}, null);
	}

	public abstract IFuture execute(IProgressRunnable runnable, IProgressMonitor monitor);

	/**
	 * Create an {@link AbstractFuture} instance.  Subclasses must override to define 
	 * the concrete type of future to return from  {@link #execute(IProgressRunnable, IProgressMonitor)}.
	 * @param progressMonitor any progress monitor to provide to the future upon construction.  May be
	 * <code>null</code>.
	 * @return the created future
	 */
	protected abstract AbstractFuture createFuture(IProgressMonitor progressMonitor);

}
