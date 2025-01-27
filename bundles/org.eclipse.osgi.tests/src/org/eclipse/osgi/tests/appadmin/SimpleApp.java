/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.osgi.tests.appadmin;

import java.util.HashMap;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class SimpleApp implements IApplication {

	private boolean active = true;
	private boolean stopped = false;

	@Override
	public synchronized Object start(IApplicationContext context) {
		System.out.println("started simple app");
		System.out.flush();
		HashMap results = (HashMap) context.getArguments().get(ApplicationAdminTest.testResults);
		if (results != null)
			results.put(ApplicationAdminTest.simpleResults, ApplicationAdminTest.SUCCESS);
		context.applicationRunning();
		while (active) {
			try {
				wait(100);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		stopped = true;
		notifyAll();
		System.out.println("stopped simple app");
		System.out.flush();
		return context.getArguments();
	}

	@Override
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
