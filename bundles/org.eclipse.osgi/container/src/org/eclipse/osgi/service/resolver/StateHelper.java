/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.1
 * @see PlatformAdmin#getStateHelper
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface StateHelper {
	/**
	 * Indicates that access is encouraged to an <code>ExportPackageDescription</code>.
	 */
	public static int ACCESS_ENCOURAGED = 0x01;
	/**
	 * Indicates that access is discouraged to an <code>ExportPackageDescription</code>.
	 */
	public static int ACCESS_DISCOURAGED = 0x02;

	/**
	 * An option to include packages available from the execution environment when 
	 * getting the visible packages of a bundle.  For example, when running on a 
	 * J2SE 1.4 VM the system bundle will export the javax.xml.parsers package as part of the 
	 * execution environment.  When this option is used then any packages from the execution
	 * environment which the bundle is wired to will be included.
	 * @see StateHelper#getVisiblePackages(BundleDescription, int)
	 */
	public static int VISIBLE_INCLUDE_EE_PACKAGES = 0x01;

	/**
	 * An option to get all visible packages that a host bundle is currently wired to.  This 
	 * includes packages wired to as a result of a dynamic import and packages wired to as a 
	 * result of additional constraints specified by a fragment bundle.  Using this option 
	 * with a fragment will cause an empty array to be returned.
	 * @see StateHelper#getVisiblePackages(BundleDescription, int)
	 * @since 3.6
	 */
	public static int VISIBLE_INCLUDE_ALL_HOST_WIRES = 0x02;

	/**
	 * Returns all bundles in the state depending on the given bundles. The given bundles
	 * appear in the returned array.
	 * 
	 * @param bundles the initial set of bundles
	 * @return an array containing bundle descriptions for the given roots and all
	 * bundles in the state that depend on them
	 */
	public BundleDescription[] getDependentBundles(BundleDescription[] bundles);

	/**
	 * Returns all the prerequisite bundles in the state for the given bundles.  The given
	 * bundles appear in the returned array.
	 * @param bundles the inital set of bundles
	 * @return an array containing bundle descriptions for the given leaves and their
	 * prerequisite bundles in the state.
	 * @since 3.2
	 */
	public BundleDescription[] getPrerequisites(BundleDescription[] bundles);

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
	 * Returns all unsatisfied constraints in the given bundles that have no possible supplier. 
	 * Returns an empty array if no unsatisfied leaf constraints can be found.
	 * <p>
	 * The returned constraints include only the unsatisfied constraints in the given 
	 * state that have no possible supplier (leaf constraints).  There may 
	 * be additional unsatisfied constraints in the given bundles but these will have at 
	 * least one possible supplier.  In this case the possible supplier of the constraint 
	 * is not resolved for some reason.  For example, a given state only has Bundles X and Y
	 * installed and Bundles X and Y have the following constraints:
	 * </p>
	 * <pre>
	 * Bundle X requires Bundle Y
	 * Bundle Y requires Bundle Z</pre>
	 * <p>
	 * In this case Bundle Y has an unsatisfied constraint leaf on Bundle Z.  This will 
	 * cause Bundle X's constraint on Bundle Y to be unsatisfied as well because the 
	 * bundles are involved in a dependency chain.  Bundle X's constraint on Bundle Y is 
	 * not considered a leaf because there is a possible supplier Y in the given state.
	 * </p>
	 * <p>
	 * Note that a bundle may have no unsatisfied constraints and still not be 
	 * resolved.
	 * </p>  
	 * 
	 * @param bundles the bundles to examine
	 * @return an array containing all unsatisfied leaf constraints for the given bundles
	 * @since 3.2
	 */
	public VersionConstraint[] getUnsatisfiedLeaves(BundleDescription[] bundles);

	/**
	 * Returns whether the given package specification constraint is resolvable. 
	 * A package specification constraint may be 
	 * resolvable but not resolved, which means that the bundle that provides
	 * it has not been resolved for some other reason (e.g. another constraint 
	 * could not be resolved, another version has been picked, etc).
	 *  
	 * @param specification the package specification constraint to be examined
	 * @return <code>true</code> if the constraint can be resolved, 
	 * <code>false</code> otherwise
	 */
	public boolean isResolvable(ImportPackageSpecification specification);

	/**
	 * Returns whether the given bundle specification constraint is resolvable. 
	 * A bundle specification constraint may be 
	 * resolvable but not resolved, which means that the bundle that provides
	 * it has not been resolved for some other reason (e.g. another constraint 
	 * could not be resolved, another version has been picked, etc).
	 *  
	 * @param specification the bundle specification constraint to be examined
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
	 * @param specification the host specification constraint to be examined
	 * @return <code>true</code> if the constraint can be resolved, 
	 * <code>false</code> otherwise
	 */
	public boolean isResolvable(HostSpecification specification);

	/**
	 * Sorts the given array of <strong>resolved</strong> bundles in pre-requisite order. If A 
	 * requires B, A appears after B. 
	 * Fragments will appear after all of their hosts. Constraints contributed by fragments will 
	 * be treated as if contributed by theirs hosts, affecting their position. This is true even if
	 * the fragment does not appear in the given bundle array.
	 * <p>
	 * Unresolved bundles are ignored.
	 * </p>
	 *  
	 * @param toSort an array of bundles to be sorted
	 * @return any cycles found 
	 */
	public Object[][] sortBundles(BundleDescription[] toSort);

	/**
	 * Returns a list of all packages that the specified bundle has access to which are
	 * exported by other bundles.
	 * <p>
	 * Same as calling getVisiblePackages(bundle, 0)
	 * </p>
	 * @param bundle a bundle to get the list of packages for.
	 * @return a list of all packages that the specified bundle has access to which are
	 * exported by other bundles.
	 */
	public ExportPackageDescription[] getVisiblePackages(BundleDescription bundle);

	/**
	 * Returns a list of all packages that the specified bundle has access to which are
	 * exported by other bundles.  This takes into account all constraint specifications
	 * (Import-Package, Require-Bundle etc).  A deep dependency search is done for all 
	 * packages which are available through the required bundles and any bundles which 
	 * are reexported.  This method also takes into account all directives
	 * which may be specified on the constraint specifications (e.g. uses, x-friends etc.)
	 * 
	 * @param bundle a bundle to get the list of packages for.
	 * @param options the options for selecting the visible packages
	 * @return a list of all packages that the specified bundle has access to which are
	 * exported by other bundles.
	 * @see StateHelper#VISIBLE_INCLUDE_EE_PACKAGES
	 * @see StateHelper#VISIBLE_INCLUDE_ALL_HOST_WIRES
	 * @since 3.3
	 */
	public ExportPackageDescription[] getVisiblePackages(BundleDescription bundle, int options);

	/**
	 * Returns the access code that the specified <code>BundleDescription</code> has to the 
	 * specified <code>ExportPackageDescription</code>.
	 * @param bundle the bundle to find the access code for
	 * @param export the export to find the access code for
	 * @return the access code to the export.
	 * @see StateHelper#ACCESS_ENCOURAGED
	 * @see StateHelper#ACCESS_DISCOURAGED
	 */
	public int getAccessCode(BundleDescription bundle, ExportPackageDescription export);
}
