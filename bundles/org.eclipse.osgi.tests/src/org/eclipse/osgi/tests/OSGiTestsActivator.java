/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 578599 - Make org.eclipse.osgi.tests' activator obsolete
 *******************************************************************************/
package org.eclipse.osgi.tests;

import java.util.stream.Stream;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class OSGiTestsActivator {
	public static final String PI_OSGI_TESTS = "org.eclipse.osgi.tests";

	public static final String TEST_FILES_ROOT = "test_files/";

	private OSGiTestsActivator() {
	}

	public static Bundle getBundle() {
		return FrameworkUtil.getBundle(OSGiTestsActivator.class);
	}

	public static BundleContext getContext() {
		return getBundle().getBundleContext();
	}

	public static void addRequiredOSGiTestsBundles(ConfigurationSessionTestSuite suite) {
		suite.addMinimalBundleSet();
		suite.addThisBundle();
		getClassesFromRequiredOSGITestsBundles().forEach(suite::addBundle);
	}

	public static void addRequiredOSGiTestsBundles(CustomSessionConfiguration sessionConfiguration) {
		getClassesFromRequiredOSGITestsBundles().forEach(sessionConfiguration::addBundle);
	}

	private static Stream<Class<?>> getClassesFromRequiredOSGITestsBundles() {
		return Stream.of( //
				org.osgi.util.function.Function.class, //
				org.osgi.util.measurement.Measurement.class, //
				org.osgi.util.position.Position.class, //
				org.osgi.util.promise.Promise.class, //
				org.osgi.util.xml.XMLParserActivator.class, //
				org.osgi.service.event.Event.class //
		);
	}

}
