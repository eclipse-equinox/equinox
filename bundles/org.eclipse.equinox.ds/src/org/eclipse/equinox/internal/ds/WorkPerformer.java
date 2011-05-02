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
package org.eclipse.equinox.internal.ds;

/**
 * The WorkPerformer interface contains a method that is called by a queue to
 * perform certain work.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface WorkPerformer {

	/**
	 * This method can then complete processing work on the work queue thread.
	 * 
	 * <p>
	 * The job queue will ignore any Throwable thrown by this method in order to
	 * continue proper dispatching the next work items.
	 * 
	 * @param actionId
	 *            Action ID of the work type which has to be done.
	 * @param dataToProcess
	 *            The data which has to be processed
	 */
	public void performWork(int actionId, Object dataToProcess);
}
