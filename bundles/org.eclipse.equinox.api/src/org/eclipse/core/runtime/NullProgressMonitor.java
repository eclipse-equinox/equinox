/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * A default progress monitor implementation suitable for subclassing.
 * <p>
 * This implementation supports cancelation. The default implementations of the
 * other methods do nothing.
 * </p>
 * <p>
 * This class can be used without OSGi running.
 * </p>
 */
public class NullProgressMonitor implements IProgressMonitor {

	/**
	 * Indicates whether cancel has been requested.
	 */
	private volatile boolean cancelled = false;

	/**
	 * Constructs a new progress monitor.
	 */
	public NullProgressMonitor() {
		super();
	}

	/**
	 * This implementation does nothing. Subclasses may override this method to do
	 * interesting processing when a task begins.
	 * 
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	@Override
	public void beginTask(String name, int totalWork) {
		// do nothing
	}

	/**
	 * This implementation does nothing. Subclasses may override this method to do
	 * interesting processing when a task is done.
	 * 
	 * @see IProgressMonitor#done()
	 */
	@Override
	public void done() {
		// do nothing
	}

	/**
	 * This implementation does nothing. Subclasses may override this method.
	 * 
	 * @see IProgressMonitor#internalWorked(double)
	 */
	@Override
	public void internalWorked(double work) {
		// do nothing
	}

	/**
	 * This implementation returns the value of the internal state variable set by
	 * <code>setCanceled</code>. Subclasses which override this method should
	 * override <code>setCanceled</code> as well.
	 *
	 * @see IProgressMonitor#isCanceled()
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	@Override
	public boolean isCanceled() {
		return cancelled;
	}

	/**
	 * This implementation sets the value of an internal state variable. Subclasses
	 * which override this method should override <code>isCanceled</code> as well.
	 *
	 * @see IProgressMonitor#isCanceled()
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	@Override
	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * This implementation does nothing. Subclasses may override this method to do
	 * something with the name of the task.
	 * 
	 * @see IProgressMonitor#setTaskName(String)
	 */
	@Override
	public void setTaskName(String name) {
		// do nothing
	}

	/**
	 * This implementation does nothing. Subclasses may override this method to do
	 * interesting processing when a subtask begins.
	 * 
	 * @see IProgressMonitor#subTask(String)
	 */
	@Override
	public void subTask(String name) {
		// do nothing
	}

	/**
	 * This implementation does nothing. Subclasses may override this method to do
	 * interesting processing when some work has been completed.
	 * 
	 * @see IProgressMonitor#worked(int)
	 */
	@Override
	public void worked(int work) {
		// do nothing
	}
}
