/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.*;

/**
 * An implementation for the StateHelper API. Access to this implementation is
 * provided  by the PlatformAdmin. Since this helper is a general facility for
 * state manipulation, it should not be tied to any implementation details.
 */
public final class StateHelperImpl implements StateHelper {
	private static final StateHelper instance = new StateHelperImpl();

	/**
	 * @see StateHelper
	 */
	public BundleDescription[] getDependentBundles(BundleDescription[] bundles) {
		if (bundles == null || bundles.length == 0)
			return new BundleDescription[0];

		Set reachable = new HashSet(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			if (!bundles[i].isResolved())
				continue;
			addDependentBundles(bundles[i], reachable);
		}
		return (BundleDescription[]) reachable.toArray(new BundleDescription[reachable.size()]);
	}

	private void addDependentBundles(BundleDescription bundle, Set reachable) {
		if (reachable.contains(bundle))
			return;
		reachable.add(bundle);
		BundleDescription[] dependents = bundle.getDependents();
		for (int i = 0; i < dependents.length; i++)
			addDependentBundles(dependents[i], reachable);
	}

	public BundleDescription[] getPrerequisites(BundleDescription[] bundles) {
		if (bundles == null || bundles.length == 0)
			return new BundleDescription[0];
		Set reachable = new HashSet(bundles.length);
		for (int i = 0; i < bundles.length; i++)
			addPrerequisites(bundles[i], reachable);
		return (BundleDescription[]) reachable.toArray(new BundleDescription[reachable.size()]);
	}

	private void addPrerequisites(BundleDescription bundle, Set reachable) {
		if (reachable.contains(bundle))
			return;
		reachable.add(bundle);
		List depList = ((BundleDescriptionImpl) bundle).getBundleDependencies();
		BundleDescription[] dependencies = (BundleDescription[]) depList.toArray(new BundleDescription[depList.size()]);
		for (int i = 0; i < dependencies.length; i++)
			addPrerequisites(dependencies[i], reachable);
	}

