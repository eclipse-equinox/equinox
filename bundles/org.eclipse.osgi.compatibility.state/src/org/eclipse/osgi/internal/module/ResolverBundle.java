/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
import org.eclipse.osgi.internal.resolver.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;

/*
 * A companion to BundleDescription from the state used while resolving.
 */
public class ResolverBundle extends VersionSupplier implements Comparable<ResolverBundle> {
	public static final int UNRESOLVED = 0;
	public static final int RESOLVING = 1;
	public static final int RESOLVED = 2;

	private final Long bundleID;
	private BundleConstraint host;
	private ResolverImport[] imports;
	private ResolverExport[] exports;
	private BundleConstraint[] requires;
	private GenericCapability[] genericCapabilities;
	private GenericConstraint[] genericReqiures;
	// Fragment support
	private ArrayList<ResolverBundle> fragments;
	private HashMap<Long, List<ResolverExport>> fragmentExports;
	private HashMap<Long, List<ResolverImport>> fragmentImports;
	private HashMap<Long, List<BundleConstraint>> fragmentRequires;
	private HashMap<Long, List<GenericCapability>> fragmentGenericCapabilities;
	private HashMap<Long, List<GenericConstraint>> fragmentGenericRequires;
	// Flag specifying whether this bundle is resolvable
	private boolean resolvable = true;
	// Internal resolver state for this bundle
	private int state = UNRESOLVED;
	private boolean uninstalled = false;
	private final ResolverImpl resolver;
	private boolean newFragmentExports;
	private boolean newFragmentCapabilities;

	ResolverBundle(BundleDescription bundle, ResolverImpl resolver) {
		super(bundle);
		this.bundleID = new Long(bundle.getBundleId());
		this.resolver = resolver;
		initialize(bundle.isResolved());
	}

	void initialize(boolean useSelectedExports) {
		if (getBundleDescription().getHost() != null) {
			host = new BundleConstraint(this, getBundleDescription().getHost());
			exports = new ResolverExport[0];
			imports = new ResolverImport[0];
			requires = new BundleConstraint[0];
			GenericSpecification[] requirements = getBundleDescription().getGenericRequires();
			List<GenericConstraint> constraints = new ArrayList<GenericConstraint>();
			for (GenericSpecification requirement : requirements) {
				if (StateImpl.OSGI_EE_NAMESPACE.equals(requirement.getType()))
					constraints.add(new GenericConstraint(this, requirement));
			}
			genericReqiures = constraints.toArray(new GenericConstraint[constraints.size()]);
			GenericDescription[] capabilities = getBundleDescription().getGenericCapabilities();
			GenericCapability identity = null;
			for (GenericDescription capability : capabilities) {
				if (IdentityNamespace.IDENTITY_NAMESPACE.equals(capability.getType())) {
					identity = new GenericCapability(this, capability);
					break;
				}
			}

			genericCapabilities = identity == null ? new GenericCapability[0] : new GenericCapability[] {identity};
			return;
		}

		ImportPackageSpecification[] actualImports = getBundleDescription().getImportPackages();
		// Reorder imports so that optionals are at the end so that we wire statics before optionals
		List<ResolverImport> importList = new ArrayList<ResolverImport>(actualImports.length);
		for (int i = actualImports.length - 1; i >= 0; i--)
			if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(actualImports[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))
				importList.add(new ResolverImport(this, actualImports[i]));
			else
				importList.add(0, new ResolverImport(this, actualImports[i]));
		imports = importList.toArray(new ResolverImport[importList.size()]);

		ExportPackageDescription[] actualExports = useSelectedExports ? getBundleDescription().getSelectedExports() : getBundleDescription().getExportPackages();
		exports = new ResolverExport[actualExports.length];
		for (int i = 0; i < actualExports.length; i++)
			exports[i] = new ResolverExport(this, actualExports[i]);

		BundleSpecification[] actualRequires = getBundleDescription().getRequiredBundles();
		requires = new BundleConstraint[actualRequires.length];
		for (int i = 0; i < requires.length; i++)
			requires[i] = new BundleConstraint(this, actualRequires[i]);

		GenericSpecification[] actualGenericRequires = getBundleDescription().getGenericRequires();
		genericReqiures = new GenericConstraint[actualGenericRequires.length];
		for (int i = 0; i < genericReqiures.length; i++)
			genericReqiures[i] = new GenericConstraint(this, actualGenericRequires[i]);

		GenericDescription[] actualCapabilities = useSelectedExports ? getBundleDescription().getSelectedGenericCapabilities() : getBundleDescription().getGenericCapabilities();
		genericCapabilities = new GenericCapability[actualCapabilities.length];
		for (int i = 0; i < genericCapabilities.length; i++)
			genericCapabilities[i] = new GenericCapability(this, actualCapabilities[i]);

		fragments = null;
		fragmentExports = null;
		fragmentImports = null;
		fragmentRequires = null;
		fragmentGenericCapabilities = null;
		fragmentGenericRequires = null;
	}

