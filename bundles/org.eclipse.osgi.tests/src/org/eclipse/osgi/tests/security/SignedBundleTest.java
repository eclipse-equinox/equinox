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

import static org.eclipse.osgi.tests.OSGiTestsActivator.addRequiredOSGiTestsBundles;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.BUNDLE_SECURITY_TESTS;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.copyEntryFile;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.getEntryFile;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.getSignedContentFactory;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.getTestCertificate;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.getTestJarPath;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.getTrustEngine;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.installBundle;
import static org.eclipse.osgi.tests.security.SecurityTestUtil.registerEclipseTrustEngine;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.osgi.signedcontent.InvalidContentException;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentEntry;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

public class SignedBundleTest {

	/*
	 * private static Test[] s_tests = { // positive tests new
	 * SignedBundleTest("testSignedContent01", "unsigned", new String[] {}) { public
	 * void runTest() { testSignedContent01(); } }, new
	 * SignedBundleTest("SignedContent positive test: signed jar, 1 trusted signer",
	 * "signed", new String[] {"ca1_leafa"}) { public void runTest() {
	 * testSignedContent02(); } } , new
	 * SignedBundleTest("SignedContent positive test: signed jar, 2 trusted signers"
	 * , "multiply_signed", new String[] {"ca1_leafa", "ca1_leafb"}) {(non-Javadoc)
	 *
	 * @see junit.framework.TestCase#runTest() public void runTest() {
	 * testSignedContent03(); } }};
	 */

	/*
	 * //positive tests signer1 signer2 valid n/a n/a n/a = positive, unsigned
	 * ('unsigned.jar') yes n/a yes = positive, 1 signer ('signed.jar','ca1_leafa')
	 * yes yes yes = positive, 2 signers
	 * ('multiply_signed.jar','ca1_leafa,'ca1_leafb')
	 *
	 * //negative = untrusted tests no n/a yes = negative, 1 signer, 1 untrusted
	 * ('signed.jar') no no yes = negative, 2 signers, 2 untrusted
	 * ('multiply_signed.jar') yes no yes = negative, 2 signers, 1 untrusted
	 * ('multiply_signed.jar', 'ca1_leafa')
	 *
	 * //negative = validity tests yes n/a no = negative, 1 signer, 1 corrupt
	 * ('signed_with_corrupt.jar','ca1_leafa') yes yes no = negative, 2 signers, 2
	 * corrupt
	 *
	 * //TODO: OSGi-specific partial signer cases //TODO: TSA tests (w/TSA signer
	 * trusted, untrusted, etc) //TODO: More? NESTED JARS?
	 */

	// private String jarName;
	// private String[] aliases;

	@RegisterExtension
	final SessionTestExtension extension = SessionTestExtension.forPlugin(BUNDLE_SECURITY_TESTS)
			.withCustomization(createCustomConfiguration()).create();

	private static CustomSessionConfiguration createCustomConfiguration() {
		CustomSessionConfiguration configuration = SessionTestExtension.createCustomConfiguration();
		addRequiredOSGiTestsBundles(configuration);
		return configuration;
	}

	private ServiceRegistration trustReg;

	@BeforeEach
	protected void setUp() throws Exception {
		trustReg = registerEclipseTrustEngine();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		if (trustReg != null) {
			trustReg.unregister();
		}
	}