	private Map getExportedPackageMap(State state) {
		Map result = new HashMap(11);
		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			ExportPackageDescription[] packages = bundles[i].getExportPackages();
			for (int j = 0; j < packages.length; j++) {
				ExportPackageDescription description = packages[j];
				Set exports = (Set) result.get(description.getName());
				if (exports == null) {
					exports = new HashSet(1);
					result.put(description.getName(), exports);
				}
				exports.add(description);
			}
		}
		return result;
	}

	private Map getGenericsMap(State state, boolean resolved) {
		Map result = new HashMap(11);
		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (resolved && !bundles[i].isResolved())
				continue; // discard unresolved bundles
			GenericDescription[] generics = bundles[i].getGenericCapabilities();
			for (int j = 0; j < generics.length; j++) {
				GenericDescription description = generics[j];
				Set genericSet = (Set) result.get(description.getName());
				if (genericSet == null) {
					genericSet = new HashSet(1);
					result.put(description.getName(), genericSet);
				}
				genericSet.add(description);
			}
		}
		return result;
	}

	private VersionConstraint[] getUnsatisfiedLeaves(State state, BundleDescription[] bundles) {
		Map packages = getExportedPackageMap(state);
		Map generics = getGenericsMap(state, false);
		HashSet result = new HashSet(11);
		ArrayList bundleList = new ArrayList(bundles.length);
		for (int i = 0; i < bundles.length; i++)
			bundleList.add(bundles[i]);
		for (int i = 0; i < bundleList.size(); i++) {
			BundleDescription description = (BundleDescription) bundleList.get(i);
			VersionConstraint[] constraints = getUnsatisfiedConstraints(description);
			for (int j = 0; j < constraints.length; j++) {
				VersionConstraint constraint = constraints[j];
				BaseDescription satisfied = null;
				if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
					BundleDescription[] suppliers = state.getBundles(constraint.getName());
					for (int k = 0; k < suppliers.length && satisfied == null; k++)
						satisfied = constraint.isSatisfiedBy(suppliers[k]) ? suppliers[k] : null;
				} else if (constraint instanceof ImportPackageSpecification) {
					Set exports = (Set) packages.get(constraint.getName());
					if (exports != null)
						for (Iterator iter = exports.iterator(); iter.hasNext() && satisfied == null;) {
							ExportPackageDescription exportDesc = (ExportPackageDescription) iter.next();
							satisfied = constraint.isSatisfiedBy(exportDesc) ? exportDesc : null;
						}
				} else if (constraint instanceof GenericSpecification) {
					Set genericSet = (Set) generics.get(constraint.getName());
					if (genericSet != null)
						for (Iterator iter = genericSet.iterator(); iter.hasNext() && satisfied == null;) {
							GenericDescription genDesc = (GenericDescription) iter.next();
							satisfied = constraint.isSatisfiedBy(genDesc) ? genDesc : null;
						}
				}
				if (satisfied == null)
					result.add(constraint);
				else if (!satisfied.getSupplier().isResolved() && !bundleList.contains(satisfied.getSupplier()))
					bundleList.add(satisfied.getSupplier());
			}
		}
		return (VersionConstraint[]) result.toArray(new VersionConstraint[result.size()]);

	}

	public VersionConstraint[] getUnsatisfiedLeaves(BundleDescription[] bundles) {
		if (bundles.length == 0)
			return new VersionConstraint[0];
		State state = bundles[0].getContainingState();
		return getUnsatisfiedLeaves(state, bundles);
	}

	/**
	 * @see StateHelper
	 */
	public VersionConstraint[] getUnsatisfiedConstraints(BundleDescription bundle) {
		State containingState = bundle.getContainingState();
		if (containingState == null)
			// it is a bug in the client to call this method when not attached to a state
			throw new IllegalStateException("Does not belong to a state"); //$NON-NLS-1$		
		List unsatisfied = new ArrayList();
		HostSpecification host = bundle.getHost();
		if (host != null)
			if (!host.isResolved() && !isResolvable(host))
				unsatisfied.add(host);
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++)
			if (!requiredBundles[i].isResolved() && !isResolvable(requiredBundles[i]))
				unsatisfied.add(requiredBundles[i]);
		ImportPackageSpecification[] packages = bundle.getImportPackages();
		for (int i = 0; i < packages.length; i++)
			if (!packages[i].isResolved() && !isResolvable(packages[i]))
				unsatisfied.add(packages[i]);
		GenericSpecification[] generics = bundle.getGenericRequires();
		for (int i = 0; i < generics.length; i++)
			if (!generics[i].isResolved() && !isResolvable(generics[i]))
				unsatisfied.add(generics[i]);
		NativeCodeSpecification nativeCode = bundle.getNativeCodeSpecification();
		if (nativeCode != null && !nativeCode.isResolved())
			unsatisfied.add(nativeCode);
		return (VersionConstraint[]) unsatisfied.toArray(new VersionConstraint[unsatisfied.size()]);
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(ImportPackageSpecification constraint) {
		ExportPackageDescription[] exports = constraint.getBundle().getContainingState().getExportedPackages();
		for (int i = 0; i < exports.length; i++)
			if (constraint.isSatisfiedBy(exports[i]))
				return true;
		return false;
	}

	private boolean isResolvable(GenericSpecification constraint) {
		Map genericCapabilities = getGenericsMap(constraint.getBundle().getContainingState(), true);
		Set genericSet = (Set) genericCapabilities.get(constraint.getName());
		if (genericSet == null)
			return false;
		for (Iterator iter = genericSet.iterator(); iter.hasNext();)
			if (constraint.isSatisfiedBy((GenericDescription) iter.next()))
				return true;
		return false;
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(BundleSpecification specification) {
		return isBundleConstraintResolvable(specification);
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(HostSpecification specification) {
		return isBundleConstraintResolvable(specification);
	}

	/*
	 * Returns whether a bundle specification/host specification can be resolved.
	 */
	private boolean isBundleConstraintResolvable(VersionConstraint constraint) {
		BundleDescription[] availableBundles = constraint.getBundle().getContainingState().getBundles(constraint.getName());
		for (int i = 0; i < availableBundles.length; i++)
			if (availableBundles[i].isResolved() && constraint.isSatisfiedBy(availableBundles[i]))
				return true;
		return false;
	}

	public Object[][] sortBundles(BundleDescription[] toSort) {
		List references = new ArrayList(toSort.length);
		for (int i = 0; i < toSort.length; i++)
			if (toSort[i].isResolved())
				buildReferences(toSort[i], references);
		Object[][] cycles = ComputeNodeOrder.computeNodeOrder(toSort, (Object[][]) references.toArray(new Object[references.size()][]));
		if (cycles.length == 0)
			return cycles;
		// fix up host/fragment orders (bug 184127)
		for (int i = 0; i < cycles.length; i++) {
			for (int j = 0; j < cycles[i].length; j++) {
				BundleDescription fragment = (BundleDescription) cycles[i][j];
				if (fragment.getHost() == null)
					continue;
				BundleDescription host = (BundleDescription) fragment.getHost().getSupplier();
				if (host == null)
					continue;
				fixFragmentOrder(host, fragment, toSort);
			}
		}
		return cycles;
	}

	private void fixFragmentOrder(BundleDescription host, BundleDescription fragment, BundleDescription[] toSort) {
		int hostIndex = -1;
		int fragIndex = -1;
		for (int i = 0; i < toSort.length && (hostIndex == -1 || fragIndex == -1); i++) {
			if (toSort[i] == host)
				hostIndex = i;
			else if (toSort[i] == fragment)
				fragIndex = i;
		}
		if (fragIndex > -1 && fragIndex < hostIndex) {
			for (int i = fragIndex; i < hostIndex; i++)
				toSort[i] = toSort[i + 1];
			toSort[hostIndex] = fragment;
		}
	}

	private void buildReferences(BundleDescription description, List references) {
		HostSpecification host = description.getHost();
		// it is a fragment
		if (host != null) {
			// just create a dependency between fragment and host
			if (host.getHosts() != null) {
				BundleDescription[] hosts = host.getHosts();
				for (int i = 0; i < hosts.length; i++)
					if (hosts[i] != description)
						references.add(new Object[] {description, hosts[i]});
			}
		} else {
			// it is a host
			buildReferences(description, ((BundleDescriptionImpl) description).getBundleDependencies(), references);
		}
	}

	private void buildReferences(BundleDescription description, List dependencies, List references) {
		for (Iterator iter = dependencies.iterator(); iter.hasNext();)
			addReference(description, (BundleDescription) iter.next(), references);
	}

	private void addReference(BundleDescription description, BundleDescription reference, List references) {
		// build the reference from the description
		if (description == reference || reference == null)
			return;
		BundleDescription[] fragments = reference.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i].isResolved()) {
				ExportPackageDescription[] exports = fragments[i].getExportPackages();
				if (exports.length > 0)
					references.add(new Object[] {description, fragments[i]});
			}
		}
		references.add(new Object[] {description, reference});
	}

	public ExportPackageDescription[] getVisiblePackages(BundleDescription bundle) {
		return getVisiblePackages(bundle, 0);
	}

	public ExportPackageDescription[] getVisiblePackages(BundleDescription bundle, int options) {
		StateImpl state = (StateImpl) bundle.getContainingState();
		boolean strict = false;
		if (state != null)
			strict = state.inStrictMode();
		BundleDescription host = (BundleDescription) (bundle.getHost() == null ? bundle : bundle.getHost().getSupplier());
		ArrayList orderedPkgList = new ArrayList(); // list of all ExportPackageDescriptions that are visible (ArrayList is used to keep order)
		Set pkgSet = new HashSet();
		Set importList = new HashSet(); // list of package names which are directly imported
		// get the list of directly imported packages first.
		ImportsHolder imports = new ImportsHolder(bundle, options);
		for (int i = 0; i < imports.getSize(); i++) {
			ExportPackageDescription pkgSupplier = imports.getSupplier(i);
			if (pkgSupplier == null || pkgSupplier.getExporter() == host) // do not return the bundle'sr own imports
				continue;
			if (!isSystemExport(pkgSupplier, options) && !pkgSet.contains(pkgSupplier)) {
				orderedPkgList.add(pkgSupplier);
				pkgSet.add(pkgSupplier);
			}
			// get the sources of the required bundles of the exporter
			BundleSpecification[] requires = pkgSupplier.getExporter().getRequiredBundles();
			Set visited = new HashSet();
			visited.add(bundle); // always add self to prevent recursing into self
			Set importNames = new HashSet(1);
			importNames.add(imports.getName(i));
			for (int j = 0; j < requires.length; j++) {
				BundleDescription bundleSupplier = (BundleDescription) requires[j].getSupplier();
				if (bundleSupplier != null)
					getPackages(bundleSupplier, bundle.getSymbolicName(), importList, orderedPkgList, pkgSet, visited, strict, importNames, options);
			}
			importList.add(imports.getName(i)); // be sure to add to direct import list

		}
		// now find all the packages that are visible from required bundles
		RequiresHolder requires = new RequiresHolder(bundle, options);
		Set visited = new HashSet(requires.getSize());
		visited.add(bundle); // always add self to prevent recursing into self
		for (int i = 0; i < requires.getSize(); i++) {
			BundleDescription bundleSupplier = requires.getSupplier(i);
			if (bundleSupplier != null)
				getPackages(bundleSupplier, bundle.getSymbolicName(), importList, orderedPkgList, pkgSet, visited, strict, null, options);
		}
		return (ExportPackageDescription[]) orderedPkgList.toArray(new ExportPackageDescription[orderedPkgList.size()]);
	}

	private void getPackages(BundleDescription requiredBundle, String symbolicName, Set importList, ArrayList orderedPkgList, Set pkgSet, Set visited, boolean strict, Set pkgNames, int options) {
		if (visited.contains(requiredBundle))
			return; // prevent duplicate entries and infinate loops incase of cycles
		visited.add(requiredBundle);
		// add all the exported packages from the required bundle; take x-friends into account.
		ExportPackageDescription[] substitutedExports = requiredBundle.getSubstitutedExports();
		ExportPackageDescription[] imports = requiredBundle.getResolvedImports();
		Set substituteNames = null; // a temporary set used to scope packages we get from getPackages
		for (int i = 0; i < substitutedExports.length; i++) {
			if ((pkgNames == null || pkgNames.contains(substitutedExports[i].getName()))) {
				for (int j = 0; j < imports.length; j++) {
					if (substitutedExports[i].getName().equals(imports[j].getName()) && !pkgSet.contains(imports[j])) {
						if (substituteNames == null)
							substituteNames = new HashSet(1);
						else
							substituteNames.clear();
						// substituteNames is a set of one package containing the single substitute we are trying to get the source for
						substituteNames.add(substitutedExports[i].getName());
						getPackages(imports[j].getSupplier(), symbolicName, importList, orderedPkgList, pkgSet, new HashSet(0), strict, substituteNames, options);
					}
				}
			}
		}
		importList = substitutedExports.length == 0 ? importList : new HashSet(importList);
		for (int i = 0; i < substitutedExports.length; i++)
			// we add the package name to the import list to prevent required bundles from adding more sources
			importList.add(substitutedExports[i].getName());

		ExportPackageDescription[] exports = requiredBundle.getSelectedExports();
		HashSet exportNames = new HashSet(exports.length); // set is used to improve performance of duplicate check.
		for (int i = 0; i < exports.length; i++)
			if ((pkgNames == null || pkgNames.contains(exports[i].getName())) && !isSystemExport(exports[i], options) && isFriend(symbolicName, exports[i], strict) && !importList.contains(exports[i].getName()) && !pkgSet.contains(exports[i])) {
				if (!exportNames.contains(exports[i].getName())) {
					// only add the first export
					orderedPkgList.add(exports[i]);
					pkgSet.add(exports[i]);
					exportNames.add(exports[i].getName());
				}
			}
		// now look for exports from the required bundle.
		RequiresHolder requiredBundles = new RequiresHolder(requiredBundle, options);
		for (int i = 0; i < requiredBundles.getSize(); i++) {
			if (requiredBundles.getSupplier(i) == null)
				continue;
			if (requiredBundles.isExported(i)) {
				// looking for a specific package and that package is exported by this bundle or adding all packages from a reexported bundle
				getPackages(requiredBundles.getSupplier(i), symbolicName, importList, orderedPkgList, pkgSet, visited, strict, pkgNames, options);
			} else if (exportNames.size() > 0) {
				// adding any exports from required bundles which we also export
				Set tmpVisited = new HashSet();
				getPackages(requiredBundles.getSupplier(i), symbolicName, importList, orderedPkgList, pkgSet, tmpVisited, strict, exportNames, options);
			}
		}
	}

	private boolean isSystemExport(ExportPackageDescription export, int options) {
		if ((options & VISIBLE_INCLUDE_EE_PACKAGES) != 0)
			return false;
		return ((Integer) export.getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue() >= 0;
	}

	private boolean isFriend(String consumerBSN, ExportPackageDescription export, boolean strict) {
		if (!strict)
			return true; // ignore friends rules if not in strict mode
		String[] friends = (String[]) export.getDirective(Constants.FRIENDS_DIRECTIVE);
		if (friends == null)
			return true; // no x-friends means it is wide open
		for (int i = 0; i < friends.length; i++)
			if (friends[i].equals(consumerBSN))
				return true; // the consumer is a friend
		return false;
	}

	public int getAccessCode(BundleDescription bundle, ExportPackageDescription export) {
		if (((Boolean) export.getDirective(Constants.INTERNAL_DIRECTIVE)).booleanValue())
			return ACCESS_DISCOURAGED;
		if (!isFriend(bundle.getSymbolicName(), export, true)) // pass strict here so that x-friends is processed
			return ACCESS_DISCOURAGED;
		return ACCESS_ENCOURAGED;
	}

	public static StateHelper getInstance() {
		return instance;
	}

}

