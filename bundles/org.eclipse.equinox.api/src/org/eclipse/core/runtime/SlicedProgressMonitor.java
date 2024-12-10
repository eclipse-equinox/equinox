/*******************************************************************************
 * Copyright (c) 2020 Christoph Laeubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Laeubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * default implementation of a {@link SlicedProgressMonitor} that synchronizes
 * on the given monitor to report work
 * 
 * @since 3.14
 */
public class SlicedProgressMonitor implements IProgressMonitor {

	private final int slicedWork;
	private final IProgressMonitor monitor;
	private Boolean canceled;
	private double increment;
	private String taskName;
	private double accumulator;
	private int workUnits;
	private boolean beginTaskCalled;
	private String subTaskName;

	public SlicedProgressMonitor(IProgressMonitor monitor, int totalWork) {
		this.monitor = monitor;
		this.slicedWork = totalWork;
		this.workUnits = totalWork;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		if (beginTaskCalled) {
			throw new IllegalStateException("This must only be called once on a given progress monitor instance."); //$NON-NLS-1$
		}
		taskName = name;
		if (totalWork > 0) {
			increment = slicedWork / (double) totalWork;
		}
		beginTaskCalled = true;
	}

	@Override
	public void done() {
		if (workUnits > 0) {
			synchronized (monitor) {
				monitor.worked(workUnits);
			}
		}
	}

	@Override
	public void internalWorked(double internalWork) {
		accumulator += internalWork;
		int workConsumed = 0;
		while (accumulator >= 1 && workUnits > 0) {
			accumulator -= 1;
			workUnits--;
			workConsumed++;
		}
		if (workConsumed > 0) {
			synchronized (monitor) {
				monitor.worked(workConsumed);
			}
		}
	}

	@Override
	public boolean isCanceled() {
		if (canceled != null) {
			return canceled;
		}
		synchronized (monitor) {
			return monitor.isCanceled();
		}
	}

	@Override
	public void setCanceled(boolean value) {
		this.canceled = value;
	}

	@Override
	public void setTaskName(String name) {
		this.taskName = name;
	}

	@Override
	public void subTask(String name) {
		this.subTaskName = name;
	}

	@Override
	public void worked(int work) {
		if (work > 0 && increment > 0) {
			internalWorked(work * increment);
		}
	}

	public String getName() {
		return taskName;
	}

	public String getSubTaskName() {
		return subTaskName;
	}

}
