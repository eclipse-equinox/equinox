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
package org.eclipse.osgi.tests.appadmin;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class ExitValueApp implements IApplication {

	public static final String exitValue = "Exit Value"; //$NON-NLS-1$
	private boolean active = true;
	private boolean stopped = false;

	public synchronized Object start(IApplicationContext context) {
		context.applicationRunning();
		if (active) {
			try {
				wait(5000); // only run for 5 seconds at most
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		stopped = true;
		notifyAll();
		return exitValue;
	}

	public synchronized void stop() {
		active = false;
		notifyAll();
		while (!stopped)
			try {
				wait(100);
			} catch (InterruptedException e) {
				// do nothing
			}
	}

}
