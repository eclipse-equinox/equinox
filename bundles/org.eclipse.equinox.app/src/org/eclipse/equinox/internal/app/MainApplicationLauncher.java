/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.osgi.service.application.ApplicationException;

/**
 * A main threaded application may be launched using this class to launch the main threaded application.
 */
public class MainApplicationLauncher implements ApplicationRunnable {
	private final EclipseAppContainer appContainer;
	private ApplicationRunnable launchMainApp; // a handle to a main threaded application

	public MainApplicationLauncher(EclipseAppContainer appContainer) {
		this.appContainer = appContainer;
	}

	public Object run(Object context) throws Exception {
		appContainer.startDefaultApp(false);
		ApplicationRunnable mainHandle = getMainHandle();
		if (mainHandle != null)
			return mainHandle.run(context);
		throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, Messages.application_noIdFound);
	}

	private synchronized ApplicationRunnable getMainHandle() {
		return launchMainApp;
	}

	public void stop() {
		// force the application to quit
		ApplicationRunnable handle = getMainHandle();
		if (handle != null)
			handle.stop();
	}

	synchronized void launch(ApplicationRunnable app) {
		launchMainApp = app;
	}
}
