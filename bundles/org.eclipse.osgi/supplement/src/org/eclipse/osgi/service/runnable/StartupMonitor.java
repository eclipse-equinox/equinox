/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Niefer - IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.runnable;

/**
 * Service interface used to monitor the startup process.
 *  
 * Bundles can register a monitor in order to be given processing time on the 
 * primary thread during the startup process.  Clients with threading restrictions can
 * use this interface to process events that may have been collected from another thread.
 *  <p> 
 * Monitors share time on the primary thread.  The primary thread used to run the application 
 * will not proceed until monitors return from any operation.  Because of this, monitors should 
 * not perform long running operations.
 * </p>
 * <p>
 * Clients may implement this interface but should not invoke it.  The platform 
 * is responsible for invoking the monitor at the appropriate times.
 * </p> 
 * @since 3.3
 */
public interface StartupMonitor {
	/** 
	 * Update the monitor. This method is periodically called by the platform from the primary thread during 
	 * periods where the primary thread is waiting on another thread (ie start level increasing, refreshing packages)
	 * <p>
	 * If multiple monitors are registered then the platform will only call the monitor with the highest service 
	 * ranking.  In case of a service ranking tie the service with the lowest service id is selected (i.e. the 
	 * first monitor registered).
	 * </p>
	 */
	public void update();

	/**
	 * This method is called by the platform from the primary thread once the application is completely 
	 * initialized and running.  This method should perform certain operations that are needed once an 
	 * application is running.  One example is bringing down a splash screen if it exists.
	 * <p>
	 * If multiple monitors are registered then the platform will call all monitors.  The monitors are called
	 * according to service ranking; monitors with higher service rankings are called first.  In case of a 
	 * service ranking tie the service with the lowest service id is called first (i.e. the first monitor registered).
	 * </p>
	 */
	public void applicationRunning();
}
