/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.internal.connect;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.EquinoxClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;

public class DelegatingConnectClassLoader extends EquinoxClassLoader {
	static {
		try {
			ClassLoader.registerAsParallelCapable();
		} catch (Throwable t) {
			// ignore any error
		}
	}
	private final ClassLoader connectClassLoader;

	public DelegatingConnectClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation, ClassLoader connectClassLoader) {
		super(parent, configuration, delegate, generation);
		this.connectClassLoader = connectClassLoader;
	}

	@Override
	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
		if (connectClassLoader == null) {
			return null;
		}
		return connectClassLoader.loadClass(classname);
	}

	@Override
	public URL findLocalResource(String resource) {
		if (connectClassLoader == null) {
			return null;
		}
		return connectClassLoader.getResource(resource);
	}

	@Override
	public Enumeration<URL> findLocalResources(String resource) {
		if (connectClassLoader == null) {
			return Collections.emptyEnumeration();
		}
		try {
			return connectClassLoader.getResources(resource);
		} catch (IOException e) {
			return Collections.emptyEnumeration();
		}
	}
}
