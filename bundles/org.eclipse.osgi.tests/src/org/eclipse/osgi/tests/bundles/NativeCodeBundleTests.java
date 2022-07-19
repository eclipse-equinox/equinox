/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleRevision;

public class NativeCodeBundleTests extends AbstractBundleTests {

	@Test
	public void testNativeCode01() throws Exception {
		Bundle nativetestA1 = installer.installBundle("nativetest.a1");
		nativetestA1.start();
		nativetestA1.stop();
		Object[] a1Results = simpleResults.getResults(1);

		installer.updateBundle("nativetest.a1", "nativetest.a2");
		nativetestA1.start();
		nativetestA1.stop();
		Object[] a2Results = simpleResults.getResults(1);
		assertTrue("1.0", a1Results.length == 1);
		assertTrue("1.1", a2Results.length == 1);
		assertNotNull("1.2", a1Results[0]);
		assertNotNull("1.3", a2Results[0]);
		assertFalse("1.4", a1Results[0].equals(a2Results[0]));
	}

	@Test
	public void testNativeCode02() throws Exception {
		Bundle nativetestB1 = installer.installBundle("nativetest.b1");
		nativetestB1.start();
		nativetestB1.stop();
		Object[] b1Results = simpleResults.getResults(1);

		installer.updateBundle("nativetest.b1", "nativetest.b2");
		nativetestB1.start();
		nativetestB1.stop();
		Object[] b2Results = simpleResults.getResults(1);
		assertTrue("1.0", b1Results.length == 1);
		assertTrue("1.1", b2Results.length == 1);
		assertNotNull("1.2", b1Results[0]);
		assertNotNull("1.3", b2Results[0]);
		assertFalse("1.4", b1Results[0].equals(b2Results[0]));
	}

	@Test
	public void testNativeCode03() throws Exception {
		setNativeAttribute("nativecodetest", "1");

		Bundle nativetestC = installer.installBundle("nativetest.c");
		nativetestC.start();
		nativetestC.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		assertEquals("1.1", "libs.test1", getContent((String) results[0]));
	}

	@Test
	public void testNativeCode04() throws Exception {
		setNativeAttribute("nativecodetest", "unresolved");
		Bundle nativetestC = installer.installBundle("nativetest.c");
		installer.resolveBundles(new Bundle[] {nativetestC});
		assertTrue("1.0", nativetestC.getState() == Bundle.INSTALLED);
	}

	@Test
	public void testNativeCode05() throws Exception {
		setNativeAttribute("nativecodetest", "2");
		Bundle nativetestC = installer.installBundle("nativetest.c");
		nativetestC.start();
		nativetestC.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		assertEquals("1.1", "libs.test3", getContent((String) results[0]));
	}

	@Test
	public void testNativeCode06() throws Exception {
		setNativeAttribute("nativecodetest", "3");
		Bundle nativetestC = installer.installBundle("nativetest.c");
		nativetestC.start();
		nativetestC.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		assertEquals("1.1", "libs.test2", getContent((String) results[0]));
	}

	@Test
	public void testNativeCode07() throws Exception {
		Bundle nativetestC = installer.installBundle("nativetest.d");
		nativetestC.start();
		nativetestC.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		assertNull("1.1", results[0]);
	}

	@Test
	public void testNativeCode08() throws Exception {
		setNativeAttribute("nativecodetest", "4");
		Bundle nativetestC = installer.installBundle("nativetest.c");
		nativetestC.start();
		nativetestC.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		// will be null since the native path does not exist
		assertNull("1.1", results[0]);
	}

	@Test
	public void testNativeCode09() throws Exception {
		setNativeAttribute("nativecodetest", "1");

		Bundle nativetest = installer.installBundle("nativetest.e");
		nativetest.start();
		nativetest.stop();
		Object[] results = simpleResults.getResults(1);

		assertTrue("1.0", results.length == 1);
		assertEquals("1.1", "libs.test1", getContent((String) results[0]));
	}

	private String getContent(String file) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			return br.readLine();
		}
	}

	private void setNativeAttribute(String key, String value) {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		ModuleRevision systemRevision = (ModuleRevision) systemBundle.adapt(BundleRevision.class);
		ModuleCapability nativeCapability = systemRevision.getModuleCapabilities(NativeNamespace.NATIVE_NAMESPACE).get(0);
		Map attrs = new HashMap(nativeCapability.getAttributes());
		attrs.put(key, value);
		nativeCapability.setTransientAttrs(attrs);
	}
}
