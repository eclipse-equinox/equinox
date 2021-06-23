/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.storage.friends.PasswordProviderDescription;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ObsoletesTest {

	final private static String LINUX_BUNDLE = "org.eclipse.equinox.security.linux"; //$NON-NLS-1$
	final private static String EXTENSION_POINT = "org.eclipse.equinox.security.secureStorage"; //$NON-NLS-1$
	final private static String LINUX_PASSWORD_PROVIDER = "org.eclipse.equinox.security.securestorage.linuxkeystoreintegrationjna"; //$NON-NLS-1$
	final private static String CLASS_NAME = "class"; //$NON-NLS-1$

	@Before
	public void setUp() {
		org.junit.Assume.assumeTrue(hasBundle(LINUX_BUNDLE));
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT);
		IExtension[] extensions = point.getExtensions();
		IExtension linuxExtension = null;
		for (IExtension extension : extensions) {
			String moduleID = extension.getUniqueIdentifier();
			if (moduleID == null) // IDs on those extensions are mandatory; if not specified, ignore the extension
				continue;
			moduleID = moduleID.toLowerCase();
			if (moduleID.equals(LINUX_PASSWORD_PROVIDER)) {
				linuxExtension = extension;
				break;
			}
		}
		org.junit.Assume.assumeTrue(linuxExtension != null);
		IConfigurationElement[] elements = linuxExtension.getConfigurationElements();
		org.junit.Assume.assumeTrue(elements.length > 0);
		IConfigurationElement element = elements[0]; // only one module is allowed per extension
		Object clazz;
		try {
			clazz = element.createExecutableExtension(CLASS_NAME);
			// Bug 537833 - on some Linux systems, the password provider does not work (e.g.
			// Linux KDE)
			org.junit.Assume.assumeTrue(
					clazz instanceof IValidatingPasswordProvider && ((IValidatingPasswordProvider) clazz).isValid());
		} catch (CoreException e) {
			org.junit.Assume.assumeNoException(e);
		}
	}

	@Test
	public void testObsoletes() {
		List<PasswordProviderDescription> descs = InternalExchangeUtils
				.passwordProvidersFind("org.eclipse.equinox.security.linuxkeystoreintegration");
		assertNotNull(descs);
		assertEquals(1, descs.size());
		PasswordProviderDescription desc = descs.get(0);
		assertEquals("org.eclipse.equinox.security.linuxkeystoreintegrationjna", desc.getId());
	}

	static private boolean hasBundle(String symbolicID) {
		BundleContext context = SecurityTestsActivator.getDefault().getBundleContext();
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			String bundleName = bundle.getSymbolicName();
			if (!symbolicID.equals(bundleName))
				continue;
			int bundleState = bundle.getState();
			return (bundleState != Bundle.INSTALLED) && (bundleState != Bundle.UNINSTALLED);
		}
		return false;
	}
}
