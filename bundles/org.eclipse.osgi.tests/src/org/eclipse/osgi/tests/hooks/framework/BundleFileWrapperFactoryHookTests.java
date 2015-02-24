/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.hooks.framework;

import java.io.File;
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
	private static final String HOOK_CONFIGURATOR_CLASS = "wrapper.hooks.a.TestHookConfigurator";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	protected void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
	}

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
		URL url = b.getResource("data/resource1");
		assertTrue("Wrong protocol used: " + url, "jar".equals(url.getProtocol()) || "file".equals(url.getProtocol()));
	}
}
