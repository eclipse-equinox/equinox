/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader.sources;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;

public class SingleSourcePackage extends PackageSource {
	BundleLoader supplier;

	public SingleSourcePackage(String id, BundleLoader supplier) {
		super(id);
		this.supplier = supplier;
	}

	public SingleSourcePackage[] getSuppliers() {
		return new SingleSourcePackage[] {this};
	}

	public String toString() {
		return id + " -> " + supplier; //$NON-NLS-1$
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return supplier.findLocalClass(name);
	}

	public URL getResource(String name) {
		return supplier.findLocalResource(name);
	}

	public Enumeration<URL> getResources(String name) {
		return supplier.findLocalResources(name);
	}

	public boolean equals(Object source) {
		if (this == source)
			return true;
		if (!(source instanceof SingleSourcePackage))
			return false;
		SingleSourcePackage singleSource = (SingleSourcePackage) source;
		// we do an == test on id because the id is interned in the constructor of PackageSource
		return supplier == singleSource.supplier && id == singleSource.getId();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		result = prime * result + supplier.hashCode();
		return result;
	}

	@Override
	public Collection<String> listResources(String path, String filePattern) {
		ModuleClassLoader mcl = supplier.getModuleClassLoader();
		return mcl.listLocalResources(path, filePattern, 0);
	}
}
