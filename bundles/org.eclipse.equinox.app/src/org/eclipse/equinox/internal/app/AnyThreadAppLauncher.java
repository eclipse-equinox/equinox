/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class AnyThreadAppLauncher implements Runnable {
	private final EclipseAppHandle appHandle;

	private AnyThreadAppLauncher(EclipseAppHandle appHandle) {
		this.appHandle = appHandle;
	}

	@Override
	public void run() {
		try {
			// pasing null will cause EclipseAppHandle to get the correct arguments
			appHandle.run(null);
		} catch (Throwable e) {
			Activator.log(
					new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.ERROR, 0, e.getMessage(), 0, e, null));
		}
	}

	static void launchEclipseApplication(EclipseAppHandle appHandle) {
		AnyThreadAppLauncher launchable = new AnyThreadAppLauncher(appHandle);
		new Thread(launchable, "app thread - " + appHandle.getInstanceId()).start(); //$NON-NLS-1$
	}
}
