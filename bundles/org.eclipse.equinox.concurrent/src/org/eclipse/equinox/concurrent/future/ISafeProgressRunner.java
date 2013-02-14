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

/**
 * A runner that can execute {@link IProgressRunnable}s safely. Running an
 * {@link IProgressRunnable} safely means not throwing any {@link Exception}
 * possibly thrown by the given {@link IProgressRunnable}.
 * @since 1.1
 * 
 */
public interface ISafeProgressRunner {
	void runWithProgress(IProgressRunnable<?> runnable);
}
