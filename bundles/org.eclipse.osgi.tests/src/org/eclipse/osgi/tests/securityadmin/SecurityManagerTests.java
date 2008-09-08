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
import org.osgi.framework.launch.SystemBundle;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityManagerTests extends AbstractBundleTests {
	private static final PermissionInfo hostFragmentPermission = new PermissionInfo(BundlePermission.class.getName(), "*", "host,fragment"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final PermissionInfo allPackagePermission = new PermissionInfo(PackagePermission.class.getName(), "*", "import,export"); //$NON-NLS-1$ //$NON-NLS-2$
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		configuration.put(Framework.PROP_EQUINOX_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		configuration.put(Framework.PROP_EQUINOX_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		ConditionalPermissionsUpdate update = ca.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(ca.createConditionalPermissionInfoBase(null, null, new PermissionInfo[] {hostFragmentPermission, allPackagePermission}, ConditionalPermissionInfoBase.ALLOW));
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

	public void testLocalization01() throws BundleException {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testLocalization01"); //$NON-NLS-1$
		Properties configuration = new Properties();
		configuration.put(SystemBundle.STORAGE, config.getAbsolutePath());
		configuration.put(Framework.PROP_EQUINOX_SECURITY, Framework.SECURITY_OSGI);
		Equinox equinox = new Equinox();
		equinox.init(configuration);
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
		ConditionalPermissionsUpdate update = ca.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(ca.createConditionalPermissionInfoBase(null, null, new PermissionInfo[] {hostFragmentPermission, allPackagePermission}, ConditionalPermissionInfoBase.ALLOW));
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
}
