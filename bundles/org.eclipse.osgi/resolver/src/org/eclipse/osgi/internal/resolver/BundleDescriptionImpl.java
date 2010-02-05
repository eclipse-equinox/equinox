/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522 and 255520)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.IOException;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.service.resolver.*;

public final class BundleDescriptionImpl extends BaseDescriptionImpl implements BundleDescription, KeyedElement {
	static final String[] EMPTY_STRING = new String[0];
	static final ImportPackageSpecification[] EMPTY_IMPORTS = new ImportPackageSpecification[0];
	static final BundleSpecification[] EMPTY_BUNDLESPECS = new BundleSpecification[0];
	static final ExportPackageDescription[] EMPTY_EXPORTS = new ExportPackageDescription[0];
	static final BundleDescription[] EMPTY_BUNDLEDESCS = new BundleDescription[0];
	static final GenericSpecification[] EMPTY_GENERICSPECS = new GenericSpecification[0];
	static final GenericDescription[] EMPTY_GENERICDESCS = new GenericDescription[0];

	static final int RESOLVED = 0x01;
	static final int SINGLETON = 0x02;
	static final int REMOVAL_PENDING = 0x04;
	static final int FULLY_LOADED = 0x08;
	static final int LAZY_LOADED = 0x10;
	static final int HAS_DYNAMICIMPORT = 0x20;
	static final int ATTACH_FRAGMENTS = 0x40;
	static final int DYNAMIC_FRAGMENTS = 0x80;

	// set to fully loaded and allow dynamic fragments by default
	private volatile int stateBits = FULLY_LOADED | ATTACH_FRAGMENTS | DYNAMIC_FRAGMENTS;

	private volatile long bundleId = -1;
	private volatile HostSpecification host; //null if the bundle is not a fragment. volatile to allow unsynchronized checks for null
	private volatile StateImpl containingState;

	private volatile Object userObject;
	private volatile int lazyDataOffset = -1;
	private volatile int lazyDataSize = -1;

	//TODO These could be arrays
	private ArrayList dependencies;
	private ArrayList dependents;

	private volatile LazyData lazyData;
	private volatile int equinox_ee = -1;

	public BundleDescriptionImpl() {
		// 
	}

	public long getBundleId() {
		return bundleId;
	}

	public String getSymbolicName() {
		return getName();
	}

	public BundleDescription getSupplier() {
		return this;
	}

