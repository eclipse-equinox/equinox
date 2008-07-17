/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.framework.util.*;
import org.eclipse.osgi.internal.baseadaptor.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public abstract class StateImpl implements State {
	private static final String OSGI_OS = "osgi.os"; //$NON-NLS-1$
	private static final String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	private static final String OSGI_NL = "osgi.nl"; //$NON-NLS-1$
	private static final String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String[] PROPS = {OSGI_OS, OSGI_WS, OSGI_NL, OSGI_ARCH, Constants.OSGI_FRAMEWORK_SYSTEM_PACKAGES, Constants.OSGI_RESOLVER_MODE, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "osgi.resolveOptional", "osgi.genericAliases", Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION, Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_LANGUAGE, Constants.STATE_SYSTEM_BUNDLE}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final DisabledInfo[] EMPTY_DISABLEDINFOS = new DisabledInfo[0];
	transient private Resolver resolver;
	transient private StateDeltaImpl changes;
	transient private boolean resolving = false;
	transient private HashSet removalPendings = new HashSet();
	private boolean resolved = true;
	private long timeStamp = System.currentTimeMillis();
	private KeyedHashSet bundleDescriptions = new KeyedHashSet(false);
	private HashMap resolverErrors = new HashMap();
	private StateObjectFactory factory;
	private KeyedHashSet resolvedBundles = new KeyedHashSet();
	private HashMap disabledBundles = new HashMap();
	boolean fullyLoaded = false;
	private boolean dynamicCacheChanged = false;
	// only used for lazy loading of BundleDescriptions
	private StateReader reader;
	private Dictionary[] platformProperties = {new Hashtable(PROPS.length)}; // Dictionary here because of Filter API
	private long highestBundleId = -1;
	private HashSet platformPropertyKeys = new HashSet(PROPS.length);

	private static long cumulativeTime;

	// to prevent extra-package instantiation 
	protected StateImpl() {
		// always add the default platform property keys.
		addPlatformPropertyKeys(PROPS);
	}

	public boolean addBundle(BundleDescription description) {
		if (!basicAddBundle(description))
			return false;
		String platformFilter = description.getPlatformFilter();
		if (platformFilter != null) {
			try {
				// add any new platform filter propery keys this bundle is using
				FilterImpl filter = (FilterImpl) FrameworkUtil.createFilter(platformFilter);
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
			resetSystemExports();
		if (resolver != null)
			resolver.bundleAdded(description);
		updateTimeStamp();
		return true;
	}

	public boolean updateBundle(BundleDescription newDescription) {
		BundleDescriptionImpl existing = (BundleDescriptionImpl) bundleDescriptions.get((BundleDescriptionImpl) newDescription);
		if (existing == null)
			return false;
		if (!bundleDescriptions.remove(existing))
			return false;
		resolvedBundles.remove(existing);
		synchronized (disabledBundles) {
			ArrayList infos = (ArrayList) disabledBundles.remove(existing);
			if (infos != null) {
				ArrayList newInfos = new ArrayList(infos.size());
				for (Iterator iInfos = infos.iterator(); iInfos.hasNext();) {
					DisabledInfo info = (DisabledInfo) iInfos.next();
					newInfos.add(new DisabledInfo(info.getPolicyName(), info.getMessage(), newDescription));
				}
				disabledBundles.put(newDescription, newInfos);
			}
		}
		existing.setStateBit(BundleDescriptionImpl.REMOVAL_PENDING, true);
		if (!basicAddBundle(newDescription))
			return false;
		resolved = false;
		getDelta().recordBundleUpdated((BundleDescriptionImpl) newDescription);
		if (getSystemBundle().equals(newDescription.getSymbolicName()))
			resetSystemExports();
		if (resolver != null) {
			boolean pending = existing.getDependents().length > 0;
			resolver.bundleUpdated(newDescription, existing, pending);
			if (pending) {
				getDelta().recordBundleRemovalPending(existing);
				removalPendings.add(existing);
			} else {
				// an existing bundle has been updated with no dependents it can safely be unresolved now
				synchronized (this) {
					try {
						resolving = true;
						resolverErrors.remove(existing);
						resolveBundle(existing, false, null, null, null, null, null);
					} finally {
						resolving = false;
					}
				}
			}
		}
		updateTimeStamp();
		return true;
	}

	public BundleDescription removeBundle(long bundleId) {
		BundleDescription toRemove = getBundle(bundleId);
		if (toRemove == null || !removeBundle(toRemove))
			return null;
		return toRemove;
	}

	public boolean removeBundle(BundleDescription toRemove) {
		if (!bundleDescriptions.remove((KeyedElement) toRemove))
			return false;
		resolvedBundles.remove((KeyedElement) toRemove);
		synchronized (disabledBundles) {
			disabledBundles.remove(toRemove);
		}
		resolved = false;
		getDelta().recordBundleRemoved((BundleDescriptionImpl) toRemove);
		((BundleDescriptionImpl) toRemove).setStateBit(BundleDescriptionImpl.REMOVAL_PENDING, true);
		if (resolver != null) {
			boolean pending = toRemove.getDependents().length > 0;
			resolver.bundleRemoved(toRemove, pending);
			if (pending) {
				getDelta().recordBundleRemovalPending((BundleDescriptionImpl) toRemove);
				removalPendings.add(toRemove);
			} else {
				// a bundle has been removed with no dependents it can safely be unresolved now
				synchronized (this) {
					try {
						resolving = true;
						resolverErrors.remove(toRemove);
						resolveBundle(toRemove, false, null, null, null, null, null);
					} finally {
						resolving = false;
					}
				}
			}
		}
		updateTimeStamp();
		return true;
	}

	public StateDelta getChanges() {
		return getDelta();
	}

	private StateDeltaImpl getDelta() {
		if (changes == null)
			changes = new StateDeltaImpl(this);
		return changes;
	}

	public BundleDescription[] getBundles(String symbolicName) {
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName))
			symbolicName = getSystemBundle();
		final List bundles = new ArrayList();
		for (Iterator iter = bundleDescriptions.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			if (symbolicName.equals(bundle.getSymbolicName()))
				bundles.add(bundle);
		}
		return (BundleDescription[]) bundles.toArray(new BundleDescription[bundles.size()]);
	}

	public BundleDescription[] getBundles() {
		return (BundleDescription[]) bundleDescriptions.elements(new BundleDescription[bundleDescriptions.size()]);
	}

	public BundleDescription getBundle(long id) {
		BundleDescription result = (BundleDescription) bundleDescriptions.getByKey(new Long(id));
		if (result != null)
			return result;
		// need to look in removal pending bundles;
		for (Iterator iter = removalPendings.iterator(); iter.hasNext();) {
			BundleDescription removedBundle = (BundleDescription) iter.next();
			if (removedBundle.getBundleId() == id) // just return the first matching id
				return removedBundle;
		}
		return null;
	}

	public BundleDescription getBundle(String name, Version version) {
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

	public long getTimeStamp() {
		return timeStamp;
	}

	public boolean isResolved() {
		return resolved || isEmpty();
	}

	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier) {
		((VersionConstraintImpl) constraint).setSupplier(supplier);
	}

	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		resolveBundle(bundle, status, hosts, selectedExports, null, resolvedRequires, resolvedImports);
	}

	public synchronized void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
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
		// to support develoment mode we will resolveConstraints even if the resolve status == false
		// we only do this if the resolved constraints are not null
		if (selectedExports == null || resolvedRequires == null || resolvedImports == null)
			unresolveConstraints(modifiable);
		else
			resolveConstraints(modifiable, hosts, selectedExports, substitutedExports, resolvedRequires, resolvedImports);
	}

	public synchronized void removeBundleComplete(BundleDescription bundle) {
		if (!resolving)
			throw new IllegalStateException(); // TODO need error message here!
		getDelta().recordBundleRemovalComplete((BundleDescriptionImpl) bundle);
		removalPendings.remove(bundle);
	}

	private void resolveConstraints(BundleDescriptionImpl bundle, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
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

		bundle.addDependencies(hosts, true);
		bundle.addDependencies(resolvedRequires, true);
		bundle.addDependencies(resolvedImports, true);
		// add dependecies for generics
		GenericSpecification[] genericRequires = bundle.getGenericRequires();
		if (genericRequires.length > 0) {
			ArrayList genericSuppliers = new ArrayList(genericRequires.length);
			for (int i = 0; i < genericRequires.length; i++) {
				GenericDescription[] suppliers = genericRequires[i].getSuppliers();
				if (suppliers != null)
					for (int j = 0; j < suppliers.length; j++)
						genericSuppliers.add(suppliers[j]);
			}
			bundle.addDependencies((BaseDescription[]) genericSuppliers.toArray(new BaseDescription[genericSuppliers.size()]), true);
		}
	}

	private void checkHostForSubstitutedExports(BundleDescriptionImpl host, BundleDescriptionImpl fragment) {
		// TODO need to handle this case where a fragment has its own export substituted
		// there are issues here because the order in which fragments are resolved is not always the same ...
	}

	//	private void checkForSubstitutedExports(BundleDescriptionImpl bundle, ExportPackageDescription[] selectedExports) {
	//		ExportPackageDescription[] existingSubstitutes = bundle.getSubstitutedExports();
	//		ExportPackageDescription[] declaredExports = bundle.getExportPackages();
	//		ArrayList substitutes = new ArrayList();
	//		for (int i = 0; i < declaredExports.length; i++) {
	//			boolean selected = false;
	//			for (int j = 0; !selected && j < selectedExports.length; j++)
	//				selected = declaredExports[i] == selectedExports[j];
	//			if (!selected)
	//				substitutes.add(declaredExports[i]);
	//		}
	//		if (substitutes.size() > 0) {
	//			substitutes.ensureCapacity(substitutes.size() + existingSubstitutes.length);
	//			for (int i = 0; i < existingSubstitutes.length; i++)
	//				substitutes.add(0, existingSubstitutes[i]);
	//			bundle.setSubstitutedExports((ExportPackageDescription[]) substitutes.toArray(new ExportPackageDescription[substitutes.size()]));
	//		}
	//	}

	private void unresolveConstraints(BundleDescriptionImpl bundle) {
		HostSpecificationImpl host = (HostSpecificationImpl) bundle.getHost();
		if (host != null)
			host.setHosts(null);

		bundle.setSelectedExports(null);
		bundle.setResolvedImports(null);
		bundle.setResolvedRequires(null);
		bundle.setSubstitutedExports(null);

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

	private synchronized StateDelta resolve(boolean incremental, BundleDescription[] reResolve) {
		try {
			resolving = true;
			if (resolver == null)
				throw new IllegalStateException("no resolver set"); //$NON-NLS-1$
			fullyLoad();
			long start = 0;
			if (StateManager.DEBUG_PLATFORM_ADMIN_RESOLVER)
				start = System.currentTimeMillis();
			if (!incremental) {
				resolved = false;
				reResolve = getBundles();
				// need to get any removal pendings before flushing
				if (removalPendings.size() > 0) {
					BundleDescription[] removed = getRemovalPendings();
					reResolve = mergeBundles(reResolve, removed);
				}
				flush(reResolve);
			}
			if (resolved && reResolve == null)
				return new StateDeltaImpl(this);
			if (removalPendings.size() > 0) {
				BundleDescription[] removed = getRemovalPendings();
				reResolve = mergeBundles(reResolve, removed);
			}
			// use the Headers class to handle ignoring case while matching keys (bug 180817)
			Headers[] tmpPlatformProperties = new Headers[platformProperties.length];
			for (int i = 0; i < platformProperties.length; i++) {
				tmpPlatformProperties[i] = new Headers(platformProperties[i].size());
				for (Enumeration keys = platformProperties[i].keys(); keys.hasMoreElements();) {
					Object key = keys.nextElement();
					tmpPlatformProperties[i].put(key, platformProperties[i].get(key));
				}
			}
			resolver.resolve(reResolve, tmpPlatformProperties);
			resolved = removalPendings.size() == 0;

			StateDelta savedChanges = changes == null ? new StateDeltaImpl(this) : changes;
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

	private BundleDescription[] mergeBundles(BundleDescription[] reResolve, BundleDescription[] removed) {
		if (reResolve == null)
			return removed; // just return all the removed bundles
		if (reResolve.length == 0)
			return reResolve; // if reResolve length==0 then we want to prevent pending removal
		// merge in all removal pending bundles that are not already in the list
		ArrayList result = new ArrayList(reResolve.length + removed.length);
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
		return (BundleDescription[]) result.toArray(new BundleDescription[result.size()]);
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
		return resolve(true, null);
	}

	public StateDelta resolve(boolean incremental) {
		return resolve(incremental, null);
	}

	public StateDelta resolve(BundleDescription[] reResolve) {
		return resolve(true, reResolve);
	}

	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getResolvedBundles() {
		return (BundleDescription[]) resolvedBundles.elements(new BundleDescription[resolvedBundles.size()]);
	}

	public boolean isEmpty() {
		return bundleDescriptions.isEmpty();
	}

	void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	boolean basicAddBundle(BundleDescription description) {
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

	void addResolvedBundle(BundleDescriptionImpl resolvedBundle) {
		resolvedBundles.add(resolvedBundle);
	}

	public ExportPackageDescription[] getExportedPackages() {
		fullyLoad();
		final List allExportedPackages = new ArrayList();
		for (Iterator iter = resolvedBundles.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			ExportPackageDescription[] bundlePackages = bundle.getSelectedExports();
			if (bundlePackages == null)
				continue;
			for (int i = 0; i < bundlePackages.length; i++)
				allExportedPackages.add(bundlePackages[i]);
		}
		for (Iterator iter = removalPendings.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			ExportPackageDescription[] bundlePackages = bundle.getSelectedExports();
			if (bundlePackages == null)
				continue;
			for (int i = 0; i < bundlePackages.length; i++)
				allExportedPackages.add(bundlePackages[i]);
		}
		return (ExportPackageDescription[]) allExportedPackages.toArray(new ExportPackageDescription[allExportedPackages.size()]);
	}

	BundleDescription[] getFragments(final BundleDescription host) {
		final List fragments = new ArrayList();
		for (Iterator iter = bundleDescriptions.iterator(); iter.hasNext();) {
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
		return (BundleDescription[]) fragments.toArray(new BundleDescription[fragments.size()]);
	}

	public void setTimeStamp(long newTimeStamp) {
		timeStamp = newTimeStamp;
	}

	private void updateTimeStamp() {
		if (getTimeStamp() == Long.MAX_VALUE)
			setTimeStamp(0);
		setTimeStamp(getTimeStamp() + 1);
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	void setFactory(StateObjectFactory factory) {
		this.factory = factory;
	}

	public BundleDescription getBundleByLocation(String location) {
		for (Iterator i = bundleDescriptions.iterator(); i.hasNext();) {
			BundleDescription current = (BundleDescription) i.next();
			if (location.equals(current.getLocation()))
				return current;
		}
		return null;
	}

	public Resolver getResolver() {
		return resolver;
	}

	public void setResolver(Resolver newResolver) {
		if (resolver == newResolver)
			return;
		if (resolver != null) {
			Resolver oldResolver = resolver;
			resolver = null;
			oldResolver.setState(null);
		}
		resolver = newResolver;
		if (resolver == null)
			return;
		resolver.setState(this);
	}

	public boolean setPlatformProperties(Dictionary platformProperties) {
		return setPlatformProperties(new Dictionary[] {platformProperties});
	}

	public boolean setPlatformProperties(Dictionary[] platformProperties) {
		return setPlatformProperties(platformProperties, true);
	}

	synchronized boolean setPlatformProperties(Dictionary[] platformProperties, boolean resetSystemExports) {
		if (platformProperties.length == 0)
			throw new IllegalArgumentException();
		// copy the properties for our use internally;
		// only copy String and String[] values
		Dictionary[] newPlatformProperties = new Dictionary[platformProperties.length];
		for (int i = 0; i < platformProperties.length; i++) {
			newPlatformProperties[i] = new Hashtable(platformProperties[i].size());
			synchronized (platformProperties[i]) {
				for (Enumeration keys = platformProperties[i].keys(); keys.hasMoreElements();) {
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
		if (this.platformProperties.length != newPlatformProperties.length) {
			result = true;
			performResetSystemExports = true;
		} else {
			// we need to see if any of the existing filter prop keys have changed
			String[] keys = getPlatformPropertyKeys();
			for (int i = 0; i < newPlatformProperties.length && !result; i++) {
				result |= changedProps(this.platformProperties[i], newPlatformProperties[i], keys);
				if (resetSystemExports) {
					performResetSystemExports |= checkProp(this.platformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES), newPlatformProperties[i].get(Constants.FRAMEWORK_SYSTEMPACKAGES));
					performResetSystemExports |= checkProp(this.platformProperties[i].get(Constants.SYSTEM_BUNDLE_SYMBOLICNAME), newPlatformProperties[i].get(Constants.SYSTEM_BUNDLE_SYMBOLICNAME));
				}
			}
		}
		// always do a complete replacement of the properties in case new bundles are added that uses new filter props
		this.platformProperties = newPlatformProperties;
		if (performResetSystemExports)
			resetSystemExports();
		return result;
	}

	private void resetSystemExports() {
		BundleDescription[] systemBundles = getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		if (systemBundles.length > 0) {
			BundleDescriptionImpl systemBundle = (BundleDescriptionImpl) systemBundles[0];
			ExportPackageDescription[] exports = systemBundle.getExportPackages();
			ArrayList newExports = new ArrayList(exports.length);
			for (int i = 0; i < exports.length; i++)
				if (((Integer) exports[i].getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue() < 0)
					newExports.add(exports[i]);
			addSystemExports(newExports);
			systemBundle.setExportPackages((ExportPackageDescription[]) newExports.toArray(new ExportPackageDescription[newExports.size()]));
		}
	}

	private void addSystemExports(ArrayList exports) {
		for (int i = 0; i < platformProperties.length; i++)
			try {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) platformProperties[i].get(Constants.OSGI_FRAMEWORK_SYSTEM_PACKAGES));
				if (elements == null)
					continue;
				// we can pass false for strict mode here because we never want to mark the system exports as internal.
				ExportPackageDescription[] systemExports = StateBuilder.createExportPackages(elements, null, null, 2, false);
				Integer profInx = new Integer(i);
				for (int j = 0; j < systemExports.length; j++) {
					((ExportPackageDescriptionImpl) systemExports[j]).setDirective(ExportPackageDescriptionImpl.EQUINOX_EE, profInx);
					exports.add(systemExports[j]);
				}
			} catch (BundleException e) {
				// TODO consider throwing this... 
			}
	}

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

	private boolean changedProps(Dictionary origProps, Dictionary newProps, String[] keys) {
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
			symbolicName = (String) platformProperties[0].get(Constants.STATE_SYSTEM_BUNDLE); //$NON-NLS-1$
		return symbolicName != null ? symbolicName : Constants.getInternalSymbolicName();
	}

	public BundleDescription[] getRemovalPendings() {
		return (BundleDescription[]) removalPendings.toArray(new BundleDescription[removalPendings.size()]);
	}

	public synchronized ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		if (resolver == null)
			throw new IllegalStateException("no resolver set"); //$NON-NLS-1$
		BundleDescriptionImpl importer = (BundleDescriptionImpl) importingBundle;
		if (importer.getDynamicStamp(requestedPackage) == getTimeStamp())
			return null;
		try {
			resolving = true;
			fullyLoad();
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
		}
	}

	void setReader(StateReader reader) {
		this.reader = reader;
	}

	StateReader getReader() {
		return reader;
	}

	public void fullyLoad() {
		if (reader == null)
			return;
		synchronized (reader) {
			if (fullyLoaded == true)
				return;
			if (reader.isLazyLoaded())
				reader.fullyLoad();
			fullyLoaded = true;
		}
	}

	public void unloadLazyData(long expireTime) {
		// make sure no other thread is trying to unload or load
		synchronized (reader) {
			if (reader.getAccessedFlag()) {
				reader.setAccessedFlag(false); // reset accessed flag
				return;
			}
			fullyLoaded = false;
			BundleDescription[] bundles = getBundles();
			for (int i = 0; i < bundles.length; i++)
				((BundleDescriptionImpl) bundles[i]).unload();
		}
	}

	public ExportPackageDescription[] getSystemPackages() {
		ArrayList result = new ArrayList();
		BundleDescription[] systemBundles = getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		if (systemBundles.length > 0) {
			BundleDescriptionImpl systemBundle = (BundleDescriptionImpl) systemBundles[0];
			ExportPackageDescription[] exports = systemBundle.getExportPackages();
			for (int i = 0; i < exports.length; i++)
				if (((Integer) exports[i].getDirective(ExportPackageDescriptionImpl.EQUINOX_EE)).intValue() >= 0)
					result.add(exports[i]);
		}
		return (ExportPackageDescription[]) result.toArray(new ExportPackageDescription[result.size()]);
	}

	boolean inStrictMode() {
		return Constants.STRICT_MODE.equals(getPlatformProperties()[0].get(Constants.OSGI_RESOLVER_MODE));
	}

	public synchronized ResolverError[] getResolverErrors(BundleDescription bundle) {
		if (bundle.isResolved())
			return new ResolverError[0];
		ArrayList result = (ArrayList) resolverErrors.get(bundle);
		return result == null ? new ResolverError[0] : (ResolverError[]) result.toArray(new ResolverError[result.size()]);
	}

	public synchronized void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		if (!resolving)
			throw new IllegalStateException(); // TODO need error message here!
		ArrayList errors = (ArrayList) resolverErrors.get(bundle);
		if (errors == null) {
			errors = new ArrayList(1);
			resolverErrors.put(bundle, errors);
		}
		errors.add(new ResolverErrorImpl((BundleDescriptionImpl) bundle, type, data, unsatisfied));
	}

	public synchronized void removeResolverErrors(BundleDescription bundle) {
		if (!resolving)
			throw new IllegalStateException(); // TODO need error message here!
		resolverErrors.remove(bundle);
	}

	public boolean dynamicCacheChanged() {
		return dynamicCacheChanged;
	}

	void setDynamicCacheChanged(boolean dynamicCacheChanged) {
		this.dynamicCacheChanged = dynamicCacheChanged;
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
			return (String[]) platformPropertyKeys.toArray(new String[platformPropertyKeys.size()]);
		}
	}

	public long getHighestBundleId() {
		return highestBundleId;
	}

	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidNativePaths) {
		((NativeCodeDescriptionImpl) nativeCodeDescription).setInvalidNativePaths(hasInvalidNativePaths);
	}

	public BundleDescription[] getDisabledBundles() {
		synchronized (disabledBundles) {
			return (BundleDescription[]) disabledBundles.keySet().toArray(new BundleDescription[0]);
		}
	}

	public void addDisabledInfo(DisabledInfo disabledInfo) {
		if (getBundle(disabledInfo.getBundle().getBundleId()) != disabledInfo.getBundle())
			throw new IllegalArgumentException(NLS.bind(StateMsg.BUNDLE_NOT_IN_STATE, disabledInfo.getBundle()));
		synchronized (disabledBundles) {
			ArrayList currentInfos = (ArrayList) disabledBundles.get(disabledInfo.getBundle());
			if (currentInfos == null) {
				currentInfos = new ArrayList(1);
				currentInfos.add(disabledInfo);
				disabledBundles.put(disabledInfo.getBundle(), currentInfos);
			} else {
				Iterator it = currentInfos.iterator();
				while (it.hasNext()) {
					DisabledInfo currentInfo = (DisabledInfo) it.next();
					if (disabledInfo.getPolicyName().equals(currentInfo.getPolicyName())) {
						currentInfos.remove(currentInfo);
						break;
					}
				}
				currentInfos.add(disabledInfo);
			}
		}
		updateTimeStamp();
	}

	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		synchronized (disabledBundles) {
			ArrayList currentInfos = (ArrayList) disabledBundles.get(disabledInfo.getBundle());
			if ((currentInfos != null) && currentInfos.contains(disabledInfo)) {
				currentInfos.remove(disabledInfo);
				if (currentInfos.isEmpty()) {
					disabledBundles.remove(disabledInfo.getBundle());
				}
			}
		}
		updateTimeStamp();
	}

	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		synchronized (disabledBundles) {
			ArrayList currentInfos = (ArrayList) disabledBundles.get(bundle);
			if (currentInfos == null)
				return null;
			Iterator it = currentInfos.iterator();
			while (it.hasNext()) {
				DisabledInfo currentInfo = (DisabledInfo) it.next();
				if (currentInfo.getPolicyName().equals(policyName)) {
					return currentInfo;
				}
			}
			return null;
		}
	}

	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		synchronized (disabledBundles) {
			ArrayList currentInfos = (ArrayList) disabledBundles.get(bundle);
			return currentInfos == null ? EMPTY_DISABLEDINFOS : (DisabledInfo[]) currentInfos.toArray(new DisabledInfo[currentInfos.size()]);
		}
	}

	/*
	 * Used by StateWriter to get all the DisabledInfo objects to persist
	 */
	DisabledInfo[] getDisabledInfos() {
		ArrayList results = new ArrayList();
		synchronized (disabledBundles) {
			for (Iterator allDisabledInfos = disabledBundles.values().iterator(); allDisabledInfos.hasNext();)
				results.addAll((Collection) allDisabledInfos.next());
		}
		return (DisabledInfo[]) results.toArray(new DisabledInfo[results.size()]);
	}
}
