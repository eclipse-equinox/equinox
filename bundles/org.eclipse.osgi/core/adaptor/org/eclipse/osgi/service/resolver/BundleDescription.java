/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.Map;
import org.osgi.framework.wiring.BundleRevision;

/**
 * This class represents a specific version of a bundle in the system.
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface BundleDescription extends BaseDescription, BundleRevision {

	/**
	 * Gets the Bundle-SymbolicName of this BundleDescription.
	 * Same as calling {@link BaseDescription#getName()}.
	 * @return The bundle symbolic name or null if the bundle
	 * does not have a symbolic name.
	 */
	public String getSymbolicName();

	/**
	 * Returns the arbitrary attributes for this bundle description.
	 * @return the arbitrary attributes for this bundle description
	 * @since 3.7
	 */
	public Map<String, Object> getAttributes();

	/**
	 * The location string for this bundle.
	 * @return The bundle location or null if the bundle description
	 * does not have a location
	 */
	public String getLocation();

	/**
	 * Returns an array of bundle specifications defined by the Require-Bundle
	 * clause in this bundle.
	 * 
	 * @return an array of bundle specifications
	 */
	public BundleSpecification[] getRequiredBundles();

	/**
	 * Returns an array of export package descriptions defined by the Export-Package clauses.
	 * All export package descriptions are returned even if they have not been selected by
	 * the resolver as an exporter of the package.
	 *
	 * @return an array of export package descriptions
	 */
	public ExportPackageDescription[] getExportPackages();

	/**
	 * Returns an array of import package specifications defined by the Import-Package clause.
	 * @return an array of import package specifications
	 */
	public ImportPackageSpecification[] getImportPackages();

	/**
	 * Returns an array of dynamic import package specifications that have been added
	 * dynamically to this bundle description.
	 * @return an array of dynamic import package specifications
	 * @see State#addDynamicImportPackages(BundleDescription, ImportPackageSpecification[])
	 * @since 3.7
	 */
	public ImportPackageSpecification[] getAddedDynamicImportPackages();

	/**
	 * Returns an array of generic specifications constraints required by this bundle.
	 * @return an array of generic specifications
	 * @since 3.2
	 */
	public GenericSpecification[] getGenericRequires();

	/**
	 * Returns an array of generic descriptions for the capabilities of this bundle.
	 * @return an array of generic descriptions
	 * @since 3.2
	 */
	public GenericDescription[] getGenericCapabilities();

	/**
	 * Returns true if this bundle has one or more dynamically imported packages.
	 * @return true if this bundle has one or more dynamically imported packages.
	 */
	public boolean hasDynamicImports();

	/**
	 * Returns all the exported packages from this bundle that have been selected by
	 * the resolver.  The returned list will include the ExportPackageDescriptions
	 * returned by {@link #getExportPackages()} that have been selected by the resolver and
	 * packages which are propagated by this bundle.
	 * @return the selected list of packages that this bundle exports.  If the bundle is
	 * unresolved or has no shared packages then an empty array is returned.
	 */
	public ExportPackageDescription[] getSelectedExports();

	/**
	 * Returns all the capabilities provided by ths bundle that have been selected by
	 * the resolver.  The returned list will include the capabilities
	 * returned by {@link #getGenericCapabilities()} that have been selected by the 
	 * resolver and any capabilities provided by fragments attached to this bundle.
	 * @return the selected capabilities that this bundle provides.  If the bundle is
	 * unresolved or has no capabilities then an empty array is returned.
	 * @since 3.7
	 */
	public GenericDescription[] getSelectedGenericCapabilities();

	/**
	 * Returns all the bundle descriptions that satisfy all the require bundles for this bundle.
	 * If the bundle is not resolved or the bundle does not require any bundles then an empty array is
	 * returned.
	 * @return the bundles descriptions that satisfy all the require bundles for this bundle.
	 */
	public BundleDescription[] getResolvedRequires();

	/**
	 * Returns all the export packages that satisfy all the imported packages for this bundle.
	 * If the bundle is not resolved or the bundle does not import any packages then an empty array is
	 * returned.
	 * @return the exported packages that satisfy all the imported packages for this bundle.
	 */
	public ExportPackageDescription[] getResolvedImports();

	/**
	 * Returns all the capabilities that satisfy all the capability requirements for this
	 * bundle.  This includes any capabilities required by fragments attached to this bundle.
	 * @return the capabilities that satisfy all the capability requirements for this bundle.
	 * If the bundle is unresolved or has no capability requirements then an empty array is
	 * returned.
	 * @since 3.7
	 */
	public GenericDescription[] getResolvedGenericRequires();

	/**
	 * Returns true if this bundle is resolved in its host state.
	 * 
	 * @return true if this bundle is resolved in its host state.
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
	 * Returns the string representation of this bundle.
	 * 
	 * @return String representation of this bundle.
	 */
	public String toString();

	/**
	 * Returns the host for this bundle. null is returned if this bundle is not
	 * a fragment.
	 * 
	 * @return the host for this bundle.
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
	 * 
	 * @return an array of BundleDescriptions containing all known fragments
	 */
	public BundleDescription[] getFragments();

	/**
	 * Returns whether this bundle is a singleton.  Singleton bundles require 
	 * that at most one single version of the bundle can be resolved at a time. 
	 * <p>
	 * The existence of a single bundle marked as singleton causes all bundles
	 * with the same symbolic name to be treated as singletons as well.  
	 * </p>
	 * 
	 * @return <code>true</code>, if this bundle is a singleton, 
	 * <code>false</code> otherwise
	 */
	public boolean isSingleton();

	/**
	 * Returns whether this bundle is pending a removal.  A bundle is pending
	 * removal if it has been removed from the state but other bundles in
	 * the state currently depend on it.
	 * @return <code>true</code>, if this bundle is pending a removal,
	 * <code>false</code> otherwise
	 */
	public boolean isRemovalPending();

	/**
	 * Returns all bundles which depend on this bundle.  A bundle depends on
	 * another bundle if it requires the bundle, imports a package which is 
	 * exported by the bundle, is a fragment to the bundle or is the host
	 * of the bundle.
	 * @return all bundles which depend on this bundle.
	 */
	public BundleDescription[] getDependents();

	/**
	 * Returns the platform filter in the form of an LDAP filter
	 * @return the platfomr filter in the form of an LDAP filter
	 */
	public String getPlatformFilter();

	/**
	 * Returns true if this bundle allows fragments to attach
	 * @return true if this bundle allows fragments to attach
	 */
	public boolean attachFragments();

	/**
	 * Returns true if this bundle allows fragments to attach dynamically
	 * after it has been resolved.
	 * @return true if this bundle allows fragments to attach dynamically
	 */
	public boolean dynamicFragments();

	/**
	 * Returns the list of execution environments that are required by 
	 * this bundle.  Any one of the listed execution environments will 
	 * allow this bundle to be resolved.
	 * @since 3.2
	 * @return the list of execution environments that are required.
	 */
	public String[] getExecutionEnvironments();

	/**
	 *  Returns the native code specification for this bundle.  A value
	 *  of <code>null</code> is returned if there is no native code
	 *  specification.
	 * @return the native code specification.
	 * @since 3.4
	 */
	public NativeCodeSpecification getNativeCodeSpecification();

	/**
	 * Returns the export packages that satisfy imported packages for this bundle description
	 * and substitute one of the exports for this bundle description.  If the bundle is not resolved
	 * or the bundle does not have substituted exports then an empty array is
	 * returned.
	 * @return all substituted exports for this bundle description
	 * @since 3.4
	 */
	public ExportPackageDescription[] getSubstitutedExports();
}
