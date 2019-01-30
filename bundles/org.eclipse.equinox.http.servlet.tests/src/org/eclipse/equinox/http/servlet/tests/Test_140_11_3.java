/*******************************************************************************
 * Copyright (c) Jan. 26, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - tests
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.http.runtime.HttpServiceRuntime;

public class Test_140_11_3 extends BaseTest {

	public static final String	SERVICE_NAMESPACE					= "osgi.service";
	public static final String	CAPABILITY_OBJECTCLASS_ATTRIBUTE	= "objectClass";

	@Test
	public void test_140_11_3() throws Exception {
		BundleContext context = getBundleContext();

		ServiceReference<HttpServiceRuntime> srA = context.getServiceReference(HttpServiceRuntime.class);

		BundleWiring bundleWiring = srA.getBundle().adapt(BundleWiring.class);

		List<BundleCapability> capabilities = bundleWiring.getCapabilities(SERVICE_NAMESPACE);

		boolean found = false;

		for (Capability capability : capabilities) {
			Map<String, Object> attributes = capability.getAttributes();
			@SuppressWarnings("unchecked")
			List<String> objectClasses = (List<String>) attributes.get(CAPABILITY_OBJECTCLASS_ATTRIBUTE);

			if ((objectClasses != null) && objectClasses.contains(HttpServiceRuntime.class.getName())) {
				Map<String, String> directives = capability.getDirectives();

				String uses = directives.get("uses");

				List<String> packages = Arrays.asList(uses.split(","));

				assertTrue(packages.contains("org.osgi.service.http.runtime"));
				assertTrue(packages.contains("org.osgi.service.http.runtime.dto"));

				found = true;
			}
		}

		assertTrue(found);
	}
}
