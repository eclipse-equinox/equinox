/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.tests.listeners;

import java.io.IOException;
import java.net.MalformedURLException;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

public class ExceptionHandlerTests {
	// These tests exercise the code change for bug 73111.

	class FrameworkEventListenerWithResult implements FrameworkListener {
		FrameworkEvent event = null;

		public synchronized void frameworkEvent(FrameworkEvent newEvent) {
			if (newEvent.getType() != FrameworkEvent.ERROR)
				return;

			if (this.event != null)
				return;
			event = newEvent;
			notify();
		}

		public synchronized FrameworkEvent getResult(long timeout) {
			if (event != null) {
				FrameworkEvent tmp = event;
				event = null;
				return tmp;
			}
			try {
				wait(timeout);
			} catch (InterruptedException e) {
			}
			FrameworkEvent tmp = event;
			event = null;
			return tmp;
		}
	}

	@Test
	public void testNonFatalException() {
		FrameworkEventListenerWithResult fwkListener = new FrameworkEventListenerWithResult();
		OSGiTestsActivator.getContext().addFrameworkListener(fwkListener);

		BundleListener npeGenerator = event -> {
			throw new NullPointerException("Generated exception");
		};
		OSGiTestsActivator.getContext().addBundleListener(npeGenerator);

		try {
			BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(),
					OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle09");
			FrameworkEvent eventReceived = fwkListener.getResult(60000);
			Assert.assertEquals(FrameworkEvent.ERROR, eventReceived.getType());
			Assert.assertEquals(true, eventReceived.getThrowable() instanceof NullPointerException);
		} catch (MalformedURLException e) {
			// Does not happen
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OSGiTestsActivator.getContext().removeFrameworkListener(fwkListener);
		OSGiTestsActivator.getContext().removeBundleListener(npeGenerator);
	}

	@Test
	public void testFatalException() {
		FrameworkEventListenerWithResult fwkListener = new FrameworkEventListenerWithResult();
		OSGiTestsActivator.getContext().addFrameworkListener(fwkListener);

		BundleListener fatalException = event -> {
			throw new OutOfMemoryError("Generated exception");
		};
		OSGiTestsActivator.getContext().addBundleListener(fatalException);

		try {
			System.setProperty("eclipse.exitOnError", "false"); // Here we set the value to false, because otherwise we
																// would simply exit
			BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(),
					OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle10");
			FrameworkEvent eventReceived = fwkListener.getResult(10000);
			Assert.assertEquals(FrameworkEvent.ERROR, eventReceived.getType());
			Assert.assertEquals(true, eventReceived.getThrowable() instanceof VirtualMachineError);
			System.setProperty("eclipse.exitOnError", "true");
		} catch (MalformedURLException e) {
			// Does not happen
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OSGiTestsActivator.getContext().removeFrameworkListener(fwkListener);
		OSGiTestsActivator.getContext().removeBundleListener(fatalException);
	}

}