/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Xenos - initial API and implementation
 *     Stefan Xenos - bug 174539 - add a 1-argument convert(...) method     
 *     Stefan Xenos - bug 174040 - SubMonitor#convert doesn't always set task name
 *     Stefan Xenos - bug 206942 - updated javadoc to recommend better constants for infinite progress
 *     Stefan Xenos (Google) - bug 475747 - Support efficient, convenient cancellation checks in SubMonitor
 *     Stefan Xenos (Google) - bug 476924 - Add a SUPPRESS_ISCANCELED flag to SubMonitor
 *     IBM Corporation - ongoing maintenance
 *     Christoph Laeubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.api.internal.APISupport;
import org.eclipse.equinox.api.internal.TracingOptions;

/**
 * <p>
 * A progress monitor that uses a given amount of work ticks from a parent
 * monitor. This is intended as a safer, easier-to-use alternative to
 * SubProgressMonitor. The main benefits of SubMonitor over SubProgressMonitor
 * are:
 * </p>
 * <ul>
 * <li>It is not necessary to call beginTask() or done() on an instance of
 * SubMonitor.</li>
 * <li>SubMonitor has a simpler syntax for creating nested monitors.</li>
 * <li>SubMonitor is more efficient for deep recursion chains.</li>
 * <li>SubMonitor has a setWorkRemaining method that allows the remaining space
 * on the monitor to be redistributed without reporting any work.</li>
 * <li>SubMonitor protects the caller from common progress reporting bugs in a
 * called method. For example, if a called method fails to call done() on the
 * given monitor or fails to consume all the ticks on the given monitor, the
 * parent will correct the problem after the method returns.</li>
 * </ul>
 * <p>
 * <b>USAGE:</b>
 * </p>
 * 
 * <p>
 * When implementing a method that accepts an IProgressMonitor:
 * </p>
 * <ul>
 * <li>At the start of your method, use <code>SubMonitor.convert(...).</code> to
 * convert the IProgressMonitor into a SubMonitor.</li>
 * <li>Use <code>SubMonitor.split(...)</code> whenever you need to call another
 * method that accepts an IProgressMonitor.</li>
 * </ul>
 * 
 * <p>
 * <b>Example: Recommended usage</b>
 * </p>
 *
 * <p>
 * This example demonstrates how the recommended usage of
 * <code>SubMonitor</code> makes it unnecessary to call IProgressMonitor.done()
 * in most situations.
 * </p>
 * 
 * <p>
 * It is never necessary to call done() on a monitor obtained from
 * <code>convert</code> or <code>progress.split()</code>. In this example, there
 * is no guarantee that <code>monitor</code> is an instance of
 * <code>SubMonitor</code>, making it necessary to call
 * <code>monitor.done()</code>. The JavaDoc contract makes this the
 * responsibility of the caller.
 * </p>
 * 
 * <pre>
 *      // {@literal @}param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
 *      //        to call done() on the given monitor. Accepts <code>null</code>, indicating that no progress should be
 *      //        reported and that the operation cannot be cancelled.
 *      //
 *      void doSomething(IProgressMonitor monitor) {
 *          // Convert the given monitor into a progress instance 
 *          SubMonitor progress = SubMonitor.convert(monitor, 100);
 *              
 *          // Use 30% of the progress to do some work
 *          doSomeWork(progress.split(30));
 *          
 *          // Advance the monitor by another 30%
 *          progress.split(30);
 *          
 *          // Use the remaining 40% of the progress to do some more work
 *          doSomeWork(progress.split(40)); 
 *      }
 * </pre>
 * 
 * <p>
 * <b>Example: Branches</b>
 * </p>
 *
 * <p>
 * This example demonstrates how to smoothly report progress in situations where
 * some of the work is optional.
 * </p>
 * 
 * <pre>
 * void doSomething(IProgressMonitor monitor) {
 * 	SubMonitor progress = SubMonitor.convert(monitor, 100);
 * 
 * 	if (condition) {
 * 		// Use 50% of the progress to do some work
 * 		doSomeWork(progress.split(50));
 * 	}
 * 
 * 	// Don't report any work, but ensure that we have 50 ticks remaining on the
 * 	// progress monitor.
 * 	// If we already consumed 50 ticks in the above branch, this is a no-op.
 * 	// Otherwise, the remaining
 * 	// space in the monitor is redistributed into 50 ticks.
 * 
 * 	progress.setWorkRemaining(50);
 * 
 * 	// Use the remainder of the progress monitor to do the rest of the work
 * 	doSomeWork(progress.split(50));
 * }
 * </pre>
 * 
 * <p>
 * Please beware of the following anti-pattern:
 * </p>
 *
 * <pre>
 * if (condition) {
 * 	// Use 50% of the progress to do some work
 * 	doSomeWork(progress.split(50));
 * } else {
 * 	// Bad: Causes the progress monitor to appear to start at 50%, wasting half
 * 	// of the
 * 	// space in the monitor.
 * 	progress.worked(50);
 * }
 * </pre>
 * 
 * 
 * <p>
 * <b>Example: Loops</b>
 * </p>
 *
 * <p>
 * This example demonstrates how to report progress in a loop.
 * </p>
 * 
 * <pre>
 * void doSomething(IProgressMonitor monitor, Collection someCollection) {
 * 	SubMonitor progress = SubMonitor.convert(monitor, 100);
 *
 * 	// Create a new progress monitor that uses 70% of the total progress and will
 * 	// allocate one tick
 * 	// for each element of the given collection.
 * 	SubMonitor loopProgress = progress.split(70).setWorkRemaining(someCollection.size());
 * 
 * 	for (Iterator iter = someCollection.iterator(); iter.hasNext();) {
 * 		Object next = iter.next();
 * 
 * 		doWorkOnElement(next, loopProgress.split(1));
 * 	}
 * 
 * 	// Use the remaining 30% of the progress monitor to do some work outside the
 * 	// loop
 * 	doSomeWork(progress.split(30));
 * }
 * </pre>
 *
 *
 * <p>
 * <b>Example: Infinite progress</b>
 * </p>
 * 
 * <p>
 * This example demonstrates how to report logarithmic progress in situations
 * where the number of ticks cannot be easily computed in advance.
 * </p>
 * 
 * <pre>
 * void doSomething(IProgressMonitor monitor, LinkedListNode node) {
 * 	SubMonitor progress = SubMonitor.convert(monitor);
 * 
 * 	while (node != null) {
 * 		// Regardless of the amount of progress reported so far,
 * 		// use 0.01% of the space remaining in the monitor to process the next node.
 * 		progress.setWorkRemaining(10000);
 * 
 * 		doWorkOnElement(node, progress.split(1));
 * 
 * 		node = node.next;
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * 
 * @since org.eclipse.equinox.common 3.3
 */