/*
 * This class is used to encapsulate the import packages of a bundle used by getVisiblePackages(). If the method is called with the option 
 * VISIBLE_INCLUDE_ALL_HOST_WIRES, it uses resolved import packages to find all visible packages by a bundle. Called without this option, 
 * it uses imported packages instead of resolved imported packages and does not consider resolved dynamic imports. 
 * ImportsHolder serves to hide which of these is used, so that the body of getVisiblePackages() does not become full of checks.
 * 
 */
class ImportsHolder {
	private final ImportPackageSpecification[] importedPackages;
	private final ExportPackageDescription[] resolvedImports;
	private final boolean isUsingResolved;

	// Depending on the options used, either importedPackages or resolvedImports is initialize, but not both. 
	ImportsHolder(BundleDescription bundle, int options) {
		isUsingResolved = (options & StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES) != 0;
		if (isUsingResolved) {
			importedPackages = null;
			resolvedImports = bundle.getResolvedImports();
		} else {
			importedPackages = bundle.getImportPackages();
			resolvedImports = null;
		}
	}

	ExportPackageDescription getSupplier(int index) {
		if (isUsingResolved)
			return resolvedImports[index];
		return (ExportPackageDescription) importedPackages[index].getSupplier();
	}

	String getName(int index) {
		if (isUsingResolved)
			return resolvedImports[index].getName();
		return importedPackages[index].getName();
	}

