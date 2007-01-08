/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.IOException;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.service.resolver.*;

public class BundleDescriptionImpl extends BaseDescriptionImpl implements BundleDescription, KeyedElement {
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
	private int stateBits = FULLY_LOADED | ATTACH_FRAGMENTS | DYNAMIC_FRAGMENTS;

	private long bundleId = -1;
	private HostSpecification host; //null if the bundle is not a fragment
	private StateImpl containingState;

	private Object userObject;
	private int lazyDataOffset = -1;
	private int lazyDataSize = -1;

	//TODO These could be arrays
	private ArrayList dependencies;
	private ArrayList dependents;

	private LazyData lazyData;
	private int equinox_ee = -1;

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
		fullyLoad();
		return lazyData.location;
	}

	public String getPlatformFilter() {
		fullyLoad();
		return lazyData.platformFilter;
	}

	public String[] getExecutionEnvironments() {
		fullyLoad();
		if (lazyData.executionEnvironments == null)
			return EMPTY_STRING;
		return lazyData.executionEnvironments;
	}

	public ImportPackageSpecification[] getImportPackages() {
		fullyLoad();
		if (lazyData.importPackages == null)
			return EMPTY_IMPORTS;
		return lazyData.importPackages;
	}

	public BundleSpecification[] getRequiredBundles() {
		fullyLoad();
		if (lazyData.requiredBundles == null)
			return EMPTY_BUNDLESPECS;
		return lazyData.requiredBundles;
	}

	public GenericSpecification[] getGenericRequires() {
		fullyLoad();
		if (lazyData.genericRequires == null)
			return EMPTY_GENERICSPECS;
		return lazyData.genericRequires;
	}

	public GenericDescription[] getGenericCapabilities() {
		fullyLoad();
		if (lazyData.genericCapabilities == null)
			return EMPTY_GENERICDESCS;
		return lazyData.genericCapabilities;
	}

	public ExportPackageDescription[] getExportPackages() {
		fullyLoad();
		return lazyData.exportPackages == null ? EMPTY_EXPORTS : lazyData.exportPackages;
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
		return containingState.getFragments(this);
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
		fullyLoad();
		if (lazyData.selectedExports == null)
			return EMPTY_EXPORTS;
		return lazyData.selectedExports;
	}

	public BundleDescription[] getResolvedRequires() {
		fullyLoad();
		if (lazyData.resolvedRequires == null)
			return EMPTY_BUNDLEDESCS;
		return lazyData.resolvedRequires;
	}

	public ExportPackageDescription[] getResolvedImports() {
		fullyLoad();
		if (lazyData.resolvedImports == null)
			return EMPTY_EXPORTS;
		return lazyData.resolvedImports;
	}

	protected void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	protected void setSymbolicName(String symbolicName) {
		setName(symbolicName);
	}

	protected void setLocation(String location) {
		checkLazyData();
		lazyData.location = location;
	}

	protected void setPlatformFilter(String platformFilter) {
		checkLazyData();
		lazyData.platformFilter = platformFilter;
	}

	protected void setExecutionEnvironments(String[] executionEnvironments) {
		checkLazyData();
		lazyData.executionEnvironments = executionEnvironments;
	}

	protected void setExportPackages(ExportPackageDescription[] exportPackages) {
		checkLazyData();
		lazyData.exportPackages = exportPackages;
		if (exportPackages != null) {
			for (int i = 0; i < exportPackages.length; i++) {
				((ExportPackageDescriptionImpl) exportPackages[i]).setExporter(this);
			}
		}
	}

	protected void setImportPackages(ImportPackageSpecification[] importPackages) {
		checkLazyData();
		lazyData.importPackages = importPackages;
		if (importPackages != null) {
			for (int i = 0; i < importPackages.length; i++) {
				if (Constants.OSGI_SYSTEM_BUNDLE.equals(importPackages[i].getBundleSymbolicName()))
					((ImportPackageSpecificationImpl) importPackages[i]).setBundleSymbolicName(Constants.getInternalSymbolicName());
				((ImportPackageSpecificationImpl) importPackages[i]).setBundle(this);
				if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(importPackages[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))
					stateBits |= HAS_DYNAMICIMPORT;
			}
		}
	}

	protected void setRequiredBundles(BundleSpecification[] requiredBundles) {
		checkLazyData();
		lazyData.requiredBundles = requiredBundles;
		if (requiredBundles != null)
			for (int i = 0; i < requiredBundles.length; i++) {
				if (Constants.OSGI_SYSTEM_BUNDLE.equals(requiredBundles[i].getName()))
					((VersionConstraintImpl) requiredBundles[i]).setName(Constants.getInternalSymbolicName());
				((VersionConstraintImpl) requiredBundles[i]).setBundle(this);
			}
	}

	protected void setGenericCapabilities(GenericDescription[] genericCapabilities) {
		checkLazyData();
		lazyData.genericCapabilities = genericCapabilities;
		if (genericCapabilities != null)
			for (int i = 0; i < genericCapabilities.length; i++)
				((GenericDescriptionImpl) genericCapabilities[i]).setSupplier(this);
	}

	protected void setGenericRequires(GenericSpecification[] genericRequires) {
		checkLazyData();
		lazyData.genericRequires = genericRequires;
		if (genericRequires != null)
			for (int i = 0; i < genericRequires.length; i++)
				((VersionConstraintImpl) genericRequires[i]).setBundle(this);
	}

	protected int getStateBits() {
		return stateBits;
	}

	protected void setStateBit(int stateBit, boolean on) {
		if (on)
			stateBits |= stateBit;
		else
			stateBits &= ~stateBit;
	}

	protected void setContainingState(State value) {
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

	protected void setHost(HostSpecification host) {
		this.host = host;
		if (host != null) {
			if (Constants.OSGI_SYSTEM_BUNDLE.equals(host.getName()))
				((VersionConstraintImpl) host).setName(Constants.getInternalSymbolicName());
			((VersionConstraintImpl) host).setBundle(this);
		}
	}

	protected void setLazyLoaded(boolean lazyLoad) {
		fullyLoad();
		if (lazyLoad)
			stateBits |= LAZY_LOADED;
		else
			stateBits &= ~LAZY_LOADED;
	}

	protected void setSelectedExports(ExportPackageDescription[] selectedExports) {
		checkLazyData();
		lazyData.selectedExports = selectedExports;
		if (selectedExports != null) {
			for (int i = 0; i < selectedExports.length; i++) {
				((ExportPackageDescriptionImpl) selectedExports[i]).setExporter(this);
			}
		}
	}

	protected void setResolvedImports(ExportPackageDescription[] resolvedImports) {
		checkLazyData();
		lazyData.resolvedImports = resolvedImports;
	}

	protected void setResolvedRequires(BundleDescription[] resolvedRequires) {
		checkLazyData();
		lazyData.resolvedRequires = resolvedRequires;
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
		return (int) (bundleId % Integer.MAX_VALUE);
	}

	/* TODO Determine if we need more than just Object ID type of hashcode.
	 public int hashCode() {
	 if (getSymbolicName() == null)
	 return (int) (bundleId % Integer.MAX_VALUE);
	 return (int) ((bundleId * (getSymbolicName().hashCode())) % Integer.MAX_VALUE);
	 }
	 */

	protected synchronized void removeDependencies() {
		if (dependencies == null)
			return;
		Iterator iter = dependencies.iterator();
		while (iter.hasNext()) {
			((BundleDescriptionImpl) iter.next()).removeDependent(this);
		}
		dependencies = null;
	}

	protected void addDependencies(BaseDescription[] newDependencies, boolean checkDups) {
		if (newDependencies == null)
			return;
		if (!checkDups && dependencies == null)
			dependencies = new ArrayList(newDependencies.length);
		for (int i = 0; i < newDependencies.length; i++) {
			addDependency((BaseDescriptionImpl) newDependencies[i], checkDups);
		}
	}

	protected synchronized void addDependency(BaseDescriptionImpl dependency, boolean checkDups) {
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

	/*
	 * Gets all the bundle dependencies as a result of import-package or require-bundle.
	 * Self and fragment bundles are removed.
	 */
	synchronized List getBundleDependencies() {
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

	public Object getUserObject() {
		return userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	protected synchronized void addDependent(BundleDescription dependent) {
		if (dependents == null)
			dependents = new ArrayList(10);
		// no need to check for duplicates here; this is only called in addDepenency which already checks for dups.
		dependents.add(dependent);
	}

	protected synchronized void removeDependent(BundleDescription dependent) {
		if (dependents == null)
			return;
		dependents.remove(dependent);
	}

	public synchronized BundleDescription[] getDependents() {
		if (dependents == null)
			return EMPTY_BUNDLEDESCS;
		return (BundleDescription[]) dependents.toArray(new BundleDescription[dependents.size()]);
	}

	void setFullyLoaded(boolean fullyLoaded) {
		if (fullyLoaded) {
			stateBits |= FULLY_LOADED;
		} else {
			stateBits &= ~FULLY_LOADED;
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

	private void fullyLoad() {
		if ((stateBits & LAZY_LOADED) == 0)
			return;
		StateReader reader = containingState.getReader();
		synchronized (reader) {
			if (isFullyLoaded()) {
				reader.setAccessedFlag(true); // set reader accessed flag
			return;
			}
			try {
				reader.fullyLoad(this);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage()); // TODO not sure what to do here!!
			}
		}
	}

	synchronized void addDynamicResolvedImport(ExportPackageDescriptionImpl result) {
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
		setLazyLoaded(false);
	}

	/*
	 * This method must be called while the state reader for the containing state is locked.
	 */
	void unload() {
		if ((stateBits & LAZY_LOADED) == 0)
			return;
		if (!isFullyLoaded())
			return;
		setFullyLoaded(false);
		LazyData tempData = lazyData;
		lazyData = null;
		if (tempData == null || tempData.selectedExports == null)
			return;
		for (int i = 0; i < tempData.selectedExports.length; i++)
			containingState.getReader().objectTable.remove(new Integer(((ExportPackageDescriptionImpl) tempData.selectedExports[i]).getTableIndex()));
	}

	void setDynamicStamps(HashMap dynamicStamps) {
		lazyData.dynamicStamps = dynamicStamps;
	}

	void setDynamicStamp(String requestedPackage, Long timestamp) {
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

	long getDynamicStamp(String requestedPackage) {
		fullyLoad();
		Long stamp = lazyData.dynamicStamps == null ? null : (Long) lazyData.dynamicStamps.get(requestedPackage);
		return stamp == null ? 0 : stamp.longValue();
	}

	HashMap getDynamicStamps() {
		fullyLoad();
		return lazyData.dynamicStamps;
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

		ExportPackageDescription[] selectedExports;
		BundleDescription[] resolvedRequires;
		ExportPackageDescription[] resolvedImports;

		String[] executionEnvironments;

		HashMap dynamicStamps;
	}
}
