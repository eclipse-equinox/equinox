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

import junit.framework.Test;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.internal.service.security.DefaultAuthorizationEngine;
import org.eclipse.osgi.service.security.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

public class AuthorizeSignedTest extends BaseSecurityTest {

	protected void setUp() throws Exception {
		registerEclipseTrustEngine();
		AuthorizationEngine authEngine = getAuthorizationEngine();
		if (authEngine instanceof DefaultAuthorizationEngine) {
			((DefaultAuthorizationEngine) authEngine).setLoadPolicy(DefaultAuthorizationEngine.ENFORCE_SIGNED);
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		AuthorizationEngine authEngine = getAuthorizationEngine();
		if (authEngine instanceof DefaultAuthorizationEngine) {
			((DefaultAuthorizationEngine) authEngine).setLoadPolicy(0);
		}
	}

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(BUNDLE_SECURITY_TESTS, "Unit tests for AuthorizationEngine with 'signed' policy");
		addDefaultSecurityBundles(suite);
		setAuthorizationEnabled(suite);
		//setAuthorizationPolicy(suite, "signed");
		//for (int i = 0; i < s_tests.length; i++) {
		//	suite.addTest(s_tests[i]);
		suite.addTestSuite(AuthorizeSignedTest.class);
		//}
		return suite;
	}

	//test01: signed (allow)
	public void testAuthorize01() {
		Bundle testBundle = null;
		ServiceRegistration registration = null;
		final int[] s_test01called = new int[] {-1};
		try {
			registration = OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					s_test01called[0] = event.getResult();
				}
			}, null);
			testBundle = installBundle(getTestJarPath("signed")); //signed by ca1_leafa
			assertTrue("Handler not called!", s_test01called[0] != -1);
			assertEquals("Content was not allowed!", AuthorizationEvent.ALLOWED, s_test01called[0]);
		} finally {
			try {
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (BundleException e) {
				fail("Failed to uninstall bundle", e);
			}
			if (registration != null)
				registration.unregister();
		}
	}

	//test02: trusted (allow)
	public void testAuthorize02() {

		Bundle testBundle = null;
		ServiceRegistration registration = null;
		final int[] s_test02called = new int[] {-1};
		try {
			try {
				getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
			} catch (Throwable e) {
				fail("Unexpected exception", e);
			}

			registration = OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					s_test02called[0] = event.getResult();
				}
			}, null);

			testBundle = installBundle(getTestJarPath("signed")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test02called[0] != -1);
			assertEquals("Content was not allowed!", AuthorizationEvent.ALLOWED, s_test02called[0]);
		} finally {
			try {
				getTrustEngine().removeTrustAnchor("ca1_leafa");
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (Throwable t) {
				fail("Failed to uninstall bundle", t);
			}
			if (registration != null)
				registration.unregister();
		}
	}

	//test03: unsigned (deny)
	public void testAuthorize03() {

		Bundle testBundle = null;
		ServiceRegistration registration = null;
		final int[] s_test03called = new int[] {-1};
		try {
			registration = OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					s_test03called[0] = event.getResult();
				}
			}, null);

			testBundle = installBundle(getTestJarPath("unsigned")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test03called[0] != -1);
			assertEquals("Content was  allowed!", AuthorizationEvent.DENIED, s_test03called[0]);
		} finally {
			try {
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (Throwable t) {
				fail("Failed to uninstall bundle", t);
			}
			if (registration != null)
				registration.unregister();
		}
	}

	//test04: corrupted (allow, then explode later)
	public void testAuthorize04() {

		Bundle testBundle = null;
		ServiceRegistration registration = null;
		final int[] s_test04called = new int[] {-1};
		try {
			try {
				getTrustEngine().addTrustAnchor(getTestCertificate("ca1_leafa"), "ca1_leafa");
			} catch (Throwable e) {
				fail("Unexpected exception", e);
			}

			registration = OSGiTestsActivator.getContext().registerService(AuthorizationListener.class.getName(), new AuthorizationListener() {
				public void authorizationEvent(AuthorizationEvent event) {
					s_test04called[0] = event.getResult();
				}
			}, null);

			testBundle = installBundle(getTestJarPath("signed_with_corrupt")); //signed by ca1_leafa

			assertTrue("Handler not called!", s_test04called[0] != -1);
			assertEquals("Content was not allowed!", AuthorizationEvent.ALLOWED, s_test04called[0]);
		} finally {
			try {
				getTrustEngine().removeTrustAnchor("ca1_leafa");
				if (testBundle != null) {
					testBundle.uninstall();
				}
			} catch (Throwable t) {
				fail("Failed to uninstall bundle", t);
			}
			if (registration != null)
				registration.unregister();
		}
	}
	//test05: expired (deny) //TODO!
}
