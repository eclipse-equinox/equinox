/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

public class SingleSourcePackage extends PackageSource {
	BundleLoaderProxy supplier;

	public SingleSourcePackage(String name, BundleLoaderProxy supplier) {
		this.id = name;
		this.supplier = supplier;
	}

	public BundleLoaderProxy getSupplier() {
		return supplier;
	}

	public boolean isMultivalued() {
		return false;
	}

	public BundleLoaderProxy[] getSuppliers() {
		return new BundleLoaderProxy[] {supplier};
	}

	public String toString() {
		return id + " -> " + supplier;
	}
}