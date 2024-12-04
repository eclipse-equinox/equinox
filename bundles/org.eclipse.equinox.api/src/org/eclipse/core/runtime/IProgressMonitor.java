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
 *     Christoph Laeubrich 	- [Bug 565924] join with IProgressMonitorWithBlocking, add null aware helper methods
 *     						- [Bug 567808] - add support for slicing a monitor
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * The <code>IProgressMonitor</code> interface is implemented by objects that
 * monitor the progress of an activity; the methods in this interface are
 * invoked by code that performs the activity.
 * <p>
 * All activity is broken down into a linear sequence of tasks against which
 * progress is reported. When a task begins, a <code>beginTask(String, int)
 * </code> notification is reported, followed by any number and mixture of
 * progress reports (<code>worked()</code>) and subtask notifications
 * (<code>subTask(String)</code>). When the task is eventually completed, a
 * <code>done()</code> notification is reported. After the <code>done()</code>
 * notification, the progress monitor cannot be reused; i.e., <code>
 * beginTask(String, int)</code> cannot be called again after the call to
 * <code>done()</code>.
 * </p>
 * <p>
 * A request to cancel an operation can be signaled using the
 * <code>setCanceled</code> method. Operations taking a progress monitor are
 * expected to poll the monitor (using <code>isCanceled</code>) periodically and
 * abort at their earliest convenience. Operation can however choose to ignore
 * cancelation requests.
 * </p>
 * <p>
 * Since notification is synchronous with the activity itself, the listener
 * should provide a fast and robust implementation. If the handling of
 * notifications would involve blocking operations, or operations which might
 * throw uncaught exceptions, the notifications should be queued, and the actual
 * processing deferred (or perhaps delegated to a separate thread).
 * </p>
 * <p>
 * <b>CALLER/CALLEE RESPONSIBILITIES:</b>
 * </p>
 * <p>
 * Methods that receive an {@link IProgressMonitor} ("callees") must either obey
 * the following conventions or include JavaDoc explaining how it differs from
 * these rules. The called method:
 * <ul>
 * <li>Will call {@link #beginTask} on the argument 0 or 1 times, at its
 * option.</li>
 * <li>Will not promise to invoke {@link #done()} on the monitor.</li>
 * <li>Will not call {@link #setCanceled} on the monitor.</li>
 * <li>May rely on the monitor not being null.</li>
 * <li>May rely on the monitor ignoring the string passed to
 * {@link #beginTask}.</li>
 * </ul>
 * <p>
 * The caller:
 * <ul>
 * <li>Will either pass in a fresh instance of {@link IProgressMonitor} that has
 * not had {@link #beginTask} invoked on it yet, or will select an
 * implementation of {@link IProgressMonitor} that explicitly permits multiple
 * calls to {@link #beginTask}.</li>
 * <li>Will not rely on the callee to invoke {@link #done()} on the monitor. It
 * must either select an implementation of {@link IProgressMonitor} that does
 * not require {@link #done()} to be called, for example {@link SubMonitor}, or
 * it must invoke {@link #done()} itself after the method returns.</li>
 * <li>Will not pass in a null monitor unless the JavaDoc of the callee says
 * that it accepts null.</li>
 * <li>Will pass in a monitor that ignores the name argument to
 * {@link #beginTask} unless the JavaDoc for the callee states otherwise.
 * </ul>
 * 
 * <p>
 * The responsibilities described above were introduced in Eclipse 4.7 (Oxygen).
 * Prior to Eclipse 4.7, it was common practice for the callee to invoke
 * {@link #done()} on its monitor and for the caller to rely upon this fact. As
 * of Eclipse 4.6, all the important top-level entry points have been updated to
 * call {@link #done()}, meaning they work with both methods that invoke
 * {@link #done()} and methods that don't.
 * </p>
 * <p>
 * Since this convention was introduced in Eclipse 4.7, some plugin code may
 * need to change. In particular, callers that pass a monitor are no longer
 * allowed to rely upon the callee invoking {@link #done()} and must do so
 * themselves if necessary.
 * </p>
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IProgressMonitor {

	/**
	 * Constant indicating an unknown amount of work.
	 */
	public final static int UNKNOWN = -1;

	/**
	 * Notifies that the main task is beginning. This must only be called once on a
	 * given progress monitor instance.
	 * 
	 * @param name      the name (or description) of the main task
	 * @param totalWork the total number of work units into which the main task is
	 *                  been subdivided. If the value is <code>UNKNOWN</code> the
	 *                  implementation is free to indicate progress in a way which
	 *                  doesn't require the total number of work units in advance.
	 */
	public void beginTask(String name, int totalWork);

	/**
	 * Notifies that the work is done; that is, either the main task is completed or
	 * the user canceled it. This method may be called more than once
	 * (implementations should be prepared to handle this case).
	 */
	public void done();

	/**
	 * Internal method to handle scaling correctly. This method must not be called
	 * by a client. Clients should always use the method <code>worked(int)</code>.
	 * 
	 * @param work the amount of work done
	 */
	public void internalWorked(double work);

	/**
	 * Returns whether cancelation of current operation has been requested.
	 * Long-running operations should poll to see if cancelation has been requested.
	 *
	 * @return <code>true</code> if cancellation has been requested, and
	 *         <code>false</code> otherwise
	 * @see #setCanceled(boolean)
	 */
	public boolean isCanceled();

	/**
	 * Sets the cancel state to the given value.
	 * 
	 * @param value <code>true</code> indicates that cancelation has been requested
	 *              (but not necessarily acknowledged); <code>false</code> clears
	 *              this flag
	 * @see #isCanceled()
	 */
	public void setCanceled(boolean value);

	/**
	 * Sets the task name to the given value. This method is used to restore the
	 * task label after a nested operation was executed. Normally there is no need
	 * for clients to call this method.
	 *
	 * @param name the name (or description) of the main task
	 * @see #beginTask(java.lang.String, int)
	 */
	public void setTaskName(String name);

	/**
	 * Notifies that a subtask of the main task is beginning. Subtasks are optional;
	 * the main task might not have subtasks.
	 *
	 * @param name the name (or description) of the subtask
	 */
	public void subTask(String name);

	/**
	 * Notifies that a given number of work unit of the main task has been
	 * completed. Note that this amount represents an installment, as opposed to a
	 * cumulative amount of work done to date.
	 *
	 * @param work a non-negative number of work units just completed
	 */
	public void worked(int work);

	/**
	 * Indicates that this operation is blocked by some background activity. If a
	 * running operation ever calls <code>setBlocked</code>, it must eventually call
	 * <code>clearBlocked</code> before the operation completes.
	 * <p>
	 * If the caller is blocked by a currently executing job, this method will
	 * return an <code>IJobStatus</code> indicating the job that is currently
	 * blocking the caller. If this blocking job is not known, this method will
	 * return a plain informational <code>IStatus</code> object.
	 * </p>
	 * 
	 * @param reason an optional status object whose message describes the reason
	 *               why this operation is blocked, or <code>null</code> if this
	 *               information is not available.
	 * @see #clearBlocked()
	 * @since 3.13
	 */
	public default void setBlocked(IStatus reason) {
		// default implementation does nothing
	}

	/**
	 * Clears the blocked state of the running operation. If a running operation
	 * ever calls <code>setBlocked</code>, it must eventually call
	 * <code>clearBlocked</code> before the operation completes.
	 * 
	 * @see #setBlocked(IStatus)
	 * @since 3.13
	 */
	public default void clearBlocked() {
		// default implementation does nothing
	}

	/**
	 * This method creates a slice out of this monitor. The slice behaves as if a
	 * new monitor instance is created that simply reports work back to its parent
	 * monitor. Even though it is safe to pass the sliced instance to another
	 * thread, instance itself might not be thread-safe and each slice should
	 * therefore only be used by one thread at once. To account for this, if sliced
	 * instances are passed to another thread, only sliced instances should be used
	 * like in this example:
	 * 
	 * <pre>
	 * IProgressMonitor monitor = ...
	 * 
	 * processAsync(monitor.slice(70));
	 * monitor = monitor.slice(30); // get a local slice so we can use the monitor
	 * 								// without interference with the async processing
	 * monitor.beginTask("Working on private slice", 1);
	 * ...
	 * monitor.worked(1);           // this is now safe to be called further on
	 * ...
	 * monitor.done();				// mark our part as done, 
	 * 								// the other slice will be finished by processAsync(...)
	 * 								// ... and the original monitor by the caller of this method
	 * 
	 * </pre>
	 * 
	 * The caller of this method (or the Thread that gets this instance passed) is
	 * responsible to make sure that {@link #done()} is called once the monitor is
	 * no longer needed.
	 * 
	 * @param work the amount of work for this {@link IProgressMonitor} to slice
	 * @return a {@link IProgressMonitor} slice for the given amount, the default
	 *         implementation suppress any strings passed to
	 *         {@link #setTaskName(String)}, {@link #subTask(String)} and
	 *         {@link #beginTask(String, int)}, and does not propagate
	 *         {@link #setCanceled(boolean)} (but reports cancelation of the parent
	 * @since 3.14
	 */
	public default IProgressMonitor slice(int work) {
		return new SlicedProgressMonitor(this, work);
	}

	/**
	 * Calls {@link #done()} on the given monitor if is non-null. If the given
	 * monitor is null, this is a no-op.
	 * 
	 * @param monitor the monitor to make done, might be <code>null</code>
	 * @since 3.14
	 */
	static void done(IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.done();
		}
	}

	/**
	 * Returns a <code>null</code> safe access to the given monitor, for example in
	 * cases where a monitor is passed to a function the implementation can call
	 * this method to get a guaranteed non-null monitor reference
	 * 
	 * @param monitor the monitor to check
	 * @return the passed monitor instance or {@link NullProgressMonitor} if monitor
	 *         was <code>null</code>
	 * @since 3.14
	 */
	static IProgressMonitor nullSafe(IProgressMonitor monitor) {
		if (monitor == null) {
			return new NullProgressMonitor();
		}
		return monitor;
	}
}
