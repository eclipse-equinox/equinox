/*******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;


/**
 * <p>
 * A future represents the future outcome of some operation(s).
 * </p>
 * <p>
 * The expected usage of a future is as a return value from some operation 
 * that is to be executed asynchronously and then return some result.  
 * </p>
 * <p>
 * So, for example, a simple usage of an IFuture would be:
 * <pre>
 * IFuture future = foo();
 * ...
 * Object result = future.get();
 * </pre>
 * Clients generally will hold onto the future for some amount of time, and then call
 * {@link #get()} or {@link #get(long)} to retrieve the result of the operation.  They
 * may also call {@link #hasValue()} to determine whether any values have been provided
 * to the future (if <code>true</code>, meaning that subsequent calls to 
 * {@link #get()} will not block), or {@link #isDone()} to determine if <b>all</b> 
 * operations and results have been completed.
 * </p>
 * <p>
 * If {@link #hasValue()} is true, then the client may access status information
 * associated with the completed operation(s) via {@link #getStatus()}.  Until {@link #hasValue()}
 * is <code>true</code>, {@link #getStatus()} will be <code>null</code>.
 * </p>
 * 
 * @see IStatus
 * 
 */
public interface IFuture {

	/**
	 * Cancel the operation
	 * @return <tt>false</tt> if the operation could not be canceled,
	 * typically because it has already completed normally;
	 * <tt>true</tt> otherwise
	 */
	public boolean cancel();

	/**
	 * Waits if necessary for one or more operations to complete, and then returns result(s).
	 * This method will block until either a) at least one result is available; or b) at 
	 * least one operation throws an exception.
	 * 
	 * @return Object result of the asynchronous operation(s)
	 * @throws InterruptedException
	 *             if thread calling this method is interrupted.
	 * @throws OperationCanceledException
	 *             if the operation has been canceled via progress monitor {@link #getProgressMonitor()}.
	 */
	Object get() throws InterruptedException, OperationCanceledException;

	/**
	 * Waits if necessary for one or more operations to complete, and then returns result(s).
	 * This method will block until either a) at least one result is available; or b) at 
	 * least one operation throws an exception.
	 * 
	 * @param waitTimeInMillis
	 *            the maximum time to wait in milliseconds for the operation(s) to complete.
	 * @return Object result of the asynchronous operation(s)
	 * @throws InterruptedException
	 *             if thread calling this method is interrupted.
	 * @throws TimeoutException
	 *             if the given wait time is exceeded without getting result.
	 * @throws OperationCanceledException
	 *             if the operation has been canceled via progress monitor {@link #getProgressMonitor()}.
	 */
	Object get(long waitTimeInMillis) throws InterruptedException, TimeoutException, OperationCanceledException;

	/**
	 * <p>
	 * Get status for operation.  Will return <code>null</code> until at least one operation(s) are
	 * complete.
	 * </p>
	 * <p>
	 * If {@link #hasValue()} returns <code>true</code>, this method will return a non-<code>null</code>
	 * IStatus.  If {@link #hasValue()} returns <code>false</code>, this method will return <code>null</code>.
	 * </p>
	 * <p>
	 * Note that the returned IStatus instance may be an IMultiStatus, meaning that multiple operations have
	 * completed or are pending completion.
	 * </p>
	 * @return IStatus the status of completed operation(s).  Will return <code>null</code> if {@link #hasValue()}
	 * returns <code>false</code>.
	 * 
	 * @see #hasValue()
	 */
	public IStatus getStatus();

	/**
	 * <p>
	 * Returns <tt>true</tt> if <b>any</b> underlying operation(s) have completed.
	 * </p>
	 * <p>
	 * If this future represents access to just one operation, then this method
	 * and {@link #isDone()} will always return the same value.  That is, when a single
	 * operation has a value, it is then considered done/completed and both
	 * {@link #isDone()} and this method will return <code>true</code>.
	 * </p>
	 * <p>
	 * If this future represents multiple operations, then this method will 
	 * return <code>true</code> when <b>any</b> of the operations have 
	 * completed.  Until the first operation is completed, it will 
	 * return <code>false</code>.
	 * </p>
	 * @return <tt>true</tt> if any operations represented by this future have 
	 * completed.
	 */
	boolean hasValue();

	/**
	 * <p>
	 * Returns <tt>true</tt> if <b>all</b> underlying operation(s) have been completed.  
	 * </p>
	 * <p>
	 * If this future represents access to just one operation, then this method
	 * and {@link #hasValue()} will always return the same value.  That is, when a single
	 * operation has a value, it is then considered done/completed and both
	 * {@link #hasValue()} and #isDone will return <code>true</code>.
	 * </p>
	 * <p>
	 * If this future represents multiple operations, then this method will only
	 * return <code>true</code> when <b>all</b> of the operations have 
	 * completed.  Until all operations have completed, it will return <code>false</code>.
	 * </p>
	 * <p>
	 * Completion can be due to normal operation completion, an exception, or
	 * user cancellation -- in all of these cases, this method will return
	 * <tt>true</tt> if all underlying operation(s) have been completed.
	 * </p>
	 * 
	 * @return <tt>true</tt> if all operation(s) have completed in some manner.
	 */
	boolean isDone();

}
