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
package org.eclipse.osgi.tests.securityadmin;

import java.io.File;
import java.security.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.startlevel.StartLevel;

public class SecurityManagerTests extends AbstractBundleTests {
	private static final PermissionInfo hostFragmentPermission = new PermissionInfo(BundlePermission.class.getName(), "*", "host,fragment"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final PermissionInfo hostFragmentProvidePermission = new PermissionInfo(BundlePermission.class.getName(), "*", "host,fragment,provide"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final PermissionInfo allPackagePermission = new PermissionInfo(PackagePermission.class.getName(), "*", "import,export"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final PermissionInfo importPackagePermission = new PermissionInfo(PackagePermission.class.getName(), "*", "import"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final PermissionInfo importFrameworkPackagePermission = new PermissionInfo(PackagePermission.class.getName(), "org.osgi.framework", "import"); //$NON-NLS-1$ //$NON-NLS-2$
	private Policy previousPolicy;

	public static Test suite() {
		return new TestSuite(SecurityManagerTests.class);
	}

	protected void setUp() throws Exception {
		if (System.getSecurityManager() != null)
			fail("Cannot test with security manager set"); //$NON-NLS-1$
		previousPolicy = Policy.getPolicy();
		final Permission allPermission = new AllPermission();
		final PermissionCollection allPermissions = new PermissionCollection() {
			private static final long serialVersionUID = 3258131349494708277L;

			// A simple PermissionCollection that only has AllPermission
			public void add(Permission permission) {
				//no adding to this policy
			}

			public boolean implies(Permission permission) {
				return true;
			}

			public Enumeration elements() {
				return new Enumeration() {
					int cur = 0;

					public boolean hasMoreElements() {
						return cur < 1;
					}

					public Object nextElement() {
						if (cur == 0) {
							cur = 1;
							return allPermission;
						}
						throw new NoSuchElementException();
					}
				};
			}
		};

		Policy.setPolicy(new Policy() {

			public PermissionCollection getPermissions(CodeSource codesource) {
				return allPermissions;
			}

			public void refresh() {
				// nothing
			}

		});
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (System.getSecurityManager() != null)
			System.setSecurityManager(null);
		Policy.setPolicy(previousPolicy);
	}

	public void testEnableSecurityManager01() {
		File config = OSGiTestsActivator.getContext().getDataFile("testEnableSecurityManager01"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}
		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
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
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testEnableSecurityManager02() throws BundleException {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testEnableSecurityManager02"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}

		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing host and fragment to test bug 245678
		String locationSecurityA = installer.getBundleLocation("security.a"); //$NON-NLS-1$
		String locationSecurityAFragA = installer.getBundleLocation("security.a.frag.a"); //$NON-NLS-1$
		// set the security for the host and fragment
		ConditionalPermissionAdmin ca = (ConditionalPermissionAdmin) systemContext.getService(systemContext.getServiceReference(ConditionalPermissionAdmin.class.getName()));
		ConditionalPermissionUpdate update = ca.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(ca.newConditionalPermissionInfo(null, null, new PermissionInfo[] {hostFragmentPermission, allPackagePermission}, ConditionalPermissionInfo.ALLOW));
		assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$

		Bundle securityA = systemContext.installBundle(locationSecurityA);
		systemContext.installBundle(locationSecurityAFragA);

		equinox.start();

		try {
			securityA.start();
		} catch (BundleException e) {
			fail("Failed to start securityA", e); //$NON-NLS-1$
		} finally {
			// put the framework back to the RESOLVED state
			equinox.stop();

			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testEnableSecurityManager03() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile("testEnableSecurityManager03"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}

		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle to tests bug 
		String locationSecurityA = installer.getBundleLocation("security.a"); //$NON-NLS-1$
		// set the security for the bundle
		ConditionalPermissionAdmin ca = (ConditionalPermissionAdmin) systemContext.getService(systemContext.getServiceReference(ConditionalPermissionAdmin.class.getName()));
		ConditionalPermissionUpdate update = ca.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(ca.newConditionalPermissionInfo(null, null, new PermissionInfo[] {hostFragmentPermission, importPackagePermission}, ConditionalPermissionInfo.ALLOW));
		assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$

		Bundle securityA = systemContext.installBundle(locationSecurityA);

		equinox.start();
		PackageAdmin pa = (PackageAdmin) systemContext.getService(systemContext.getServiceReference(PackageAdmin.class.getName()));

		try {
			assertTrue(pa.resolveBundles(new Bundle[] {securityA}));
			ExportedPackage[] eps = pa.getExportedPackages(securityA);
			assertNull("Found unexpected exports", eps); //$NON-NLS-1$
			RequiredBundle[] rbs = pa.getRequiredBundles(securityA.getSymbolicName());
			assertNull("Found unexpected required bundle", rbs); //$NON-NLS-1$
			// grant export permission
			update = ca.newConditionalPermissionUpdate();
			rows = update.getConditionalPermissionInfos();
			rows.clear();
			rows.add(ca.newConditionalPermissionInfo(null, null, new PermissionInfo[] {hostFragmentProvidePermission, allPackagePermission}, ConditionalPermissionInfo.ALLOW));
			assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$

			securityA.uninstall();
			securityA = systemContext.installBundle(locationSecurityA);
			assertTrue(pa.resolveBundles(new Bundle[] {securityA}));
			eps = pa.getExportedPackages(securityA);
			assertNotNull("Did not find expected exports", eps); //$NON-NLS-1$
			assertEquals("Wrong number of exports found", 1, eps.length); //$NON-NLS-1$
			rbs = pa.getRequiredBundles(securityA.getSymbolicName());
			assertNotNull("Did not find required bundle", eps); //$NON-NLS-1$
			assertEquals("Wrong number of required bundles found", 1, rbs.length); //$NON-NLS-1$

		} finally {
			// put the framework back to the RESOLVED state
			equinox.stop();

			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testEnableSecurityManager04() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile("testEnableSecurityManager04"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}

		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle to tests bug 
		String locationLinkA = installer.getBundleLocation("test.link.a"); //$NON-NLS-1$
		String locationLinkAClient = installer.getBundleLocation("test.link.a.client"); //$NON-NLS-1$

		// set the security for the bundles
		ConditionInfo linkACondition = new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {locationLinkA});
		ConditionInfo linkAClientCondition = new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {locationLinkAClient});
		PermissionInfo filterPermission = new PermissionInfo(PackagePermission.class.getName(), "(&(name=test.link.a)(package.name=test.link.*))", "import"); //$NON-NLS-1$ //$NON-NLS-2$
		ConditionalPermissionAdmin ca = (ConditionalPermissionAdmin) systemContext.getService(systemContext.getServiceReference(ConditionalPermissionAdmin.class.getName()));
		ConditionalPermissionUpdate update = ca.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(ca.newConditionalPermissionInfo(null, new ConditionInfo[] {linkACondition}, new PermissionInfo[] {hostFragmentProvidePermission, allPackagePermission}, ConditionalPermissionInfo.ALLOW));
		rows.add(ca.newConditionalPermissionInfo(null, new ConditionInfo[] {linkAClientCondition}, new PermissionInfo[] {importFrameworkPackagePermission, filterPermission}, ConditionalPermissionInfo.ALLOW));
		assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$

		Bundle linkA = systemContext.installBundle(locationLinkA);
		Bundle linkAClient = systemContext.installBundle(locationLinkAClient);
		equinox.start();
		PackageAdmin pa = (PackageAdmin) systemContext.getService(systemContext.getServiceReference(PackageAdmin.class.getName()));

		try {
			assertTrue(pa.resolveBundles(new Bundle[] {linkA, linkAClient}));
			// change import permission to fail filter match
			filterPermission = new PermissionInfo(PackagePermission.class.getName(), "(&(name=fail.match)(package.name=test.link.*))", "import"); //$NON-NLS-1$ //$NON-NLS-2$
			update = ca.newConditionalPermissionUpdate();
			rows = update.getConditionalPermissionInfos();
			rows.clear();
			rows.add(ca.newConditionalPermissionInfo(null, new ConditionInfo[] {linkACondition}, new PermissionInfo[] {hostFragmentProvidePermission, allPackagePermission}, ConditionalPermissionInfo.ALLOW));
			rows.add(ca.newConditionalPermissionInfo(null, new ConditionInfo[] {linkAClientCondition}, new PermissionInfo[] {importFrameworkPackagePermission, filterPermission}, ConditionalPermissionInfo.ALLOW));
			assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$
			pa.refreshPackages(new Bundle[] {linkA, linkAClient});
			// hack to wait for refresh to end
			Thread.sleep(2000);
			assertEquals("linkA has wrong state", Bundle.RESOLVED, linkA.getState()); //$NON-NLS-1$
			assertEquals("linkAClient has wrong state", Bundle.INSTALLED, linkAClient.getState()); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// fail
			fail("interrupted", e); //$NON-NLS-1$
		} finally {
			// put the framework back to the RESOLVED state
			equinox.stop();

			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testEnableSecurityManager05() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}

		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle to tests bug 
		String locationLinkA = installer.getBundleLocation("test.link.a"); //$NON-NLS-1$
		String locationLinkAClient = installer.getBundleLocation("test.link.a.client"); //$NON-NLS-1$

		Bundle linkA = systemContext.installBundle(locationLinkA);
		Bundle linkAClient = systemContext.installBundle(locationLinkAClient);
		equinox.start();

		try {
			PackageAdmin pa = (PackageAdmin) systemContext.getService(systemContext.getServiceReference(PackageAdmin.class.getName()));
			assertTrue(pa.resolveBundles(new Bundle[] {linkA, linkAClient}));
			linkA.uninstall();
			linkAClient.uninstall();

			linkA = systemContext.installBundle(locationLinkA);
			linkAClient = systemContext.installBundle(locationLinkAClient);
			assertTrue(pa.resolveBundles(new Bundle[] {linkA, linkAClient}));
		} finally {
			// put the framework back to the RESOLVED state
			equinox.stop();

			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testLocalization01() throws BundleException {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testLocalization01"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}
		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing host and fragment to test bug 245678
		String locationSecurityA = installer.getBundleLocation("security.a"); //$NON-NLS-1$
		String locationSecurityAFragA = installer.getBundleLocation("security.a.frag.a"); //$NON-NLS-1$
		// set the security for the host and fragment
		ConditionalPermissionAdmin ca = (ConditionalPermissionAdmin) systemContext.getService(systemContext.getServiceReference(ConditionalPermissionAdmin.class.getName()));
		ConditionalPermissionUpdate update = ca.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(ca.newConditionalPermissionInfo(null, null, new PermissionInfo[] {hostFragmentPermission, allPackagePermission}, ConditionalPermissionInfo.ALLOW));
		assertTrue("Cannot commit rows", update.commit()); //$NON-NLS-1$

		Bundle securityA = systemContext.installBundle(locationSecurityA);
		systemContext.installBundle(locationSecurityAFragA);

		equinox.start();

		try {
			securityA.start();
			Dictionary headers = securityA.getHeaders("en_US"); //$NON-NLS-1$
			String name = (String) headers.get(Constants.BUNDLE_NAME);
			assertEquals("Wrong Bundle-Name", "en_US", name); //$NON-NLS-1$ //$NON-NLS-2$

			headers = securityA.getHeaders("en"); //$NON-NLS-1$
			name = (String) headers.get(Constants.BUNDLE_NAME);
			assertEquals("Wrong Bundle-Name", "default", name); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Failed to start securityA", e); //$NON-NLS-1$
		} finally {
			// put the framework back to the RESOLVED state
			equinox.stop();

			try {
				equinox.waitForStop(10000);
			} catch (InterruptedException e) {
				fail("Unexpected interrupted exception", e); //$NON-NLS-1$
			}
		}
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testBug254600() {
		File config = OSGiTestsActivator.getContext().getDataFile("testBug254600"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}
		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle securityB = null;
		long idB = -1;
		try {
			String locationSecurityA = installer.getBundleLocation("security.a"); //$NON-NLS-1$
			String locationSecurityB = installer.getBundleLocation("security.b"); //$NON-NLS-1$
			systemContext.installBundle(locationSecurityA);
			securityB = systemContext.installBundle(locationSecurityB);
			idB = securityB.getBundleId();
		} catch (BundleException e) {
			fail("Failed to install security test bundles", e); //$NON-NLS-1$
		}

		try {
			securityB.start();
			securityB.stop();
		} catch (BundleException e) {
			fail("Failed to start security.b bundle", e); //$NON-NLS-1$
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
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		securityB = systemContext.getBundle(idB);

		try {
			securityB.start();
			securityB.stop();
		} catch (BundleException e) {
			fail("Failed to start security.b bundle", e); //$NON-NLS-1$
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
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

	public void testBug287750() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Properties configuration = new Properties();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}
		assertNotNull("SecurityManager is null", System.getSecurityManager()); //$NON-NLS-1$
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle testBundle = null;
		try {
			String locationTestBundle = installer.getBundleLocation("test.bug287750"); //$NON-NLS-1$
			testBundle = systemContext.installBundle(locationTestBundle);
		} catch (BundleException e) {
			fail("Failed to install security test bundles", e); //$NON-NLS-1$
		}

		try {
			testBundle.start();
		} catch (BundleException e) {
			fail("Failed to start security.b bundle", e); //$NON-NLS-1$
		}
		StartLevel sl = (StartLevel) systemContext.getService(systemContext.getServiceReference(StartLevel.class.getName()));
		if (sl.getStartLevel() != 10)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// nothing
			}
		assertEquals("Wrong startlevel", 10, sl.getStartLevel()); //$NON-NLS-1$
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
		assertNull("SecurityManager is not null", System.getSecurityManager()); //$NON-NLS-1$
	}

}
