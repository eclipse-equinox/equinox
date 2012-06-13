/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader;

import java.net.URL;
import java.util.*;

public class MultiSourcePackage extends PackageSource {
	SingleSourcePackage[] suppliers;

	MultiSourcePackage(String id, SingleSourcePackage[] suppliers) {
		super(id);
		this.suppliers = suppliers;
	}

	public SingleSourcePackage[] getSuppliers() {
		return suppliers;
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> result = null;
		for (int i = 0; i < suppliers.length; i++) {
			result = suppliers[i].loadClass(name);
			if (result != null)
				return result;
		}
		return result;
	}

	public URL getResource(String name) {
		URL result = null;
		for (int i = 0; i < suppliers.length; i++) {
			result = suppliers[i].getResource(name);
			if (result != null)
				return result;
		}
		return result;
	}

	public Enumeration<URL> getResources(String name) {
		Enumeration<URL> results = null;
		for (int i = 0; i < suppliers.length; i++)
			results = BundleLoader.compoundEnumerations(results, suppliers[i].getResources(name));
		return results;
	}

	@Override
	public Collection<String> listResources(String path, String filePattern) {
		List<String> result = new ArrayList<String>();
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
