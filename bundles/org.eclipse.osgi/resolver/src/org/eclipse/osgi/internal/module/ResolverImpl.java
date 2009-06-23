/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 ******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.security.AccessController;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.module.GroupingChecker.PackageRoots;
import org.eclipse.osgi.internal.resolver.BundleDescriptionImpl;
import org.eclipse.osgi.internal.resolver.ExportPackageDescriptionImpl;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class ResolverImpl implements org.eclipse.osgi.service.resolver.Resolver {
	// Debug fields
	private static final String RESOLVER = FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + "/resolver"; //$NON-NLS-1$
	private static final String OPTION_DEBUG = RESOLVER + "/debug";//$NON-NLS-1$
	private static final String OPTION_WIRING = RESOLVER + "/wiring"; //$NON-NLS-1$
	private static final String OPTION_IMPORTS = RESOLVER + "/imports"; //$NON-NLS-1$
	private static final String OPTION_REQUIRES = RESOLVER + "/requires"; //$NON-NLS-1$
	private static final String OPTION_GENERICS = RESOLVER + "/generics"; //$NON-NLS-1$
	private static final String OPTION_USES = RESOLVER + "/uses"; //$NON-NLS-1$
	private static final String OPTION_CYCLES = RESOLVER + "/cycles"; //$NON-NLS-1$
	public static boolean DEBUG = false;
	public static boolean DEBUG_WIRING = false;
	public static boolean DEBUG_IMPORTS = false;
	public static boolean DEBUG_REQUIRES = false;
	public static boolean DEBUG_GENERICS = false;
	public static boolean DEBUG_USES = false;
	public static boolean DEBUG_CYCLES = false;
	private static int MAX_MULTIPLE_SUPPLIERS_MERGE = 10;
	private static int MAX_USES_TIME_BASE = 30000; // 30 seconds
	private static int MAX_USES_TIME_LIMIT = 90000; // 90 seconds
	private static final SecureAction secureAction = (SecureAction) AccessController.doPrivileged(SecureAction.createSecureAction());

	private String[][] CURRENT_EES;

	// The State associated with this resolver
	private State state;
	// Used to check permissions for import/export, provide/require, host/fragment
	private final PermissionChecker permissionChecker;
	// Set of bundles that are pending removal
	private MappedList removalPending = new MappedList();
	// Indicates whether this resolver has been initialized
	private boolean initialized = false;

	// Repository for exports
	private VersionHashMap resolverExports = null;
	// Repository for bundles
	private VersionHashMap resolverBundles = null;
	// Repository for generics
	private VersionHashMap resolverGenerics = null;
	// List of unresolved bundles
	private HashSet unresolvedBundles = null;
	// Keys are BundleDescriptions, values are ResolverBundles
	private HashMap bundleMapping = null;
	private GroupingChecker groupingChecker;
	private Comparator selectionPolicy;
	private boolean developmentMode = false;
	private volatile CompositeResolveHelperRegistry compositeHelpers;

	public ResolverImpl(BundleContext context, boolean checkPermissions) {
		this.permissionChecker = new PermissionChecker(context, checkPermissions, this);
	}

	PermissionChecker getPermissionChecker() {
		return permissionChecker;
	}

	// Initializes the resolver
	private void initialize() {
		resolverExports = new VersionHashMap(this);
		resolverBundles = new VersionHashMap(this);
		resolverGenerics = new VersionHashMap(this);
		unresolvedBundles = new HashSet();
		bundleMapping = new HashMap();
		BundleDescription[] bundles = state.getBundles();
		groupingChecker = new GroupingChecker();

		ArrayList fragmentBundles = new ArrayList();
		// Add each bundle to the resolver's internal state
		for (int i = 0; i < bundles.length; i++)
			initResolverBundle(bundles[i], fragmentBundles, false);
		// Add each removal pending bundle to the resolver's internal state
		Object[] removedBundles = removalPending.getAllValues();
		for (int i = 0; i < removedBundles.length; i++)
			initResolverBundle((BundleDescription) removedBundles[i], fragmentBundles, true);
		// Iterate over the resolved fragments and attach them to their hosts
		for (Iterator iter = fragmentBundles.iterator(); iter.hasNext();) {
			ResolverBundle fragment = (ResolverBundle) iter.next();
			BundleDescription[] hosts = ((HostSpecification) fragment.getHost().getVersionConstraint()).getHosts();
			for (int i = 0; i < hosts.length; i++) {
				ResolverBundle host = (ResolverBundle) bundleMapping.get(hosts[i]);
				if (host != null)
					// Do not add fragment exports here because they would have been added by the host above.
					host.attachFragment(fragment, false);
			}
		}
		rewireBundles(); // Reconstruct wirings
		setDebugOptions();
		initialized = true;
	}

	private void initResolverBundle(BundleDescription bundleDesc, ArrayList fragmentBundles, boolean pending) {
		ResolverBundle bundle = new ResolverBundle(bundleDesc, this);
		bundleMapping.put(bundleDesc, bundle);
		if (!pending || bundleDesc.isResolved()) {
			resolverExports.put(bundle.getExportPackages());
			resolverBundles.put(bundle.getName(), bundle);
			resolverGenerics.put(bundle.getGenericCapabilities());
		}
		if (bundleDesc.isResolved()) {
			bundle.setState(ResolverBundle.RESOLVED);
			if (bundleDesc.getHost() != null)
				fragmentBundles.add(bundle);
		} else {
			if (!pending)
				unresolvedBundles.add(bundle);
		}
	}

	// Re-wire previously resolved bundles
	private void rewireBundles() {
		ArrayList visited = new ArrayList(bundleMapping.size());
		for (Iterator iter = bundleMapping.values().iterator(); iter.hasNext();) {
			ResolverBundle rb = (ResolverBundle) iter.next();
			if (!rb.getBundle().isResolved() || rb.isFragment())
				continue;
			rewireBundle(rb, visited);
		}
	}

	private void rewireBundle(ResolverBundle rb, ArrayList visited) {
		if (visited.contains(rb))
			return;
		visited.add(rb);
		// Wire requires to bundles
		BundleConstraint[] requires = rb.getRequires();
		for (int i = 0; i < requires.length; i++) {
			rewireRequire(requires[i], visited);
		}
		// Wire imports to exports
		ResolverImport[] imports = rb.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			rewireImport(imports[i], visited);
		}
		// Wire generics
		GenericConstraint[] genericRequires = rb.getGenericRequires();
		for (int i = 0; i < genericRequires.length; i++)
			rewireGeneric(genericRequires[i], visited);
	}

	private void rewireGeneric(GenericConstraint constraint, ArrayList visited) {
		if (constraint.getMatchingCapabilities() != null)
			return;
		GenericDescription[] suppliers = ((GenericSpecification) constraint.getVersionConstraint()).getSuppliers();
		if (suppliers == null)
			return;
		Object[] matches = resolverGenerics.get(constraint.getName());
		for (int i = 0; i < matches.length; i++) {
			GenericCapability match = (GenericCapability) matches[i];
			for (int j = 0; j < suppliers.length; j++)
				if (match.getBaseDescription() == suppliers[j])
					constraint.setMatchingCapability(match);
		}
		GenericCapability[] matchingCapabilities = constraint.getMatchingCapabilities();
		if (matchingCapabilities != null)
			for (int i = 0; i < matchingCapabilities.length; i++)
				rewireBundle(matchingCapabilities[i].getResolverBundle(), visited);
	}

	private void rewireRequire(BundleConstraint req, ArrayList visited) {
		if (req.getSelectedSupplier() != null)
			return;
		ResolverBundle matchingBundle = (ResolverBundle) bundleMapping.get(req.getVersionConstraint().getSupplier());
		req.addPossibleSupplier(matchingBundle);
		if (matchingBundle == null && !req.isOptional()) {
			System.err.println("Could not find matching bundle for " + req.getVersionConstraint()); //$NON-NLS-1$
			// TODO log error!!
		}
		if (matchingBundle != null) {
			rewireBundle(matchingBundle, visited);
		}
	}

	private void rewireImport(ResolverImport imp, ArrayList visited) {
		if (imp.isDynamic() || imp.getSelectedSupplier() != null)
			return;
		// Re-wire 'imp'
		ResolverExport matchingExport = null;
		ExportPackageDescription importSupplier = (ExportPackageDescription) imp.getVersionConstraint().getSupplier();
		ResolverBundle exporter = importSupplier == null ? null : (ResolverBundle) bundleMapping.get(importSupplier.getExporter());
		Object[] matches = resolverExports.get(imp.getName());
		for (int j = 0; j < matches.length; j++) {
			ResolverExport export = (ResolverExport) matches[j];
			if (export.getExporter() == exporter && importSupplier == export.getExportPackageDescription()) {
				matchingExport = export;
				break;
			}
		}
		imp.addPossibleSupplier(matchingExport);
		// If we still have a null wire and it's not optional, then we have an error
		if (imp.getSelectedSupplier() == null && !imp.isOptional()) {
			System.err.println("Could not find matching export for " + imp.getVersionConstraint()); //$NON-NLS-1$
			// TODO log error!!
		}
		if (imp.getSelectedSupplier() != null) {
			rewireBundle(((ResolverExport) imp.getSelectedSupplier()).getExporter(), visited);
		}
	}

	// Checks a bundle to make sure it is valid.  If this method returns false for
	// a given bundle, then that bundle will not even be considered for resolution
	private boolean isResolvable(BundleDescription bundle, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		// check if this is a rejected singleton
		if (rejectedSingletons.contains(bundle))
			return false;
		// check to see if the bundle is disabled
		DisabledInfo[] disabledInfos = state.getDisabledInfos(bundle);
		if (disabledInfos.length > 0) {
			StringBuffer message = new StringBuffer();
			for (int i = 0; i < disabledInfos.length; i++) {
				if (i > 0)
					message.append(' ');
				message.append('\"').append(disabledInfos[i].getPolicyName()).append(':').append(disabledInfos[i].getMessage()).append('\"');
			}
			state.addResolverError(bundle, ResolverError.DISABLED_BUNDLE, message.toString(), null);
			return false; // fail because we are disable
		}
		// Check for singletons
		if (bundle.isSingleton()) {
			Object[] sameName = resolverBundles.get(bundle.getName());
			if (sameName.length > 1) // Need to check if one is already resolved
				for (int i = 0; i < sameName.length; i++) {
					if (sameName[i] == bundle || !((ResolverBundle) sameName[i]).getBundle().isSingleton())
						continue; // Ignore the bundle we are resolving and non-singletons
					if (((ResolverBundle) sameName[i]).getBundle().isResolved()) {
						rejectedSingletons.add(bundle);
						return false; // Must fail since there is already a resolved bundle
					}
				}
		}
		// check the required execution environment
		String[] ees = bundle.getExecutionEnvironments();
		boolean matchedEE = ees.length == 0;
		if (!matchedEE)
			for (int i = 0; i < ees.length && !matchedEE; i++)
				for (int j = 0; j < CURRENT_EES.length && !matchedEE; j++)
					for (int k = 0; k < CURRENT_EES[j].length && !matchedEE; k++)
						if (CURRENT_EES[j][k].equals(ees[i])) {
							((BundleDescriptionImpl) bundle).setEquinoxEE(j);
							matchedEE = true;
						}
		if (!matchedEE) {
			StringBuffer bundleEE = new StringBuffer(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.length() + 20);
			bundleEE.append(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT).append(": "); //$NON-NLS-1$
			for (int i = 0; i < ees.length; i++) {
				if (i > 0)
					bundleEE.append(","); //$NON-NLS-1$
				bundleEE.append(ees[i]);
			}
			state.addResolverError(bundle, ResolverError.MISSING_EXECUTION_ENVIRONMENT, bundleEE.toString(), null);
			return false;
		}

		// check the native code specification
		NativeCodeSpecification nativeCode = bundle.getNativeCodeSpecification();
		if (nativeCode != null) {
			NativeCodeDescription[] nativeCodeSuppliers = nativeCode.getPossibleSuppliers();
			NativeCodeDescription highestRanked = null;
			for (int i = 0; i < nativeCodeSuppliers.length; i++)
				if (nativeCode.isSatisfiedBy(nativeCodeSuppliers[i]) && (highestRanked == null || highestRanked.compareTo(nativeCodeSuppliers[i]) < 0))
					highestRanked = nativeCodeSuppliers[i];
			if (highestRanked == null) {
				if (!nativeCode.isOptional()) {
					state.addResolverError(bundle, ResolverError.NO_NATIVECODE_MATCH, nativeCode.toString(), nativeCode);
					return false;
				}
			} else {
				if (highestRanked.hasInvalidNativePaths()) {
					state.addResolverError(bundle, ResolverError.INVALID_NATIVECODE_PATHS, highestRanked.toString(), nativeCode);
					return false;
				}
			}
			state.resolveConstraint(nativeCode, highestRanked);
		}

		// check the platform filter
		String platformFilter = bundle.getPlatformFilter();
		if (platformFilter == null)
			return true;
		if (platformProperties == null)
			return false;
		try {
			Filter filter = FilterImpl.newInstance(platformFilter);
			for (int i = 0; i < platformProperties.length; i++)
				// using matchCase here in case of duplicate case invarient keys (bug 180817)
				if (filter.matchCase(platformProperties[i]))
					return true;
		} catch (InvalidSyntaxException e) {
			// return false below
		}
		state.addResolverError(bundle, ResolverError.PLATFORM_FILTER, platformFilter, null);
		return false;
	}

	// Attach fragment to its host
	private void attachFragment(ResolverBundle bundle, ArrayList rejectedSingletons, Collection processedFragments) {
		if (processedFragments.contains(bundle.getName()))
			return;
		processedFragments.add(bundle.getName());
		// we want to attach multiple versions of the same fragment
		// from highest version to lowest to give the higher versions first pick
		// of the available host bundles.
		Object[] fragments = resolverBundles.get(bundle.getName());
		for (int i = 0; i < fragments.length; i++) {
			ResolverBundle fragment = (ResolverBundle) fragments[i];
			if (!fragment.isResolved())
				attachFragment0(fragment, rejectedSingletons);
		}
	}

	private void attachFragment0(ResolverBundle bundle, ArrayList rejectedSingletons) {
		if (!bundle.isFragment() || !bundle.isResolvable() || rejectedSingletons.contains(bundle.getBundle()))
			return;
		// no need to select singletons now; it will be done when we select the rest of the singleton bundles (bug 152042)
		// find all available hosts to attach to.
		boolean foundMatch = false;
		BundleConstraint hostConstraint = bundle.getHost();
		Object[] hosts = resolverBundles.get(hostConstraint.getVersionConstraint().getName());
		for (int i = 0; i < hosts.length; i++)
			if (((ResolverBundle) hosts[i]).isResolvable() && hostConstraint.isSatisfiedBy((ResolverBundle) hosts[i])) {
				foundMatch = true;
				resolverExports.put(((ResolverBundle) hosts[i]).attachFragment(bundle, true));
			}
		if (!foundMatch)
			state.addResolverError(bundle.getBundle(), ResolverError.MISSING_FRAGMENT_HOST, bundle.getHost().getVersionConstraint().toString(), bundle.getHost().getVersionConstraint());
	}

	public synchronized void resolve(BundleDescription[] reRefresh, Dictionary[] platformProperties) {
		if (DEBUG)
			ResolverImpl.log("*** BEGIN RESOLUTION ***"); //$NON-NLS-1$
		if (state == null)
			throw new IllegalStateException("RESOLVER_NO_STATE"); //$NON-NLS-1$

		if (!initialized)
			initialize();
		// set developmentMode each resolution
		developmentMode = platformProperties.length == 0 ? false : org.eclipse.osgi.framework.internal.core.Constants.DEVELOPMENT_MODE.equals(platformProperties[0].get(org.eclipse.osgi.framework.internal.core.Constants.OSGI_RESOLVER_MODE));
		reRefresh = addDevConstraints(reRefresh);
		// Unresolve all the supplied bundles and their dependents
		if (reRefresh != null)
			for (int i = 0; i < reRefresh.length; i++) {
				ResolverBundle rb = (ResolverBundle) bundleMapping.get(reRefresh[i]);
				if (rb != null)
					unresolveBundle(rb, false);
			}
		// reorder exports and bundles after unresolving the bundles
		resolverExports.reorder();
		resolverBundles.reorder();
		resolverGenerics.reorder();
		// always get the latest EEs
		getCurrentEEs(platformProperties);
		// keep a list of rejected singltons
		ArrayList rejectedSingletons = new ArrayList();
		boolean resolveOptional = platformProperties.length == 0 ? false : "true".equals(platformProperties[0].get("osgi.resolveOptional")); //$NON-NLS-1$//$NON-NLS-2$
		ResolverBundle[] currentlyResolved = null;
		if (resolveOptional) {
			BundleDescription[] resolvedBundles = state.getResolvedBundles();
			currentlyResolved = new ResolverBundle[resolvedBundles.length];
			for (int i = 0; i < resolvedBundles.length; i++)
				currentlyResolved[i] = (ResolverBundle) bundleMapping.get(resolvedBundles[i]);
		}
		// attempt to resolve all unresolved bundles
		ResolverBundle[] bundles = (ResolverBundle[]) unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
		resolveBundles(bundles, platformProperties, rejectedSingletons);
		if (selectSingletons(bundles, rejectedSingletons)) {
			// a singleton was unresolved as a result of selecting a different version
			// try to resolve unresolved bundles again; this will attempt to use the selected singleton
			bundles = (ResolverBundle[]) unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
			resolveBundles(bundles, platformProperties, rejectedSingletons);
		}
		for (Iterator rejected = rejectedSingletons.iterator(); rejected.hasNext();) {
			BundleDescription reject = (BundleDescription) rejected.next();
			BundleDescription sameName = state.getBundle(reject.getSymbolicName(), null);
			state.addResolverError(reject, ResolverError.SINGLETON_SELECTION, sameName.toString(), null);
		}
		if (resolveOptional)
			resolveOptionalConstraints(currentlyResolved);
		if (DEBUG)
			ResolverImpl.log("*** END RESOLUTION ***"); //$NON-NLS-1$
	}

	private BundleDescription[] addDevConstraints(BundleDescription[] reRefresh) {
		if (!developmentMode)
			return reRefresh; // we don't care about this unless we are in development mode
		// when in develoment mode we need to reRefresh hosts  of unresolved fragments that add new constraints 
		// and reRefresh and unresolved bundles that have dependents
		HashSet additionalRefresh = new HashSet();
		ResolverBundle[] unresolved = (ResolverBundle[]) unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
		for (int i = 0; i < unresolved.length; i++) {
			addUnresolvedWithDependents(unresolved[i], additionalRefresh);
			addHostsFromFragmentConstraints(unresolved[i], additionalRefresh);
		}
		if (additionalRefresh.size() == 0)
			return reRefresh; // no new bundles found to refresh
		// add the original reRefresh bundles to the set
		if (reRefresh != null)
			for (int i = 0; i < reRefresh.length; i++)
				additionalRefresh.add(reRefresh[i]);
		return (BundleDescription[]) additionalRefresh.toArray(new BundleDescription[additionalRefresh.size()]);
	}

	private void addUnresolvedWithDependents(ResolverBundle unresolved, HashSet additionalRefresh) {
		BundleDescription[] dependents = unresolved.getBundle().getDependents();
		if (dependents.length > 0)
			additionalRefresh.add(unresolved.getBundle());
	}

	private void addHostsFromFragmentConstraints(ResolverBundle unresolved, Set additionalRefresh) {
		if (!unresolved.isFragment())
			return;
		ImportPackageSpecification[] newImports = unresolved.getBundle().getImportPackages();
		BundleSpecification[] newRequires = unresolved.getBundle().getRequiredBundles();
		if (newImports.length == 0 && newRequires.length == 0)
			return; // the fragment does not have its own constraints
		BundleConstraint hostConstraint = unresolved.getHost();
		Object[] hosts = resolverBundles.get(hostConstraint.getVersionConstraint().getName());
		for (int j = 0; j < hosts.length; j++)
			if (hostConstraint.isSatisfiedBy((ResolverBundle) hosts[j]) && ((ResolverBundle) hosts[j]).isResolved())
				// we found a host that is resolved;
				// add it to the set of bundle to refresh so we can ensure this fragment is allowed to resolve
				additionalRefresh.add(((ResolverBundle) hosts[j]).getBundle());

	}

	private void resolveOptionalConstraints(ResolverBundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i] != null)
				resolveOptionalConstraints(bundles[i]);
		}
	}

	// TODO this does not do proper uses constraint verification.
	private void resolveOptionalConstraints(ResolverBundle bundle) {
		BundleConstraint[] requires = bundle.getRequires();
		ArrayList cycle = new ArrayList();
		boolean resolvedOptional = false;
		for (int i = 0; i < requires.length; i++)
			if (requires[i].isOptional() && requires[i].getSelectedSupplier() == null) {
				cycle.clear();
				resolveRequire(requires[i], cycle);
				if (requires[i].getSelectedSupplier() != null)
					resolvedOptional = true;
			}
		ResolverImport[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++)
			if (imports[i].isOptional() && imports[i].getSelectedSupplier() == null) {
				cycle.clear();
				resolveImport(imports[i], cycle);
				if (imports[i].getSelectedSupplier() != null)
					resolvedOptional = true;
			}
		if (resolvedOptional) {
			state.resolveBundle(bundle.getBundle(), false, null, null, null, null, null);
			stateResolveConstraints(bundle);
			stateResolveBundle(bundle);
		}
	}

	private void getCurrentEEs(Dictionary[] platformProperties) {
		CURRENT_EES = new String[platformProperties.length][];
		for (int i = 0; i < platformProperties.length; i++) {
			String eeSpecs = (String) platformProperties[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			CURRENT_EES[i] = ManifestElement.getArrayFromList(eeSpecs, ","); //$NON-NLS-1$
		}
	}

	private void resolveBundles(ResolverBundle[] bundles, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		// First check that all the meta-data is valid for each unresolved bundle
		// This will reset the resolvable flag for each bundle
		for (int i = 0; i < bundles.length; i++) {
			state.removeResolverErrors(bundles[i].getBundle());
			// if in development mode then make all bundles resolvable
			// we still want to call isResolvable here to populate any possible ResolverErrors for the bundle
			bundles[i].setResolvable(isResolvable(bundles[i].getBundle(), platformProperties, rejectedSingletons) || developmentMode);
			bundles[i].clearRefs();
		}
		resolveBundles0(bundles, platformProperties, rejectedSingletons);
		if (DEBUG_WIRING)
			printWirings();
		// set the resolved status of the bundles in the State
		stateResolveBundles(bundles);
	}

	private void resolveBundles0(ResolverBundle[] bundles, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		if (developmentMode)
			// need to sort bundles to keep consistent order for fragment attachment (bug 174930)
			Arrays.sort(bundles);
		// First attach all fragments to the matching hosts
		Collection processedFragments = new HashSet(bundles.length);
		for (int i = 0; i < bundles.length; i++)
			attachFragment(bundles[i], rejectedSingletons, processedFragments);

		// Lists of cyclic dependencies recording during resolving
		ArrayList cycle = new ArrayList(1); // start small
		// Attempt to resolve all unresolved bundles
		for (int i = 0; i < bundles.length; i++) {
			if (DEBUG)
				ResolverImpl.log("** RESOLVING " + bundles[i] + " **"); //$NON-NLS-1$ //$NON-NLS-2$
			cycle.clear();
			resolveBundle(bundles[i], cycle);
			// Check for any bundles involved in a cycle.
			// if any bundles in the cycle are not resolved then we need to resolve the resolvable ones
			checkCycle(cycle);
		}
		// Resolve all fragments that are still attached to at least one host.
		if (unresolvedBundles.size() > 0) {
			ResolverBundle[] unresolved = (ResolverBundle[]) unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
			for (int i = 0; i < unresolved.length; i++)
				resolveFragment(unresolved[i]);
		}
		checkUsesConstraints(bundles, platformProperties, rejectedSingletons);
		checkComposites(bundles, platformProperties, rejectedSingletons);
	}

	private void checkComposites(ResolverBundle[] bundles, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		CompositeResolveHelperRegistry helpers = getCompositeHelpers();
		if (helpers == null)
			return;
		Set exclude = null;
		for (int i = 0; i < bundles.length; i++) {
			CompositeResolveHelper helper = helpers.getCompositeResolveHelper(bundles[i].getBundle());
			if (helper == null)
				continue;
			if (!bundles[i].isResolved())
				continue;
			if (!helper.giveExports(getExportsWiredTo(bundles[i]))) {
				state.addResolverError(bundles[i].getBundle(), ResolverError.DISABLED_BUNDLE, null, null);
				bundles[i].setResolvable(false);
				bundles[i].clearRefs();
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(bundles[i], false, false);
				if (exclude == null)
					exclude = new HashSet(1);
				exclude.add(bundles[i]);
			}
		}
		reResolveBundles(exclude, bundles, platformProperties, rejectedSingletons);
	}

	private void checkUsesConstraints(ResolverBundle[] bundles, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		ArrayList conflictingConstraints = findBestCombination(bundles);
		if (conflictingConstraints == null)
			return;
		Set conflictedBundles = null;
		for (Iterator conflicts = conflictingConstraints.iterator(); conflicts.hasNext();) {
			ResolverConstraint conflict = (ResolverConstraint) conflicts.next();
			if (conflict.isOptional()) {
				conflict.clearPossibleSuppliers();
				continue;
			}
			if (conflictedBundles == null)
				conflictedBundles = new HashSet(conflictingConstraints.size());
			ResolverBundle conflictedBundle;
			if (conflict.isFromFragment())
				conflictedBundle = (ResolverBundle) bundleMapping.get(conflict.getVersionConstraint().getBundle());
			else
				conflictedBundle = conflict.getBundle();
			if (conflictedBundle != null) {
				if (DEBUG_USES)
					System.out.println("Found conflicting constraint: " + conflict + " in bundle " + conflictedBundle); //$NON-NLS-1$//$NON-NLS-2$
				conflictedBundles.add(conflictedBundle);
				int type = conflict instanceof ResolverImport ? ResolverError.IMPORT_PACKAGE_USES_CONFLICT : ResolverError.REQUIRE_BUNDLE_USES_CONFLICT;
				state.addResolverError(conflictedBundle.getBundle(), type, conflict.getVersionConstraint().toString(), conflict.getVersionConstraint());
				conflictedBundle.setResolvable(false);
				conflictedBundle.clearRefs();
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(conflictedBundle, false, false);
			}
		}
		reResolveBundles(conflictedBundles, bundles, platformProperties, rejectedSingletons);
	}

	private void reResolveBundles(Set exclude, ResolverBundle[] bundles, Dictionary[] platformProperties, ArrayList rejectedSingletons) {
		if (exclude == null || exclude.size() == 0)
			return;
		ArrayList remainingUnresolved = new ArrayList();
		for (int i = 0; i < bundles.length; i++) {
			if (!exclude.contains(bundles[i])) {
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(bundles[i], false, false);
				remainingUnresolved.add(bundles[i]);
			}
		}
		resolveBundles0((ResolverBundle[]) remainingUnresolved.toArray(new ResolverBundle[remainingUnresolved.size()]), platformProperties, rejectedSingletons);
	}

	private ArrayList findBestCombination(ResolverBundle[] bundles) {
		String usesMode = secureAction.getProperty("osgi.resolver.usesMode"); //$NON-NLS-1$
		if ("ignore".equals(usesMode)) //$NON-NLS-1$
			return null;
		HashSet bundleConstraints = new HashSet();
		HashSet packageConstraints = new HashSet();
		// first try out the initial selections
		ArrayList initialConflicts = getConflicts(bundles, packageConstraints, bundleConstraints);
		if (initialConflicts == null || "tryFirst".equals(usesMode)) { //$NON-NLS-1$
			groupingChecker.clear();
			// the first combination have no conflicts or we only are trying the first combination; return without iterating over all combinations
			return initialConflicts;
		}
		ResolverConstraint[][] multipleSuppliers = getMultipleSuppliers(bundles, packageConstraints, bundleConstraints);
		ArrayList conflicts = null;
		int[] bestCombination = new int[multipleSuppliers.length];
		conflicts = findBestCombination(bundles, multipleSuppliers, bestCombination, initialConflicts);
		if (DEBUG_USES) {
			System.out.print("Best combination found: "); //$NON-NLS-1$
			printCombination(bestCombination);
		}
		for (int i = 0; i < bestCombination.length; i++) {
			for (int j = 0; j < multipleSuppliers[i].length; j++)
				multipleSuppliers[i][j].setSelectedSupplier(bestCombination[i]);
		}

		// do not need to keep uses data in memory
		groupingChecker.clear();
		return conflicts;
	}

	private int[] getCombination(ResolverConstraint[][] multipleSuppliers, int[] combination) {
		for (int i = 0; i < combination.length; i++)
			combination[i] = multipleSuppliers[i][0].getSelectedSupplierIndex();
		return combination;
	}

	private ArrayList findBestCombination(ResolverBundle[] bundles, ResolverConstraint[][] multipleSuppliers, int[] bestCombination, ArrayList bestConflicts) {
		// now iterate over every possible combination until either zero conflicts are found 
		// or we have run out of combinations
		// if all combinations are tried then return the combination with the lowest number of conflicts
		long initialTime = System.currentTimeMillis();
		long timeLimit = Math.min(MAX_USES_TIME_BASE + (bundles.length * 30), MAX_USES_TIME_LIMIT);
		int bestConflictCount = getConflictCount(bestConflicts);
		ResolverBundle[] bestConflictBundles = getConflictedBundles(bestConflicts);
		while (bestConflictCount != 0 && (System.currentTimeMillis() - initialTime) < timeLimit && getNextCombination(multipleSuppliers)) {
			if (DEBUG_USES)
				printCombination(getCombination(multipleSuppliers, new int[multipleSuppliers.length]));
			// first count the conflicts for the bundles with conflicts from the best combination
			// this significantly reduces the time it takes to populate the GroupingChecker for cases where
			// the combination is no better.
			ArrayList conflicts = getConflicts(bestConflictBundles, null, null);
			int conflictCount = getConflictCount(conflicts);
			if (conflictCount >= bestConflictCount) {
				if (DEBUG_USES)
					System.out.println("Combination is not better that current best: " + conflictCount + ">=" + bestConflictCount); //$NON-NLS-1$ //$NON-NLS-2$
				// no need to test the other bundles;
				// this combination is no better for the bundles which conflict with the current best combination
				continue;
			}
			// this combination improves upon the conflicts for the bundles which conflict with the current best combination;
			// do an complete conflict count
			conflicts = getConflicts(bundles, null, null);
			conflictCount = getConflictCount(conflicts);
			if (conflictCount < bestConflictCount) {
				// this combination is better that the current best combination; save this combination as the current best
				bestConflictCount = conflictCount;
				bestConflicts = conflicts;
				getCombination(multipleSuppliers, bestCombination);
				bestConflictBundles = getConflictedBundles(bestConflicts);
				if (DEBUG_USES)
					System.out.println("Combination selected as current best: number of conflicts: " + bestConflictCount); //$NON-NLS-1$
			} else if (DEBUG_USES) {
				System.out.println("Combination is not better that current best: " + conflictCount + ">=" + bestConflictCount); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return bestConflicts;
	}

	private void printCombination(int[] curCombination) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < curCombination.length; i++) {
			sb.append(curCombination[i]);
			if (i < curCombination.length - 1)
				sb.append(',');
		}
		sb.append(']');
		System.out.println(sb.toString());
	}

	private ResolverBundle[] getConflictedBundles(ArrayList bestConflicts) {
		if (bestConflicts == null)
			return new ResolverBundle[0];
		ArrayList conflictedBundles = new ArrayList(bestConflicts.size());
		for (Iterator iConflicts = bestConflicts.iterator(); iConflicts.hasNext();) {
			ResolverConstraint constraint = (ResolverConstraint) iConflicts.next();
			if (!conflictedBundles.contains(constraint.getBundle()))
				conflictedBundles.add(constraint.getBundle());
		}
		return (ResolverBundle[]) conflictedBundles.toArray(new ResolverBundle[conflictedBundles.size()]);
	}

	private boolean getNextCombination(ResolverConstraint[][] multipleSuppliers) {
		int current = 0;
		while (current < multipleSuppliers.length) {
			if (multipleSuppliers[current][0].selectNextSupplier()) {
				for (int i = 1; i < multipleSuppliers[current].length; i++)
					multipleSuppliers[current][i].selectNextSupplier();
				return true; // the current slot has a next supplier
			}
			for (int i = 0; i < multipleSuppliers[current].length; i++)
				multipleSuppliers[current][i].setSelectedSupplier(0); // reset the current slot
			current++; // move to the next slot
		}
		return false;
	}

	// only count non-optional conflicts
	private int getConflictCount(ArrayList conflicts) {
		if (conflicts == null || conflicts.size() == 0)
			return 0;
		int result = 0;
		for (Iterator iConflicts = conflicts.iterator(); iConflicts.hasNext();)
			if (!((ResolverConstraint) iConflicts.next()).isOptional())
				result += 1;
		return result;
	}

	private ArrayList getConflicts(ResolverBundle[] bundles, HashSet packageConstraints, HashSet bundleConstraints) {
		groupingChecker.clear();
		ArrayList conflicts = null;
		for (int i = 0; i < bundles.length; i++)
			conflicts = addConflicts(bundles[i], packageConstraints, bundleConstraints, conflicts);
		return conflicts;
	}

	private ArrayList addConflicts(ResolverBundle bundle, HashSet packageConstraints, HashSet bundleConstraints, ArrayList conflicts) {
		boolean foundConflict = false;
		BundleConstraint[] requires = bundle.getRequires();
		for (int i = 0; i < requires.length; i++) {
			ResolverBundle selectedSupplier = (ResolverBundle) requires[i].getSelectedSupplier();
			PackageRoots[][] conflict = selectedSupplier == null ? null : groupingChecker.isConsistent(bundle, selectedSupplier);
			if (conflict != null) {
				addConflictNames(conflict, packageConstraints, bundleConstraints);
				if (!foundConflict) {
					if (conflicts == null)
						conflicts = new ArrayList(1);
					conflicts.add(requires[i]);
					foundConflict = !requires[i].isOptional(); // only record the conflicts upto the first non-optional
				}
			}
		}
		ResolverImport[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ResolverExport selectedSupplier = (ResolverExport) imports[i].getSelectedSupplier();
			PackageRoots[][] conflict = selectedSupplier == null ? null : groupingChecker.isConsistent(bundle, selectedSupplier);
			if (conflict != null) {
				addConflictNames(conflict, packageConstraints, bundleConstraints);
				if (!foundConflict) {
					if (conflicts == null)
						conflicts = new ArrayList(1);
					conflicts.add(imports[i]);
					foundConflict = !imports[i].isOptional(); // only record the conflicts upto the first non-optional
				}
			}
		}
		return conflicts;
	}

	// records the conflict names we can use to scope down the list of multiple suppliers
	private void addConflictNames(PackageRoots[][] conflict, HashSet packageConstraints, HashSet bundleConstraints) {
		if (packageConstraints == null || bundleConstraints == null)
			return;
		for (int i = 0; i < conflict.length; i++) {
			packageConstraints.add(conflict[i][0].getName());
			packageConstraints.add(conflict[i][1].getName());
			ResolverExport[] exports0 = conflict[i][0].getRoots();
			if (exports0 != null)
				for (int j = 0; j < exports0.length; j++) {
					ResolverBundle exporter = exports0[j].getExporter();
					if (exporter != null && exporter.getName() != null)
						bundleConstraints.add(exporter.getName());
				}
			ResolverExport[] exports1 = conflict[i][1].getRoots();
			if (exports1 != null)
				for (int j = 0; j < exports1.length; j++) {
					ResolverBundle exporter = exports1[j].getExporter();
					if (exporter != null && exporter.getName() != null)
						bundleConstraints.add(exporter.getName());
				}
		}
	}

	// get a list of resolver constraints that have multiple suppliers
	// a 2 demensional array is used each entry is a list of identical constraints that have identical suppliers.
	private ResolverConstraint[][] getMultipleSuppliers(ResolverBundle[] bundles, HashSet packageConstraints, HashSet bundleConstraints) {
		ArrayList multipleImportSupplierList = new ArrayList(1);
		ArrayList multipleRequireSupplierList = new ArrayList(1);
		for (int i = 0; i < bundles.length; i++) {
			BundleConstraint[] requires = bundles[i].getRequires();
			for (int j = 0; j < requires.length; j++)
				if (requires[j].getNumPossibleSuppliers() > 1)
					multipleRequireSupplierList.add(requires[j]);
			ResolverImport[] imports = bundles[i].getImportPackages();
			for (int j = 0; j < imports.length; j++) {
				if (imports[j].getNumPossibleSuppliers() > 1) {
					Integer eeProfile = (Integer) ((ResolverExport) imports[j].getSelectedSupplier()).getExportPackageDescription().getDirective(ExportPackageDescriptionImpl.EQUINOX_EE);
					if (eeProfile.intValue() < 0) {
						// this is a normal package; always add it
						multipleImportSupplierList.add(imports[j]);
					} else {
						// this is a system bunde export
						// If other exporters of this package also require the system bundle
						// then this package does not need to be added to the mix
						// this is an optimization for bundles like org.eclipse.xerces
						// that export lots of packages also exported by the system bundle on J2SE 1.4
						VersionSupplier[] suppliers = imports[j].getPossibleSuppliers();
						for (int suppliersIndex = 1; suppliersIndex < suppliers.length; suppliersIndex++) {
							Integer ee = (Integer) ((ResolverExport) suppliers[suppliersIndex]).getExportPackageDescription().getDirective(ExportPackageDescriptionImpl.EQUINOX_EE);
							if (ee.intValue() >= 0)
								continue;
							if (((ResolverExport) suppliers[suppliersIndex]).getExporter().getRequire(getSystemBundle()) == null)
								if (((ResolverExport) suppliers[suppliersIndex]).getExporter().getRequire(Constants.SYSTEM_BUNDLE_SYMBOLICNAME) == null) {
									multipleImportSupplierList.add(imports[j]);
									break;
								}
						}
					}
				}
			}
		}
		ArrayList results = new ArrayList();
		if (multipleImportSupplierList.size() + multipleRequireSupplierList.size() > MAX_MULTIPLE_SUPPLIERS_MERGE) {
			// we have hit a max on the multiple suppliers in the lists without merging.
			// first merge the identical constraints that have identical suppliers
			HashMap multipleImportSupplierMaps = new HashMap(1);
			for (Iterator iMultipleImportSuppliers = multipleImportSupplierList.iterator(); iMultipleImportSuppliers.hasNext();)
				addMutipleSupplierConstraint(multipleImportSupplierMaps, (ResolverConstraint) iMultipleImportSuppliers.next());
			HashMap multipleRequireSupplierMaps = new HashMap(1);
			for (Iterator iMultipleRequireSuppliers = multipleRequireSupplierList.iterator(); iMultipleRequireSuppliers.hasNext();)
				addMutipleSupplierConstraint(multipleRequireSupplierMaps, (ResolverConstraint) iMultipleRequireSuppliers.next());
			addMergedSuppliers(results, multipleImportSupplierMaps);
			addMergedSuppliers(results, multipleRequireSupplierMaps);
			// check the results to see if we have reduced the number enough
			if (results.size() > MAX_MULTIPLE_SUPPLIERS_MERGE && packageConstraints != null && bundleConstraints != null) {
				// we still have too big of a list; filter out constraints that are not in conflict
				Iterator iResults = results.iterator();
				results = new ArrayList();
				while (iResults.hasNext()) {
					ResolverConstraint[] constraints = (ResolverConstraint[]) iResults.next();
					ResolverConstraint constraint = constraints.length > 0 ? constraints[0] : null;
					if (constraint instanceof ResolverImport) {
						if (packageConstraints.contains(constraint.getName()))
							results.add(constraints);
					} else if (constraint instanceof BundleConstraint) {
						if (bundleConstraints.contains(constraint.getName()))
							results.add(constraints);
					}
				}
			}
		} else {
			// the size is acceptable; just copy the lists as-is
			for (Iterator iMultipleImportSuppliers = multipleImportSupplierList.iterator(); iMultipleImportSuppliers.hasNext();)
				results.add(new ResolverConstraint[] {(ResolverConstraint) iMultipleImportSuppliers.next()});
			for (Iterator iMultipleRequireSuppliers = multipleRequireSupplierList.iterator(); iMultipleRequireSuppliers.hasNext();)
				results.add(new ResolverConstraint[] {(ResolverConstraint) iMultipleRequireSuppliers.next()});
		}
		return (ResolverConstraint[][]) results.toArray(new ResolverConstraint[results.size()][]);
	}

	String getSystemBundle() {
		Dictionary[] platformProperties = state.getPlatformProperties();
		String systemBundle = (String) (platformProperties.length == 0 ? null : platformProperties[0].get(Constants.STATE_SYSTEM_BUNDLE));
		if (systemBundle == null)
			systemBundle = Constants.getInternalSymbolicName();
		return systemBundle;
	}

	private void addMergedSuppliers(ArrayList mergedSuppliers, HashMap constraints) {
		for (Iterator iConstraints = constraints.values().iterator(); iConstraints.hasNext();) {
			ArrayList mergedConstraintLists = (ArrayList) iConstraints.next();
			for (Iterator mergedLists = mergedConstraintLists.iterator(); mergedLists.hasNext();) {
				ArrayList constraintList = (ArrayList) mergedLists.next();
				mergedSuppliers.add(constraintList.toArray(new ResolverConstraint[constraintList.size()]));
			}
		}
	}

	private void addMutipleSupplierConstraint(HashMap constraints, ResolverConstraint constraint) {
		ArrayList mergedConstraintLists = (ArrayList) constraints.get(constraint.getName());
		if (mergedConstraintLists == null) {
			mergedConstraintLists = new ArrayList(1);
			ArrayList constraintList = new ArrayList(1);
			constraintList.add(constraint);
			mergedConstraintLists.add(constraintList);
			constraints.put(constraint.getName(), mergedConstraintLists);
			return;
		}
		for (Iterator mergedLists = mergedConstraintLists.iterator(); mergedLists.hasNext();) {
			ArrayList constraintList = (ArrayList) mergedLists.next();
			ResolverConstraint mergedConstraint = (ResolverConstraint) constraintList.get(0);
			VersionSupplier[] suppliers1 = constraint.getPossibleSuppliers();
			VersionSupplier[] suppliers2 = mergedConstraint.getPossibleSuppliers();
			if (suppliers1.length != suppliers2.length)
				continue;
			for (int i = 0; i < suppliers1.length; i++)
				if (suppliers1[i] != suppliers2[i])
					continue;
			constraintList.add(constraint);
			return;
		}
		ArrayList constraintList = new ArrayList(1);
		constraintList.add(constraint);
		mergedConstraintLists.add(constraintList);
	}

	private void checkCycle(ArrayList cycle) {
		int cycleSize = cycle.size();
		if (cycleSize == 0)
			return;
		cycleLoop: for (Iterator iCycle = cycle.iterator(); iCycle.hasNext();) {
			ResolverBundle cycleBundle = (ResolverBundle) iCycle.next();
			if (!cycleBundle.isResolvable()) {
				iCycle.remove(); // remove this bundle from the list of bundles that need re-resolved
				continue cycleLoop;
			}
			// Check that we haven't wired to any dropped exports
			ResolverImport[] imports = cycleBundle.getImportPackages();
			for (int j = 0; j < imports.length; j++) {
				// check for dropped exports
				while (imports[j].getSelectedSupplier() != null) {
					ResolverExport importSupplier = (ResolverExport) imports[j].getSelectedSupplier();
					if (importSupplier.getSubstitute() != null)
						imports[j].selectNextSupplier();
					else
						break;
				}
				if (!imports[j].isDynamic() && !imports[j].isOptional() && imports[j].getSelectedSupplier() == null) {
					cycleBundle.setResolvable(false);
					cycleBundle.clearRefs();
					state.addResolverError(imports[j].getVersionConstraint().getBundle(), ResolverError.MISSING_IMPORT_PACKAGE, imports[j].getVersionConstraint().toString(), imports[j].getVersionConstraint());
					iCycle.remove();
					continue cycleLoop;
				}
			}
		}
		if (cycle.size() != cycleSize) {
			//we removed an un-resolvable bundle; must re-resolve remaining cycle
			for (int i = 0; i < cycle.size(); i++) {
				ResolverBundle cycleBundle = (ResolverBundle) cycle.get(i);
				cycleBundle.clearWires();
				cycleBundle.clearRefs();
			}
			ArrayList innerCycle = new ArrayList(cycle.size());
			for (int i = 0; i < cycle.size(); i++)
				resolveBundle((ResolverBundle) cycle.get(i), innerCycle);
			checkCycle(innerCycle);
		} else {
			for (int i = 0; i < cycle.size(); i++) {
				if (DEBUG || DEBUG_CYCLES)
					ResolverImpl.log("Pushing " + cycle.get(i) + " to RESOLVED"); //$NON-NLS-1$ //$NON-NLS-2$
				setBundleResolved((ResolverBundle) cycle.get(i));
			}
		}
	}

	private boolean selectSingletons(ResolverBundle[] bundles, ArrayList rejectedSingletons) {
		if (developmentMode)
			return false; // do no want to unresolve singletons in development mode
		boolean result = false;
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bundleDesc = bundles[i].getBundle();
			if (!bundleDesc.isSingleton() || !bundleDesc.isResolved() || rejectedSingletons.contains(bundleDesc))
				continue;
			Object[] sameName = resolverBundles.get(bundleDesc.getName());
			if (sameName.length > 1) { // Need to make a selection based off of num dependents
				for (int j = 0; j < sameName.length; j++) {
					BundleDescription sameNameDesc = ((VersionSupplier) sameName[j]).getBundle();
					ResolverBundle sameNameBundle = (ResolverBundle) sameName[j];
					if (sameName[j] == bundles[i] || !sameNameDesc.isSingleton() || !sameNameDesc.isResolved() || rejectedSingletons.contains(sameNameDesc))
						continue; // Ignore the bundle we are selecting, non-singletons, and non-resolved
					result = true;
					boolean rejectedPolicy = selectionPolicy != null ? selectionPolicy.compare(sameNameDesc, bundleDesc) < 0 : sameNameDesc.getVersion().compareTo(bundleDesc.getVersion()) > 0;
					int sameNameRefs = sameNameBundle.getRefs();
					int curRefs = bundles[i].getRefs();
					// a bundle is always rejected if another bundle has more references to it;
					// otherwise a bundle is rejected based on the selection policy (version) only if the number of references are equal
					if ((sameNameRefs == curRefs && rejectedPolicy) || sameNameRefs > curRefs) {
						// this bundle is not selected; add it to the rejected list
						if (!rejectedSingletons.contains(bundles[i].getBundle()))
							rejectedSingletons.add(bundles[i].getBundle());
						break;
					}
					// we did not select the sameNameDesc; add the bundle to the rejected list
					if (!rejectedSingletons.contains(sameNameDesc))
						rejectedSingletons.add(sameNameDesc);
				}
			}
		}
		// clear the refs; we don't care about the refs after singlton selection
		for (int i = 0; i < bundles.length; i++)
			bundles[i].clearRefs();
		// unresolve the rejected singletons
		for (Iterator rejects = rejectedSingletons.iterator(); rejects.hasNext();)
			unresolveBundle((ResolverBundle) bundleMapping.get(rejects.next()), false);
		// reorder exports and bundles after unresolving the bundles
		resolverExports.reorder();
		resolverBundles.reorder();
		resolverGenerics.reorder();
		return result;
	}

	private void resolveFragment(ResolverBundle fragment) {
		if (!fragment.isFragment())
			return;
		if (fragment.getHost().getNumPossibleSuppliers() > 0)
			if (!developmentMode || state.getResolverErrors(fragment.getBundle()).length == 0)
				setBundleResolved(fragment);
	}

	// This method will attempt to resolve the supplied bundle and any bundles that it is dependent on
	private boolean resolveBundle(ResolverBundle bundle, ArrayList cycle) {
		if (bundle.isFragment())
			return false;
		if (!bundle.isResolvable()) {
			if (DEBUG)
				ResolverImpl.log("  - " + bundle + " is unresolvable"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		switch (bundle.getState()) {
			case ResolverBundle.RESOLVED :
				// 'bundle' is already resolved so just return
				if (DEBUG)
					ResolverImpl.log("  - " + bundle + " already resolved"); //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			case ResolverBundle.UNRESOLVED :
				// 'bundle' is UNRESOLVED so move to RESOLVING
				bundle.clearWires();
				setBundleResolving(bundle);
				break;
			case ResolverBundle.RESOLVING :
				if (cycle.contains(bundle))
					return true;
				break;
			default :
				break;
		}

		boolean failed = false;

		if (!failed) {
			GenericConstraint[] genericRequires = bundle.getGenericRequires();
			for (int i = 0; i < genericRequires.length; i++) {
				if (!resolveGenericReq(genericRequires[i], cycle)) {
					if (DEBUG || DEBUG_GENERICS)
						ResolverImpl.log("** GENERICS " + genericRequires[i].getVersionConstraint().getName() + "[" + genericRequires[i].getBundleDescription() + "] failed to resolve"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					state.addResolverError(genericRequires[i].getVersionConstraint().getBundle(), ResolverError.MISSING_GENERIC_CAPABILITY, genericRequires[i].getVersionConstraint().toString(), genericRequires[i].getVersionConstraint());
					if (genericRequires[i].isFromFragment()) {
						if (!developmentMode) // only detach fragments when not in devmode
							resolverExports.remove(bundle.detachFragment((ResolverBundle) bundleMapping.get(genericRequires[i].getVersionConstraint().getBundle()), null));
						continue;
					}
					if (!developmentMode) {
						// fail fast; otherwise we want to attempt to resolver other constraints in dev mode
						failed = true;
						break;
					}
				}
			}
		}

		if (!failed) {
			// Iterate thru required bundles of 'bundle' trying to find matching bundles.
			BundleConstraint[] requires = bundle.getRequires();
			for (int i = 0; i < requires.length; i++) {
				if (!resolveRequire(requires[i], cycle)) {
					if (DEBUG || DEBUG_REQUIRES)
						ResolverImpl.log("** REQUIRE " + requires[i].getVersionConstraint().getName() + "[" + requires[i].getBundleDescription() + "] failed to resolve"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					state.addResolverError(requires[i].getVersionConstraint().getBundle(), ResolverError.MISSING_REQUIRE_BUNDLE, requires[i].getVersionConstraint().toString(), requires[i].getVersionConstraint());
					// If the require has failed to resolve and it is from a fragment, then remove the fragment from the host
					if (requires[i].isFromFragment()) {
						if (!developmentMode) // only detach fragments when not in devmode
							resolverExports.remove(bundle.detachFragment((ResolverBundle) bundleMapping.get(requires[i].getVersionConstraint().getBundle()), requires[i]));
						continue;
					}
					if (!developmentMode) {
						// fail fast; otherwise we want to attempt to resolver other constraints in dev mode
						failed = true;
						break;
					}
				}
			}
		}

		if (!failed) {
			// Iterate thru imports of 'bundle' trying to find matching exports.
			ResolverImport[] imports = bundle.getImportPackages();
			for (int i = 0; i < imports.length; i++) {
				// Only resolve non-dynamic imports here
				if (!imports[i].isDynamic() && !resolveImport(imports[i], cycle)) {
					if (DEBUG || DEBUG_IMPORTS)
						ResolverImpl.log("** IMPORT " + imports[i].getName() + "[" + imports[i].getBundleDescription() + "] failed to resolve"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					// If the import has failed to resolve and it is from a fragment, then remove the fragment from the host
					state.addResolverError(imports[i].getVersionConstraint().getBundle(), ResolverError.MISSING_IMPORT_PACKAGE, imports[i].getVersionConstraint().toString(), imports[i].getVersionConstraint());
					if (imports[i].isFromFragment()) {
						if (!developmentMode) // only detach fragments when not in devmode
							resolverExports.remove(bundle.detachFragment((ResolverBundle) bundleMapping.get(imports[i].getVersionConstraint().getBundle()), imports[i]));
						continue;
					}
					if (!developmentMode) {
						// fail fast; otherwise we want to attempt to resolver other constraints in dev mode
						failed = true;
						break;
					}
				}
			}
		}

		// check that fragment constraints are met by the constraints that got resolved to the host
		checkFragmentConstraints(bundle);

		// do some extra checking when in development mode to see if other resolver error occurred
		if (developmentMode && !failed && state.getResolverErrors(bundle.getBundle()).length > 0)
			failed = true;

		// Need to check that all mandatory imports are wired. If they are then
		// set the bundle RESOLVED, otherwise set it back to UNRESOLVED
		if (failed) {
			setBundleUnresolved(bundle, false, developmentMode);
			if (DEBUG)
				ResolverImpl.log(bundle + " NOT RESOLVED"); //$NON-NLS-1$
		} else if (!cycle.contains(bundle)) {
			setBundleResolved(bundle);
			if (DEBUG)
				ResolverImpl.log(bundle + " RESOLVED"); //$NON-NLS-1$
		}

		if (bundle.getState() == ResolverBundle.UNRESOLVED)
			bundle.setResolvable(false); // Set it to unresolvable so we don't attempt to resolve it again in this round

		return bundle.getState() != ResolverBundle.UNRESOLVED;
	}

	private void checkFragmentConstraints(ResolverBundle bundle) {
		// get all currently attached fragments and ensure that any constraints
		// they have do not conflict with the constraints resolved to by the host
		ResolverBundle[] fragments = bundle.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			BundleDescription fragment = fragments[i].getBundle();
			if (bundle.constraintsConflict(fragment, fragment.getImportPackages(), fragment.getRequiredBundles(), fragment.getGenericRequires()) && !developmentMode)
				// found some conflicts; detach the fragment
				resolverExports.remove(bundle.detachFragment(fragments[i], null));
		}
	}

	private boolean resolveGenericReq(GenericConstraint constraint, ArrayList cycle) {
		if (DEBUG_REQUIRES)
			ResolverImpl.log("Trying to resolve: " + constraint.getBundle() + ", " + constraint.getVersionConstraint()); //$NON-NLS-1$ //$NON-NLS-2$
		GenericCapability[] matchingCapabilities = constraint.getMatchingCapabilities();
		if (matchingCapabilities != null) {
			// Check for unrecorded cyclic dependency
			for (int i = 0; i < matchingCapabilities.length; i++)
				if (matchingCapabilities[i].getResolverBundle().getState() == ResolverBundle.RESOLVING)
					if (!cycle.contains(constraint.getBundle()))
						cycle.add(constraint.getBundle());
			if (DEBUG_REQUIRES)
				ResolverImpl.log("  - already wired"); //$NON-NLS-1$
			return true; // Already wired (due to grouping dependencies) so just return
		}
		Object[] capabilities = resolverGenerics.get(constraint.getVersionConstraint().getName());
		boolean result = false;
		for (int i = 0; i < capabilities.length; i++) {
			GenericCapability capability = (GenericCapability) capabilities[i];
			if (DEBUG_GENERICS)
				ResolverImpl.log("CHECKING GENERICS: " + capability.getBaseDescription()); //$NON-NLS-1$
			// Check if capability matches
			if (constraint.isSatisfiedBy(capability)) {
				capability.getResolverBundle().addRef(constraint.getBundle());
				if (result && (((GenericSpecification) constraint.getVersionConstraint()).getResolution() & GenericSpecification.RESOLUTION_MULTIPLE) == 0)
					continue; // found a match already and this is not a multiple constraint
				constraint.setMatchingCapability(capability); // Wire to the capability
				if (constraint.getBundle() == capability.getResolverBundle()) {
					result = true; // Wired to ourselves
					continue;
				}
				VersionSupplier[] capabilityHosts = capability.isFromFragment() ? capability.getResolverBundle().getHost().getPossibleSuppliers() : new ResolverBundle[] {capability.getResolverBundle()};
				boolean foundResolvedMatch = false;
				for (int j = 0; capabilityHosts != null && j < capabilityHosts.length; j++) {
					ResolverBundle capabilitySupplier = (ResolverBundle) capabilityHosts[j];
					if (capabilitySupplier == constraint.getBundle()) {
						// the capability is from a fragment attached to this host do not recursively resolve the host again
						foundResolvedMatch = true;
						continue;
					}
					// if in dev mode then allow a constraint to resolve to an unresolved bundle
					if (capabilitySupplier.getState() == ResolverBundle.RESOLVED || (resolveBundle(capabilitySupplier, cycle) || developmentMode)) {
						foundResolvedMatch |= !capability.isFromFragment() ? true : capability.getResolverBundle().getHost().getPossibleSuppliers() != null;
						// Check cyclic dependencies
						if (capabilitySupplier.getState() == ResolverBundle.RESOLVING)
							if (!cycle.contains(capabilitySupplier))
								cycle.add(capabilitySupplier);
					}
				}
				if (!foundResolvedMatch) {
					constraint.removeMatchingCapability(capability);
					continue; // constraint hasn't resolved
				}
				if (DEBUG_GENERICS)
					ResolverImpl.log("Found match: " + capability.getBaseDescription() + ". Wiring"); //$NON-NLS-1$ //$NON-NLS-2$
				result = true;
			}
		}
		return result ? true : (((GenericSpecification) constraint.getVersionConstraint()).getResolution() & GenericSpecification.RESOLUTION_OPTIONAL) != 0;
	}

	// Resolve the supplied import. Returns true if the import can be resolved, false otherwise
	private boolean resolveRequire(BundleConstraint req, ArrayList cycle) {
		if (DEBUG_REQUIRES)
			ResolverImpl.log("Trying to resolve: " + req.getBundle() + ", " + req.getVersionConstraint()); //$NON-NLS-1$ //$NON-NLS-2$
		if (req.getSelectedSupplier() != null) {
			// Check for unrecorded cyclic dependency
			if (!cycle.contains(req.getBundle())) {
				cycle.add(req.getBundle());
				if (DEBUG_CYCLES)
					ResolverImpl.log("require-bundle cycle: " + req.getBundle() + " -> " + req.getSelectedSupplier()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (DEBUG_REQUIRES)
				ResolverImpl.log("  - already wired"); //$NON-NLS-1$
			return true; // Already wired (due to grouping dependencies) so just return
		}
		Object[] bundles = resolverBundles.get(req.getVersionConstraint().getName());
		boolean result = false;
		for (int i = 0; i < bundles.length; i++) {
			ResolverBundle bundle = (ResolverBundle) bundles[i];
			if (DEBUG_REQUIRES)
				ResolverImpl.log("CHECKING: " + bundle.getBundle()); //$NON-NLS-1$
			// Check if export matches
			if (req.isSatisfiedBy(bundle)) {
				bundle.addRef(req.getBundle());
				// first add the possible supplier; this is done before resolving the supplier bundle to prevent endless cycle loops.
				req.addPossibleSupplier(bundle);
				if (req.getBundle() != bundle) {
					// if in dev mode then allow a constraint to resolve to an unresolved bundle
					if (bundle.getState() != ResolverBundle.RESOLVED && !resolveBundle(bundle, cycle) && !developmentMode) {
						req.removePossibleSupplier(bundle);
						continue; // Bundle hasn't resolved
					}
				}
				// Check cyclic dependencies
				if (req.getBundle() != bundle) {
					if (bundle.getState() == ResolverBundle.RESOLVING)
						// If the bundle is RESOLVING, we have a cyclic dependency
						if (!cycle.contains(req.getBundle())) {
							cycle.add(req.getBundle());
							if (DEBUG_CYCLES)
								ResolverImpl.log("require-bundle cycle: " + req.getBundle() + " -> " + req.getSelectedSupplier()); //$NON-NLS-1$ //$NON-NLS-2$
						}
				}
				if (DEBUG_REQUIRES)
					ResolverImpl.log("Found match: " + bundle.getBundle() + ". Wiring"); //$NON-NLS-1$ //$NON-NLS-2$
				result = true;
			}
		}
		if (result || req.isOptional())
			return true; // If the req is optional then just return true

		return false;
	}

	// Resolve the supplied import. Returns true if the import can be resolved, false otherwise
	private boolean resolveImport(ResolverImport imp, ArrayList cycle) {
		if (DEBUG_IMPORTS)
			ResolverImpl.log("Trying to resolve: " + imp.getBundle() + ", " + imp.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		if (imp.getSelectedSupplier() != null) {
			// Check for unrecorded cyclic dependency
			if (!cycle.contains(imp.getBundle())) {
				cycle.add(imp.getBundle());
				if (DEBUG_CYCLES)
					ResolverImpl.log("import-package cycle: " + imp.getBundle() + " -> " + imp.getSelectedSupplier() + " from " + imp.getSelectedSupplier().getBundle()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (DEBUG_IMPORTS)
				ResolverImpl.log("  - already wired"); //$NON-NLS-1$
			return true; // Already wired (due to grouping dependencies) so just return
		}
		boolean result = false;
		ResolverExport[] substitutableExps = imp.getBundle().getExports(imp.getName());
		Object[] exports = resolverExports.get(imp.getName());
		for (int i = 0; i < exports.length; i++) {
			ResolverExport export = (ResolverExport) exports[i];
			if (DEBUG_IMPORTS)
				ResolverImpl.log("CHECKING: " + export.getExporter().getBundle() + ", " + export.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			// Check if export matches
			if (imp.isSatisfiedBy(export)) {
				int originalState = export.getExporter().getState();
				if (imp.isDynamic() && originalState != ResolverBundle.RESOLVED)
					continue; // Must not attempt to resolve an exporter when dynamic
				if (imp.getSelectedSupplier() != null && ((ResolverExport) imp.getSelectedSupplier()).getExporter() == imp.getBundle())
					break; // We wired to ourselves; nobody else matters
				export.getExporter().addRef(imp.getBundle());
				// first add the possible supplier; this is done before resolving the supplier bundle to prevent endless cycle loops.
				imp.addPossibleSupplier(export);
				if (imp.getBundle() != export.getExporter()) {
					for (int j = 0; j < substitutableExps.length; j++)
						if (substitutableExps[j].getSubstitute() == null)
							substitutableExps[j].setSubstitute(export); // Import wins, drop export
					// if in dev mode then allow a constraint to resolve to an unresolved bundle
					if ((originalState != ResolverBundle.RESOLVED && !resolveBundle(export.getExporter(), cycle) && !developmentMode) || export.getSubstitute() != null) {
						// remove the possible supplier
						imp.removePossibleSupplier(export);
						// add back the exports of this package from the importer
						if (imp.getSelectedSupplier() == null)
							for (int j = 0; j < substitutableExps.length; j++)
								if (substitutableExps[j].getSubstitute() == export)
									substitutableExps[j].setSubstitute(null);
						continue; // Bundle hasn't resolved || export has not been selected and is unavailable
					}
				} else if (export.getSubstitute() != null)
					continue; // we already found a possible import that satisifies us; our export is dropped

				// Record any cyclic dependencies
				if (imp.getBundle() != export.getExporter())
					if (export.getExporter().getState() == ResolverBundle.RESOLVING) {
						// If the exporter is RESOLVING, we have a cyclic dependency
						if (!cycle.contains(imp.getBundle())) {
							cycle.add(imp.getBundle());
							if (DEBUG_CYCLES)
								ResolverImpl.log("import-package cycle: " + imp.getBundle() + " -> " + imp.getSelectedSupplier() + " from " + imp.getSelectedSupplier().getBundle()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				if (DEBUG_IMPORTS)
					ResolverImpl.log("Found match: " + export.getExporter() + ". Wiring " + imp.getBundle() + ":" + imp.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				result = true;
			}
		}

		if (result)
			return true;
		if (imp.isOptional())
			return true; // If the import is optional then just return true
		if (substitutableExps.length > 0 && substitutableExps[0].getSubstitute() == null)
			return true; // If we still have an export that is not substituted return true
		return false;
	}

	// Move a bundle to UNRESOLVED
	private void setBundleUnresolved(ResolverBundle bundle, boolean removed, boolean keepFragsAttached) {
		if (bundle.getState() == ResolverBundle.UNRESOLVED && !developmentMode)
			// in this case there is nothing more to do
			return;
		// Note that when in dev mode we only want to force the fragment detach if asked to;
		// this would be done only when forcing a dependency chain to unresolve from unresolveBundle method
		if (removed || !keepFragsAttached) {
			// Force the initialization of the bundle, its exports and its capabilities.  This is needed to force proper attachment of fragments.
			resolverExports.remove(bundle.getExportPackages());
			resolverGenerics.remove(bundle.getGenericCapabilities());
			bundle.detachAllFragments();
			bundle.initialize(false);
			if (!removed) {
				// add back the available exports/capabilities
				resolverExports.put(bundle.getExportPackages());
				resolverGenerics.put(bundle.getGenericCapabilities());
			}
		}
		// TODO unresolvedBundles should be a set; for now only need to do a contains check in devMode.
		if (!removed && (!developmentMode || !unresolvedBundles.contains(bundle)))
			unresolvedBundles.add(bundle);
		bundle.setState(ResolverBundle.UNRESOLVED);
	}

	// Move a bundle to RESOLVED
	private void setBundleResolved(ResolverBundle bundle) {
		if (bundle.getState() == ResolverBundle.RESOLVED)
			return;
		unresolvedBundles.remove(bundle);
		bundle.setState(ResolverBundle.RESOLVED);
	}

	// Move a bundle to RESOLVING
	private void setBundleResolving(ResolverBundle bundle) {
		if (bundle.getState() == ResolverBundle.RESOLVING)
			return;
		unresolvedBundles.remove(bundle);
		bundle.setState(ResolverBundle.RESOLVING);
	}

	// Resolves the bundles in the State
	private void stateResolveBundles(ResolverBundle[] resolvedBundles) {
		for (int i = 0; i < resolvedBundles.length; i++) {
			if (!resolvedBundles[i].getBundle().isResolved())
				stateResolveBundle(resolvedBundles[i]);
		}
	}

	private void stateResolveConstraints(ResolverBundle rb) {
		ResolverImport[] imports = rb.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ResolverExport export = (ResolverExport) imports[i].getSelectedSupplier();
			BaseDescription supplier = export == null ? null : export.getExportPackageDescription();
			state.resolveConstraint(imports[i].getVersionConstraint(), supplier);
		}
		BundleConstraint[] requires = rb.getRequires();
		for (int i = 0; i < requires.length; i++) {
			ResolverBundle bundle = (ResolverBundle) requires[i].getSelectedSupplier();
			BaseDescription supplier = bundle == null ? null : bundle.getBundle();
			state.resolveConstraint(requires[i].getVersionConstraint(), supplier);
		}
		GenericConstraint[] genericRequires = rb.getGenericRequires();
		for (int i = 0; i < genericRequires.length; i++) {
			GenericCapability[] matchingCapabilities = genericRequires[i].getMatchingCapabilities();
			if (matchingCapabilities == null)
				state.resolveConstraint(genericRequires[i].getVersionConstraint(), null);
			else
				for (int j = 0; j < matchingCapabilities.length; j++)
					state.resolveConstraint(genericRequires[i].getVersionConstraint(), matchingCapabilities[j].getBaseDescription());
		}
	}

	private void stateResolveFragConstraints(ResolverBundle rb) {
		ResolverBundle host = (ResolverBundle) rb.getHost().getSelectedSupplier();
		ImportPackageSpecification[] imports = rb.getBundle().getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ResolverImport hostImport = host == null ? null : host.getImport(imports[i].getName());
			ResolverExport export = (ResolverExport) (hostImport == null ? null : hostImport.getSelectedSupplier());
			BaseDescription supplier = export == null ? null : export.getExportPackageDescription();
			state.resolveConstraint(imports[i], supplier);
		}
		BundleSpecification[] requires = rb.getBundle().getRequiredBundles();
		for (int i = 0; i < requires.length; i++) {
			BundleConstraint hostRequire = host == null ? null : host.getRequire(requires[i].getName());
			ResolverBundle bundle = (ResolverBundle) (hostRequire == null ? null : hostRequire.getSelectedSupplier());
			BaseDescription supplier = bundle == null ? null : bundle.getBundle();
			state.resolveConstraint(requires[i], supplier);
		}
	}

	private void stateResolveBundle(ResolverBundle rb) {
		// if in dev mode then we want to tell the state about the constraints we were able to resolve
		if (!rb.isResolved() && !developmentMode)
			return;
		if (rb.isFragment())
			stateResolveFragConstraints(rb);
		else
			stateResolveConstraints(rb);
		// Gather selected exports
		ResolverExport[] exports = rb.getSelectedExports();
		ArrayList selectedExports = new ArrayList(exports.length);
		for (int i = 0; i < exports.length; i++) {
			if (permissionChecker.checkPackagePermission(exports[i].getExportPackageDescription()))
				selectedExports.add(exports[i].getExportPackageDescription());
		}
		ExportPackageDescription[] selectedExportsArray = (ExportPackageDescription[]) selectedExports.toArray(new ExportPackageDescription[selectedExports.size()]);

		// Gather substitute exports
		ResolverExport[] substituted = rb.getSubstitutedExports();
		ArrayList substitutedExports = new ArrayList(substituted.length);
		for (int i = 0; i < substituted.length; i++) {
			substitutedExports.add(substituted[i].getExportPackageDescription());
		}
		ExportPackageDescription[] substitutedExportsArray = (ExportPackageDescription[]) substitutedExports.toArray(new ExportPackageDescription[substitutedExports.size()]);

		// Gather exports that have been wired to
		ExportPackageDescription[] exportsWiredToArray = getExportsWiredTo(rb);

		// Gather bundles that have been wired to
		BundleConstraint[] requires = rb.getRequires();
		ArrayList bundlesWiredTo = new ArrayList(requires.length);
		for (int i = 0; i < requires.length; i++)
			if (requires[i].getSelectedSupplier() != null)
				bundlesWiredTo.add(requires[i].getSelectedSupplier().getBaseDescription());
		BundleDescription[] bundlesWiredToArray = (BundleDescription[]) bundlesWiredTo.toArray(new BundleDescription[bundlesWiredTo.size()]);

		BundleDescription[] hostBundles = null;
		if (rb.isFragment()) {
			VersionSupplier[] matchingBundles = rb.getHost().getPossibleSuppliers();
			if (matchingBundles != null && matchingBundles.length > 0) {
				hostBundles = new BundleDescription[matchingBundles.length];
				for (int i = 0; i < matchingBundles.length; i++) {
					hostBundles[i] = matchingBundles[i].getBundle();
					if (rb.isNewFragmentExports() && hostBundles[i].isResolved()) {
						// update the host's set of selected exports
						ResolverExport[] hostExports = ((ResolverBundle) matchingBundles[i]).getSelectedExports();
						ExportPackageDescription[] hostExportsArray = new ExportPackageDescription[hostExports.length];
						for (int j = 0; j < hostExports.length; j++)
							hostExportsArray[j] = hostExports[j].getExportPackageDescription();
						state.resolveBundle(hostBundles[i], true, null, hostExportsArray, hostBundles[i].getSubstitutedExports(), hostBundles[i].getResolvedRequires(), hostBundles[i].getResolvedImports());
					}
				}
			}
		}

		// Resolve the bundle in the state
		state.resolveBundle(rb.getBundle(), rb.isResolved(), hostBundles, selectedExportsArray, substitutedExportsArray, bundlesWiredToArray, exportsWiredToArray);
	}

	private static ExportPackageDescription[] getExportsWiredTo(ResolverBundle rb) {
		// Gather exports that have been wired to
		ResolverImport[] imports = rb.getImportPackages();
		ArrayList exportsWiredTo = new ArrayList(imports.length);
		for (int i = 0; i < imports.length; i++)
			if (imports[i].getSelectedSupplier() != null)
				exportsWiredTo.add(imports[i].getSelectedSupplier().getBaseDescription());
		return (ExportPackageDescription[]) exportsWiredTo.toArray(new ExportPackageDescription[exportsWiredTo.size()]);
	}

	// Resolve dynamic import
	public synchronized ExportPackageDescription resolveDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		if (state == null)
			throw new IllegalStateException("RESOLVER_NO_STATE"); //$NON-NLS-1$

		// Make sure the resolver is initialized
		if (!initialized)
			initialize();

		ResolverBundle rb = (ResolverBundle) bundleMapping.get(importingBundle);
		if (rb.getExport(requestedPackage) != null)
			return null; // do not allow dynamic wires for packages which this bundle exports
		ResolverImport[] resolverImports = rb.getImportPackages();
		// Check through the ResolverImports of this bundle.
		// If there is a matching one then pass it into resolveImport()
		boolean found = false;
		for (int j = 0; j < resolverImports.length; j++) {
			// Make sure it is a dynamic import
			if (!resolverImports[j].isDynamic())
				continue;
			String importName = resolverImports[j].getName();
			// If the import uses a wildcard, then temporarily replace this with the requested package
			if (importName.equals("*") || //$NON-NLS-1$
					(importName.endsWith(".*") && requestedPackage.startsWith(importName.substring(0, importName.length() - 1)))) { //$NON-NLS-1$
				resolverImports[j].setName(requestedPackage);
			}
			// Resolve the import
			if (requestedPackage.equals(resolverImports[j].getName())) {
				found = true;
				// populate the grouping checker with current imports
				groupingChecker.populateRoots(resolverImports[j].getBundle());
				if (resolveImport(resolverImports[j], new ArrayList())) {
					found = false;
					while (!found && resolverImports[j].getSelectedSupplier() != null) {
						if (groupingChecker.isDynamicConsistent(resolverImports[j].getBundle(), (ResolverExport) resolverImports[j].getSelectedSupplier()) != null)
							resolverImports[j].selectNextSupplier(); // not consistent; try the next
						else
							found = true; // found a valid wire
					}
					resolverImports[j].setName(null);
					if (!found) {
						// not found or there was a conflict; reset the suppliers and return null
						resolverImports[j].setPossibleSuppliers(null);
						return null;
					}
					// If the import resolved then return it's matching export
					if (DEBUG_IMPORTS)
						ResolverImpl.log("Resolved dynamic import: " + rb + ":" + resolverImports[j].getName() + " -> " + ((ResolverExport) resolverImports[j].getSelectedSupplier()).getExporter() + ":" + requestedPackage); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					ExportPackageDescription matchingExport = ((ResolverExport) resolverImports[j].getSelectedSupplier()).getExportPackageDescription();
					// If it is a wildcard import then clear the wire, so other
					// exported packages can be found for it
					if (importName.endsWith("*")) //$NON-NLS-1$
						resolverImports[j].setPossibleSuppliers(null);
					return matchingExport;
				}
			}
			// Reset the import package name
			resolverImports[j].setName(null);
		}
		// this is to support adding dynamic imports on the fly.
		if (!found) {
			Map directives = new HashMap(1);
			directives.put(Constants.RESOLUTION_DIRECTIVE, ImportPackageSpecification.RESOLUTION_DYNAMIC);
			ImportPackageSpecification packageSpec = state.getFactory().createImportPackageSpecification(requestedPackage, null, null, null, directives, null, importingBundle);
			ResolverImport newImport = new ResolverImport(rb, packageSpec);
			if (resolveImport(newImport, new ArrayList())) {
				while (newImport.getSelectedSupplier() != null) {
					if (groupingChecker.isDynamicConsistent(rb, (ResolverExport) newImport.getSelectedSupplier()) != null)
						newImport.selectNextSupplier();
					else
						break;
				}
				return ((ResolverExport) newImport.getSelectedSupplier()).getExportPackageDescription();
			}
		}
		if (DEBUG || DEBUG_IMPORTS)
			ResolverImpl.log("Failed to resolve dynamic import: " + requestedPackage); //$NON-NLS-1$
		return null; // Couldn't resolve the import, so return null
	}

	public void bundleAdded(BundleDescription bundle) {
		if (!initialized)
			return;

		if (bundleMapping.get(bundle) != null)
			return; // this description already exists in the resolver
		ResolverBundle rb = new ResolverBundle(bundle, this);
		bundleMapping.put(bundle, rb);
		unresolvedBundles.add(rb);
		resolverExports.put(rb.getExportPackages());
		resolverBundles.put(rb.getName(), rb);
		resolverGenerics.put(rb.getGenericCapabilities());
	}

	public void bundleRemoved(BundleDescription bundle, boolean pending) {
		ResolverBundle rb = initialized ? (ResolverBundle) bundleMapping.get(bundle) : null;
		if (rb != null)
			rb.setUninstalled();
		internalBundleRemoved(bundle, pending);
	}

	private void internalBundleRemoved(BundleDescription bundle, boolean pending) {
		// check if there are any dependants
		if (pending)
			removalPending.put(new Long(bundle.getBundleId()), bundle);
		if (!initialized)
			return;
		ResolverBundle rb = (ResolverBundle) bundleMapping.get(bundle);
		if (rb == null)
			return;

		if (!pending) {
			bundleMapping.remove(bundle);
			groupingChecker.clear(rb);
		}
		if (!pending || !bundle.isResolved()) {
			resolverExports.remove(rb.getExportPackages());
			resolverBundles.remove(rb);
			resolverGenerics.remove(rb.getGenericCapabilities());
		}
		unresolvedBundles.remove(rb);
	}

	private void unresolveBundle(ResolverBundle bundle, boolean removed) {
		if (bundle == null)
			return;
		// check the removed list if unresolving then remove from the removed list
		Object[] removedBundles = removalPending.remove(new Long(bundle.getBundle().getBundleId()));
		for (int i = 0; i < removedBundles.length; i++) {
			ResolverBundle re = (ResolverBundle) bundleMapping.get(removedBundles[i]);
			unresolveBundle(re, true);
			state.removeBundleComplete((BundleDescription) removedBundles[i]);
			resolverExports.remove(re.getExportPackages());
			resolverBundles.remove(re);
			resolverGenerics.remove(re.getGenericCapabilities());
			bundleMapping.remove(removedBundles[i]);
			groupingChecker.clear(re);
			// the bundle is removed
			if (removedBundles[i] == bundle.getBundle())
				removed = true;
		}

		if (!bundle.getBundle().isResolved() && !developmentMode)
			return;
		CompositeResolveHelperRegistry currentLinks = compositeHelpers;
		if (currentLinks != null) {
			CompositeResolveHelper helper = currentLinks.getCompositeResolveHelper(bundle.getBundle());
			if (helper != null)
				helper.giveExports(null);
		}
		// if not removed then add to the list of unresolvedBundles,
		// passing false for devmode because we need all fragments detached
		setBundleUnresolved(bundle, removed, false);
		// Get bundles dependent on 'bundle'
		BundleDescription[] dependents = bundle.getBundle().getDependents();
		state.resolveBundle(bundle.getBundle(), false, null, null, null, null, null);
		// Unresolve dependents of 'bundle'
		for (int i = 0; i < dependents.length; i++)
			unresolveBundle((ResolverBundle) bundleMapping.get(dependents[i]), false);
	}

	public void bundleUpdated(BundleDescription newDescription, BundleDescription existingDescription, boolean pending) {
		internalBundleRemoved(existingDescription, pending);
		bundleAdded(newDescription);
	}

	public void flush() {
		resolverExports = null;
		resolverBundles = null;
		resolverGenerics = null;
		unresolvedBundles = null;
		bundleMapping = null;
		Object[] removed = removalPending.getAllValues();
		for (int i = 0; i < removed.length; i++)
			state.removeBundleComplete((BundleDescription) removed[i]);
		removalPending.clear();
		initialized = false;
	}

	public State getState() {
		return state;
	}

	public void setState(State newState) {
		if (this.state != null) {
			throw new IllegalStateException("Cannot change the State of a Resolver"); //$NON-NLS-1$
		}
		state = newState;
		flush();
	}

	private void setDebugOptions() {
		FrameworkDebugOptions options = FrameworkDebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		DEBUG = options.getBooleanOption(OPTION_DEBUG, false);
		DEBUG_WIRING = options.getBooleanOption(OPTION_WIRING, false);
		DEBUG_IMPORTS = options.getBooleanOption(OPTION_IMPORTS, false);
		DEBUG_REQUIRES = options.getBooleanOption(OPTION_REQUIRES, false);
		DEBUG_GENERICS = options.getBooleanOption(OPTION_GENERICS, false);
		DEBUG_USES = options.getBooleanOption(OPTION_USES, false);
		DEBUG_CYCLES = options.getBooleanOption(OPTION_CYCLES, false);
	}

	// LOGGING METHODS
	private void printWirings() {
		ResolverImpl.log("****** Result Wirings ******"); //$NON-NLS-1$
		Object[] bundles = resolverBundles.getAllValues();
		for (int j = 0; j < bundles.length; j++) {
			ResolverBundle rb = (ResolverBundle) bundles[j];
			if (rb.getBundle().isResolved()) {
				continue;
			}
			ResolverImpl.log("    * WIRING for " + rb); //$NON-NLS-1$
			// Require bundles
			BundleConstraint[] requireBundles = rb.getRequires();
			if (requireBundles.length == 0) {
				ResolverImpl.log("        (r) no requires"); //$NON-NLS-1$
			} else {
				for (int i = 0; i < requireBundles.length; i++) {
					if (requireBundles[i].getSelectedSupplier() == null) {
						ResolverImpl.log("        (r) " + rb.getBundle() + " -> NULL!!!"); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						ResolverImpl.log("        (r) " + rb.getBundle() + " -> " + requireBundles[i].getSelectedSupplier()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			// Hosts
			BundleConstraint hostSpec = rb.getHost();
			if (hostSpec != null) {
				VersionSupplier[] hosts = hostSpec.getPossibleSuppliers();
				if (hosts != null)
					for (int i = 0; i < hosts.length; i++) {
						ResolverImpl.log("        (h) " + rb.getBundle() + " -> " + hosts[i].getBundle()); //$NON-NLS-1$ //$NON-NLS-2$
					}
			}
			// Imports
			ResolverImport[] imports = rb.getImportPackages();
			if (imports.length == 0) {
				ResolverImpl.log("        (w) no imports"); //$NON-NLS-1$
				continue;
			}
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isDynamic() && imports[i].getSelectedSupplier() == null) {
					ResolverImpl.log("        (w) " + imports[i].getBundle() + ":" + imports[i].getName() + " -> DYNAMIC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else if (imports[i].isOptional() && imports[i].getSelectedSupplier() == null) {
					ResolverImpl.log("        (w) " + imports[i].getBundle() + ":" + imports[i].getName() + " -> OPTIONAL (could not be wired)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else if (imports[i].getSelectedSupplier() == null) {
					ResolverImpl.log("        (w) " + imports[i].getBundle() + ":" + imports[i].getName() + " -> NULL!!!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else {
					ResolverImpl.log("        (w) " + imports[i].getBundle() + ":" + imports[i].getName() + " -> " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							((ResolverExport) imports[i].getSelectedSupplier()).getExporter() + ":" + imports[i].getSelectedSupplier().getName()); //$NON-NLS-1$
				}
			}
		}
	}

	static void log(String message) {
		Debug.println(message);
	}

	VersionHashMap getResolverExports() {
		return resolverExports;
	}

	public void setSelectionPolicy(Comparator selectionPolicy) {
		this.selectionPolicy = selectionPolicy;
	}

	public Comparator getSelectionPolicy() {
		return selectionPolicy;
	}

	public void setCompositeResolveHelperRegistry(CompositeResolveHelperRegistry compositeHelpers) {
		this.compositeHelpers = compositeHelpers;
	}

	CompositeResolveHelperRegistry getCompositeHelpers() {
		return compositeHelpers;
	}
}
