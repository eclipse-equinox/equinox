/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.debug.DebugOptions;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.service.resolver.*;

public class StateImpl implements State {
	transient private Resolver resolver;
	transient private StateDeltaImpl changes;
	transient private Map listeners = new HashMap(11);
	transient private KeyedHashSet resolvedBundles = new KeyedHashSet();
	private boolean resolved = true;
	protected long timeStamp = System.currentTimeMillis();
	private KeyedHashSet bundleDescriptions = new KeyedHashSet(false);
	private StateObjectFactory factory;	

	public static boolean DEBUG_RESOLVER = false;
	private static long cumulativeTime;
	
	StateImpl() {}

	public boolean addBundle(BundleDescription description) {
		if (description.getBundleId() < 0)
			throw new IllegalArgumentException("no id set");		
		if (!basicAddBundle(description))
			return false;
		resolved = false;
		getDelta().recordBundleAdded((BundleDescriptionImpl) description);
		if (resolver != null)
			resolver.bundleAdded(description);
		return true;
	}
	public StateChangeEvent compare(State state) {
		throw new UnsupportedOperationException("not implemented"); //$NON-NLS-1$
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
		resolved = false;
		getDelta().recordBundleRemoved((BundleDescriptionImpl) toRemove);
		if (resolver != null)
			resolver.bundleRemoved(toRemove);
		return true;
	}
	public StateDelta getChanges() {
		return getDelta();
	}
	private StateDeltaImpl getDelta() {
		if (changes == null)
			changes = getNewDelta();
		return changes;
	}
	private StateDeltaImpl getNewDelta() {
		return new StateDeltaImpl(this);
	}
	public BundleDescription[] getBundles(final String requiredUniqueId) {
		final List bundles = new ArrayList();
		for (Iterator iter = bundleDescriptions.iterator(); iter.hasNext(); ) {
			BundleDescription bundle = (BundleDescription) iter.next();
			if (requiredUniqueId.equals(bundle.getUniqueId()))
				bundles.add(bundle);
		}
		return (BundleDescription[]) bundles.toArray(new BundleDescription[bundles.size()]);
	}	
	public BundleDescription[] getBundles() {
		return (BundleDescription[]) bundleDescriptions.elements(new BundleDescription[bundleDescriptions.size()]);
	}
	public BundleDescription getBundle(long id) {
		return (BundleDescription) bundleDescriptions.getByKey(new Long(id));
	}
	// TODO: this does not comply with the spec	
	public BundleDescription getBundle(String requiredUniqueId, Version requiredVersion) {
		BundleDescription[] bundles = getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (requiredUniqueId.equals(bundles[i].getUniqueId()) && bundles[i].getVersion().equals(requiredVersion))
				return bundles[i];
		}
		return null;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public boolean isResolved() {
		return resolved || isEmpty();
	}
	public void resolveConstraint(VersionConstraint constraint, Version actualVersion, BundleDescription supplier) {
		VersionConstraintImpl modifiable = ((VersionConstraintImpl) constraint);
		if (modifiable.getActualVersion() != actualVersion || modifiable.getSupplier() != supplier) {
			modifiable.setActualVersion(actualVersion);
			modifiable.setSupplier(supplier);			
			if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
				boolean optional = (constraint instanceof BundleSpecification) && ((BundleSpecification) constraint).isOptional();				
				getDelta().recordConstraintResolved((BundleDescriptionImpl) constraint.getBundle(), optional);
			}
		}
	}
	public void resolveBundle(BundleDescription bundle, int status) {
		((BundleDescriptionImpl) bundle).setState(status);
		getDelta().recordBundleResolved((BundleDescriptionImpl) bundle, status);
		if (status == Bundle.RESOLVED)
			resolvedBundles.add((KeyedElement) bundle);
		else {
			// ensures no links are left 
			unresolveConstraints(bundle);
			// remove the bundle from the resolved pool
			resolvedBundles.remove((KeyedElement) bundle);
		}
	}
	private void unresolveConstraints(BundleDescription bundle) {
		HostSpecification host = bundle.getHost();
		if (host != null)
			((VersionConstraintImpl) host).unresolve();
		PackageSpecification[] packages = bundle.getPackages();
		for (int i = 0; i < packages.length; i++)
			((VersionConstraintImpl) packages[i]).unresolve();
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++)
			((VersionConstraintImpl) requiredBundles[i]).unresolve();
	}

	public Resolver getResolver() {
		return resolver;
	}
	public void setResolver(Resolver newResolver) {
		if (resolver == newResolver)
			return;
		if (resolver != null)
			resolver.setState(null);
		resolver = newResolver;
		if (resolver == null)
			return;
		resolver.setState(this);
	}
	private StateDelta resolve(boolean incremental, BundleDescription[] reResolve) {
		if (resolver == null)
			throw new IllegalStateException("no resolver set"); //$NON-NLS-1$		
		long start = 0;
		if (DEBUG_RESOLVER)
			start = System.currentTimeMillis();
		if (!incremental)
			flush();
		if (resolved && reResolve == null)
			return new StateDeltaImpl(this);
		if (reResolve != null)
			resolver.resolve(reResolve);
		else
			resolver.resolve();
		resolved = true;
		// TODO: need to fire events for listeners
		StateDelta savedChanges = changes == null ? new StateDeltaImpl(this) : changes;
		changes = new StateDeltaImpl(this);

		if (DEBUG_RESOLVER) {
			cumulativeTime = cumulativeTime + (System.currentTimeMillis() - start);
			DebugOptions.getDefault().setOption("org.eclipse.core.runtime.adaptor/resolver/timing/value", Long.toString(cumulativeTime));
		}

		return savedChanges;
	}
	private void flush() {
		resolver.flush();
		resolved = false;
		if (resolvedBundles.isEmpty())
			return;
		for (Iterator iter = resolvedBundles.iterator(); iter.hasNext(); ) {
			BundleDescriptionImpl resolvedBundle = (BundleDescriptionImpl) iter.next();
			resolvedBundle.setState(0);
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
	public void addStateChangeListener(StateChangeListener listener, int flags) {
		if (listeners.containsKey(listener))
			return;
		listeners.put(listener, new Integer(flags));
	}
	public void removeStateChangeListener(StateChangeListener listener) {
		listeners.remove(listener);
	}
	public boolean isEmpty() {
		return bundleDescriptions.isEmpty();
	}
	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
	public boolean basicAddBundle(BundleDescription description) {
		((BundleDescriptionImpl) description).setContainingState(this);
		return bundleDescriptions.add((BundleDescriptionImpl) description);
	}
	void addResolvedBundle(BundleDescriptionImpl resolved) {
		resolvedBundles.add(resolved);
	}
	public BundleDescription[] getDependentBundles(BundleDescription[] roots) {
		Set remaining = new HashSet(Arrays.asList(resolvedBundles.elements()));
		KeyedHashSet reachable = new KeyedHashSet(roots.length);
		// put the roots in the graph
		for (int i = 0; i < roots.length; i++)
			if (roots[i].isResolved()) {
				reachable.add((KeyedElement) roots[i]);
				remaining.remove(roots[i]);
			}
		boolean changed;
		do {
			changed = false;
			// start over each iteration
			for (Iterator remainingIter = remaining.iterator(); remainingIter.hasNext(); ) {
				BundleDescription candidate = (BundleDescription) remainingIter.next();
				if (isDependent(candidate, reachable)) {
					reachable.add((KeyedElement) candidate);
					remainingIter.remove();
					changed = true;
				}
			}
		} while (changed);
		return (BundleDescription[]) reachable.elements(new BundleDescription[reachable.size()]);
	}
	private boolean isDependent(BundleDescription candidate, KeyedHashSet elements) {
		// is a fragment of any of them?
		HostSpecification candidateHost = candidate.getHost();
		if (candidateHost != null && candidateHost.isResolved() && elements.contains((KeyedElement) candidateHost.getSupplier()))
			return true;
		// does require any of them?		
		BundleSpecification[] candidateRequired = candidate.getRequiredBundles();
		for (int i = 0; i < candidateRequired.length; i++)
			if (candidateRequired[i].isResolved() && elements.contains((KeyedElement) candidateRequired[i].getSupplier()))
				return true;
		// does import any of their packages?			
		PackageSpecification[] candidatePackages = candidate.getPackages();
		for (int i = 0; i < candidatePackages.length; i++)
			if (candidatePackages[i].isResolved() && candidatePackages[i].getSupplier() != candidate && elements.contains((KeyedElement) candidatePackages[i].getSupplier()))
				return true;
		return false;
	}
	public PackageSpecification getExportedPackage(String packageName, Version version) {
		boolean ignoreVersion = version == null;
		for (Iterator iter = resolvedBundles.iterator(); iter.hasNext(); ) {
			PackageSpecification[] packages = ((BundleDescriptionImpl) iter.next()).getPackages();
			for (int i = 0; i < packages.length; i++)
				if (packages[i].getName().equals(packageName) && (ignoreVersion || packages[i].getVersionSpecification().equals(version)) && (packages[i].getSupplier() != null))
					return packages[i].getSupplier().getPackage(packageName);
		}
		return null;
	}
	public PackageSpecification[] getExportedPackages(BundleDescription bundle) {
		if (!bundle.isResolved())
			return new PackageSpecification[0];
		PackageSpecification[] allPackages = bundle.getPackages();
		PackageSpecification[] exported = new PackageSpecification[allPackages.length];
		int exportedCount = 0;
		for (int i = 0; i < allPackages.length; i++)
			if (allPackages[i].isExported() && allPackages[i].getSupplier() == bundle)
				exported[exportedCount++] = allPackages[i];
		if (exportedCount < exported.length) {
			PackageSpecification[] tmpExported = new PackageSpecification[exportedCount];
			System.arraycopy(exported, 0, tmpExported, 0, exportedCount);
			exported = tmpExported;
		}
		return exported;
	}
	public PackageSpecification[] getExportedPackages() {
		final List allExportedPackages = new ArrayList();
		for (Iterator iter = resolvedBundles.iterator(); iter.hasNext(); ) {
			BundleDescription bundle = (BundleDescription) iter.next();
			PackageSpecification[] bundlePackages = bundle.getPackages();
			for (int i = 0; i < bundlePackages.length; i++)
				if (bundlePackages[i].isExported() && bundlePackages[i].getSupplier() == bundle)
					allExportedPackages.add(bundlePackages[i]);
		}
		return (PackageSpecification[]) allExportedPackages.toArray(new PackageSpecification[allExportedPackages.size()]);
	}
	public BundleDescription[] getImportingBundles(final PackageSpecification exportedPackage) {
		if (!exportedPackage.isResolved())
			return null;
		final List allImportingBundles = new ArrayList();
		for (Iterator iter = resolvedBundles.iterator(); iter.hasNext(); ) {
			BundleDescription bundle = (BundleDescription) iter.next();
			PackageSpecification[] bundlePackages = bundle.getPackages();
			for (int i = 0; i < bundlePackages.length; i++)
				if (bundlePackages[i].getName().equals(exportedPackage.getName()) && bundlePackages[i].getSupplier() == exportedPackage.getBundle()) {
					allImportingBundles.add(bundle);
					break;
				}
		}
		return (BundleDescription[]) allImportingBundles.toArray(new BundleDescription[allImportingBundles.size()]);
	}
	public BundleDescription[] getFragments(final BundleDescription host) {
		final List fragments = new ArrayList();
		for (Iterator iter = bundleDescriptions.iterator(); iter.hasNext(); ) {
			BundleDescription bundle = (BundleDescription) iter.next();
			HostSpecification hostSpec = bundle.getHost();
			if (hostSpec != null && hostSpec.getSupplier() == host)
				fragments.add(bundle);
		}
		return (BundleDescription[]) fragments.toArray(new BundleDescription[fragments.size()]);
	}
	public void setTimeStamp(long newTimeStamp) {
		timeStamp = newTimeStamp;		
	}
	public StateObjectFactory getFactory() {
		return factory;
	}
	void setFactory(StateObjectFactory factory) {
		this.factory = factory;
	}
}