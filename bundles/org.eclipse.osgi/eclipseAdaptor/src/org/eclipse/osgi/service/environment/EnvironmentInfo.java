/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.environment;

/**
 * A Framework service which gives access to the command line used to start
 * this running framework as well as information about the environment
 * such as the current operating system, machine architecture, locale and 
 * windowing system.
 * 
 * @since 3.0
 */
public interface EnvironmentInfo {

	/**
	 * Returns all command line arguments specified when the running framework was started.
	 * @return the array of command line arguments.
	 */
	public String[] getCommandLineArgs();

	/**
	 * Returns the arguments consumed by the framework implementation itself.  Which
	 * arguments are consumed is implementation specific.
	 * @return the array of command line arguments consumed by the framework.
	 */
	public String[] getFrameworkArgs();

	/**
	 * Returns the arguments not consumed by the framework implementation itself.  Which
	 * arguments are consumed is implementation specific.
	 * @return the array of command line arguments not consumed by the framework.
	 */
	public String[] getNonFrameworkArgs();

	/**
	 * Returns the string name of the current system architecture.  
	 * The value is a user-defined string if the architecture is 
	 * specified on the command line, otherwise it is the value 
	 * returned by <code>java.lang.System.getProperty("os.arch")</code>.
	 * 
	 * @return the string name of the current system architecture
	 */
	public String getOSArch();

	/**
	 * Returns the string name of the current locale for use in finding files
	 * whose path starts with <code>$nl$</code>.
	 *
	 * @return the string name of the current locale
	 */
	public String getNL();

	/**
	 * Returns the string name of the current operating system for use in finding
	 * files whose path starts with <code>$os$</code>.  <code>OS_UNKNOWN</code> is
	 * returned if the operating system cannot be determined.  
	 * The value may indicate one of the operating systems known to the platform
	 * (as specified in <code>knownOSValues</code>) or a user-defined string if
	 * the operating system name is specified on the command line.
	 *
	 * @return the string name of the current operating system
	 */
	public String getOS();

	/**
	 * Returns the string name of the current window system for use in finding files
	 * whose path starts with <code>$ws$</code>.  <code>null</code> is returned
	 * if the window system cannot be determined.
	 *
	 * @return the string name of the current window system or <code>null</code>
	 */
	public String getWS();

	/**
	 * Returns true if the framework is in debug mode.
	 * @return whether or not the framework is in debug mode
	 */
	public boolean inDebugMode();

	/**
	 * Returns true if the framework is in development mode.
	 * @return whether or not the framework is in development mode
	 */
	public boolean inDevelopmentMode();
}
