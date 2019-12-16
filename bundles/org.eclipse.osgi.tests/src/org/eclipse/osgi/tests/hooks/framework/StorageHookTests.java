/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
package org.eclipse.osgi.tests.hooks.framework;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.SystemBundleTests;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.service.packageadmin.PackageAdmin;

public class StorageHookTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "test";
	private static final String HOOK_CONFIGURATOR_BUNDLE = "storage.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "org.eclipse.osgi.tests.hooks.framework.storage.a.TestHookConfigurator";
	private static final String HOOK_CONFIGURATOR_FIELD_CREATE_STORAGE_HOOK_CALLED = "createStorageHookCalled";
	private static final String HOOK_CONFIGURATOR_FIELD_FAIL_LOAD = "failLoad";
	private static final String HOOK_CONFIGURATOR_FIELD_INVALID = "invalid";
	private static final String HOOK_CONFIGURATOR_FIELD_INVALID_FACTORY_CLASS = "invalidFactoryClass";
	private static final String HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED = "validateCalled";
	private static final String HOOK_CONFIGURATOR_FIELD_DELETING_CALLED = "deletingGenerationCalled";
	private static final String HOOK_CONFIGURATOR_FIELD_ADAPT_MANIFEST = "adaptManifest";
	private static final String HOOK_CONFIGURATOR_FIELD_ADAPT_CAPABILITY_ATTRIBUTE = "adaptCapabilityAttribute";
	private static final String HOOK_CONFIGURATOR_FIELD_REPLACE_BUILDER = "replaceModuleBuilder";
	private static final String HOOK_CONFIGURATOR_FIELD_HANDLE_CONTENT = "handleContentConnection";
	private static final String HOOK_CONFIGURATOR_FIELD_NULL_STORAGE_HOOK = "returnNullStorageHook";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	/*
	 * Bundles must be discarded if a storage hook throws an 
	 * IllegalStateException during validation.
	 */
	public void testBundleDiscardedWhenClasspathStorageHookInvalidates() throws Exception {
		initAndStartFramework();
		installBundle();
		setStorageHookInvalid(true);
		restartFramework();
		assertStorageHookValidateCalled();
		assertBundleDiscarded();
	}

	/*
	 * Bundles must not be discarded when a storage hook says they are valid.
	 */
	public void testBundleNotDiscardedWhenClasspathStorageHookValidates() throws Exception {
		initAndStartFramework();
		installBundle();
		setStorageHookInvalid(false);
		restartFramework();
		assertStorageHookValidateCalled();
		assertBundleNotDiscarded();
	}

	/*
	 * A storage hook with the wrong factory class should cause bundle
	 * installation to fail.
	 */
	public void testWrongStorageHookFactoryClassOnBundleInstall() throws Exception {
		setFactoryClassInvalid(true);
		initAndStartFramework();
		try {
			installBundle();
			fail("Bundle install should have failed");
		} catch (BundleException e) {
			assertBundleException(e);
		}
		assertCreateStorageHookCalled();
	}

	/*
	 * A storage hook with the wrong factory class should cause bundle update
	 * to fail.
	 */
	public void testWrongStorageHookFactoryClassOnBundleUpdate() throws Exception {
		initAndStartFramework();
		installBundle();
		setFactoryClassInvalid(true);
		try {
			updateBundle();
			fail("Bundle update should have failed");
		} catch (BundleException e) {
			assertBundleException(e);
		}
		assertCreateStorageHookCalled();
	}

	/*
	 * A storage hook with the wrong factory class should cause a framework
	 * restart with persisted bundles to fail.
	 */
	public void testWrongStorageHookFactoryClassOnFrameworkRestart() throws Exception {
		initAndStartFramework();
		installBundle();
		setFactoryClassInvalid(true);
		try {
			restartFramework();
			fail("Framework restart should have failed");
		} catch (IllegalStateException e) {
			assertThrowable(e);
		}
		assertCreateStorageHookCalled();
	}

	public void testCleanOnFailLoad() throws Exception {
		initAndStartFramework();
		installBundle();
		setFactoryHookFailLoad(true);
		restartFramework();
		assertBundleDiscarded();
		// install a bundle without reference to test that the staging area is created correctly after clean
		File bundlesBase = new File(OSGiTestsActivator.getContext().getDataFile(getName()), "bundles");
		bundlesBase.mkdirs();
		framework.getBundleContext().installBundle(SystemBundleTests.createBundle(bundlesBase, getName(), false, false).toURI().toString());
	}

	public void testDeletingGenerationCalledOnDiscard() throws Exception {
		initAndStartFramework();
		installBundle();
		setStorageHookInvalid(true);
		restartFramework();
		assertStorageHookDeletingGenerationCalled();
		assertBundleDiscarded();
	}

	public void testDeletingGenerationCalledUninstall() throws Exception {
		initAndStartFramework();
		installBundle();
		Bundle b = framework.getBundleContext().getBundle(location);
		assertNotNull("Missing test bundle.", b);
		b.uninstall();
		assertStorageHookDeletingGenerationCalled();
	}

	public void testDeletingGenerationCalledUpdate() throws Exception {
		initAndStartFramework();
		installBundle();
		Bundle b = framework.getBundleContext().getBundle(location);
		assertNotNull("Missing test bundle.", b);
		b.update();
		assertStorageHookDeletingGenerationCalled();
	}

	public void testAdaptModuleRevisionBuilder() throws Exception {
		setFactoryClassAdaptManifest(true);
		initAndStartFramework();

		installBundle();
		Bundle b = framework.getBundleContext().getBundle(location);
		assertNotEquals("Wrong ID.", 5678, b.getBundleId());
		assertNotNull("Missing test bundle.", b);
		List<Capability> testCaps = b.adapt(BundleRevision.class).getCapabilities("test.file.path");
		assertEquals("Wrong number of test caps.", 1, testCaps.size());
		String path1 = (String) testCaps.get(0).getAttributes().get("test.file.path");
		assertNotNull("No path", path1);
		String operation1 = (String) testCaps.get(0).getAttributes().get("test.operation");
		assertEquals("Wrong operation", ModuleEvent.INSTALLED.toString(), operation1);
		String location1 = (String) testCaps.get(0).getAttributes().get("test.origin");
		assertEquals("Wrong origin", framework.getBundleContext().getBundle().getLocation(), location1);

		b.update();
		assertNotEquals("Wrong ID.", 5678, b.getBundleId());
		testCaps = b.adapt(BundleRevision.class).getCapabilities("test.file.path");
		assertEquals("Wrong number of test caps.", 1, testCaps.size());
		String path2 = (String) testCaps.get(0).getAttributes().get("test.file.path");
		assertNotNull("No path", path2);
		String operation2 = (String) testCaps.get(0).getAttributes().get("test.operation");
		assertEquals("Wrong operation", ModuleEvent.UPDATED.toString(), operation2);
		String location2 = (String) testCaps.get(0).getAttributes().get("test.origin");
		assertEquals("Wrong origin", location, location2);

		assertNotEquals("Path of updated bundle is the same.", path1, path2);

		framework.stop();
		framework.waitForStop(5000);

		// create new framework object to test loading of persistent capability.
		framework = createFramework(configuration);
		framework.start();
		b = framework.getBundleContext().getBundle(location);
		assertNotEquals("Wrong ID.", 5678, b.getBundleId());
		testCaps = b.adapt(BundleRevision.class).getCapabilities("test.file.path");
		assertEquals("Wrong number of test caps.", 1, testCaps.size());
		path2 = (String) testCaps.get(0).getAttributes().get("test.file.path");
		assertNotNull("No path", path2);
		operation2 = (String) testCaps.get(0).getAttributes().get("test.operation");
		assertEquals("Wrong operation", ModuleEvent.UPDATED.toString(), operation2);
		location2 = (String) testCaps.get(0).getAttributes().get("test.origin");
		assertEquals("Wrong origin", location, location2);

		setFactoryClassAdaptManifest(false);
		setFactoryClassReplaceBuilder(true);
		b.uninstall();
		installBundle();
		b = framework.getBundleContext().getBundle(location);
		assertNotEquals("Wrong ID.", 5678, b.getBundleId());
		assertNotNull("Missing test bundle.", b);
		assertEquals("Wrong BSN.", "replace", b.getSymbolicName());
		testCaps = b.adapt(BundleRevision.class).getCapabilities("replace");
		assertEquals("Wrong number of capabilities.", 1, testCaps.size());

		setFactoryClassReplaceBuilder(false);
		setFactoryClassAdaptCapabilityAttribute(true);
		b.uninstall();
		installBundle();
		b = framework.getBundleContext().getBundle(location);
		BundleCapability bundleCap = b.adapt(BundleRevision.class).getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE).iterator().next();
		assertEquals("Wrong attribute value", "testAttribute", bundleCap.getAttributes().get("matching.attribute"));
		assertEquals("Wrong attribute value", "testDirective", bundleCap.getDirectives().get("matching.directive"));
	}

	@SuppressWarnings("deprecation")
	public void testFrameworkUtilHelper() throws Exception {
		initAndStartFramework();
		Class<?> frameworkUtilClass = classLoader.loadClass("org.osgi.framework.FrameworkUtil");
		Bundle b = (Bundle) frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, String.class);
		assertEquals("Wrong bundle found.", framework.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION), b);
		PackageAdmin packageAdmin = framework.getBundleContext().getService(framework.getBundleContext().getServiceReference(PackageAdmin.class));
		b = packageAdmin.getBundle(String.class);
		assertEquals("Wrong bundle found.", framework.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION), b);
	}

	public void testHandleContent() throws Exception {
		initAndStartFramework();

		// install with an empty stream, the hook will replace it will content to a real bundle
		setFactoryClassHandleContent(true);
		Bundle b = framework.getBundleContext().installBundle("testBundle", new ByteArrayInputStream(new byte[0]));
		assertEquals("Wrong symbolicName", "testHandleContentConnection", b.getSymbolicName());
		b.uninstall();

		// install with no stream, the hook will supply the real content of the bundle
		b = framework.getBundleContext().installBundle("testBundle");
		assertEquals("Wrong symbolicName", "testHandleContentConnection", b.getSymbolicName());
		b.uninstall();

		// tell the hook to no longer handle content, the default behavior of the framework will be used
		setFactoryClassHandleContent(false);
		b = installBundle();
		assertEquals("Wrong symbolicName", "test1", b.getSymbolicName());

		// tell the hook to handle content again, update will update to the content supplied from the hook
		setFactoryClassHandleContent(true);
		b.update(new ByteArrayInputStream(new byte[0]));
		assertEquals("Wrong symbolicName", "testHandleContentConnection", b.getSymbolicName());

		// tell the hook to no longer handle content, update will go back to using content derived from the original location
		setFactoryClassHandleContent(false);
		b.update();
		assertEquals("Wrong symbolicName", "test1", b.getSymbolicName());

		// now update again with hook handling content
		setFactoryClassHandleContent(true);
		b.update();
		assertEquals("Wrong symbolicName", "testHandleContentConnection", b.getSymbolicName());
	}

	public void testNullStorageHook() throws Exception {

		initAndStartFramework();
		File bundlesBase = new File(OSGiTestsActivator.getContext().getDataFile(getName()), "bundles");
		bundlesBase.mkdirs();
		String initialBundleLoc = SystemBundleTests.createBundle(bundlesBase, getName(), false, false).toURI().toString();
		Bundle initialBundle = framework.getBundleContext().installBundle(initialBundleLoc);
		assertNotNull("Expected to have an initial bundle.", initialBundle);

		// Have storage hook factory return null StorageHook
		setFactoryNullStorageHook(true);
		Bundle b = installBundle();
		assertNotNull("Expected to have a bundle after install.", b);
		framework.stop();
		framework.waitForStop(5000);

		// create new framework to make sure null storage hook works from persistence also.
		framework = createFramework(configuration);
		framework.init();

		initialBundle = framework.getBundleContext().getBundle(initialBundleLoc);
		assertNotNull("Expected to have initial bundle after restart.", initialBundle);
		b = framework.getBundleContext().getBundle(location);
		assertNotNull("Expected to have a bundle after restart.", b);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
		resetStorageHook();
	}

	@Override
	protected void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	private void assertBundleDiscarded() {
		assertBundleDiscarded(location, framework);
	}

	private void assertBundleException(BundleException e) {
		assertThrowable(e.getCause());
	}

	private void assertBundleNotDiscarded() {
		assertBundleNotDiscarded(location, framework);
	}

	private void assertCreateStorageHookCalled() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		assertTrue("Storage hook factory createStorageHook not called by framework", clazz.getField(HOOK_CONFIGURATOR_FIELD_CREATE_STORAGE_HOOK_CALLED).getBoolean(null));
	}

	private void assertThrowable(Throwable t) {
		assertTrue("Unexpected exception", t != null && (t instanceof IllegalStateException) && t.getMessage().startsWith("The factory class "));
	}

	private void assertStorageHookValidateCalled() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		assertTrue("Storage hook validate not called by framework", clazz.getField(HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED).getBoolean(null));
	}

	private void assertStorageHookDeletingGenerationCalled() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		assertTrue("Storage hook deletingGeneration not called by framework", clazz.getField(HOOK_CONFIGURATOR_FIELD_DELETING_CALLED).getBoolean(null));
	}

	private void initAndStartFramework() throws Exception {
		initAndStart(framework);
	}

	private Bundle installBundle() throws Exception {
		return framework.getBundleContext().installBundle(location);
	}

	private void resetStorageHook() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_CREATE_STORAGE_HOOK_CALLED).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID_FACTORY_CLASS).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_DELETING_CALLED).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_ADAPT_MANIFEST).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_FAIL_LOAD).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_HANDLE_CONTENT).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_REPLACE_BUILDER).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_NULL_STORAGE_HOOK).set(null, false);
	}

	private void restartFramework() throws Exception {
		framework = restart(framework, configuration);
	}

	private void setFactoryClassInvalid(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID_FACTORY_CLASS).set(null, value);
	}

	private void setStorageHookInvalid(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID).set(null, value);
	}

	private void setFactoryClassAdaptManifest(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_ADAPT_MANIFEST).set(null, value);
	}

	private void setFactoryClassAdaptCapabilityAttribute(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_ADAPT_CAPABILITY_ATTRIBUTE).set(null, value);
	}

	private void setFactoryClassReplaceBuilder(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_REPLACE_BUILDER).set(null, value);
	}

	private void setFactoryHookFailLoad(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_FAIL_LOAD).set(null, value);
	}

	private void setFactoryClassHandleContent(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_HANDLE_CONTENT).set(null, value);
	}

	private void setFactoryNullStorageHook(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_NULL_STORAGE_HOOK).set(null, value);
	}

	private void updateBundle() throws Exception {
		framework.getBundleContext().getBundle(location).update();
	}
}
