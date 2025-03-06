/******************************************************************************
 * Copyright (c) 2010, 2013 Composent, Inc. and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;

/**
 * <p>
 * Listenable future implementation for a single operation.
 * </p>
 * <p>
 * Subclasses may be created if desired. Note that if subclasses are created,
 * that they should/must be very careful with respect to overriding the
 * synchronized methods in this class.
 * </p>
 * 
 * @since 1.1
 */
public class SingleOperationListenableFuture<ResultType> extends SingleOperationFuture<ResultType>
		implements IListenableFuture<ResultType> {

	private IProgressRunnable<ResultType> progressRunnable;
	private IProgressMonitor progressMonitor;
	private IExecutor listenerExecutor;

	public SingleOperationListenableFuture() {
		super();
	}

	public SingleOperationListenableFuture(IProgressMonitor progressMonitor) {
		super(progressMonitor);
	}

	@Override
	public void addListener(IExecutor executor, IProgressRunnable<ResultType> progressRunnable,
			IProgressMonitor monitor) {
		Assert.isNotNull(executor);
		Assert.isNotNull(progressRunnable);
		synchronized (this) {
			if (this.progressRunnable == null) {
				this.progressRunnable = progressRunnable;
				this.progressMonitor = monitor;
				this.listenerExecutor = executor;
				// Now, if we're already done, then execute the listenable now
				if (isDone()) {
					execListenable();
				}
			}
		}
	}

	@Override
	public void runWithProgress(final IProgressRunnable<?> runnable) {
		Assert.isNotNull(runnable);
		if (!isCanceled()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					synchronized (SingleOperationListenableFuture.this) {
						if (!isCanceled()) {
							setException(exception);
						}
						execListenable();
					}
				}

				@Override
				public void run() throws Exception {
					@SuppressWarnings("unchecked")
					ResultType result = (ResultType) runnable.run(getProgressMonitor());
					synchronized (SingleOperationListenableFuture.this) {
						if (!isCanceled()) {
							set(result);
						}
						execListenable();
					}
				}
			});
		}
	}

	private void execListenable() {
		// If no progressRunnable has been set, then we simply return
		if (progressRunnable == null || listenerExecutor == null) {
			return;
		}
		// Make sure that the progress monitor is set to non-null
		if (progressMonitor == null) {
			progressMonitor = new NullProgressMonitor();
		}
		// then we execute using executor
		this.listenerExecutor.execute(progressRunnable, progressMonitor);
	}
}
