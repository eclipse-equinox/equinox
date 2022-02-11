/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 578702 - Move FileLocatorTest to rt.equinox.bundles repository
 *******************************************************************************/
package org.eclipse.core.runtime.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class FileLocatorTest {

	private final static String searchLocation = "$nl$/intro/messages.properties";

	private final static String nl = "aa_BB"; // make sure we have a stable NL value

	private final static String mostSpecificPath = "/nl/aa/BB/intro/messages.properties";
	private final static String lessSpecificPath = "/nl/aa/intro/messages.properties";
	private final static String nonSpecificPath = "/intro/messages.properties";

	@Test
	public void testFileLocatorFind() throws IOException, BundleException {
		Bundle bundle = BundleTestingHelper.installBundle("Plugin", getContext(),
				"Plugin_Testing/fileLocator/testFileLocator");
		BundleTestingHelper.refreshPackages(getContext(), new Bundle[] { bundle });
		Bundle fragment = BundleTestingHelper.installBundle("Fragment", getContext(),
				"Plugin_Testing/fileLocator/testFileLocator.nl");
		BundleTestingHelper.refreshPackages(getContext(), new Bundle[] { fragment });

		IPath path = new Path(searchLocation);
		Map<String, String> map = new HashMap<>(1);
		map.put("$nl$", nl);

		URL oneSolution = FileLocator.find(bundle, path, map);
		assertNotNull(oneSolution);
		assertEquals(mostSpecificPath, oneSolution.getPath());
		assertBundleURL(oneSolution);

		URL[] solutions = FileLocator.findEntries(bundle, path, map);

		// expected:
		// Bundle/nl/aa/BB/intro/messages.properties,
		// Fragment/nl/aa/BB/intro/messages.properties,
		// Bundle/nl/aa/intro/messages.properties,
		// Fragment/nl/aa/intro/messages.properties,
		// Bundle/121/intro/messages.properties

		assertEquals(5, solutions.length);

		assertEquals(mostSpecificPath, solutions[0].getPath());
		assertBundleURL(solutions[0]);
		assertEquals(mostSpecificPath, solutions[1].getPath());
		assertFragmentURL(solutions[1]);

		assertEquals(lessSpecificPath, solutions[2].getPath());
		assertBundleURL(solutions[2]);
		assertEquals(lessSpecificPath, solutions[3].getPath());
		assertFragmentURL(solutions[3]);

		assertEquals(nonSpecificPath, solutions[4].getPath());
		assertBundleURL(solutions[4]);

		// remove the first bundle
		fragment.uninstall();
		BundleTestingHelper.refreshPackages(getContext(), new Bundle[] { fragment });
		bundle.uninstall();
		BundleTestingHelper.refreshPackages(getContext(), new Bundle[] { bundle });
	}

	@Test
	public void testFileLocatorGetBundleFile01() throws BundleException, IOException {
		// test for bug 198447
		// install the bundle via reference
		BundleContext context = getContext();
		URL url = context.getBundle().getEntry("Plugin_Testing/fileLocator/testFileLocatorGetRootFile");
		Bundle bundle = context.installBundle("reference:" + FileLocator.toFileURL(url).toExternalForm());
		BundleTestingHelper.refreshPackages(context, new Bundle[] { bundle });

		File file1 = FileLocator.getBundleFile(bundle);
		assertNotNull(file1);

		URL fileURL = FileLocator
				.toFileURL(context.getBundle().getEntry("Plugin_Testing/fileLocator/testFileLocatorGetRootFile"));
		assertEquals(file1, new File(fileURL.getFile()));

		// remove the bundle
		bundle.uninstall();
		BundleTestingHelper.refreshPackages(context, new Bundle[] { bundle });
	}

	@Test
	public void testFileLocatorGetBundleFile02() throws BundleException, IOException, URISyntaxException {
		// install the bundle via reference
		BundleContext context = getContext();
		URL url = context.getBundle().getEntry("Plugin_Testing/fileLocator/testFileLocatorGetRootFile.jar");
		Bundle bundle = context.installBundle("reference:" + FileLocator.toFileURL(url).toExternalForm());
		BundleTestingHelper.refreshPackages(context, new Bundle[] { bundle });

		File file1 = FileLocator.getBundleFile(bundle);
		assertNotNull(file1);

		URL fileURL = FileLocator
				.toFileURL(context.getBundle().getEntry("Plugin_Testing/fileLocator/testFileLocatorGetRootFile.jar"));
		assertEquals(file1, new File(fileURL.getFile()));

		URL manifest = bundle.getEntry("META-INF/MANIFEST.MF");
		manifest = FileLocator.resolve(manifest);
		assertEquals("Expection jar protocol: " + manifest.toExternalForm(), "jar", manifest.getProtocol());

		String manifestExternal = manifest.toExternalForm();
		int index = manifestExternal.lastIndexOf('!');
		assertTrue("No ! found", index >= 0);
		String fileExternal = manifestExternal.substring(4, index);

		URL fileExternalURL = new URL(fileExternal);
		new File(fileExternalURL.toURI());

		// remove the bundle
		bundle.uninstall();
		BundleTestingHelper.refreshPackages(context, new Bundle[] { bundle });
	}

	private BundleContext getContext() {
		return FrameworkUtil.getBundle(FileLocatorTest.class).getBundleContext();
	}

	private Bundle getHostBundle(URL url) {
		String host = url.getHost();
		int dot = host.indexOf('.');
		Long hostId = Long.decode(dot < 0 ? host : host.substring(0, dot));
		assertNotNull(hostId);
		return getContext().getBundle(hostId.longValue());
	}

	private void assertBundleURL(URL url) {
		Bundle hostBundle = getHostBundle(url);
		assertNotNull(hostBundle);
		assertEquals("fileLocatorTest", hostBundle.getSymbolicName());
	}

	private void assertFragmentURL(URL url) {
		Bundle hostBundle = getHostBundle(url);
		assertNotNull(hostBundle);
		assertEquals("fileLocatorTest.nl", hostBundle.getSymbolicName());
	}
}