	int getSize() {
		if (isUsingResolved)
			return resolvedImports.length;
		return importedPackages.length;
	}
}

/*
 * This class is used to encapsulate the required bundles by a bundle, used by getVisiblePackages(). If the method is called with the option 
 * VISIBLE_INCLUDE_ALL_HOST_WIRES, it uses resolved required bundles to find all visible packages by a bundle. Called without this option, 
 * it uses required bundles instead of resolved required bundles and does not consider the constraints from fragments. 
 * RequiresHolder serves to hide which of these is used.  
 */
class RequiresHolder {
	private final BundleSpecification[] requiredBundles;
	private final BundleDescription[] resolvedRequires;
	private final boolean isUsingResolved;
	private final Map resolvedBundlesExported;

	// Depending on the options used, either requiredBundles or resolvedRequires is initialize, but not both.
	RequiresHolder(BundleDescription bundle, int options) {
		isUsingResolved = (options & StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES) != 0;
		if (isUsingResolved) {
			requiredBundles = null;
			resolvedBundlesExported = new HashMap();
			resolvedRequires = bundle.getResolvedRequires();
			determineRequiresVisibility(bundle);
		} else {
			requiredBundles = bundle.getRequiredBundles();
			resolvedBundlesExported = null;
			resolvedRequires = null;
		}
	}

