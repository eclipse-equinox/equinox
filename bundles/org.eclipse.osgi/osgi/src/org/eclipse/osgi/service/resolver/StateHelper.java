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
package org.eclipse.osgi.service.resolver;

import java.util.*;
import org.eclipse.osgi.framework.internal.core.KeyedElement;
import org.eclipse.osgi.framework.internal.core.KeyedHashSet;

/**
 * A helper class that provides convenience methods for manipulating 
 * state objects.
 */
public class StateHelper {
	/**
	 * Returns all bundles in the state depending on the given bundles. The given bundles
	 * appear in the returned array.
	 * 
	 * @param roots the initial set bundles
	 * @return an array containing bundle descriptions for the given roots and all
	 * bundles in the state that depend on them
	 */
	public static BundleDescription[] getDependentBundles(BundleDescription[] roots) {
		if (roots == null || roots.length == 0)
			return new BundleDescription[0];
		Set remaining = new HashSet(Arrays.asList(roots[0].getContainingState().getResolvedBundles()));
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
	/*
	 * Returns whether a bundle has any dependency on any of the given bundles.   
	 */
	private static boolean isDependent(BundleDescription candidate, KeyedHashSet bundles) {
		// is a fragment of any of them?
		HostSpecification candidateHost = candidate.getHost();
		if (candidateHost != null && candidateHost.isResolved() && bundles.contains((KeyedElement) candidateHost.getSupplier()))
			return true;
		// does require any of them?		
		BundleSpecification[] candidateRequired = candidate.getRequiredBundles();
		for (int i = 0; i < candidateRequired.length; i++)
			if (candidateRequired[i].isResolved() && bundles.contains((KeyedElement) candidateRequired[i].getSupplier()))
				return true;
		// does import any of their packages?			
		PackageSpecification[] candidatePackages = candidate.getPackages();
		for (int i = 0; i < candidatePackages.length; i++)
			if (candidatePackages[i].isResolved() && candidatePackages[i].getSupplier() != candidate && bundles.contains((KeyedElement) candidatePackages[i].getSupplier()))
				return true;
		return false;
	}
	/**
	 * Returns all unsatisfied constraints in the given bundle. Returns an 
	 * empty array if no unsatisfied constraints can be found.
	 * <p>
	 * Note that a bundle may have no unsatisfied constraints and still not be 
	 * resolved.
	 * </p>  
	 * 
	 * @param bundle the bundle to examine
	 * @return an array containing all unsatisfied constraints for the given bundle
	 */
	public static VersionConstraint[] getUnsatisfiedConstraints(BundleDescription bundle) {
		State containingState = bundle.getContainingState();
		if (containingState == null)
			// it is a bug in the client to call this method when not attached to a state
			throw new IllegalStateException("Does not belong to a state"); //$NON-NLS-1$		
		ArrayList unsatisfied = new ArrayList(); 
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
	 * Returns whether the given package specification constraint is resolvable. 
	 * A package specification constraint may be 
	 * resolvable but not resolved, which means that the bundle that provides
	 * it has not been resolved for some other reason (e.g. another constraint 
	 * could not be resolved, another version has been picked, etc).
	 *  
	 * @param constraint the package specification constraint to be examined
	 * @return <code>true</code> if the constraint can be resolved, 
	 * <code>false</code> otherwise
	 */
	public static boolean isResolvable(PackageSpecification specification) {
		if (specification.isExported())
			return true;
		PackageSpecification exported = getExportedPackage(specification.getBundle().getContainingState(), specification.getName(), null);
		if (exported == null)
			return false;
		return specification.isSatisfiedBy(exported.getVersionSpecification());		
	}
	/**
	 * Returns whether the given host specification constraint is resolvable. 
	 * A bundle specification constraint may be 
	 * resolvable but not resolved, which means that the bundle that provides
	 * it has not been resolved for some other reason (e.g. another constraint 
	 * could not be resolved, another version has been picked, etc).
	 *  
	 * @param constraint the bundle specification constraint to be examined
	 * @return <code>true</code> if the constraint can be resolved, 
	 * <code>false</code> otherwise
	 */
	public static boolean isResolvable(BundleSpecification specification) {
		return isBundleConstraintResolvable(specification);
	}
	/**
	 * Returns whether the given host specification constraint is resolvable. 
	 * A host specification constraint may be 
	 * resolvable but not resolved, which means that the bundle that provides
	 * it has not been resolved for some other reason (e.g. another constraint 
	 * could not be resolved, another version has been picked, etc).
	 *  
	 * @param constraint the host specification constraint to be examined
	 * @return <code>true</code> if the constraint can be resolved, 
	 * <code>false</code> otherwise
	 */
	public static boolean isResolvable(HostSpecification specification) {
		return isBundleConstraintResolvable(specification);
	}
	/*
	 * Returns whether a bundle specification/host specification can be resolved.
	 */
	private static boolean isBundleConstraintResolvable(VersionConstraint constraint) {		
		BundleDescription[] availableBundles = constraint.getBundle().getContainingState().getBundles(constraint.getName());
		for (int i = 0; i < availableBundles.length; i++)
			if (availableBundles[i].isResolved() && constraint.isSatisfiedBy(availableBundles[i].getVersion()))
				return true;
		return false;
	}
	/**
	 * Returns all packages exported by the given bundle. Returns an empty array 
	 * if no packages are exported.
	 * 
	 * @param bundle the bundle
	 * @return all packages exported by the given bundle
	 */
	public static PackageSpecification[] getExportedPackages(BundleDescription bundle) {
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
	 * Returns the package specification corresponding to the given 
	 * package name/version that has been elected to be exported in the given 
	 * state. Returns <code>null</code> if none exists. 
	 * <p>
	 * In case a package version is not provided, returns any version of the 
	 * package that has been exported. 
	 * </p>
	 * 
	 * @param state the state 
	 * @param packageName the name of the package 
	 * @param version the version of the package (can be <code>null</code>)
	 * @return
	 */
	public static PackageSpecification getExportedPackage(State state, String packageName, Version version) {
		BundleDescription[] resolvedBundles = state.getResolvedBundles();
		boolean ignoreVersion = version == null;
		for (int i = 0; i < resolvedBundles.length; i++) {
			PackageSpecification[] packages = resolvedBundles[i].getPackages();
			for (int j = 0; i < packages.length; j++)
				if (packages[j].getName().equals(packageName) && (ignoreVersion || packages[i].getVersionSpecification().equals(version)) && (packages[j].getSupplier() != null))
					return packages[j].getSupplier().getPackage(packageName);
		}
		return null;
	}	
}