/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.osgi.tests;

import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.osgi.framework.BundleContext;

/**
 * @since 3.1
 */
public class OSGiTest extends CoreTest {

	public static final String PI_OSGI_TESTS = "org.eclipse.osgi.tests";

	public OSGiTest() {
		super();
	}

	public OSGiTest(String name) {
		super(name);
	}

	public BundleContext getContext() {
		return OSGiTestsActivator.getContext();
	}

	public static void addRequiredOSGiTestsBundles(ConfigurationSessionTestSuite suite) {
		suite.addMinimalBundleSet();
		suite.addThisBundle();
		suite.addBundle(org.osgi.util.function.Function.class);
		suite.addBundle(org.osgi.util.measurement.Measurement.class);
		suite.addBundle(org.osgi.util.position.Position.class);
		suite.addBundle(org.osgi.util.promise.Promise.class);
		suite.addBundle(org.osgi.util.xml.XMLParserActivator.class);
		suite.addBundle(org.osgi.service.event.Event.class);
	}

}
