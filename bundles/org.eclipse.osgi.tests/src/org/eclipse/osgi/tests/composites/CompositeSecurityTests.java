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
package org.eclipse.osgi.tests.composites;

import java.security.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.*;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;
import org.osgi.service.permissionadmin.PermissionInfo;

public class CompositeSecurityTests extends AbstractCompositeTests {
	public static Test suite() {
		return new TestSuite(CompositeSecurityTests.class);
	}

	private Policy previousPolicy;

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

	public void testLinkSecurity01() {
		Map linkManifest = new HashMap();
		linkManifest.put(Constants.BUNDLE_SYMBOLICNAME, "testLinkSecurity01"); //$NON-NLS-1$
		linkManifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		linkManifest.put(Constants.IMPORT_PACKAGE, "org.eclipse.osgi.tests.bundles"); //$NON-NLS-1$
		linkManifest.put(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_IMPORT, "(objectClass=org.eclipse.osgi.tests.bundles.BundleInstaller)"); //$NON-NLS-1$
		CompositeBundle composite = createCompositeBundle(linkBundleFactory, "testLinkSecurity01", null, linkManifest, true, true); //$NON-NLS-1$

		startCompositeBundle(composite, false);

		stopCompositeBundle(composite);
	}

	public void testLinkSecurity02() {
		Map linkManifest = new HashMap();
		linkManifest.put(Constants.BUNDLE_SYMBOLICNAME, "testLinkSecurity02"); //$NON-NLS-1$
		linkManifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		linkManifest.put(Constants.IMPORT_PACKAGE, "org.eclipse.osgi.tests.bundles"); //$NON-NLS-1$
		linkManifest.put(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_IMPORT, "(objectClass=org.eclipse.osgi.tests.bundles.BundleInstaller)"); //$NON-NLS-1$
		CompositeBundle composite = createCompositeBundle(linkBundleFactory, "testLinkSecurity02", null, linkManifest, true, true); //$NON-NLS-1$
		Bundle linkD = installIntoChild(composite.getCompositeFramework(), "test.link.d"); //$NON-NLS-1$

		ConditionInfo[] conditions = new ConditionInfo[] {new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {linkD.getLocation()})};
		ConditionalPermissionAdmin childCondAdmin = getCondPermAdmin(composite.getCompositeFramework().getBundleContext());
		childCondAdmin.addConditionalPermissionInfo(conditions, new PermissionInfo[] {});
		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.RESOLVE_ERROR, e.getType()); //$NON-NLS-1$
		}

		ConditionalPermissionsUpdate update = childCondAdmin.createConditionalPermissionsUpdate();
		List infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();

		PermissionInfo packagePerm = new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.IMPORT); //$NON-NLS-1$
		PermissionInfo servicePerm = new PermissionInfo(ServicePermission.class.getName(), "*", ServicePermission.GET + ',' + ServicePermission.REGISTER); //$NON-NLS-1$
		PermissionInfo allPerm = new PermissionInfo(AllPermission.class.getName(), "*", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		PermissionInfo[] permissions = new PermissionInfo[] {packagePerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
			assertEquals("Unexpected exception message", "Missing Service Permission", e.getCause().getMessage()); //$NON-NLS-1$//$NON-NLS-2$
		}

		update = childCondAdmin.createConditionalPermissionsUpdate();
		infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();
		permissions = new PermissionInfo[] {packagePerm, servicePerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
			assertEquals("Unexpected exception message", "Missing AllPermissions", e.getCause().getMessage()); //$NON-NLS-1$//$NON-NLS-2$
		}

		update = childCondAdmin.createConditionalPermissionsUpdate();
		infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();
		permissions = new PermissionInfo[] {allPerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
		} catch (BundleException e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}

		uninstallCompositeBundle(composite);
	}

	public void testLinkSecurity03() {
		Map linkManifest = new HashMap();
		linkManifest.put(Constants.BUNDLE_SYMBOLICNAME, "testLinkSecurity03"); //$NON-NLS-1$
		linkManifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		linkManifest.put(Constants.IMPORT_PACKAGE, "org.eclipse.osgi.tests.bundles"); //$NON-NLS-1$
		linkManifest.put(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_IMPORT, "(objectClass=org.eclipse.osgi.tests.bundles.BundleInstaller)"); //$NON-NLS-1$
		CompositeBundle composite = createCompositeBundle(linkBundleFactory, "testLinkSecurity03", null, linkManifest, true, true); //$NON-NLS-1$

		Bundle conditionBundle = installIntoChild(composite.getCompositeFramework(), "test.link.postponed"); //$NON-NLS-1$
		assertTrue("Condition bundle not resolved", getPackageAdmin(composite.getCompositeFramework()).resolveBundles(new Bundle[] {conditionBundle}));
		Bundle linkD = installIntoChild(composite.getCompositeFramework(), "test.link.d"); //$NON-NLS-1$

		ConditionInfo[] conditions = new ConditionInfo[] {new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {linkD.getLocation()})};
		ConditionalPermissionAdmin childCondAdmin = getCondPermAdmin(composite.getCompositeFramework().getBundleContext());
		childCondAdmin.addConditionalPermissionInfo(conditions, new PermissionInfo[] {});
		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.RESOLVE_ERROR, e.getType()); //$NON-NLS-1$
		}

		ConditionalPermissionsUpdate update = childCondAdmin.createConditionalPermissionsUpdate();
		List infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();

		PermissionInfo packagePerm = new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.IMPORT); //$NON-NLS-1$
		PermissionInfo servicePerm = new PermissionInfo(ServicePermission.class.getName(), "*", ServicePermission.GET + ',' + ServicePermission.REGISTER); //$NON-NLS-1$
		PermissionInfo allPerm = new PermissionInfo(AllPermission.class.getName(), "*", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		PermissionInfo[] permissions = new PermissionInfo[] {packagePerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
			assertEquals("Unexpected exception message", "Missing Service Permission", e.getCause().getMessage()); //$NON-NLS-1$//$NON-NLS-2$
		}

		update = childCondAdmin.createConditionalPermissionsUpdate();
		infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();
		permissions = new PermissionInfo[] {packagePerm, servicePerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
			fail("Should fail to start test.link.d"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Wrong exception type", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
			assertEquals("Unexpected exception message", "Missing AllPermissions", e.getCause().getMessage()); //$NON-NLS-1$//$NON-NLS-2$
		}

		update = childCondAdmin.createConditionalPermissionsUpdate();
		infos = update.getConditionalPermissionInfoBases();
		assertEquals("Wrong number of infos", 1, infos.size()); //$NON-NLS-1$
		infos.clear();
		permissions = new PermissionInfo[] {allPerm};
		infos.add(childCondAdmin.createConditionalPermissionInfoBase("test.link.d", conditions, permissions, ConditionalPermissionInfoBase.ALLOW)); //$NON-NLS-1$
		update.commit();

		try {
			linkD.start();
		} catch (BundleException e) {
			fail("Unexpected exception", e);
		}

		uninstallCompositeBundle(composite);
	}

	private ConditionalPermissionAdmin getCondPermAdmin(BundleContext context) {
		ServiceReference permAdminRef = context.getServiceReference(ConditionalPermissionAdmin.class.getName());
		if (permAdminRef == null) {
			fail("No Conditional Permission Admin service"); //$NON-NLS-1$
			return null;
		}
		try {
			ConditionalPermissionAdmin permAdmin = (ConditionalPermissionAdmin) context.getService(permAdminRef);
			if (permAdmin == null)
				fail("No Conditional Permission Admin service"); //$NON-NLS-1$
			return permAdmin;
		} finally {
			context.ungetService(permAdminRef);
		}
	}
}
