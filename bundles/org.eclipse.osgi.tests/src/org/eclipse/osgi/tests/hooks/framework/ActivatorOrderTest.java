/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.osgi.tests.hooks.framework;

import static org.eclipse.osgi.tests.bundles.AbstractBundleTests.stop;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class ActivatorOrderTest extends AbstractFrameworkHookTests {

	private Framework framework;
	private static final String HOOK_CONFIGURATOR_BUNDLE = "activator.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS1 = "org.eclipse.osgi.tests.hooks.framework.activator.a.TestHookConfigurator1";
	private static final String HOOK_CONFIGURATOR_CLASS2 = "org.eclipse.osgi.tests.hooks.framework.activator.a.TestHookConfigurator2";
	private static final String HOOK_CONFIGURATOR_CLASS3 = "org.eclipse.osgi.tests.hooks.framework.activator.a.TestHookConfigurator3";

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		File file = OSGiTestsActivator.getContext().getDataFile(testName.getMethodName());
		HashMap<String, String> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS,
				HOOK_CONFIGURATOR_CLASS1 + "," + HOOK_CONFIGURATOR_CLASS2 + "," + HOOK_CONFIGURATOR_CLASS3);

		framework = createFramework(configuration);
	}

	@Test
	public void testActivatorOrder() throws Exception {
		List<String> actualEvents = new ArrayList<>();
		Class<?> clazz1 = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS1);
		clazz1.getField("events").set(null, actualEvents);
		Class<?> clazz2 = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS2);
		clazz2.getField("events").set(null, actualEvents);
		Class<?> clazz3 = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS3);
		clazz3.getField("events").set(null, actualEvents);

		List<String> expectedEvents = Arrays.asList("HOOK1 STARTED", "HOOK2 STARTED", "HOOK3 STARTED", "HOOK3 STOPPED",
				"HOOK2 STOPPED", "HOOK1 STOPPED");

		initAndStart(framework);
		stop(framework);
		assertEquals("Activator order not as expected", expectedEvents, actualEvents);

	}

}
