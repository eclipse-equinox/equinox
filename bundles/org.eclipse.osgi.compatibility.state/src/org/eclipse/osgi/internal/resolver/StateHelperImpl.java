/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.container.ComputeNodeOrder;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

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

		Set<BundleDescription> reachable = new LinkedHashSet<>(bundles.length);
		for (BundleDescription bundle : bundles) {
			if (!bundle.isResolved()) {
				continue;
			}
			addDependentBundles(bundle, reachable);
		}
		return reachable.toArray(new BundleDescription[reachable.size()]);
	}

	private void addDependentBundles(BundleDescription bundle, Set<BundleDescription> reachable) {
		if (reachable.contains(bundle))
			return;
		reachable.add(bundle);
		BundleDescription[] dependents = bundle.getDependents();
		for (BundleDescription dependent : dependents) {
			addDependentBundles(dependent, reachable);
		}
	}

	public BundleDescription[] getPrerequisites(BundleDescription[] bundles) {
		if (bundles == null || bundles.length == 0)
			return new BundleDescription[0];
		Set<BundleDescription> reachable = new LinkedHashSet<>(bundles.length);
		for (BundleDescription bundle : bundles) {
			addPrerequisites(bundle, reachable);
		}
		return reachable.toArray(new BundleDescription[reachable.size()]);
	}

	private void addPrerequisites(BundleDescription bundle, Set<BundleDescription> reachable) {
		if (reachable.contains(bundle))
			return;
		reachable.add(bundle);
		List<BundleDescription> depList = ((BundleDescriptionImpl) bundle).getBundleDependencies();
		BundleDescription[] dependencies = depList.toArray(new BundleDescription[depList.size()]);
		for (BundleDescription dependency : dependencies) {
			addPrerequisites(dependency, reachable);
		}
	}

	private Map<String, List<ExportPackageDescription>> getExportedPackageMap(State state) {
		Map<String, List<ExportPackageDescription>> result = new HashMap<>();
		BundleDescription[] bundles = state.getBundles();
		for (BundleDescription bundle : bundles) {
			ExportPackageDescription[] packages = bundle.getExportPackages();
			for (ExportPackageDescription description : packages) {
				List<ExportPackageDescription> exports = result.get(description.getName());
				if (exports == null) {
					exports = new ArrayList<>();
					result.put(description.getName(), exports);
				}
				exports.add(description);
			}
		}
		return result;
	}

	private Map<String, List<GenericDescription>> getGenericsMap(State state, boolean resolved) {
		Map<String, List<GenericDescription>> result = new HashMap<>();
		BundleDescription[] bundles = state.getBundles();
		for (BundleDescription bundle : bundles) {
			if (resolved && !bundle.isResolved()) {
				continue; // discard unresolved bundles
			}
			GenericDescription[] generics = bundle.getGenericCapabilities();
			for (GenericDescription description : generics) {
				List<GenericDescription> genericList = result.get(description.getName());
				if (genericList == null) {
					genericList = new ArrayList<>(1);
					result.put(description.getName(), genericList);
				}
				genericList.add(description);
			}
		}
		return result;
	}

	private VersionConstraint[] getUnsatisfiedLeaves(State state, BundleDescription[] bundles, ResolverHook hook) {
		Map<String, List<ExportPackageDescription>> packages = getExportedPackageMap(state);
		Map<String, List<GenericDescription>> generics = getGenericsMap(state, false);
		Set<VersionConstraint> result = new HashSet<>();
		List<BundleDescription> bundleList = new ArrayList<>(bundles.length);
		Collections.addAll(bundleList, bundles);
		for (int i = 0; i < bundleList.size(); i++) {
			BundleDescription description = bundleList.get(i);
			VersionConstraint[] constraints = getUnsatisfiedConstraints(description, hook);
			for (VersionConstraint constraint : constraints) {
				Collection<BaseDescription> satisfied = null;
				if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
					BundleDescription[] suppliers = state.getBundles(constraint.getName());
					satisfied = getPossibleCandidates(constraint, suppliers, constraint instanceof HostSpecification ? BundleRevision.HOST_NAMESPACE : null, hook, false);
				} else if (constraint instanceof ImportPackageSpecification) {
					List<ExportPackageDescription> exports = packages.get(constraint.getName());
					if (exports != null)
						satisfied = getPossibleCandidates(constraint, exports.toArray(new BaseDescription[exports.size()]), null, hook, false);
				} else if (constraint instanceof GenericSpecification) {
					List<GenericDescription> genericSet = generics.get(constraint.getName());
					if (genericSet != null)
						satisfied = getPossibleCandidates(constraint, genericSet.toArray(new BaseDescription[genericSet.size()]), null, hook, false);
				}
				if (satisfied == null || satisfied.isEmpty()) {
					result.add(constraint);
				} else {
					for (BaseDescription baseDescription : satisfied) {
						if (!baseDescription.getSupplier().isResolved() && !bundleList.contains(baseDescription.getSupplier())) {
							bundleList.add(baseDescription.getSupplier());
							// TODO only add the first supplier that is not resolved; 
							// this is the previous behavior before the fix for bug 333071; should consider adding all unresolved
							break;
						}
					}
				}
			}
		}
		return result.toArray(new VersionConstraint[result.size()]);

	}

	public VersionConstraint[] getUnsatisfiedLeaves(BundleDescription[] bundles) {
		if (bundles.length == 0)
			return new VersionConstraint[0];
		State state = bundles[0].getContainingState();
		ResolverHook hook = beginHook(state, Arrays.asList((BundleRevision[]) bundles));
		try {
			return getUnsatisfiedLeaves(state, bundles, hook);
		} finally {
			if (hook != null)
				hook.end();
		}
	}

	private ResolverHook beginHook(State state, Collection<BundleRevision> triggers) {
		if (!(state instanceof StateImpl))
			return null;
		ResolverHookFactory hookFactory = ((StateImpl) state).getResolverHookFactory();
		return hookFactory == null ? null : hookFactory.begin(triggers);
	}

	/**
	 * @see StateHelper
	 */
	public VersionConstraint[] getUnsatisfiedConstraints(BundleDescription bundle) {
		ResolverHook hook = beginHook(bundle.getContainingState(), Arrays.asList(new BundleRevision[] {bundle}));
		try {
			return getUnsatisfiedConstraints(bundle, hook);
		} finally {
			if (hook != null)
				hook.end();
		}
	}

	private VersionConstraint[] getUnsatisfiedConstraints(BundleDescription bundle, ResolverHook hook) {
		State containingState = bundle.getContainingState();
		if (containingState == null)
			// it is a bug in the client to call this method when not attached to a state
			throw new IllegalStateException("Does not belong to a state"); //$NON-NLS-1$
		List<VersionConstraint> unsatisfied = new ArrayList<>();
		HostSpecification host = bundle.getHost();
		if (host != null)
			if (!host.isResolved() && !isBundleConstraintResolvable(host, BundleRevision.HOST_NAMESPACE, hook))
				unsatisfied.add(host);
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (BundleSpecification requiredBundle : requiredBundles) {
			if (!requiredBundle.isResolved() && !isBundleConstraintResolvable(requiredBundle, null, hook)) {
				unsatisfied.add(requiredBundle);
			}
		}
		ImportPackageSpecification[] packages = bundle.getImportPackages();
		for (ImportPackageSpecification importSpecification : packages) {
			if (!importSpecification.isResolved() && !isResolvable(importSpecification, hook)) {
				if (bundle.isResolved()) {
					// if the bundle is resolved the check if the import is option.
					// Here we assume that an unresolved mandatory import must have been dropped
					// in favor of an export from the same bundle (bug 338240)
					if (!ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(importSpecification.getDirective(Constants.RESOLUTION_DIRECTIVE))) {
						continue;
					}
				}
				unsatisfied.add(importSpecification);
			}
		}
		GenericSpecification[] generics = bundle.getGenericRequires();
		for (GenericSpecification generic : generics) {
			if (!generic.isResolved() && !isResolvable(generic, hook)) {
				unsatisfied.add(generic);
			}
		}
		NativeCodeSpecification nativeCode = bundle.getNativeCodeSpecification();
		if (nativeCode != null && !nativeCode.isResolved())
			unsatisfied.add(nativeCode);
		return unsatisfied.toArray(new VersionConstraint[unsatisfied.size()]);
	}

	private ArrayMap<BundleCapability, BaseDescription> asArrayMap(List<BaseDescription> descriptions, String namespace) {
		List<BundleCapability> capabilities = new ArrayList<>(descriptions.size());
		for (BaseDescription description : descriptions)
			capabilities.add(((BaseDescriptionImpl) description).getCapability(namespace));
		return new ArrayMap<>(capabilities, descriptions);
	}

	private List<BaseDescription> getPossibleCandidates(VersionConstraint constraint, BaseDescription[] descriptions, String namespace, ResolverHook hook, boolean resolved) {
		List<BaseDescription> candidates = new ArrayList<>();
		for (BaseDescription description : descriptions) {
			if ((!resolved || description.getSupplier().isResolved()) && constraint.isSatisfiedBy(description)) {
				candidates.add(description);
			}
		}
		if (hook != null)
			hook.filterMatches(constraint.getRequirement(), asArrayMap(candidates, namespace));
		return candidates;
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(ImportPackageSpecification constraint) {
		ResolverHook hook = beginHook(constraint.getBundle().getContainingState(), Arrays.asList(new BundleRevision[] {constraint.getBundle()}));
		try {
			return isResolvable(constraint, hook);
		} finally {
			if (hook != null)
				hook.end();
		}
	}

	private boolean isResolvable(ImportPackageSpecification constraint, ResolverHook hook) {
		ExportPackageDescription[] exports = constraint.getBundle().getContainingState().getExportedPackages();
		return getPossibleCandidates(constraint, exports, null, hook, true).size() > 0;
	}

	private boolean isResolvable(GenericSpecification constraint, ResolverHook hook) {
		Map<String, List<GenericDescription>> genericCapabilities = getGenericsMap(constraint.getBundle().getContainingState(), true);
		List<GenericDescription> genericList = genericCapabilities.get(constraint.getName());
		if (genericList == null)
			return false;
		return getPossibleCandidates(constraint, genericList.toArray(new BaseDescription[genericList.size()]), null, hook, true).size() > 0;
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(BundleSpecification specification) {
		return isBundleConstraintResolvable(specification, null);
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(HostSpecification specification) {
		return isBundleConstraintResolvable(specification, BundleRevision.HOST_NAMESPACE);
	}

	/*
	 * Returns whether a bundle specification/host specification can be resolved.
	 */
	private boolean isBundleConstraintResolvable(VersionConstraint constraint, String namespace) {
		ResolverHook hook = beginHook(constraint.getBundle().getContainingState(), Arrays.asList(new BundleRevision[] {constraint.getBundle()}));
		try {
			return isBundleConstraintResolvable(constraint, namespace, hook);
		} finally {
			if (hook != null)
				hook.end();
		}
	}

	private boolean isBundleConstraintResolvable(VersionConstraint constraint, String namespace, ResolverHook hook) {
		BundleDescription[] availableBundles = constraint.getBundle().getContainingState().getBundles(constraint.getName());
		return getPossibleCandidates(constraint, availableBundles, namespace, hook, true).size() > 0;
	}

	public Object[][] sortBundles(BundleDescription[] toSort) {
		List<Object[]> references = new ArrayList<>(toSort.length);
		for (BundleDescription toAddReference : toSort) {
			if (toAddReference.isResolved()) {
				buildReferences(toAddReference, references);
			}
		}
		Object[][] cycles = ComputeNodeOrder.computeNodeOrder(toSort, references.toArray(new Object[references.size()][]));
		if (cycles.length == 0)
			return cycles;
		// fix up host/fragment orders (bug 184127)
		for (Object[] cycle : cycles) {
			for (Object possibleFragment : cycle) {
				BundleDescription fragment = (BundleDescription) possibleFragment;
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

	private void buildReferences(BundleDescription description, List<Object[]> references) {
		HostSpecification hostSpecification = description.getHost();
		// it is a fragment
		if (hostSpecification != null) {
			// just create a dependencies for non-payload requirements (osgi.wiring.host and osgi.ee)
			if (hostSpecification.getHosts() != null) {
				BundleDescription[] hosts = hostSpecification.getHosts();
				for (BundleDescription host : hosts) {
					if (host != description) {
						references.add(new Object[]{description, host});
					}
				}
			}
			GenericDescription[] genericDependencies = description.getResolvedGenericRequires();
			for (GenericDescription dependency : genericDependencies) {
				if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(dependency.getType())) {
					references.add(new Object[] {description, dependency.getSupplier()});
				}
			}
		} else {
			// it is a host
			buildReferences(description, ((BundleDescriptionImpl) description).getBundleDependencies(), references);
		}
	}

	private void buildReferences(BundleDescription description, List<BundleDescription> dependencies, List<Object[]> references) {
		for (Iterator<BundleDescription> iter = dependencies.iterator(); iter.hasNext();)
			addReference(description, iter.next(), references);
	}

	private void addReference(BundleDescription description, BundleDescription reference, List<Object[]> references) {
		// build the reference from the description
		if (description == reference || reference == null)
			return;
		BundleDescription[] fragments = reference.getFragments();
		for (BundleDescription fragment : fragments) {
			if (fragment.isResolved()) {
				ExportPackageDescription[] exports = fragment.getExportPackages();
				if (exports.length > 0) {
					references.add(new Object[]{description, fragment});
				}
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
		List<ExportPackageDescription> orderedPkgList = new ArrayList<>(); // list of all ExportPackageDescriptions that are visible (ArrayList is used to keep order)
		Set<ExportPackageDescription> pkgSet = new HashSet<>();
		Set<String> importList = new HashSet<>(); // list of package names which are directly imported
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
			Set<BundleDescription> visited = new HashSet<>();
			visited.add(bundle); // always add self to prevent recursing into self
			Set<String> importNames = new HashSet<>(1);
			importNames.add(imports.getName(i));
			for (BundleSpecification require : requires) {
				BundleDescription bundleSupplier = (BundleDescription) require.getSupplier();
				if (bundleSupplier != null)
					getPackages(bundleSupplier, bundle.getSymbolicName(), importList, orderedPkgList, pkgSet, visited, strict, importNames, options);
			}
			importList.add(imports.getName(i)); // be sure to add to direct import list

		}
		// now find all the packages that are visible from required bundles
		RequiresHolder requires = new RequiresHolder(bundle, options);
		Set<BundleDescription> visited = new HashSet<>(requires.getSize());
		visited.add(bundle); // always add self to prevent recursing into self
		for (int i = 0; i < requires.getSize(); i++) {
			BundleDescription bundleSupplier = requires.getSupplier(i);
			if (bundleSupplier != null)
				getPackages(bundleSupplier, bundle.getSymbolicName(), importList, orderedPkgList, pkgSet, visited, strict, null, options);
		}
		return orderedPkgList.toArray(new ExportPackageDescription[orderedPkgList.size()]);
	}

	private void getPackages(BundleDescription requiredBundle, String symbolicName, Set<String> importList, List<ExportPackageDescription> orderedPkgList, Set<ExportPackageDescription> pkgSet, Set<BundleDescription> visited, boolean strict, Set<String> pkgNames, int options) {
		if (visited.contains(requiredBundle))
			return; // prevent duplicate entries and infinate loops incase of cycles
		visited.add(requiredBundle);
		// add all the exported packages from the required bundle; take x-friends into account.
		ExportPackageDescription[] substitutedExports = requiredBundle.getSubstitutedExports();
		ExportPackageDescription[] imports = requiredBundle.getResolvedImports();
		Set<String> substituteNames = null; // a temporary set used to scope packages we get from getPackages
		for (ExportPackageDescription substitutedExport : substitutedExports) {
			if (pkgNames == null || pkgNames.contains(substitutedExport.getName())) {
				for (ExportPackageDescription resolvedImport : imports) {
					if (substitutedExport.getName().equals(resolvedImport.getName()) && !pkgSet.contains(resolvedImport)) {
						if (substituteNames == null)
							substituteNames = new HashSet<>(1);
						else
							substituteNames.clear();
						// substituteNames is a set of one package containing the single substitute we are trying to get the source for
						substituteNames.add(substitutedExport.getName());
						getPackages(resolvedImport.getSupplier(), symbolicName, importList, orderedPkgList, pkgSet, new HashSet<BundleDescription>(0), strict, substituteNames, options);
					}
				}
			}
		}
		importList = substitutedExports.length == 0 ? importList : new HashSet<>(importList);
		for (ExportPackageDescription substitutedExport : substitutedExports) {
			// we add the package name to the import list to prevent required bundles from adding more sources
			importList.add(substitutedExport.getName());
		}

		ExportPackageDescription[] exports = requiredBundle.getSelectedExports();
		HashSet<String> exportNames = new HashSet<>(exports.length); // set is used to improve performance of duplicate check.
		for (ExportPackageDescription export : exports) {
			if ((pkgNames == null || pkgNames.contains(export.getName())) && !isSystemExport(export, options) && isFriend(symbolicName, export, strict) && !importList.contains(export.getName()) && !pkgSet.contains(export)) {
				if (!exportNames.contains(export.getName())) {
					// only add the first export
					orderedPkgList.add(export);
					pkgSet.add(export);
					exportNames.add(export.getName());
				}
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
				Set<BundleDescription> tmpVisited = new HashSet<>();
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
		String[] friends = (String[]) export.getDirective(StateImpl.FRIENDS_DIRECTIVE);
		if (friends == null)
			return true; // no x-friends means it is wide open
		for (String friend : friends) {
			if (friend.equals(consumerBSN)) {
				return true; // the consumer is a friend
			}
		}
		return false;
	}

	public int getAccessCode(BundleDescription bundle, ExportPackageDescription export) {
		if (((Boolean) export.getDirective(StateImpl.INTERNAL_DIRECTIVE)).booleanValue())
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
	private final Map<BundleDescription, Boolean> resolvedBundlesExported;

	// Depending on the options used, either requiredBundles or resolvedRequires is initialize, but not both.
	RequiresHolder(BundleDescription bundle, int options) {
		isUsingResolved = (options & StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES) != 0;
		if (isUsingResolved) {
			requiredBundles = null;
			resolvedBundlesExported = new HashMap<>();
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
			return resolvedBundlesExported.get(resolvedRequires[index]).booleanValue();
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
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		Set<BundleDescription> resolved = new HashSet<>();

		for (BundleDescription resolvedRequire : resolvedRequires) {
			resolved.add(resolvedRequire);
		}
		// Get the visibility of all directly required bundles
		for (BundleSpecification required : requiredBundles) {
			if (required.getSupplier() != null) {
				resolvedBundlesExported.put((BundleDescription) required.getSupplier(), Boolean.valueOf(required.isExported()));
				resolved.remove(required.getSupplier());
			}
		}

		BundleDescription[] fragments = bundle.getFragments();

		// Get the visibility of resolved required bundles, which come from fragments
		if (resolved.size() > 0) {
			for (BundleDescription fragment : fragments) {
				BundleSpecification[] fragmentRequiredBundles = fragment.getRequiredBundles();
				for (BundleSpecification fragmentRequiredBundle : fragmentRequiredBundles) {
					if (resolved.contains(fragmentRequiredBundle.getSupplier())) {
						resolvedBundlesExported.put((BundleDescription) fragmentRequiredBundle.getSupplier(), Boolean.valueOf(fragmentRequiredBundle.isExported()));
						resolved.remove(fragmentRequiredBundle.getSupplier());
					}
				}
				if (resolved.size() == 0) {
					break;
				}
			}
		}
	}
}
