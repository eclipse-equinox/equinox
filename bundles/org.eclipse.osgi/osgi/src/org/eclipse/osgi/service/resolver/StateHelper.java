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

/**
 * A helper class that provides convenience methods for manipulating 
 * state objects. <code>PlatformAdmin</code> provides an access point
 * for a state helper.
 * <p>
 * Clients should not implement this interface.
 * </p>
 * 
 * @see PlatformAdmin#getStateHelper
 */
public interface StateHelper {
	/**
	 * Returns all bundles in the state depending on the given bundles. The given bundles
	 * appear in the returned array.
	 * 
	 * @param roots the initial set bundles
	 * @return an array containing bundle descriptions for the given roots and all
	 * bundles in the state that depend on them
	 */
	public BundleDescription[] getDependentBundles(BundleDescription[] roots);
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
	public VersionConstraint[] getUnsatisfiedConstraints(BundleDescription bundle);
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
	public boolean isResolvable(PackageSpecification specification);
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
	public boolean isResolvable(BundleSpecification specification);
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
	public boolean isResolvable(HostSpecification specification);
	/**
	 * Returns all packages exported by the given bundle. Returns an empty array 
	 * if no packages are exported.
	 * 
	 * @param bundle the bundle
	 * @return all packages exported by the given bundle
	 */
	public PackageSpecification[] getExportedPackages(BundleDescription bundle);
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
	public PackageSpecification getExportedPackage(State state, String packageName, Version version);
}