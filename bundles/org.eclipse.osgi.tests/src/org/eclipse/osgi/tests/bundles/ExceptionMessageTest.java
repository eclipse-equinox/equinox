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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;

public class ExceptionMessageTest extends AbstractBundleTests {

	public void testTrasientStartLevelError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.adapt(BundleStartLevel.class).setStartLevel(500);
		try {
			b.start(Bundle.START_TRANSIENT);
			fail();
		} catch (BundleException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
		}
	}

	public void testUninstallModuleError() throws BundleException {
		Bundle b = installer.installBundle("test");
		BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
		b.uninstall();
		try {
			bsl.getStartLevel();
			fail();
		} catch (IllegalStateException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
		}
	}

	public void testUninstallContextError() throws BundleException, InvalidSyntaxException {
		Bundle b = installer.installBundle("test");
		b.start();
		BundleContext context = b.getBundleContext();
		b.uninstall();
		try {
			context.createFilter("(a=b)");
			fail();
		} catch (IllegalStateException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.toString()));
		}
	}

	public void testStartFragmentError() throws BundleException, IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "fragment");
		headers.put(Constants.FRAGMENT_HOST, "host");
		File bundles = OSGiTestsActivator.getContext().getDataFile("/"); // $NON-NLS-1$
		File bundleFile = SystemBundleTests.createBundle(bundles, getName(), headers);
		Bundle b = OSGiTestsActivator.getContext().installBundle(bundleFile.toURI().toString());
		try {
			b.start();
			fail();
		} catch (BundleException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.adapt(Module.class).toString()));
		} finally {
			b.uninstall();
		}
	}

	public void testLoadActivatorError() throws IOException, BundleException {
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_ACTIVATOR, "does.not.Exist");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "no.activator");
		File bundles = OSGiTestsActivator.getContext().getDataFile("/"); // $NON-NLS-1$
		File bundleFile = SystemBundleTests.createBundle(bundles, getName(), headers);
		Bundle b = OSGiTestsActivator.getContext().installBundle(bundleFile.toURI().toString());
		try {
			b.start();
			fail();
		} catch (BundleException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(b.toString()));
		} finally {
			b.uninstall();
		}
	}

	public void testUnregisterSetPropsError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.start();
		BundleContext context = b.getBundleContext();
		ServiceRegistration<Object> reg = context.registerService(Object.class, new Object(),
				new Hashtable<>(Collections.singletonMap("k1", "v1")));
		reg.unregister();

		try {
			reg.setProperties(new Hashtable<>(Collections.singletonMap("k2", "v2")));
			fail();
		} catch (IllegalStateException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(reg.toString()));
		}

		try {
			reg.unregister();
			fail();
		} catch (IllegalStateException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(reg.toString()));
		}

		try {
			reg.getReference();
			fail();
		} catch (IllegalStateException e) {
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().endsWith(reg.toString()));
		}
	}

	public void testUnregisterTwiceError() throws BundleException {
		Bundle b = installer.installBundle("test");
		b.start();
		BundleContext context = b.getBundleContext();
		ServiceRegistration<Object> reg = context.registerService(Object.class, new Object(),
				new Hashtable<>(Collections.singletonMap("k1", "v1")));
		reg.unregister();

	}
}
