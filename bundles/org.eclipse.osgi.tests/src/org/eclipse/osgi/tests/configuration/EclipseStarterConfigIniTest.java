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

import static org.eclipse.osgi.tests.OSGiTestsActivator.PI_OSGI_TESTS;
import static org.eclipse.osgi.tests.OSGiTestsActivator.addRequiredOSGiTestsBundles;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.AssertionFailedError;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EclipseStarterConfigIniTest {

	private static CustomSessionConfiguration sessionConfiguration = createSessionConfiguration();

	@RegisterExtension
	static SessionTestExtension extension = SessionTestExtension.forPlugin(PI_OSGI_TESTS)
			.withCustomization(sessionConfiguration).create();

	private static CustomSessionConfiguration createSessionConfiguration() {
		CustomSessionConfiguration configuration = SessionTestExtension.createCustomConfiguration().setCascaded();
		configuration.setConfigIniValue("osgi.compatibility.bootdelegation", "false");
		addRequiredOSGiTestsBundles(configuration);
		return configuration;
	}

	@Test
	@Order(1)
	public void testFalseCompatBootDelegation() throws Exception {
		doTestCompatBootDelegation("compat.false", true);
	}

	@Test
	@Order(2)
	@ExecuteInHost
	public void setDefaultCompatBootDelegation() {
		sessionConfiguration.setConfigIniValue("osgi.compatibility.bootdelegation", null);
	}

	@Test
	@Order(3)
	public void testDefaultCompatBootDelegation() throws Exception {
		doTestCompatBootDelegation("compat.default", false);
	}

	public void doTestCompatBootDelegation(String bundleName, boolean expectFailure) throws Exception {
		BundleContext context = OSGiTestsActivator.getContext();
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		ZipOutputStream zipOut = new ZipOutputStream(bytesOut);
		zipOut.putNextEntry(new ZipEntry("nothing"));
		zipOut.closeEntry();
		zipOut.close();
		Bundle b = context.installBundle(bundleName, new ByteArrayInputStream(bytesOut.toByteArray()));
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
