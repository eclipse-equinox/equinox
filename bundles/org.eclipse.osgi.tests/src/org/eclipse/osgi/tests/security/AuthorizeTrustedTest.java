/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.security;

import junit.framework.Test;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.service.security.AuthorizationEvent;
import org.eclipse.osgi.service.security.AuthorizationListener;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class AuthorizeTrustedTest extends BaseSecurityTest {

	protected void setUp() throws Exception {
		registerEclipseTrustEngine();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(BUNDLE_SECURITY_TESTS, "Unit tests for AuthorizationEngine with 'trusted' policy");
		addDefaultSecurityBundles(suite);
		setEclipseTrustEngine(suite);
		setAuthorizationEnabled(suite);
		setAuthorizationPolicy(suite, "trusted");
		//for (int i = 0; i < s_tests.length; i++) {
		//	suite.addTest(s_tests[i]);
		suite.addTestSuite(AuthorizeTrustedTest.class);
		//}
		return suite;
	}

	static boolean s_test01called = false;

	//test01: trusted, should pass
	public void testAuthorize01() {

		Bundle testBundle = null;
		try {
			getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");

			OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					assertEquals("Content is not allowed!", AuthorizationEvent.ALLOWED, event.getResult());
					s_test01called = true;
				}
			}, null);

			testBundle = installBundle(getTestJarPath("signed")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test01called);
		} catch (Throwable t) {
			fail("unexpected exception", t);
		} finally {
			try {
				getTrustEngine().removeTrustAnchor("ca1_leafa");
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (Throwable t) {
				fail("unexpected exception", t);
			}
		}
	}

	static boolean s_test02called = false;

	//test02: unsigned, should fail
	public void testAuthorize02() {

		Bundle testBundle = null;
		try {
			OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					assertEquals("Content was allowed!", AuthorizationEvent.DENIED, event.getResult());
					s_test02called = true;
				}
			}, null);

			testBundle = installBundle(getTestJarPath("unsigned"));
			//Thread.sleep(100);
			assertTrue("Handler not called!", s_test02called);

		} catch (Throwable t) {
			fail("unexpected exception", t);
		} finally {
			try {
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	static boolean s_test03called = false;

	//test03: untrusted, should fail
	public void testAuthorize03() {

		Bundle testBundle = null;
		try {
			OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					assertEquals("Content was allowed!", AuthorizationEvent.DENIED, event.getResult());
					s_test03called = true;
				}
			}, null);

			testBundle = installBundle(getTestJarPath("signed")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test03called);

		} catch (Throwable t) {
			fail("unexpected exception", t);
		} finally {
			try {
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	static boolean s_test04called = false;

	//test04: corrupt, should fail
	public void testAuthorize04() {

		Bundle testBundle = null;
		try {
			OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					assertEquals("Content is not allowed!", AuthorizationEvent.DENIED, event.getResult());
					s_test04called = true;
				}
			}, null);

			testBundle = installBundle(getTestJarPath("signed_with_corrupt")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test04called);

		} catch (Throwable t) {
			fail("Unexpected exception", t);
		} finally {
			try {
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
		}
	}

	//test05: TODO: expired, should fail //TODO!
}
