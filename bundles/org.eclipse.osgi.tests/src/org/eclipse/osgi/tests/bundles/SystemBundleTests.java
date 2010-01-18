/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.net.URL;
import java.util.Properties;
import java.util.jar.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class SystemBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(SystemBundleTests.class);
	}

	public void testSystemBundle01() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle01"); //$NON-NLS-1$
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration1 = new Properties();
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
		Properties configuration2 = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
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
		configuration = new Properties();
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
		configuration = new Properties();
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
		Properties configuration = new Properties();
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
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle17() {
		// test parent app
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_APP);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle18() {
		// test parent ext
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle19() {
		// test parent framework
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
		checkParentClassLoader(configuration);
	}

	private void checkParentClassLoader(Properties configuration) {
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
			String configParent = configuration.getProperty(Constants.FRAMEWORK_BUNDLE_PARENT);
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

	public void testBug253942() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testBug253942"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.bundlefile.limit", "10"); //$NON-NLS-1$//$NON-NLS-2$

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

		File[] testBundles = null;
		try {
			testBundles = createBundles(new File(config, "bundles"), 20); //$NON-NLS-1$
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
		systemContext = equinox.getBundleContext();
		Bundle[] bundles = systemContext.getBundles();
		// get an entry from each bundle to ensure each one gets opened.
		try {
			for (int i = 0; i < bundles.length; i++) {
				bundles[i].getEntry("/META-INF/MANIFEST.MF"); //$NON-NLS-1$
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

	public void testURLExternalFormat01() {
		// create multiple instances test
		File config1 = OSGiTestsActivator.getContext().getDataFile("testURLExternalFormat01_1"); //$NON-NLS-1$
		Properties configuration1 = new Properties();
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
		Properties configuration2 = new Properties();
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

	private static File[] createBundles(File outputDir, int bundleCount) throws IOException {
		outputDir.mkdirs();

		File[] bundles = new File[bundleCount];

		for (int i = 0; i < bundleCount; i++)
			bundles[i] = createBundle(outputDir, "-b" + i); //$NON-NLS-1$

		return bundles;
	}

	private static File createBundle(File outputDir, String id) throws IOException {
		File file = new File(outputDir, "bundle" + id + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), createManifest(id));
		jos.flush();
		jos.close();
		return file;
	}

	private static Manifest createManifest(String id) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		attributes.putValue("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		attributes.putValue("Bundle-SymbolicName", "bundle" + id); //$NON-NLS-1$ //$NON-NLS-2$
		return manifest;
	}
}
