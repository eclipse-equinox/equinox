/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.ds.workqueue;

/**
 * The WorkDispatcher interface contains the method that is called by the
 * WorkQueue to dispatch work.
 */

public interface WorkDispatcher {
	/**
	 * This method is called once for each work item. This method can then
	 * complete processing work on the work queue thread.
	 * 
	 * <p>
	 * The WorkQueue will ignore any Throwable thrown by this method in order to
	 * continue dispatch of the next work item.
	 * 
	 * @param workAction Work action value passed from the work enqueuer.
	 * @param workObject Work object passed from the work enqueuer.
	 */
	public void dispatchWork(int workAction, Object workObject);
}