	public String getLocation() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.location;
		}
	}

	public String getPlatformFilter() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.platformFilter;
		}
	}

	public String[] getExecutionEnvironments() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.executionEnvironments == null)
				return EMPTY_STRING;
			return currentData.executionEnvironments;
		}
	}

	public ImportPackageSpecification[] getImportPackages() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.importPackages == null)
				return EMPTY_IMPORTS;
			return currentData.importPackages;
		}
	}

	public BundleSpecification[] getRequiredBundles() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.requiredBundles == null)
				return EMPTY_BUNDLESPECS;
			return currentData.requiredBundles;
		}
	}

	public GenericSpecification[] getGenericRequires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.genericRequires == null)
				return EMPTY_GENERICSPECS;
			return currentData.genericRequires;
		}
	}

	public GenericDescription[] getGenericCapabilities() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.genericCapabilities == null)
				return EMPTY_GENERICDESCS;
			return currentData.genericCapabilities;
		}
	}

	public NativeCodeSpecification getNativeCodeSpecification() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.nativeCode;
		}
	}

	public ExportPackageDescription[] getExportPackages() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.exportPackages == null ? EMPTY_EXPORTS : currentData.exportPackages;
		}
	}

	public boolean isResolved() {
		return (stateBits & RESOLVED) != 0;
	}

	public State getContainingState() {
		return containingState;
	}

	public BundleDescription[] getFragments() {
		if (host != null)
			return EMPTY_BUNDLEDESCS;
		StateImpl currentState = (StateImpl) getContainingState();
		if (currentState == null)
			throw new IllegalStateException("BundleDescription does not belong to a state."); //$NON-NLS-1$
		return currentState.getFragments(this);
	}

	public HostSpecification getHost() {
		return host;
	}

	public boolean isSingleton() {
		return (stateBits & SINGLETON) != 0;
	}

	public boolean isRemovalPending() {
		return (stateBits & REMOVAL_PENDING) != 0;
	}

	public boolean hasDynamicImports() {
		return (stateBits & HAS_DYNAMICIMPORT) != 0;
	}

	public boolean attachFragments() {
		return (stateBits & ATTACH_FRAGMENTS) != 0;
	}

	public boolean dynamicFragments() {
		return (stateBits & DYNAMIC_FRAGMENTS) != 0;
	}

	public ExportPackageDescription[] getSelectedExports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.selectedExports == null)
				return EMPTY_EXPORTS;
			return currentData.selectedExports;
		}
	}

	public ExportPackageDescription[] getSubstitutedExports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.substitutedExports == null)
				return EMPTY_EXPORTS;
			return currentData.substitutedExports;
		}
	}

	public BundleDescription[] getResolvedRequires() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.resolvedRequires == null)
				return EMPTY_BUNDLEDESCS;
			return currentData.resolvedRequires;
		}
	}

	public ExportPackageDescription[] getResolvedImports() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			if (currentData.resolvedImports == null)
				return EMPTY_EXPORTS;
			return currentData.resolvedImports;
		}
	}

	protected void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	protected void setSymbolicName(String symbolicName) {
		setName(symbolicName);
	}

	protected void setLocation(String location) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.location = location;
		}
	}

	protected void setPlatformFilter(String platformFilter) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.platformFilter = platformFilter;
		}
	}

	protected void setExecutionEnvironments(String[] executionEnvironments) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.executionEnvironments = executionEnvironments;
		}
	}

	protected void setExportPackages(ExportPackageDescription[] exportPackages) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.exportPackages = exportPackages;
			if (exportPackages != null) {
				for (int i = 0; i < exportPackages.length; i++) {
					((ExportPackageDescriptionImpl) exportPackages[i]).setExporter(this);
				}
			}
		}
	}

	protected void setImportPackages(ImportPackageSpecification[] importPackages) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.importPackages = importPackages;
			if (importPackages != null) {
				for (int i = 0; i < importPackages.length; i++) {
					((ImportPackageSpecificationImpl) importPackages[i]).setBundle(this);
					if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(importPackages[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))
						stateBits |= HAS_DYNAMICIMPORT;
				}
			}
		}
	}

	protected void setRequiredBundles(BundleSpecification[] requiredBundles) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.requiredBundles = requiredBundles;
			if (requiredBundles != null)
				for (int i = 0; i < requiredBundles.length; i++) {
					((VersionConstraintImpl) requiredBundles[i]).setBundle(this);
				}
		}
	}

	protected void setGenericCapabilities(GenericDescription[] genericCapabilities) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.genericCapabilities = genericCapabilities;
			if (genericCapabilities != null)
				for (int i = 0; i < genericCapabilities.length; i++)
					((GenericDescriptionImpl) genericCapabilities[i]).setSupplier(this);
		}
	}

	protected void setGenericRequires(GenericSpecification[] genericRequires) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.genericRequires = genericRequires;
			if (genericRequires != null)
				for (int i = 0; i < genericRequires.length; i++)
					((VersionConstraintImpl) genericRequires[i]).setBundle(this);
		}
	}

	protected void setNativeCodeSpecification(NativeCodeSpecification nativeCode) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.nativeCode = nativeCode;
			if (nativeCode != null) {
				((NativeCodeSpecificationImpl) nativeCode).setBundle(this);
				NativeCodeDescription[] suppliers = nativeCode.getPossibleSuppliers();
				if (suppliers != null)
					for (int i = 0; i < suppliers.length; i++)
						((NativeCodeDescriptionImpl) suppliers[i]).setSupplier(this);
			}
		}
	}

	protected int getStateBits() {
		return stateBits;
	}

	protected void setStateBit(int stateBit, boolean on) {
		synchronized (this.monitor) {
			if (on)
				stateBits |= stateBit;
			else
				stateBits &= ~stateBit;
		}
	}

	protected void setContainingState(State value) {
		synchronized (this.monitor) {
			containingState = (StateImpl) value;
			if (containingState != null && containingState.getReader() != null) {
				if (containingState.getReader().isLazyLoaded())
					stateBits |= LAZY_LOADED;
				else
					stateBits &= ~LAZY_LOADED;
			} else {
				stateBits &= ~LAZY_LOADED;
			}
		}
	}

	protected void setHost(HostSpecification host) {
		synchronized (this.monitor) {
			this.host = host;
			if (host != null) {
				((VersionConstraintImpl) host).setBundle(this);
			}
		}
	}

	protected void setLazyLoaded(boolean lazyLoad) {
		loadLazyData();
		synchronized (this.monitor) {
			if (lazyLoad)
				stateBits |= LAZY_LOADED;
			else
				stateBits &= ~LAZY_LOADED;
		}
	}

	protected void setSelectedExports(ExportPackageDescription[] selectedExports) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.selectedExports = selectedExports;
			if (selectedExports != null) {
				for (int i = 0; i < selectedExports.length; i++) {
					((ExportPackageDescriptionImpl) selectedExports[i]).setExporter(this);
				}
			}
		}
	}

	protected void setSubstitutedExports(ExportPackageDescription[] substitutedExports) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.substitutedExports = substitutedExports;
		}
	}

	protected void setResolvedImports(ExportPackageDescription[] resolvedImports) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.resolvedImports = resolvedImports;
		}
	}

	protected void setResolvedRequires(BundleDescription[] resolvedRequires) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.resolvedRequires = resolvedRequires;
		}
	}

	public String toString() {
		if (getSymbolicName() == null)
			return "[" + getBundleId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		return getSymbolicName() + "_" + getVersion(); //$NON-NLS-1$
	}

	public Object getKey() {
		return new Long(bundleId);
	}

	public boolean compare(KeyedElement other) {
		if (!(other instanceof BundleDescriptionImpl))
			return false;
		BundleDescriptionImpl otherBundleDescription = (BundleDescriptionImpl) other;
		return bundleId == otherBundleDescription.bundleId;
	}

	public int getKeyHashCode() {
		return (int) (bundleId ^ (bundleId >>> 32));
	}

	/* TODO Determine if we need more than just Object ID type of hashcode.
	 public int hashCode() {
	 if (getSymbolicName() == null)
	 return (int) (bundleId % Integer.MAX_VALUE);
	 return (int) ((bundleId * (getSymbolicName().hashCode())) % Integer.MAX_VALUE);
	 }
	 */

	protected void removeDependencies() {
		synchronized (this.monitor) {
			if (dependencies == null)
				return;
			Iterator iter = dependencies.iterator();
			while (iter.hasNext()) {
				((BundleDescriptionImpl) iter.next()).removeDependent(this);
			}
			dependencies = null;
		}
	}

	protected void addDependencies(BaseDescription[] newDependencies, boolean checkDups) {
		synchronized (this.monitor) {
			if (newDependencies == null)
				return;
			if (!checkDups && dependencies == null)
				dependencies = new ArrayList(newDependencies.length);
			for (int i = 0; i < newDependencies.length; i++) {
				addDependency((BaseDescriptionImpl) newDependencies[i], checkDups);
			}
		}
	}

	protected void addDependency(BaseDescriptionImpl dependency, boolean checkDups) {
		synchronized (this.monitor) {
			BundleDescriptionImpl bundle = (BundleDescriptionImpl) dependency.getSupplier();
			if (bundle == this)
				return;
			if (dependencies == null)
				dependencies = new ArrayList(10);
			if (!checkDups || !dependencies.contains(bundle)) {
				bundle.addDependent(this);
				dependencies.add(bundle);
			}
		}
	}

	/*
	 * Gets all the bundle dependencies as a result of import-package or require-bundle.
	 * Self and fragment bundles are removed.
	 */
	List getBundleDependencies() {
		synchronized (this.monitor) {
			if (dependencies == null)
				return new ArrayList(0);
			ArrayList required = new ArrayList(dependencies.size());
			for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
				Object dep = iter.next();
				if (dep != this && dep instanceof BundleDescription && ((BundleDescription) dep).getHost() == null)
					required.add(dep);
			}
			return required;
		}
	}

	public Object getUserObject() {
		return userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	protected void addDependent(BundleDescription dependent) {
		synchronized (this.monitor) {
			if (dependents == null)
				dependents = new ArrayList(10);
			// no need to check for duplicates here; this is only called in addDepenency which already checks for dups.
			dependents.add(dependent);
		}
	}

	protected void removeDependent(BundleDescription dependent) {
		synchronized (this.monitor) {
			if (dependents == null)
				return;
			dependents.remove(dependent);
		}
	}

	public BundleDescription[] getDependents() {
		synchronized (this.monitor) {
			if (dependents == null)
				return EMPTY_BUNDLEDESCS;
			return (BundleDescription[]) dependents.toArray(new BundleDescription[dependents.size()]);
		}
	}

	void setFullyLoaded(boolean fullyLoaded) {
		synchronized (this.monitor) {
			if (fullyLoaded) {
				stateBits |= FULLY_LOADED;
			} else {
				stateBits &= ~FULLY_LOADED;
			}
		}
	}

	boolean isFullyLoaded() {
		return (stateBits & FULLY_LOADED) != 0;
	}

	void setLazyDataOffset(int lazyDataOffset) {
		this.lazyDataOffset = lazyDataOffset;
	}

	int getLazyDataOffset() {
		return this.lazyDataOffset;
	}

	void setLazyDataSize(int lazyDataSize) {
		this.lazyDataSize = lazyDataSize;
	}

	int getLazyDataSize() {
		return this.lazyDataSize;
	}

	// DO NOT call while holding this.monitor
	private LazyData loadLazyData() {
		// TODO add back if ee min 1.2 adds holdsLock method
		//if (Thread.holdsLock(this.monitor)) {
		//	throw new IllegalStateException("Should not call fullyLoad() holding monitor."); //$NON-NLS-1$
		//}
		if ((stateBits & LAZY_LOADED) == 0)
			return this.lazyData;

		StateImpl currentState = (StateImpl) getContainingState();
		StateReader reader = currentState == null ? null : currentState.getReader();
		if (reader == null)
			throw new IllegalStateException("No valid reader for the bundle description"); //$NON-NLS-1$

		synchronized (reader) {
			if (isFullyLoaded()) {
				reader.setAccessedFlag(true); // set reader accessed flag
				return this.lazyData;
			}
			try {
				reader.fullyLoad(this);
				return this.lazyData;
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e); // TODO not sure what to do here!!
			}
		}
	}

	void addDynamicResolvedImport(ExportPackageDescriptionImpl result) {
		synchronized (this.monitor) {
			// mark the dependency
			addDependency(result, true);
			// add the export to the list of the resolvedImports
			checkLazyData();
			if (lazyData.resolvedImports == null) {
				lazyData.resolvedImports = new ExportPackageDescription[] {result};
				return;
			}
			ExportPackageDescription[] newImports = new ExportPackageDescription[lazyData.resolvedImports.length + 1];
			System.arraycopy(lazyData.resolvedImports, 0, newImports, 0, lazyData.resolvedImports.length);
			newImports[newImports.length - 1] = result;
			lazyData.resolvedImports = newImports;
		}
		setLazyLoaded(false);
	}

	void unload() {
		StateImpl currentState = (StateImpl) getContainingState();
		StateReader reader = currentState == null ? null : currentState.getReader();
		if (reader == null)
			throw new IllegalStateException("BundleDescription does not belong to a reader."); //$NON-NLS-1$
		synchronized (reader) {
			if ((stateBits & LAZY_LOADED) == 0)
				return;
			if (!isFullyLoaded())
				return;
			synchronized (this.monitor) {
				setFullyLoaded(false);
				lazyData = null;
			}
		}
	}

	void setDynamicStamps(HashMap dynamicStamps) {
		synchronized (this.monitor) {
			checkLazyData();
			lazyData.dynamicStamps = dynamicStamps;
		}
	}

	void setDynamicStamp(String requestedPackage, Long timestamp) {
		synchronized (this.monitor) {
			checkLazyData();
			if (lazyData.dynamicStamps == null) {
				if (timestamp == null)
					return;
				lazyData.dynamicStamps = new HashMap();
			}
			if (timestamp == null)
				lazyData.dynamicStamps.remove(requestedPackage);
			else
				lazyData.dynamicStamps.put(requestedPackage, timestamp);
		}
	}

	long getDynamicStamp(String requestedPackage) {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			Long stamp = currentData.dynamicStamps == null ? null : (Long) currentData.dynamicStamps.get(requestedPackage);
			return stamp == null ? 0 : stamp.longValue();
		}
	}

	HashMap getDynamicStamps() {
		LazyData currentData = loadLazyData();
		synchronized (this.monitor) {
			return currentData.dynamicStamps;
		}
	}

	public void setEquinoxEE(int equinox_ee) {
		this.equinox_ee = equinox_ee;
	}

	public int getEquinoxEE() {
		return equinox_ee;
	}

	private void checkLazyData() {
		if (lazyData == null)
			lazyData = new LazyData();
	}

	private final class LazyData {
		String location;
		String platformFilter;

		BundleSpecification[] requiredBundles;
		ExportPackageDescription[] exportPackages;
		ImportPackageSpecification[] importPackages;
		GenericDescription[] genericCapabilities;
		GenericSpecification[] genericRequires;
		NativeCodeSpecification nativeCode;

		ExportPackageDescription[] selectedExports;
		BundleDescription[] resolvedRequires;
		ExportPackageDescription[] resolvedImports;
		ExportPackageDescription[] substitutedExports;
		String[] executionEnvironments;

		HashMap dynamicStamps;
	}
}
