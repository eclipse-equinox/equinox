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
 *     Lars.Vogel <Lars.Vogel@vogella.com> - Bug 479914
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * A progress monitor that uses a given amount of work ticks from a parent monitor. Code that 
 * currently uses this utility should be rewritten to use {@link SubMonitor} instead.
 * Consider the following example:
 * <pre>
 *     void someMethod(IProgressMonitor pm) {
 *        pm.beginTask("Main Task", 100);
 *        SubProgressMonitor subMonitor1= new SubProgressMonitor(pm, 60);
 *        try {
 *           doSomeWork(subMonitor1);
 *        } finally {
 *           subMonitor1.done();
 *        }
 *        SubProgressMonitor subMonitor2= new SubProgressMonitor(pm, 40);
 *        try {
 *           doSomeMoreWork(subMonitor2);
 *        } finally {
 *           subMonitor2.done();
 *        }
 *     }
 * </pre>
 * <p>
 * The above code should be refactored to this:
 * <pre>
 *     void someMethod(IProgressMonitor pm) {
 *        SubMonitor subMonitor = SubMonitor.convert(pm, "Main Task", 100);
 *        doSomeWork(subMonitor.split(60));
 *        doSomeMoreWork(subMonitor.split(40));
 *     }
 * </pre>
 * <p>
 * The process for converting code which used SubProgressMonitor into SubMonitor is:
 * <ul>
 * <li>Calls to {@link IProgressMonitor#beginTask} on the root monitor should be replaced by a call
 * to {@link SubMonitor#convert}. Keep the returned SubMonitor around as a local variable and refer
 * to it instead of the root monitor for the remainder of the method.</li>
 * <li>All calls to {@link #SubProgressMonitor(IProgressMonitor, int)} should be replaced by calls to
 * {@link SubMonitor#split(int)}.</li>
 * <li>If a SubProgressMonitor is constructed using the SUPPRESS_SUBTASK_LABEL flag, replace it with the
 * two-argument version of {@link SubMonitor#split(int, int)} using {@link SubMonitor#SUPPRESS_SUBTASK}
 * as the second argument.</li>
 * <li>It is not necessary to call done on an instance of {@link SubMonitor}.</li> 
 * </ul>
 * <p>
 * Please see the {@link SubMonitor} documentation for further examples.
 * <p>
 * This class can be used without OSGi running.
 * </p><p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * 
 * @deprecated use {@link SubMonitor} instead
 */
@Deprecated
public class SubProgressMonitor extends ProgressMonitorWrapper {

	/**
	 * Style constant indicating that calls to <code>subTask</code>
	 * should not have any effect. This is equivalent to {@link SubMonitor#SUPPRESS_SUBTASK}
	 *
	 * @see #SubProgressMonitor(IProgressMonitor,int,int)
	 */
	public static final int SUPPRESS_SUBTASK_LABEL = 1 << 1;
	/**
	 * Style constant indicating that the main task label 
	 * should be prepended to the subtask label.
	 *
	 * @see #SubProgressMonitor(IProgressMonitor,int,int)
	 */
	public static final int PREPEND_MAIN_LABEL_TO_SUBTASK = 1 << 2;

	private int parentTicks = 0;
	private double sentToParent = 0.0;
	private double scale = 0.0;
	private int nestedBeginTasks = 0;
	private boolean usedUp = false;
	private boolean hasSubTask = false;
	private int style;
	private String mainTaskLabel;

	/**
	 * Creates a new sub-progress monitor for the given monitor. The sub 
	 * progress monitor uses the given number of work ticks from its 
	 * parent monitor.
	 *
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 */
	public SubProgressMonitor(IProgressMonitor monitor, int ticks) {
		this(monitor, ticks, 0);
	}

	/**
	 * Creates a new sub-progress monitor for the given monitor. The sub 
	 * progress monitor uses the given number of work ticks from its 
	 * parent monitor.
	 *
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 * @param style one of
	 *    <ul>
	 *    <li> <code>SUPPRESS_SUBTASK_LABEL</code> </li>
	 *    <li> <code>PREPEND_MAIN_LABEL_TO_SUBTASK</code> </li>
	 *    </ul>
	 * @see #SUPPRESS_SUBTASK_LABEL
	 * @see #PREPEND_MAIN_LABEL_TO_SUBTASK
	 */
	public SubProgressMonitor(IProgressMonitor monitor, int ticks, int style) {
		super(monitor);
		this.parentTicks = (ticks > 0) ? ticks : 0;
		this.style = style;
	}

	/**
	 *
	 * Starts a new main task. Since this progress monitor is a sub
	 * progress monitor, the given name will NOT be used to update
	 * the progress bar's main task label. That means the given 
	 * string will be ignored. If style <code>PREPEND_MAIN_LABEL_TO_SUBTASK
	 * </code> is specified, then the given string will be prepended to
	 * every string passed to <code>subTask(String)</code>.
	 */
	@Override
	public void beginTask(String name, int totalWork) {
		nestedBeginTasks++;
		// Ignore nested begin task calls.
		if (nestedBeginTasks > 1) {
			return;
		}
		// be safe:  if the argument would cause math errors (zero or 
		// negative), just use 0 as the scale.  This disables progress for
		// this submonitor. 
		scale = totalWork <= 0 ? 0 : (double) parentTicks / (double) totalWork;
		if ((style & PREPEND_MAIN_LABEL_TO_SUBTASK) != 0) {
			mainTaskLabel = name;
		}
	}

	@Override
	public void done() {
		// Ignore if more done calls than beginTask calls or if we are still
		// in some nested beginTasks
		if (nestedBeginTasks == 0 || --nestedBeginTasks > 0)
			return;
		// Send any remaining ticks and clear out the subtask text
		double remaining = parentTicks - sentToParent;
		if (remaining > 0)
			super.internalWorked(remaining);
		//clear the sub task if there was one
		if (hasSubTask)
			subTask(""); //$NON-NLS-1$
		sentToParent = 0;
	}

	@Override
	public void internalWorked(double work) {
		if (usedUp || nestedBeginTasks != 1) {
			return;
		}

		double realWork = (work > 0.0d) ? scale * work : 0.0d;
		super.internalWorked(realWork);
		sentToParent += realWork;
		if (sentToParent >= parentTicks) {
			usedUp = true;
		}
	}

	@Override
	public void subTask(String name) {
		if ((style & SUPPRESS_SUBTASK_LABEL) != 0) {
			return;
		}
		hasSubTask = true;
		String label = name;
		if ((style & PREPEND_MAIN_LABEL_TO_SUBTASK) != 0 && mainTaskLabel != null && mainTaskLabel.length() > 0) {
			label = mainTaskLabel + ' ' + label;
		}
		super.subTask(label);
	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}
}
