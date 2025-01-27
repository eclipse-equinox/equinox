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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class StaleObjects {
	private static class HandleCatcher implements IRegistryChangeListener {
		private IExtension extensionFromTheListener;

		public HandleCatcher() {
			RegistryFactory.getRegistry().addRegistryChangeListener(this);
		}

		@Override
		public void registryChanged(IRegistryChangeEvent event) {
			extensionFromTheListener = event.getExtensionDeltas()[0].getExtension();
			assertThrows(InvalidRegistryObjectException.class, () -> extensionFromTheListener.getSimpleIdentifier());
		}

		public IExtension getAcquiredHandle() {
			return extensionFromTheListener;
		}
	}

	@Test
	public synchronized void testA() throws IOException, BundleException {
		HandleCatcher listener = new HandleCatcher();
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle bundle01 = BundleTestingHelper.installBundle("", bundleContext, "Plugin_Testing/registry/testStale1");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle01 });

		IExtension willBeStale = RegistryFactory.getRegistry().getExtension("testStale.ext1");

		// Test that handles obtained from deltas are working.

		// Test that handles obtained from an addition deltas are working even after the
		// delta is done being broadcasted.
		IExtension result = null;
		while ((result = listener.getAcquiredHandle()) == null) {
			try {
				wait(200);
			} catch (InterruptedException e) {
				// ignore.
			}
		}
		result.getSimpleIdentifier();

		// Add a listener capturing a handle removal. Inside the handle catcher the
		// handle is valid
		HandleCatcher listener2 = new HandleCatcher();
		try {
			wait(500); // Wait for the listeners to be done
		} catch (InterruptedException e) {
			// ignore.
		}

		bundle01.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle01 });

		// Outside of the event notification the handle from a removed object should be
		// invalid
		while (listener2.getAcquiredHandle() == null) {
			try {
				wait(200);
			} catch (InterruptedException e) {
				// ignore.
			}
		}
		assertThrows(InvalidRegistryObjectException.class, () -> listener2.getAcquiredHandle().getSimpleIdentifier());

		// Check that the initial handles are stale as well
		assertThrows(InvalidRegistryObjectException.class, () -> willBeStale.getSimpleIdentifier());
		RegistryFactory.getRegistry().removeRegistryChangeListener(listener2);
		RegistryFactory.getRegistry().removeRegistryChangeListener(listener);
	}

	@Test
	public void testStaleConfigurationElement() throws IOException, BundleException {
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle bundle01 = BundleTestingHelper.installBundle("", bundleContext, "Plugin_Testing/registry/testStale2");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle01 });

		IConfigurationElement ce = RegistryFactory.getRegistry().getExtension("testStale2.ext1")
				.getConfigurationElements()[0];
		assertNotNull(ce);

		bundle01.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle01 });

		assertThrows(CoreException.class, () -> ce.createExecutableExtension("name"));
	}
}
