/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;

public class DevClassPathWithExtensionTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "ext.framework.a";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	protected void setUp() throws Exception {
		super.setUp();
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_DEV, "bin/");
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

	public void testDevClassPathWithExtension() throws Exception {
		initAndStartFramework();

		Bundle b = installBundle();
		assertTrue("Did not resolve test extension", resolveBundles(Collections.singleton(b)));

		stop(framework);

		// reload the framework which uses read-only data structures
		framework = createFramework(configuration);
		initAndStart(framework);
	}

	private boolean resolveBundles(Collection<Bundle> bundles) {
		return framework.adapt(FrameworkWiring.class).resolveBundles(bundles);
	}

}
