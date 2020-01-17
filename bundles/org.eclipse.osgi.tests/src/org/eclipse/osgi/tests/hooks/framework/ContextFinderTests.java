/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.launch.Framework;

public class ContextFinderTests extends AbstractFrameworkHookTests {

	private Map<String, String> configuration;
	private Framework framework;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		framework = createFramework(configuration);
		initAndStart(framework);
	}

	@Override
	protected void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	public void testContextClassLoaderNullLocal() throws InvalidSyntaxException, IOException {
		BundleContext bc = framework.getBundleContext();
		ClassLoader contextFinder = bc.getService(bc.getServiceReferences(ClassLoader.class, "(equinox.classloader.type=contextClassLoader)").iterator().next());
		Enumeration<URL> result = contextFinder.getResources("does/not/exist.txt");
		assertNotNull("Null result.", result);
		assertFalse("Found unexpected result", result.hasMoreElements());
	}

}
