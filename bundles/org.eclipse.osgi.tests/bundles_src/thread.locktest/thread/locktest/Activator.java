/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package thread.locktest;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

public class Activator implements BundleActivator, Runnable {

	public void start(BundleContext context) throws Exception {
		Thread thread = new Thread(this, "thread.locktest");
		System.out.println("about to start thread");
		thread.start();
		System.out.println("about to join the thread");
		thread.join(40000);
		System.out.println("after joining thread");
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STARTED, context.getBundle()));
	}

	public void stop(BundleContext context) throws Exception {
		AbstractBundleTests.simpleResults.addEvent(new BundleEvent(BundleEvent.STOPPED, context.getBundle()));
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		System.out.println("about to load Class1");
		new Class1();
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.println("loaded Class1 " + totalTime);
		if (totalTime < 40000)
			AbstractBundleTests.simpleResults.addEvent(Long.valueOf(5000));
	}

}
