/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import static org.eclipse.osgi.tests.bundles.AbstractBundleTests.stopQuietly;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class BundleFileWrapperFactoryHookTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "substitutes.a";
	private static final String HOOK_CONFIGURATOR_BUNDLE = "wrapper.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "org.eclipse.osgi.tests.wrapper.hooks.a.TestHookConfigurator";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(testName.getMethodName());
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
	}

	@Override
	public void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	private void initAndStartFramework() throws Exception {
		initAndStart(framework);
	}

	private Bundle installBundle() throws Exception {
		return framework.getBundleContext().installBundle(location);
	}

	@Test
	public void testGetResourceURL() throws Exception {
		initAndStartFramework();

		Bundle b = installBundle();
		URL url1 = b.getResource("data/resource1");
		assertEquals("Wrong protocol used: " + url1, "custom", url1.getProtocol());
		assertEquals("Wrong content found.", "CUSTOM_CONTENT", readURL(url1));
	}

	private String readURL(URL url) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			for (String line = reader.readLine(); line != null;) {
				sb.append(line);
				line = reader.readLine();
				if (line != null)
					sb.append('\n');
			}
		}
		return sb.toString();
	}
}
