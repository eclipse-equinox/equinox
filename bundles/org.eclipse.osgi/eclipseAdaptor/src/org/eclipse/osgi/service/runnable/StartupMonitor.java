/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
 *  StartupMonitor.
 *  
 *  Bundles can register a startup monitor in order to be given processing time on the 
 *  primary thread during the startup process.  Clients with threading restrictions can
 *  use this interface to process events that may have been collected from another thread.
 *  
 */
public interface StartupMonitor {
	
	/** 
	 * Update the monitor
	 *  This method is periodically called from the primary thread during periods where the main
	 *  is waiting on another thread (ie start level increasing, refreshing packages)
	 *  
	 *  This monitor shares time on the Main Thread with any other StartupMonitors that have been 
	 *  registered.  As well, the thread we are waiting on has completed, the launching of 
	 *  application will not proceed until the startup monitors return from their last update.
	 *  Because of this, StartupMonitors should not perform long running operations during update. 
	 */
	public void update();
}
