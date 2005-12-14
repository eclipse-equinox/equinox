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

package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static final String PI_APP = "org.eclipse.equinox.app"; //$NON-NLS-1$
	public static boolean DEBUG = false;
	private ContainerManager containerMgr;
	public void start(BundleContext context) throws Exception {
		getDebugOptions(context);
		// set the app manager context before starting the containerMgr
		AppManager.setBundleContext(context);
		// create and start the app containerMgr
		containerMgr = new ContainerManager(context);
		containerMgr.startContainer();
		// start the app commands for the console
		try {
			AppCommands.create(context);
		} catch(NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
	}

	public void stop(BundleContext context) throws Exception {
		// stop the app commands for the console
		try {
			AppCommands.destroy(context);
		} catch(NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
		// stop the app containerMgr
		containerMgr.stopContainer();
		containerMgr = null;
		// un set the app manager context after the containerMgr has been stopped
		AppManager.setBundleContext(null);
	}

	private void getDebugOptions(BundleContext context) {
		ServiceReference debugRef = context.getServiceReference(DebugOptions.class.getName());
		if (debugRef == null)
			return;
		DebugOptions debugOptions = (DebugOptions) context.getService(debugRef);
		DEBUG = debugOptions.getBooleanOption(PI_APP + "/debug", false); //$NON-NLS-1$
		context.ungetService(debugRef);
	}
}