	ResolverExport getExport(String name) {
		ResolverExport[] allExports = getExports(name);
		return allExports.length == 0 ? null : allExports[0];
	}

	ResolverExport[] getExports(String name) {
		List<ResolverExport> results = new ArrayList<ResolverExport>(1); // rare to have more than one
		// it is faster to ask the VersionHashMap for this package name and then compare the exporter to this
		List<ResolverExport> resolverExports = resolver.getResolverExports().get(name);
		for (ResolverExport export : resolverExports)
			if (export.getExporter() == this)
				results.add(export);
		return results.toArray(new ResolverExport[results.size()]);
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
			allGenericRequires[i].clearPossibleSuppliers();

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

	private <T> List<T> getAll(T[] hostEntries, Map<Long, List<T>> fragmentMap) {
		List<T> result = new ArrayList<T>(hostEntries.length);
		for (T entry : hostEntries)
			result.add(entry);
		for (ResolverBundle fragment : fragments) {
			List<T> fragEntries = fragmentMap.get(fragment.bundleID);
			if (fragEntries != null)
				result.addAll(fragEntries);
		}
		return result;
	}

	ResolverImport[] getImportPackages() {
		if (isFragment() || fragments == null || fragments.size() == 0)
			return imports;
		List<ResolverImport> result = getAll(imports, fragmentImports);
		return result.toArray(new ResolverImport[result.size()]);
	}

	ResolverExport[] getExportPackages() {
		if (isFragment() || fragments == null || fragments.size() == 0)
			return exports;
		List<ResolverExport> result = getAll(exports, fragmentExports);
		return result.toArray(new ResolverExport[result.size()]);
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
		if (isFragment() || fragments == null || fragments.size() == 0)
			return genericCapabilities;
		List<GenericCapability> result = getAll(genericCapabilities, fragmentGenericCapabilities);
		return result.toArray(new GenericCapability[result.size()]);
	}

	BundleConstraint[] getRequires() {
		if (isFragment() || fragments == null || fragments.size() == 0)
			return requires;
		List<BundleConstraint> result = getAll(requires, fragmentRequires);
		return result.toArray(new BundleConstraint[result.size()]);
	}

	GenericConstraint[] getGenericRequires() {
		if (isFragment() || fragments == null || fragments.size() == 0)
			return genericReqiures;
		List<GenericConstraint> result = getAll(genericReqiures, fragmentGenericRequires);
		return result.toArray(new GenericConstraint[result.size()]);
	}

	BundleConstraint getRequire(String name) {
		BundleConstraint[] allRequires = getRequires();
		for (int i = 0; i < allRequires.length; i++)
			if (allRequires[i].getVersionConstraint().getName().equals(name))
				return allRequires[i];
		return null;
	}

	public BundleDescription getBundleDescription() {
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
		return "[" + getBundleDescription() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void initFragments() {
		if (fragments == null)
			fragments = new ArrayList<ResolverBundle>(1);
		if (fragmentExports == null)
			fragmentExports = new HashMap<Long, List<ResolverExport>>(1);
		if (fragmentImports == null)
			fragmentImports = new HashMap<Long, List<ResolverImport>>(1);
		if (fragmentRequires == null)
			fragmentRequires = new HashMap<Long, List<BundleConstraint>>(1);
		if (fragmentGenericCapabilities == null)
			fragmentGenericCapabilities = new HashMap<Long, List<GenericCapability>>(1);
		if (fragmentGenericRequires == null)
			fragmentGenericRequires = new HashMap<Long, List<GenericConstraint>>(1);
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

	void attachFragment(ResolverBundle fragment, boolean dynamicAttach) {
		if (isFragment())
			return; // cannot attach to fragments;
		if (!getBundleDescription().attachFragments() || (isResolved() && !getBundleDescription().dynamicFragments()))
			return; // host is restricting attachment
		if (fragment.getHost().getNumPossibleSuppliers() > 0 && !((HostSpecification) fragment.getHost().getVersionConstraint()).isMultiHost())
			return; // fragment is restricting attachment

		ImportPackageSpecification[] newImports = fragment.getBundleDescription().getImportPackages();
		BundleSpecification[] newRequires = fragment.getBundleDescription().getRequiredBundles();
		ExportPackageDescription[] newExports = fragment.getBundleDescription().getExportPackages();
		GenericDescription[] newGenericCapabilities = fragment.getBundleDescription().getGenericCapabilities();
		GenericSpecification[] newGenericRequires = fragment.getBundleDescription().getGenericRequires();

		// if this is not during initialization then check if constraints conflict
		if (dynamicAttach && constraintsConflict(fragment.getBundleDescription(), newImports, newRequires, newGenericRequires))
			return; // do not allow fragments with conflicting constraints
		if (isResolved() && newExports.length > 0)
			fragment.setNewFragmentExports(true);

		initFragments();
		// need to make sure there is not already another version of this fragment 
		// already attached to this host
		for (Iterator<ResolverBundle> iFragments = fragments.iterator(); iFragments.hasNext();) {
			ResolverBundle existingFragment = iFragments.next();
			String bsn = existingFragment.getName();
			if (bsn != null && bsn.equals(fragment.getName()))
				return;
		}
		if (fragments.contains(fragment))
			return;
		fragments.add(fragment);
		fragment.getHost().addPossibleSupplier(this);

		if (newImports.length > 0) {
			ArrayList<ResolverImport> hostImports = new ArrayList<ResolverImport>(newImports.length);
			for (int i = 0; i < newImports.length; i++)
				if (!isImported(newImports[i].getName()))
					hostImports.add(new ResolverImport(this, newImports[i]));
			fragmentImports.put(fragment.bundleID, hostImports);
		}

		if (newRequires.length > 0) {
			ArrayList<BundleConstraint> hostRequires = new ArrayList<BundleConstraint>(newRequires.length);
			for (int i = 0; i < newRequires.length; i++)
				if (!isRequired(newRequires[i].getName()))
					hostRequires.add(new BundleConstraint(this, newRequires[i]));
			fragmentRequires.put(fragment.bundleID, hostRequires);
		}

		if (newGenericRequires.length > 0) {
			ArrayList<GenericConstraint> hostGenericRequires = new ArrayList<GenericConstraint>(newGenericRequires.length);
			for (int i = 0; i < newGenericRequires.length; i++) {
				// only add namespaces that are not osgi.ee
				if (!StateImpl.OSGI_EE_NAMESPACE.equals(newGenericRequires[i].getType()))
					hostGenericRequires.add(new GenericConstraint(this, newGenericRequires[i]));
			}
			if (!hostGenericRequires.isEmpty())
				fragmentGenericRequires.put(fragment.bundleID, hostGenericRequires);
		}

		ArrayList<ResolverExport> hostExports = new ArrayList<ResolverExport>(newExports.length);
		if (newExports.length > 0 && dynamicAttach) {
			for (int i = 0; i < newExports.length; i++) {
				ResolverExport currentExports[] = getExports(newExports[i].getName());
				boolean foundEquivalent = false;
				for (int j = 0; j < currentExports.length && !foundEquivalent; j++) {
					if (equivalentExports(currentExports[j], newExports[i]))
						foundEquivalent = true;
				}
				if (!foundEquivalent) {
					ExportPackageDescription hostExport = new ExportPackageDescriptionImpl(getBundleDescription(), newExports[i]);
					hostExports.add(new ResolverExport(this, hostExport));
				}
			}
			fragmentExports.put(fragment.bundleID, hostExports);
		}

		List<GenericCapability> hostCapabilities = new ArrayList<GenericCapability>(newGenericCapabilities.length);
		if (newGenericCapabilities.length > 0 && dynamicAttach) {
			for (GenericDescription capability : newGenericCapabilities) {
				if (!IdentityNamespace.IDENTITY_NAMESPACE.equals(capability.getType())) {
					GenericDescription hostCapabililty = new GenericDescriptionImpl(getBundleDescription(), capability);
					hostCapabilities.add(new GenericCapability(this, hostCapabililty));
				}
			}
			if (hostCapabilities.size() > 0) {
				fragmentGenericCapabilities.put(fragment.bundleID, hostCapabilities);
				if (isResolved())
					fragment.setNewFragmentCapabilities(true);
			}
		}
		if (dynamicAttach) {
			resolver.getResolverExports().put(hostExports.toArray(new ResolverExport[hostExports.size()]));
			resolver.addGenerics(hostCapabilities.toArray(new GenericCapability[hostCapabilities.size()]));
		}
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

	public static boolean equivalentMaps(Map<String, Object> existingDirectives, Map<String, Object> newDirectives, boolean exactMatch) {
		if (existingDirectives == null && newDirectives == null)
			return true;
		if (existingDirectives == null ? newDirectives != null : newDirectives == null)
			return false;
		if (exactMatch && existingDirectives.size() != newDirectives.size())
			return false;
		for (Iterator<Entry<String, Object>> entries = existingDirectives.entrySet().iterator(); entries.hasNext();) {
			Entry<String, Object> entry = entries.next();
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
			if (importPackageConflict(resolvedExport, newImports[i])) {
				result = true;
				resolver.getState().addResolverError(fragment, ResolverError.FRAGMENT_CONFLICT, newImports[i].toString(), newImports[i]);
			}
		}
		for (int i = 0; i < newRequires.length; i++) {
			BundleConstraint hostRequire = getRequire(newRequires[i].getName());
			ResolverBundle resolvedRequire = (ResolverBundle) (hostRequire == null ? null : hostRequire.getSelectedSupplier());
			if ((resolvedRequire == null && isResolved()) || (resolvedRequire != null && !newRequires[i].isSatisfiedBy(resolvedRequire.getBundleDescription()))) {
				result = true;
				resolver.getState().addResolverError(fragment, ResolverError.FRAGMENT_CONFLICT, newRequires[i].toString(), newRequires[i]);
			}
		}
		// generic constraints cannot conflict; 
		// only check that a fragment does not add generic constraints to an already resolved host
		if (isResolved() && newGenericRequires != null) {
			for (GenericSpecification genericSpecification : newGenericRequires) {
				if (!StateImpl.OSGI_EE_NAMESPACE.equals(genericSpecification.getType())) {
					result = true;
					resolver.getState().addResolverError(fragment, ResolverError.FRAGMENT_CONFLICT, genericSpecification.toString(), genericSpecification);
				}
			}
		}
		return result;
	}

	private boolean importPackageConflict(ResolverExport resolvedExport, ImportPackageSpecification newImport) {
		if (resolvedExport == null)
			return isResolved();
		return !((ImportPackageSpecificationImpl) newImport).isSatisfiedBy(resolvedExport.getExportPackageDescription(), false);
	}

	private void setNewFragmentExports(boolean newFragmentExports) {
		this.newFragmentExports = newFragmentExports;
	}

	boolean isNewFragmentExports() {
		return newFragmentExports;
	}

	private void setNewFragmentCapabilities(boolean newFragmentCapabilities) {
		this.newFragmentCapabilities = newFragmentCapabilities;
	}

	boolean isNewFragmentCapabilities() {
		return newFragmentCapabilities;
	}

	public void detachFromHosts() {
		if (!isFragment()) {
			return;
		}
		VersionSupplier[] hosts = getHost().getPossibleSuppliers();
		if (hosts == null) {
			return;
		}
		for (VersionSupplier possibleHost : hosts) {
			((ResolverBundle) possibleHost).detachFragment(this, null);
		}
	}

	void detachFragment(ResolverBundle fragment, ResolverConstraint reason) {
		if (isFragment())
			return;
		initFragments();

		// must save off old imports and requires before we remove the fragment;
		// this will be used to merge the constraints of the same name from the remaining fragments 
		ResolverImport[] oldImports = getImportPackages();
		BundleConstraint[] oldRequires = getRequires();
		if (!fragments.remove(fragment))
			return;

		fragment.setNewFragmentExports(false);
		fragment.setNewFragmentCapabilities(false);
		fragment.getHost().removePossibleSupplier(this);
		fragmentImports.remove(fragment.bundleID);
		fragmentRequires.remove(fragment.bundleID);
		List<ResolverExport> removedExports = fragmentExports.remove(fragment.bundleID);
		fragmentGenericRequires.remove(fragment.bundleID);
		List<GenericCapability> removedCapabilities = fragmentGenericCapabilities.remove(fragment.bundleID);
		if (reason != null) {
			// the fragment is being detached because one of its imports or requires cannot be resolved;
			// we need to check the remaining fragment constraints to make sure they do not have
			// the same unresolved constraint.
			// bug 353103: must make a snapshot to avoid ConcurrentModificationException
			ResolverBundle[] remainingFrags = fragments.toArray(new ResolverBundle[fragments.size()]);
			for (ResolverBundle remainingFrag : remainingFrags) {
				List<ResolverImport> additionalImports = new ArrayList<ResolverImport>(0);
				List<BundleConstraint> additionalRequires = new ArrayList<BundleConstraint>(0);
				if (hasUnresolvedConstraint(reason, fragment, remainingFrag, oldImports, oldRequires, additionalImports, additionalRequires))
					continue;
				// merge back the additional imports or requires which the detached fragment has in common with the remaining fragment
				if (additionalImports.size() > 0) {
					List<ResolverImport> remainingImports = fragmentImports.get(remainingFrag.bundleID);
					if (remainingImports == null)
						fragmentImports.put(remainingFrag.bundleID, additionalImports);
					else
						remainingImports.addAll(additionalImports);
				}
				if (additionalRequires.size() > 0) {
					List<BundleConstraint> remainingRequires = fragmentRequires.get(remainingFrag.bundleID);
					if (remainingRequires == null)
						fragmentRequires.put(remainingFrag.bundleID, additionalRequires);
					else
						remainingRequires.addAll(additionalRequires);
				}
			}
		}
		ResolverExport[] results = removedExports == null ? new ResolverExport[0] : removedExports.toArray(new ResolverExport[removedExports.size()]);
		for (int i = 0; i < results.length; i++)
			// TODO this is a hack; need to figure out how to indicate that a fragment export is no longer attached
			results[i].setSubstitute(results[i]);
		resolver.getResolverExports().remove(results);
		if (removedCapabilities != null)
			resolver.removeGenerics(removedCapabilities.toArray(new GenericCapability[removedCapabilities.size()]));
	}

	private boolean hasUnresolvedConstraint(ResolverConstraint reason, ResolverBundle detachedFragment, ResolverBundle remainingFragment, ResolverImport[] oldImports, BundleConstraint[] oldRequires, List<ResolverImport> additionalImports, List<BundleConstraint> additionalRequires) {
		ImportPackageSpecification[] remainingFragImports = remainingFragment.getBundleDescription().getImportPackages();
		BundleSpecification[] remainingFragRequires = remainingFragment.getBundleDescription().getRequiredBundles();
		VersionConstraint[] constraints;
		if (reason instanceof ResolverImport)
			constraints = remainingFragImports;
		else
			constraints = remainingFragRequires;
		for (int i = 0; i < constraints.length; i++)
			if (reason.getName().equals(constraints[i].getName())) {
				detachFragment(remainingFragment, reason);
				return true;
			}
		for (int i = 0; i < oldImports.length; i++) {
			if (oldImports[i].getVersionConstraint().getBundle() != detachedFragment.getBundleDescription())
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
			if (oldRequires[i].getVersionConstraint().getBundle() != detachedFragment.getBundleDescription())
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
		ResolverBundle[] allFragments = fragments.toArray(new ResolverBundle[fragments.size()]);
		for (int i = 0; i < allFragments.length; i++)
			detachFragment(allFragments[i], null);
		fragments = null;
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
	public int compareTo(ResolverBundle o) {
		String bsn = getName();
		String otherBsn = o.getName();
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
