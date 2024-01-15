/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.internal.loader.sources;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to optimize finding provided-packages for a bundle. If the
 * package cannot be found in a list of required bundles then this class is used
 * to cache a null package source so that the search does not need to be done
 * again.
 */
public class NullPackageSource extends PackageSource {
	static Map<String, NullPackageSource> sources = new HashMap<>();

	private NullPackageSource(String name) {
		super(name);
	}

	@Override
	public SingleSourcePackage[] getSuppliers() {
		return null;
	}

	@Override
	public boolean isNullSource() {
		return true;
	}

	@Override
	public Class<?> loadClass(String name) {
		return null;
	}

	@Override
	public URL getResource(String name) {
		return null;
	}

	@Override
	public Enumeration<URL> getResources(String name) {
		return null;
	}

	public static synchronized NullPackageSource getNullPackageSource(String name) {
		NullPackageSource result = sources.get(name);
		if (result != null)
			return result;
		result = new NullPackageSource(name);
		sources.put(name, result);
		return result;
	}

	@Override
	public List<String> listResources(String path, String filePattern) {
		return Collections.emptyList();
	}
}