public final class SubMonitor implements IProgressMonitorWithBlocking {

	/**
	 * Number of trivial split operations (operations which do not report any
	 * progress) which can be performed before the monitor performs a cancellation
	 * check. This ensures that cancellation checks do not create a performance
	 * problem in tight loops that create a lot of SubMonitors, while still ensuring
	 * that cancellation is checked occasionally in such loops. This only affects
	 * operations which are too small to report any progress. Operations which are
	 * large enough to consume at least one tick will always be checked for
	 * cancellation.
	 */
	private static final int TRIVIAL_SPLITS_BEFORE_CANCELLATION_CHECK = 20;

	/**
	 * The limit for {@link RootInfo#cancellationCheckCounter} before performing a
	 * cancellation check.
	 */
	private static final int TRIVIAL_OPERATION_COUNT_LIMIT = TRIVIAL_SPLITS_BEFORE_CANCELLATION_CHECK;

	/**
	 * Amount to increment {@link RootInfo#cancellationCheckCounter} when performing
	 * a trivial {@link #split(int)} operation.
	 */
	private static final int TRIVIAL_SPLIT_DELTA = 1;

	/**
	 * Minimum number of ticks to allocate when calling beginTask on an unknown
	 * IProgressMonitor. Pick a number that is big enough such that, no matter where
	 * progress is being displayed, the user would be unlikely to notice if progress
	 * were to be reported with higher accuracy.
	 */
	private static final int MINIMUM_RESOLUTION = 1000;

	/**
	 * The RootInfo holds information about the root progress monitor. A SubMonitor
	 * and its active descendants share the same RootInfo.
	 */
	private static final class RootInfo {
		final IProgressMonitor root;

		/**
		 * Remembers the last task name. Prevents us from setting the same task name
		 * multiple times in a row.
		 */
		String taskName;

		/**
		 * Remembers the last subtask name. Prevents the SubMonitor from setting the
		 * same subtask string more than once in a row.
		 */
		String subTask;

		/**
		 * Counter that indicates when we should perform an cancellation check for a
		 * trivial operation.
		 */
		int cancellationCheckCounter;

		/**
		 * Creates a RootInfo structure that delegates to the given progress monitor.
		 *
		 * @param root progress monitor to delegate to
		 */
		public RootInfo(IProgressMonitor root) {
			this.root = root;
		}

		public boolean isCanceled() {
			return root.isCanceled();
		}

		public void setCanceled(boolean value) {
			root.setCanceled(value);
		}

		public void setTaskName(String taskName) {
			if (eq(taskName, this.taskName)) {
				return;
			}
			this.taskName = taskName;
			root.setTaskName(taskName);
		}

		public void subTask(String name) {
			if (eq(subTask, name)) {
				return;
			}

			this.subTask = name;
			root.subTask(name);
		}

		public void worked(int i) {
			root.worked(i);
		}

		public void clearBlocked() {
			root.clearBlocked();
		}

		public void setBlocked(IStatus reason) {

			root.setBlocked(reason);
		}

		public void checkForCancellation() {
			if (root.isCanceled()) {
				throw new OperationCanceledException();
			}
		}

