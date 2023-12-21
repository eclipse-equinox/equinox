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

import static org.eclipse.osgi.tests.bundles.AbstractBundleTests.stopQuietly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.SystemBundleTests;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class DevClassPathDuplicateTests extends AbstractFrameworkHookTests {
	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		File store = OSGiTestsActivator.getContext().getDataFile(testName.getMethodName());
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, store.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_DEV, "duplicate/");
		framework = createFramework(configuration);

		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "b.dup.cp");
		headers.put(Constants.BUNDLE_CLASSPATH, "duplicate/");
		Map<String, String> entries = new HashMap<>();
		entries.put("duplicate/", null);
		entries.put("duplicate/resource.txt", "hello");
		File testBundle = SystemBundleTests.createBundle(store, "b.dup.cp", headers, entries);
		location = testBundle.toURI().toASCIIString();
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
	public void testDevClassPathWithExtension() throws Exception {
		initAndStartFramework();

		Bundle b = installBundle();
		b.start();
		Enumeration<URL> resources = b.getResources("resource.txt");
		assertNotNull("no resources", resources);
		int cnt = 0;
		while (resources.hasMoreElements()) {
			cnt++;
			resources.nextElement();
		}
		assertEquals("Wrong number of resources.", 1, cnt);
	}
}
