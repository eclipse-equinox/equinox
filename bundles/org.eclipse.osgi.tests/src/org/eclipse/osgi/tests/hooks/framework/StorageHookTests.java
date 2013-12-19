/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.hooks.framework;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

public class StorageHookTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "test";
	private static final String HOOK_CONFIGURATOR_BUNDLE = "storage.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "org.eclipse.osgi.tests.hooks.framework.storage.a.TestHookConfigurator";
	private static final String HOOK_CONFIGURATOR_FIELD_CREATE_STORAGE_HOOK_CALLED = "createStorageHookCalled";
	private static final String HOOK_CONFIGURATOR_FIELD_INVALID = "invalid";
	private static final String HOOK_CONFIGURATOR_FIELD_INVALID_FACTORY_CLASS = "invalidFactoryClass";
	private static final String HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED = "validateCalled";
	private static final String HOOK_CONFIGURATOR_FIELD_DELETING_CALLED = "deletingGenerationCalled";

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

	private void installBundle() throws Exception {
		framework.getBundleContext().installBundle(location);
	}

	private void resetStorageHook() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_CREATE_STORAGE_HOOK_CALLED).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID_FACTORY_CLASS).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_DELETING_CALLED).set(null, false);
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

	private void updateBundle() throws Exception {
		framework.getBundleContext().getBundle(location).update();
	}
}
