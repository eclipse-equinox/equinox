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

import org.eclipse.core.runtime.*;

/**
 * <p>
 * Future implementation for a single operation.
 * </p>
 * <p>
 * Subclasses may be created if desired. Note that if subclasses are created,
 * that they should/must be very careful with respect to overriding the
 * synchronized methods in this class.
 * </p>
 * 
 * @since 1.1
 */
public class SingleOperationFuture<ResultType> extends AbstractFuture<ResultType> {
	private static final String PLUGIN_ID = "org.eclipse.equinox.concurrent";

	private ResultType resultValue = null;
	private IStatus status = null;
	private TimeoutException timeoutException = null;
	protected IProgressMonitor progressMonitor;

	public SingleOperationFuture() {
		this((IProgressMonitor) null);
	}

	public SingleOperationFuture(IProgressMonitor progressMonitor) {
		super();
		this.progressMonitor = new FutureProgressMonitor(
				(progressMonitor == null) ? new NullProgressMonitor() : progressMonitor);
	}

	@Override
	public synchronized ResultType get() throws InterruptedException, OperationCanceledException {
		throwIfCanceled();
		while (!isDone())
			wait();
		throwIfCanceled();
		return resultValue;
	}

	@Override
	public synchronized ResultType get(long waitTimeInMillis)
			throws InterruptedException, TimeoutException, OperationCanceledException {
		// If waitTime out of bounds then throw illegal argument exception
		if (waitTimeInMillis < 0)
			throw new IllegalArgumentException("waitTimeInMillis must be => 0"); //$NON-NLS-1$
		// If we've been canceled then throw
		throwIfCanceled();
		// If we've previously experienced a timeout then throw
		if (timeoutException != null)
			throw timeoutException;
		// If we're already done, then return result
		if (isDone())
			return resultValue;
		// Otherwise, wait for some time, then throw if canceled during wait,
		// return value if
		// Compute start time and set waitTime
		long startTime = System.currentTimeMillis();
		long waitTime = waitTimeInMillis;
		// we've received one during wait or throw timeout exception if too much
		// time has elapsed
		for (;;) {
			wait(waitTime);
			throwIfCanceled();
			if (isDone())
				return resultValue;
			waitTime = waitTimeInMillis - (System.currentTimeMillis() - startTime);
			if (waitTime <= 0)
				throw createTimeoutException(waitTimeInMillis);
		}
	}

	@Override
	public synchronized boolean isDone() {
		return (status != null);
	}

	/**
	 * This method is not intended to be called by clients. Rather it should only be
	 * used by {@link IExecutor}s.
	 * 
	 * @noreference
	 */
	@Override
	public void runWithProgress(final IProgressRunnable<?> runnable) {
		Assert.isNotNull(runnable);
		if (!isCanceled()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					synchronized (SingleOperationFuture.this) {
						if (!isCanceled())
							setException(exception);
					}
				}

				@Override
				public void run() throws Exception {
					@SuppressWarnings("unchecked")
					ResultType result = (ResultType) runnable.run(getProgressMonitor());
					synchronized (SingleOperationFuture.this) {
						if (!isCanceled())
							set(result);
					}
				}
			});
		}
	}

	@Override
	public synchronized IStatus getStatus() {
		return status;
	}

	@Override
	public boolean hasValue() {
		// for a single operation future, hasValue means that the single
		// operation has completed, and there will be no more.
		return isDone();
	}

	@Override
	public synchronized boolean cancel() {
		if (isDone())
			return false;
		if (isCanceled())
			return false;
		setStatus(new Status(IStatus.CANCEL, PLUGIN_ID, IStatus.CANCEL, "Operation canceled", null)); //$NON-NLS-1$ //$NON-NLS-2$
		getProgressMonitor().setCanceled(true);
		notifyAll();
		return true;
	}

	protected synchronized void setException(Throwable ex) {
		setStatus(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Exception during operation", ex)); //$NON-NLS-1$ //$NON-NLS-2$
		notifyAll();
	}

	protected synchronized void set(ResultType newValue) {
		resultValue = newValue;
		setStatus(Status.OK_STATUS);
		notifyAll();
	}

	private synchronized void setStatus(IStatus status) {
		this.status = status;
	}

	private TimeoutException createTimeoutException(long timeout) {
		setStatus(
				new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Operation timeout after " + timeout + "ms", null)); //$NON-NLS-1$ //$NON-NLS-2$
																															// //$NON-NLS-3$
		timeoutException = new TimeoutException("Single operation timeout", timeout); //$NON-NLS-1$
		return timeoutException;
	}

	private void throwIfCanceled() throws OperationCanceledException {
		IProgressMonitor pm = getProgressMonitor();
		if (pm != null && pm.isCanceled()) {
			throw new OperationCanceledException("Single operation canceled"); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	@Override
	public synchronized boolean isCanceled() {
		return getProgressMonitor().isCanceled();
	}
}
