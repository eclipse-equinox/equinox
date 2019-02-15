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
package org.eclipse.osgi.tests.classloader.hooks.a;

import java.net.URL;
import java.util.NoSuchElementException;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

public class TestHookConfigurator implements HookConfigurator {
	private static final String REJECT_PROP = "classloader.hooks.a.reject";
	private static final String BAD_TRANSFORM_PROP = "classloader.hooks.a.bad.transform";
	private static final String RECURSION_LOAD = "classloader.hooks.a.recursion.load";
	private static final String RECURSION_LOAD_SUPPORTED = "classloader.hooks.a.recursion.load.supported";
	private static final String FILTER_CLASS_PATHS = "classloader.hooks.a.filter.class.paths";
	private static final String PREVENT_RESOURCE_LOAD_PRE = "classloader.hooks.a.fail.resource.load.pre";
	private static final String PREVENT_RESOURCE_LOAD_POST = "classloader.hooks.a.fail.resource.load.post";
	final ThreadLocal<Boolean> doingRecursionLoad = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addClassLoaderHook(new ClassLoaderHook() {

			@Override
			public boolean rejectTransformation(String name, byte[] transformedBytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
				return Boolean.getBoolean(REJECT_PROP);
			}

			@Override
			public byte[] processClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
				if (Boolean.getBoolean(BAD_TRANSFORM_PROP)) {
					return new byte[] {'b', 'a', 'd', 'b', 'y', 't', 'e', 's'};
				}
				if (Boolean.getBoolean(RECURSION_LOAD)) {
					if (isProcessClassRecursionSupported() && doingRecursionLoad.get()) {
						return null;
					}
					Module m = manager.getGeneration().getBundleInfo().getStorage().getModuleContainer().getModule(1);
					doingRecursionLoad.set(true);
					try {
						m.getCurrentRevision().getWiring().getClassLoader().loadClass("substitutes.x.Ax");
						if (!isProcessClassRecursionSupported()) {
							throw new LinkageError("Recursion is no supported.");
						}
					} catch (ClassNotFoundException e) {
						if (isProcessClassRecursionSupported()) {
							throw new LinkageError("Recursion should be supported.");
						}
						// expected
					} finally {
						doingRecursionLoad.set(false);
					}
				}
				return null;
			}

			@Override
			public boolean isProcessClassRecursionSupported() {
				return Boolean.getBoolean(RECURSION_LOAD_SUPPORTED);
			}

			@Override
			public ClasspathEntry[] getClassPathEntries(String name, ClasspathManager manager) {
				if (Boolean.getBoolean(FILTER_CLASS_PATHS)) {
					return new ClasspathEntry[0];
				}
				return super.getClassPathEntries(name, manager);
			}

			@Override
			public void preFindLocalResource(String name, ClasspathManager manager) {
				if (Boolean.getBoolean(PREVENT_RESOURCE_LOAD_PRE)) {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void postFindLocalResource(String name, URL resource, ClasspathManager manager) {
				if (Boolean.getBoolean(PREVENT_RESOURCE_LOAD_POST)) {
					throw new NoSuchElementException();
				}
			}
		});
	}
}
