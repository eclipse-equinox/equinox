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

import static org.eclipse.osgi.tests.OSGiTestsActivator.PI_OSGI_TESTS;
import static org.eclipse.osgi.tests.OSGiTestsActivator.addRequiredOSGiTestsBundles;
import static org.eclipse.osgi.tests.OSGiTestsActivator.getContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;

// This is for the most part a stripped down copy of ApplicationAdminTest.
public class ApplicationRelaunchTest {
	public static final String testRunnerRelauncherApp = PI_OSGI_TESTS + ".relaunchApp"; //$NON-NLS-1$
	public static final String testResults = "test.results"; //$NON-NLS-1$
	public static final String SUCCESS = "success"; //$NON-NLS-1$
	public static final String simpleResults = "test.simpleResults"; //$NON-NLS-1$

	@RegisterExtension
	static final SessionTestExtension extension = SessionTestExtension.forPlugin(PI_OSGI_TESTS)
			.withApplicationId(testRunnerRelauncherApp).withCustomization(createSessionConfiguration()).create();

	private static final CustomSessionConfiguration createSessionConfiguration() {
		CustomSessionConfiguration configuration = SessionTestExtension.createCustomConfiguration();
		addRequiredOSGiTestsBundles(configuration);
		return configuration;
	}

	@BeforeAll
	@ExecuteInHost
	public static void setup() {
		extension.setSystemProperty("eclipse.application.registerDescriptors", "true");
	}

	private ApplicationDescriptor getApplication(String appName) throws InvalidSyntaxException {
		BundleContext context = getContext();
		assertNotNull(context, "BundleContext is null"); //$NON-NLS-1$
		Class appDescClass = ApplicationDescriptor.class;
		assertNotNull(appDescClass, "ApplicationDescriptor.class is null"); //$NON-NLS-1$
		ServiceReference[] refs = context.getServiceReferences(appDescClass.getName(),
				"(" + ApplicationDescriptor.APPLICATION_PID + "=" + appName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (refs == null || refs.length == 0) {
			refs = getContext().getServiceReferences(ApplicationDescriptor.class.getName(), null);
			String availableApps = ""; //$NON-NLS-1$
			if (refs != null) {
				for (int i = 0; i < refs.length; i++) {
					availableApps += refs[i].getProperty(ApplicationDescriptor.APPLICATION_PID);
					if (i < refs.length - 1) {
						availableApps += ","; //$NON-NLS-1$
					}
				}
			}
			fail("Could not find app pid: " + appName + " available apps are: " + availableApps); //$NON-NLS-1$ //$NON-NLS-2$
		}
		ApplicationDescriptor result = (ApplicationDescriptor) getContext().getService(refs[0]);
		if (result != null) {
			getContext().ungetService(refs[0]);
		} else {
			fail("Could not get application descriptor service: " + appName); //$NON-NLS-1$
		}
		return result;
	}

	private HashMap getArguments() {
		HashMap args = new HashMap();
		args.put(testResults, new HashMap());
		return args;
	}

	@Test
	public void testRelaunch() throws Exception {
		// this is the same as ApplicationAdminTest.testSimpleApp() (but launched
		// through a different test runner app RelaunchApp which is the thing being
		// tested)
		ApplicationDescriptor app = getApplication(PI_OSGI_TESTS + ".simpleApp"); //$NON-NLS-1$
		HashMap args = getArguments();
		HashMap results = (HashMap) args.get(testResults);
		ApplicationHandle handle = app.launch(args);
		handle.destroy();
		String result = (String) results.get(simpleResults);
		assertEquals(SUCCESS, result, "Check application result"); //$NON-NLS-1$
	}

}
