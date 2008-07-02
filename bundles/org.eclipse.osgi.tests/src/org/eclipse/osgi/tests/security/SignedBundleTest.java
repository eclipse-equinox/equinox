/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.security;

import java.io.File;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.signedcontent.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class SignedBundleTest extends BaseSecurityTest {

	/*
	private static Test[] s_tests = {
	// positive tests 
	new SignedBundleTest("testSignedContent01", "unsigned", new String[] {}) {
		public void runTest() {
			testSignedContent01();
		}
	}, new SignedBundleTest("SignedContent positive test: signed jar, 1 trusted signer", "signed", new String[] {"ca1_leafa"}) {
		public void runTest() {
			testSignedContent02();
		}
	}
	, new SignedBundleTest("SignedContent positive test: signed jar, 2 trusted signers", "multiply_signed", new String[] {"ca1_leafa", "ca1_leafb"}) {(non-Javadoc)
	  @see junit.framework.TestCase#runTest()
	 	public void runTest() {
			testSignedContent03();
		}
	}};
	*/

	/*
	//positive tests
	signer1	signer2	valid
	n/a		n/a		n/a		= positive, unsigned				('unsigned.jar')
	yes		n/a		yes		= positive, 1 signer				('signed.jar','ca1_leafa')
	yes		yes		yes		= positive, 2 signers				('multiply_signed.jar','ca1_leafa,'ca1_leafb')

	//negative = untrusted tests
	no		n/a		yes		= negative, 1 signer, 1 untrusted	('signed.jar')		
	no		no		yes		= negative, 2 signers, 2 untrusted  ('multiply_signed.jar')
	yes		no		yes		= negative, 2 signers, 1 untrusted	('multiply_signed.jar', 'ca1_leafa')

	//negative = validity tests
	yes		n/a		no		= negative, 1 signer, 1 corrupt		('signed_with_corrupt.jar','ca1_leafa')
	yes		yes		no		= negative, 2 signers, 2 corrupt
	
	//TODO: OSGi-specific partial signer cases
	//TODO: TSA tests (w/TSA signer trusted, untrusted, etc)
	//TODO: More? NESTED JARS? 		
	*/

	//private String jarName;
	//private String[] aliases;
	public SignedBundleTest() {
		super();
	}

	public SignedBundleTest(String name, String jarname, String[] aliases) {
		super(name);
		//this.jarName = jarname;
		//this.aliases = aliases;
	}

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(BUNDLE_SECURITY_TESTS, "Unit session tests for SignedContent");
		addDefaultSecurityBundles(suite);
		setAuthorizationEnabled(suite);
		suite.addTestSuite(SignedBundleTest.class);
		return suite;
	}

	public static Test localSuite() {
		return new TestSuite(SignedBundleTest.class, "Unit local tests for SignedContent");
	}

	protected void setUp() throws Exception {
		registerEclipseTrustEngine();
		/*
				TrustEngine engine = getTrustEngine();

				if (supportStore == null) {
					fail("Could not open keystore with test certificates!");
				}

				// get the certs from the support store and add
				for (int i = 0; i < aliases.length; i++) {
					Certificate cert = supportStore.getCertificate(aliases[i]);
					engine.addTrustAnchor(cert, aliases[i]);
				}
		*/
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	//	SignedContent positive test: unsigned jar
	public void testSignedContent01() {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("unsigned"));
			assertNotNull("Test bundle not installed!", testBundle);
			//getTrustEngine().addTrustAnchor(anchor, alias);

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			assertFalse("Content is signed!!", signedContent.isSigned());
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//SignedContent positive test: signed jar, 1 trusted signer
	public void testSignedContent02() {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);
			// check the signer validity
			signedContent.checkValidity(infos[0]);
			// check the signer trust
			assertTrue("Signer is not trusted", infos[0].isTrusted());
			// check the trust anchor
			assertNotNull("Trust anchor is null", infos[0].getTrustAnchor());
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				entries[i].verify();
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 1, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
				getTrustEngine().removeTrustAnchor("ca1_leafa");
			} catch (Exception e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//SignedContent positive test: signed jar, 2 trusted signers
	public void testSignedContent03() {

		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));
			this.getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
			this.getTrustEngine().addTrustAnchor(getTestCertificate("ca2_leafa"), "ca2_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				signedContent.checkValidity(infos[i]);
				signedContent.checkValidity(infos[i]);
				// check the signer trust
				assertTrue("Signer is not trusted: " + infos[i].getCertificateChain()[0], infos[i].isTrusted());
				// check the trust anchor
				assertNotNull("Trust anchor is null", infos[i].getTrustAnchor());
			}
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				entries[i].verify();
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 2, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
				getTrustEngine().removeTrustAnchor("ca1_leafa");
				getTrustEngine().removeTrustAnchor("ca2_leafa");
			} catch (Exception e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//SignedContent negative, 1 signer, 1 untrusted
	public void testSignedContent04() {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				// check the signer trust
				assertTrue("Signer is trusted: " + infos[i].getCertificateChain()[0], !(infos[i].isTrusted()));
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//SignedContent negative, 2 signers, 2 untrusted
	public void testSignedContent05() {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				// check the signer trust
				assertTrue("Signer is trusted: " + infos[i].getCertificateChain()[0], !(infos[i].isTrusted()));
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//SignedContent negative, 2 signers, 1 untrusted
	public void testSignedContent06() {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("multiply_signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);

			// make sure ca1 signer is trusted

			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				Certificate certs[] = infos[i].getCertificateChain();

				if (infos[i].isTrusted()) {
					X509Certificate x509Cert = (X509Certificate) certs[0];
					assertTrue("CA1 LeafA signer is not trusted", x509Cert.getSubjectDN().getName().indexOf("CA1 LeafA") >= 0);
				}
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
				getTrustEngine().removeTrustAnchor("ca1_leafa");
			} catch (Exception e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	// negative, 1 signer, 1 corrupt signed_with_corrupt.jar
	public void testSignedContent07() {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_with_corrupt"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);

			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				try {
					entries[i].verify();
					if ("org/eclipse/equinox/security/junit/CorruptClass.class".equals(entries[i].getName()))
						fail("Expected a corruption for: " + entries[i].getName());
				} catch (InvalidContentException e) {
					if (!"org/eclipse/equinox/security/junit/CorruptClass.class".equals(entries[i].getName()))
						fail("Unexpected corruption in: " + entries[i].getName(), e);
				}
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 1, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}

		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
				getTrustEngine().removeTrustAnchor("ca1_leafa");
			} catch (Exception e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	// positve 1 signer, 1 tsa
	public void testSignedContent08() {
		Bundle testBundle = null;
		try {
			testBundle = installBundle(getTestJarPath("signed_tsa"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(testBundle);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);

			assertNotNull("Signing time is null!", signedContent.getSigningTime(infos[0]));

		} catch (Exception e) {
			fail("Unexpected exception", e);
		} finally {
			try {
				testBundle.uninstall();
				getTrustEngine().removeTrustAnchor("ca1_leafa");
			} catch (Exception e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//	SignedContent positive test: unsigned jar
	public void testSignedContent09() {
		try {
			File unsignedFile = getEntryFile(getTestJarPath("unsigned"));

			assertNotNull("Could not find unsigned file!", unsignedFile);
			//getTrustEngine().addTrustAnchor(anchor, alias);

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(unsignedFile);
			assertNotNull("SignedContent is null", signedContent);
			assertFalse("Content is signed!!", signedContent.isSigned());
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent positive test: signed jar, 1 trusted signer
	public void testSignedContent10() {
		try {
			File signedFile = getEntryFile(getTestJarPath("signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);
			// check the signer validity
			signedContent.checkValidity(infos[0]);
			// check the signer trust
			assertTrue("Signer is not trusted", infos[0].isTrusted());
			// check the trust anchor
			assertNotNull("Trust anchor is null", infos[0].getTrustAnchor());
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				entries[i].verify();
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 1, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent positive test: signed jar, 2 trusted signers
	public void testSignedContent11() {
		try {
			File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));
			this.getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
			this.getTrustEngine().addTrustAnchor(getTestCertificate("ca2_leafa"), "ca2_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				signedContent.checkValidity(infos[i]);
				signedContent.checkValidity(infos[i]);
				// check the signer trust
				assertTrue("Signer is not trusted: " + infos[i].getCertificateChain()[0], infos[i].isTrusted());
				// check the trust anchor
				assertNotNull("Trust anchor is null", infos[i].getTrustAnchor());
			}
			// verify and validate the entries
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				entries[i].verify();
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 2, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent negative, 1 signer, 1 untrusted
	public void testSignedContent12() {
		try {
			File signedFile = getEntryFile(getTestJarPath("signed"));
			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				// check the signer trust
				assertTrue("Signer is trusted: " + infos[i].getCertificateChain()[0], !(infos[i].isTrusted()));
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent negative, 2 signers, 2 untrusted
	public void testSignedContent13() {
		try {
			File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);
			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				// check the signer trust
				assertTrue("Signer is trusted: " + infos[i].getCertificateChain()[0], !(infos[i].isTrusted()));
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent negative, 2 signers, 1 untrusted
	public void testSignedContent14() {
		try {
			File multipleSigned = getEntryFile(getTestJarPath("multiply_signed"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(multipleSigned);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 2, infos.length);

			// make sure ca1 signer is trusted

			// check the signer validity
			for (int i = 0; i < infos.length; i++) {
				Certificate certs[] = infos[i].getCertificateChain();

				if (infos[i].isTrusted()) {
					X509Certificate x509Cert = (X509Certificate) certs[0];
					assertTrue("CA1 LeafA signer is not trusted", x509Cert.getSubjectDN().getName().indexOf("CA1 LeafA") >= 0);
				}
			}
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	// negative, 1 signer, 1 corrupt signed_with_corrupt.jar
	public void testSignedContent15() {
		try {
			File corruptedFile = getEntryFile(getTestJarPath("signed_with_corrupt"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(corruptedFile);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);

			SignedContentEntry[] entries = signedContent.getSignedEntries();
			assertNotNull("Entries is null", entries);
			for (int i = 0; i < entries.length; i++) {
				try {
					entries[i].verify();
					if ("org/eclipse/equinox/security/junit/CorruptClass.class".equals(entries[i].getName()))
						fail("Expected a corruption for: " + entries[i].getName());
				} catch (InvalidContentException e) {
					if (!"org/eclipse/equinox/security/junit/CorruptClass.class".equals(entries[i].getName()))
						fail("Unexpected corruption in: " + entries[i].getName(), e);
				}
				SignerInfo[] entryInfos = entries[i].getSignerInfos();
				assertNotNull("SignerInfo is null", entryInfos);
				assertEquals("wrong number of entry signers", 1, entryInfos.length);
				assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
			}

		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	// positve 1 signer, 1 tsa
	public void testSignedContent16() {
		try {
			File signedTsaFile = getEntryFile(getTestJarPath("signed_tsa"));
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			// get the signed content for the bundle
			SignedContent signedContent = getSignedContentFactory().getSignedContent(signedTsaFile);
			assertNotNull("SignedContent is null", signedContent);
			// check if it is signed
			assertTrue("Should be signed", signedContent.isSigned());
			// get the signer infos
			SignerInfo[] infos = signedContent.getSignerInfos();
			assertNotNull("SignerInfo is null", infos);
			assertEquals("wrong number of signers", 1, infos.length);

			assertNotNull("Signing time is null!", signedContent.getSigningTime(infos[0]));

		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}

	//SignedContent positive test: signed jar, 1 trusted signer
	public void testBug225090_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed"));
		getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull("SignedContent is null", signedContent);
		// check if it is signed
		assertTrue("Should be signed", signedContent.isSigned());
		// get the signer infos
		SignerInfo[] infos = signedContent.getSignerInfos();
		assertNotNull("SignerInfo is null", infos);
		assertEquals("wrong number of signers", 1, infos.length);
		// check the signer validity
		signedContent.checkValidity(infos[0]);
		// check the signer trust
		assertTrue("Signer is not trusted", infos[0].isTrusted());
		// check the trust anchor
		assertNotNull("Trust anchor is null", infos[0].getTrustAnchor());
		// verify and validate the entries
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		for (int i = 0; i < entries.length; i++) {
			entries[i].verify();
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 1, entryInfos.length);
			assertEquals("Entry signer does not equal content signer", infos[0], entryInfos[0]);
		}
		signedFile.delete();
		assertFalse("File should not exist", signedFile.exists());
	}

	//	SignedContent positive test: unsigned jar
	public void testBug225090_02() throws Exception {
		File unsignedFile = copyEntryFile(getTestJarPath("unsigned"));

		assertNotNull("Could not find unsigned file!", unsignedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(unsignedFile);
		assertNotNull("SignedContent is null", signedContent);
		assertFalse("Content is signed!!", signedContent.isSigned());
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		for (int i = 0; i < entries.length; i++) {
			entries[i].verify();
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 0, entryInfos.length);
		}
		unsignedFile.delete();
		assertFalse("File should not exist", unsignedFile.exists());
	}

	public void testBug228427_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata"));

		assertNotNull("Could not find signed file!", signedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull("SignedContent is null", signedContent);
		assertTrue("Content is not signed!!", signedContent.isSigned());
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		assertEquals("Incorrect number of signed entries", 4, entries.length);
		for (int i = 0; i < entries.length; i++) {
			entries[i].verify();
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 1, entryInfos.length);
		}
		signedFile.delete();
		assertFalse("File should not exist", signedFile.exists());
	}

	public void testBug228427_02() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_added"));

		assertNotNull("Could not find signed file!", signedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull("SignedContent is null", signedContent);
		assertTrue("Content is not signed!!", signedContent.isSigned());
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		assertEquals("Incorrect number of signed entries", 4, entries.length);
		for (int i = 0; i < entries.length; i++) {
			entries[i].verify();
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 1, entryInfos.length);
		}
		signedFile.delete();
		assertFalse("File should not exist", signedFile.exists());
	}

	public void testBug228427_03() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_corrupt"));

		assertNotNull("Could not find signed file!", signedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull("SignedContent is null", signedContent);
		assertTrue("Content is not signed!!", signedContent.isSigned());
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		assertEquals("Incorrect number of signed entries", 4, entries.length);
		for (int i = 0; i < entries.length; i++) {
			try {
				entries[i].verify();
				assertFalse("Wrong entry is validated: " + entries[i].getName(), "META-INF/test/test1.file".equals(entries[i].getName()));
			} catch (InvalidContentException e) {
				assertEquals("Wrong entry is corrupted", "META-INF/test/test1.file", entries[i].getName());
			}
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 1, entryInfos.length);
		}
		signedFile.delete();
		assertFalse("File should not exist", signedFile.exists());
	}

	public void testBug228427_04() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_metadata_removed"));

		assertNotNull("Could not find signed file!", signedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		SignedContent signedContent = getSignedContentFactory().getSignedContent(signedFile);
		assertNotNull("SignedContent is null", signedContent);
		assertTrue("Content is not signed!!", signedContent.isSigned());
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		assertNotNull("Entries is null", entries);
		assertEquals("Incorrect number of signed entries", 4, entries.length);
		for (int i = 0; i < entries.length; i++) {
			try {
				entries[i].verify();
				assertFalse("Wrong entry is validated: " + entries[i].getName(), "META-INF/test.file".equals(entries[i].getName()));
			} catch (InvalidContentException e) {
				assertEquals("Wrong entry is corrupted", "META-INF/test.file", entries[i].getName());
			}
			SignerInfo[] entryInfos = entries[i].getSignerInfos();
			assertNotNull("SignerInfo is null", entryInfos);
			assertEquals("wrong number of entry signers", 1, entryInfos.length);
		}
		signedFile.delete();
		assertFalse("File should not exist", signedFile.exists());
	}

	public void testBug236329_01() throws Exception {
		File signedFile = copyEntryFile(getTestJarPath("signed_with_sf_corrupted"));

		assertNotNull("Could not find signed file!", signedFile);
		//getTrustEngine().addTrustAnchor(anchor, alias);

		// get the signed content for the bundle
		try {
			getSignedContentFactory().getSignedContent(signedFile);
			fail("Should have gotten a SignatureException for file: " + signedFile);
		} catch (SignatureException e) {
			// expected
		}
	}
}
