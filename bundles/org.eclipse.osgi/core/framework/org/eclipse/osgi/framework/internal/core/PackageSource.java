package org.eclipse.osgi.framework.internal.core;

public abstract class PackageSource implements KeyedElement {
	protected String id;
	
	public String getId() {
		return id;
	}
	public abstract boolean isMultivalued();
	public abstract BundleLoaderProxy getSupplier();
	public abstract BundleLoaderProxy[] getSuppliers();

	public boolean compare(KeyedElement other) {
		return id.equals(((PackageSource)other).getId());
	}
	public int getKeyHashCode() {
		return id.hashCode();
	}
	public Object getKey() {
		return id;
	}
	public boolean isNullSource(){
		return false;
	}
}
