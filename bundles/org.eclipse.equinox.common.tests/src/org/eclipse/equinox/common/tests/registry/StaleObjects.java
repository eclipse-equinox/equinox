/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests.registry;

import java.io.IOException;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class StaleObjects extends TestCase {
	private class HandleCatcher implements IRegistryChangeListener {
		private IExtension extensionFromTheListener;

		public HandleCatcher() {
			RegistryFactory.getRegistry().addRegistryChangeListener(this);
		}

		@Override
		public void registryChanged(IRegistryChangeEvent event) {
			boolean gotException = false;
			try {
				extensionFromTheListener = event.getExtensionDeltas()[0].getExtension();
				extensionFromTheListener.getSimpleIdentifier();
			} catch (InvalidRegistryObjectException e) {
				gotException = true;
			}
			assertEquals(false, gotException);
		}

		public IExtension getAcquiredHandle() {
			return extensionFromTheListener;
		}
	}

	public synchronized void testA() throws IOException, BundleException {
		HandleCatcher listener = new HandleCatcher();
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle bundle01 = BundleTestingHelper.installBundle("", bundleContext, "Plugin_Testing/registry/testStale1");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});

		IExtension willBeStale = RegistryFactory.getRegistry().getExtension("testStale.ext1");

		//Test that handles obtained from deltas are working.

		//Test that handles obtained from an addition deltas are working even after the delta is done being broadcasted.
		boolean gotException = false;
		try {
			IExtension result = null;
			while ((result = listener.getAcquiredHandle()) == null) {
				try {
					wait(200);
				} catch (InterruptedException e) {
					//ignore.
				}
			}
			result.getSimpleIdentifier();
		} catch (InvalidRegistryObjectException e) {
			gotException = true;
		}
		assertEquals(false, gotException);

		//Add a listener capturing a handle removal. Inside the handle catcher the handle is valid
		HandleCatcher listener2 = new HandleCatcher();
		try {
			wait(500); //Wait for the listeners to be done
		} catch (InterruptedException e) {
			//ignore.
		}

		bundle01.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});

		//Outside of the event notification the handle from a removed object should be invalid
		gotException = false;
		try {
			while (listener2.getAcquiredHandle() == null) {
				try {
					wait(200);
				} catch (InterruptedException e) {
					//ignore.
				}
			}
			listener2.getAcquiredHandle().getSimpleIdentifier();
		} catch (InvalidRegistryObjectException e) {
			gotException = true;
		}
		assertEquals(true, gotException);

		//Check that the initial handles are stale as well
		gotException = false;
		try {
			willBeStale.getSimpleIdentifier();
		} catch (InvalidRegistryObjectException e) {
			gotException = true;
		}
		assertEquals(true, gotException);
		RegistryFactory.getRegistry().removeRegistryChangeListener(listener2);
		RegistryFactory.getRegistry().removeRegistryChangeListener(listener);
	}

	public void testStaleConfigurationElement() throws IOException, BundleException {
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle bundle01 = BundleTestingHelper.installBundle("", bundleContext, "Plugin_Testing/registry/testStale2");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});

		IConfigurationElement ce = RegistryFactory.getRegistry().getExtension("testStale2.ext1").getConfigurationElements()[0];
		assertNotNull(ce);

		bundle01.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});

		boolean gotException = false;
		try {
			ce.createExecutableExtension("name");
		} catch (CoreException c) {
			gotException = true;
		}
		assertEquals(true, gotException);
	}
}
