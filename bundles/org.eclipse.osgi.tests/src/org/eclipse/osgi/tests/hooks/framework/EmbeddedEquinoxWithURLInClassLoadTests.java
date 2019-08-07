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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class EmbeddedEquinoxWithURLInClassLoadTests extends AbstractFrameworkHookTests {

	private Framework framework;

	@Override
	protected void setUp() throws Exception {
		URL myManifest = getClass().getResource("/META-INF/MANIFEST.MF");
		testURL = myManifest.toExternalForm();
		super.setUp();
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
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

	public void testFrameworkClassLoaderWithNewURI() throws Exception {
		initAndStartFramework();
	}

	public void testEmbeddedURLHandler() throws Exception {
		initAndStart(framework);
		Bundle testHandler = framework.getBundleContext().installBundle(bundleInstaller.getBundleLocation("test.protocol.handler"));
		testHandler.start();
		Bundle testHandlerUser = framework.getBundleContext().installBundle(bundleInstaller.getBundleLocation("test.protocol.handler.user"));
		testHandlerUser.start();
		try {
			URL testingURL = new URL("testing1://test");
			fail("Should not find testing1 protocol: " + testingURL);
		} catch (MalformedURLException e) {
			// expected
		}
	}
}
