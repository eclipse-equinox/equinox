/******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface defining a block of code that can be run, return an Object result,
 * and throw an arbitrary Exception.
 * 
 */
public interface IProgressRunnable {
	/** Perform some action that returns a result or throws an exception
	 * @param monitor the IProgressMonitor associated with this callable
	 * @return result from the call
	 * @throws Exception
	 */
	Object run(IProgressMonitor monitor) throws Exception;
}