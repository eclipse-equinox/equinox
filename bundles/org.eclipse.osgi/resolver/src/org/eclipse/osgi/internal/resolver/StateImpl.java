/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.framework.util.*;
import org.eclipse.osgi.internal.baseadaptor.StateManager;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

public abstract class StateImpl implements State {
	private static final String OSGI_OS = "osgi.os"; //$NON-NLS-1$
	private static final String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	private static final String OSGI_NL = "osgi.nl"; //$NON-NLS-1$
	private static final String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String[] PROPS = {OSGI_OS, OSGI_WS, OSGI_NL, OSGI_ARCH, Constants.FRAMEWORK_SYSTEMPACKAGES, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Constants.OSGI_RESOLVER_MODE, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases", Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION, Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_LANGUAGE, Constants.STATE_SYSTEM_BUNDLE, Constants.FRAMEWORK_SYSTEMCAPABILITIES, Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final DisabledInfo[] EMPTY_DISABLEDINFOS = new DisabledInfo[0];
	public static final String OSGI_EE_NAMESPACE = "osgi.ee"; //$NON-NLS-1$

	transient private Resolver resolver;
	transient private StateDeltaImpl changes;
	transient private boolean resolving = false;
	transient private LinkedList<BundleDescription> removalPendings = new LinkedList<BundleDescription>();

	private boolean resolved = true;
	private long timeStamp = System.currentTimeMillis();
	private final KeyedHashSet bundleDescriptions = new KeyedHashSet(false);
	private final Map<BundleDescription, List<ResolverError>> resolverErrors = new HashMap<BundleDescription, List<ResolverError>>();
	private StateObjectFactory factory;
	private final KeyedHashSet resolvedBundles = new KeyedHashSet();
	private final Map<BundleDescription, List<DisabledInfo>> disabledBundles = new HashMap<BundleDescription, List<DisabledInfo>>();
	private boolean fullyLoaded = false;
	private boolean dynamicCacheChanged = false;
	// only used for lazy loading of BundleDescriptions
	private StateReader reader;
	@SuppressWarnings("unchecked")
	private Dictionary<Object, Object>[] platformProperties = new Dictionary[] {new Hashtable<String, String>(PROPS.length)}; // Dictionary here because of Filter API
	private long highestBundleId = -1;
	private final Set<String> platformPropertyKeys = new HashSet<String>(PROPS.length);
	private ResolverHookFactory hookFactory;
	private ResolverHook hook;
	private boolean developmentMode = false;

	private static long cumulativeTime;

	final Object monitor = new Object();

	// to prevent extra-package instantiation 
	protected StateImpl() {
		// always add the default platform property keys.
		addPlatformPropertyKeys(PROPS);
	}

	public boolean addBundle(BundleDescription description) {
		synchronized (this.monitor) {
			if (!basicAddBundle(description))
				return false;
			String platformFilter = description.getPlatformFilter();
			if (platformFilter != null) {
				try {
					// add any new platform filter propery keys this bundle is using
					FilterImpl filter = FilterImpl.newInstance(platformFilter);
					addPlatformPropertyKeys(filter.getAttributes());
				} catch (InvalidSyntaxException e) {
					// ignore this is handled in another place
				}
			}
			NativeCodeSpecification nativeCode = description.getNativeCodeSpecification();
			if (nativeCode != null) {
				NativeCodeDescription[] suppliers = nativeCode.getPossibleSuppliers();
				for (int i = 0; i < suppliers.length; i++) {
					FilterImpl filter = (FilterImpl) suppliers[i].getFilter();
					if (filter != null)
						addPlatformPropertyKeys(filter.getAttributes());
				}
			}
			resolved = false;
			getDelta().recordBundleAdded((BundleDescriptionImpl) description);
			if (getSystemBundle().equals(description.getSymbolicName()))
				resetAllSystemCapabilities();
			if (resolver != null)
				resolver.bundleAdded(description);
			updateTimeStamp();
			return true;
		}
	}

	public boolean updateBundle(BundleDescription newDescription) {
		synchronized (this.monitor) {
			BundleDescriptionImpl existing = (BundleDescriptionImpl) bundleDescriptions.get((BundleDescriptionImpl) newDescription);
			if (existing == null)
				return false;
			if (!bundleDescriptions.remove(existing))
				return false;
			resolvedBundles.remove(existing);
			List<DisabledInfo> infos = disabledBundles.remove(existing);
			if (infos != null) {
				List<DisabledInfo> newInfos = new ArrayList<DisabledInfo>(infos.size());
				for (Iterator<DisabledInfo> iInfos = infos.iterator(); iInfos.hasNext();) {
					DisabledInfo info = iInfos.next();
					newInfos.add(new DisabledInfo(info.getPolicyName(), info.getMessage(), newDescription));
				}
				disabledBundles.put(newDescription, newInfos);
			}
			existing.setStateBit(BundleDescriptionImpl.REMOVAL_PENDING, true);
			if (!basicAddBundle(newDescription))
				return false;
			resolved = false;
			getDelta().recordBundleUpdated((BundleDescriptionImpl) newDescription);
			if (getSystemBundle().equals(newDescription.getSymbolicName()))
				resetAllSystemCapabilities();
			if (resolver != null) {
				boolean pending = isInUse(existing);
				resolver.bundleUpdated(newDescription, existing, pending);
				if (pending) {
					getDelta().recordBundleRemovalPending(existing);
					addRemovalPending(existing);
				} else {
					// an existing bundle has been updated with no dependents it can safely be unresolved now
					try {
						resolving = true;
						resolverErrors.remove(existing);
						resolveBundle(existing, false, null, null, null, null, null, null, null, null);
					} finally {
						resolving = false;
					}
				}
			}
			updateTimeStamp();
			return true;
		}
	}

	public BundleDescription removeBundle(long bundleId) {
		synchronized (this.monitor) {
			BundleDescription toRemove = getBundle(bundleId);
			if (toRemove == null || !removeBundle(toRemove))
				return null;
			return toRemove;
		}
	}

	public boolean removeBundle(BundleDescription toRemove) {
		synchronized (this.monitor) {
			toRemove = (BundleDescription) bundleDescriptions.get((KeyedElement) toRemove);
			if (toRemove == null || !bundleDescriptions.remove((KeyedElement) toRemove))
				return false;
			resolvedBundles.remove((KeyedElement) toRemove);
			disabledBundles.remove(toRemove);
			resolved = false;
			getDelta().recordBundleRemoved((BundleDescriptionImpl) toRemove);
			((BundleDescriptionImpl) toRemove).setStateBit(BundleDescriptionImpl.REMOVAL_PENDING, true);
			if (resolver != null) {
				boolean pending = isInUse(toRemove);
				resolver.bundleRemoved(toRemove, pending);
				if (pending) {
					getDelta().recordBundleRemovalPending((BundleDescriptionImpl) toRemove);
					addRemovalPending(toRemove);
				} else {
					// a bundle has been removed with no dependents it can safely be unresolved now
					try {
						resolving = true;
						resolverErrors.remove(toRemove);
						resolveBundle(toRemove, false, null, null, null, null, null);
					} finally {
						resolving = false;
					}
				}
			}
			updateTimeStamp();
			return true;
		}
	}

	private boolean isInUse(BundleDescription bundle) {
		Object userObject = bundle.getUserObject();
		if (userObject instanceof BundleLoaderProxy)
			return ((BundleLoaderProxy) userObject).inUse();
		return bundle.getDependents().length > 0;
	}

	public StateDelta getChanges() {
		synchronized (this.monitor) {
			return getDelta();
		}
	}

	private StateDeltaImpl getDelta() {
		if (changes == null)
			changes = new StateDeltaImpl(this);
		return changes;
	}

	public BundleDescription[] getBundles(String symbolicName) {
		synchronized (this.monitor) {
			if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName))
				symbolicName = getSystemBundle();
			final List<BundleDescription> bundles = new ArrayList<BundleDescription>();
			for (Iterator<KeyedElement> iter = bundleDescriptions.iterator(); iter.hasNext();) {
				BundleDescription bundle = (BundleDescription) iter.next();
				if (symbolicName.equals(bundle.getSymbolicName()))
					bundles.add(bundle);
			}
			return bundles.toArray(new BundleDescription[bundles.size()]);
		}
	}

	public BundleDescription[] getBundles() {
		synchronized (this.monitor) {
			return (BundleDescription[]) bundleDescriptions.elements(new BundleDescription[bundleDescriptions.size()]);
		}
	}

	public BundleDescription getBundle(long id) {
		synchronized (this.monitor) {
			BundleDescription result = (BundleDescription) bundleDescriptions.getByKey(new Long(id));
			if (result != null)
				return result;
			// need to look in removal pending bundles;
			for (Iterator<BundleDescription> iter = removalPendings.iterator(); iter.hasNext();) {
				BundleDescription removedBundle = iter.next();
				if (removedBundle.getBundleId() == id) // just return the first matching id
					return removedBundle;
			}
			return null;
		}
	}

	public BundleDescription getBundle(String name, Version version) {
		synchronized (this.monitor) {
			BundleDescription[] allBundles = getBundles(name);
			if (allBundles.length == 1)
				return version == null || allBundles[0].getVersion().equals(version) ? allBundles[0] : null;
			if (allBundles.length == 0)
				return null;
			BundleDescription unresolvedFound = null;
			BundleDescription resolvedFound = null;
			for (int i = 0; i < allBundles.length; i++) {
				BundleDescription current = allBundles[i];
				BundleDescription base;

				if (current.isResolved())
					base = resolvedFound;
				else
					base = unresolvedFound;

				if (version == null || current.getVersion().equals(version)) {
					if (base != null && (base.getVersion().compareTo(current.getVersion()) <= 0 || base.getBundleId() > current.getBundleId())) {
						if (base == resolvedFound)
							resolvedFound = current;
						else
							unresolvedFound = current;
					} else {
						if (current.isResolved())
							resolvedFound = current;
						else
							unresolvedFound = current;
					}

				}
			}
			if (resolvedFound != null)
				return resolvedFound;
			return unresolvedFound;
		}
	}

	public long getTimeStamp() {
		synchronized (this.monitor) {
			return timeStamp;
		}
	}

	public boolean isResolved() {
		synchronized (this.monitor) {
			return resolved || isEmpty();
		}
	}

	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier) {
		((VersionConstraintImpl) constraint).setSupplier(supplier);
	}

	/**
	 * @deprecated
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		resolveBundle(bundle, status, hosts, selectedExports, null, resolvedRequires, resolvedImports);
	}

	/**
	 * @deprecated
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		resolveBundle(bundle, status, hosts, selectedExports, substitutedExports, null, resolvedRequires, resolvedImports, null, null);
	}

	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedWires) {
		synchronized (this.monitor) {
			if (!resolving)
				throw new IllegalStateException(); // TODO need error message here!
			BundleDescriptionImpl modifiable = (BundleDescriptionImpl) bundle;
			// must record the change before setting the resolve state to 
			// accurately record if a change has happened.
			getDelta().recordBundleResolved(modifiable, status);
			// force the new resolution data to stay in memory; we will not read this from disk anymore
			modifiable.setLazyLoaded(false);
			modifiable.setStateBit(BundleDescriptionImpl.RESOLVED, status);
			if (status) {
				resolverErrors.remove(modifiable);
				resolvedBundles.add(modifiable);
			} else {
				// remove the bundle from the resolved pool
				resolvedBundles.remove(modifiable);
				modifiable.removeDependencies();
			}
			// to support development mode we will resolveConstraints even if the resolve status == false
			// we only do this if the resolved constraints are not null
			if (selectedExports == null || resolvedRequires == null || resolvedImports == null)
				unresolveConstraints(modifiable);
			else
				resolveConstraints(modifiable, hosts, selectedExports, substitutedExports, selectedCapabilities, resolvedRequires, resolvedImports, resolvedCapabilities, resolvedWires);
		}
	}

	public void removeBundleComplete(BundleDescription bundle) {
		synchronized (this.monitor) {
			if (!resolving)
				throw new IllegalStateException(); // TODO need error message here!
			getDelta().recordBundleRemovalComplete((BundleDescriptionImpl) bundle);
			removalPendings.remove(bundle);
		}
	}

	private void resolveConstraints(BundleDescriptionImpl bundle, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedWires) {
		HostSpecificationImpl hostSpec = (HostSpecificationImpl) bundle.getHost();
		if (hostSpec != null) {
			if (hosts != null) {
				hostSpec.setHosts(hosts);
				for (int i = 0; i < hosts.length; i++) {
					((BundleDescriptionImpl) hosts[i]).addDependency(bundle, true);
					checkHostForSubstitutedExports((BundleDescriptionImpl) hosts[i], bundle);
				}
			}
		}

		bundle.setSelectedExports(selectedExports);
		bundle.setResolvedRequires(resolvedRequires);
		bundle.setResolvedImports(resolvedImports);
		bundle.setSubstitutedExports(substitutedExports);
		bundle.setSelectedCapabilities(selectedCapabilities);
		bundle.setResolvedCapabilities(resolvedCapabilities);
		bundle.setStateWires(resolvedWires);

		bundle.addDependencies(hosts, true);
		bundle.addDependencies(resolvedRequires, true);
		bundle.addDependencies(resolvedImports, true);
		bundle.addDependencies(resolvedCapabilities, true);
	}

	private void checkHostForSubstitutedExports(BundleDescriptionImpl host, BundleDescriptionImpl fragment) {
		// TODO need to handle this case where a fragment has its own export substituted
		// there are issues here because the order in which fragments are resolved is not always the same ...
	}

	private void unresolveConstraints(BundleDescriptionImpl bundle) {
		HostSpecificationImpl host = (HostSpecificationImpl) bundle.getHost();
		if (host != null)
			host.setHosts(null);

		bundle.setSelectedExports(null);
		bundle.setResolvedImports(null);
		bundle.setResolvedRequires(null);
		bundle.setSubstitutedExports(null);
		bundle.setSelectedCapabilities(null);
		bundle.setResolvedCapabilities(null);
		bundle.setStateWires(null);
		bundle.clearAddedDynamicImportPackages();

		// remove the constraint suppliers
		NativeCodeSpecificationImpl nativeCode = (NativeCodeSpecificationImpl) bundle.getNativeCodeSpecification();
		if (nativeCode != null)
			nativeCode.setSupplier(null);
		ImportPackageSpecification[] imports = bundle.getImportPackages();
		for (int i = 0; i < imports.length; i++)
			((ImportPackageSpecificationImpl) imports[i]).setSupplier(null);
		BundleSpecification[] requires = bundle.getRequiredBundles();
		for (int i = 0; i < requires.length; i++)
			((BundleSpecificationImpl) requires[i]).setSupplier(null);
		GenericSpecification[] genericRequires = bundle.getGenericRequires();
		if (genericRequires.length > 0)
			for (int i = 0; i < genericRequires.length; i++)
				((GenericSpecificationImpl) genericRequires[i]).setSupplers(null);

		bundle.removeDependencies();
	}

	private StateDelta resolve(boolean incremental, BundleDescription[] reResolve, BundleDescription[] triggers) {
		fullyLoad();
		synchronized (this.monitor) {
			if (resolver == null)
				throw new IllegalStateException("no resolver set"); //$NON-NLS-1$
			if (resolving == true)
				throw new IllegalStateException("An attempt to start a nested resolve process has been detected."); //$NON-NLS-1$
			try {
				resolving = true;
				long start = 0;
				if (StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER)
					start = System.currentTimeMillis();
				if (!incremental) {
					resolved = false;
					reResolve = getBundles();
					// need to get any removal pendings before flushing
					if (removalPendings.size() > 0) {
						BundleDescription[] removed = internalGetRemovalPending();
						reResolve = mergeBundles(reResolve, removed);
					}
					flush(reResolve);
				} else {
					if (resolved && reResolve == null)
						return new StateDeltaImpl(this);
					if (developmentMode) {
						// in dev mode we need to aggressively flush removal pendings 
						if (removalPendings.size() > 0) {
							BundleDescription[] removed = internalGetRemovalPending();
							reResolve = mergeBundles(reResolve, removed);
						}
					}
					if (reResolve == null)
						reResolve = internalGetRemovalPending();
					if (triggers == null) {
						Set<BundleDescription> triggerSet = new HashSet<BundleDescription>();
						Collection<BundleDescription> closure = getDependencyClosure(Arrays.asList(reResolve));
						for (BundleDescription toRefresh : closure) {
							Bundle bundle = toRefresh.getBundle();
							if (bundle != null && (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED | Bundle.RESOLVED)) == 0)
								triggerSet.add(toRefresh);
						}
						triggers = triggerSet.toArray(new BundleDescription[triggerSet.size()]);
					}
				}
				// use the Headers class to handle ignoring case while matching keys (bug 180817)
				@SuppressWarnings("unchecked")
				Headers<Object, Object>[] tmpPlatformProperties = new Headers[platformProperties.length];
				for (int i = 0; i < platformProperties.length; i++) {
					tmpPlatformProperties[i] = new Headers<Object, Object>(platformProperties[i].size());
					for (Enumeration<Object> keys = platformProperties[i].keys(); keys.hasMoreElements();) {
						Object key = keys.nextElement();
						tmpPlatformProperties[i].put(key, platformProperties[i].get(key));
					}
				}

				ResolverHookFactory currentFactory = hookFactory;
				if (currentFactory != null) {
					@SuppressWarnings("unchecked")
					Collection<BundleRevision> triggerRevisions = Collections.unmodifiableCollection(triggers == null ? Collections.EMPTY_LIST : Arrays.asList((BundleRevision[]) triggers));
					begin(triggerRevisions);
				}
				ResolverHookException error = null;
				try {
					resolver.resolve(reResolve, tmpPlatformProperties);
				} catch (ResolverHookException e) {
					error = e;
					resolverErrors.clear();
				}
				resolved = removalPendings.size() == 0;

				StateDeltaImpl savedChanges = changes == null ? new StateDeltaImpl(this) : changes;
				savedChanges.setResolverHookException(error);
				changes = new StateDeltaImpl(this);

				if (StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER) {
					long time = System.currentTimeMillis() - start;
					Debug.println("Time spent resolving: " + time); //$NON-NLS-1$
					cumulativeTime = cumulativeTime + time;
					FrameworkDebugOptions.getDefault().setOption("org.eclipse.core.runtime.adaptor/resolver/timing/value", Long.toString(cumulativeTime)); //$NON-NLS-1$
				}
				if (savedChanges.getChanges().length > 0)
					updateTimeStamp();
				return savedChanges;
			} finally {
				resolving = false;
			}
		}
	}

	private BundleDescription[] mergeBundles(BundleDescription[] reResolve, BundleDescription[] removed) {
		if (reResolve == null)
			return removed; // just return all the removed bundles
		if (reResolve.length == 0)
			return reResolve; // if reResolve length==0 then we want to prevent pending removal
		// merge in all removal pending bundles that are not already in the list
		List<BundleDescription> result = new ArrayList<BundleDescription>(reResolve.length + removed.length);
		for (int i = 0; i < reResolve.length; i++)
			result.add(reResolve[i]);
		for (int i = 0; i < removed.length; i++) {
			boolean found = false;
			for (int j = 0; j < reResolve.length; j++) {
				if (removed[i] == reResolve[j]) {
					found = true;
					break;
				}
			}
			if (!found)
				result.add(removed[i]);
		}
		return result.toArray(new BundleDescription[result.size()]);
	}

	private void flush(BundleDescription[] bundles) {
		resolver.flush();
		resolved = false;
		resolverErrors.clear();
		if (resolvedBundles.isEmpty())
			return;
		for (int i = 0; i < bundles.length; i++) {
			resolveBundle(bundles[i], false, null, null, null, null, null);
		}
		resolvedBundles.clear();
	}

	public StateDelta resolve() {
		return resolve(true, null, null);
	}

	public StateDelta resolve(boolean incremental) {
		return resolve(incremental, null, null);
	}

	public StateDelta resolve(BundleDescription[] reResolve) {
		return resolve(true, reResolve, null);
	}

	public StateDelta resolve(BundleDescription[] resolve, boolean discard) {
		BundleDescription[] reResolve = discard ? resolve : new BundleDescription[0];
		BundleDescription[] triggers = discard ? null : resolve;
		return resolve(true, reResolve, triggers);
	}

	@SuppressWarnings("deprecation")
	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	public void setResolverHookFactory(ResolverHookFactory hookFactory) {
		synchronized (this.monitor) {
			if (this.hookFactory != null)
				throw new IllegalStateException("Resolver hook factory is already set."); //$NON-NLS-1$
			this.hookFactory = hookFactory;
		}
	}

	private ResolverHook begin(Collection<BundleRevision> triggers) {
		ResolverHookFactory current;
		synchronized (this.monitor) {
			current = this.hookFactory;
		}
		ResolverHook newHook = current.begin(triggers);
		synchronized (this.monitor) {
			this.hook = newHook;
		}
		return newHook;
	}

	ResolverHookFactory getResolverHookFactory() {
		synchronized (this.monitor) {
			return this.hookFactory;
		}
	}

	public ResolverHook getResolverHook() {
		synchronized (this.monitor) {
			return this.hook;
		}
	}

	public BundleDescription[] getResolvedBundles() {
		synchronized (this.monitor) {
			return (BundleDescription[]) resolvedBundles.elements(new BundleDescription[resolvedBundles.size()]);
		}
	}

	public boolean isEmpty() {
		synchronized (this.monitor) {
			return bundleDescriptions.isEmpty();
		}
	}

	void setResolved(boolean resolved) {
		synchronized (this.monitor) {
			this.resolved = resolved;
		}
	}

	boolean basicAddBundle(BundleDescription description) {
		synchronized (this.monitor) {
			StateImpl origState = (StateImpl) description.getContainingState();
			if (origState != null && origState != this) {
				if (origState.removalPendings.contains(description))
					throw new IllegalStateException(NLS.bind(StateMsg.BUNDLE_PENDING_REMOVE_STATE, description.toString()));
				if (origState.getBundle(description.getBundleId()) == description)
					throw new IllegalStateException(NLS.bind(StateMsg.BUNDLE_IN_OTHER_STATE, description.toString()));
			}
			((BundleDescriptionImpl) description).setContainingState(this);
			((BundleDescriptionImpl) description).setStateBit(BundleDescriptionImpl.REMOVAL_PENDING, false);
			if (bundleDescriptions.add((BundleDescriptionImpl) description)) {
				if (description.getBundleId() > getHighestBundleId())
					highestBundleId = description.getBundleId();
				return true;
			}
			return false;
		}
	}

	void addResolvedBundle(BundleDescriptionImpl resolvedBundle) {
		synchronized (this.monitor) {
			resolvedBundles.add(resolvedBundle);
		}
	}

	public ExportPackageDescription[] getExportedPackages() {
		fullyLoad();
		synchronized (this.monitor) {
			List<ExportPackageDescription> allExportedPackages = new ArrayList<ExportPackageDescription>();
			for (Iterator<KeyedElement> iter = resolvedBundles.iterator(); iter.hasNext();) {
				BundleDescription bundle = (BundleDescription) iter.next();
				ExportPackageDescription[] bundlePackages = bundle.getSelectedExports();
				if (bundlePackages == null)
					continue;
				for (int i = 0; i < bundlePackages.length; i++)
					allExportedPackages.add(bundlePackages[i]);
			}
			for (Iterator<BundleDescription> iter = removalPendings.iterator(); iter.hasNext();) {
				BundleDescription bundle = iter.next();
				ExportPackageDescription[] bundlePackages = bundle.getSelectedExports();
				if (bundlePackages == null)
					continue;
				for (int i = 0; i < bundlePackages.length; i++)
					allExportedPackages.add(bundlePackages[i]);
			}
			return allExportedPackages.toArray(new ExportPackageDescription[allExportedPackages.size()]);
		}
	}

	BundleDescription[] getFragments(final BundleDescription host) {
		final List<BundleDescription> fragments = new ArrayList<BundleDescription>();
		for (Iterator<KeyedElement> iter = bundleDescriptions.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			HostSpecification hostSpec = bundle.getHost();

			if (hostSpec != null) {
				BundleDescription[] hosts = hostSpec.getHosts();
				if (hosts != null)
					for (int i = 0; i < hosts.length; i++)
						if (hosts[i] == host) {
							fragments.add(bundle);
							break;
						}
			}
		}
		return fragments.toArray(new BundleDescription[fragments.size()]);
	}

	public void setTimeStamp(long newTimeStamp) {
		synchronized (this.monitor) {
			timeStamp = newTimeStamp;
		}
	}

	private void updateTimeStamp() {
		synchronized (this.monitor) {
			if (getTimeStamp() == Long.MAX_VALUE)
				setTimeStamp(0);
			setTimeStamp(getTimeStamp() + 1);
		}
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	void setFactory(StateObjectFactory factory) {
		this.factory = factory;
	}

	public BundleDescription getBundleByLocation(String location) {
		synchronized (this.monitor) {
			for (Iterator<KeyedElement> i = bundleDescriptions.iterator(); i.hasNext();) {
				BundleDescription current = (BundleDescription) i.next();
				if (location.equals(current.getLocation()))
					return current;
			}
			return null;
		}
	}

	public Resolver getResolver() {
		synchronized (this.monitor) {
			return resolver;
		}
	}

	public void setResolver(Resolver newResolver) {
		if (resolver == newResolver)
			return;
		if (resolver != null) {
			Resolver oldResolver = resolver;
			resolver = null;
			oldResolver.setState(null);
		}
		synchronized (this.monitor) {
			resolver = newResolver;
		}
		if (resolver == null)
			return;
		resolver.setState(this);
	}

	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties) {
		return setPlatformProperties(new Dictionary[] {platformProperties});
	}

	public boolean setPlatformProperties(Dictionary<?, ?>[] platformProperties) {
		return setPlatformProperties(platformProperties, true);
	}

	synchronized boolean setPlatformProperties(Dictionary<?, ?>[] platformProperties, boolean resetSystemExports) {
		if (platformProperties.length == 0)
			throw new IllegalArgumentException();
		// copy the properties for our use internally;
		// only copy String and String[] values
		@SuppressWarnings("unchecked")
		Dictionary<Object, Object>[] newPlatformProperties = new Dictionary[platformProperties.length];
		for (int i = 0; i < platformProperties.length; i++) {
			newPlatformProperties[i] = new Hashtable<Object, Object>(platformProperties[i].size());
			synchronized (platformProperties[i]) {
				for (Enumeration<?> keys = platformProperties[i].keys(); keys.hasMoreElements();) {
					Object key = keys.nextElement();
					Object value = platformProperties[i].get(key);
					newPlatformProperties[i].put(key, value);
				}
			}
			// make sure the bundle native code osgi properties have decent defaults
			if (newPlatformProperties[i].get(Constants.FRAMEWORK_OS_NAME) == null && newPlatformProperties[i].get(OSGI_OS) != null)
				newPlatformProperties[i].put(Constants.FRAMEWORK_OS_NAME, newPlatformProperties[i].get(OSGI_OS));
			if (newPlatformProperties[i].get(Constants.FRAMEWORK_PROCESSOR) == null && newPlatformProperties[i].get(OSGI_ARCH) != null)
				newPlatformProperties[i].put(Constants.FRAMEWORK_PROCESSOR, newPlatformProperties[i].get(OSGI_ARCH));
			if (newPlatformProperties[i].get(Constants.FRAMEWORK_LANGUAGE) == null && newPlatformProperties[i].get(OSGI_NL) instanceof String) {
				String osgiNL = (String) newPlatformProperties[i].get(OSGI_NL);
				int idx = osgiNL.indexOf('_');
				if (idx >= 0)
					osgiNL = osgiNL.substring(0, idx);
				newPlatformProperties[i].put(Constants.FRAMEWORK_LANGUAGE, osgiNL);
			}

		}
		boolean result = false;
		boolean performResetSystemExports = false;
		boolean performResetSystemCapabilities = false;
		if (this.platformProperties.length != newPlatformProperties.length) {
			result = true;
			performResetSystemExports = true;
			performResetSystemCapabilities = true;
		} else {
			// we need to see if any of the existing filter prop keys have changed
			String[] keys = getPlatformPropertyKeys();
			for (int i = 0; i < newPlatformProperties.length && !result; i++) {
				result |= changedProps(this.platformProperties[i], newPlatformProperties[i], keys);
				if (resetSystemExports) {
					performResetSystemExports |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES), newPlatformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES));
					performResetSystemExports |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA), newPlatformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
					performResetSystemExports |= checkProp(this.platformProperties[i].get(Constants.SYSTEM_BUNDLE_SYMBOLICNAME), newPlatformProperties[i].get(Constants.SYSTEM_BUNDLE_SYMBOLICNAME));
					performResetSystemCapabilities |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES), newPlatformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
					performResetSystemCapabilities |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA), newPlatformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA));
					performResetSystemCapabilities |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT), newPlatformProperties[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
				}
			}
		}
		// always do a complete replacement of the properties in case new bundles are added that uses new filter props
		this.platformProperties = newPlatformProperties;
		if (performResetSystemExports)
			resetSystemExports();
		if (performResetSystemCapabilities)
			resetSystemCapabilities();
		developmentMode = this.platformProperties.length == 0 ? false : org.eclipse.osgi.framework.internal.core.Constants.DEVELOPMENT_MODE.equals(this.platformProperties[0].get(org.eclipse.osgi.framework.internal.core.Constants.OSGI_RESOLVER_MODE));
		return result;
	}

	private void resetAllSystemCapabilities() {
		resetSystemExports();
		resetSystemCapabilities();
	}

	private void resetSystemExports() {
		BundleDescription[] systemBundles = getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		for (int idx = 0; idx < systemBundles.length; idx++) {
			BundleDescriptionImpl systemBundle = (BundleDescriptionImpl) systemBundles[idx];
			ExportPackageDescription[] exports = systemBundle.getExportPackages();
			List<ExportPackageDescription> newExports = new ArrayList<ExportPackageDescription>(exports.length);
			for (int i = 0; i < exports.length; i++)
				if (((Integer) exports[i].getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue() < 0)
					newExports.add(exports[i]);
			addSystemExports(newExports);
			systemBundle.setExportPackages(newExports.toArray(new ExportPackageDescription[newExports.size()]));
		}
	}

	private void addSystemExports(List<ExportPackageDescription> exports) {
		for (int i = 0; i < platformProperties.length; i++)
			try {
				addSystemExports(exports, ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) platformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES)), i);
				addSystemExports(exports, ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) platformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA)), i);
			} catch (BundleException e) {
				// TODO consider throwing this... 
			}
	}

	private void addSystemExports(List<ExportPackageDescription> exports, ManifestElement[] elements, int index) {
		if (elements == null)
			return;
		ExportPackageDescription[] systemExports = StateBuilder.createExportPackages(elements, null, null, false);
		Integer profInx = new Integer(index);
		for (int j = 0; j < systemExports.length; j++) {
			((ExportPackageDescriptionImpl) systemExports[j]).setDirective(ExportPackageDescriptionImpl.EQUINOX_EE, profInx);
			exports.add(systemExports[j]);
		}
	}

	private void resetSystemCapabilities() {
		BundleDescription[] systemBundles = getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		for (BundleDescription systemBundle : systemBundles) {
			GenericDescription[] capabilities = systemBundle.getGenericCapabilities();
			List<GenericDescription> newCapabilities = new ArrayList<GenericDescription>(capabilities.length);
			for (GenericDescription capability : capabilities) {
				Object equinoxEEIndex = capability.getDeclaredAttributes().get(ExportPackageDescriptionImpl.EQUINOX_EE);
				if (equinoxEEIndex == null)
					newCapabilities.add(capability); // keep the built in ones.
			}
			// now add the externally defined ones
			addSystemCapabilities(newCapabilities);
			((BundleDescriptionImpl) systemBundle).setGenericCapabilities(newCapabilities.toArray(new GenericDescription[newCapabilities.size()]));
		}
	}

	private void addSystemCapabilities(List<GenericDescription> capabilities) {
		for (int i = 0; i < platformProperties.length; i++)
			try {
				addSystemCapabilities(capabilities, ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, (String) platformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES)), i);
				addSystemCapabilities(capabilities, ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, (String) platformProperties[i].get(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA)), i);
				checkOSGiEE(capabilities, (String) platformProperties[i].get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT), i);
			} catch (BundleException e) {
				// TODO consider throwing this... 
			}
	}

	private void checkOSGiEE(List<GenericDescription> capabilities, String profileEE, Integer profileIndex) {
		if (profileEE == null || profileEE.length() == 0)
			return;
		for (GenericDescription capability : capabilities) {
			if (OSGI_EE_NAMESPACE.equals(capability.getType()) && profileIndex.equals(capability.getAttributes().get(ExportPackageDescriptionImpl.EQUINOX_EE)))
				return; // profile already specifies osgi.ee capabilities
		}
		Map<String, List<String>> eeVersions = new HashMap<String, List<String>>();
		String[] ees = ManifestElement.getArrayFromList(profileEE);
		for (String ee : ees) {
			String[] eeNameVersion = StateBuilder.getOSGiEENameVersion(ee);

			List<String> versions = eeVersions.get(eeNameVersion[0]);
			if (versions == null) {
				versions = new ArrayList<String>();
				eeVersions.put(eeNameVersion[0], versions);
			}
			if (eeNameVersion[1] != null && !versions.contains(eeNameVersion[1]))
				versions.add(eeNameVersion[1]);
		}
		for (Map.Entry<String, List<String>> eeVersion : eeVersions.entrySet()) {
			GenericDescriptionImpl capability = new GenericDescriptionImpl();
			capability.setType(OSGI_EE_NAMESPACE);
			Dictionary<String, Object> attributes = new Hashtable<String, Object>();
			attributes.put(capability.getType(), eeVersion.getKey());
			if (eeVersion.getValue().size() > 0) {
				List<Version> versions = new ArrayList<Version>(eeVersion.getValue().size());
				for (String version : eeVersion.getValue()) {
					versions.add(new Version(version));
				}
				attributes.put("version", versions); //$NON-NLS-1$
			}
			attributes.put(ExportPackageDescriptionImpl.EQUINOX_EE, profileIndex);
			capability.setAttributes(attributes);
			capabilities.add(capability);
		}
	}

	private void addSystemCapabilities(List<GenericDescription> capabilities, ManifestElement[] elements, Integer profileIndex) {
		try {
			StateBuilder.createOSGiCapabilities(elements, capabilities, profileIndex);
		} catch (BundleException e) {
			throw new RuntimeException("Unexpected exception adding system capabilities.", e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("rawtypes")
	public Dictionary[] getPlatformProperties() {
		return platformProperties;
	}

	private boolean checkProp(Object origObj, Object newObj) {
		if ((origObj == null && newObj != null) || (origObj != null && newObj == null))
			return true;
		if (origObj == null)
			return false;
		if (origObj.getClass() != newObj.getClass())
			return true;
		if (origObj instanceof String[]) {
			String[] origProps = (String[]) origObj;
			String[] newProps = (String[]) newObj;
			if (origProps.length != newProps.length)
				return true;
			for (int i = 0; i < origProps.length; i++) {
				if (!origProps[i].equals(newProps[i]))
					return true;
			}
			return false;
		}
		return !origObj.equals(newObj);
	}

	private boolean changedProps(Dictionary<Object, Object> origProps, Dictionary<Object, Object> newProps, String[] keys) {
		for (int i = 0; i < keys.length; i++) {
			Object origProp = origProps.get(keys[i]);
			Object newProp = newProps.get(keys[i]);
			if (checkProp(origProp, newProp))
				return true;
		}
		return false;
	}

	public String getSystemBundle() {
		String symbolicName = null;
		if (platformProperties != null && platformProperties.length > 0)
			symbolicName = (String) platformProperties[0].get(Constants.STATE_SYSTEM_BUNDLE);
		return symbolicName != null ? symbolicName : Constants.getInternalSymbolicName();
	}

	public BundleDescription[] getRemovalPending() {
		synchronized (this.monitor) {
			return removalPendings.toArray(new BundleDescription[removalPendings.size()]);
		}
	}

	private void addRemovalPending(BundleDescription removed) {
		synchronized (this.monitor) {
			if (!removalPendings.contains(removed))
				removalPendings.addFirst(removed);
		}
	}

	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles) {
		BundleDescription[] removals = getRemovalPending();
		Set<BundleDescription> result = new HashSet<BundleDescription>();
		for (BundleDescription bundle : bundles) {
			addDependents(bundle, result, removals);
		}
		return result;
	}

	private static void addDependents(BundleDescription bundle, Set<BundleDescription> result, BundleDescription[] removals) {
		if (result.contains(bundle))
			return; // avoid cycles
		result.add(bundle);
		BundleDescription[] dependents = bundle.getDependents();
		for (BundleDescription dependent : dependents)
			addDependents(dependent, result, removals);
		// check if this is a removal pending
		for (BundleDescription removed : removals) {
			if (removed.getBundleId() == bundle.getBundleId())
				addDependents(removed, result, removals);
		}
	}

	/**
	 * Returns the latest versions BundleDescriptions which have old removal pending versions.
	 * @return the BundleDescriptions that have removal pending versions.
	 */
	private BundleDescription[] internalGetRemovalPending() {
		synchronized (this.monitor) {
			Iterator<BundleDescription> removed = removalPendings.iterator();
			BundleDescription[] result = new BundleDescription[removalPendings.size()];
			int i = 0;
			while (removed.hasNext())
				// we return the latest version of the description if it is still contained in the state (bug 287636)
				result[i++] = getBundle(removed.next().getBundleId());
			return result;
		}
	}

	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		if (resolver == null)
			throw new IllegalStateException("no resolver set"); //$NON-NLS-1$
		BundleDescriptionImpl importer = (BundleDescriptionImpl) importingBundle;
		if (importer.getDynamicStamp(requestedPackage) == getTimeStamp())
			return null;
		fullyLoad();
		synchronized (this.monitor) {
			ResolverHook currentHook = null;
			try {
				resolving = true;
				ResolverHookFactory currentFactory = hookFactory;
				if (currentFactory != null) {
					Collection<BundleRevision> triggers = new ArrayList<BundleRevision>(1);
					triggers.add(importingBundle);
					triggers = Collections.unmodifiableCollection(triggers);
					currentHook = begin(triggers);
				}
				// ask the resolver to resolve our dynamic import
				ExportPackageDescriptionImpl result = (ExportPackageDescriptionImpl) resolver.resolveDynamicImport(importingBundle, requestedPackage);
				if (result == null)
					importer.setDynamicStamp(requestedPackage, new Long(getTimeStamp()));
				else {
					importer.setDynamicStamp(requestedPackage, null); // remove any cached timestamp
					// need to add the result to the list of resolved imports
					importer.addDynamicResolvedImport(result);
				}
				setDynamicCacheChanged(true);
				return result;
			} finally {
				resolving = false;
				if (currentHook != null)
					currentHook.end();
			}
		}

	}

	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports) {
		synchronized (this.monitor) {
			((BundleDescriptionImpl) importingBundle).addDynamicImportPackages(dynamicImports);
			setDynamicCacheChanged(true);
		}
	}

	void setReader(StateReader reader) {
		synchronized (this.monitor) {
			this.reader = reader;
		}
	}

	StateReader getReader() {
		synchronized (this.monitor) {
			return reader;
		}
	}

	// not synchronized on this to prevent deadlock
	public final void fullyLoad() {
		synchronized (this.monitor) {
			if (reader == null)
				return;
			if (fullyLoaded == true)
				return;
			if (reader.isLazyLoaded())
				reader.fullyLoad();
			fullyLoaded = true;
		}
	}

	// not synchronized on this to prevent deadlock
	public final boolean unloadLazyData(long checkStamp) {
		// make sure no other thread is trying to unload or load
		synchronized (this.monitor) {
			if (checkStamp != getTimeStamp() || dynamicCacheChanged())
				return false;
			if (reader.getAccessedFlag()) {
				reader.setAccessedFlag(false); // reset accessed flag
				return true;
			}
			fullyLoaded = false;
			BundleDescription[] bundles = getBundles();
			for (int i = 0; i < bundles.length; i++)
				((BundleDescriptionImpl) bundles[i]).unload();
			reader.flushLazyObjectCache();
			resolver.flush();
			return true;
		}
	}

	public ExportPackageDescription[] getSystemPackages() {
		synchronized (this.monitor) {
			List<ExportPackageDescription> result = new ArrayList<ExportPackageDescription>();
			BundleDescription[] systemBundles = getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
			if (systemBundles.length > 0) {
				BundleDescriptionImpl systemBundle = (BundleDescriptionImpl) systemBundles[0];
				ExportPackageDescription[] exports = systemBundle.getExportPackages();
				for (int i = 0; i < exports.length; i++)
					if (((Integer) exports[i].getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue() >= 0)
						result.add(exports[i]);
			}
			return result.toArray(new ExportPackageDescription[result.size()]);
		}
	}

	boolean inStrictMode() {
		synchronized (this.monitor) {
			return Constants.STRICT_MODE.equals(getPlatformProperties()[0].get(Constants.OSGI_RESOLVER_MODE));
		}
	}

	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		synchronized (this.monitor) {
			if (bundle.isResolved())
				return new ResolverError[0];
			List<ResolverError> result = resolverErrors.get(bundle);
			return result == null ? new ResolverError[0] : result.toArray(new ResolverError[result.size()]);
		}
	}

	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		synchronized (this.monitor) {
			if (!resolving)
				throw new IllegalStateException(); // TODO need error message here!
			List<ResolverError> errors = resolverErrors.get(bundle);
			if (errors == null) {
				errors = new ArrayList<ResolverError>(1);
				resolverErrors.put(bundle, errors);
			}
			errors.add(new ResolverErrorImpl((BundleDescriptionImpl) bundle, type, data, unsatisfied));
		}
	}

	public void removeResolverErrors(BundleDescription bundle) {
		synchronized (this.monitor) {
			if (!resolving)
				throw new IllegalStateException(); // TODO need error message here!
			resolverErrors.remove(bundle);
		}
	}

	public boolean dynamicCacheChanged() {
		synchronized (this.monitor) {
			return dynamicCacheChanged;
		}
	}

	void setDynamicCacheChanged(boolean dynamicCacheChanged) {
		synchronized (this.monitor) {
			this.dynamicCacheChanged = dynamicCacheChanged;
		}
	}

	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	void addPlatformPropertyKeys(String[] keys) {
		synchronized (platformPropertyKeys) {
			for (int i = 0; i < keys.length; i++)
				if (!platformPropertyKeys.contains(keys[i]))
					platformPropertyKeys.add(keys[i]);
		}
	}

	String[] getPlatformPropertyKeys() {
		synchronized (platformPropertyKeys) {
			return platformPropertyKeys.toArray(new String[platformPropertyKeys.size()]);
		}
	}

	public long getHighestBundleId() {
		synchronized (this.monitor) {
			return highestBundleId;
		}
	}

	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidNativePaths) {
		((NativeCodeDescriptionImpl) nativeCodeDescription).setInvalidNativePaths(hasInvalidNativePaths);
	}

	public BundleDescription[] getDisabledBundles() {
		synchronized (this.monitor) {
			return disabledBundles.keySet().toArray(new BundleDescription[0]);
		}
	}

	public void addDisabledInfo(DisabledInfo disabledInfo) {
		synchronized (this.monitor) {
			if (getBundle(disabledInfo.getBundle().getBundleId()) != disabledInfo.getBundle())
				throw new IllegalArgumentException(NLS.bind(StateMsg.BUNDLE_NOT_IN_STATE, disabledInfo.getBundle()));
			List<DisabledInfo> currentInfos = disabledBundles.get(disabledInfo.getBundle());
			if (currentInfos == null) {
				currentInfos = new ArrayList<DisabledInfo>(1);
				currentInfos.add(disabledInfo);
				disabledBundles.put(disabledInfo.getBundle(), currentInfos);
			} else {
				Iterator<DisabledInfo> it = currentInfos.iterator();
				while (it.hasNext()) {
					DisabledInfo currentInfo = it.next();
					if (disabledInfo.getPolicyName().equals(currentInfo.getPolicyName())) {
						currentInfos.remove(currentInfo);
						break;
					}
				}
				currentInfos.add(disabledInfo);
			}
			updateTimeStamp();
		}
	}

	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		synchronized (this.monitor) {
			List<DisabledInfo> currentInfos = disabledBundles.get(disabledInfo.getBundle());
			if ((currentInfos != null) && currentInfos.contains(disabledInfo)) {
				currentInfos.remove(disabledInfo);
				if (currentInfos.isEmpty()) {
					disabledBundles.remove(disabledInfo.getBundle());
				}
			}
			updateTimeStamp();
		}
	}

	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		synchronized (this.monitor) {
			List<DisabledInfo> currentInfos = disabledBundles.get(bundle);
			if (currentInfos == null)
				return null;
			Iterator<DisabledInfo> it = currentInfos.iterator();
			while (it.hasNext()) {
				DisabledInfo currentInfo = it.next();
				if (currentInfo.getPolicyName().equals(policyName)) {
					return currentInfo;
				}
			}
			return null;
		}
	}

	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		synchronized (this.monitor) {
			List<DisabledInfo> currentInfos = disabledBundles.get(bundle);
			return currentInfos == null ? EMPTY_DISABLEDINFOS : currentInfos.toArray(new DisabledInfo[currentInfos.size()]);
		}
	}

	/*
	 * Used by StateWriter to get all the DisabledInfo objects to persist
	 */
	DisabledInfo[] getDisabledInfos() {
		List<DisabledInfo> results = new ArrayList<DisabledInfo>();
		synchronized (this.monitor) {
			for (Iterator<List<DisabledInfo>> allDisabledInfos = disabledBundles.values().iterator(); allDisabledInfos.hasNext();)
				results.addAll(allDisabledInfos.next());
		}
		return results.toArray(new DisabledInfo[results.size()]);
	}
}
