package org.eclipse.osgi.framework.internal.core;

/**
 * This class is used to optimize finding provided-packages for a bundle.
 * If the package cannot be found in a list of required bundles then this class
 * is used to cache a null package source so that the search does not need to
 * be done again.
 */
public class NullPackageSource extends PackageSource {
	public NullPackageSource(String name) {
		this.id = name;
	}
	public BundleLoaderProxy getSupplier() {
		return null;
	}
	public boolean isMultivalued() {
		return false;
	}
	public BundleLoaderProxy[] getSuppliers() {
		return null;
	}
	public boolean isNullSource(){
		return true;
	}
	public String toString() {
		return id + " -> null";
	}
}
