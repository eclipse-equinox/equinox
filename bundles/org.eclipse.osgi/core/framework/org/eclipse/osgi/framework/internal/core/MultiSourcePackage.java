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

public class MultiSourcePackage extends PackageSource {
	BundleLoaderProxy[] suppliers;

	MultiSourcePackage(String id, BundleLoaderProxy[] suppliers) {
		this.id = id;
		this.suppliers = suppliers;
	}

	public BundleLoaderProxy[] getSuppliers() {
		return suppliers;
	}

	public boolean isMultivalued() {
		return true;
	}

	public BundleLoaderProxy getSupplier() {
		return null;
	}

}