		public void reportTrivialOperation(int cancellationDelta) {
			cancellationCheckCounter += cancellationDelta;
			// This is a trivial operation. Only perform a cancellation check after the
			// counter expires.
			if (cancellationCheckCounter >= TRIVIAL_OPERATION_COUNT_LIMIT) {
				cancellationCheckCounter = 0;
				checkForCancellation();
			}
		}
	}

	/**
	 * Total number of ticks that this progress monitor is permitted to consume from
	 * the root.
	 */
	private int totalParent;

	/**
	 * Number of ticks that this progress monitor has already reported in the root.
	 */
	private int usedForParent = 0;

	/**
	 * Number of ticks that have been consumed by this instance's children.
	 */
	private double usedForChildren = 0.0;

	/**
	 * Number of ticks allocated for this instance's children. This is the total
	 * number of ticks that may be passed into worked(int) or split(int).
	 */
	private int totalForChildren;

	/**
	 * Children created by split will be completed automatically the next time the
	 * parent progress monitor is touched. This points to the last incomplete child
	 * created with split.
	 */
	private SubMonitor lastSubMonitor;

	/**
	 * Used to communicate with the root of this progress monitor tree
	 */
	private final RootInfo root;

	/**
	 * A bitwise combination of the SUPPRESS_* flags.
	 */
	private final int flags;

	/**
	 * True iff beginTask has been called on the public interface yet.
	 */
	private boolean beginTaskCalled;

	/**
	 * True iff ticks have been allocated yet.
	 */
	private boolean ticksAllocated;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that the calls to
	 * subTask on the child should be ignored. Without this flag, calling subTask on
	 * the child will result in a call to subTask on its parent.
	 */
	public static final int SUPPRESS_SUBTASK = 0x0001;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that strings passed into
	 * beginTask should be ignored. If this flag is specified, then the progress
	 * monitor instance will accept null as the first argument to beginTask. Without
	 * this flag, any string passed to beginTask will result in a call to
	 * setTaskName on the parent.
	 */
	public static final int SUPPRESS_BEGINTASK = 0x0002;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that strings passed into
	 * setTaskName should be ignored. If this string is omitted, then a call to
	 * setTaskName on the child will result in a call to setTaskName on the parent.
	 */
	public static final int SUPPRESS_SETTASKNAME = 0x0004;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that isCanceled should
	 * always return false.
	 * 
	 * @since 3.8
	 */
	public static final int SUPPRESS_ISCANCELED = 0x0008;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that strings passed to
	 * setTaskName, subTask, and beginTask should all be ignored.
	 */
	public static final int SUPPRESS_ALL_LABELS = SUPPRESS_SETTASKNAME | SUPPRESS_BEGINTASK | SUPPRESS_SUBTASK;

	/**
	 * May be passed as a flag to {@link #split}. Indicates that strings passed to
	 * setTaskName, subTask, and beginTask should all be propagated to the parent.
	 */
	public static final int SUPPRESS_NONE = 0;

	/**
	 * Bitwise combination of all flags which may be passed in to the public
	 * interface on {@link #split}
	 */
	private static final int ALL_PUBLIC_FLAGS = SUPPRESS_ALL_LABELS | SUPPRESS_ISCANCELED;

	/**
	 * Bitwise combination of all flags which are inherited directly from a parent
	 * SubMonitor to its immediate children. All other flags are either not
	 * inherited or are inherited from more complicated logic in {@link #split}
	 */
	private static final int ALL_INHERITED_FLAGS = SUPPRESS_SUBTASK | SUPPRESS_ISCANCELED;

	private static final Set<String> knownBuggyMethods = new HashSet<>();

	/**
	 * Creates a new SubMonitor that will report its progress via the given
	 * RootInfo.
	 * 
	 * @param rootInfo            the root of this progress monitor tree
	 * @param totalWork           total work to perform on the given progress
	 *                            monitor
	 * @param availableToChildren number of ticks allocated for this instance's
	 *                            children
	 * @param flags               a bitwise combination of the SUPPRESS_* constants
	 */
	private SubMonitor(RootInfo rootInfo, int totalWork, int availableToChildren, int flags) {
		root = rootInfo;
		totalParent = (totalWork > 0) ? totalWork : 0;
		this.totalForChildren = availableToChildren;
		this.flags = flags;
		ticksAllocated = availableToChildren > 0;
	}

	/**
	 * <p>
	 * Converts an unknown (possibly null) IProgressMonitor into a SubMonitor. It is
	 * not necessary to call done() on the result, but the caller is responsible for
	 * calling done() on the argument. Calls beginTask on the argument.
	 * </p>
	 * 
	 * <p>
	 * This method should generally be called at the beginning of a method that
	 * accepts an IProgressMonitor in order to convert the IProgressMonitor into a
	 * SubMonitor.
	 * </p>
	 * 
	 * <p>
	 * Since it is illegal to call beginTask on the same IProgressMonitor more than
	 * once, the same instance of IProgressMonitor must not be passed to convert
	 * more than once.
	 * </p>
	 * 
	 * @param monitor monitor to convert to a SubMonitor instance or null. Treats
	 *                null as a new instance of <code>NullProgressMonitor</code>.
	 * @return a SubMonitor instance that adapts the argument
	 */
	public static SubMonitor convert(IProgressMonitor monitor) {
		return convert(monitor, "", 0); //$NON-NLS-1$
	}

