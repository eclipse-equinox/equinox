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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

public class ClassLoaderHookTests extends AbstractFrameworkHookTests {
	private static final String TEST_BUNDLE = "substitutes.a";
	private static final String TEST_CLASSNAME = "substitutes.x.Ax";
	private static final String HOOK_CONFIGURATOR_BUNDLE = "classloader.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "classloader.hooks.a.TestHookConfigurator";
	private static final String REJECT_PROP = "classloader.hooks.a.reject";
	private static final String BAD_TRANSFORM_PROP = "classloader.hooks.a.bad.transform";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	protected void setUp() throws Exception {
		super.setUp();
		setRejectTransformation(false);
		setBadTransform(false);
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

	private void setRejectTransformation(boolean value) throws Exception {
		System.setProperty(REJECT_PROP, Boolean.toString(value));
	}

	private void setBadTransform(boolean value) {
		System.setProperty(BAD_TRANSFORM_PROP, Boolean.toString(value));
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
		} catch (ClassFormatError e) {
			// expected
		}
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
}
