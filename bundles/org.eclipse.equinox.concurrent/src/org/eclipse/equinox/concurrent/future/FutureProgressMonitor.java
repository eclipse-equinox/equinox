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
import org.eclipse.core.runtime.ProgressMonitorWrapper;

/**
 * Progress Monitor for use with future.  This progress monitor provides a wrapper
 * for potentially two progress monitors:  one provided by the {@link IFuture} 
 * client in a method call, the other (a child progress monitor) provided by the 
 * IExecutor that creates the future instance.
 *
 */
public class FutureProgressMonitor extends ProgressMonitorWrapper {

	private IProgressMonitor monitor;
	private Object lock = new Object();

	/**
	 * Create a new progress monitor wrappering the given monitor. The nested
	 * monitor is the one exposed to clients of futures.
	 * @param progressMonitor the client-facing monitor used with a future.  May be <code>null</code>.
	 * 
	 * @see #setChildProgressMonitor(IProgressMonitor)
	 */
	public FutureProgressMonitor(IProgressMonitor progressMonitor) {
		super(progressMonitor);
	}

	public void beginTask(String name, int totalWork) {
		super.beginTask(name, totalWork);
		synchronized (lock) {
			if (monitor != null)
				monitor.beginTask(name, totalWork);
		}
	}

	public void done() {
		super.done();
		synchronized (lock) {
			monitor.done();
			monitor = null;
		}
	}

	public void internalWorked(double work) {
		super.internalWorked(work);
		synchronized (lock) {
			if (monitor != null)
				monitor.internalWorked(work);
		}
	}

	public void setCanceled(boolean value) {
		super.setCanceled(value);
		synchronized (lock) {
			if (monitor != null)
				monitor.setCanceled(value);
		}
	}

	public void setTaskName(String name) {
		super.setTaskName(name);
		synchronized (lock) {
			if (monitor != null)
				monitor.setTaskName(name);
		}
	}

	public void subTask(String name) {
		super.subTask(name);
		synchronized (lock) {
			if (monitor != null)
				monitor.subTask(name);
		}
	}

	public void worked(int work) {
		super.worked(work);
		synchronized (lock) {
			if (monitor != null)
				monitor.worked(work);
		}
	}

	/**
	 * Set the client-facing progress monitor to the given value.
	 * 
	 * @param value a second (child) monitor to report progress/take cancelation from.
	 * If the parent progress monitor has been previously canceled, the child progress monitor's
	 * setCanceled method will be called.
	 */
	public void setChildProgressMonitor(IProgressMonitor value) {
		synchronized (lock) {
			this.monitor = value;
			if (monitor != null && isCanceled())
				this.monitor.setCanceled(true);
		}
	}

}