/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.tests.appadmin;

import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.osgi.tests.OSGiTest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;

// This is for the most part a stripped down copy of ApplicationAdminTest.
public class ApplicationRelaunchTest extends OSGiTest {
	public static final String testRunnerRelauncherApp = PI_OSGI_TESTS + ".relaunchApp"; //$NON-NLS-1$
	public static final String testResults = "test.results"; //$NON-NLS-1$
	public static final String SUCCESS = "success"; //$NON-NLS-1$
	public static final String simpleResults = "test.simpleResults"; //$NON-NLS-1$
	public static final String[] tests = new String[] { "testRelaunch" };
	private static final String PI_OSGI_SERVICES = "org.eclipse.osgi.services"; //$NON-NLS-1$

	public static Test suite() {
		TestSuite suite = new TestSuite(ApplicationRelaunchTest.class.getName());

		ConfigurationSessionTestSuite appAdminSessionTest = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, ApplicationRelaunchTest.class.getName());
		String[] ids = ConfigurationSessionTestSuite.MINIMAL_BUNDLE_SET;
		for (String id : ids) {
			appAdminSessionTest.addBundle(id);
		}
		appAdminSessionTest.addBundle(PI_OSGI_SERVICES);
		appAdminSessionTest.addBundle(PI_OSGI_TESTS);
		appAdminSessionTest.setApplicationId(testRunnerRelauncherApp);
		try {
			appAdminSessionTest.getSetup().setSystemProperty("eclipse.application.registerDescriptors", "true"); //$NON-NLS-1$//$NON-NLS-2$
		} catch (SetupException e) {
			throw new RuntimeException(e);
		}
		// we add tests the hard way so we can control the order of the tests.
		for (String test : tests) {
			appAdminSessionTest.addTest(new ApplicationRelaunchTest(test));
		}
		suite.addTest(appAdminSessionTest);
		return suite;
	}

	public ApplicationRelaunchTest(String name) {
		super(name);
	}

	private ApplicationDescriptor getApplication(String appName) {
		try {
			BundleContext context = getContext();
			assertNotNull("BundleContext is null!!", context); //$NON-NLS-1$
			Class appDescClass = ApplicationDescriptor.class;
			assertNotNull("ApplicationDescriptor.class is null!!", appDescClass); //$NON-NLS-1$
			ServiceReference[] refs = context.getServiceReferences(appDescClass.getName(), "(" + ApplicationDescriptor.APPLICATION_PID + "=" + appName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (refs == null || refs.length == 0) {
				refs = getContext().getServiceReferences(ApplicationDescriptor.class.getName(), null);
				String availableApps = ""; //$NON-NLS-1$
				if (refs != null) {
					for (int i = 0; i < refs.length; i++) {
						availableApps += refs[i].getProperty(ApplicationDescriptor.APPLICATION_PID);
						if (i < refs.length - 1)
							availableApps += ","; //$NON-NLS-1$
					}
				}
				fail("Could not find app pid: " + appName + " available apps are: " + availableApps); //$NON-NLS-1$ //$NON-NLS-2$
			}
			ApplicationDescriptor result = (ApplicationDescriptor) getContext().getService(refs[0]);
			if (result != null)
				getContext().ungetService(refs[0]);
			else
				fail("Could not get application descriptor service: " + appName); //$NON-NLS-1$
			return result;
		} catch (InvalidSyntaxException e) {
			fail("Could not create app filter", e); //$NON-NLS-1$
		}
		return null;
	}

	private HashMap getArguments() {
		HashMap args = new HashMap();
		args.put(testResults, new HashMap());
		return args;
	}

	public void testRelaunch() {
		// this is the same as ApplicationAdminTest.testSimpleApp() (but launched
		// through a different test runner app RelaunchApp which is the thing being
		// tested)
		ApplicationDescriptor app = getApplication(PI_OSGI_TESTS + ".simpleApp"); //$NON-NLS-1$
		HashMap args = getArguments();
		HashMap results = (HashMap) args.get(testResults);
		try {
			ApplicationHandle handle = app.launch(args);
			handle.destroy();
		} catch (Throwable e) {
			fail("failed to launch simpleApp", e); //$NON-NLS-1$
		}
		String result = (String) results.get(simpleResults);
		assertEquals("Check application result", SUCCESS, result); //$NON-NLS-1$
	}

}
