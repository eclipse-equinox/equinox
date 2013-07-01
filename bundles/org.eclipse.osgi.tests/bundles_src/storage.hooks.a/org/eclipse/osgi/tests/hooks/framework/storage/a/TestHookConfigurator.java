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
package org.eclipse.osgi.tests.hooks.framework.storage.a;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Dictionary;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.storage.BundleInfo.Generation;

public class TestHookConfigurator implements HookConfigurator {
	private static class TestStorageHookFactory extends StorageHookFactory<Object, Object, TestStorageHookFactory.TestStorageHook> {
		private static class TestStorageHook extends StorageHookFactory.StorageHook<Object, Object> {
			public TestStorageHook(Generation generation, Class clazz) {
				super(generation, clazz);
			}

			@Override
			public void initialize(Dictionary manifest) {
				// Nothing.
			}

			@Override
			public void load(Object loadContext, DataInputStream is) {
				// Nothing.
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
	public static volatile boolean invalidFactoryClass;
	public static volatile boolean validateCalled;

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addStorageHookFactory(new TestStorageHookFactory());
	}
}
