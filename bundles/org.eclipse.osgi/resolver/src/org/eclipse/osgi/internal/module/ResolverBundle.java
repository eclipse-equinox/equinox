/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.*;
import org.eclipse.osgi.internal.resolver.ExportPackageDescriptionImpl;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;

/*
 * A companion to BundleDescription from the state used while resolving.
 */
public class ResolverBundle extends VersionSupplier implements Comparable {
	public static final int UNRESOLVED = 0;
	public static final int RESOLVING = 1;
	public static final int RESOLVED = 2;

	private Long bundleID;
	private BundleConstraint host;
	private ResolverImport[] imports;
	private ResolverExport[] exports;
	private BundleConstraint[] requires;
	private GenericCapability[] capabilities;
	private GenericConstraint[] genericReqiures;
	// Fragment support
	private ArrayList fragments;
	private HashMap fragmentExports;
	private HashMap fragmentImports;
	private HashMap fragmentRequires;
	private HashMap fragmentGenericRequires;
	// Flag specifying whether this bundle is resolvable
	private boolean resolvable = true;
	// Internal resolver state for this bundle
	private int state = UNRESOLVED;
	private boolean uninstalled = false;
	private ResolverImpl resolver;
	private boolean newFragmentExports;
	private ArrayList refs;

	ResolverBundle(BundleDescription bundle, ResolverImpl resolver) {
		super(bundle);
		this.bundleID = new Long(bundle.getBundleId());
		this.resolver = resolver;
		initialize(bundle.isResolved());
	}

	void initialize(boolean useSelectedExports) {
		if (getBundle().isSingleton())
			refs = new ArrayList();
		// always add generic capabilities
		GenericDescription[] actualCapabilities = getBundle().getGenericCapabilities();
		capabilities = new GenericCapability[actualCapabilities.length];
		for (int i = 0; i < capabilities.length; i++)
			capabilities[i] = new GenericCapability(this, actualCapabilities[i]);
		if (getBundle().getHost() != null) {
			host = new BundleConstraint(this, getBundle().getHost());
			exports = new ResolverExport[0];
			imports = new ResolverImport[0];
			requires = new BundleConstraint[0];
			genericReqiures = new GenericConstraint[0];
			return;
		}

		ImportPackageSpecification[] actualImports = getBundle().getImportPackages();
		// Reorder imports so that optionals are at the end so that we wire statics before optionals
		ArrayList importList = new ArrayList(actualImports.length);
		for (int i = actualImports.length - 1; i >= 0; i--)
			if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(actualImports[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))
				importList.add(new ResolverImport(this, actualImports[i]));
			else
				importList.add(0, new ResolverImport(this, actualImports[i]));
		imports = (ResolverImport[]) importList.toArray(new ResolverImport[importList.size()]);

		ExportPackageDescription[] actualExports = useSelectedExports ? getBundle().getSelectedExports() : getBundle().getExportPackages();
		exports = new ResolverExport[actualExports.length];
		for (int i = 0; i < actualExports.length; i++)
			exports[i] = new ResolverExport(this, actualExports[i]);

		BundleSpecification[] actualRequires = getBundle().getRequiredBundles();
		requires = new BundleConstraint[actualRequires.length];
		for (int i = 0; i < requires.length; i++)
			requires[i] = new BundleConstraint(this, actualRequires[i]);

		GenericSpecification[] actualGenericRequires = getBundle().getGenericRequires();
		genericReqiures = new GenericConstraint[actualGenericRequires.length];
		for (int i = 0; i < genericReqiures.length; i++)
			genericReqiures[i] = new GenericConstraint(this, actualGenericRequires[i]);

		fragments = null;
		fragmentExports = null;
		fragmentImports = null;
		fragmentRequires = null;
		fragmentGenericRequires = null;
	}

	ResolverExport getExport(String name) {
		ResolverExport[] allExports = getExports(name);
		return allExports.length == 0 ? null : allExports[0];
	}

	ResolverExport[] getExports(String name) {
		ArrayList results = new ArrayList(1); // rare to have more than one
		// it is faster to ask the VersionHashMap for this package name and then compare the exporter to this
		Object[] resolverExports = resolver.getResolverExports().get(name);
		for (int i = 0; i < resolverExports.length; i++)
			if (((ResolverExport) resolverExports[i]).getExporter() == this)
				results.add(resolverExports[i]);
		return (ResolverExport[]) results.toArray(new ResolverExport[results.size()]);
	}

