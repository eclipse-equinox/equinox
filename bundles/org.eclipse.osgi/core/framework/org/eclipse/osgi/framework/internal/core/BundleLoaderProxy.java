package org.eclipse.osgi.framework.internal.core;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.PackageSpecification;

public class BundleLoaderProxy implements KeyedElement {
	private BundleLoader loader;
	private BundleHost bundle;
	private String uniqueId;
	private String key;
	private boolean stale = false;
	private KeyedHashSet users;
	protected boolean markedUsedDependencies = false;

	public BundleLoaderProxy(BundleHost bundle) {
		this.bundle = bundle;
		this.uniqueId = bundle.getGlobalName();
		if (this.uniqueId == null) {
			this.uniqueId = new StringBuffer().append(bundle.id).append("NOUNIQUEID").toString();
		}
		this.key = new StringBuffer(uniqueId).append("_").append(bundle.getVersion().toString()).toString();
		this.users = new KeyedHashSet(false);
	}
	public BundleLoader getBundleLoader() {
		if (loader == null)
			loader = bundle.getBundleLoader();
		return loader;
	}
	public Bundle getBundle() {
		return bundle;
	}

	public void setBundleLoader(BundleLoader value) {
		loader = value;
	}

	public void markUsed(BundleLoaderProxy user){
		// only mark as used if the user is not our own bundle.
		if (user.getBundle() != bundle) {
			users.add(user);
		}
	}

	public void unMarkUsed(BundleLoaderProxy user){
		users.removeByKey(user.getKey());
	}

	public int getKeyHashCode() {
		return key.hashCode();
	}

	public boolean compare(KeyedElement other) {
		if (!(other instanceof BundleLoaderProxy))
			return false;
		BundleLoaderProxy otherLoaderProxy = (BundleLoaderProxy) other;
		return (uniqueId.equals(otherLoaderProxy.uniqueId) && 
				bundle.getVersion().isPerfect(otherLoaderProxy.bundle.getVersion()));
	}

	public Object getKey() {
		return key;
	}

	public void setStale() {
		stale = true;
	}
	public boolean isStale() {
		return stale;
	}

	public boolean inUse(){
		return (users.size() > 0);
	}

	public Bundle[] getDependentBundles() {
		KeyedElement[] proxyLoaders = users.elements();
		KeyedHashSet bundles = new KeyedHashSet(proxyLoaders.length,false);
		for (int i=0; i<proxyLoaders.length; i++) {
			BundleLoaderProxy loaderProxy = (BundleLoaderProxy) proxyLoaders[i];
			bundles.add(loaderProxy.getBundle());
		}

		KeyedElement[] elements = bundles.elements();
		Bundle[] result = new Bundle[elements.length];
		System.arraycopy(elements,0,result,0,elements.length);

		return result;
	}

	public String toString() {
		return bundle.getLocation();
	}

	protected void markDependencies(){
		if (markedUsedDependencies || !bundle.isResolved()) {
			return;
		}
		markedUsedDependencies = true;

		BundleDescription bundleDes = bundle.getBundleDescription();
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if (bundleDes == null) {
				Debug.println("Bundle.resolved called and getBundleDescription returned null: " + this);
				Debug.printStackTrace(new Exception("Stack trace"));
			}
		}

		PackageSpecification[] packages = bundleDes.getPackages();
		BundleSpecification[] requiredBundles = bundleDes.getRequiredBundles();
		BundleDescription[] fragDescriptions = bundleDes.getFragments();

		markUsedPackages(packages);
		markUsedBundles(requiredBundles);

		for (int i=0; i<fragDescriptions.length; i++) {
			if (fragDescriptions[i].isResolved()) {
				markUsedPackages(fragDescriptions[i].getPackages());
				markUsedBundles(fragDescriptions[i].getRequiredBundles());
			}
		}

		// besure to create the BundleLoader;
		getBundleLoader();
	}

	private void markUsedPackages(PackageSpecification[] packages) {
		if (packages!=null) {
			for (int i=0; i<packages.length; i++) {
				SingleSourcePackage packagesource = 
				(SingleSourcePackage) bundle.framework.packageAdmin.exportedPackages.getByKey(packages[i].getName());
				if (packagesource != null) {
					packagesource.getSupplier().markUsed(this);
				}
			}
		}
	}

	private void markUsedBundles(BundleSpecification[] requiredBundles) {
		if (requiredBundles != null) {
			for (int i=0; i<requiredBundles.length; i++) {
				if (requiredBundles[i].isResolved()) {
					String bundleKey = new StringBuffer(requiredBundles[i].getName()).append("_").append(requiredBundles[i].getActualVersion().toString()).toString();
					
					BundleLoaderProxy loaderProxy = 
					(BundleLoaderProxy) bundle.framework.packageAdmin.exportedBundles.getByKey(bundleKey);
					if (loaderProxy != null) {
						loaderProxy.markUsed(this);
					}
				}
			}
		}
	}
}
