package org.eclipse.osgi.framework.internal.core;

public class MultiSourcePackage extends PackageSource {
	BundleLoaderProxy[] suppliers;
		
	MultiSourcePackage(String id, BundleLoaderProxy[] suppliers) {
		this.id= id;
		this.suppliers= suppliers;
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