	BundleDescription getSupplier(int index) {
		if (isUsingResolved)
			return resolvedRequires[index];
		return (BundleDescription) requiredBundles[index].getSupplier();
	}

	boolean isExported(int index) {
		if (isUsingResolved)
			return ((Boolean) resolvedBundlesExported.get(resolvedRequires[index])).booleanValue();
		return requiredBundles[index].isExported();
	}

	int getSize() {
		if (isUsingResolved)
			return resolvedRequires.length;
		return requiredBundles.length;
	}

	/*
	 * This method determines for all resolved required bundles if they are reexported.
	 * Fragment bundles are also considered.
	 */
	private void determineRequiresVisibility(BundleDescription bundle) {
		BundleSpecification[] required = bundle.getRequiredBundles();
		HashSet resolved = new HashSet();

		for (int i = 0; i < resolvedRequires.length; i++) {
			resolved.add(resolvedRequires[i]);
		}

		// Get the visibility of all directly required bundles
		for (int i = 0; i < required.length; i++) {
			if (required[i].getSupplier() != null) {
				resolvedBundlesExported.put(required[i].getSupplier(), new Boolean(required[i].isExported()));
				resolved.remove(required[i].getSupplier());
			}
		}

		BundleDescription[] fragments = bundle.getFragments();

		// Get the visibility of resolved required bundles, which come from fragments
		if (resolved.size() > 0) {
			for (int i = 0; i < fragments.length; i++) {
				BundleSpecification[] fragmentRequiredBundles = fragments[i].getRequiredBundles();
				for (int j = 0; j < fragmentRequiredBundles.length; j++) {
					if (resolved.contains(fragmentRequiredBundles[i].getSupplier())) {
						resolvedBundlesExported.put(fragmentRequiredBundles[i].getSupplier(), new Boolean(fragmentRequiredBundles[i].isExported()));
						resolved.remove(fragmentRequiredBundles[i].getSupplier());
					}
				}
				if (resolved.size() == 0) {
					break;
				}
			}
		}
	}
}
