/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.threadpool;

/**
 * Gives access to the user-provided Runnable object of the thread. All threads
 * created by the ThreadPoolManager service implement that interface, so that in
 * a method called by such a thread you can get the Runnable object provided in
 * <code>
 * ThreadPoolManager.execute(Runnable job, String threadName)</code>
 * with the following line:
 * <p>
 * <code><ul> Runnable runnable = ((ThreadContext) Thread.currentThread()).getRunnable();</ul></code>
 *
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ThreadContext {
	/**
	 * Returns the Runnable object provided in the
	 * <code>ThreadPoolManager.execute(Runnable job, String threadName)</code>
	 * method.
	 */
	public Runnable getRunnable();
}
