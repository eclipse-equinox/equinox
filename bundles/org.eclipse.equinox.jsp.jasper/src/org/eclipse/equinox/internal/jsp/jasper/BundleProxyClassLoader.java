/*******************************************************************************
 * Copyright (c) 2005, 2017 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.jsp.jasper;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * A BundleProxyClassLoader wraps a bundle and uses the various Bundle methods to produce a ClassLoader. 
 */
public class BundleProxyClassLoader extends ClassLoader {
	private final Bundle activatorBundle = Activator.getBundle(Activator.class);
	private Bundle bundle;
	private ClassLoader parent;

	public BundleProxyClassLoader(Bundle bundle) {
		this.bundle = bundle;
	}

	public BundleProxyClassLoader(Bundle bundle, ClassLoader parent) {
		super(parent);
		this.parent = parent;
		this.bundle = bundle;
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		return bundle.getResources(name);
	}

	@Override
	public URL findResource(String name) {
		return bundle.getResource(name);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			if (name.startsWith("com.sun.el")) { //$NON-NLS-1$
				return activatorBundle.loadClass(name);
			}
		} catch (ClassNotFoundException ex) {
			//$FALL-THROUGH$
		}

		return bundle.loadClass(name);
	}

	@Override
	public URL getResource(String name) {
		return (parent == null) ? findResource(name) : super.getResource(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
		if (resolve)
			super.resolveClass(clazz);

		return clazz;
	}
}
