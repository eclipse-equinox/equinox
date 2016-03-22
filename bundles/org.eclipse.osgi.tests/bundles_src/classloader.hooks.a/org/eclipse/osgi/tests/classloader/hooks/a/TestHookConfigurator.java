/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.classloader.hooks.a;

import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

public class TestHookConfigurator implements HookConfigurator {
	private static final String REJECT_PROP = "classloader.hooks.a.reject";
	private static final String BAD_TRANSFORM_PROP = "classloader.hooks.a.bad.transform";

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
				return null;
			}

		});
	}
}
