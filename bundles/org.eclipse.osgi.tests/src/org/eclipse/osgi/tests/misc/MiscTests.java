/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.sources.SingleSourcePackage;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class MiscTests {
	@Test
	public void testBug251427() {
		Bundle testsBundle = OSGiTestsActivator.getBundle();
		assertNotNull("tests bundle is null", testsBundle); //$NON-NLS-1$
		Bundle harnessBundle = Platform.getBundle("org.eclipse.core.tests.harness"); //$NON-NLS-1$
		assertNotNull("harness bundle is null", harnessBundle); //$NON-NLS-1$

		ModuleWiring testsWiring = (ModuleWiring) testsBundle.adapt(BundleWiring.class);
		ModuleWiring harnessWiring = (ModuleWiring) harnessBundle.adapt(BundleWiring.class);

		SingleSourcePackage p111 = new SingleSourcePackage("pkg1", (BundleLoader) testsWiring.getModuleLoader()); //$NON-NLS-1$
		SingleSourcePackage p121 = new SingleSourcePackage("pkg1", (BundleLoader) testsWiring.getModuleLoader()); //$NON-NLS-1$
		SingleSourcePackage p112 = new SingleSourcePackage("pkg1", (BundleLoader) harnessWiring.getModuleLoader()); //$NON-NLS-1$

		SingleSourcePackage p212 = new SingleSourcePackage("pkg2", (BundleLoader) harnessWiring.getModuleLoader()); //$NON-NLS-1$
		SingleSourcePackage p222 = new SingleSourcePackage("pkg2", (BundleLoader) harnessWiring.getModuleLoader()); //$NON-NLS-1$
		SingleSourcePackage p211 = new SingleSourcePackage("pkg2", (BundleLoader) testsWiring.getModuleLoader()); //$NON-NLS-1$

		assertEquals("sources not equal", p111, p121); //$NON-NLS-1$
		assertEquals("sources hashCode not equal", p111.hashCode(), p121.hashCode()); //$NON-NLS-1$
		assertEquals("sources not equal", p212, p222); //$NON-NLS-1$
		assertEquals("sources hashCode not equal", p212.hashCode(), p222.hashCode()); //$NON-NLS-1$

		assertFalse("sources are equal", p111.equals(p112)); //$NON-NLS-1$
		assertFalse("sources are equal", p212.equals(p211)); //$NON-NLS-1$
	}
}
