/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.*;
import java.net.*;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Assert;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.url.*;

public class SystemBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(SystemBundleTests.class);
	}

	public void testSystemBundle01() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle01"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		ServiceReference[] refs = null;
		try {
			refs = systemContext.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) systemContext.getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle01/")); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle02() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle02"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle03() {
		// create/stop/ test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle03"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		String configArea = systemContext.getProperty("osgi.configuration.area"); //$NON-NLS-1$
		assertNotNull("config property is null", configArea); //$NON-NLS-1$
		assertTrue("Wrong configuration area", configArea.endsWith("testSystemBundle03/")); //$NON-NLS-1$ //$NON-NLS-2$
		// don't do anything; just put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle04() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle04"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		try {
			substitutesA.start();
		} catch (BundleException e) {
			fail("Unexpected bundle exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle05_1() {
		// create/install/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle05_1"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle05_2() {
		// create/install/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle05_2"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		Bundle substitutesA2 = null;
		try {
			substitutesA2 = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected exception installing", e); //$NON-NLS-1$
		}
		// assert the same bundle ID
		assertEquals("Bundle ids are not the same", substitutesA.getBundleId(), substitutesA2.getBundleId()); //$NON-NLS-1$
		// no need to start the bundle again it should have been persistently started
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA2.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

	}

	public void testSystemBundle06() {
		// create multiple instances test
		File config1 = OSGiTestsActivator.getContext().getDataFile("testSystemBundle06_1"); //$NON-NLS-1$
		Map<String, Object> configuration1 = new HashMap<String, Object>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile("testSystemBundle06_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<String, Object>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		// start framework 1 first
		try {
			equinox1.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox1.getState()); //$NON-NLS-1$
		// start framework 2 first
		try {
			equinox2.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox2.getState()); //$NON-NLS-1$

		// put the framework 1 back to the RESOLVED state
		try {
			equinox1.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox1.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		try {
			equinox2.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox2.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle07() {
		// test init twice
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle07_01"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		try {
			equinox.init();
		} catch (Exception e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		try {
			equinox.init();
		} catch (Exception e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}

		ServiceReference[] refs = null;
		try {
			refs = systemContext.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) systemContext.getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle07_01/")); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle08() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle08_1"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle08_2"); //$NON-NLS-1$
		configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		ServiceReference[] refs = null;
		try {
			refs = equinox.getBundleContext().getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) equinox.getBundleContext().getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle08_2/")); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle09() {
		// test FrameworkUtil.createFilter
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle09"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		Bundle testFilterA = null;
		try {
			testFilterA = equinox.getBundleContext().installBundle(installer.getBundleLocation("test.filter.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected exception installing", e); //$NON-NLS-1$
		}
		try {
			testFilterA.start();
		} catch (BundleException e) {
			fail("Unexpected exception starting test bundle", e); //$NON-NLS-1$
		}
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle10() {
		// create/start/update/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle10"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		final Exception[] failureException = new BundleException[1];
		final FrameworkEvent[] success = new FrameworkEvent[] {null};
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					success[0] = equinox.waitForStop(10000);
				} catch (InterruptedException e) {
					failureException[0] = e;
				}
			}
		}, "test waitForStop thread"); //$NON-NLS-1$
		t.start();
		try {
			// delay hack to allow t thread to block on waitForStop before we update.
			Thread.sleep(500);
		} catch (InterruptedException e) {
			fail("unexpected interuption", e);
		}
		try {
			equinox.update();
		} catch (BundleException e) {
			fail("Failed to update the framework", e); //$NON-NLS-1$
		}
		try {
			t.join();
		} catch (InterruptedException e) {
			fail("unexpected interuption", e); //$NON-NLS-1$
		}
		if (failureException[0] != null)
			fail("Error occurred while waiting", failureException[0]); //$NON-NLS-1$
		assertNotNull("Wait for stop event is null", success[0]); //$NON-NLS-1$
		assertEquals("Wait for stop event type is wrong", FrameworkEvent.STOPPED_UPDATE, success[0].getType()); //$NON-NLS-1$
		// TODO delay hack to allow the framework to get started again
		for (int i = 0; i < 5 && Bundle.ACTIVE != equinox.getState(); i++)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// nothing
			}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle11() {
		// test extra packages property
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle11"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "test.pkg1, test.pkg2"); //$NON-NLS-1$
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		PackageAdmin pa = (PackageAdmin) equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(PackageAdmin.class.getName()));
		ExportedPackage[] pkg1 = pa.getExportedPackages("test.pkg1"); //$NON-NLS-1$
		assertNotNull(pkg1);
		assertEquals("Wrong number of exports", 1, pkg1.length); //$NON-NLS-1$
		assertEquals("Wrong package name", "test.pkg1", pkg1[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$
		ExportedPackage[] pkg2 = pa.getExportedPackages("test.pkg2"); //$NON-NLS-1$
		assertNotNull(pkg2);
		assertEquals("Wrong number of exports", 1, pkg2.length); //$NON-NLS-1$
		assertEquals("Wrong package name", "test.pkg2", pkg2[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle12() {
		// Test stop FrameworkEvent
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle12"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// test timeout waiting for framework stop
		FrameworkEvent stopEvent = null;
		try {
			stopEvent = equinox.waitForStop(1000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.WAIT_TIMEDOUT, stopEvent.getType()); //$NON-NLS-1$

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			stopEvent = equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$
	}

	public void testSystemBundle13() {
		// create/install/start/stop clean test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle13"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		// initialize the framework again to the same configuration
		configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		substitutesA = equinox.getBundleContext().getBundle(1);

		// make sure the bundle is there
		assertNotNull("missing installed bundle", substitutesA); //$NON-NLS-1$
		assertEquals("Unexpected symbolic name", "substitutes.a", substitutesA.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		// initialize the framework again to the same configuration but use clean option
		configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		substitutesA = equinox.getBundleContext().getBundle(1);

		// make sure the bundle is there
		assertNull("Unexpected bundle is installed", substitutesA); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle14() {
		// Test startlevel property
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle14"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "10"); //$NON-NLS-1$
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		StartLevel st = (StartLevel) equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(StartLevel.class.getName()));
		assertNotNull("StartLevel service is null", st); //$NON-NLS-1$
		assertEquals("Unexpected start level", 10, st.getStartLevel()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}

		FrameworkEvent stopEvent = null;
		try {
			stopEvent = equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$
	}

	public void testSystemBundle16() {
		// test parent boot
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle17() {
		// test parent app
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_APP);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle18() {
		// test parent ext
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle19() {
		// test parent framework
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
		checkParentClassLoader(configuration);
	}

	private void checkParentClassLoader(Map<String, Object> configuration) {
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		Bundle test = null;
		try {
			test = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Failed to install bundle", e); //$NON-NLS-1$
		}
		try {
			Class activatorClazz = test.loadClass("substitutes.x.Ax"); //$NON-NLS-1$
			ClassLoader parentCL = activatorClazz.getClassLoader().getParent();
			String configParent = (String) configuration.get(Constants.FRAMEWORK_BUNDLE_PARENT);
			if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equals(configParent))
				assertTrue("Wrong parent", parentCL == ClassLoader.getSystemClassLoader()); //$NON-NLS-1$
			else if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equals(configParent))
				assertTrue("Wrong parent", parentCL == ClassLoader.getSystemClassLoader().getParent()); //$NON-NLS-1$
			else if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(configParent))
				assertTrue("Wrong parent", parentCL == equinox.getClass().getClassLoader()); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("failed to load class", e); //$NON-NLS-1$
		}
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}

		FrameworkEvent stopEvent = null;
		try {
			stopEvent = equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$
	}

	public void testChangeEE() throws IOException, BundleException {
		URL javaSE7Profile = OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getEntry("JavaSE-1.7.profile");
		URL javaSE8Profile = OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getEntry("JavaSE-1.8.profile");

		// configure equinox for javaSE 8
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.java.profile", javaSE8Profile.toExternalForm()); //$NON-NLS-1$

		Equinox equinox = new Equinox(configuration);
		equinox.start();

		// install a bundle that requires java 8
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		Map<String, String> testHeaders = new HashMap<String, String>();
		testHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testHeaders.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		testHeaders.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "JavaSE-1.8");
		File testBundle = createBundle(config, getName(), testHeaders);
		Bundle b = systemContext.installBundle("reference:file:///" + testBundle.getAbsolutePath()); //$NON-NLS-1$
		long bid = b.getBundleId();

		// should resolve fine
		Assert.assertTrue("Could not resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		equinox.stop();
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		// configure equinox for java 7
		configuration.put("osgi.java.profile", javaSE7Profile.toExternalForm());
		equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// bundle should fail to resolve
		b = equinox.getBundleContext().getBundle(bid);
		Assert.assertFalse("Could resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		equinox.stop();
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		// move back to java 8
		configuration.put("osgi.java.profile", javaSE8Profile.toExternalForm());
		equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// bundle should succeed to resolve again
		b = equinox.getBundleContext().getBundle(bid);
		Assert.assertTrue("Could not resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		equinox.stop();
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
	}

	public void testMRUBundleFileList() {
		doMRUBundleFileList(10);
	}

	//	public void testMRUBundleFileListExpectedToFail() {
	//		doMRUBundleFileList(0);
	//	}

	private void doMRUBundleFileList(int limit) {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.bundlefile.limit", Integer.toString(limit)); //$NON-NLS-1$//$NON-NLS-2$

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		File[] testBundles = null;
		try {
			testBundles = createBundles(new File(config, "bundles"), 3000); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected error creating budnles", e); //$NON-NLS-1$
		}
		for (int i = 0; i < testBundles.length; i++) {
			try {
				systemContext.installBundle("reference:file:///" + testBundles[i].getAbsolutePath()); //$NON-NLS-1$
			} catch (BundleException e) {
				fail("Unexpected install error", e); //$NON-NLS-1$
			}
		}
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		openAllBundleFiles(equinox.getBundleContext());

		final Exception[] failureException = new BundleException[1];
		final FrameworkEvent[] success = new FrameworkEvent[] {null};
		Thread waitForUpdate = new Thread(new Runnable() {
			public void run() {
				try {
					success[0] = equinox.waitForStop(10000);
				} catch (InterruptedException e) {
					failureException[0] = e;
				}
			}
		}, "test waitForStop thread"); //$NON-NLS-1$
		waitForUpdate.start();
		try {
			// delay hack to allow waitForUpdate thread to block on waitForStop before we update.
			Thread.sleep(100);
		} catch (InterruptedException e) {
			fail("unexpected interuption", e);
		}
		try {
			equinox.update();
		} catch (BundleException e) {
			fail("Failed to update the framework", e); //$NON-NLS-1$
		}
		try {
			waitForUpdate.join();
		} catch (InterruptedException e) {
			fail("unexpected interuption", e); //$NON-NLS-1$
		}
		if (failureException[0] != null)
			fail("Error occurred while waiting", failureException[0]); //$NON-NLS-1$

		// we can either have a hack here that waits until the system bundle is active
		// or we can just try to start it and race with the update() call above
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		openAllBundleFiles(equinox.getBundleContext());

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	private void openAllBundleFiles(BundleContext context) {
		Bundle[] bundles = context.getBundles();
		// get an entry from each bundle to ensure each one gets opened.
		try {
			for (int i = 0; i < bundles.length; i++) {
				assertNotNull("No manifest for: " + bundles[i], bundles[i].getEntry("/META-INF/MANIFEST.MF"));
			}
		} catch (Throwable t) {
			// An exception used to get thrown here when we tried to close 
			// the least used bundle file
			fail("Failed to get bundle entries", t);
		}
	}

	public void testURLExternalFormat01() {
		// create multiple instances test
		File config1 = OSGiTestsActivator.getContext().getDataFile("testURLExternalFormat01_1"); //$NON-NLS-1$
		Map<String, Object> configuration1 = new HashMap<String, Object>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile("testURLExternalFormat01_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<String, Object>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		Bundle test1 = null;
		Bundle test2 = null;
		try {
			test1 = systemContext1.installBundle(installer.getBundleLocation("test"));//$NON-NLS-1$
			test2 = systemContext2.installBundle(installer.getBundleLocation("test"));//$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		URL entry1 = test1.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry1", entry1); //$NON-NLS-1$
		URL entry2 = test2.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry2", entry2); //$NON-NLS-1$
		assertFalse("External form is equal: " + entry1.toExternalForm(), entry1.toExternalForm().equals(entry2.toExternalForm())); //$NON-NLS-1$
		assertFalse("Host is equal: " + entry1.getHost(), entry1.getHost().equals(entry2.getHost())); //$NON-NLS-1$
		assertFalse("URL is equal: " + entry1.toExternalForm(), entry1.equals(entry2)); //$NON-NLS-1$

		Bundle substitutes1 = null;
		Bundle substitutes2 = null;
		try {
			substitutes1 = systemContext1.installBundle(installer.getBundleLocation("substitutes.a"));//$NON-NLS-1$
			substitutes2 = systemContext2.installBundle(installer.getBundleLocation("substitutes.a"));//$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}

		entry1 = substitutes1.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry1", entry1); //$NON-NLS-1$
		entry2 = substitutes2.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry2", entry2); //$NON-NLS-1$
		assertFalse("External form is equal: " + entry1.toExternalForm(), entry1.toExternalForm().equals(entry2.toExternalForm())); //$NON-NLS-1$
		assertFalse("Host is equal: " + entry1.getHost(), entry1.getHost().equals(entry2.getHost())); //$NON-NLS-1$
		assertFalse("URL is equal: " + entry1.toExternalForm(), entry1.equals(entry2)); //$NON-NLS-1$

		// put the framework 1 back to the RESOLVED state
		try {
			equinox1.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox1.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		try {
			equinox2.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox2.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
	}

	class TestHandler extends AbstractURLStreamHandlerService {

		public URLConnection openConnection(URL u) throws IOException {
			throw new IOException();
		}

	}

	public void testURLMultiplexing01() throws BundleException {
		// create multiple instances of Equinox to test
		File config1 = OSGiTestsActivator.getContext().getDataFile(getName() + "_1");
		Map<String, Object> configuration1 = new HashMap<String, Object>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile(getName() + "_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<String, Object>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		// register a protocol hander in the "root" framework
		Dictionary props = new Hashtable();
		props.put(URLConstants.URL_HANDLER_PROTOCOL, getName().toLowerCase());
		ServiceRegistration handlerReg = OSGiTestsActivator.getContext().registerService(URLStreamHandlerService.class, new TestHandler(), props);
		try {
			URL baseTestUrl = new URL(getName().toLowerCase(), "", "/test/url");
			System.getProperties().put("test.url", baseTestUrl);
			System.setProperty("test.url.spec", baseTestUrl.toExternalForm());
		} catch (MalformedURLException e) {
			fail("Unexpected url exception.", e);
		}

		Bundle geturlBundle = systemContext1.installBundle(installer.getBundleLocation("geturl"));
		geturlBundle.start();
		PrivilegedAction geturlAction = (PrivilegedAction) systemContext1.getService(systemContext1.getServiceReference(PrivilegedAction.class));
		try {
			geturlAction.run();
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}

		// put the framework 1 back to the RESOLVED state
		try {
			equinox1.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox1.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		try {
			equinox2.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox2.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
		handlerReg.unregister();
	}

	public void testUUID() {
		File config1 = OSGiTestsActivator.getContext().getDataFile(getName() + "_1"); //$NON-NLS-1$
		Map configuration1 = new HashMap();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Failed init", e);
		}
		String uuid1_1 = equinox1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid1_1);

		File config2 = OSGiTestsActivator.getContext().getDataFile(getName() + "_2"); //$NON-NLS-1$
		Map configuration2 = new HashMap();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration1);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Failed init", e);
		}
		String uuid2_1 = equinox2.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid2_1);

		assertFalse("UUIDs are the same: " + uuid1_1, uuid1_1.equals(uuid2_1));

		try {
			equinox1.stop();
			equinox2.stop();
			equinox1.waitForStop(1000);
			equinox2.waitForStop(1000);
			equinox1.init();
			equinox2.init();
		} catch (BundleException e) {
			fail("Failed to re-init frameworks.", e);
		} catch (InterruptedException e) {
			fail("Failed to stop frameworks.", e);
		}

		String uuid1_2 = equinox1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid1_2);
		String uuid2_2 = equinox2.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid2_2);
		assertFalse("UUIDs are the same: " + uuid1_1, uuid1_1.equals(uuid1_2));
		assertFalse("UUIDs are the same: " + uuid1_2, uuid1_2.equals(uuid2_2));
		assertFalse("UUIDs are the same: " + uuid2_1, uuid2_1.equals(uuid2_2));

		try {
			equinox1.stop();
			equinox2.stop();
			equinox1.waitForStop(1000);
			equinox2.waitForStop(1000);
		} catch (BundleException e) {
			fail("Failed to re-init frameworks.", e);
		} catch (InterruptedException e) {
			fail("Failed to stop frameworks.", e);
		}
	}

	private void verifyUUID(String uuid) {
		assertNotNull("Null uuid.", uuid);
		StringTokenizer st = new StringTokenizer(uuid, "-");
		String[] uuidSections = new String[5];
		// All UUIDs must have 5 sections
		for (int i = 0; i < uuidSections.length; i++) {
			try {
				uuidSections[i] = "0x" + st.nextToken();
			} catch (NoSuchElementException e) {
				fail("Wrong number of uuid sections: " + uuid, e);
			}
		}
		// make sure there is not an extra section.
		try {
			st.nextToken();
			fail("Too many sections in uuid: " + uuid);
		} catch (NoSuchElementException e) {
			// expected
		}
		// now verify each section of the UUID can be decoded as a hex string and is the correct size
		for (int i = 0; i < uuidSections.length; i++) {
			int limit = 0;
			switch (i) {
				case 0 : {
					limit = 10; // "0x" + 4*<hexOctet> == 10 len
					break;
				}
				case 1 :
				case 2 :
				case 3 : {
					limit = 6; // "0x" + 2*<hexOctet> == 6 len
					break;
				}
				case 4 : {
					limit = 14; // "0x" + 6*<hexOctet> == 14 len
					break;
				}
				default :
					break;
			}
			assertTrue("UUISection is too big: " + uuidSections[i], uuidSections[i].length() <= limit);
			try {
				Long.decode(uuidSections[i]);
			} catch (NumberFormatException e) {
				fail("Invalid section: " + uuidSections[i], e);
			}
		}
	}

	public void testBug304213() {
		// test installing bundle with empty manifest
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		File bundleFile = null;
		try {
			File baseDir = new File(config, "bundles");
			baseDir.mkdirs();
			bundleFile = createBundle(baseDir, getName(), true, true);
		} catch (IOException e) {
			fail("Unexpected error creating bundles.", e);
		}
		try {
			systemContext.installBundle("reference:file:///" + bundleFile.getAbsolutePath()); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected install error", e); //$NON-NLS-1$
		}

		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// remove manifest for testing
		new File(bundleFile, "META-INF/MANIFEST.MF").delete();
		systemContext = equinox.getBundleContext();
		Bundle[] bundles = systemContext.getBundles();
		// get the headers from each bundle
		try {
			for (int i = 0; i < bundles.length; i++) {
				bundles[i].getHeaders(); //$NON-NLS-1$
			}
		} catch (Throwable t) {
			// An exception used to get thrown here when we tried to close 
			// the least used bundle file
			fail("Failed to get bundle entries", t);
		}

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug351083DevClassPath() throws InvalidSyntaxException {
		// create/start/stop/start/stop test
		BundleInstaller testBundleInstaller = new BundleInstaller("test_files/devCPTests", OSGiTestsActivator.getContext());

		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
			Map<String, Object> configuration = new HashMap<String, Object>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			configuration.put("osgi.dev", "../devCP");
			Equinox equinox = new Equinox(configuration);
			try {
				equinox.start();
			} catch (BundleException e) {
				fail("Unexpected exception in init()", e); //$NON-NLS-1$
			}
			BundleContext systemContext = equinox.getBundleContext();
			assertNotNull("System context is null", systemContext); //$NON-NLS-1$
			// try installing a bundle before starting
			Bundle tb1 = null;
			try {
				tb1 = systemContext.installBundle(testBundleInstaller.getBundleLocation("tb1")); //$NON-NLS-1$
			} catch (BundleException e1) {
				fail("failed to install a bundle", e1); //$NON-NLS-1$
			}
			URL resource = tb1.getResource("tb1/resource.txt");
			assertNotNull("Resource is null", resource);

			try {
				equinox.stop();
			} catch (BundleException e) {
				fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
			}
			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
			assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		} finally {
			try {
				testBundleInstaller.shutdown();
			} catch (BundleException e) {
				fail("Could not shutdown installer", e);
			}
		}
	}

	public void testBug352275() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "");
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void disabledTestBug351519RefreshEnabled() {
		// TODO this is expected to fail.  Not sure we should implement this
		doTestBug351519Refresh(Boolean.TRUE);
	}

	public void testBug351519RefreshDisabled() {
		doTestBug351519Refresh(Boolean.FALSE);
	}

	public void testBug351519RefreshDefault() {
		// Note that for the unity framework this defaults to false
		doTestBug351519Refresh(null);
	}

	public void testWeavingPersistence() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle test1 = null;
		try {
			test1 = systemContext.installBundle(installer.getBundleLocation("substitutes.a"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		long testID1 = test1.getBundleId();

		final Bundle testFinal1 = test1;
		ServiceRegistration reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal1.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"substitutes.x.Ax".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		try {
			testFinal1.loadClass("substitutes.x.Ax");
			testFinal1.loadClass("org.osgi.framework.hooks.bundle.FindHook");
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
		}
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(testID1);

		Bundle test2 = null;
		try {
			test2 = systemContext.installBundle(installer.getBundleLocation("exporter.importer1"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		long testID2 = test2.getBundleId();

		final Bundle testFinal2 = test2;
		reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal2.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"exporter.importer.test.Test1".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		try {
			testFinal2.loadClass("exporter.importer.test.Test1");
			testFinal2.loadClass("org.osgi.framework.hooks.service.FindHook");
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
		}

		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(testID1);
		test2 = systemContext.getBundle(testID2);

		BundleRevision rev1 = (BundleRevision) test1.adapt(BundleRevision.class);
		BundleRevision rev2 = (BundleRevision) test2.adapt(BundleRevision.class);
		BundleWiring wiring1 = rev1.getWiring();
		BundleWiring wiring2 = rev2.getWiring();

		assertNotNull("wiring1 is null", wiring1);
		assertNotNull("wiring2 is null", wiring2);

		List packages1 = wiring1.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		List packages2 = wiring2.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);

		// could make this a more complete check, but with the bug the dynamic wires 
		// are missing altogether because we fail to save the resolver state cache.
		assertEquals("Wrong number of wires for wiring1", 1, packages1.size());
		assertEquals("Wrong number of wires for wiring2", 1, packages2.size());

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
	}

	private void doTestBug351519Refresh(Boolean refreshDuplicates) {
		// Create a framework with equinox.refresh.duplicate.bsn=false configuration
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		if (refreshDuplicates != null) {
			configuration.put("equinox.refresh.duplicate.bsn", refreshDuplicates.toString());
		} else {
			// we default to false now
			refreshDuplicates = Boolean.FALSE;
		}
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		BundleContext systemContext = equinox.getBundleContext();

		systemContext.registerService(ResolverHookFactory.class, new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterResolvable(Collection candidates) {
						// nothing
					}

					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// resolve all singletons
						collisionCandidates.clear();
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// nothing
					}

					public void end() {
						// nothing
					}
				};
			}
		}, null);

		BundleInstaller testBundleInstaller = null;
		BundleInstaller testBundleResolver = null;
		try {
			testBundleResolver = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", systemContext);
			testBundleInstaller = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", getContext());
		} catch (InvalidSyntaxException e) {
			fail("Failed to create installers.", e);
		}
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle tb1v1 = null, tb1v2 = null;
		try {
			tb1v1 = systemContext.installBundle(testBundleInstaller.getBundleLocation("singleton.tb1v1")); //$NON-NLS-1$
			tb1v2 = systemContext.installBundle(testBundleInstaller.getBundleLocation("singleton.tb1v2")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}

		assertTrue("Could not resolve test bundles", testBundleResolver.resolveBundles(new Bundle[] {tb1v1, tb1v2}));
		Bundle[] refreshed = testBundleResolver.refreshPackages(new Bundle[] {tb1v1});
		if (refreshDuplicates) {
			List refreshedList = Arrays.asList(refreshed);
			assertEquals("Wrong number of refreshed bundles", 2, refreshed.length);
			assertTrue("Refreshed bundles does not include v1", refreshedList.contains(tb1v1));
			assertTrue("Refreshed bundles does not include v2", refreshedList.contains(tb1v2));
		} else {
			assertEquals("Wrong number of refreshed bundles", 1, refreshed.length);
			assertEquals("Refreshed bundles does not include v1", refreshed[0], tb1v1);
		}

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug375784() {
		// Create a framework with osgi.context.bootdelegation=true configuration
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.context.bootdelegation", "true");
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		// try installing a bundle before starting
		Bundle tb1;
		try {
			tb1 = systemContext.installBundle(installer.getBundleLocation("test.bug375784")); //$NON-NLS-1$
			tb1.start();
		} catch (BundleException e1) {
			fail("failed to install and start test bundle", e1); //$NON-NLS-1$
		}

		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected erorr stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug258209_1() throws BundleException {
		// create a framework to test thread context class loaders
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		ClassLoader current = Thread.currentThread().getContextClassLoader();
		Equinox equinox = new Equinox(configuration);
		equinox.init();
		Thread.currentThread().setContextClassLoader(current);

		BundleContext systemContext = equinox.getBundleContext();
		Bundle testTCCL = systemContext.installBundle(installer.getBundleLocation("test.tccl")); //$NON-NLS-1$
		equinox.adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(testTCCL));
		try {
			testTCCL.start();
		} catch (BundleException e) {
			fail("Unexpected exception starting bundle", e); //$NON-NLS-1$
		}

		assertEquals("Unexpected state", Bundle.RESOLVED, testTCCL.getState()); //$NON-NLS-1$
		// this will start the framework on the current thread; test that the correct tccl is used
		equinox.start();
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		// test that the correct tccl is used for framework update
		try {
			equinox.update();
			checkActive(testTCCL);
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
		systemContext = equinox.getBundleContext();
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		// test that the correct tccl is used for refresh packages
		equinox.adapt(FrameworkWiring.class).refreshBundles(Arrays.asList(testTCCL));
		checkActive(testTCCL);
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		// use the tccl service to start the test bundle.
		ClassLoader serviceTCCL = null;
		try {
			serviceTCCL = (ClassLoader) systemContext.getService(systemContext.getServiceReferences(ClassLoader.class.getName(), "(equinox.classloader.type=contextClassLoader)")[0]);//$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected", e);//$NON-NLS-1$
		}
		current = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(serviceTCCL);
		try {
			testTCCL.stop();
			testTCCL.start();
		} catch (BundleException e) {
			fail("Unepected", e); //$NON-NLS-1$
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}

	}

	private void checkActive(Bundle b) {
		try {
			// just a hack to make sure we are restarted
			Thread.sleep(500);
			if (b.getState() != Bundle.ACTIVE)
				Thread.sleep(500);
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBug413879() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle test1 = null;
		try {
			test1 = systemContext.installBundle(installer.getBundleLocation("substitutes.a"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}

		final Bundle testFinal1 = test1;
		ServiceRegistration reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal1.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"substitutes.x.Ax".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		ServiceRegistration<ResolverHookFactory> resolverHookReg = systemContext.registerService(ResolverHookFactory.class, new ResolverHookFactory() {
			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				// just trying to delay the resolve so that we get two threads trying to apply off the same snapshot
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return null;
			}
		}, null);

		final Set<Throwable> errors = Collections.newSetFromMap(new ConcurrentHashMap<Throwable, Boolean>());
		try {
			Runnable dynamicLoadClass = new Runnable() {
				@Override
				public void run() {
					try {
						testFinal1.loadClass("substitutes.x.Ax");
						testFinal1.loadClass("org.osgi.framework.hooks.bundle.FindHook");
					} catch (Throwable t) {
						errors.add(t);
					}
				}
			};
			Thread t1 = new Thread(dynamicLoadClass, getName() + "-1");
			Thread t2 = new Thread(dynamicLoadClass, getName() + "-2");
			t1.start();
			t2.start();
			t1.join();
			t2.join();
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
			resolverHookReg.unregister();
		}
		// put the framework back to the RESOLVED state
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected error stopping framework", e); //$NON-NLS-1$
		}
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}

		if (!errors.isEmpty()) {
			fail("Failed to resolve dynamic", errors.iterator().next());
		}
	}

	public void testBug414070() throws BundleException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		Bundle systemBundle = systemContext.getBundle();

		Bundle chainTest = systemContext.installBundle(installer.getBundleLocation("chain.test")); //$NON-NLS-1$

		final Bundle chainTestD = systemContext.installBundle(installer.getBundleLocation("chain.test.d")); //$NON-NLS-1$
		Bundle chainTestA = systemContext.installBundle(installer.getBundleLocation("chain.test.a")); //$NON-NLS-1$
		Bundle chainTestB = systemContext.installBundle(installer.getBundleLocation("chain.test.b")); //$NON-NLS-1$
		Bundle chainTestC = systemContext.installBundle(installer.getBundleLocation("chain.test.c")); //$NON-NLS-1$
		systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!chainTestD.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"chain.test.d.DMultipleChain1".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		equinox.start();

		chainTest.loadClass("chain.test.TestMultiChain").newInstance(); //$NON-NLS-1$
		// force a dynamic wire to cause a cycle
		chainTestD.loadClass("chain.test.a.AMultiChain1");

		// make sure all bundles are active now
		assertEquals("A is not active.", Bundle.ACTIVE, chainTestA.getState());
		assertEquals("B is not active.", Bundle.ACTIVE, chainTestB.getState());
		assertEquals("C is not active.", Bundle.ACTIVE, chainTestC.getState());
		assertEquals("D is not active.", Bundle.ACTIVE, chainTestD.getState());
		// record STOPPING order
		final List<Bundle> stoppingOrder = new ArrayList<Bundle>();
		systemContext.addBundleListener(new SynchronousBundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.STOPPING) {
					stoppingOrder.add(event.getBundle());
				}
			}
		});
		equinox.stop();
		try {
			equinox.waitForStop(10000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			fail("Unexpected interruption.", e);
		}

		List<Bundle> expectedOrder = Arrays.asList(systemBundle, chainTest, chainTestA, chainTestB, chainTestC, chainTestD);
		assertEquals("Wrong stopping order", expectedOrder.toArray(), stoppingOrder.toArray());
	}

	public void testBug412228() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();

		Bundle b = systemContext.installBundle(installer.getBundleLocation("test.bug412228"));
		b.start();
		equinox.start();

		long startTime = System.currentTimeMillis();
		equinox.stop();
		try {
			equinox.waitForStop(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			fail("Unexpected interruption.", e);
		}
		long stopTime = System.currentTimeMillis() - startTime;
		if (stopTime > 2000) {
			fail("waitForStop time took too long: " + stopTime);
		}

	}

	public void testBug432632() throws BundleException, IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		config.mkdirs();
		// create a config.ini with some system property substitutes
		Properties configIni = new Properties();
		configIni.setProperty("test.substitute1", "Some.$test.prop1$.test");
		configIni.setProperty("test.substitute2", "Some.$test.prop2$.test");
		configIni.store(new FileOutputStream(new File(config, "config.ini")), "Test config.ini");
		// Only provide substitution for the first prop
		System.setProperty("test.prop1", "PASSED");
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		// check for substitution
		assertEquals("Wrong value for test.substitute1", "Some.PASSED.test", systemContext.getProperty("test.substitute1"));
		// check that non-substitution keeps $ delimiters.
		assertEquals("Wrong value for test.substitute2", "Some.$test.prop2$.test", systemContext.getProperty("test.substitute2"));
		equinox.stop();
		try {
			equinox.waitForStop(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			fail("Unexpected interruption.", e);
		}

	}

	public void testDynamicSecurityManager() throws BundleException {
		SecurityManager sm = System.getSecurityManager();
		assertNull("SecurityManager must be null to test.", sm);
		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
			Map<String, Object> configuration = new HashMap<String, Object>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			Equinox equinox = new Equinox(configuration);
			try {
				equinox.start();
			} catch (BundleException e) {
				fail("Unexpected exception in start()", e); //$NON-NLS-1$
			}
			Bundle substitutesA = null;
			try {
				substitutesA = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
			} catch (BundleException e1) {
				fail("failed to install a bundle", e1); //$NON-NLS-1$
			}
			assertTrue("BundleCould not resolve.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(substitutesA)));
			substitutesA.adapt(BundleWiring.class).findEntries("/", null, 0);
			// set security manager after resolving
			System.setSecurityManager(new SecurityManager() {

				@Override
				public void checkPermission(Permission perm, Object context) {
					// do nothing
				}

				@Override
				public void checkPermission(Permission perm) {
					// do nothing
				}
			});
			equinox.stop();
			try {
				FrameworkEvent event = equinox.waitForStop(10000);
				assertEquals("Wrong event.", FrameworkEvent.STOPPED, event.getType());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("Unexpected interruption.", e);
			}
			assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		} finally {
			System.setSecurityManager(null);
		}
	}

	static final String nullTest = "null.test";

	public void testNullConfigurationValue() throws BundleException {
		System.setProperty(nullTest, "system");
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(nullTest, null);
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		String nullValue = equinox.getBundleContext().getProperty(nullTest);
		assertNull(nullTest + " is not null: " + nullValue, nullValue);
		String systemNullValue = System.getProperty(nullTest);
		assertEquals("Wrong system null value.", "system", systemNullValue);
		equinox.stop();
	}

	public void testSystemNLFragment() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.nl", "zh");
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		equinox.getHeaders();
		BundleContext systemContext = equinox.getBundleContext();
		Bundle systemNLS = systemContext.installBundle(installer.getBundleLocation("test.system.nls"));
		equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(systemNLS));
		assertEquals("Wrong fragment state", Bundle.RESOLVED, systemNLS.getState());
		assertEquals("Wrong header value.", "TEST NAME", equinox.getHeaders().get(Constants.BUNDLE_NAME));
		equinox.stop();
	}

	public void testNullConfiguration() {
		new Equinox(null);
	}

	public void testOSGiDevSetsCheckConfiguration() throws BundleException {
		String originalCheckConfiguration = System.clearProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION);
		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName());
			Map<String, Object> configuration = new HashMap<String, Object>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			configuration.put("osgi.dev", "true");
			Equinox equinox = new Equinox(configuration);
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			assertEquals("Wrong value for: " + EquinoxConfiguration.PROP_CHECK_CONFIGURATION, "true", systemContext.getProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION));
			equinox.stop();
		} finally {
			if (originalCheckConfiguration != null) {
				System.setProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION, originalCheckConfiguration);
			}
		}
	}

	private static File[] createBundles(File outputDir, int bundleCount) throws IOException {
		outputDir.mkdirs();

		File[] bundles = new File[bundleCount];

		for (int i = 0; i < bundleCount; i++)
			bundles[i] = createBundle(outputDir, "-b" + i, false, false); //$NON-NLS-1$

		return bundles;
	}

	private static File createBundle(File outputDir, String id, boolean emptyManifest, boolean dirBundle) throws IOException {
		File file = new File(outputDir, "bundle" + id + (dirBundle ? "" : ".jar")); //$NON-NLS-1$ //$NON-NLS-2$
		if (!dirBundle) {
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), createManifest(id, emptyManifest));
			jos.flush();
			jos.close();
		} else {
			File manifest = new File(file, "META-INF/MANIFEST.MF");
			manifest.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(manifest);
			createManifest(id, emptyManifest).write(out);
			out.close();
		}
		return file;
	}

	private static Manifest createManifest(String id, boolean emptyManifest) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!emptyManifest) {
			attributes.putValue("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
			attributes.putValue("Bundle-SymbolicName", "bundle" + id); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return manifest;
	}

	private static File createBundle(File outputDir, String bundleName, Map<String, String> headers) throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
		File file = new File(outputDir, "bundle" + bundleName + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), m);
		jos.flush();
		jos.close();
		return file;
	}
}
