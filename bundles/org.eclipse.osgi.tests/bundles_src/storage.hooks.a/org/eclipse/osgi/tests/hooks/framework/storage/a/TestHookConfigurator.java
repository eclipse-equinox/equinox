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
package org.eclipse.osgi.tests.hooks.framework.storage.a;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;

public class TestHookConfigurator implements HookConfigurator {
	private static class TestStorageHookFactory extends StorageHookFactory<Object, Object, TestStorageHookFactory.TestStorageHook> {
		private static class TestStorageHook extends StorageHookFactory.StorageHook<Object, Object> {
			private static AtomicInteger adaptCount = new AtomicInteger(1);

			public TestStorageHook(Generation generation, Class clazz) {
				super(generation, clazz);
			}

			@Override
			public void initialize(Dictionary manifest) {
				// Nothing
			}

			@Override
			public void load(Object loadContext, DataInputStream is) {
				if (TestHookConfigurator.failLoad) {
					// will force a clean
					throw new IllegalArgumentException();
				}
			}

			@Override
			public void save(Object saveContext, DataOutputStream os) {
				// Nothing.
			}

			@Override
			public void validate() throws IllegalStateException {
				TestHookConfigurator.validateCalled = true;
				if (TestHookConfigurator.invalid)
					throw new IllegalStateException();
			}

			@Override
			public void deletingGeneration() {
				TestHookConfigurator.deletingGenerationCalled = true;
			}

			@Override
			public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin, ModuleRevisionBuilder builder) {
				if (TestHookConfigurator.replaceModuleBuilder) {
					ModuleRevisionBuilder replace = new ModuleRevisionBuilder();
					// try setting the ID to something which is checked during the test
					replace.setId(5678);
					replace.setSymbolicName("replace");
					replace.setVersion(Version.parseVersion("1.1.1"));
					replace.addCapability("replace", Collections.<String, String> emptyMap(), Collections.<String, Object> emptyMap());
					replace.addCapability(IdentityNamespace.IDENTITY_NAMESPACE, Collections.<String, String> emptyMap(), Collections.<String, Object> singletonMap(IdentityNamespace.IDENTITY_NAMESPACE, "replace"));
					replace.addCapability(BundleNamespace.BUNDLE_NAMESPACE, Collections.<String, String> emptyMap(), Collections.<String, Object> singletonMap(BundleNamespace.BUNDLE_NAMESPACE, "replace"));
					return replace;
				}
				if (TestHookConfigurator.adaptManifest) {
					// try setting the ID to something which is checked during the test
					builder.setId(5678);
					Map<String, String> dirs = Collections.emptyMap();
					Map<String, Object> attrs = new HashMap<String, Object>();
					attrs.put("test.file.path", getGeneration().getContent().getPath() + " - " + adaptCount.getAndIncrement());
					attrs.put("test.operation", operation.toString());
					attrs.put("test.origin", origin.getLocation());
					builder.addCapability("test.file.path", dirs, attrs);
				}
				return builder;
			}
		}

		public TestStorageHookFactory() {
		}

		@Override
		public int getStorageVersion() {
			return 0;
		}

		@Override
		protected TestStorageHook createStorageHook(Generation generation) {
			createStorageHookCalled = true;
			Class<?> factoryClass = TestStorageHookFactory.class;
			if (invalidFactoryClass)
				factoryClass = StorageHookFactory.class;
			return new TestStorageHook(generation, factoryClass);
		}
	}

	public static volatile boolean createStorageHookCalled;
	public static volatile boolean invalid;
	public static volatile boolean failLoad;
	public static volatile boolean invalidFactoryClass;
	public static volatile boolean validateCalled;
	public static volatile boolean deletingGenerationCalled;
	public static volatile boolean adaptManifest;
	public static volatile boolean replaceModuleBuilder;

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addStorageHookFactory(new TestStorageHookFactory());
		hookRegistry.addActivatorHookFactory(new ActivatorHookFactory() {

			@Override
			public BundleActivator createActivator() {
				return new BundleActivator() {

					@Override
					public void start(BundleContext context) throws Exception {
						TestHelper.testBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
					}

					@Override
					public void stop(BundleContext context) throws Exception {
						// nothing
					}
				};
			}
		});
	}
}
