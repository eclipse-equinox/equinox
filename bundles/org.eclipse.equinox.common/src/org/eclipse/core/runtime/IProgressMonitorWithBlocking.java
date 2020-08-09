/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *     Christoph Laeubrich - join with IProgressMonitorWithBlocking
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * An extension to the IProgressMonitor interface for monitors that want to
 * support feedback when an activity is blocked due to concurrent activity in
 * another thread.
 * <p>
 * When a monitor that supports this extension is passed to an operation, the
 * operation should call <code>setBlocked</code> whenever it knows that it
 * must wait for a lock that is currently held by another thread. The operation
 * should continue to check for and respond to cancelation requests while
 * blocked. When the operation is no longer blocked, it must call <code>clearBlocked</code>
 * to clear the blocked state.
 * <p>
 * This interface can be used without OSGi running.
 * </p><p>
 * Clients may implement this interface.
 * </p>
 * @see IProgressMonitor
 * @since 3.0
 */
@Deprecated
public interface IProgressMonitorWithBlocking extends IProgressMonitor {
	//content moved to IProgressMonitor

}