	void clearWires() {
		ResolverImport[] allImports = getImportPackages();
		for (int i = 0; i < allImports.length; i++)
			allImports[i].clearPossibleSuppliers();

		if (host != null)
			host.clearPossibleSuppliers();

		BundleConstraint[] allRequires = getRequires();
		for (int i = 0; i < allRequires.length; i++)
			allRequires[i].clearPossibleSuppliers();

		GenericConstraint[] allGenericRequires = getGenericRequires();
		for (int i = 0; i < allGenericRequires.length; i++)
			allGenericRequires[i].setMatchingCapability(null);
	}

	boolean isResolved() {
		return getState() == ResolverBundle.RESOLVED;
	}

	boolean isFragment() {
		return host != null;
	}

	int getState() {
		return state;
	}

	void setState(int state) {
		this.state = state;
	}

	ResolverImport[] getImportPackages() {
		if (isFragment())
			return new ResolverImport[0];
		if (fragments == null || fragments.size() == 0)
			return imports;
		ArrayList resultList = new ArrayList(imports.length);
		for (int i = 0; i < imports.length; i++)
			resultList.add(imports[i]);
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			ResolverBundle fragment = (ResolverBundle) iter.next();
			ArrayList fragImports = (ArrayList) fragmentImports.get(fragment.bundleID);
			if (fragImports != null)
				resultList.addAll(fragImports);
		}
		return (ResolverImport[]) resultList.toArray(new ResolverImport[resultList.size()]);
	}

	ResolverExport[] getExportPackages() {
		if (isFragment())
			return new ResolverExport[0];
		if (fragments == null || fragments.size() == 0)
			return exports;
		ArrayList resultList = new ArrayList(exports.length);
		for (int i = 0; i < exports.length; i++)
			resultList.add(exports[i]);
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			ResolverBundle fragment = (ResolverBundle) iter.next();
			ArrayList fragExports = (ArrayList) fragmentExports.get(fragment.bundleID);
			if (fragExports != null)
				resultList.addAll(fragExports);
		}
		return (ResolverExport[]) resultList.toArray(new ResolverExport[resultList.size()]);
	}

	ResolverExport[] getSelectedExports() {
		ResolverExport[] allExports = getExportPackages();
		int removedExports = 0;
		for (int i = 0; i < allExports.length; i++)
			if (allExports[i].isDropped())
				removedExports++;
		if (removedExports == 0)
			return allExports;
		ResolverExport[] selectedExports = new ResolverExport[allExports.length - removedExports];
		int index = 0;
		for (int i = 0; i < allExports.length; i++) {
			if (allExports[i].isDropped())
				continue;
			selectedExports[index] = allExports[i];
			index++;
		}
		return selectedExports;
	}

	BundleConstraint getHost() {
		return host;
	}

	GenericCapability[] getGenericCapabilities() {
		return capabilities;
	}

	BundleConstraint[] getRequires() {
		if (isFragment())
			return new BundleConstraint[0];
		if (fragments == null || fragments.size() == 0)
			return requires;
		ArrayList resultList = new ArrayList(requires.length);
		for (int i = 0; i < requires.length; i++)
			resultList.add(requires[i]);
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			ResolverBundle fragment = (ResolverBundle) iter.next();
			ArrayList fragRequires = (ArrayList) fragmentRequires.get(fragment.bundleID);
			if (fragRequires != null)
				resultList.addAll(fragRequires);
		}
		return (BundleConstraint[]) resultList.toArray(new BundleConstraint[resultList.size()]);
	}

	GenericConstraint[] getGenericRequires() {
		if (isFragment() || fragments == null || fragments.size() == 0)
			return genericReqiures;
		ArrayList resultList = new ArrayList(genericReqiures.length);
		for (int i = 0; i < genericReqiures.length; i++)
			resultList.add(genericReqiures[i]);
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			ResolverBundle fragment = (ResolverBundle) iter.next();
			ArrayList fragGenericRegs = (ArrayList) fragmentGenericRequires.get(fragment.bundleID);
			if (fragGenericRegs != null)
				resultList.addAll(fragGenericRegs);
		}
		return (GenericConstraint[]) resultList.toArray(new GenericConstraint[resultList.size()]);
	}

	BundleConstraint getRequire(String name) {
		BundleConstraint[] allRequires = getRequires();
		for (int i = 0; i < allRequires.length; i++)
			if (allRequires[i].getVersionConstraint().getName().equals(name))
				return allRequires[i];
		return null;
	}

	public BundleDescription getBundle() {
		return (BundleDescription) getBaseDescription();
	}

	public ResolverBundle getResolverBundle() {
		return this;
	}

	ResolverImport getImport(String name) {
		ResolverImport[] allImports = getImportPackages();
		for (int i = 0; i < allImports.length; i++) {
			if (allImports[i].getName().equals(name)) {
				return allImports[i];
			}
		}
		return null;
	}

	public String toString() {
		return "[" + getBundle() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void initFragments() {
		if (fragments == null)
			fragments = new ArrayList(1);
		if (fragmentExports == null)
			fragmentExports = new HashMap(1);
		if (fragmentImports == null)
			fragmentImports = new HashMap(1);
		if (fragmentRequires == null)
			fragmentRequires = new HashMap(1);
		if (fragmentGenericRequires == null)
			fragmentGenericRequires = new HashMap(1);
	}

	private boolean isImported(String packageName) {
		ResolverImport[] allImports = getImportPackages();
		for (int i = 0; i < allImports.length; i++)
			if (packageName.equals(allImports[i].getName()))
				return true;

		return false;
	}

	private boolean isExported(String packageName) {
		ResolverExport export = getExport(packageName);
		if (export == null)
			return false;
		// let exports from a bundle manifest be exported in addition to the ones from the vm profile
		return 0 > ((Integer) export.getExportPackageDescription().getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue();
	}

	private boolean isRequired(String bundleName) {
		return getRequire(bundleName) != null;
	}

	ResolverExport[] attachFragment(ResolverBundle fragment, boolean dynamicAttach) {
		if (isFragment())
			return new ResolverExport[0]; // cannot attach to fragments;
		if (!getBundle().attachFragments() || (isResolved() && !getBundle().dynamicFragments()))
			return new ResolverExport[0]; // host is restricting attachment
		if (fragment.getHost().getNumPossibleSuppliers() > 0 && !((HostSpecification) fragment.getHost().getVersionConstraint()).isMultiHost())
			return new ResolverExport[0]; // fragment is restricting attachment

		ImportPackageSpecification[] newImports = fragment.getBundle().getImportPackages();
		BundleSpecification[] newRequires = fragment.getBundle().getRequiredBundles();
		ExportPackageDescription[] newExports = fragment.getBundle().getExportPackages();
		GenericSpecification[] newGenericRequires = fragment.getBundle().getGenericRequires();

		// if this is not during initialization then check if constraints conflict
		if (dynamicAttach && constraintsConflict(fragment.getBundle(), newImports, newRequires, newGenericRequires))
			return new ResolverExport[0]; // do not allow fragments with conflicting constraints
		if (isResolved() && newExports.length > 0)
			fragment.setNewFragmentExports(true);

		initFragments();
		// need to make sure there is not already another version of this fragment 
		// already attached to this host
		for (Iterator iFragments = fragments.iterator(); iFragments.hasNext();) {
			ResolverBundle existingFragment = (ResolverBundle) iFragments.next();
			String bsn = existingFragment.getName();
			if (bsn != null && bsn.equals(fragment.getName()))
				return new ResolverExport[0];
		}
		if (fragments.contains(fragment))
			return new ResolverExport[0];
		fragments.add(fragment);
		fragment.getHost().addPossibleSupplier(this);

		if (newImports.length > 0) {
			ArrayList hostImports = new ArrayList(newImports.length);
			for (int i = 0; i < newImports.length; i++)
				if (!isImported(newImports[i].getName()))
					hostImports.add(new ResolverImport(this, newImports[i]));
			fragmentImports.put(fragment.bundleID, hostImports);
		}

		if (newRequires.length > 0) {
			ArrayList hostRequires = new ArrayList(newRequires.length);
			for (int i = 0; i < newRequires.length; i++)
				if (!isRequired(newRequires[i].getName()))
					hostRequires.add(new BundleConstraint(this, newRequires[i]));
			fragmentRequires.put(fragment.bundleID, hostRequires);
		}

		if (newGenericRequires.length > 0) {
			ArrayList hostGenericRequires = new ArrayList(newGenericRequires.length);
			for (int i = 0; i < newGenericRequires.length; i++)
				hostGenericRequires.add(new GenericConstraint(this, newGenericRequires[i]));
			fragmentGenericRequires.put(fragment.bundleID, hostGenericRequires);
		}

		ArrayList hostExports = new ArrayList(newExports.length);
		if (newExports.length > 0 && dynamicAttach) {
			StateObjectFactory factory = resolver.getState().getFactory();
			for (int i = 0; i < newExports.length; i++) {
				if (!isExported(newExports[i].getName())) {
					ExportPackageDescription hostExport = factory.createExportPackageDescription(newExports[i].getName(), newExports[i].getVersion(), newExports[i].getDirectives(), newExports[i].getAttributes(), newExports[i].isRoot(), getBundle());
					hostExports.add(new ResolverExport(this, hostExport));
				}
			}
			fragmentExports.put(fragment.bundleID, hostExports);
		}
		return (ResolverExport[]) hostExports.toArray(new ResolverExport[hostExports.size()]);
	}

	boolean constraintsConflict(BundleDescription fragment, ImportPackageSpecification[] newImports, BundleSpecification[] newRequires, GenericSpecification[] newGenericRequires) {
		// this method iterates over all additional constraints from a fragment
		// if the host is resolved then the fragment is not allowed to add new constraints;
		// if the host is resolved and it already has a constraint of the same name then ensure the supplier satisfies the fragment's constraint
		boolean result = false;
		for (int i = 0; i < newImports.length; i++) {
			ResolverImport hostImport = getImport(newImports[i].getName());
			ResolverExport resolvedExport = (ResolverExport) (hostImport == null ? null : hostImport.getSelectedSupplier());
			if ((resolvedExport == null && isResolved()) || (resolvedExport != null && !newImports[i].isSatisfiedBy(resolvedExport.getExportPackageDescription()))) {
				result = true;
				resolver.getState().addResolverError(fragment, ResolverError.FRAGMENT_CONFLICT, newImports[i].toString(), newImports[i]);
			}
		}
		for (int i = 0; i < newRequires.length; i++) {
			BundleConstraint hostRequire = getRequire(newRequires[i].getName());
			ResolverBundle resolvedRequire = (ResolverBundle) (hostRequire == null ? null : hostRequire.getSelectedSupplier());
			if ((resolvedRequire == null && isResolved()) || (resolvedRequire != null && !newRequires[i].isSatisfiedBy(resolvedRequire.getBundle()))) {
				result = true;
				resolver.getState().addResolverError(fragment, ResolverError.FRAGMENT_CONFLICT, newRequires[i].toString(), newRequires[i]);
			}
		}
		// generic constraints cannot conflict; 
		// only check that a fragment does not add generics constraints to an already resolved host
		if (isResolved() && newGenericRequires != null && newGenericRequires.length > 0)
			result = true;
		return result;
	}

	private void setNewFragmentExports(boolean newFragmentExports) {
		this.newFragmentExports = newFragmentExports;
	}

	boolean isNewFragmentExports() {
		return newFragmentExports;
	}

	ResolverExport[] detachFragment(ResolverBundle fragment, ResolverConstraint reason) {
		if (isFragment())
			return new ResolverExport[0];
		initFragments();

		if (!fragments.remove(fragment))
			return new ResolverExport[0];

		fragment.setNewFragmentExports(false);
		fragment.getHost().removePossibleSupplier(this);
		ArrayList fragImports = (ArrayList) fragmentImports.remove(fragment.bundleID);
		ArrayList fragRequires = (ArrayList) fragmentRequires.remove(fragment.bundleID);
		ArrayList removedExports = (ArrayList) fragmentExports.remove(fragment.bundleID);
		fragmentGenericRequires.remove(fragment.bundleID);
		if (reason != null) {
			ResolverBundle[] remainingFrags = (ResolverBundle[]) fragments.toArray(new ResolverBundle[fragments.size()]);
			for (int i = 0; i < remainingFrags.length; i++) {
				resolver.getResolverExports().remove(detachFragment(remainingFrags[i], null));
				VersionConstraint[] constraints;
				if (reason instanceof ResolverImport)
					constraints = remainingFrags[i].getBundle().getImportPackages();
				else
					constraints = remainingFrags[i].getBundle().getRequiredBundles();
				for (int j = 0; j < constraints.length; j++)
					if (reason.getName().equals(constraints[j].getName()))
						continue; // this fragment should remained unattached.
				resolver.getResolverExports().put(attachFragment(remainingFrags[i], true));
				ArrayList newImports = (ArrayList) fragmentImports.get(remainingFrags[i].bundleID);
				if (newImports != null && fragImports != null)
					for (Iterator iNewImports = newImports.iterator(); iNewImports.hasNext();) {
						ResolverImport newImport = (ResolverImport) iNewImports.next();
						for (Iterator iOldImports = fragImports.iterator(); iOldImports.hasNext();) {
							ResolverImport oldImport = (ResolverImport) iOldImports.next();
							if (newImport.getName().equals(oldImport.getName()))
								newImport.setPossibleSuppliers(oldImport.getPossibleSuppliers());
						}
					}
				ArrayList newRequires = (ArrayList) fragmentRequires.get(remainingFrags[i].bundleID);
				if (newRequires != null && fragRequires != null)
					for (Iterator iNewRequires = newRequires.iterator(); iNewRequires.hasNext();) {
						BundleConstraint newRequire = (BundleConstraint) iNewRequires.next();
						for (Iterator iOldRequires = fragRequires.iterator(); iOldRequires.hasNext();) {
							BundleConstraint oldRequire = (BundleConstraint) iOldRequires.next();
							if (newRequire.getName().equals(oldRequire.getName()))
								newRequire.setPossibleSuppliers(oldRequire.getPossibleSuppliers());
						}
					}
			}
		}
		return removedExports == null ? new ResolverExport[0] : (ResolverExport[]) removedExports.toArray(new ResolverExport[removedExports.size()]);
	}

	void detachAllFragments() {
		if (fragments == null)
			return;
		ResolverBundle[] allFragments = (ResolverBundle[]) fragments.toArray(new ResolverBundle[fragments.size()]);
		for (int i = 0; i < allFragments.length; i++)
			detachFragment(allFragments[i], null);
	}

	boolean isResolvable() {
		return resolvable;
	}

	void setResolvable(boolean resolvable) {
		this.resolvable = resolvable;
	}

	void addExport(ResolverExport re) {
		ResolverExport[] newExports = new ResolverExport[exports.length + 1];
		for (int i = 0; i < exports.length; i++)
			newExports[i] = exports[i];
		newExports[exports.length] = re;
		exports = newExports;
	}

	ResolverImpl getResolver() {
		return resolver;
	}

	void clearRefs() {
		if (refs != null)
			refs.clear();
	}

	void addRef(ResolverBundle ref) {
		if (refs != null && !refs.contains(ref))
			refs.add(ref);
	}

	int getRefs() {
		return refs == null ? 0 : refs.size();
	}

	ResolverBundle[] getFragments() {
		return fragments == null ? new ResolverBundle[0] : (ResolverBundle[]) fragments.toArray(new ResolverBundle[fragments.size()]);
	}

	/*
	 * This is used to sort bundles by BSN.  This is needed to fix bug 174930
	 * If both BSNs are null then 0 is returned
	 * If this BSN is null the 1 is returned
	 * If the other BSN is null then -1 is returned
	 * otherwise String.compareTo is used
	 */
	public int compareTo(Object o) {
		String bsn = getName();
		String otherBsn = ((ResolverBundle) o).getName();
		if (bsn == null)
			return otherBsn == null ? 0 : 1;
		return otherBsn == null ? -1 : bsn.compareTo(otherBsn);
	}

	void setUninstalled() {
		uninstalled = true;
	}

	boolean isUninstalled() {
		return uninstalled;
	}
}