	/**
	 * <p>
	 * Converts an unknown (possibly null) IProgressMonitor into a SubMonitor
	 * allocated with the given number of ticks. It is not necessary to call done()
	 * on the result, but the caller is responsible for calling done() on the
	 * argument. Calls beginTask on the argument.
	 * </p>
	 * 
	 * <p>
	 * This method should generally be called at the beginning of a method that
	 * accepts an IProgressMonitor in order to convert the IProgressMonitor into a
	 * SubMonitor.
	 * </p>
	 *
	 * <p>
	 * Since it is illegal to call beginTask on the same IProgressMonitor more than
	 * once, the same instance of IProgressMonitor must not be passed to convert
	 * more than once.
	 * </p>
	 * 
	 * @param monitor monitor to convert to a SubMonitor instance or null. Treats
	 *                null as a new instance of <code>NullProgressMonitor</code>.
	 * @param work    number of ticks that will be available in the resulting
	 *                monitor
	 * @return a SubMonitor instance that adapts the argument
	 */
	public static SubMonitor convert(IProgressMonitor monitor, int work) {
		return convert(monitor, "", work); //$NON-NLS-1$
	}

	/**
	 * <p>
	 * Converts an unknown (possibly null) IProgressMonitor into a SubMonitor
	 * allocated with the given number of ticks. It is not necessary to call done()
	 * on the result, but the caller is responsible for calling done() on the
	 * argument. Calls beginTask on the argument.
	 * </p>
	 * 
	 * <p>
	 * This method should generally be called at the beginning of a method that
	 * accepts an IProgressMonitor in order to convert the IProgressMonitor into a
	 * SubMonitor.
	 * </p>
	 *
	 * <p>
	 * Since it is illegal to call beginTask on the same IProgressMonitor more than
	 * once, the same instance of IProgressMonitor must not be passed to convert
	 * more than once.
	 * </p>
	 * 
	 * @param monitor  to convert into a SubMonitor instance or null. If given a
	 *                 null argument, the resulting SubMonitor will not report its
	 *                 progress anywhere.
	 * @param taskName user readable name to pass to monitor.beginTask. Never null.
	 * @param work     initial number of ticks to allocate for children of the
	 *                 SubMonitor
	 * @return a new SubMonitor instance that is a child of the given monitor
	 */
	public static SubMonitor convert(IProgressMonitor monitor, String taskName, int work) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
			return new SubMonitor(new RootInfo(monitor), 0, work, SUPPRESS_ALL_LABELS);
		}

		// Optimization: if the given monitor already a SubMonitor, no conversion is
		// necessary
		if (monitor instanceof SubMonitor) {
			SubMonitor subMonitor = (SubMonitor) monitor;
			subMonitor.beginTaskImpl(taskName, work);
			return subMonitor;
		}

		monitor.beginTask(taskName, MINIMUM_RESOLUTION);
		return new SubMonitor(new RootInfo(monitor), MINIMUM_RESOLUTION, work, SUPPRESS_NONE);
	}

	/**
	 * Calls {@link #done()} on the given monitor if is non-null. If the given
	 * monitor is null, this is a no-op.
	 * <p>
	 * This is a convenience method intended to reduce the boilerplate around code
	 * which must call {@link #done()} on a possibly-null monitor.
	 * 
	 * @param monitor a progress monitor or null
	 * @since 3.8
	 */
	public static void done(IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.done();
		}
	}

	/**
	 * <p>
	 * Sets the work remaining for this SubMonitor instance. This is the total
	 * number of ticks that may be reported by all subsequent calls to worked(int),
	 * split(int), etc. This may be called many times for the same SubMonitor
	 * instance. When this method is called, the remaining space on the progress
	 * monitor is redistributed into the given number of ticks.
	 * </p>
	 * 
	 * <p>
	 * It doesn't matter how much progress has already been reported with this
	 * SubMonitor instance. If you call setWorkRemaining(100), you will be able to
	 * report 100 more ticks of work before the progress meter reaches 100%.
	 * </p>
	 * 
	 * @param workRemaining total number of remaining ticks
	 * @return the receiver
	 */
	public SubMonitor setWorkRemaining(int workRemaining) {
		if (TracingOptions.debugProgressMonitors && ticksAllocated && usedForChildren >= totalForChildren
				&& workRemaining > 0) {
			logProblem("Attempted to allocate ticks on a SubMonitor which had no space available. " //$NON-NLS-1$
					+ "This may indicate that a SubMonitor was reused inappropriately (which is a bug) " //$NON-NLS-1$
					+ "or may indicate that the caller was implementing infinite progress and overflowed " //$NON-NLS-1$
					+ "(which may not be a bug but may require selecting a higher ratio)"); //$NON-NLS-1$
		}

		// Ensure we don't try to allocate negative ticks
		if (workRemaining > 0) {
			ticksAllocated = true;
		} else {
			workRemaining = 0;
		}

		// Ensure we don't cause division by zero
		if (totalForChildren > 0 && totalParent > usedForParent) {
			// Note: We want the following value to remain invariant after this method
			// returns
			double remainForParent = totalParent * (1.0d - (usedForChildren / totalForChildren));
			usedForChildren = (workRemaining * (1.0d - remainForParent / (totalParent - usedForParent)));
		} else
			usedForChildren = 0.0d;

		totalParent = totalParent - usedForParent;
		usedForParent = 0;
		totalForChildren = workRemaining;
		return this;
	}

	/**
	 * Consumes the given number of child ticks, given as a double. Must only be
	 * called if the monitor is in floating-point mode.
	 * 
	 * @param ticks the number of ticks to consume
	 * @return ticks the number of ticks to be consumed from parent
	 */
	private int consume(double ticks) {
		if (TracingOptions.debugProgressMonitors && !ticksAllocated && ticks > 0) {
			logProblem("You must allocate ticks using beginTask or setWorkRemaining before trying to consume them"); //$NON-NLS-1$
		}

		if (totalParent == 0 || totalForChildren == 0) // this monitor has no available work to report
			return 0;

		usedForChildren += ticks;

		if (usedForChildren > totalForChildren) {
			usedForChildren = totalForChildren;
			if (TracingOptions.debugProgressMonitors) {
				logProblem("This progress monitor consumed more ticks than were allocated for it."); //$NON-NLS-1$
			}
		} else if (usedForChildren < 0.0)
			usedForChildren = 0.0;

		int parentPosition = (int) (totalParent * usedForChildren / totalForChildren);
		int delta = parentPosition - usedForParent;

		usedForParent = parentPosition;
		return delta;
	}

	@Override
	public boolean isCanceled() {
		if ((flags & SUPPRESS_ISCANCELED) == 0) {
			return root.isCanceled();
		}
		return false;
	}

	/**
	 * Checks whether cancellation of current operation has been requested and
	 * throws an {@link OperationCanceledException} if it was the case. This method
	 * is a shorthand for:
	 * 
	 * <pre>
	 * if (monitor.isCanceled())
	 * 	throw new OperationCanceledException();
	 * </pre>
	 *
	 * @return this SubMonitor to allow for chained invocation
	 * @throws OperationCanceledException if cancellation has been requested
	 * @see #isCanceled()
	 * @since 3.9
	 */
	public SubMonitor checkCanceled() throws OperationCanceledException {
		if (isCanceled()) {
			throw new OperationCanceledException();
		}
		return this;
	}

	@Override
	public void setTaskName(String name) {
		if ((flags & SUPPRESS_SETTASKNAME) == 0)
			root.setTaskName(name);
	}

	/**
	 * Starts a new main task. The string argument is ignored if and only if the
	 * SUPPRESS_BEGINTASK flag has been set on this SubMonitor instance.
	 * 
	 * <p>
	 * This method is equivalent calling setWorkRemaining(...) on the receiver.
	 * Unless the SUPPRESS_BEGINTASK flag is set, this will also be equivalent to
	 * calling setTaskName(...) on the parent.
	 * </p>
	 * 
	 * @param name      new main task name
	 * @param totalWork number of ticks to allocate
	 * 
	 * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(java.lang.String,
	 *      int)
	 */
	@Override
	public void beginTask(String name, int totalWork) {
		if (TracingOptions.debugProgressMonitors && beginTaskCalled) {
			logProblem("beginTask was called on this instance more than once"); //$NON-NLS-1$
		}
		beginTaskImpl(name, totalWork);
	}

	private void beginTaskImpl(String name, int totalWork) {
		if ((flags & SUPPRESS_BEGINTASK) == 0 && name != null)
			root.setTaskName(name);
		setWorkRemaining(totalWork);
		beginTaskCalled = true;
	}

	@Override
	public void done() {
		cleanupActiveChild();
		int delta = totalParent - usedForParent;
		if (delta > 0)
			root.worked(delta);

		totalParent = 0;
		usedForParent = 0;
		totalForChildren = 0;
		usedForChildren = 0.0d;
	}

	@Override
	public void internalWorked(double work) {
		cleanupActiveChild();

		int delta = consume((work > 0.0d) ? work : 0.0d);
		if (delta != 0)
			root.worked(delta);
	}

	@Override
	public void subTask(String name) {
		if ((flags & SUPPRESS_SUBTASK) == 0)
			root.subTask(name);
	}

	@Override
	public void worked(int work) {
		if (TracingOptions.debugProgressMonitors && work == 0) {
			logProblem("Attempted to report 0 ticks of work"); //$NON-NLS-1$
		}

		internalWorked(work);
	}

	@Override
	public void setCanceled(boolean b) {
		root.setCanceled(b);
	}

	/**
	 * <p>
	 * Creates a new SubMonitor that will consume the given number of ticks from its
	 * parent. Shorthand for calling {@link #newChild(int, int)} with (totalWork,
	 * SUPPRESS_BEGINTASK).
	 * 
	 * <p>
	 * This is much like {@link #split(int)} but it does not check for cancellation
	 * and will not throw {@link OperationCanceledException}. New code should
	 * consider using {@link #split(int)} to benefit from automatic cancellation
	 * checks.
	 * 
	 * @param totalWork number of ticks to consume from the receiver
	 * @return new sub progress monitor that may be used in place of a new
	 *         SubMonitor
	 */
	public SubMonitor newChild(int totalWork) {
		return newChild(totalWork, SUPPRESS_BEGINTASK);
	}

	/**
	 * <p>
	 * This is much like {@link #split}, but it does not check for cancellation and
	 * will not throw {@link OperationCanceledException}. New code should consider
	 * using {@link #split} to benefit from automatic cancellation checks.
	 * 
	 * <p>
	 * Creates a sub progress monitor that will consume the given number of ticks
	 * from the receiver. It is not necessary to call <code>beginTask</code> or
	 * <code>done</code> on the result. However, the resulting progress monitor will
	 * not report any work after the first call to done() or before ticks are
	 * allocated. Ticks may be allocated by calling beginTask or setWorkRemaining.
	 * </p>
	 * 
	 * <p>
	 * Each SubMonitor only has one active child at a time. Each time newChild() is
	 * called, the result becomes the new active child and any unused progress from
	 * the previously-active child is consumed.
	 * </p>
	 * 
	 * <p>
	 * This is property makes it unnecessary to call done() on a SubMonitor
	 * instance, since child monitors are automatically cleaned up the next time the
	 * parent is touched.
	 * </p>
	 * 
	 * <pre>
	 *  <code>
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 1: Typical usage of newChild
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100); 
	 *          doSomething(progress.newChild(50));
	 *          doSomethingElse(progress.newChild(50));
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 2: Demonstrates the function of active children. Creating children
	 *      // is sufficient to smoothly report progress, even if worked(...) and done()
	 *      // are never called.
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          for (int i = 0; i &lt; 100; i++) {
	 *              // Creating the next child monitor will clean up the previous one,
	 *              // causing progress to be reported smoothly even if we don't do anything
	 *              // with the monitors we create
	 *          	progress.newChild(1);
	 *          }
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 3: Demonstrates a common anti-pattern
	 *      void wrongMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // WRONG WAY: Won't have the intended effect, as only one of these progress
	 *          // monitors may be active at a time and the other will report no progress.
	 *          callMethod(progress.newChild(50), computeValue(progress.newChild(50)));
	 *      }
	 *      
	 *      void rightMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // RIGHT WAY: Break up method calls so that only one SubMonitor is in use at a time.
	 *          Object someValue = computeValue(progress.newChild(50));
	 *          callMethod(progress.newChild(50), someValue);
	 *      }
	 * </code>
	 * </pre>
	 * 
	 * @param totalWork     number of ticks to consume from the receiver
	 * @param suppressFlags a bitwise combination of SUPPRESS_* flags. They can be
	 *                      used to suppress various behaviors on the newly-created
	 *                      monitor. Callers should generally include the
	 *                      {@link #SUPPRESS_BEGINTASK} flag unless they are
	 *                      invoking a method whose JavaDoc specifically states that
	 *                      the string argument to {@link #beginTask(String, int)}
	 *                      must be preserved.
	 * @return new sub progress monitor that may be used in place of a new
	 *         SubMonitor
	 */
	public SubMonitor newChild(int totalWork, int suppressFlags) {
		double totalWorkDouble = (totalWork > 0) ? totalWork : 0.0d;
		totalWorkDouble = Math.min(totalWorkDouble, totalForChildren - usedForChildren);
		SubMonitor oldActiveChild = lastSubMonitor;
		cleanupActiveChild();

		// Compute the flags for the child. We want the net effect to be as though the
		// child is
		// delegating to its parent, even though it is actually talking directly to the
		// root.
		// This means that we need to compute the flags such that - even if a label
		// isn't
		// suppressed by the child - if that same label would have been suppressed when
		// the
		// child delegated to its parent, the child must explicitly suppress the label.
		int childFlags = flags & ALL_INHERITED_FLAGS;

		if ((flags & SUPPRESS_SETTASKNAME) != 0) {
			// If the parent was ignoring labels passed to setTaskName, then the child will
			// ignore
			// labels passed to either beginTask or setTaskName - since both delegate to
			// setTaskName
			// on the parent
			childFlags |= SUPPRESS_SETTASKNAME | SUPPRESS_BEGINTASK;
		}

		// Note: the SUPPRESS_BEGINTASK flag does not affect the child since there
		// is no method on the child that would delegate to beginTask on the parent.
		childFlags |= (suppressFlags & ALL_PUBLIC_FLAGS);

		int consumed = consume(totalWorkDouble);

		if (TracingOptions.debugProgressMonitors) {
			if (totalWork == 0) {
				logProblem("Attempted to create a child without providing it with any ticks"); //$NON-NLS-1$
			}
		} else {
			// Only perform optimizations which reuse monitors if we're not debugging
			// progress monitors,
			// since reusing the monitors prevents us from tracking the usage of an
			// individual monitor
			// in any meaningful way.

			// If we're creating a new child that can't report any ticks and we just
			// consumed a previous
			// child, just reuse the previous child.
			if (consumed == 0 && oldActiveChild != null && childFlags == oldActiveChild.flags) {
				lastSubMonitor = oldActiveChild;
				return oldActiveChild;
			}

			// If the new child is going to consume the entire parent, return the parent
			// itself.
			if (usedForParent >= totalParent && childFlags == flags) {
				totalParent = consumed;
				usedForParent = 0;
				totalForChildren = 0;
				usedForChildren = 0;
				return this;
			}
		}

		SubMonitor result = new SubMonitor(root, consumed, 0, childFlags);
		lastSubMonitor = result;
		return result;
	}

	/**
	 * This is shorthand for calling
	 * <code>split(totalWork, SUPPRESS_BEGINTASK)</code>. See
	 * {@link #split(int, int)} for more details.
	 * 
	 * <p>
	 * Creates a sub progress monitor that will consume the given number of ticks
	 * from the receiver. It is not necessary to call <code>beginTask</code> or
	 * <code>done</code> on the result. However, the resulting progress monitor will
	 * not report any work after the first call to done() or before ticks are
	 * allocated. Ticks may be allocated by calling {@link #beginTask} or
	 * {@link #setWorkRemaining}.
	 * </p>
	 * 
	 * <p>
	 * This method is much like {@link #newChild}, but it will additionally check
	 * for cancellation and will throw an OperationCanceledException if the monitor
	 * has been cancelled. Not every call to this method will trigger a cancellation
	 * check. The checks will be performed as often as possible without degrading
	 * the performance of the caller.
	 * 
	 * <p>
	 * Each SubMonitor only has one active child at a time. Each time
	 * {@link #newChild} or {@link #split} is called, the result becomes the new
	 * active child and any unused progress from the previously-active child is
	 * consumed.
	 * </p>
	 * 
	 * <p>
	 * This makes it unnecessary to call done() on a SubMonitor instance, since
	 * child monitors are automatically cleaned up the next time the parent is
	 * touched.
	 * </p>
	 * 
	 * <pre>
	 * <code>
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 1: Typical usage of split
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100); 
	 *          doSomething(progress.split(50));
	 *          doSomethingElse(progress.split(50));
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 2: Demonstrates the function of active children. Creating children
	 *      // is sufficient to smoothly report progress, even if worked(...) and done()
	 *      // are never called.
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          for (int i = 0; i &lt; 100; i++) {
	 *              // Creating the next child monitor will clean up the previous one,
	 *              // causing progress to be reported smoothly even if we don't do anything
	 *              // with the monitors we create
	 *          	progress.split(1);
	 *          }
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 3: Demonstrates a common anti-pattern
	 *      void wrongMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // WRONG WAY: Won't have the intended effect, as only one of these progress
	 *          // monitors may be active at a time and the other will report no progress.
	 *          callMethod(progress.split(50), computeValue(progress.split(50)));
	 *      }
	 *      
	 *      void rightMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // RIGHT WAY: Break up method calls so that only one SubMonitor is in use at a time.
	 *          Object someValue = computeValue(progress.split(50));
	 *          callMethod(progress.split(50), someValue);
	 *      }
	 * </code>
	 * </pre>
	 * 
	 * @param totalWork number of ticks to consume from the receiver
	 * @return a new SubMonitor instance
	 * @throws OperationCanceledException if the monitor has been cancelled
	 * @since 3.8
	 */
	public SubMonitor split(int totalWork) throws OperationCanceledException {
		return split(totalWork, SUPPRESS_BEGINTASK);
	}

	/**
	 * <p>
	 * Creates a sub progress monitor that will consume the given number of ticks
	 * from the receiver. It is not necessary to call <code>beginTask</code> or
	 * <code>done</code> on the result. However, the resulting progress monitor will
	 * not report any work after the first call to done() or before ticks are
	 * allocated. Ticks may be allocated by calling {@link #beginTask} or
	 * {@link #setWorkRemaining}
	 * </p>
	 * 
	 * <p>
	 * This method is much like {@link #newChild}, but will additionally check for
	 * cancellation and will throw an {@link OperationCanceledException} if the
	 * monitor has been cancelled. Not every call to this method will trigger a
	 * cancellation check. The checks will be performed as often as possible without
	 * degrading the performance of the caller.
	 * 
	 * <p>
	 * Each SubMonitor only has one active child at a time. Each time
	 * {@link #newChild} or {@link #split} is called, the result becomes the new
	 * active child and any unused progress from the previously-active child is
	 * consumed.
	 * </p>
	 * 
	 * <p>
	 * This is property makes it unnecessary to call done() on a SubMonitor
	 * instance, since child monitors are automatically cleaned up the next time the
	 * parent is touched.
	 * </p>
	 * 
	 * <pre>
	 *  <code>
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 1: Typical usage of split
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100); 
	 *          doSomething(progress.split(50));
	 *          doSomethingElse(progress.split(50));
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 2: Demonstrates the function of active children. Creating children
	 *      // is sufficient to smoothly report progress, even if worked(...) and done()
	 *      // are never called.
	 *      void myMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          for (int i = 0; i &lt; 100; i++) {
	 *              // Creating the next child monitor will clean up the previous one,
	 *              // causing progress to be reported smoothly even if we don't do anything
	 *              // with the monitors we create
	 *          	progress.split(1);
	 *          }
	 *      }
	 *      
	 *      ////////////////////////////////////////////////////////////////////////////
	 *      // Example 3: Demonstrates a common anti-pattern
	 *      void wrongMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // WRONG WAY: Won't have the intended effect, as only one of these progress
	 *          // monitors may be active at a time and the other will report no progress.
	 *          callMethod(progress.split(50), computeValue(progress.split(50)));
	 *      }
	 *      
	 *      void rightMethod(IProgressMonitor parent) {
	 *          SubMonitor progress = SubMonitor.convert(parent, 100);
	 *          
	 *          // RIGHT WAY: Break up method calls so that only one SubMonitor is in use at a time.
	 *          Object someValue = computeValue(progress.split(50));
	 *          callMethod(progress.split(50), someValue);
	 *      }
	 * </code>
	 * </pre>
	 * 
	 * @param totalWork     number of ticks to consume from the receiver
	 * @param suppressFlags a bitwise combination of SUPPRESS_* flags. They can be
	 *                      used to suppress various behaviors on the newly-created
	 *                      monitor. Callers should generally include the
	 *                      {@link #SUPPRESS_BEGINTASK} flag unless they are
	 *                      invoking a method whose JavaDoc specifically states that
	 *                      the string argument to {@link #beginTask(String, int)}
	 *                      must be preserved.
	 * @return new sub progress monitor that may be used in place of a new
	 *         SubMonitor
	 * @throws OperationCanceledException if the monitor has been cancelled
	 * @since 3.8
	 */
	public SubMonitor split(int totalWork, int suppressFlags) throws OperationCanceledException {
		int oldUsedForParent = this.usedForParent;
		SubMonitor result = newChild(totalWork, suppressFlags);

		if ((result.flags & SUPPRESS_ISCANCELED) == 0) {
			int ticksTheChildWillReportToParent = result.totalParent;

			// If the new child reports a nonzero amount of progress.
			if (ticksTheChildWillReportToParent > 0) {
				// Don't check for cancellation if the child is consuming 100% of its parent
				// since whatever code created
				// the parent already performed this check.
				if (oldUsedForParent > 0 || usedForParent < totalParent) {
					// Treat this as a nontrivial operation and check for cancellation
					// unconditionally.
					root.checkForCancellation();
				}
			} else {
				root.reportTrivialOperation(TRIVIAL_SPLIT_DELTA);
			}
		}
		return result;
	}

	private void cleanupActiveChild() {
		IProgressMonitor child = lastSubMonitor;
		if (child == null) {
			return;
		}

		lastSubMonitor = null;
		child.done();
	}

	@Override
	public void clearBlocked() {
		root.clearBlocked();
	}

	@Override
	public void setBlocked(IStatus reason) {
		root.setBlocked(reason);
	}

	protected static boolean eq(Object o1, Object o2) {
		if (o1 == null)
			return (o2 == null);
		if (o2 == null)
			return false;
		return o1.equals(o2);
	}

	private static String getCallerName() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		String ourClassName = SubMonitor.class.getCanonicalName();
		for (int idx = 1; idx < stackTrace.length; idx++) {
			String className = stackTrace[idx].getClassName();
			if (className.equals(ourClassName)) {
				continue;
			}

			return stackTrace[idx].toString();
		}

		return "Unknown"; //$NON-NLS-1$
	}

	private static void logProblem(String message) {
		String caller = getCallerName();
		synchronized (knownBuggyMethods) {
			if (!knownBuggyMethods.add(caller)) {
				return;
			}
		}
		APISupport.log(new Status(IStatus.WARNING, "org.eclipse.core.runtime", message, new Throwable())); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "SubMonitor [totalParent=" + totalParent + ", usedForParent=" + usedForParent + ", usedForChildren=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ usedForChildren + ", totalForChildren=" + totalForChildren + ", beginTaskCalled=" + beginTaskCalled //$NON-NLS-1$ //$NON-NLS-2$
				+ "]"; //$NON-NLS-1$
	}

}
