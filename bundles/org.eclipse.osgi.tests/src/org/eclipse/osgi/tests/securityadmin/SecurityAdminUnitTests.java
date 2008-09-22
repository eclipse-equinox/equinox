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

import ext.framework.b.TestCondition;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.security.*;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityAdminUnitTests extends AbstractBundleTests {

	private static final PermissionInfo[] SOCKET_INFOS = new PermissionInfo[] {new PermissionInfo("java.net.SocketPermission", "localhost", "accept")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final PermissionInfo[] READONLY_INFOS = new PermissionInfo[] {new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "read")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final PermissionInfo[] READWRITE_INFOS = new PermissionInfo[] {
	// multiple permission infos
			new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "read"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "write") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	};

	private static final PermissionInfo[] RUNTIME_INFOS = new PermissionInfo[] {new PermissionInfo("java.lang.RuntimePermission", "exitVM", null)}; //$NON-NLS-1$ //$NON-NLS-2$

	private static final ConditionInfo[] ALLLOCATION_CONDS = new ConditionInfo[] {new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", new String[] {"*"})}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final ConditionInfo POST_MUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_SAT", "true", "true", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	private static final ConditionInfo POST_MUT_UNSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_UNSAT", "true", "true", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private static final ConditionInfo SIGNER_CONDITION1 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test1,c=US"}); //$NON-NLS-1$//$NON-NLS-2$
	private static final ConditionInfo SIGNER_CONDITION2 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test2,c=US"}); //$NON-NLS-1$//$NON-NLS-2$
	private static final ConditionInfo NOT_SIGNER_CONDITION1 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test1,c=US", "!"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

	//private static final ConditionInfo POST_MUT_NOTSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_NOTSAT", "true", "true", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	//private static final ConditionInfo POST_NOTMUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_NOTMUT_SAT", "true", "false", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	//private static final ConditionInfo POST_NOTMUT_NOTSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_NOTMUT_NOTSAT", "true", "false", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	public static Test suite() {
		return new TestSuite(SecurityAdminUnitTests.class);
	}

	private SecurityAdmin createSecurityAdmin(EquinoxSecurityManager sm) {
		try {
			return new SecurityAdmin(sm, null, new TestPermissionStorage());
		} catch (IOException e) {
			fail("unexpected exception creating SecuirtyAdmin", e); //$NON-NLS-1$;
		}
		return null;
	}

	public void testCreateSecurityAdmin() {
		createSecurityAdmin(null);
	}

	public void testCreateDomain() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		if (!pd.implies(new AllPermission()))
			fail("test bundle should have AllPermission"); //$NON-NLS-1$
	}

	public void testLocationPermission01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		securityAdmin.setPermissions(test.getLocation(), READONLY_INFOS);

		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		securityAdmin.setPermissions(test.getLocation(), null);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testLocationPermission02() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		securityAdmin.setPermissions(test.getLocation(), READWRITE_INFOS);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		securityAdmin.setPermissions(test.getLocation(), null);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testLocationPermission03() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);

		securityAdmin.setDefaultPermissions(READONLY_INFOS);
		securityAdmin.setPermissions(test.getLocation(), READWRITE_INFOS);
		ConditionalPermissionInfo condPermInfo = securityAdmin.addConditionalPermissionInfo(ALLLOCATION_CONDS, SOCKET_INFOS);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		securityAdmin.setPermissions(test.getLocation(), null);

		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		securityAdmin.setDefaultPermissions(null);
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

	}

	public void testDefaultPermissions01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		securityAdmin.setDefaultPermissions(READONLY_INFOS);
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		securityAdmin.setDefaultPermissions(null);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testDefaultPermissions02() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		securityAdmin.setDefaultPermissions(READONLY_INFOS);

		securityAdmin.setPermissions(test.getLocation(), SOCKET_INFOS);

		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		securityAdmin.setPermissions(test.getLocation(), null);

		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		securityAdmin.setDefaultPermissions(null);

		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testNotLocationCondition01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);

		ConditionalPermissionInfo condPermInfo = securityAdmin.addConditionalPermissionInfo(getLocationConditions("xxx", true), SOCKET_INFOS); //$NON-NLS-1$
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(pd, new AllPermission(), true);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testNotLocationCondition02() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);

		ConditionalPermissionInfo condPermInfo = securityAdmin.addConditionalPermissionInfo(getLocationConditions(test.getLocation(), true), SOCKET_INFOS);
		testPermission(pd, new AllPermission(), false);
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(pd, new AllPermission(), true);
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleLocationConditions01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);

		ConditionalPermissionInfo condPermInfo1 = securityAdmin.addConditionalPermissionInfo(getLocationConditions("xxx", false), SOCKET_INFOS); //$NON-NLS-1$
		ConditionalPermissionInfo condPermInfo2 = securityAdmin.addConditionalPermissionInfo(ALLLOCATION_CONDS, READONLY_INFOS);

		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo1.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo2.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleLocationConditions02() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);

		ConditionalPermissionInfo condPermInfo1 = securityAdmin.addConditionalPermissionInfo(getLocationConditions("xxx", false), SOCKET_INFOS); //$NON-NLS-1$
		ConditionalPermissionInfo condPermInfo2 = securityAdmin.addConditionalPermissionInfo(ALLLOCATION_CONDS, READONLY_INFOS);
		ConditionalPermissionInfo condPermInfo3 = securityAdmin.addConditionalPermissionInfo(getLocationConditions(test.getLocation(), false), RUNTIME_INFOS);

		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo1.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo2.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo3.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testUpdate01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		assertTrue("table is not empty", rows.isEmpty()); //$NON-NLS-1$
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
	}

	public void testUpdate02() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		ConditionalPermissionInfoBase info = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW);
		rows.add(info);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.clear();
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testUpdate03() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		ConditionalPermissionInfoBase info1 = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READWRITE_INFOS, ConditionalPermissionInfoBase.DENY);
		ConditionalPermissionInfoBase info2 = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW);
		rows.add(info1);
		rows.add(info2);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testUpdate04() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		ConditionalPermissionInfoBase info1 = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READWRITE_INFOS, ConditionalPermissionInfoBase.DENY);
		ConditionalPermissionInfoBase info2 = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW);
		rows.add(info1);
		rows.add(info2);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		ConditionalPermissionsUpdate update1 = securityAdmin.createConditionalPermissionsUpdate();
		List rows1 = update1.getConditionalPermissionInfoBases();
		rows1.remove(0);

		ConditionalPermissionsUpdate update2 = securityAdmin.createConditionalPermissionsUpdate();
		List rows2 = update2.getConditionalPermissionInfoBases();
		rows2.remove(0);
		assertTrue("failed to commit", update2.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), false);

		assertFalse("succeeded commit", update1.commit()); //$NON-NLS-1$

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.remove(0);

		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new AllPermission(), true);
	}

	public void testSecurityManager01() {
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		ConditionalPermissionInfoBase info = securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW);
		rows.add(info);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		Bundle test = installTestBundle();
		ProtectionDomain pd = securityAdmin.createProtectionDomain((AbstractBundle) test);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd};
		testSMPermission(sm, pds, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(sm, pds, new AllPermission(), false);

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.clear();
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testSMPermission(sm, pds, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(sm, pds, new AllPermission(), true);
	}

	public void testPostponedConditions01() {
		installConditionBundle();
		TestCondition.clearConditions();
		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		SecurityAdmin securityAdmin = createSecurityAdmin(sm);
		Bundle test1 = installTestBundle();
		Bundle test2 = installTest2Bundle();
		ProtectionDomain pd1 = securityAdmin.createProtectionDomain((AbstractBundle) test1);
		ProtectionDomain pd2 = securityAdmin.createProtectionDomain((AbstractBundle) test2);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		tc2unsat.setSatisfied(true);
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		tc1sat.setSatisfied(true);
		tc2sat.setSatisfied(true);
		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		tc1unsat.setSatisfied(false);
		tc2unsat.setSatisfied(false);
		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);
		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions02() {
		installConditionBundle();
		TestCondition.clearConditions();
		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		SecurityAdmin securityAdmin = createSecurityAdmin(sm);
		Bundle test1 = installTestBundle();
		Bundle test2 = installTest2Bundle();
		ProtectionDomain pd1 = securityAdmin.createProtectionDomain((AbstractBundle) test1);
		ProtectionDomain pd2 = securityAdmin.createProtectionDomain((AbstractBundle) test2);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions03() {
		installConditionBundle();
		TestCondition.clearConditions();
		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		SecurityAdmin securityAdmin = createSecurityAdmin(sm);
		Bundle test1 = installTestBundle();
		Bundle test2 = installTest2Bundle();
		ProtectionDomain pd1 = securityAdmin.createProtectionDomain((AbstractBundle) test1);
		ProtectionDomain pd2 = securityAdmin.createProtectionDomain((AbstractBundle) test2);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		tc2unsat.setSatisfied(true);
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions04() {
		installConditionBundle();
		TestCondition.clearConditions();
		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		SecurityAdmin securityAdmin = createSecurityAdmin(sm);
		Bundle test1 = installTestBundle();
		Bundle test2 = installTest2Bundle();
		ProtectionDomain pd1 = securityAdmin.createProtectionDomain((AbstractBundle) test1);
		ProtectionDomain pd2 = securityAdmin.createProtectionDomain((AbstractBundle) test2);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions05() {
		installConditionBundle();
		TestCondition.clearConditions();
		EquinoxSecurityManager sm = new EquinoxSecurityManager();
		SecurityAdmin securityAdmin = createSecurityAdmin(sm);
		Bundle test1 = installTestBundle();
		Bundle test2 = installTest2Bundle();
		ProtectionDomain pd1 = securityAdmin.createProtectionDomain((AbstractBundle) test1);
		ProtectionDomain pd2 = securityAdmin.createProtectionDomain((AbstractBundle) test2);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(sm, pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		testSMPermission(sm, pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testAccessControlContext01() {
		// test single row with signer condition
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext02() {
		// test with DENY row
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfoBase.DENY));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext03() {
		// test multiple signer conditions
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		update = securityAdmin.createConditionalPermissionsUpdate();
		rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION2}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext04() {
		// test multiple signer conditions
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1, SIGNER_CONDITION2}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test1,c=US", "cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext05() {
		// test with empty rows
		SecurityAdmin securityAdmin = createSecurityAdmin(null);

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new AllPermission());
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		// set the default permissions
		securityAdmin.setDefaultPermissions(READWRITE_INFOS);
		acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new AllPermission());
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
	}

	public void testAccessControlContext06() {
		// test with empty condition rows
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
	}

	public void testAccessControlContext07() {
		// test ! signer condition
		SecurityAdmin securityAdmin = createSecurityAdmin(null);
		ConditionalPermissionsUpdate update = securityAdmin.createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		rows.add(securityAdmin.createConditionalPermissionInfoBase(null, new ConditionInfo[] {NOT_SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfoBase.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		AccessControlContext acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		acc = securityAdmin.getAccessControlContext(new String[] {"cn=t1,cn=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	private void testSMPermission(EquinoxSecurityManager sm, ProtectionDomain[] pds, Permission permission, boolean expectedToPass) {
		AccessControlContext acc = new AccessControlContext(pds);
		try {
			sm.checkPermission(permission, acc);
			if (!expectedToPass)
				fail("test should not have the permission " + permission); //$NON-NLS-1$
		} catch (SecurityException e) {
			if (expectedToPass)
				fail("test should have the permission " + permission); //$NON-NLS-1$
		}
	}

	private void testPermission(ProtectionDomain pd, Permission permission, boolean expectedToPass) {
		if (expectedToPass ^ pd.implies(permission))
			fail("test should" + (expectedToPass ? "" : " not") + " have the permission " + permission); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private ConditionInfo[] getLocationConditions(String location, boolean not) {
		String[] args = not ? new String[] {location, "!"} : new String[] {location}; //$NON-NLS-1$
		return new ConditionInfo[] {new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", args)}; //$NON-NLS-1$
	}

	private Bundle installTestBundle() {
		try {
			return installer.installBundle("test"); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("failed to install bundle", e); //$NON-NLS-1$
		}
		return null;
	}

	private Bundle installTest2Bundle() {
		try {
			return installer.installBundle("test2"); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("failed to install bundle", e); //$NON-NLS-1$
		}
		return null;
	}

	private Bundle installConditionBundle() {
		try {
			Bundle bundle = installer.installBundle("ext.framework.b", false); //$NON-NLS-1$
			installer.resolveBundles(new Bundle[] {bundle});
			return bundle;
		} catch (BundleException e) {
			fail("failed to install bundle", e); //$NON-NLS-1$
		}
		return null;
	}
}
