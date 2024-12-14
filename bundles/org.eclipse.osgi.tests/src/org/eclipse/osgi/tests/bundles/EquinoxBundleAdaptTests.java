/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.security.Permission;
import java.security.ProtectionDomain;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.tests.securityadmin.SecurityManagerTests;
import org.junit.Assume;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class EquinoxBundleAdaptTests extends AbstractBundleTests {

	@Test
	public void testAdapt_Module() throws Exception {
		Bundle bundle = installer.installBundle("test");
		Module module = bundle.adapt(org.eclipse.osgi.container.Module.class);
		assertEquals(((EquinoxBundle) bundle).getModule(), module);
	}

	@Test
	@SuppressWarnings({ "deprecation", "removal" }) // SecurityManager
	public void testAdapt_ProtectionDomain() throws Exception {
		Assume.assumeTrue("Security-Manager is disallowed", SecurityManagerTests.isSecurityManagerAllowed());

		Bundle bundle = installer.installBundle("test");
		SecurityManager previousSM = System.getSecurityManager();
		try {
			System.setSecurityManager(new SecurityManager() {
				@Override
				public void checkPermission(Permission perm) {
					// Just permit everything
				}
			});
			ProtectionDomain domain = bundle.adapt(java.security.ProtectionDomain.class);
			assertNotNull(domain);
		} finally {
			System.setSecurityManager(previousSM);
		}
	}

	@Test
	public void testAdapt_SignedContent() throws Exception {
		Bundle bundle = installer.installBundle("test");
		SignedContent content = bundle.adapt(org.eclipse.osgi.signedcontent.SignedContent.class);
		assertNotNull(content);
	}

	@Test
	public void testAdapt_File() throws Exception {
		URL testBundleURL = FrameworkUtil.getBundle(EquinoxBundleAdaptTests.class).getEntry(".");
		File testBundleRoot = new File(FileLocator.toFileURL(testBundleURL).toURI()).getCanonicalFile();

		Bundle bundle = installer.installBundle("test");

		File file = bundle.adapt(java.io.File.class).getCanonicalFile();
		assertEquals(new File(testBundleRoot, "bundle_tests"), file.getParentFile());
		assertEquals("test", file.getName().replace(".jar", ""));
	}
}
