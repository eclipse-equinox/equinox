/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
import java.util.Map.Entry;
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

		ResolverExport[] allExports = getExportPackages();
		for (int i = 0; i < allExports.length; i++)
			allExports[i].setSubstitute(null);
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
		return getExports(true);
	}

	ResolverExport[] getSubstitutedExports() {
		return getExports(false);
	}

	private ResolverExport[] getExports(boolean selected) {
		ResolverExport[] results = getExportPackages();
		int removedExports = 0;
		for (int i = 0; i < results.length; i++)
			if (selected ? results[i].getSubstitute() != null : results[i].getSubstitute() == null)
				removedExports++;
		if (removedExports == 0)
			return results;
		ResolverExport[] selectedExports = new ResolverExport[results.length - removedExports];
		int index = 0;
		for (int i = 0; i < results.length; i++) {
			if (selected ? results[i].getSubstitute() != null : results[i].getSubstitute() == null)
				continue;
			selectedExports[index] = results[i];
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
				ResolverExport currentExports[] = getExports(newExports[i].getName());
				boolean foundEquivalent = false;
				for (int j = 0; j < currentExports.length && !foundEquivalent; j++) {
					if (equivalentExports(currentExports[j], newExports[i]))
						foundEquivalent = true;
				}
				if (!foundEquivalent) {
					ExportPackageDescription hostExport = factory.createExportPackageDescription(newExports[i].getName(), newExports[i].getVersion(), newExports[i].getDirectives(), newExports[i].getAttributes(), newExports[i].isRoot(), getBundle());
					hostExports.add(new ResolverExport(this, hostExport));
				}
			}
			fragmentExports.put(fragment.bundleID, hostExports);
		}
		return (ResolverExport[]) hostExports.toArray(new ResolverExport[hostExports.size()]);
	}

	private boolean equivalentExports(ResolverExport existingExport, ExportPackageDescription newDescription) {
		ExportPackageDescription existingDescription = existingExport.getExportPackageDescription();
		if (!existingDescription.getName().equals(newDescription.getName()))
			return false;
		if (!existingDescription.getVersion().equals(newDescription.getVersion()))
			return false;
		if (!equivalentMaps(existingDescription.getAttributes(), newDescription.getAttributes(), true))
			return false;
		if (!equivalentMaps(existingDescription.getDirectives(), newDescription.getDirectives(), true))
			return false;
		return true;
	}

	public static boolean equivalentMaps(Map existingDirectives, Map newDirectives, boolean exactMatch) {
		if (existingDirectives == null && newDirectives == null)
			return true;
		if (existingDirectives == null ? newDirectives != null : newDirectives == null)
			return false;
		if (exactMatch && existingDirectives.size() != newDirectives.size())
			return false;
		for (Iterator entries = existingDirectives.entrySet().iterator(); entries.hasNext();) {
			Entry entry = (Entry) entries.next();
			Object newValue = newDirectives.get(entry.getKey());
			if (newValue == null || entry.getValue().getClass() != newValue.getClass())
				return false;
			if (newValue instanceof String[]) {
				if (!Arrays.equals((Object[]) entry.getValue(), (Object[]) newValue))
					return false;
			} else if (!entry.getValue().equals(newValue)) {
				return false;
			}
		}
		return true;
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

		// must save off old imports and requires before we remove the fragment;
		// this will be used to merge the constraints of the same name from the remaining fragments 
		ResolverImport[] oldImports = getImportPackages();
		BundleConstraint[] oldRequires = getRequires();
		if (!fragments.remove(fragment))
			return new ResolverExport[0];

		fragment.setNewFragmentExports(false);
		fragment.getHost().removePossibleSupplier(this);
		fragmentImports.remove(fragment.bundleID);
		fragmentRequires.remove(fragment.bundleID);
		ArrayList removedExports = (ArrayList) fragmentExports.remove(fragment.bundleID);
		fragmentGenericRequires.remove(fragment.bundleID);
		if (reason != null) {
			// the fragment is being detached because one of its imports or requires cannot be resolved;
			// we need to check the remaining fragment constraints to make sure they do not have
			// the same unresolved constraint.
			ResolverBundle[] remainingFrags = (ResolverBundle[]) fragments.toArray(new ResolverBundle[fragments.size()]);
			for (int i = 0; i < remainingFrags.length; i++) {
				ArrayList additionalImports = new ArrayList(0);
				ArrayList additionalRequires = new ArrayList(0);
				if (hasUnresolvedConstraint(reason, fragment, remainingFrags[i], oldImports, oldRequires, additionalImports, additionalRequires))
					continue;
				// merge back the additional imports or requires which the detached fragment has in common with the remaining fragment
				if (additionalImports.size() > 0) {
					ArrayList remainingImports = (ArrayList) fragmentImports.get(remainingFrags[i].bundleID);
					if (remainingImports == null)
						fragmentImports.put(remainingFrags[i].bundleID, additionalImports);
					else
						remainingImports.addAll(additionalImports);
				}
				if (additionalRequires.size() > 0) {
					ArrayList remainingRequires = (ArrayList) fragmentRequires.get(remainingFrags[i].bundleID);
					if (remainingRequires == null)
						fragmentRequires.put(remainingFrags[i].bundleID, additionalRequires);
					else
						remainingRequires.addAll(additionalRequires);
				}
			}
		}
		ResolverExport[] results = removedExports == null ? new ResolverExport[0] : (ResolverExport[]) removedExports.toArray(new ResolverExport[removedExports.size()]);
		for (int i = 0; i < results.length; i++)
			// TODO this is a hack; need to figure out how to indicate that a fragment export is no longer attached
			results[i].setSubstitute(results[i]);
		return results;
	}

	private boolean hasUnresolvedConstraint(ResolverConstraint reason, ResolverBundle detachedFragment, ResolverBundle remainingFragment, ResolverImport[] oldImports, BundleConstraint[] oldRequires, ArrayList additionalImports, ArrayList additionalRequires) {
		ImportPackageSpecification[] remainingFragImports = remainingFragment.getBundle().getImportPackages();
		BundleSpecification[] remainingFragRequires = remainingFragment.getBundle().getRequiredBundles();
		VersionConstraint[] constraints;
		if (reason instanceof ResolverImport)
			constraints = remainingFragImports;
		else
			constraints = remainingFragRequires;
		for (int i = 0; i < constraints.length; i++)
			if (reason.getName().equals(constraints[i].getName())) {
				resolver.getResolverExports().remove(detachFragment(remainingFragment, null));
				return true;
			}
		for (int i = 0; i < oldImports.length; i++) {
			if (oldImports[i].getVersionConstraint().getBundle() != detachedFragment.getBundle())
				continue; // the constraint is not from the detached fragment
			for (int j = 0; j < remainingFragImports.length; j++) {
				if (oldImports[i].getName().equals(remainingFragImports[j].getName())) {
					// same constraint, must reuse the constraint object but swap out the fragment info
					additionalImports.add(oldImports[i]);
					oldImports[i].setVersionConstraint(remainingFragImports[j]);
					break;
				}
			}
		}
		for (int i = 0; i < oldRequires.length; i++) {
			if (oldRequires[i].getVersionConstraint().getBundle() != detachedFragment.getBundle())
				continue; // the constraint is not from the detached fragment
			for (int j = 0; j < remainingFragRequires.length; j++) {
				if (oldRequires[i].getName().equals(remainingFragRequires[j].getName())) {
					// same constraint, must reuse the constraint object but swap out the fragment info
					additionalRequires.add(oldRequires[i]);
					oldRequires[i].setVersionConstraint(remainingFragRequires[j]);
					break;
				}
			}
		}
		return false;
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
		if (((BundleDescription) getBaseDescription()).isResolved())
			return; // don't care when the bundle is already resolved
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
