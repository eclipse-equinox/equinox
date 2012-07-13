/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others. All rights reserved.
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
import org.eclipse.osgi.internal.baseadaptor.ArrayMap;
import org.eclipse.osgi.internal.module.GroupingChecker.PackageRoots;
import org.eclipse.osgi.internal.resolver.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public class ResolverImpl implements Resolver {
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
	private static final String USES_TIMEOUT_PROP = "osgi.usesTimeout"; //$NON-NLS-1$
	private static final String MULTIPLE_SUPPLIERS_LIMIT_PROP = "osgi.usesLimit"; //$NON-NLS-1$
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private String[][] CURRENT_EES;
	private ResolverHook hook;

	// The State associated with this resolver
	private State state;
	// Used to check permissions for import/export, provide/require, host/fragment
	private final PermissionChecker permissionChecker;
	// Set of bundles that are pending removal
	private MappedList<Long, BundleDescription> removalPending = new MappedList<Long, BundleDescription>();
	// Indicates whether this resolver has been initialized
	private boolean initialized = false;

	// Repository for exports
	private VersionHashMap<ResolverExport> resolverExports = null;
	// Repository for bundles
	private VersionHashMap<ResolverBundle> resolverBundles = null;
	// Repository for generics
	private Map<String, VersionHashMap<GenericCapability>> resolverGenerics = null;
	// List of unresolved bundles
	private HashSet<ResolverBundle> unresolvedBundles = null;
	// Keys are BundleDescriptions, values are ResolverBundles
	private HashMap<BundleDescription, ResolverBundle> bundleMapping = null;
	private GroupingChecker groupingChecker;
	private Comparator<BaseDescription> selectionPolicy;
	private boolean developmentMode = false;
	private boolean usesCalculationTimeout = false;
	private long usesTimeout = -1;
	private int usesMultipleSuppliersLimit;
	private volatile CompositeResolveHelperRegistry compositeHelpers;

	public ResolverImpl(boolean checkPermissions) {
		this.permissionChecker = new PermissionChecker(checkPermissions, this);
	}

	PermissionChecker getPermissionChecker() {
		return permissionChecker;
	}

	// Initializes the resolver
	private void initialize() {
		resolverExports = new VersionHashMap<ResolverExport>(this);
		resolverBundles = new VersionHashMap<ResolverBundle>(this);
		resolverGenerics = new HashMap<String, VersionHashMap<GenericCapability>>();
		unresolvedBundles = new HashSet<ResolverBundle>();
		bundleMapping = new HashMap<BundleDescription, ResolverBundle>();
		BundleDescription[] bundles = state.getBundles();
		groupingChecker = new GroupingChecker();

		ArrayList<ResolverBundle> fragmentBundles = new ArrayList<ResolverBundle>();
		// Add each bundle to the resolver's internal state
		for (int i = 0; i < bundles.length; i++)
			initResolverBundle(bundles[i], fragmentBundles, false);
		// Add each removal pending bundle to the resolver's internal state
		List<BundleDescription> removedBundles = removalPending.getAllValues();
		for (BundleDescription removed : removedBundles)
			initResolverBundle(removed, fragmentBundles, true);
		// Iterate over the resolved fragments and attach them to their hosts
		for (Iterator<ResolverBundle> iter = fragmentBundles.iterator(); iter.hasNext();) {
			ResolverBundle fragment = iter.next();
			BundleDescription[] hosts = ((HostSpecification) fragment.getHost().getVersionConstraint()).getHosts();
			for (int i = 0; i < hosts.length; i++) {
				ResolverBundle host = bundleMapping.get(hosts[i]);
				if (host != null)
					// Do not add fragment exports here because they would have been added by the host above.
					host.attachFragment(fragment, false);
			}
		}
		rewireBundles(); // Reconstruct wirings
		setDebugOptions();
		initialized = true;
	}

	private void initResolverBundle(BundleDescription bundleDesc, ArrayList<ResolverBundle> fragmentBundles, boolean pending) {
		ResolverBundle bundle = new ResolverBundle(bundleDesc, this);
		bundleMapping.put(bundleDesc, bundle);
		if (!pending || bundleDesc.isResolved()) {
			resolverExports.put(bundle.getExportPackages());
			resolverBundles.put(bundle.getName(), bundle);
			addGenerics(bundle.getGenericCapabilities());
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
		List<ResolverBundle> visited = new ArrayList<ResolverBundle>(bundleMapping.size());
		for (ResolverBundle rb : bundleMapping.values()) {
			if (!rb.getBundleDescription().isResolved())
				continue;
			rewireBundle(rb, visited);
		}
	}

	private void rewireBundle(ResolverBundle rb, List<ResolverBundle> visited) {
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

	private void rewireGeneric(GenericConstraint constraint, List<ResolverBundle> visited) {
		if (constraint.getSelectedSupplier() != null)
			return;
		GenericDescription[] suppliers = ((GenericSpecification) constraint.getVersionConstraint()).getSuppliers();
		if (suppliers == null)
			return;
		VersionHashMap<GenericCapability> namespace = resolverGenerics.get(constraint.getNameSpace());
		if (namespace == null) {
			System.err.println("Could not find matching capability for " + constraint.getVersionConstraint()); //$NON-NLS-1$
			// TODO log error!!
			return;
		}
		String constraintName = constraint.getName();
		List<GenericCapability> matches = constraintName == null ? namespace.get(constraintName) : namespace.getAllValues();
		for (GenericCapability match : matches) {
			for (GenericDescription supplier : suppliers)
				if (match.getBaseDescription() == supplier)
					constraint.addPossibleSupplier(match);
		}
		VersionSupplier[] matchingCapabilities = constraint.getPossibleSuppliers();
		if (matchingCapabilities != null)
			for (int i = 0; i < matchingCapabilities.length; i++)
				rewireBundle(matchingCapabilities[i].getResolverBundle(), visited);
	}

	private void rewireRequire(BundleConstraint req, List<ResolverBundle> visited) {
		if (req.getSelectedSupplier() != null)
			return;
		ResolverBundle matchingBundle = bundleMapping.get(req.getVersionConstraint().getSupplier());
		req.addPossibleSupplier(matchingBundle);
		if (matchingBundle == null && !req.isOptional()) {
			System.err.println("Could not find matching bundle for " + req.getVersionConstraint()); //$NON-NLS-1$
			// TODO log error!!
		}
		if (matchingBundle != null) {
			rewireBundle(matchingBundle, visited);
		}
	}

	private void rewireImport(ResolverImport imp, List<ResolverBundle> visited) {
		if (imp.isDynamic() || imp.getSelectedSupplier() != null)
			return;
		// Re-wire 'imp'
		ResolverExport matchingExport = null;
		ExportPackageDescription importSupplier = (ExportPackageDescription) imp.getVersionConstraint().getSupplier();
		ResolverBundle exporter = importSupplier == null ? null : (ResolverBundle) bundleMapping.get(importSupplier.getExporter());
		List<ResolverExport> matches = resolverExports.get(imp.getName());
		for (ResolverExport export : matches) {
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
	private boolean isResolvable(ResolverBundle bundle, Dictionary<Object, Object>[] platformProperties, Collection<ResolverBundle> hookDisabled) {
		BundleDescription bundleDesc = bundle.getBundleDescription();

		// check if the bundle is a hook disabled bundle
		if (hookDisabled.contains(bundle)) {
			state.addResolverError(bundleDesc, ResolverError.DISABLED_BUNDLE, "Resolver hook disabled bundle.", null); //$NON-NLS-1$
			return false;
		}
		// check to see if the bundle is disabled
		DisabledInfo[] disabledInfos = state.getDisabledInfos(bundleDesc);
		if (disabledInfos.length > 0) {
			StringBuffer message = new StringBuffer();
			for (int i = 0; i < disabledInfos.length; i++) {
				if (i > 0)
					message.append(' ');
				message.append('\"').append(disabledInfos[i].getPolicyName()).append(':').append(disabledInfos[i].getMessage()).append('\"');
			}
			state.addResolverError(bundleDesc, ResolverError.DISABLED_BUNDLE, message.toString(), null);
			return false; // fail because we are disable
		}

		// check the required execution environment
		String[] ees = bundleDesc.getExecutionEnvironments();
		boolean matchedEE = ees.length == 0;
		if (!matchedEE)
			for (int i = 0; i < ees.length && !matchedEE; i++)
				for (int j = 0; j < CURRENT_EES.length && !matchedEE; j++)
					for (int k = 0; k < CURRENT_EES[j].length && !matchedEE; k++)
						if (CURRENT_EES[j][k].equals(ees[i])) {
							((BundleDescriptionImpl) bundleDesc).setEquinoxEE(j);
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
			state.addResolverError(bundleDesc, ResolverError.MISSING_EXECUTION_ENVIRONMENT, bundleEE.toString(), null);
			return false;
		}

		// check the native code specification
		NativeCodeSpecification nativeCode = bundleDesc.getNativeCodeSpecification();
		if (nativeCode != null) {
			NativeCodeDescription[] nativeCodeSuppliers = nativeCode.getPossibleSuppliers();
			NativeCodeDescription highestRanked = null;
			for (int i = 0; i < nativeCodeSuppliers.length; i++)
				if (nativeCode.isSatisfiedBy(nativeCodeSuppliers[i]) && (highestRanked == null || highestRanked.compareTo(nativeCodeSuppliers[i]) < 0))
					highestRanked = nativeCodeSuppliers[i];
			if (highestRanked == null) {
				if (!nativeCode.isOptional()) {
					state.addResolverError(bundleDesc, ResolverError.NO_NATIVECODE_MATCH, nativeCode.toString(), nativeCode);
					return false;
				}
			} else {
				if (highestRanked.hasInvalidNativePaths()) {
					state.addResolverError(bundleDesc, ResolverError.INVALID_NATIVECODE_PATHS, highestRanked.toString(), nativeCode);
					return false;
				}
			}
			state.resolveConstraint(nativeCode, highestRanked);
		}

		// check the platform filter
		String platformFilter = bundleDesc.getPlatformFilter();
		if (platformFilter == null)
			return true;
		if (platformProperties == null)
			return false;
		try {
			Filter filter = FilterImpl.newInstance(platformFilter);
			for (int i = 0; i < platformProperties.length; i++) {
				// using matchCase here in case of duplicate case invarient keys (bug 180817)
				@SuppressWarnings("rawtypes")
				Dictionary props = platformProperties[i];
				if (filter.matchCase(props))
					return true;
			}
		} catch (InvalidSyntaxException e) {
			// return false below
		}
		state.addResolverError(bundleDesc, ResolverError.PLATFORM_FILTER, platformFilter, null);
		return false;
	}

	// Attach fragment to its host
	private void attachFragment(ResolverBundle bundle, Collection<String> processedFragments) {
		if (processedFragments.contains(bundle.getName()))
			return;
		processedFragments.add(bundle.getName());
		// we want to attach multiple versions of the same fragment
		// from highest version to lowest to give the higher versions first pick
		// of the available host bundles.
		List<ResolverBundle> fragments = resolverBundles.get(bundle.getName());
		for (ResolverBundle fragment : fragments) {
			if (!fragment.isResolved())
				attachFragment0(fragment);
		}
	}

	private void attachFragment0(ResolverBundle bundle) {
		if (!bundle.isFragment() || !bundle.isResolvable())
			return;
		bundle.clearWires();
		if (!resolveOSGiEE(bundle))
			return;
		// no need to select singletons now; it will be done when we select the rest of the singleton bundles (bug 152042)
		// find all available hosts to attach to.
		boolean foundMatch = false;
		BundleConstraint hostConstraint = bundle.getHost();
		long timestamp;
		List<ResolverBundle> candidates;
		do {
			timestamp = state.getTimeStamp();
			List<ResolverBundle> hosts = resolverBundles.get(hostConstraint.getVersionConstraint().getName());
			candidates = new ArrayList<ResolverBundle>(hosts);
			List<BundleCapability> hostCapabilities = new ArrayList<BundleCapability>(hosts.size());
			// Must remove candidates that do not match before calling hooks.
			for (Iterator<ResolverBundle> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
				ResolverBundle host = iCandidates.next();
				if (!host.isResolvable() || !host.getBundleDescription().attachFragments() || !hostConstraint.isSatisfiedBy(host)) {
					iCandidates.remove();
				} else {
					List<BundleCapability> h = host.getBundleDescription().getDeclaredCapabilities(BundleRevision.HOST_NAMESPACE);
					// the bundle must have 1 host capability.
					hostCapabilities.add(h.get(0));
				}
			}

			if (hook != null)
				hook.filterMatches(hostConstraint.getRequirement(), asCapabilities(new ArrayMap<BundleCapability, ResolverBundle>(hostCapabilities, candidates)));
		} while (timestamp != state.getTimeStamp());
		// we are left with only candidates that satisfy the host constraint
		for (ResolverBundle host : candidates) {
			foundMatch = true;
			host.attachFragment(bundle, true);
		}
		if (!foundMatch)
			state.addResolverError(bundle.getBundleDescription(), ResolverError.MISSING_FRAGMENT_HOST, bundle.getHost().getVersionConstraint().toString(), bundle.getHost().getVersionConstraint());

	}

	private boolean resolveOSGiEE(ResolverBundle bundle) {
		GenericConstraint[] requirements = bundle.getGenericRequires();
		for (GenericConstraint requirement : requirements) {
			if (!(StateImpl.OSGI_EE_NAMESPACE.equals(requirement.getNameSpace()) || requirement.isEffective()))
				continue;
			{
				if (!resolveGenericReq(requirement, new ArrayList<ResolverBundle>(0))) {
					if (DEBUG || DEBUG_GENERICS)
						ResolverImpl.log("** GENERICS " + requirement.getVersionConstraint().getName() + "[" + requirement.getBundleDescription() + "] failed to resolve"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					state.addResolverError(requirement.getVersionConstraint().getBundle(), ResolverError.MISSING_GENERIC_CAPABILITY, requirement.getVersionConstraint().toString(), requirement.getVersionConstraint());
					if (!developmentMode) {
						// fail fast; otherwise we want to attempt to resolver other constraints in dev mode
						return false;
					}
				} else {
					VersionSupplier supplier = requirement.getSelectedSupplier();
					Integer ee = supplier == null ? null : (Integer) ((GenericDescription) supplier.getBaseDescription()).getAttributes().get(ExportPackageDescriptionImpl.EQUINOX_EE);
					if (ee != null && ((BundleDescriptionImpl) bundle.getBaseDescription()).getEquinoxEE() < 0)
						((BundleDescriptionImpl) bundle.getBundleDescription()).setEquinoxEE(ee);
				}
			}
		}
		return true;
	}

	public synchronized void resolve(BundleDescription[] reRefresh, Dictionary<Object, Object>[] platformProperties) {
		if (DEBUG)
			ResolverImpl.log("*** BEGIN RESOLUTION ***"); //$NON-NLS-1$
		if (state == null)
			throw new IllegalStateException("RESOLVER_NO_STATE"); //$NON-NLS-1$

		if (!initialized)
			initialize();
		hook = (state instanceof StateImpl) ? ((StateImpl) state).getResolverHook() : null;
		try {
			// set developmentMode each resolution
			developmentMode = platformProperties.length == 0 ? false : org.eclipse.osgi.framework.internal.core.Constants.DEVELOPMENT_MODE.equals(platformProperties[0].get(org.eclipse.osgi.framework.internal.core.Constants.OSGI_RESOLVER_MODE));
			// set uses timeout each resolution
			usesTimeout = getUsesTimeout(platformProperties);
			// set limit for constraints with multiple suppliers each resolution
			usesMultipleSuppliersLimit = getMultipleSuppliersLimit(platformProperties);
			reRefresh = addDevConstraints(reRefresh);
			// Unresolve all the supplied bundles and their dependents
			if (reRefresh != null)
				for (int i = 0; i < reRefresh.length; i++) {
					ResolverBundle rb = bundleMapping.get(reRefresh[i]);
					if (rb != null)
						unresolveBundle(rb, false);
				}
			// reorder exports and bundles after unresolving the bundles
			resolverExports.reorder();
			resolverBundles.reorder();
			reorderGenerics();
			// always get the latest EEs
			getCurrentEEs(platformProperties);
			boolean resolveOptional = platformProperties.length == 0 ? false : "true".equals(platformProperties[0].get("osgi.resolveOptional")); //$NON-NLS-1$//$NON-NLS-2$
			ResolverBundle[] currentlyResolved = null;
			if (resolveOptional) {
				BundleDescription[] resolvedBundles = state.getResolvedBundles();
				currentlyResolved = new ResolverBundle[resolvedBundles.length];
				for (int i = 0; i < resolvedBundles.length; i++)
					currentlyResolved[i] = bundleMapping.get(resolvedBundles[i]);
			}
			// attempt to resolve all unresolved bundles
			@SuppressWarnings("unchecked")
			Collection<ResolverBundle> hookDisabled = Collections.EMPTY_LIST;
			if (hook != null) {
				List<ResolverBundle> resolvableBundles = new ArrayList<ResolverBundle>(unresolvedBundles);
				List<BundleRevision> resolvableRevisions = new ArrayList<BundleRevision>(resolvableBundles.size());
				for (ResolverBundle bundle : resolvableBundles)
					resolvableRevisions.add(bundle.getBundleDescription());
				ArrayMap<BundleRevision, ResolverBundle> resolvable = new ArrayMap<BundleRevision, ResolverBundle>(resolvableRevisions, resolvableBundles);
				int size = resolvableBundles.size();
				hook.filterResolvable(resolvable);
				if (resolvable.size() < size) {
					hookDisabled = new ArrayList<ResolverBundle>(unresolvedBundles);
					hookDisabled.removeAll(resolvableBundles);
				}
			}

			usesCalculationTimeout = false;

			List<ResolverBundle> toResolve = new ArrayList<ResolverBundle>(unresolvedBundles);
			// first resolve the system bundle to allow osgi.ee capabilities to be resolved
			List<ResolverBundle> unresolvedSystemBundles = new ArrayList<ResolverBundle>(1);
			String systemBSN = getSystemBundle();
			for (Iterator<ResolverBundle> iToResolve = toResolve.iterator(); iToResolve.hasNext();) {
				ResolverBundle rb = iToResolve.next();
				String symbolicName = rb.getName();
				if (symbolicName != null && symbolicName.equals(systemBSN)) {
					unresolvedSystemBundles.add(rb);
					iToResolve.remove();
				}
			}
			if (!unresolvedSystemBundles.isEmpty())
				resolveBundles(unresolvedSystemBundles.toArray(new ResolverBundle[unresolvedSystemBundles.size()]), platformProperties, hookDisabled);

			// Now resolve the rest
			resolveBundles(toResolve.toArray(new ResolverBundle[toResolve.size()]), platformProperties, hookDisabled);

			@SuppressWarnings("unchecked")
			Collection<ResolverBundle> optionalResolved = resolveOptional ? resolveOptionalConstraints(currentlyResolved) : Collections.EMPTY_LIST;
			ResolverHook current = hook;
			if (current != null) {
				hook = null;
				current.end();
			}

			// set the resolved status of the bundles in the State
			// Note this must be done after calling end above in case end throws errors
			stateResolveBundles(bundleMapping.values().toArray(new ResolverBundle[bundleMapping.size()]));

			for (ResolverBundle bundle : optionalResolved) {
				state.resolveBundle(bundle.getBundleDescription(), false, null, null, null, null, null, null, null, null);
				stateResolveBundle(bundle);
			}
			// reorder exports and bundles after resolving the bundles
			resolverExports.reorder();
			resolverBundles.reorder();
			reorderGenerics();
			if (resolveOptional)
				resolveOptionalConstraints(currentlyResolved);
			if (DEBUG)
				ResolverImpl.log("*** END RESOLUTION ***"); //$NON-NLS-1$
		} finally {
			if (hook != null)
				hook.end(); // need to make sure end is always called
			hook = null;
		}
	}

	private long getUsesTimeout(Dictionary<Object, Object>[] platformProperties) {
		try {
			Object timeout = platformProperties.length == 0 ? null : platformProperties[0].get(USES_TIMEOUT_PROP);
			if (timeout != null) {
				long temp = Long.parseLong(timeout.toString());
				if (temp < 0) {
					return -1;
				} else if (temp == 0) {
					return Long.MAX_VALUE;
				} else {
					return temp;
				}
			}
		} catch (NumberFormatException e) {
			// nothing;
		}
		return -1;
	}

	private int getMultipleSuppliersLimit(Dictionary<Object, Object>[] platformProperties) {
		try {
			Object limit = platformProperties.length == 0 ? null : platformProperties[0].get(MULTIPLE_SUPPLIERS_LIMIT_PROP);
			if (limit != null) {
				int temp = Integer.parseInt(limit.toString());
				if (temp < 0) {
					return MAX_MULTIPLE_SUPPLIERS_MERGE;
				} else if (temp == 0) {
					return Integer.MAX_VALUE;
				} else {
					return temp;
				}
			}
		} catch (NumberFormatException e) {
			// nothing;
		}
		return MAX_MULTIPLE_SUPPLIERS_MERGE;
	}

	private BundleDescription[] addDevConstraints(BundleDescription[] reRefresh) {
		if (!developmentMode)
			return reRefresh; // we don't care about this unless we are in development mode
		// when in develoment mode we need to reRefresh hosts  of unresolved fragments that add new constraints 
		// and reRefresh and unresolved bundles that have dependents
		Set<BundleDescription> additionalRefresh = new HashSet<BundleDescription>();
		ResolverBundle[] unresolved = unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
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
		return additionalRefresh.toArray(new BundleDescription[additionalRefresh.size()]);
	}

	private void addUnresolvedWithDependents(ResolverBundle unresolved, Set<BundleDescription> additionalRefresh) {
		BundleDescription[] dependents = unresolved.getBundleDescription().getDependents();
		if (dependents.length > 0)
			additionalRefresh.add(unresolved.getBundleDescription());
	}

	private void addHostsFromFragmentConstraints(ResolverBundle unresolved, Set<BundleDescription> additionalRefresh) {
		if (!unresolved.isFragment())
			return;
		ImportPackageSpecification[] newImports = unresolved.getBundleDescription().getImportPackages();
		BundleSpecification[] newRequires = unresolved.getBundleDescription().getRequiredBundles();
		if (newImports.length == 0 && newRequires.length == 0)
			return; // the fragment does not have its own constraints
		BundleConstraint hostConstraint = unresolved.getHost();
		List<ResolverBundle> hosts = resolverBundles.get(hostConstraint.getVersionConstraint().getName());
		for (ResolverBundle host : hosts)
			if (hostConstraint.isSatisfiedBy(host) && host.isResolved())
				// we found a host that is resolved;
				// add it to the set of bundle to refresh so we can ensure this fragment is allowed to resolve
				additionalRefresh.add(host.getBundleDescription());

	}

	private Collection<ResolverBundle> resolveOptionalConstraints(ResolverBundle[] bundles) {
		Collection<ResolverBundle> result = new ArrayList<ResolverBundle>();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i] != null && resolveOptionalConstraints(bundles[i])) {
				result.add(bundles[i]);
			}
		}
		return result;
	}

	// TODO this does not do proper uses constraint verification.
	private boolean resolveOptionalConstraints(ResolverBundle bundle) {
		BundleConstraint[] requires = bundle.getRequires();
		List<ResolverBundle> cycle = new ArrayList<ResolverBundle>();
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
		return resolvedOptional;
	}

	private void getCurrentEEs(Dictionary<Object, Object>[] platformProperties) {
		CURRENT_EES = new String[platformProperties.length][];
		for (int i = 0; i < platformProperties.length; i++) {
			String eeSpecs = (String) platformProperties[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			CURRENT_EES[i] = ManifestElement.getArrayFromList(eeSpecs, ","); //$NON-NLS-1$
		}
	}

	private void resolveBundles(ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties, Collection<ResolverBundle> hookDisabled) {

		// First check that all the meta-data is valid for each unresolved bundle
		// This will reset the resolvable flag for each bundle
		for (ResolverBundle bundle : bundles) {
			state.removeResolverErrors(bundle.getBundleDescription());
			// if in development mode then make all bundles resolvable
			// we still want to call isResolvable here to populate any possible ResolverErrors for the bundle
			bundle.setResolvable(isResolvable(bundle, platformProperties, hookDisabled) || developmentMode);
		}
		selectSingletons(bundles);
		resolveBundles0(bundles, platformProperties);
		if (DEBUG_WIRING)
			printWirings();
	}

	private void selectSingletons(ResolverBundle[] bundles) {
		if (developmentMode)
			return; // want all singletons to resolve in devmode
		Map<String, Collection<ResolverBundle>> selectedSingletons = new HashMap<String, Collection<ResolverBundle>>(bundles.length);
		for (ResolverBundle bundle : bundles) {
			if (!bundle.getBundleDescription().isSingleton() || !bundle.isResolvable())
				continue;
			String bsn = bundle.getName();
			Collection<ResolverBundle> selected = selectedSingletons.get(bsn);
			if (selected != null)
				continue; // already processed the bsn
			selected = new ArrayList<ResolverBundle>(1);
			selectedSingletons.put(bsn, selected);

			List<ResolverBundle> sameBSN = resolverBundles.get(bsn);
			if (sameBSN.size() < 2) {
				selected.add(bundle);
				continue;
			}
			// prime selected with resolved singleton bundles
			for (ResolverBundle singleton : sameBSN) {
				if (singleton.getBundleDescription().isSingleton() && singleton.getBundleDescription().isResolved())
					selected.add(singleton);
			}
			// get the collision map for the BSN
			Map<ResolverBundle, Collection<ResolverBundle>> collisionMap = getCollisionMap(sameBSN);
			// process the collision map
			for (ResolverBundle singleton : sameBSN) {
				if (selected.contains(singleton))
					continue; // no need to process resolved bundles
				Collection<ResolverBundle> collisions = collisionMap.get(singleton);
				if (collisions == null || !singleton.isResolvable())
					continue; // not a singleton or not resolvable
				Collection<ResolverBundle> pickOneToResolve = new ArrayList<ResolverBundle>();
				for (ResolverBundle collision : collisions) {
					if (selected.contains(collision)) {
						// Must fail since there is already a selected bundle which is a collision of the singleton bundle
						singleton.setResolvable(false);
						state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, collision.getBundleDescription().toString(), null);
						break;
					}
					if (!pickOneToResolve.contains(collision))
						pickOneToResolve.add(collision);
				}
				// need to make sure the bundle does not collide from the POV of another entry
				for (Map.Entry<ResolverBundle, Collection<ResolverBundle>> collisionEntry : collisionMap.entrySet()) {
					if (collisionEntry.getKey() != singleton && collisionEntry.getValue().contains(singleton)) {
						if (selected.contains(collisionEntry.getKey())) {
							// Must fail since there is already a selected bundle for which the singleton bundle is a collision
							singleton.setResolvable(false);
							state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, collisionEntry.getKey().getBundleDescription().toString(), null);
							break;
						}
						if (!pickOneToResolve.contains(collisionEntry.getKey()))
							pickOneToResolve.add(collisionEntry.getKey());
					}
				}
				if (singleton.isResolvable()) {
					pickOneToResolve.add(singleton);
					selected.add(pickOneToResolve(pickOneToResolve));
				}
			}
		}
	}

	private ResolverBundle pickOneToResolve(Collection<ResolverBundle> pickOneToResolve) {
		ResolverBundle selectedVersion = null;
		for (ResolverBundle singleton : pickOneToResolve) {
			if (selectedVersion == null)
				selectedVersion = singleton;
			boolean higherVersion = selectionPolicy != null ? selectionPolicy.compare(selectedVersion.getBundleDescription(), singleton.getBundleDescription()) > 0 : selectedVersion.getVersion().compareTo(singleton.getVersion()) < 0;
			if (higherVersion)
				selectedVersion = singleton;
		}

		for (ResolverBundle singleton : pickOneToResolve) {
			if (singleton != selectedVersion) {
				singleton.setResolvable(false);
				state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, selectedVersion.getBundleDescription().toString(), null);
			}
		}
		return selectedVersion;
	}

	private Map<ResolverBundle, Collection<ResolverBundle>> getCollisionMap(List<ResolverBundle> sameBSN) {
		Map<ResolverBundle, Collection<ResolverBundle>> result = new HashMap<ResolverBundle, Collection<ResolverBundle>>();
		for (ResolverBundle singleton : sameBSN) {
			if (!singleton.getBundleDescription().isSingleton() || !singleton.isResolvable())
				continue; // ignore non-singleton and non-resolvable
			List<ResolverBundle> collisionCandidates = new ArrayList<ResolverBundle>(sameBSN.size() - 1);
			List<BundleCapability> capabilities = new ArrayList<BundleCapability>(sameBSN.size() - 1);
			for (ResolverBundle collision : sameBSN) {
				if (collision == singleton || !collision.getBundleDescription().isSingleton() || !collision.isResolvable())
					continue; // Ignore the bundle we are checking and non-singletons and non-resolvable
				collisionCandidates.add(collision);
				capabilities.add(getIdentity(collision));
			}
			if (hook != null)
				hook.filterSingletonCollisions(getIdentity(singleton), asCapabilities(new ArrayMap<BundleCapability, ResolverBundle>(capabilities, collisionCandidates)));
			result.put(singleton, collisionCandidates);
		}
		return result;
	}

	private BundleCapability getIdentity(ResolverBundle bundle) {
		List<BundleCapability> identities = bundle.getBundleDescription().getDeclaredCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		return identities.size() == 1 ? identities.get(0) : bundle.getCapability();
	}

	private void resolveBundles0(ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties) {
		if (developmentMode)
			// need to sort bundles to keep consistent order for fragment attachment (bug 174930)
			Arrays.sort(bundles);
		// First attach all fragments to the matching hosts
		Collection<String> processedFragments = new HashSet<String>(bundles.length);
		for (int i = 0; i < bundles.length; i++)
			attachFragment(bundles[i], processedFragments);

		// Lists of cyclic dependencies recording during resolving
		List<ResolverBundle> cycle = new ArrayList<ResolverBundle>(1); // start small
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
			ResolverBundle[] unresolved = unresolvedBundles.toArray(new ResolverBundle[unresolvedBundles.size()]);
			for (int i = 0; i < unresolved.length; i++)
				resolveFragment(unresolved[i]);
		}
		checkUsesConstraints(bundles, platformProperties);
		checkComposites(bundles, platformProperties);
	}

	private void checkComposites(ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties) {
		CompositeResolveHelperRegistry helpers = getCompositeHelpers();
		if (helpers == null)
			return;
		Set<ResolverBundle> exclude = null;
		for (int i = 0; i < bundles.length; i++) {
			CompositeResolveHelper helper = helpers.getCompositeResolveHelper(bundles[i].getBundleDescription());
			if (helper == null)
				continue;
			if (!bundles[i].isResolved())
				continue;
			if (!helper.giveExports(getExportsWiredTo(bundles[i], null))) {
				state.addResolverError(bundles[i].getBundleDescription(), ResolverError.DISABLED_BUNDLE, null, null);
				bundles[i].setResolvable(false);
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(bundles[i], false, false);
				if (exclude == null)
					exclude = new HashSet<ResolverBundle>(1);
				exclude.add(bundles[i]);
			}
		}
		reResolveBundles(exclude, bundles, platformProperties);
	}

	private void checkUsesConstraints(ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties) {
		List<ResolverConstraint> conflictingConstraints = findBestCombination(bundles, platformProperties);
		if (conflictingConstraints == null)
			return;
		Set<ResolverBundle> conflictedBundles = null;
		for (ResolverConstraint conflict : conflictingConstraints) {
			if (conflict.isOptional()) {
				conflict.clearPossibleSuppliers();
				continue;
			}
			if (conflictedBundles == null)
				conflictedBundles = new HashSet<ResolverBundle>(conflictingConstraints.size());
			ResolverBundle conflictedBundle;
			if (conflict.isFromFragment())
				conflictedBundle = bundleMapping.get(conflict.getVersionConstraint().getBundle());
			else
				conflictedBundle = conflict.getBundle();
			if (conflictedBundle != null) {
				if (DEBUG_USES)
					System.out.println("Found conflicting constraint: " + conflict + " in bundle " + conflictedBundle); //$NON-NLS-1$//$NON-NLS-2$
				conflictedBundles.add(conflictedBundle);
				int type = conflict instanceof ResolverImport ? ResolverError.IMPORT_PACKAGE_USES_CONFLICT : ResolverError.REQUIRE_BUNDLE_USES_CONFLICT;
				state.addResolverError(conflictedBundle.getBundleDescription(), type, conflict.getVersionConstraint().toString(), conflict.getVersionConstraint());
				conflictedBundle.setResolvable(false);
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(conflictedBundle, false, false);
			}
		}
		reResolveBundles(conflictedBundles, bundles, platformProperties);
	}

	private void reResolveBundles(Set<ResolverBundle> exclude, ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties) {
		if (exclude == null || exclude.size() == 0)
			return;
		List<ResolverBundle> remainingUnresolved = new ArrayList<ResolverBundle>();
		for (int i = 0; i < bundles.length; i++) {
			if (!exclude.contains(bundles[i])) {
				// We pass false for keepFragmentsAttached because we need to redo the attachments (bug 272561)
				setBundleUnresolved(bundles[i], false, false);
				remainingUnresolved.add(bundles[i]);
			}
		}
		resolveBundles0(remainingUnresolved.toArray(new ResolverBundle[remainingUnresolved.size()]), platformProperties);
	}

	private List<ResolverConstraint> findBestCombination(ResolverBundle[] bundles, Dictionary<Object, Object>[] platformProperties) {
		Object usesMode = platformProperties.length == 0 ? null : platformProperties[0].get("osgi.resolver.usesMode"); //$NON-NLS-1$
		if (usesMode == null)
			usesMode = secureAction.getProperty("osgi.resolver.usesMode"); //$NON-NLS-1$
		if ("ignore".equals(usesMode) || developmentMode) //$NON-NLS-1$
			return null;
		Set<String> bundleConstraints = new HashSet<String>();
		Set<String> packageConstraints = new HashSet<String>();
		// first try out the initial selections
		List<ResolverConstraint> initialConflicts = getConflicts(bundles, packageConstraints, bundleConstraints);
		if (initialConflicts == null || "tryFirst".equals(usesMode) || usesCalculationTimeout) { //$NON-NLS-1$
			groupingChecker.clear();
			// the first combination have no conflicts or 
			// we only are trying the first combination or
			// we have timed out the calculation; return without iterating over all combinations
			return initialConflicts;
		}
		ResolverConstraint[][] multipleSuppliers = getMultipleSuppliers(bundles, packageConstraints, bundleConstraints);
		List<ResolverConstraint> conflicts = null;
		int[] bestCombination = new int[multipleSuppliers.length];
		conflicts = findBestCombination(bundles, multipleSuppliers, bestCombination, initialConflicts);
		if (DEBUG_USES) {
			System.out.print("Best combination found: "); //$NON-NLS-1$
			printCombination(bestCombination);
		}
		for (int i = 0; i < bestCombination.length; i++) {
			for (int j = 0; j < multipleSuppliers[i].length; j++) {
				ResolverConstraint constraint = multipleSuppliers[i][j];
				constraint.setSelectedSupplier(bestCombination[i]);
				// sanity check to make sure we did not just get wired to our own dropped export
				VersionSupplier selectedSupplier = constraint.getSelectedSupplier();
				if (selectedSupplier != null)
					selectedSupplier.setSubstitute(null);
			}
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

	private List<ResolverConstraint> findBestCombination(ResolverBundle[] bundles, ResolverConstraint[][] multipleSuppliers, int[] bestCombination, List<ResolverConstraint> bestConflicts) {
		// now iterate over every possible combination until either zero conflicts are found 
		// or we have run out of combinations
		// if all combinations are tried then return the combination with the lowest number of conflicts
		long initialTime = System.currentTimeMillis();
		long timeLimit;
		if (usesTimeout < 0)
			timeLimit = Math.min(MAX_USES_TIME_BASE + (bundles.length * 30), MAX_USES_TIME_LIMIT);
		else
			timeLimit = usesTimeout;

		int bestConflictCount = getConflictCount(bestConflicts);
		ResolverBundle[] bestConflictBundles = getConflictedBundles(bestConflicts);
		while (bestConflictCount != 0 && getNextCombination(multipleSuppliers)) {
			if ((System.currentTimeMillis() - initialTime) > timeLimit) {
				if (DEBUG_USES)
					System.out.println("Uses constraint check has timedout.  Using the best solution found so far."); //$NON-NLS-1$
				usesCalculationTimeout = true;
				break;
			}
			if (DEBUG_USES)
				printCombination(getCombination(multipleSuppliers, new int[multipleSuppliers.length]));
			// first count the conflicts for the bundles with conflicts from the best combination
			// this significantly reduces the time it takes to populate the GroupingChecker for cases where
			// the combination is no better.
			List<ResolverConstraint> conflicts = getConflicts(bestConflictBundles, null, null);
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

	private ResolverBundle[] getConflictedBundles(List<ResolverConstraint> bestConflicts) {
		if (bestConflicts == null)
			return new ResolverBundle[0];
		List<ResolverBundle> conflictedBundles = new ArrayList<ResolverBundle>(bestConflicts.size());
		for (ResolverConstraint constraint : bestConflicts)
			if (!conflictedBundles.contains(constraint.getBundle()))
				conflictedBundles.add(constraint.getBundle());
		return conflictedBundles.toArray(new ResolverBundle[conflictedBundles.size()]);
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
	private int getConflictCount(List<ResolverConstraint> conflicts) {
		if (conflicts == null || conflicts.size() == 0)
			return 0;
		int result = 0;
		for (ResolverConstraint constraint : conflicts)
			if (!constraint.isOptional())
				result += 1;
		return result;
	}

	private List<ResolverConstraint> getConflicts(ResolverBundle[] bundles, Set<String> packageConstraints, Set<String> bundleConstraints) {
		groupingChecker.clear();
		List<ResolverConstraint> conflicts = null;
		for (int i = 0; i < bundles.length; i++)
			conflicts = addConflicts(bundles[i], packageConstraints, bundleConstraints, conflicts);
		return conflicts;
	}

	private List<ResolverConstraint> addConflicts(ResolverBundle bundle, Set<String> packageConstraints, Set<String> bundleConstraints, List<ResolverConstraint> conflicts) {
		BundleConstraint[] requires = bundle.getRequires();
		for (int i = 0; i < requires.length; i++) {
			ResolverBundle selectedSupplier = (ResolverBundle) requires[i].getSelectedSupplier();
			PackageRoots[][] conflict = selectedSupplier == null ? null : groupingChecker.isConsistent(bundle, selectedSupplier);
			if (conflict != null) {
				addConflictNames(conflict, packageConstraints, bundleConstraints);
				if (conflicts == null)
					conflicts = new ArrayList<ResolverConstraint>(1);
				conflicts.add(requires[i]);
			}
		}
		ResolverImport[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ResolverExport selectedSupplier = (ResolverExport) imports[i].getSelectedSupplier();
			PackageRoots[][] conflict = selectedSupplier == null ? null : groupingChecker.isConsistent(bundle, selectedSupplier);
			if (conflict != null) {
				addConflictNames(conflict, packageConstraints, bundleConstraints);
				if (conflicts == null)
					conflicts = new ArrayList<ResolverConstraint>(1);
				conflicts.add(imports[i]);
			}
		}

		GenericConstraint[] genericRequires = bundle.getGenericRequires();
		for (GenericConstraint capabilityRequirement : genericRequires) {
			VersionSupplier[] suppliers = capabilityRequirement.getMatchingCapabilities();
			if (suppliers == null)
				continue;
			for (VersionSupplier supplier : suppliers) {
				PackageRoots[][] conflict = groupingChecker.isConsistent(bundle, (GenericCapability) supplier);
				if (conflict != null) {
					addConflictNames(conflict, packageConstraints, bundleConstraints);
					if (conflicts == null)
						conflicts = new ArrayList<ResolverConstraint>(1);
					conflicts.add(capabilityRequirement);
				}
			}
		}
		return conflicts;
	}

	// records the conflict names we can use to scope down the list of multiple suppliers
	private void addConflictNames(PackageRoots[][] conflict, Set<String> packageConstraints, Set<String> bundleConstraints) {
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
	private ResolverConstraint[][] getMultipleSuppliers(ResolverBundle[] bundles, Set<String> packageConstraints, Set<String> bundleConstraints) {
		List<ResolverImport> multipleImportSupplierList = new ArrayList<ResolverImport>(1);
		List<BundleConstraint> multipleRequireSupplierList = new ArrayList<BundleConstraint>(1);
		List<GenericConstraint> multipleGenericSupplierList = new ArrayList<GenericConstraint>(1);
		for (ResolverBundle bundle : bundles) {
			BundleConstraint[] requires = bundle.getRequires();
			for (BundleConstraint require : requires)
				if (require.getNumPossibleSuppliers() > 1)
					multipleRequireSupplierList.add(require);
			ResolverImport[] imports = bundle.getImportPackages();
			for (ResolverImport importPkg : imports) {
				if (importPkg.getNumPossibleSuppliers() > 1) {
					Integer eeProfile = (Integer) ((ResolverExport) importPkg.getSelectedSupplier()).getExportPackageDescription().getDirective(ExportPackageDescriptionImpl.EQUINOX_EE);
					if (eeProfile.intValue() < 0) {
						// this is a normal package; always add it
						multipleImportSupplierList.add(importPkg);
					} else {
						// this is a system bundle export
						// If other exporters of this package also require the system bundle
						// then this package does not need to be added to the mix
						// this is an optimization for bundles like org.eclipse.xerces
						// that export lots of packages also exported by the system bundle on J2SE 1.4
						VersionSupplier[] suppliers = importPkg.getPossibleSuppliers();
						for (int suppliersIndex = 1; suppliersIndex < suppliers.length; suppliersIndex++) {
							Integer ee = (Integer) ((ResolverExport) suppliers[suppliersIndex]).getExportPackageDescription().getDirective(ExportPackageDescriptionImpl.EQUINOX_EE);
							if (ee.intValue() >= 0)
								continue;
							if (((ResolverExport) suppliers[suppliersIndex]).getExporter().getRequire(getSystemBundle()) == null)
								if (((ResolverExport) suppliers[suppliersIndex]).getExporter().getRequire(Constants.SYSTEM_BUNDLE_SYMBOLICNAME) == null) {
									multipleImportSupplierList.add(importPkg);
									break;
								}
						}
					}
				}
			}
			GenericConstraint[] genericRequires = bundle.getGenericRequires();
			for (GenericConstraint genericRequire : genericRequires)
				if (genericRequire.getNumPossibleSuppliers() > 1 && genericRequire.supplierHasUses())
					multipleGenericSupplierList.add(genericRequire);
		}
		List<ResolverConstraint[]> results = new ArrayList<ResolverConstraint[]>();
		if (multipleImportSupplierList.size() + multipleRequireSupplierList.size() + multipleGenericSupplierList.size() > usesMultipleSuppliersLimit) {
			// we have hit a max on the multiple suppliers in the lists without merging.
			// first merge the identical constraints that have identical suppliers
			Map<String, List<List<ResolverConstraint>>> multipleImportSupplierMaps = new HashMap<String, List<List<ResolverConstraint>>>();
			for (ResolverImport importPkg : multipleImportSupplierList)
				addMutipleSupplierConstraint(multipleImportSupplierMaps, importPkg, importPkg.getName());
			Map<String, List<List<ResolverConstraint>>> multipleRequireSupplierMaps = new HashMap<String, List<List<ResolverConstraint>>>();
			for (BundleConstraint requireBundle : multipleRequireSupplierList)
				addMutipleSupplierConstraint(multipleRequireSupplierMaps, requireBundle, requireBundle.getName());
			Map<String, List<List<ResolverConstraint>>> multipleGenericSupplierMaps = new HashMap<String, List<List<ResolverConstraint>>>();
			for (GenericConstraint genericRequire : multipleGenericSupplierList)
				addMutipleSupplierConstraint(multipleGenericSupplierMaps, genericRequire, genericRequire.getNameSpace());
			addMergedSuppliers(results, multipleImportSupplierMaps);
			addMergedSuppliers(results, multipleRequireSupplierMaps);
			addMergedSuppliers(results, multipleGenericSupplierMaps);
			// check the results to see if we have reduced the number enough
			if (results.size() > usesMultipleSuppliersLimit && packageConstraints != null && bundleConstraints != null) {
				// we still have too big of a list; filter out constraints that are not in conflict
				List<ResolverConstraint[]> tooBig = results;
				results = new ArrayList<ResolverConstraint[]>();
				for (ResolverConstraint[] constraints : tooBig) {
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
			for (ResolverConstraint constraint : multipleImportSupplierList)
				results.add(new ResolverConstraint[] {constraint});
			for (ResolverConstraint constraint : multipleRequireSupplierList)
				results.add(new ResolverConstraint[] {constraint});
			for (ResolverConstraint constraint : multipleGenericSupplierList)
				results.add(new ResolverConstraint[] {constraint});

		}
		return results.toArray(new ResolverConstraint[results.size()][]);
	}

	String getSystemBundle() {
		Dictionary<?, ?>[] platformProperties = state.getPlatformProperties();
		String systemBundle = platformProperties.length == 0 ? null : (String) platformProperties[0].get(Constants.STATE_SYSTEM_BUNDLE);
		if (systemBundle == null)
			systemBundle = Constants.getInternalSymbolicName();
		return systemBundle;
	}

	private void addMergedSuppliers(List<ResolverConstraint[]> mergedSuppliers, Map<String, List<List<ResolverConstraint>>> constraints) {
		for (List<List<ResolverConstraint>> mergedConstraintLists : constraints.values()) {
			for (List<ResolverConstraint> constraintList : mergedConstraintLists) {
				mergedSuppliers.add(constraintList.toArray(new ResolverConstraint[constraintList.size()]));
			}
		}
	}

	private void addMutipleSupplierConstraint(Map<String, List<List<ResolverConstraint>>> constraints, ResolverConstraint constraint, String key) {
		List<List<ResolverConstraint>> mergedConstraintLists = constraints.get(key);
		if (mergedConstraintLists == null) {
			mergedConstraintLists = new ArrayList<List<ResolverConstraint>>(0);
			List<ResolverConstraint> constraintList = new ArrayList<ResolverConstraint>(1);
			constraintList.add(constraint);
			mergedConstraintLists.add(constraintList);
			constraints.put(key, mergedConstraintLists);
			return;
		}
		for (List<ResolverConstraint> constraintList : mergedConstraintLists) {
			ResolverConstraint mergedConstraint = constraintList.get(0);
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
		List<ResolverConstraint> constraintList = new ArrayList<ResolverConstraint>(1);
		constraintList.add(constraint);
		mergedConstraintLists.add(constraintList);
	}

	private void checkCycle(List<ResolverBundle> cycle) {
		int cycleSize = cycle.size();
		if (cycleSize == 0)
			return;
		cycleLoop: for (Iterator<ResolverBundle> iCycle = cycle.iterator(); iCycle.hasNext();) {
			ResolverBundle cycleBundle = iCycle.next();
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
					state.addResolverError(imports[j].getVersionConstraint().getBundle(), ResolverError.MISSING_IMPORT_PACKAGE, imports[j].getVersionConstraint().toString(), imports[j].getVersionConstraint());
					iCycle.remove();
					continue cycleLoop;
				}
			}
		}
		if (cycle.size() != cycleSize) {
			//we removed an un-resolvable bundle; must re-resolve remaining cycle
			for (int i = 0; i < cycle.size(); i++) {
				ResolverBundle cycleBundle = cycle.get(i);
				cycleBundle.clearWires();
			}
			List<ResolverBundle> innerCycle = new ArrayList<ResolverBundle>(cycle.size());
			for (int i = 0; i < cycle.size(); i++)
				resolveBundle(cycle.get(i), innerCycle);
			checkCycle(innerCycle);
		} else {
			for (int i = 0; i < cycle.size(); i++) {
				if (DEBUG || DEBUG_CYCLES)
					ResolverImpl.log("Pushing " + cycle.get(i) + " to RESOLVED"); //$NON-NLS-1$ //$NON-NLS-2$
				setBundleResolved(cycle.get(i));
			}
		}
	}

	@SuppressWarnings("unchecked")
	static Collection<BundleCapability> asCapabilities(Collection<? extends BundleCapability> capabilities) {
		return (Collection<BundleCapability>) capabilities;
	}

	private void resolveFragment(ResolverBundle fragment) {
		if (!fragment.isFragment())
			return;
		if (fragment.getHost().getNumPossibleSuppliers() > 0)
			if (!developmentMode || state.getResolverErrors(fragment.getBundleDescription()).length == 0)
				setBundleResolved(fragment);
	}

	// This method will attempt to resolve the supplied bundle and any bundles that it is dependent on
	private boolean resolveBundle(ResolverBundle bundle, List<ResolverBundle> cycle) {
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
				if (genericRequires[i].isEffective()) {
					if (!resolveGenericReq(genericRequires[i], cycle)) {
						if (DEBUG || DEBUG_GENERICS)
							ResolverImpl.log("** GENERICS " + genericRequires[i].getVersionConstraint().getName() + "[" + genericRequires[i].getBundleDescription() + "] failed to resolve"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						state.addResolverError(genericRequires[i].getVersionConstraint().getBundle(), ResolverError.MISSING_GENERIC_CAPABILITY, genericRequires[i].getVersionConstraint().toString(), genericRequires[i].getVersionConstraint());
						if (genericRequires[i].isFromFragment()) {
							if (!developmentMode) // only detach fragments when not in devmode
								bundle.detachFragment(bundleMapping.get(genericRequires[i].getVersionConstraint().getBundle()), null);
							continue;
						}
						if (!developmentMode) {
							// fail fast; otherwise we want to attempt to resolver other constraints in dev mode
							failed = true;
							break;
						}
					} else {
						if (StateImpl.OSGI_EE_NAMESPACE.equals(genericRequires[i].getNameSpace())) {
							VersionSupplier supplier = genericRequires[i].getSelectedSupplier();
							Integer ee = supplier == null ? null : (Integer) ((GenericDescription) supplier.getBaseDescription()).getAttributes().get(ExportPackageDescriptionImpl.EQUINOX_EE);
							if (ee != null && ((BundleDescriptionImpl) bundle.getBaseDescription()).getEquinoxEE() < 0)
								((BundleDescriptionImpl) bundle.getBundleDescription()).setEquinoxEE(ee);
						}
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
							bundle.detachFragment(bundleMapping.get(requires[i].getVersionConstraint().getBundle()), requires[i]);
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
							bundle.detachFragment(bundleMapping.get(imports[i].getVersionConstraint().getBundle()), imports[i]);
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
		if (developmentMode && !failed && state.getResolverErrors(bundle.getBundleDescription()).length > 0)
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
			BundleDescription fragment = fragments[i].getBundleDescription();
			if (bundle.constraintsConflict(fragment, fragment.getImportPackages(), fragment.getRequiredBundles(), fragment.getGenericRequires()) && !developmentMode)
				// found some conflicts; detach the fragment
				bundle.detachFragment(fragments[i], null);
		}
	}

	private boolean resolveGenericReq(GenericConstraint constraint, List<ResolverBundle> cycle) {
		if (DEBUG_GENERICS)
			ResolverImpl.log("Trying to resolve: " + constraint.getBundle() + ", " + constraint.getVersionConstraint()); //$NON-NLS-1$ //$NON-NLS-2$
		VersionSupplier matchingCapability = constraint.getSelectedSupplier();
		if (matchingCapability != null) {
			if (!cycle.contains(constraint.getBundle())) {
				cycle.add(constraint.getBundle());
				if (DEBUG_CYCLES)
					ResolverImpl.log("generic cycle: " + constraint.getBundle() + " -> " + constraint.getSelectedSupplier()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (DEBUG_GENERICS)
				ResolverImpl.log("  - already wired"); //$NON-NLS-1$
			return true; // Already wired (due to grouping dependencies) so just return
		}
		List<GenericCapability> candidates;
		long timestamp;
		do {
			timestamp = state.getTimeStamp();
			VersionHashMap<GenericCapability> namespace = resolverGenerics.get(constraint.getNameSpace());
			String name = constraint.getName();
			List<GenericCapability> capabilities;
			if (namespace == null)
				capabilities = Collections.EMPTY_LIST;
			else
				capabilities = name == null || name.indexOf('*') >= 0 ? namespace.getAllValues() : namespace.get(name);
			candidates = new ArrayList<GenericCapability>(capabilities);
			List<BundleCapability> genCapabilities = new ArrayList<BundleCapability>(candidates.size());
			// Must remove candidates that do not match before calling hooks.
			for (Iterator<GenericCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
				GenericCapability capability = iCandidates.next();
				if (!constraint.isSatisfiedBy(capability)) {
					iCandidates.remove();
				} else {
					genCapabilities.add(capability.getCapability());
				}
			}
			if (hook != null)
				hook.filterMatches(constraint.getRequirement(), asCapabilities(new ArrayMap<BundleCapability, GenericCapability>(genCapabilities, candidates)));
		} while (timestamp != state.getTimeStamp());
		boolean result = false;
		// We are left with only capabilities that satisfy the constraint.
		for (GenericCapability capability : candidates) {
			if (DEBUG_GENERICS)
				ResolverImpl.log("CHECKING GENERICS: " + capability.getBaseDescription()); //$NON-NLS-1$

			// first add the possible supplier; this is done before resolving the supplier bundle to prevent endless cycle loops.
			constraint.addPossibleSupplier(capability); // Wire to the capability
			if (constraint.getBundle() == capability.getResolverBundle()) {
				result = true; // Wired to ourselves
				continue;
			}
			VersionSupplier[] capabilityHosts = capability.getResolverBundle().isFragment() ? capability.getResolverBundle().getHost().getPossibleSuppliers() : new ResolverBundle[] {capability.getResolverBundle()};
			boolean foundResolvedMatch = false;
			for (int i = 0; capabilityHosts != null && i < capabilityHosts.length; i++) {
				ResolverBundle capabilitySupplier = capabilityHosts[i].getResolverBundle();
				if (capabilitySupplier == constraint.getBundle()) {
					// the capability is from a fragment attached to this host do not recursively resolve the host again
					foundResolvedMatch = true;
					continue;
				}
				boolean successfulResolve = false;
				if (capabilitySupplier.getState() != ResolverBundle.RESOLVED) {
					// only attempt to resolve the supplier if not osgi.ee name space
					if (!StateImpl.OSGI_EE_NAMESPACE.equals(constraint.getNameSpace()))
						successfulResolve = resolveBundle(capabilitySupplier, cycle);
				}

				// if in dev mode then allow a constraint to resolve to an unresolved bundle
				if (capabilitySupplier.getState() == ResolverBundle.RESOLVED || (successfulResolve || developmentMode)) {
					foundResolvedMatch |= !capability.getResolverBundle().isFragment() ? true : capability.getResolverBundle().getHost().getPossibleSuppliers() != null;
					// Check cyclic dependencies
					if (capabilitySupplier.getState() == ResolverBundle.RESOLVING)
						if (!cycle.contains(capabilitySupplier))
							cycle.add(capabilitySupplier);
				}
			}
			if (!foundResolvedMatch) {
				constraint.removePossibleSupplier(capability);
				continue; // constraint hasn't resolved
			}
			if (DEBUG_GENERICS)
				ResolverImpl.log("Found match: " + capability.getBaseDescription() + ". Wiring"); //$NON-NLS-1$ //$NON-NLS-2$
			result = true;
		}
		return result ? true : constraint.isOptional() || constraint.isFromRequiredEE();
	}

	// Resolve the supplied import. Returns true if the import can be resolved, false otherwise
	private boolean resolveRequire(BundleConstraint req, List<ResolverBundle> cycle) {
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
		List<ResolverBundle> candidates;
		long timestamp;
		do {
			timestamp = state.getTimeStamp();
			List<ResolverBundle> bundles = resolverBundles.get(req.getVersionConstraint().getName());
			candidates = new ArrayList<ResolverBundle>(bundles);
			List<BundleCapability> capabilities = new ArrayList<BundleCapability>(candidates.size());
			// Must remove candidates that do not match before calling hooks.
			for (Iterator<ResolverBundle> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
				ResolverBundle bundle = iCandidates.next();
				if (!req.isSatisfiedBy(bundle)) {
					iCandidates.remove();
				} else {
					capabilities.add(bundle.getCapability());
				}
			}
			if (hook != null)
				hook.filterMatches(req.getRequirement(), asCapabilities(new ArrayMap<BundleCapability, ResolverBundle>(capabilities, candidates)));
		} while (timestamp != state.getTimeStamp());
		// We are left with only capabilities that satisfy the require bundle.
		boolean result = false;
		for (ResolverBundle bundle : candidates) {
			if (DEBUG_REQUIRES)
				ResolverImpl.log("CHECKING: " + bundle.getBundleDescription()); //$NON-NLS-1$

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
				ResolverImpl.log("Found match: " + bundle.getBundleDescription() + ". Wiring"); //$NON-NLS-1$ //$NON-NLS-2$
			result = true;
		}

		if (result || req.isOptional())
			return true; // If the req is optional then just return true

		return false;
	}

	// Resolve the supplied import. Returns true if the import can be resolved, false otherwise
	private boolean resolveImport(ResolverImport imp, List<ResolverBundle> cycle) {
		if (DEBUG_IMPORTS)
			ResolverImpl.log("Trying to resolve: " + imp.getBundle() + ", " + imp.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		if (imp.getSelectedSupplier() != null) {
			// Check for unrecorded cyclic dependency
			if (!cycle.contains(imp.getBundle())) {
				cycle.add(imp.getBundle());
				if (DEBUG_CYCLES)
					ResolverImpl.log("import-package cycle: " + imp.getBundle() + " -> " + imp.getSelectedSupplier() + " from " + imp.getSelectedSupplier().getBundleDescription()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (DEBUG_IMPORTS)
				ResolverImpl.log("  - already wired"); //$NON-NLS-1$
			return true; // Already wired (due to grouping dependencies) so just return
		}
		boolean result = false;
		ResolverExport[] substitutableExps = imp.getBundle().getExports(imp.getName());
		long timestamp;
		List<ResolverExport> candidates;
		do {
			timestamp = state.getTimeStamp();
			List<ResolverExport> exports = resolverExports.get(imp.getName());
			candidates = new ArrayList<ResolverExport>(exports);
			List<BundleCapability> capabilities = new ArrayList<BundleCapability>(candidates.size());
			// Must remove candidates that do not match before calling hooks.
			for (Iterator<ResolverExport> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
				ResolverExport export = iCandidates.next();
				if (!imp.isSatisfiedBy(export)) {
					iCandidates.remove();
				} else {
					capabilities.add(export.getCapability());
				}
			}
			if (hook != null)
				hook.filterMatches(imp.getRequirement(), asCapabilities(new ArrayMap<BundleCapability, ResolverExport>(capabilities, candidates)));
		} while (timestamp != state.getTimeStamp());
		// We are left with only capabilities that satisfy the import.
		for (ResolverExport export : candidates) {
			if (DEBUG_IMPORTS)
				ResolverImpl.log("CHECKING: " + export.getExporter().getBundleDescription() + ", " + export.getName()); //$NON-NLS-1$ //$NON-NLS-2$

			int originalState = export.getExporter().getState();
			if (imp.isDynamic() && originalState != ResolverBundle.RESOLVED)
				continue; // Must not attempt to resolve an exporter when dynamic
			if (imp.getSelectedSupplier() != null && ((ResolverExport) imp.getSelectedSupplier()).getExporter() == imp.getBundle())
				break; // We wired to ourselves; nobody else matters
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
							ResolverImpl.log("import-package cycle: " + imp.getBundle() + " -> " + imp.getSelectedSupplier() + " from " + imp.getSelectedSupplier().getBundleDescription()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			if (DEBUG_IMPORTS)
				ResolverImpl.log("Found match: " + export.getExporter() + ". Wiring " + imp.getBundle() + ":" + imp.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			result = true;
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
			removeGenerics(bundle.getGenericCapabilities());
			bundle.detachAllFragments();
			bundle.detachFromHosts();
			bundle.initialize(false);
			if (!removed) {
				// add back the available exports/capabilities
				resolverExports.put(bundle.getExportPackages());
				addGenerics(bundle.getGenericCapabilities());
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
			if (!resolvedBundles[i].getBundleDescription().isResolved())
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
			BaseDescription supplier = bundle == null ? null : bundle.getBundleDescription();
			state.resolveConstraint(requires[i].getVersionConstraint(), supplier);
		}
		GenericConstraint[] genericRequires = rb.getGenericRequires();
		for (int i = 0; i < genericRequires.length; i++) {
			VersionSupplier[] matchingCapabilities = genericRequires[i].getMatchingCapabilities();
			if (matchingCapabilities == null)
				state.resolveConstraint(genericRequires[i].getVersionConstraint(), null);
			else
				for (int j = 0; j < matchingCapabilities.length; j++)
					state.resolveConstraint(genericRequires[i].getVersionConstraint(), matchingCapabilities[j].getBaseDescription());
		}
	}

	private void stateResolveFragConstraints(ResolverBundle rb) {
		ResolverBundle host = (ResolverBundle) rb.getHost().getSelectedSupplier();
		ImportPackageSpecification[] imports = rb.getBundleDescription().getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ResolverImport hostImport = host == null ? null : host.getImport(imports[i].getName());
			ResolverExport export = (ResolverExport) (hostImport == null ? null : hostImport.getSelectedSupplier());
			BaseDescription supplier = export == null ? null : export.getExportPackageDescription();
			state.resolveConstraint(imports[i], supplier);
		}
		BundleSpecification[] requires = rb.getBundleDescription().getRequiredBundles();
		for (int i = 0; i < requires.length; i++) {
			BundleConstraint hostRequire = host == null ? null : host.getRequire(requires[i].getName());
			ResolverBundle bundle = (ResolverBundle) (hostRequire == null ? null : hostRequire.getSelectedSupplier());
			BaseDescription supplier = bundle == null ? null : bundle.getBundleDescription();
			state.resolveConstraint(requires[i], supplier);
		}
		GenericConstraint[] genericRequires = rb.getGenericRequires();
		for (int i = 0; i < genericRequires.length; i++) {
			VersionSupplier[] matchingCapabilities = genericRequires[i].getMatchingCapabilities();
			if (matchingCapabilities == null)
				state.resolveConstraint(genericRequires[i].getVersionConstraint(), null);
			else
				for (int j = 0; j < matchingCapabilities.length; j++)
					state.resolveConstraint(genericRequires[i].getVersionConstraint(), matchingCapabilities[j].getBaseDescription());
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

		// Build up the state wires
		Map<String, List<StateWire>> stateWires = new HashMap<String, List<StateWire>>();

		// Gather selected exports
		ResolverExport[] exports = rb.getSelectedExports();
		List<ExportPackageDescription> selectedExports = new ArrayList<ExportPackageDescription>(exports.length);
		for (int i = 0; i < exports.length; i++) {
			if (permissionChecker.checkPackagePermission(exports[i].getExportPackageDescription()))
				selectedExports.add(exports[i].getExportPackageDescription());
		}
		ExportPackageDescription[] selectedExportsArray = selectedExports.toArray(new ExportPackageDescription[selectedExports.size()]);

		// Gather substitute exports
		ResolverExport[] substituted = rb.getSubstitutedExports();
		List<ExportPackageDescription> substitutedExports = new ArrayList<ExportPackageDescription>(substituted.length);
		for (int i = 0; i < substituted.length; i++) {
			substitutedExports.add(substituted[i].getExportPackageDescription());
		}
		ExportPackageDescription[] substitutedExportsArray = substitutedExports.toArray(new ExportPackageDescription[substitutedExports.size()]);

		// Gather exports that have been wired to
		ExportPackageDescription[] exportsWiredToArray = getExportsWiredTo(rb, stateWires);

		// Gather bundles that have been wired to
		BundleConstraint[] requires = rb.getRequires();
		List<BundleDescription> bundlesWiredTo = new ArrayList<BundleDescription>(requires.length);
		List<StateWire> requireWires = new ArrayList<StateWire>(requires.length);
		for (int i = 0; i < requires.length; i++)
			if (requires[i].getSelectedSupplier() != null) {
				BundleDescription supplier = (BundleDescription) requires[i].getSelectedSupplier().getBaseDescription();
				bundlesWiredTo.add(supplier);
				StateWire requireWire = newStateWire(rb.getBundleDescription(), requires[i].getVersionConstraint(), supplier, supplier);
				requireWires.add(requireWire);
			}
		BundleDescription[] bundlesWiredToArray = bundlesWiredTo.toArray(new BundleDescription[bundlesWiredTo.size()]);
		if (!requireWires.isEmpty())
			stateWires.put(BundleRevision.BUNDLE_NAMESPACE, requireWires);

		GenericCapability[] capabilities = rb.getGenericCapabilities();
		List<GenericDescription> selectedCapabilities = new ArrayList<GenericDescription>(capabilities.length);
		for (GenericCapability capability : capabilities)
			if (capability.isEffective() && permissionChecker.checkCapabilityPermission(capability.getGenericDescription()))
				selectedCapabilities.add(capability.getGenericDescription());
		GenericDescription[] selectedCapabilitiesArray = selectedCapabilities.toArray(new GenericDescription[selectedCapabilities.size()]);

		GenericConstraint[] genericRequires = rb.getGenericRequires();
		List<GenericDescription> resolvedGenericRequires = new ArrayList<GenericDescription>(genericRequires.length);
		for (GenericConstraint genericConstraint : genericRequires) {
			VersionSupplier[] matching = genericConstraint.getMatchingCapabilities();
			if (matching != null)
				for (VersionSupplier capability : matching) {
					GenericDescription supplier = ((GenericCapability) capability).getGenericDescription();
					resolvedGenericRequires.add(supplier);
					StateWire genericWire = newStateWire(rb.getBundleDescription(), genericConstraint.getVersionConstraint(), supplier.getSupplier(), supplier);
					List<StateWire> genericWires = stateWires.get(genericConstraint.getNameSpace());
					if (genericWires == null) {
						genericWires = new ArrayList<StateWire>();
						stateWires.put(genericConstraint.getNameSpace(), genericWires);
					}
					genericWires.add(genericWire);
				}
		}
		GenericDescription[] capabilitiesWiredToArray = resolvedGenericRequires.toArray(new GenericDescription[resolvedGenericRequires.size()]);

		BundleDescription[] hostBundles = null;
		if (rb.isFragment()) {
			VersionSupplier[] matchingBundles = rb.getHost().getPossibleSuppliers();
			if (matchingBundles != null && matchingBundles.length > 0) {
				hostBundles = new BundleDescription[matchingBundles.length];
				List<StateWire> hostWires = new ArrayList<StateWire>(matchingBundles.length);
				stateWires.put(BundleRevision.HOST_NAMESPACE, hostWires);
				for (int i = 0; i < matchingBundles.length; i++) {
					hostBundles[i] = matchingBundles[i].getBundleDescription();
					StateWire hostWire = newStateWire(rb.getBundleDescription(), rb.getHost().getVersionConstraint(), hostBundles[i], hostBundles[i]);
					hostWires.add(hostWire);
					if (hostBundles[i].isResolved()) {
						ExportPackageDescription[] newSelectedExports = null;
						GenericDescription[] newSelectedCapabilities = null;
						if (rb.isNewFragmentExports()) {
							// update the host's set of selected exports
							ResolverExport[] hostExports = ((ResolverBundle) matchingBundles[i]).getSelectedExports();
							newSelectedExports = new ExportPackageDescription[hostExports.length];
							for (int j = 0; j < hostExports.length; j++)
								newSelectedExports[j] = hostExports[j].getExportPackageDescription();
						}
						if (rb.isNewFragmentCapabilities()) {
							// update the host's set of selected capabilities
							GenericCapability[] hostCapabilities = ((ResolverBundle) matchingBundles[i]).getGenericCapabilities();
							newSelectedCapabilities = new GenericDescription[hostCapabilities.length];
							for (int j = 0; j < hostCapabilities.length; j++)
								newSelectedCapabilities[j] = hostCapabilities[j].getGenericDescription();
						}
						if (newSelectedCapabilities != null || newSelectedExports != null) {
							if (newSelectedCapabilities == null)
								newSelectedCapabilities = hostBundles[i].getSelectedGenericCapabilities();
							if (newSelectedExports == null)
								newSelectedExports = hostBundles[i].getSelectedExports();
							state.resolveBundle(hostBundles[i], true, null, newSelectedExports, hostBundles[i].getSubstitutedExports(), newSelectedCapabilities, hostBundles[i].getResolvedRequires(), hostBundles[i].getResolvedImports(), hostBundles[i].getResolvedGenericRequires(), ((BundleDescriptionImpl) hostBundles[i]).getWires());
						}
					}
				}
			}
		}

		// Resolve the bundle in the state
		state.resolveBundle(rb.getBundleDescription(), rb.isResolved(), hostBundles, selectedExportsArray, substitutedExportsArray, selectedCapabilitiesArray, bundlesWiredToArray, exportsWiredToArray, capabilitiesWiredToArray, stateWires);
	}

	private static ExportPackageDescription[] getExportsWiredTo(ResolverBundle rb, Map<String, List<StateWire>> stateWires) {
		// Gather exports that have been wired to
		ResolverImport[] imports = rb.getImportPackages();
		List<ExportPackageDescription> exportsWiredTo = new ArrayList<ExportPackageDescription>(imports.length);
		List<StateWire> importWires = new ArrayList<StateWire>(imports.length);
		for (int i = 0; i < imports.length; i++)
			if (imports[i].getSelectedSupplier() != null) {
				ExportPackageDescription supplier = (ExportPackageDescription) imports[i].getSelectedSupplier().getBaseDescription();
				exportsWiredTo.add(supplier);
				StateWire wire = newStateWire(rb.getBundleDescription(), imports[i].getVersionConstraint(), supplier.getExporter(), supplier);
				importWires.add(wire);
			}
		if (stateWires != null && !importWires.isEmpty())
			stateWires.put(BundleRevision.PACKAGE_NAMESPACE, importWires);
		return exportsWiredTo.toArray(new ExportPackageDescription[exportsWiredTo.size()]);
	}

	private static StateWire newStateWire(BundleDescription requirementHost, VersionConstraint declaredRequirement, BundleDescription capabilityHost, BaseDescription declaredCapability) {
		BaseDescription fragDeclared = ((BaseDescriptionImpl) declaredCapability).getFragmentDeclaration();
		declaredCapability = fragDeclared != null ? fragDeclared : declaredCapability;
		return new StateWire(requirementHost, declaredRequirement, capabilityHost, declaredCapability);
	}

	// Resolve dynamic import
	public synchronized ExportPackageDescription resolveDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		if (state == null)
			throw new IllegalStateException("RESOLVER_NO_STATE"); //$NON-NLS-1$

		// Make sure the resolver is initialized
		if (!initialized)
			initialize();
		hook = (state instanceof StateImpl) ? ((StateImpl) state).getResolverHook() : null;
		try {
			ResolverBundle rb = bundleMapping.get(importingBundle);
			if (rb.getExport(requestedPackage) != null)
				return null; // do not allow dynamic wires for packages which this bundle exports
			ResolverImport[] resolverImports = rb.getImportPackages();
			// Check through the ResolverImports of this bundle.
			// If there is a matching one then pass it into resolveImport()
			for (int j = 0; j < resolverImports.length; j++) {
				// Make sure it is a dynamic import
				if (!resolverImports[j].isDynamic())
					continue;
				// Resolve the import
				ExportPackageDescription supplier = resolveDynamicImport(resolverImports[j], requestedPackage);
				if (supplier != null)
					return supplier;
			}
			// look for packages added dynamically
			ImportPackageSpecification[] addedDynamicImports = importingBundle.getAddedDynamicImportPackages();
			for (ImportPackageSpecification addedDynamicImport : addedDynamicImports) {
				ResolverImport newImport = new ResolverImport(rb, addedDynamicImport);
				ExportPackageDescription supplier = resolveDynamicImport(newImport, requestedPackage);
				if (supplier != null)
					return supplier;
			}

			if (DEBUG || DEBUG_IMPORTS)
				ResolverImpl.log("Failed to resolve dynamic import: " + requestedPackage); //$NON-NLS-1$
			return null; // Couldn't resolve the import, so return null
		} finally {
			hook = null;
		}
	}

	private void addStateWire(BundleDescription importingBundle, VersionConstraint requirement, BundleDescription capabilityHost, ExportPackageDescription capability) {
		Map<String, List<StateWire>> wires = ((BundleDescriptionImpl) importingBundle).getWires();
		List<StateWire> imports = wires.get(BundleRevision.PACKAGE_NAMESPACE);
		if (imports == null) {
			imports = new ArrayList<StateWire>();
			wires.put(BundleRevision.PACKAGE_NAMESPACE, imports);
		}
		imports.add(newStateWire(importingBundle, requirement, capabilityHost, capability));
	}

	private ExportPackageDescription resolveDynamicImport(ResolverImport dynamicImport, String requestedPackage) {
		String importName = dynamicImport.getName();
		// If the import uses a wildcard, then temporarily replace this with the requested package
		if (importName.equals("*") || //$NON-NLS-1$
				(importName.endsWith(".*") && requestedPackage.startsWith(importName.substring(0, importName.length() - 1)))) { //$NON-NLS-1$
			dynamicImport.setName(requestedPackage);
		}
		try {
			// Resolve the import
			if (!requestedPackage.equals(dynamicImport.getName()))
				return null;

			if (resolveImport(dynamicImport, new ArrayList<ResolverBundle>())) {
				// populate the grouping checker with current imports
				groupingChecker.populateRoots(dynamicImport.getBundle());
				while (dynamicImport.getSelectedSupplier() != null) {
					if (groupingChecker.isDynamicConsistent(dynamicImport.getBundle(), (ResolverExport) dynamicImport.getSelectedSupplier()) != null) {
						dynamicImport.selectNextSupplier(); // not consistent; try the next
					} else {
						// If the import resolved then return it's matching export
						if (DEBUG_IMPORTS)
							ResolverImpl.log("Resolved dynamic import: " + dynamicImport.getBundle() + ":" + dynamicImport.getName() + " -> " + ((ResolverExport) dynamicImport.getSelectedSupplier()).getExporter() + ":" + requestedPackage); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

						// now that we have an export to wire to; populate the roots for that package for the bundle
						ResolverExport export = (ResolverExport) dynamicImport.getSelectedSupplier();
						groupingChecker.populateRoots(dynamicImport.getBundle(), export);

						ExportPackageDescription supplier = export.getExportPackageDescription();
						if (supplier != null)
							addStateWire(dynamicImport.getBundleDescription(), dynamicImport.getVersionConstraint(), supplier.getExporter(), supplier);
						return supplier;
					}
				}
				dynamicImport.clearPossibleSuppliers();
			}
		} finally {
			// If it is a wildcard import then clear the wire, so other
			// exported packages can be found for it
			if (importName.endsWith("*")) //$NON-NLS-1$
				dynamicImport.clearPossibleSuppliers();
			// Reset the import package name
			dynamicImport.setName(null);
		}
		return null;
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
		addGenerics(rb.getGenericCapabilities());
		if (hook != null && rb.isFragment()) {
			attachFragment0(rb);
		}
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
		ResolverBundle rb = bundleMapping.get(bundle);
		if (rb == null)
			return;

		if (!pending) {
			bundleMapping.remove(bundle);
			groupingChecker.clear(rb);
		}
		if (!pending || !bundle.isResolved()) {
			resolverExports.remove(rb.getExportPackages());
			resolverBundles.remove(rb);
			removeGenerics(rb.getGenericCapabilities());
		}
		unresolvedBundles.remove(rb);
	}

	private void unresolveBundle(ResolverBundle bundle, boolean removed) {
		if (bundle == null)
			return;
		// check the removed list if unresolving then remove from the removed list
		List<BundleDescription> removedBundles = removalPending.remove(new Long(bundle.getBundleDescription().getBundleId()));
		for (BundleDescription removedDesc : removedBundles) {
			ResolverBundle re = bundleMapping.get(removedDesc);
			unresolveBundle(re, true);
			state.removeBundleComplete(removedDesc);
			resolverExports.remove(re.getExportPackages());
			resolverBundles.remove(re);
			removeGenerics(re.getGenericCapabilities());
			bundleMapping.remove(removedDesc);
			groupingChecker.clear(re);
			// the bundle is removed
			if (removedDesc == bundle.getBundleDescription())
				removed = true;
		}

		if (!bundle.getBundleDescription().isResolved() && !developmentMode)
			return;
		CompositeResolveHelperRegistry currentLinks = compositeHelpers;
		if (currentLinks != null) {
			CompositeResolveHelper helper = currentLinks.getCompositeResolveHelper(bundle.getBundleDescription());
			if (helper != null)
				helper.giveExports(null);
		}
		// if not removed then add to the list of unresolvedBundles,
		// passing false for devmode because we need all fragments detached
		setBundleUnresolved(bundle, removed, false);
		// Get bundles dependent on 'bundle'
		BundleDescription[] dependents = bundle.getBundleDescription().getDependents();
		state.resolveBundle(bundle.getBundleDescription(), false, null, null, null, null, null, null, null, null);
		// Unresolve dependents of 'bundle'
		for (int i = 0; i < dependents.length; i++)
			unresolveBundle(bundleMapping.get(dependents[i]), false);
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
		List<BundleDescription> removed = removalPending.getAllValues();
		for (BundleDescription removedDesc : removed)
			state.removeBundleComplete(removedDesc);
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
		List<ResolverBundle> bundles = resolverBundles.getAllValues();
		for (ResolverBundle rb : bundles) {
			if (rb.getBundleDescription().isResolved()) {
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
						ResolverImpl.log("        (r) " + rb.getBundleDescription() + " -> NULL!!!"); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						ResolverImpl.log("        (r) " + rb.getBundleDescription() + " -> " + requireBundles[i].getSelectedSupplier()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			// Hosts
			BundleConstraint hostSpec = rb.getHost();
			if (hostSpec != null) {
				VersionSupplier[] hosts = hostSpec.getPossibleSuppliers();
				if (hosts != null)
					for (int i = 0; i < hosts.length; i++) {
						ResolverImpl.log("        (h) " + rb.getBundleDescription() + " -> " + hosts[i].getBundleDescription()); //$NON-NLS-1$ //$NON-NLS-2$
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

	VersionHashMap<ResolverExport> getResolverExports() {
		return resolverExports;
	}

	public void setSelectionPolicy(Comparator<BaseDescription> selectionPolicy) {
		this.selectionPolicy = selectionPolicy;
	}

	public Comparator<BaseDescription> getSelectionPolicy() {
		return selectionPolicy;
	}

	public void setCompositeResolveHelperRegistry(CompositeResolveHelperRegistry compositeHelpers) {
		this.compositeHelpers = compositeHelpers;
	}

	CompositeResolveHelperRegistry getCompositeHelpers() {
		return compositeHelpers;
	}

	private void reorderGenerics() {
		for (VersionHashMap<GenericCapability> namespace : resolverGenerics.values())
			namespace.reorder();
	}

	void removeGenerics(GenericCapability[] generics) {
		for (GenericCapability capability : generics) {
			VersionHashMap<GenericCapability> namespace = resolverGenerics.get(capability.getGenericDescription().getType());
			if (namespace != null)
				namespace.remove(capability);
		}
	}

	void addGenerics(GenericCapability[] generics) {
		for (GenericCapability capability : generics) {
			if (!capability.isEffective())
				continue;
			String type = capability.getGenericDescription().getType();
			VersionHashMap<GenericCapability> namespace = resolverGenerics.get(type);
			if (namespace == null) {
				namespace = new VersionHashMap<GenericCapability>(this);
				resolverGenerics.put(type, namespace);
			}
			namespace.put(capability.getName(), capability);
		}
	}
}
