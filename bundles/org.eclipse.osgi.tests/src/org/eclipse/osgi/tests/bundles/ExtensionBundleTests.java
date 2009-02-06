/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.Bundle;

public class ExtensionBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ExtensionBundleTests.class);
	}

	public void testFrameworkExtension01() throws Exception {
		Bundle fwkext = installer.installBundle("ext.framework.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.framework.a.importer"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testFrameworkExtension02() throws Exception {
		Bundle fwkext = installer.installBundle("ext.framework.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.framework.a.requires"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testExtClasspathExtension01() throws Exception {
		Bundle fwkext = installer.installBundle("ext.extclasspath.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.extclasspath.a.importer"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
