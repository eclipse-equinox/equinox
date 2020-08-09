/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Laeubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * An abstract wrapper around a progress monitor which,
 * unless overridden, forwards <code>IProgressMonitor</code>
 * and <code>IProgressMonitorWithBlocking</code> methods to the wrapped progress monitor.
 * <p>
 * This class can be used without OSGi running.
 * </p><p>
 * Clients may subclass.
 * </p>
 */
public abstract class ProgressMonitorWrapper implements IProgressMonitor, IProgressMonitorWithBlocking {

	/** The wrapped progress monitor. */
	private IProgressMonitor progressMonitor;

	/** 
	 * Creates a new wrapper around the given monitor.
	 *
	 * @param monitor the progress monitor to forward to
	 */
	protected ProgressMonitorWrapper(IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		progressMonitor = monitor;
	}

	/** 
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	@Override
	public void beginTask(String name, int totalWork) {
		progressMonitor.beginTask(name, totalWork);
	}

	/**
	 * This implementation of a <code>IProgressMonitorWithBlocking</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitorWithBlocking#clearBlocked()
	 * @since 3.0
	 */
	@Override
	public void clearBlocked() {
		progressMonitor.clearBlocked();
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#done()
	 */
	@Override
	public void done() {
		progressMonitor.done();
	}

	/**
	 * Returns the wrapped progress monitor.
	 *
	 * @return the wrapped progress monitor
	 */
	public IProgressMonitor getWrappedProgressMonitor() {
		return progressMonitor;
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#internalWorked(double)
	 */
	@Override
	public void internalWorked(double work) {
		progressMonitor.internalWorked(work);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#isCanceled()
	 */
	@Override
	public boolean isCanceled() {
		return progressMonitor.isCanceled();
	}

	/**
	 * This implementation of a <code>IProgressMonitorWithBlocking</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitorWithBlocking#setBlocked(IStatus)
	 * @since 3.0
	 */
	@Override
	public void setBlocked(IStatus reason) {
		progressMonitor.setBlocked(reason);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	@Override
	public void setCanceled(boolean b) {
		progressMonitor.setCanceled(b);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#setTaskName(String)
	 */
	@Override
	public void setTaskName(String name) {
		progressMonitor.setTaskName(name);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#subTask(String)
	 */
	@Override
	public void subTask(String name) {
		progressMonitor.subTask(name);
	}

	/**
	 * This implementation of a <code>IProgressMonitor</code>
	 * method forwards to the wrapped progress monitor.
	 * Clients may override this method to do additional
	 * processing.
	 *
	 * @see IProgressMonitor#worked(int)
	 */
	@Override
	public void worked(int work) {
		progressMonitor.worked(work);
	}
}
