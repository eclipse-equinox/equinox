/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
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

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
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
	protected void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
	}

	@Override
	protected void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	private void initAndStartFramework() throws Exception {
		initAndStart(framework);
	}

	private Bundle installBundle() throws Exception {
		return framework.getBundleContext().installBundle(location);
	}

	public void testGetResourceURL() throws Exception {
		initAndStartFramework();

		Bundle b = installBundle();
		URL url1 = b.getResource("data/resource1");
		assertEquals("Wrong protocol used: " + url1, "custom", url1.getProtocol());
		assertEquals("Wrong content found.", "CUSTOM_CONTENT", readURL(url1));
	}

	private String readURL(URL url) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			try {
				for (String line = reader.readLine(); line != null;) {
					sb.append(line);
					line = reader.readLine();
					if (line != null)
						sb.append('\n');
				}
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			fail("Unexpected exception reading url: " + url.toExternalForm(), e); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
