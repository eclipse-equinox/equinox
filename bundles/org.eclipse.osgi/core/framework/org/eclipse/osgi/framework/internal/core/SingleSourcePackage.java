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
		return new BundleLoaderProxy[] { supplier };
	}
	
	public String toString() {
		return id + " -> " + supplier;
	}
}