	// SignedContent positive test: unsigned jar
	@Test
	public void testSignedContent01() throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("unsigned"));
			assertNotNull(testBundle, "Test bundle not installed!");
			// getTrustEngine().addTrustAnchor(anchor, alias);

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			assertFalse(signedContent.isSigned(), "Content is signed!!");
		} finally {
			testBundle.uninstall();
		}
	}

	// SignedContent positive test: signed jar, 1 trusted signer
	@Test
	public void testSignedContent02() throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");
			// check the signer validity
			signedContent.checkValidity(infos[0]);
			// check the signer trust
			assertTrue(infos[0].isTrusted(), "Signer is not trusted");
			// check the trust anchor
			assertNotNull(infos[0].getTrustAnchor(), "Trust anchor is null");
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			for (SignedContentEntry entry : entries) {
				entry.verify();
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(1, entryInfos.length, "wrong number of entry signers");
				assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
			}
		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca1_leafa");
		}
	}

	// SignedContent positive test: signed jar, 2 trusted signers
	@Test
	public void testSignedContent03() throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
			getTrustEngine().addTrustAnchor(getTestCertificate("ca2_leafa"), "ca2_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(2, infos.length, "wrong number of signers");
			// check the signer validity
			for (SignerInfo info : infos) {
				signedContent.checkValidity(info);
				signedContent.checkValidity(info);
				// check the signer trust
				assertTrue(info.isTrusted(), "Signer is not trusted: " + info.getCertificateChain()[0]);
				// check the trust anchor
				assertNotNull(info.getTrustAnchor(), "Trust anchor is null");
			}
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			for (SignedContentEntry entry : entries) {
				entry.verify();
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(2, entryInfos.length, "wrong number of entry signers");
				assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
			}
		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca1_leafa");
			getTrustEngine().removeTrustAnchor("ca2_leafa");
		}
	}

	// SignedContent negative, 1 signer, 1 untrusted
	@Test
	public void testSignedContent04() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");
			// check the signer validity
			for (SignerInfo info : infos) {
				// check the signer trust
				assertTrue(!(info.isTrusted()), "Signer is trusted: " + info.getCertificateChain()[0]);
			}
		} finally {
			testBundle.uninstall();
		}
	}

	// SignedContent negative, 2 signers, 2 untrusted
	@Test
	public void testSignedContent05() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(2, infos.length, "wrong number of signers");
			// check the signer validity
			for (SignerInfo info : infos) {
				// check the signer trust
				assertTrue(!(info.isTrusted()), "Signer is trusted: " + info.getCertificateChain()[0]);
			}
		} finally {
			testBundle.uninstall();
		}
	}

	// SignedContent negative, 2 signers, 1 untrusted
	@Test
	public void testSignedContent06() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(2, infos.length, "wrong number of signers");

			// make sure ca1 signer is trusted
			// check the signer validity
			for (SignerInfo info : infos) {
				Certificate[] certs = info.getCertificateChain();
				if (info.isTrusted()) {
					X509Certificate x509Cert = (X509Certificate) certs[0];
					assertTrue(x509Cert.getSubjectX500Principal().getName().indexOf("CA1 LeafA") >= 0,
							"CA1 LeafA signer is not trusted");
				}
			}
		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca1_leafa");
		}
	}

	// negative, 1 signer, 1 corrupt signed_with_corrupt.jar
	@Test
	public void testSignedContent07() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_with_corrupt"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");

			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			for (SignedContentEntry entry : entries) {
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				try {
					entry.verify();
					if ("org/eclipse/equinox/security/junit/CorruptClass.class".equals(entry.getName())) {
						fail("Expected a corruption for: " + entry.getName());
					}
					assertEquals(1, entryInfos.length, "wrong number of entry signers");
					assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
				} catch (InvalidContentException e) {
					assertEquals("org/eclipse/equinox/security/junit/CorruptClass.class", entry.getName(),
							"Unexpected corruption in '" + entry.getName() + "': " + e);
					// no signers if entry is corrupt
					assertEquals(0, entryInfos.length, "wrong number of entry signers");
				}

			}

		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca1_leafa");
		}
	}

	@Test
	public void testSignedContent07a() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_with_corrupt"));

			// Loading a corrupt class will cause a LinkageError
			testBundle.loadClass("org.eclipse.equinox.security.junit.CorruptClass");

		} catch (LinkageError error) {
			// will happen if not running with runtime checks
			if ("all".equals(System.getProperty("osgi.signedcontent.support"))) {
				// if signed content support is enabled then the cause is an
				// InvalidContentException
				Throwable t = error.getCause();
				assertTrue(t instanceof InvalidContentException, "Cause is the wrong type: " + t);
			}
		} finally {
			testBundle.uninstall();
		}
	}

	// positve 1 signer, 1 tsa
	@Test
	public void testSignedContent08() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_tsa"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");

			assertNotNull(signedContent.getSigningTime(infos[0]), "Signing time is null!");
		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca1_leafa");
		}
	}

	// SignedContent positive test: unsigned jar
	@Test
	public void testSignedContent09() throws Exception {
		File unsignedFile = getEntryFile(getTestJarPath("unsigned"));

		assertNotNull(unsignedFile, "Could not find unsigned file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(unsignedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertFalse(signedContent.isSigned(), "Content is signed!!");
	}

	// SignedContent positive test: signed jar, 1 trusted signer
	@Test
	public void testSignedContent10() throws Exception {
		File signedFile = getEntryFile(getTestJarPath("signed"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(1, infos.length, "wrong number of signers");
		// check the signer validity
		signedContent.checkValidity(infos[0]);
		// check the signer trust
		assertTrue(infos[0].isTrusted(), "Signer is not trusted");
		// check the trust anchor
		assertNotNull(infos[0].getTrustAnchor(), "Trust anchor is null");
		// verify and validate the entries
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(1, entryInfos.length, "wrong number of entry signers");
			assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
		}
	}

	// SignedContent positive test: signed jar, 2 trusted signers
	@Test
	public void testSignedContent11() throws Exception {
		File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
		getTrustEngine().addTrustAnchor(getTestCertificate("ca2_leafa"), "ca2_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(2, infos.length, "wrong number of signers");
		// check the signer validity
		for (SignerInfo info : infos) {
			signedContent.checkValidity(info);
			signedContent.checkValidity(info);
			// check the signer trust
			assertTrue(info.isTrusted(), "Signer is not trusted: " + info.getCertificateChain()[0]);
			// check the trust anchor
			assertNotNull(info.getTrustAnchor(), "Trust anchor is null");
		}
		// verify and validate the entries
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(2, entryInfos.length, "wrong number of entry signers");
			assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
		}
	}

	// SignedContent negative, 1 signer, 1 untrusted
	@Test
	public void testSignedContent12() throws Exception {
		File signedFile = getEntryFile(getTestJarPath("signed"));
		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(1, infos.length, "wrong number of signers");
		// check the signer validity
		for (SignerInfo info : infos) {
			// check the signer trust
			assertTrue(!(info.isTrusted()), "Signer is trusted: " + info.getCertificateChain()[0]);
		}
	}

	// SignedContent negative, 2 signers, 2 untrusted
	@Test
	public void testSignedContent13() throws Exception {
		File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(2, infos.length, "wrong number of signers");
		// check the signer validity
		for (SignerInfo info : infos) {
			// check the signer trust
			assertTrue(!(info.isTrusted()), "Signer is trusted: " + info.getCertificateChain()[0]);
		}
	}

	// SignedContent negative, 2 signers, 1 untrusted
	@Test
	public void testSignedContent14() throws Exception {
		File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(2, infos.length, "wrong number of signers");

		// make sure ca1 signer is trusted
		// check the signer validity
		for (SignerInfo info : infos) {
			Certificate[] certs = info.getCertificateChain();
			if (info.isTrusted()) {
				X509Certificate x509Cert = (X509Certificate) certs[0];
				assertTrue(x509Cert.getSubjectX500Principal().getName().indexOf("CA1 LeafA") >= 0,
						"CA1 LeafA signer is not trusted");
			}
		}
	}

	// negative, 1 signer, 1 corrupt signed_with_corrupt.jar
	@Test
	public void testSignedContent15() throws Exception {
		File corruptedFile = getEntryFile(getTestJarPath("signed_with_corrupt"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(corruptedFile);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(1, infos.length, "wrong number of signers");

		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		for (SignedContentEntry entry : entries) {
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			try {
				entry.verify();
				if ("org/eclipse/equinox/security/junit/CorruptClass.class".equals(entry.getName())) {
					fail("Expected a corruption for: " + entry.getName());
				}
				assertEquals(1, entryInfos.length, "wrong number of entry signers");
				assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
			} catch (InvalidContentException e) {
				assertEquals("org/eclipse/equinox/security/junit/CorruptClass.class", entry.getName(),
						"Unexpected corruption in '" + entry.getName() + "': " + e);
				assertEquals(0, entryInfos.length, "wrong number of entry signers");
			}
		}
	}

	// positve 1 signer, 1 tsa
	@Test
	public void testSignedContent16() throws Exception {
		File signedTsaFile = getEntryFile(getTestJarPath("signed_tsa"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedTsaFile);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(1, infos.length, "wrong number of signers");

		assertNotNull(signedContent.getSigningTime(infos[0]), "Signing time is null!");
	}

	// SignedContent positive test: signed jar, 1 trusted signer
	@Test
	public void testBug225090_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		// check if it is signed
		assertTrue(signedContent.isSigned(), "Should be signed");
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull(infos, "SignerInfo is null");
		assertEquals(1, infos.length, "wrong number of signers");
		// check the signer validity
		signedContent.checkValidity(infos[0]);
		// check the signer trust
		assertTrue(infos[0].isTrusted(), "Signer is not trusted");
		// check the trust anchor
		assertNotNull(infos[0].getTrustAnchor(), "Trust anchor is null");
		// verify and validate the entries
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(1, entryInfos.length, "wrong number of entry signers");
			assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
		}
		signedFile.delete();
		assertFalse(signedFile.exists(), "File should not exist");
	}

	// SignedContent positive test: unsigned jar
	@Test
	public void testBug225090_02() throws Exception {
		File unsignedFile = copyEntryFile(getTestJarPath("unsigned"));

		assertNotNull(unsignedFile, "Could not find unsigned file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(unsignedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertFalse(signedContent.isSigned(), "Content is signed!!");
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(0, entryInfos.length, "wrong number of entry signers");
		}
		unsignedFile.delete();
		assertFalse(unsignedFile.exists(), "File should not exist");
	}

	@Test
	public void testBug228427_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata"));

		assertNotNull(signedFile, "Could not find signed file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertTrue(signedContent.isSigned(), "Content is not signed!!");
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		assertEquals(5, entries.length, "Incorrect number of signed entries");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(1, entryInfos.length, "wrong number of entry signers");
		}
		signedFile.delete();
		assertFalse(signedFile.exists(), "File should not exist");
	}

	@Test
	public void testBug228427_02() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_added"));

		assertNotNull(signedFile, "Could not find signed file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertTrue(signedContent.isSigned(), "Content is not signed!!");
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		assertEquals(4, entries.length, "Incorrect number of signed entries");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(1, entryInfos.length, "wrong number of entry signers");
		}
		signedFile.delete();
		assertFalse(signedFile.exists(), "File should not exist");
	}

	@Test
	public void testBug228427_03() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_corrupt"));

		assertNotNull(signedFile, "Could not find signed file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertTrue(signedContent.isSigned(), "Content is not signed!!");
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		assertEquals(5, entries.length, "Incorrect number of signed entries");
		for (SignedContentEntry entry : entries) {
			try {
				entry.verify();
				assertFalse("META-INF/test/test1.file".equals(entry.getName()),
						"Wrong entry is validated: " + entry.getName());
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(1, entryInfos.length, "wrong number of entry signers");
			} catch (InvalidContentException e) {
				assertEquals("META-INF/test/test1.file", entry.getName(), "Wrong entry is corrupted");
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(0, entryInfos.length, "wrong number of entry signers");
			}

		}
		signedFile.delete();
		assertFalse(signedFile.exists(), "File should not exist");
	}

	@Test
	public void testBug228427_04() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_removed"));

		assertNotNull(signedFile, "Could not find signed file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertTrue(signedContent.isSigned(), "Content is not signed!!");
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull(entries, "Entries is null");
		assertEquals(4, entries.length, "Incorrect number of signed entries");
		for (SignedContentEntry entry : entries) {
			entry.verify();
			SignerInfo[] entryInfos = entry.getSignerInfos();
			assertNotNull(entryInfos, "SignerInfo is null");
			assertEquals(1, entryInfos.length, "wrong number of entry signers");
		}
		signedFile.delete();
		assertFalse(signedFile.exists(), "File should not exist");
	}

	@Test
	public void testBug236329_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_sf_corrupted"));

		assertNotNull(signedFile, "Could not find signed file!");
		// getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		try {
			getSignedContentFactory().getSignedContent(signedFile);
			fail("Should have gotten a SignatureException for file: " + signedFile);
		} catch (IOException e) {
			// expected
		}
	}

	@Test
	public void testBug252098() throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("test.bug252098"));
			assertNotNull(testBundle, "Test bundle not installed!");
			testBundle.start();
		} finally {
			if (testBundle != null) {
				testBundle.uninstall();
			}
		}
	}

	@Test
	public void testBug378155() throws Exception {
		doTestBug378155("SHA256withRSA");
		doTestBug378155("SHA384withRSA");
		doTestBug378155("SHA512withRSA");
	}

	private void doTestBug378155(String bundleName) throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath(bundleName));
			assertNotNull(testBundle, "Test bundle not installed!");
			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");
			// check the signer validity
			signedContent.checkValidity(infos[0]);
			// check the signer trust (it is NOT trusted)
			assertFalse(infos[0].isTrusted(), "Signer is trusted");
			// check the trust anchor
			assertNull("Trust anchor is not null", infos[0].getTrustAnchor());
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			for (SignedContentEntry entry : entries) {
				entry.verify();
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(1, entryInfos.length, "wrong number of entry signers");
				assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
			}
		} finally {
			if (testBundle != null) {
				testBundle.uninstall();
			}
		}
	}

	@Test
	public void testBug434711() throws Exception {
		File nonAsciiFile = getEntryFile(getTestJarPath("bundleWithNonAsciiCharsFilename"));

		assertNotNull(nonAsciiFile, "Could not find Non Ascii Chars file!");
		SignedContent signedContent = getSignedContentFactory().getSignedContent(nonAsciiFile);
		assertNotNull(signedContent, "SignedContent is null");
		assertTrue(signedContent.isSigned(), "Content is not signed!!");
		for (SignedContentEntry entry : signedContent.getSignedEntries()) {
			entry.verify();
		}
	}

	@Test
	public void test489686() throws Exception {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_with_missing_digest"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// Notice that if all entries are missing that have corresponding digests then
			// the built in JarFile APIs treat the JAR is unsigned
			// check if it is signed
			assertFalse(signedContent.isSigned(), "Should not be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(0, infos.length, "wrong number of signers");

			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			assertEquals(0, entries.length, "Expected no signed entries.");
		} finally {
			testBundle.uninstall();
		}
	}

	@Test
	public void testSignedContentJava16() throws Exception {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signedJava16"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca2_leafa"), "ca2_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull(signedContent, "SignedContent is null");
			// check if it is signed
			assertTrue(signedContent.isSigned(), "Should be signed");
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull(infos, "SignerInfo is null");
			assertEquals(1, infos.length, "wrong number of signers");
			// check the signer validity
			signedContent.checkValidity(infos[0]);
			// check the signer trust
			assertTrue(infos[0].isTrusted(), "Signer is not trusted");
			// check the trust anchor
			assertNotNull(infos[0].getTrustAnchor(), "Trust anchor is null");
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull(entries, "Entries is null");
			for (SignedContentEntry entry : entries) {
				entry.verify();
				SignerInfo[] entryInfos = entry.getSignerInfos();
				assertNotNull(entryInfos, "SignerInfo is null");
				assertEquals(1, entryInfos.length, "wrong number of entry signers");
				assertEquals(infos[0], entryInfos[0], "Entry signer does not equal content signer");
			}
		} finally {
			testBundle.uninstall();
			getTrustEngine().removeTrustAnchor("ca2_leafa");
		}
	}
}
