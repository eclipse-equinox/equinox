/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.TestSuite;
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

	private static ConditionInfo info01True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {"-"}); //$NON-NLS-1$
	private static ConditionInfo info02False = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {"-", "!"}); //$NON-NLS-1$ //$NON-NLS-2$
	private static ConditionInfo info03True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain01True});
	private static ConditionInfo info04True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain02True});
	private static ConditionInfo info05True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain03True});
	private static ConditionInfo info06False = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain04False});
	private static ConditionInfo info07False = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain05False});
	private static ConditionInfo info08True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain06True});
	private static ConditionInfo info09True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain07True});
	private static ConditionInfo info10True = new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {dnChain08True});

	private Collection<Bundle> installedBundles = new ArrayList<>();

	public static TestSuite suite() {
		return new TestSuite(OSGiAPICertificateTest.class);
	}

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
	protected Bundle installBundle(String bundlePath) {
		Bundle b = super.installBundle(bundlePath);
		installedBundles.add(b);
		return b;
	}

	public void testBundleSignerCondition01() {
		// test trusted cert with all signed match
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info01True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition02() {
		// test trusted cert with all signed match + "!" not operation
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info02False);
			assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition03() {
		// test untrusted cert with all signed match
		try {
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info01True);
			assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition04() {
		// test untrusted cert with all signed match + "!" not operation
		try {
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info02False);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition05() {
		// test trusted cert with exact match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info03True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition06() {
		// test trusted cert with prefix wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info04True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition07() {
		// test trusted cert with postfix wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info05True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition08() {
		// test trusted cert with wrong prefix dn
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info06False);
			assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition09() {
		// test trusted cert with wrong postfix dn
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info07False);
			assertEquals("Unexpected condition value", Condition.FALSE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition10() {
		// test trusted cert with RDN wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info08True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition11() {
		// test trusted cert with RDN wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info09True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBundleSignerCondition12() {
		// test trusted cert with RDN wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			Condition condition = BundleSignerCondition.getCondition(testBundle, info10True);
			assertEquals("Unexpected condition value", Condition.TRUE, condition); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission01() {
		// test trusted cert with exact match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=-)", AdminPermission.CONTEXT); //$NON-NLS-1$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission02() {
		// test trusted cert with exact match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain01TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission03() {
		// test trusted cert with exact match pattern + ! operation
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(!(signer=-))", AdminPermission.CONTEXT); //$NON-NLS-1$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission04() {
		// test trusted cert with exact match pattern + ! operation
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(!(signer=" + dnChain01TrueEscaped + "))", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission05() {
		// test trusted cert with prefix wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain02TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission06() {
		// test trusted cert with postfix wildcard match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain03TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission07() {
		// test trusted cert with bad postfix dn match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain04FalseEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission08() {
		// test trusted cert with bad prefix dn match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain05FalseEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertFalse("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission09() {
		// test trusted cert with RDN match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain06TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission10() {
		// test trusted cert with RDN match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain07TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testAdminPermission11() {
		// test trusted cert with RDN match pattern
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa"); //$NON-NLS-1$ //$NON-NLS-2$
			Bundle testBundle = installBundle(getTestJarPath("signed")); //$NON-NLS-1$
			AdminPermission declaredPerm = new AdminPermission("(signer=" + dnChain08TrueEscaped + ")", AdminPermission.CONTEXT); //$NON-NLS-1$ //$NON-NLS-2$
			AdminPermission checkedPerm = new AdminPermission(testBundle, AdminPermission.CONTEXT);
			assertTrue("Security check failed", declaredPerm.implies(checkedPerm)); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

}
