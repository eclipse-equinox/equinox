/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.condpermadmin.BundleSignerCondition;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class OSGiAPICertificateTest extends BaseSecurityTest {
	private static String dn1 = "CN=CA1 LeafA, O=CA1, L=Boston, ST=Massachusetts, C=US"; //$NON-NLS-1$
	private static String dn2 = "CN=CA1 Root, O=CA1, L=Boston, ST=Massachusetts, C=US"; //$NON-NLS-1$
	private static String dn3 = "CN=CA1 LeafA, O=CA1, L=Austin, ST=Texas, C=US"; //$NON-NLS-1$
	private static String dn4 = "CN=CA1 Root, O=CA1, L=Austin, ST=Texas, C=US"; //$NON-NLS-1$
	private static String dn5 = "CN=CA1 LeafA, O=CA1, L=*, ST=*, C=US"; //$NON-NLS-1$
	private static String dn6 = "CN=CA1 Root, O=CA1, L=*, ST=*, C=US"; //$NON-NLS-1$
	private static String dn7 = "*, L=*, ST=*, C=US"; //$NON-NLS-1$

	private static String dnChain01True = dn1 + ';' + dn2;
	private static String dnChain02True = "*;" + dn2; //$NON-NLS-1$
	private static String dnChain03True = dn1 + ";*"; //$NON-NLS-1$
	private static String dnChain04False = dn1 + ';' + dn4;
	private static String dnChain05False = dn3 + ';' + dn2;
	private static String dnChain06True = dn5 + ';' + dn6;
	private static String dnChain07True = dn7 + ';' + dn6;
	private static String dnChain08True = dn5 + ';' + dn7;

	private static String dnChain01TrueEscaped = escapeStar(dnChain01True);
	private static String dnChain02TrueEscaped = escapeStar(dnChain02True);
	private static String dnChain03TrueEscaped = escapeStar(dnChain03True);
	private static String dnChain04FalseEscaped = escapeStar(dnChain04False);
	private static String dnChain05FalseEscaped = escapeStar(dnChain05False);
	private static String dnChain06TrueEscaped = escapeStar(dnChain06True);
	private static String dnChain07TrueEscaped = escapeStar(dnChain07True);
	private static String dnChain08TrueEscaped = escapeStar(dnChain08True);

	private static ConditionInfo info01True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { "-" }); //$NON-NLS-1$
	private static ConditionInfo info02False = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { "-", "!" }); //$NON-NLS-1$ //$NON-NLS-2$
	private static ConditionInfo info03True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain01True });
	private static ConditionInfo info04True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain02True });
	private static ConditionInfo info05True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain03True });
	private static ConditionInfo info06False = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain04False });
	private static ConditionInfo info07False = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain05False });
	private static ConditionInfo info08True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain06True });
	private static ConditionInfo info09True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain07True });
	private static ConditionInfo info10True = new ConditionInfo(BundleSignerCondition.class.getName(),
			new String[] { dnChain08True });

	private Collection<Bundle> installedBundles = new ArrayList<>();

	private static String escapeStar(String dnChain) {
		if (dnChain == null || dnChain.length() == 0)
			return dnChain;
		for (int star = dnChain.indexOf('*'); star >= 0; star = dnChain.indexOf('*', star + 2))
			dnChain = dnChain.substring(0, star) + '\\' + dnChain.substring(star);
		return dnChain;
	}

	public OSGiAPICertificateTest() {
		super();
	}

	public OSGiAPICertificateTest(String name, String jarname, String[] aliases) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		registerEclipseTrustEngine();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Bundle b : installedBundles) {
			try {
				b.uninstall();
			} catch (BundleException e) {
				// do nothing
			}
		}
	}

	@Override
	protected Bundle installBundle(String bundlePath) throws BundleException, IOException {
		Bundle b = super.installBundle(bundlePath);
		installedBundles.add(b);
		return b;
	}

	public void testBundleSignerCondition01() throws Exception {
		// test trusted cert with all signed match
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info01True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition02() throws Exception {
		// test trusted cert with all signed match + "!" not operation
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info02False);
		assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition03() throws Exception {
		// test untrusted cert with all signed match
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info01True);
		assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition04() throws Exception {
		// test untrusted cert with all signed match + "!" not operation
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info02False);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition05() throws Exception {
		// test trusted cert with exact match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info03True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition06() throws Exception {
		// test trusted cert with prefix wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info04True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition07() throws Exception {
		// test trusted cert with postfix wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info05True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition08() throws Exception {
		// test trusted cert with wrong prefix dn
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info06False);
		assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition09() throws Exception {
		// test trusted cert with wrong postfix dn
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info07False);
		assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition10() throws Exception {
		// test trusted cert with RDN wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info08True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition11() throws Exception {
		// test trusted cert with RDN wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info09True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testBundleSignerCondition12() throws Exception {
		// test trusted cert with RDN wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		Condition condition = BundleSignerCondition.getCondition(testBundle, info10True);
		assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
	}

	public void testAdminPermission01() throws Exception {
		// test trusted cert with exact match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=-)", AdminPermission.CONTEXT); //$NON-NLS-1$
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission02() throws Exception {
		// test trusted cert with exact match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain01TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission03() throws Exception {
		// test trusted cert with exact match pattern + ! operation
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(!(signer=-))", AdminPermission.CONTEXT); //$NON-NLS-1$
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission04() throws Exception {
		// test trusted cert with exact match pattern + ! operation
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(!(signer=" + dnChain01TrueEscaped + "))", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission05() throws Exception {
		// test trusted cert with prefix wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain02TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission06() throws Exception {
		// test trusted cert with postfix wildcard match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain03TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission07() throws Exception {
		// test trusted cert with bad postfix dn match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain04FalseEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission08() throws Exception {
		// test trusted cert with bad prefix dn match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain05FalseEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission09() throws Exception {
		// test trusted cert with RDN match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain06TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission10() throws Exception {
		// test trusted cert with RDN match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain07TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

	public void testAdminPermission11() throws Exception {
		// test trusted cert with RDN match pattern
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
		Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
		AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain08TrueEscaped + ")", //$NON-NLS-1$ //$NON-NLS-2$
				AdminPermission.CONTEXT);
		AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
		assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
	}

}
