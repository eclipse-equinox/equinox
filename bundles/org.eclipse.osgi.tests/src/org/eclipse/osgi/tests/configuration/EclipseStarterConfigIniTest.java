/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package org.eclipse.osgi.tests.configuration;

import static org.eclipse.osgi.tests.OSGiTest.PI_OSGI_TESTS;
import static org.eclipse.osgi.tests.OSGiTest.addRequiredOSGiTestsBundles;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class EclipseStarterConfigIniTest extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(EclipseStarterConfigIniTest.class.getName());

		ConfigurationSessionTestSuite falseCompatBootDelegation = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, EclipseStarterConfigIniTest.class.getName());
		addRequiredOSGiTestsBundles(falseCompatBootDelegation);
		falseCompatBootDelegation.addTest(new EclipseStarterConfigIniTest("testFalseCompatBootDelegation"));
		falseCompatBootDelegation.setConfigIniValue("osgi.compatibility.bootdelegation", "false");
		suite.addTest(falseCompatBootDelegation);

		ConfigurationSessionTestSuite defaultCompatBootDelegation = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, EclipseStarterConfigIniTest.class.getName());
		addRequiredOSGiTestsBundles(defaultCompatBootDelegation);
		defaultCompatBootDelegation.addTest(new EclipseStarterConfigIniTest("testDefaultCompatBootDelegation"));
		suite.addTest(defaultCompatBootDelegation);
		return suite;
	}

	public EclipseStarterConfigIniTest(String name) {
		super(name);
	}

	public void testFalseCompatBootDelegation() throws Exception {
		doTestCompatBootDelegation(true);
	}

	public void testDefaultCompatBootDelegation() throws Exception {
		doTestCompatBootDelegation(false);
	}

	public void doTestCompatBootDelegation(boolean expectFailure) throws Exception {
		BundleContext context = OSGiTestsActivator.getContext();
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		ZipOutputStream zipOut = new ZipOutputStream(bytesOut);
		zipOut.putNextEntry(new ZipEntry("nothing"));
		zipOut.closeEntry();
		zipOut.close();
		Bundle b = context.installBundle(getName(), new ByteArrayInputStream(bytesOut.toByteArray()));
		String testClassName = javax.net.SocketFactory.class.getName();
		// The bundle does not import anything so should not find javax stuff
		if (expectFailure) {
			assertThrows("Expected to fail to load VM class from bundle that does not import it",
					ClassNotFoundException.class, () -> b.loadClass(testClassName));
		} else {
			try {
				b.loadClass(testClassName);
			} catch (ClassNotFoundException e) {
				AssertionFailedError error = new AssertionFailedError(
						"Expected to successfully load VM class from bundle that does not import it");
				error.initCause(e);
				throw error;
			}
		}
	}
}
