/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

public class ClassLoaderHookTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "substitutes.a";
	private static final String TEST_CLASSNAME = "substitutes.x.Ax";
	private static final String TEST_CLASSNAME_RESOURCE = "substitutes/x/Ax.class";
	private static final String HOOK_CONFIGURATOR_BUNDLE = "classloader.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "org.eclipse.osgi.tests.classloader.hooks.a.TestHookConfigurator";
	private static final String REJECT_PROP = "classloader.hooks.a.reject";
	private static final String BAD_TRANSFORM_PROP = "classloader.hooks.a.bad.transform";
	private static final String RECURSION_LOAD = "classloader.hooks.a.recursion.load";
	private static final String RECURSION_LOAD_SUPPORTED = "classloader.hooks.a.recursion.load.supported";
	private static final String FILTER_CLASS_PATHS = "classloader.hooks.a.filter.class.paths";
	private static final String PREVENT_RESOURCE_LOAD_PRE = "classloader.hooks.a.fail.resource.load.pre";
	private static final String PREVENT_RESOURCE_LOAD_POST = "classloader.hooks.a.fail.resource.load.post";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	protected void setUp() throws Exception {
		super.setUp();
		setRejectTransformation(false);
		setBadTransform(false);
		setRecursionLoad(false);
		setRecursionLoadSupported(false);
		setFilterClassPaths(false);
		setPreventResourceLoadPre(false);
		setPreventResourceLoadPost(false);
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		loc = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(loc));
		location = bundleInstaller.getBundleLocation(TEST_BUNDLE);
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
	}

	protected void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	private void initAndStartFramework() throws Exception {
		initAndStart(framework);
	}

	private Bundle installBundle() throws Exception {
		return framework.getBundleContext().installBundle(location);
	}

	private void setRejectTransformation(boolean value) {
		System.setProperty(REJECT_PROP, Boolean.toString(value));
	}

	private void setBadTransform(boolean value) {
		System.setProperty(BAD_TRANSFORM_PROP, Boolean.toString(value));
	}

	private void setRecursionLoad(boolean value) {
		System.setProperty(RECURSION_LOAD, Boolean.toString(value));
	}

	private void setRecursionLoadSupported(boolean value) {
		System.setProperty(RECURSION_LOAD_SUPPORTED, Boolean.toString(value));
	}

	private void setFilterClassPaths(boolean value) {
		System.setProperty(FILTER_CLASS_PATHS, Boolean.toString(value));
	}

	private void setPreventResourceLoadPre(boolean value) {
		System.setProperty(PREVENT_RESOURCE_LOAD_PRE, Boolean.toString(value));
	}

	private void setPreventResourceLoadPost(boolean value) {
		System.setProperty(PREVENT_RESOURCE_LOAD_POST, Boolean.toString(value));
	}

	public void testRejectTransformationFromWeavingHook() throws Exception {
		setRejectTransformation(true);
		initAndStartFramework();
		framework.getBundleContext().registerService(WeavingHook.class, new WeavingHook() {

			@Override
			public void weave(WovenClass wovenClass) {
				wovenClass.setBytes(new byte[] {'b', 'a', 'd', 'b', 'y', 't', 'e', 's'});
				wovenClass.getDynamicImports().add("badimport");
			}
		}, null);
		Bundle b = installBundle();
		b.loadClass(TEST_CLASSNAME);
		// class load must succeed because the badbytes got rejected
		// make sure we don't have any dynamic imports added
		assertEquals("Found some imports.", 0, b.adapt(BundleRevision.class).getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE).size());

		// no don't reject
		setRejectTransformation(false);
		refreshBundles(Collections.singleton(b));
		try {
			b.loadClass(TEST_CLASSNAME);
			fail("Expected a ClassFormatError.");
		} catch (ClassFormatError e) {
			// expected
		}
		// class load must fail because the badbytes got used to define the class
		// make sure we have a dynamic imports added
		assertEquals("Found some imports.", 1, b.adapt(BundleRevision.class).getWiring().getRequirements(PackageNamespace.PACKAGE_NAMESPACE).size());
	}

	public void testRejectTransformationFromClassLoadingHook() throws Exception {
		setRejectTransformation(true);
		setBadTransform(true);
		initAndStartFramework();
		Bundle b = installBundle();
		b.loadClass(TEST_CLASSNAME);

		// no don't reject
		setRejectTransformation(false);
		refreshBundles(Collections.singleton(b));
		try {
			b.loadClass(TEST_CLASSNAME);
			fail("Expected a ClassFormatError.");
		} catch (ClassFormatError e) {
			// expected
		}
	}

	public void testRecursionFromClassLoadingHookNotSupported() throws Exception {
		setRecursionLoad(true);
		initAndStartFramework();
		Bundle b = installBundle();
		b.loadClass(TEST_CLASSNAME);
	}

	public void testRecursionFromClassLoadingHookIsSupported() throws Exception {
		setRecursionLoad(true);
		setRecursionLoadSupported(true);
		initAndStartFramework();
		Bundle b = installBundle();
		b.loadClass(TEST_CLASSNAME);
	}

	private void refreshBundles(Collection<Bundle> bundles) throws InterruptedException {
		final CountDownLatch refreshSignal = new CountDownLatch(1);
		framework.adapt(FrameworkWiring.class).refreshBundles(bundles, new FrameworkListener() {

			@Override
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
					refreshSignal.countDown();
				}
			}
		});
		refreshSignal.await(30, TimeUnit.SECONDS);
	}

	public void testFilterClassPaths() throws Exception {
		setFilterClassPaths(false);
		initAndStartFramework();
		Bundle b = installBundle();
		b.loadClass(TEST_CLASSNAME);

		setFilterClassPaths(true);
		refreshBundles(Collections.singleton(b));
		try {
			b.loadClass(TEST_CLASSNAME);
			fail("Expected a ClassNotFoundException.");
		} catch (ClassNotFoundException e) {
			// expected
		}
	}

	public void testPreventResourceLoadFromClassLoadingHook() throws Exception {
		setPreventResourceLoadPre(false);
		setPreventResourceLoadPost(false);
		initAndStartFramework();
		Bundle b = installBundle();
		URL resource = b.getResource(TEST_CLASSNAME_RESOURCE);
		assertNotNull("Could not find resource.", resource);

		setPreventResourceLoadPre(true);
		resource = b.getResource(TEST_CLASSNAME_RESOURCE);
		assertNull("Could find resource.", resource);

		setPreventResourceLoadPre(false);
		setPreventResourceLoadPost(true);
		resource = b.getResource(TEST_CLASSNAME_RESOURCE);
		assertNull("Could find resource.", resource);
	}
}
