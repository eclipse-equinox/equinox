/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;

public class ExceptionMessageTest extends AbstractBundleTests {

	public void testTrasientStartLevelError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.adapt(BundleStartLevel.class).setStartLevel(500);
		BundleException e = assertThrows(BundleException.class, () -> b.start(Bundle.START_TRANSIENT));
		assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
	}

	public void testUninstallModuleError() throws BundleException {
		Bundle b = installer.installBundle("test");
		BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
		b.uninstall();
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> bsl.getStartLevel());
		assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
	}

	public void testUninstallContextError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.start();
		BundleContext context = b.getBundleContext();
		b.uninstall();
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> context.createFilter("(a=b)"));
		assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.toString()));
	}

	public void testStartFragmentError() throws BundleException, IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "fragment");
		headers.put(Constants.FRAGMENT_HOST, "host");
		File bundles = OSGiTestsActivator.getContext().getDataFile("/"); // $NON-NLS-1$
		File bundleFile = SystemBundleTests.createBundle(bundles, getName(), headers);
		Bundle b = OSGiTestsActivator.getContext().installBundle(bundleFile.toURI().toString());

		BundleException e = assertThrows(BundleException.class, () -> b.start());
		b.uninstall();
		assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
	}

	public void testLoadActivatorError() throws IOException, BundleException {
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_ACTIVATOR, "does.not.Exist");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "no.activator");
		File bundles = OSGiTestsActivator.getContext().getDataFile("/"); // $NON-NLS-1$
		File bundleFile = SystemBundleTests.createBundle(bundles, getName(), headers);
		Bundle b = OSGiTestsActivator.getContext().installBundle(bundleFile.toURI().toString());

		BundleException e = assertThrows(BundleException.class, () -> b.start());
		assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.toString()));
	}

	public void testUnregisterSetPropsError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.start();
		Dictionary<String, Object> props1 = getDicinotary("k1", "v1");
		ServiceRegistration<Object> reg = b.getBundleContext().registerService(Object.class, new Object(), props1);
		reg.unregister();

		Dictionary<String, Object> props2 = getDicinotary("k2", "v2");
		IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> reg.setProperties(props2));
		assertTrue("Wrong message: " + e1.getMessage(), e1.getMessage().endsWith(reg.toString()));

		IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> reg.unregister());
		assertTrue("Wrong message: " + e2.getMessage(), e2.getMessage().endsWith(reg.toString()));

		IllegalStateException e3 = assertThrows(IllegalStateException.class, () -> reg.getReference());
		assertTrue("Wrong message: " + e3.getMessage(), e3.getMessage().endsWith(reg.toString()));
	}

	public void testUnregisterTwiceError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.start();
		BundleContext context = b.getBundleContext();
		ServiceRegistration<Object> reg = context.registerService(Object.class, new Object(),
				getDicinotary("k1", "v1"));
		reg.unregister();
	}

	private Dictionary<String, Object> getDicinotary(String key, Object value) {
		return FrameworkUtil.asDictionary(Collections.singletonMap(key, value));
	}
}
