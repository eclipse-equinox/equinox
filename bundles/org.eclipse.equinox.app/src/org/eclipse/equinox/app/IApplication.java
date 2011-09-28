/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.app;

/**
 * Bootstrap type for an application.  An IApplication represent executable 
 * entry points into an application.  An IApplication can be configured into 
 * the Platform's <code>org.eclipse.core.runtime.applications</code> extension-point.
 *
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 1.0
 */
public interface IApplication {

	/**
	 * Exit object indicating normal termination
	 */
	public static final Integer EXIT_OK = new Integer(0);

	/**
	 * Exit object requesting platform restart
	 */
	public static final Integer EXIT_RESTART = new Integer(23);

	/**
	 * Exit object requesting that the command passed back be executed.  Typically
	 * this is used to relaunch Eclipse with different command line arguments.  When the executable is
	 * relaunched the command line will be retrieved from the 
	 * {@link IApplicationContext#EXIT_DATA_PROPERTY eclipse.exitdata} system property.  
	 */
	public static final Integer EXIT_RELAUNCH = new Integer(24);

	/**
	 * Starts this application with the given context and returns a result.  The content of 
	 * the context is unchecked and should conform to the expectations of the application being 
	 * invoked.  This method can return the value {@link IApplicationContext#EXIT_ASYNC_RESULT} if 
	 * the application will deliver its results asynchronously with the 
	 * {@link IApplicationContext#setResult(Object, IApplication)} method; otherwise this method must not exit 
	 * until the application is finished and is ready to exit.
	 * <p>
	 * Applications can return any object they like.  If an <code>Integer</code> is returned
	 * it is treated as the program exit code if Eclipse is exiting.
	 * </p>
	 * <p>
	 * Note: This method is called by the platform; it is not intended
	 * to be called directly by clients.
	 * </p>
	 * @return the return value of the application
	 * @see #EXIT_OK
	 * @see #EXIT_RESTART
	 * @see #EXIT_RELAUNCH
	 * @see IApplicationContext#EXIT_ASYNC_RESULT
	 * @param context the application context to pass to the application
	 * @exception Exception if there is a problem running this application.
	 */
	public Object start(IApplicationContext context) throws Exception;

	/**
	 * Forces this running application to exit.  This method should wait until the 
	 * running application is ready to exit.  The {@link #start(IApplicationContext)} 
	 * should already have exited or should exit very soon after this method exits<p>
	 * 
	 * This method is only called to force an application to exit.
	 * This method will not be called if an application exits normally from 
	 * the {@link #start(IApplicationContext)} method.
	 * <p>
	 * Note: This method is called by the platform; it is not intended
	 * to be called directly by clients.
	 * </p>
	 */
	public void stop();
}
