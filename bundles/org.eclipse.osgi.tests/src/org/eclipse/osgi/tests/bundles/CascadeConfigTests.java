/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

public class CascadeConfigTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(CascadeConfigTests.class);
	}

	public void testCascadeConfig() throws Exception {
		// First create a framework with the 'parent' configuration
		File configParent = OSGiTestsActivator.getContext().getDataFile(getName() + "_parent");
		Map<String, Object> parentMap = new HashMap<String, Object>();
		parentMap.put(Constants.FRAMEWORK_STORAGE, configParent.getAbsolutePath());
		Equinox equinox = new Equinox(parentMap);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		systemContext.installBundle(installer.getBundleLocation("test"));

		equinox.stop();
		equinox.waitForStop(10000);

		// Now create a child framework and make sure test1 bundle is there
		File configChild = OSGiTestsActivator.getContext().getDataFile(getName() + "_child");
		Map<String, Object> childMap = new HashMap<String, Object>();
		childMap.put(Constants.FRAMEWORK_STORAGE, configChild.getAbsolutePath());
		childMap.put("osgi.sharedConfiguration.area", configParent.getCanonicalPath());

		equinox = new Equinox(childMap);
		equinox.init();

		systemContext = equinox.getBundleContext();
		Bundle test1 = systemContext.getBundle(installer.getBundleLocation("test"));
		assertNotNull("Missing bundle.", test1);

		systemContext.installBundle(installer.getBundleLocation("test2"));

		equinox.stop();
		equinox.waitForStop(10000);

		// reuse the same configuration and make sure both bundles are there
		equinox = new Equinox(childMap);
		equinox.init();

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(installer.getBundleLocation("test"));
		assertNotNull("Missing bundle.", test1);
		Bundle test2 = systemContext.getBundle(installer.getBundleLocation("test2"));
		assertNotNull("Missing bundle.", test2);

		equinox.stop();
		equinox.waitForStop(10000);

		// restart using the parent and make sure only the test1 bundle is there
		equinox = new Equinox(parentMap);
		equinox.init();

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(installer.getBundleLocation("test"));
		assertNotNull("Missing bundle.", test1);
		test2 = systemContext.getBundle(installer.getBundleLocation("test2"));
		assertNull("Unexpected bundle.", test2);

		equinox.stop();
		equinox.waitForStop(10000);
	}

}
