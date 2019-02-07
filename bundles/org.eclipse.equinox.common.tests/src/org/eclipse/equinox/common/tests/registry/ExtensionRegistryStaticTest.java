/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests.registry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import junit.framework.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class ExtensionRegistryStaticTest extends TestCase {

	private BundleContext fBundleContext;

	public ExtensionRegistryStaticTest(String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fBundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
	}

	public void testA() throws IOException, BundleException {
		//test the addition of an extension point
		String name = "A";
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/test" + name);
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		testExtensionPoint(name);
	}

	public void testAFromCache() {
		//Check that it has been persisted
		testExtensionPoint("A");
	}

	private void testExtensionPoint(String name) {
		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name).getLabel(), "Label xpt" + name);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name).getNamespace(), "test" + name);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name).getNamespaceIdentifier(), "test" + name);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name).getContributor().getName(), "test" + name);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("test" + name + ".xpt" + name).getSchemaReference(), "schema/xpt" + name + ".exsd");
	}

	public void testB() throws IOException, BundleException {
		//test the addition of an extension without extension point
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testB/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		assertNull(RegistryFactory.getRegistry().getExtension("testB2", "xptB2", "ext1"));
	}

	public void testBFromCache() throws IOException, BundleException {
		// Test the addition of an extension point when orphans extension exists
		assertNull(RegistryFactory.getRegistry().getExtension("testB2", "xptB2", "ext1"));
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testB/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});
		testExtensionPoint("B2");

		//	Test the configuration elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testB2", "xptB2", "testB1.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testB2.xptB2").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testB1").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testB1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testB2", "xptB2", "testB1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testB2.xptB2", "testB1.ext1"));

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testB2.xptB2").getExtension("testB1.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testB2.xptB2").getExtensions()[0].getUniqueIdentifier(), "testB1.ext1");

		//uninstall the bundle contributing the extension point
		bundle02.uninstall();
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});

		assertNull(RegistryFactory.getRegistry().getExtension("testB1.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensions("testB1").length, 0);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoints("testB2").length, 0);
		assertNull(RegistryFactory.getRegistry().getExtensionPoint("testB2.xptB2"));
	}

	public void testBRemoved() {
		//Test if testB has been removed.
		assertNull(RegistryFactory.getRegistry().getExtension("testB1.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensions("testB1").length, 0);
		assertEquals(RegistryFactory.getRegistry().getExtensionPoints("testB2").length, 0);
		assertNull(RegistryFactory.getRegistry().getExtensionPoint("testB2.xptB2"));
	}

	public void testC() throws IOException, BundleException {
		//test the addition of an extension point then the addition of an extension
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testC/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		testExtensionPoint("C1");
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testC/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});

		//Test the configurataion elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testC1", "xptC1", "testC2.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testC1.xptC1").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testC2").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testC2.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testC1", "xptC1", "testC2.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testC1.xptC1", "testC2.ext1"));

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testC1.xptC1").getExtension("testC2.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testC1.xptC1").getExtensions()[0].getUniqueIdentifier(), "testC2.ext1");
	}

	public void testD() throws IOException, BundleException {
		//test the addition of an extension then the addition of an extension point
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testD/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testD/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		testExtensionPoint("D1");

		//Test the configurataion elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testD1", "xptD1", "testD2.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testD1.xptD1").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testD2").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testD2.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testD1", "xptD1", "testD2.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testD1.xptD1", "testD2.ext1"));

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testD1.xptD1").getExtension("testD2.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testD1.xptD1").getExtensions()[0].getUniqueIdentifier(), "testD2.ext1");
	}

	public void testE() throws IOException, BundleException {
		//test the addition of an extension point and then add the extension through a fragment
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testE/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testE/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});
		testExtensionPoint("E1");

		//Test the configurataion elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testE1", "xptE1", "testE1.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testE1.xptE1").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testE1").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testE1", "xptE1", "testE1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testE1.xptE1", "testE1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testE1.ext1")); //This test exhibits a bug in the 3.0 implementation

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testE1.xptE1").getExtension("testE1.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testE1.xptE1").getExtensions()[0].getUniqueIdentifier(), "testE1.ext1");
	}

	public void testF() throws IOException, BundleException {
		//test the addition of the extension through a fragment then the addition of an extension point
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testF/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testF/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		testExtensionPoint("F1");

		//Test the configurataion elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testF1", "xptF1", "testF1.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testF1.xptE1").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testF1").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testF1", "xptF1", "testF1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testF1.xptF1", "testF1.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testF1.ext1")); //This test exhibits a bug in the 3.0 implementation

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testF1.xptF1").getExtension("testF1.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testF1.xptF1").getExtensions()[0].getUniqueIdentifier(), "testF1.ext1");

		//Test the namespace
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testF1"), true);
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testF2"), false);
	}

	public void testG() throws IOException, BundleException {
		//fragment contributing an extension point to a plugin that do not have extension or extension point
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testG/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testG/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2").getLabel(), "Label xptG2");
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2").getNamespace(), "testG1");
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2").getNamespaceIdentifier(), "testG1");
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2").getContributor().getName(), "testG1");
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testG1.xptG2").getSchemaReference(), "schema/xptG2.exsd");

		//Test the namespace
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testG1"), true);
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testG2"), false);
	}

	public void testH() throws IOException, BundleException {
		//		fragment contributing an extension to a plugin that does not have extension or extension point
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testH/1");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});
		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testH/2");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});
		Bundle bundle03 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testH/3");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle03});

		testExtensionPoint("H1");

		//Test the configurataion elements
		assertEquals(RegistryFactory.getRegistry().getExtension("testH1", "xptH1", "testH3.ext1").getConfigurationElements().length, 0);
		assertEquals(RegistryFactory.getRegistry().getConfigurationElementsFor("testH1.xptH1").length, 0);

		//Test the number of extension in the namespace
		assertEquals(RegistryFactory.getRegistry().getExtensions("testH3").length, 1);

		//Test the extension
		assertNotNull(RegistryFactory.getRegistry().getExtension("testH1", "xptH1", "testH3.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testH1.xptH1", "testH3.ext1"));
		assertNotNull(RegistryFactory.getRegistry().getExtension("testH3.ext1")); //This test exhibits a bug in the 3.0 implementation

		assertNotNull(RegistryFactory.getRegistry().getExtensionPoint("testH1.xptH1").getExtension("testH3.ext1"));
		assertEquals(RegistryFactory.getRegistry().getExtensionPoint("testH1.xptH1").getExtensions()[0].getUniqueIdentifier(), "testH3.ext1");

		//Test the namespace
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testH1"), true);
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testH3"), true);
		assertEquals(Arrays.asList(RegistryFactory.getRegistry().getNamespaces()).contains("testH2"), false); //fragments do not come with their namespace
	}

	public void test71826() throws MalformedURLException, BundleException, IOException {
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/71826/fragmentF");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});

		Bundle bundle02 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/71826/pluginB");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle02});

		Bundle bundle03 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/71826/pluginA");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle03});

		IExtensionPoint xp = RegistryFactory.getRegistry().getExtensionPoint("71826A.xptE");
		assertNotNull("1.0", xp);
		IExtension[] exts = xp.getExtensions();
		assertEquals("1.1", 2, exts.length);
		assertNotNull("1.2", xp.getExtension("71826A.F1"));
		assertNotNull("1.3", xp.getExtension("71826B.B1"));
	}

	public void testJ() throws MalformedURLException, BundleException, IOException {
		//Test the third level configuration elements
		Bundle bundle01 = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/testI");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {bundle01});

		IExtension ext = RegistryFactory.getRegistry().getExtension("testI.ext1");
		IConfigurationElement ce = ext.getConfigurationElements()[0];
		assertEquals(ce.getName(), "ce");
		assertNotNull(ce.getValue());
		assertEquals(ce.getChildren()[0].getName(), "ce2");
		assertNull(ce.getChildren()[0].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[0].getName(), "ce3");
		assertNull(ce.getChildren()[0].getChildren()[0].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[1].getName(), "ce3");
		assertNull(ce.getChildren()[0].getChildren()[1].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[0].getChildren()[0].getName(), "ce4");
		assertNotNull(ce.getChildren()[0].getChildren()[0].getChildren()[0].getValue());
	}

	public void testJbis() {
		//Test the third level configuration elements from cache
		IExtension ext = RegistryFactory.getRegistry().getExtension("testI.ext1");
		IConfigurationElement ce = ext.getConfigurationElements()[0];
		assertEquals(ce.getName(), "ce");
		assertNotNull(ce.getValue());
		assertEquals(ce.getChildren()[0].getName(), "ce2");
		assertNull(ce.getChildren()[0].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[0].getName(), "ce3");
		assertNull(ce.getChildren()[0].getChildren()[0].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[1].getName(), "ce3");
		assertNull(ce.getChildren()[0].getChildren()[1].getValue());
		assertEquals(ce.getChildren()[0].getChildren()[0].getChildren()[0].getName(), "ce4");
		assertNotNull(ce.getChildren()[0].getChildren()[0].getChildren()[0].getValue());
	}

	public void testNonSingletonBundle() throws MalformedURLException, BundleException, IOException {
		//Non singleton bundles are not supposed to be added
		Bundle nonSingletonBundle = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/nonSingleton");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {nonSingletonBundle});
		assertNull(RegistryFactory.getRegistry().getExtensionPoint("NonSingleton.ExtensionPoint"));
	}

	public void testSingletonFragment() throws MalformedURLException, BundleException, IOException {
		//Fragments to non singleton host can not contribute extension or extension points
		Bundle fragmentToNonSingleton = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/fragmentToNonSingleton");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {fragmentToNonSingleton});
		assertNull(RegistryFactory.getRegistry().getExtensionPoint("NonSingleton.Bar"));
	}

	public void testNonSingletonFragment() throws MalformedURLException, BundleException, IOException {
		//Non singleton bundles are not supposed to be added
		Bundle regular = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/nonSingletonFragment/plugin");
		Bundle nonSingletonFragment = BundleTestingHelper.installBundle("", fBundleContext, "Plugin_Testing/registry/nonSingletonFragment/fragment");
		BundleTestingHelper.refreshPackages(fBundleContext, new Bundle[] {regular, nonSingletonFragment});
		assertNull(RegistryFactory.getRegistry().getExtensionPoint("Regular.Bar"));
	}

	public static Test suite() {
		//Order is important
		TestSuite sameSession = new TestSuite(ExtensionRegistryStaticTest.class.getName());
		sameSession.addTest(new ExtensionRegistryStaticTest("testA"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testAFromCache"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testB"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testBFromCache"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testBRemoved"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testC"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testD"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testE"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testF"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testG"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testH"));
		sameSession.addTest(new ExtensionRegistryStaticTest("test71826"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testJ"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testJbis"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testNonSingletonBundle"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testSingletonFragment"));
		sameSession.addTest(new ExtensionRegistryStaticTest("testNonSingletonFragment"));
		return sameSession;
	}
}
