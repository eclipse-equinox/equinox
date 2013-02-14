/******************************************************************************
 * Copyright (c) 2009, 2013 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

/**
 * An executor that can execute Runnables, rather than {@link IProgressRunnable}
 * s.
 * 
 * @see IExecutor#execute(IProgressRunnable,
 *      org.eclipse.core.runtime.IProgressMonitor)
 * @since 1.1
 */
public interface IRunnableExecutor {
	void execute(Runnable runnable);
}
