/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.app;

import java.util.Map;
import org.eclipse.equinox.registry.IConfigurationElement;
import org.osgi.service.application.ApplicationDescriptor;

/**
 * A context is used by a container to launch an application using 
 * {@link IContainer#launch(IAppContext)}.  The initial status of a 
 * context is {@link #ACTIVE} when it is given to a container to 
 * launch an application.
 * @see IApplication
 * @see IContainer
 * <p>
 * Clients may not implement this interface.
 * </p>
 * @since 3.2
 */
public interface IAppContext {
	/**
	 * Indicates the application is active for this context
	 */
	public int ACTIVE = 0x01;
	/**
	 * Indicates the application is stopping for this context
	 */
	public int STOPPING = 0x02;
	/**
	 * Indicates the application is stopped for this context
	 */
	public int STOPPED = 0x04;
	/**
	 * Returns the arguments to use when launching an application.
	 * @return the arguments to use when launching an applicaiton, null may be returned.
	 */
	public Map getArguments();
	/**
	 * Returns the application descriptor for this context.
	 * @return the application descriptor for this context.
	 */
	public ApplicationDescriptor getApplicationDescriptor();
	/**
	 * Returns the configuration element for this context.
	 * @return the configuration element for this context.
	 */
	public IConfigurationElement getConfiguration();

	/**
	 * Called by the container when an application is stopping or has stopped.
	 * @param status may be {@link #STOPPING} or {@link #STOPPED}
	 * @see #STOPPING
	 * @see #STOPPED
	 * @throws IllegalArgumentException if {@link #ACTIVE} is used as the status.
	 */
	public void setAppStatus(int status);

	/**
	 * Returns the current app status according to the context
	 * @return the current app status according to the context
	 */
	public int getAppStatus();
}
