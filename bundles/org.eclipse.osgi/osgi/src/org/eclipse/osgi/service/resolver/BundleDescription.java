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
package org.eclipse.osgi.service.resolver;

import java.util.Dictionary;

/**
 * This class represents a specific version of a bundle in the system.
 */

public interface BundleDescription {

	/**
	 * The location string for this bundle
	 */
	public String getLocation();

	/**
	 * Returns an array of package specifications defined by the Import-Package
	 * and Export-Package clauses. Exported and imported packages can be told
	 * apart by checking the isExport flag.
	 * 
	 * @see PackageSpecification#isExport
	 * @return an array of package specifications
	 */
	public PackageSpecification[] getPackages();
	/**
	 * Returns an array of package specifications defined by the
	 * Provide-Package clause.
	 * 
	 * @return an array of package names
	 */
	public String[] getProvidedPackages();
	/**
	 * Returns the package specification whose package name matches the given
	 * name. The packages detailed in getPackages() are searched for the given
	 * name.
	 * 
	 * @param name the name of the package to look for
	 * @return the package description for the discovered bundle or null if
	 * none could be found.
	 */
	public PackageSpecification getPackage(String name);

	/**
	 * Returns an array of bundle specifications defined by the Require-Bundle
	 * clause in this bundle.
	 * 
	 * @return an array of bundle specifications
	 */
	public BundleSpecification[] getRequiredBundles();

	/**
	 * Returns the bundle specification for the bundle with the given unique id
	 * in this bundle.
	 * 
	 * @param uniqueId the id of the required bundle to look for.
	 * @return the discovered bunde specification or null if none could be
	 * found.
	 */
	public BundleSpecification getRequiredBundle(String name);

	/**
	 * Return unique id of the bundle.
	 * 
	 * @return the unique id of this bundle.
	 */
	public String getUniqueId();

	/**
	 * Returns true if this bundle is resolved in its host state.
	 * 
	 * @return
	 */
	public boolean isResolved();

	/**
	 * Returns the state object which hosts this bundle. null is returned if
	 * this bundle is not currently in a state.
	 * 
	 * @return the state object which hosts this bundle.
	 */
	public State getContainingState();

	/**
	 * Returns the framework state of this bundle. The return value can be one
	 * of the valid states as defined in org.osgi.framwork.Bundle or NONE as
	 * defined in BundleDescription.
	 * 
	 * @return @see org.osgi.framework.Bundle
	 */
	public int getState();

	/**
	 * Returns the version specification of for this bundle.
	 * 
	 * @return the version specification for this bundle.
	 */
	public Version getVersion();
	/**
	 * Returns an array containing all constraints specified by the given 
	 * bundle that could not be satisfied. If all constraints could be 
	 * satisfied, returns an empty array. This does not relate to the fact that
	 * the bundle became resolved or not. A resolved bundle may have 
	 * unsatisfied constraints (if they are optional), as well as an unresolved 
	 * bundle may not have any unsatisfied constraints (which means that it has
	 * not been picked - for instance, if only one version is allowed and there 
	 * is a "better" version).  
	 * @return an array of <code>VersionConstraint</code> objects containing 
	 * 	all constraints that could not be satisfied.  
	 */
	public VersionConstraint[] getUnsatisfiedConstraints();

	/**
	 * Returns a read-only dictionary for the Manifest of this bundle.
	 */
	public Dictionary getManifest();

	/**
	 * Returns the string representation of this bundle.
	 * 
	 * @return String representation of this bundle.
	 */
	public String toString();

	/**
	 * Returns the host for this bundle. null is returned if this bundle is not
	 * a fragment.
	 * 
	 * @return
	 */
	public HostSpecification getHost();

	/**
	 * Returns the numeric id of this bundle.  Typically a bundle description
	 * will only have a numeric id if it represents a bundle that is installed in a 
	 * framework as the framework assigns the ids.  -1 is returned if the id is not known.
	 * 
	 * @return the numeric id of this bundle description
	 */
	public long getBundleId();

	/**
	 * Returns all fragments known to this bundle (regardless resolution status).
	 * @return an array of BundleDescriptions containing all known fragments
	 */
	public BundleDescription[] getFragments();
}
