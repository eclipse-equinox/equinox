/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.osgi.internal.loader.BundleLoader;

public class MultiSourcePackage extends PackageSource {
	private final SingleSourcePackage[] suppliers;

	public MultiSourcePackage(String id, SingleSourcePackage[] suppliers) {
		super(id);
		this.suppliers = suppliers;
	}

	@Override
	public SingleSourcePackage[] getSuppliers() {
		return suppliers;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> result = null;
		for (int i = 0; i < suppliers.length; i++) {
			result = suppliers[i].loadClass(name);
			if (result != null)
				return result;
		}
		return result;
	}

	@Override
	public URL getResource(String name) {
		URL result = null;
		for (int i = 0; i < suppliers.length; i++) {
			result = suppliers[i].getResource(name);
			if (result != null)
				return result;
		}
		return result;
	}

	@Override
	public Enumeration<URL> getResources(String name) {
		Enumeration<URL> results = null;
		for (int i = 0; i < suppliers.length; i++)
			results = BundleLoader.compoundEnumerations(results, suppliers[i].getResources(name));
		return results;
	}

	@Override
	public Collection<String> listResources(String path, String filePattern) {
		List<String> result = new ArrayList<>();
		for (SingleSourcePackage source : suppliers) {
			Collection<String> sourceResources = source.listResources(path, filePattern);
			for (String resource : sourceResources) {
				if (!result.contains(resource))
					result.add(resource);
			}
		}
		return result;
	}
}
