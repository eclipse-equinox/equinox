/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.net.URL;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.launch.SystemBundle;

public class SystemBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(SystemBundleTests.class);
	}

	public void testSystemBundle01() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle01"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
		try {
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration1.put(SystemBundle.STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox();
		equinox1.init(configuration1);
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile("testSystemBundle06_2"); //$NON-NLS-1$
		Properties configuration2 = new Properties();
		configuration2.put(SystemBundle.STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox();
		equinox2.init(configuration2);
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

		// put the framework 1 back to the RESOLVED state
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);

		config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle07_02"); //$NON-NLS-1$
		configuration = new Properties();
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		try {
			equinox.init(configuration);
			fail("Failed to throw illegal state exception on double init"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// expected
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
			equinox.init(configuration);
			fail("Failed to throw illegal state exception on double init"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// expected
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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

		config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle08_2"); //$NON-NLS-1$
		configuration = new Properties();
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		equinox.init(configuration);

		try {
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
			fail("Unexpected exception starting test bundle", e);
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
}
