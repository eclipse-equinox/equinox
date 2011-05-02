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
 * This interface is registered as a service in the framework. It allows bundles
 * to specify the amount of threads they will need for their normal work. A
 * bundle may be executing no more than a specified number of threads at the
 * same time. This interface is provided in order to prevent a scenario where
 * one bundle allocates all threads, thus freezing all other thread requestors.
 *
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ThreadPoolFactory {

	/**
	 * Gets reference to the thread pool, by specifying how many threads will be
	 * simultaneously engaged by the requesting bundle. All execution requests
	 * above this number are put in a waiting queue until a threads is free.
	 * 
	 * @param poolSize
	 *            count of threads that can be simultaneously used by the
	 *            requestor
	 * @param sizeIsInPercents
	 *            indicates if the poolSize is percent of the maximum number of
	 *            threads in the pool (true) or is a fixed count (false)
	 * @return reference to the thread pool manager
	 */
	public ThreadPoolManager getThreadPool(int poolSize, boolean sizeIsInPercents);
}
