/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.*;

/**
 * An implementation for the StateHelper API. Access to this implementation is
 * provided  by the PlatformAdmin. Since this helper is a general facility for
 * state manipulation, it should not be tied to any implementation details.
 */
public class StateHelperImpl implements StateHelper {
	private static StateHelper instance = new StateHelperImpl();

	/**
	 * @see StateHelper
	 */
	public BundleDescription[] getDependentBundles(BundleDescription[] roots) {
		if (roots == null || roots.length == 0)
			return new BundleDescription[0];
		Set remaining = new HashSet(Arrays.asList(roots[0].getContainingState().getResolvedBundles()));
		Set reachable = new HashSet(roots.length);
		// put the roots in the graph
		for (int i = 0; i < roots.length; i++)
			if (roots[i].isResolved()) {
				reachable.add(roots[i]);
				remaining.remove(roots[i]);
			}
		boolean changed;
		do {
			changed = false;
			// start over each iteration
			for (Iterator remainingIter = remaining.iterator(); remainingIter.hasNext();) {
				BundleDescription candidate = (BundleDescription) remainingIter.next();
				if (isDependent(candidate, reachable)) {
					reachable.add(candidate);
					remainingIter.remove();
					changed = true;
				}
			}
		} while (changed);
		return (BundleDescription[]) reachable.toArray(new BundleDescription[reachable.size()]);
	}

	/*
	 * Returns whether a bundle has any dependency on any of the given bundles.   
	 */
	private boolean isDependent(BundleDescription candidate, Set bundles) {
		// is a fragment of any of them?
		HostSpecification candidateHost = candidate.getHost();
		if (candidateHost != null && candidateHost.isResolved() && bundles.contains(candidateHost.getSupplier()))
			return true;
		// does require any of them?		
		BundleSpecification[] candidateRequired = candidate.getRequiredBundles();
		for (int i = 0; i < candidateRequired.length; i++)
			if (candidateRequired[i].isResolved() && bundles.contains(candidateRequired[i].getSupplier()))
				return true;
		// does import any of their packages?			
		PackageSpecification[] candidatePackages = candidate.getPackages();
		for (int i = 0; i < candidatePackages.length; i++)
			if (candidatePackages[i].isResolved() && candidatePackages[i].getSupplier() != candidate && bundles.contains(candidatePackages[i].getSupplier()))
				return true;
		return false;
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
		HostSpecification[] hosts = bundle.getHosts();
		for (int i = 0; i < hosts.length; i++)
			if (!hosts[i].isResolved() && !isResolvable(hosts[i]))
				unsatisfied.add(hosts[i]);
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++)
			if (!requiredBundles[i].isResolved() && !isResolvable(requiredBundles[i]))
				unsatisfied.add(requiredBundles[i]);
		PackageSpecification[] packages = bundle.getPackages();
		for (int i = 0; i < packages.length; i++)
			if (!packages[i].isResolved() && !isResolvable(packages[i]))
				unsatisfied.add(packages[i]);
		return (VersionConstraint[]) unsatisfied.toArray(new VersionConstraint[unsatisfied.size()]);
	}

	/**
	 * @see StateHelper
	 */
	public boolean isResolvable(PackageSpecification specification) {
		if (specification.isExported())
			return true;
		PackageSpecification exported = getExportedPackage(specification.getBundle().getContainingState(), specification.getName(), null);
		if (exported == null)
			return false;
		return specification.isSatisfiedBy(exported.getVersionRange().getMinimum());
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
			if (availableBundles[i].isResolved() && constraint.isSatisfiedBy(availableBundles[i].getVersion()))
				return true;
		return false;
	}

	/**
	 * @see StateHelper
	 */
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

	/**
	 * @see StateHelper
	 */
	public PackageSpecification getExportedPackage(State state, String packageName, Version version) {
		// TODO why not use State.getExportedPackages here?
		BundleDescription[] resolvedBundles = state.getResolvedBundles();
		boolean ignoreVersion = version == null;
		for (int i = 0; i < resolvedBundles.length; i++) {
			PackageSpecification[] packages = resolvedBundles[i].getPackages();
			for (int j = 0; j < packages.length; j++)
				if (packages[j].getName().equals(packageName) && (ignoreVersion || packages[j].getVersionRange().getMinimum().equals(version)) && (packages[j].getSupplier() != null))
					return packages[j].getSupplier().getPackage(packageName);
		}
		return null;
	}

	public Object[][] sortBundles(BundleDescription[] toSort) {
		List references = new ArrayList(toSort.length);
		for (int i = 0; i < toSort.length; i++)
			if (toSort[i].isResolved())
				buildReferences(toSort[i], references);
		return ComputeNodeOrder.computeNodeOrder(toSort, (Object[][]) references.toArray(new Object[references.size()][]));
	}

	private void buildReferences(BundleDescription description, List references) {
		HostSpecification[] hosts = description.getHosts();
		// it is a fragment
		if (hosts.length > 0) {
			for (int i = 0; i < hosts.length; i++)
				// just create a dependency between fragment and host
				if (hosts[i].getSupplier() != null && hosts[i].getSupplier() != description)
					references.add(new Object[] {description, hosts[i].getSupplier()});
		} else {
			// it is a host
			buildReferences(description, description.getRequiredBundles(), references);
			buildReferences(description, description.getPackages(), references);
			BundleDescription[] fragments = description.getFragments();
			// handles constraints contributed by fragments
			for (int i = 0; i < fragments.length; i++) {
				// handles fragment's constraints as if they belonged to the host instead 
				buildReferences(description, fragments[i].getRequiredBundles(), references);
				buildReferences(description, fragments[i].getPackages(), references);
			}
		}
	}

	private void buildReferences(BundleDescription actual, VersionConstraint[] constraints, List references) {
		for (int i = 0; i < constraints.length; i++) {
			VersionConstraint constraint = constraints[i];
			if (constraint.getSupplier() != null && constraint.getSupplier() != actual)
				references.add(new Object[] {actual, constraint.getSupplier()});
		}
	}

	public static StateHelper getInstance() {
		return instance;
	}
}