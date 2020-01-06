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
