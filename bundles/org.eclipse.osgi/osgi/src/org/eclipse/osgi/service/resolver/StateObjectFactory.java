/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.io.*;
import java.util.Dictionary;
import org.osgi.framework.BundleException;

/**
 * A factory for states and their component objects.  
 */
public interface StateObjectFactory {
	/**
	 * Creates an empty state. 
	 * 
	 * @return the created state
	 */
	public State createState();
	/**
	 * Creates a new state that is a copy of the given state. The returned state 
	 * will contain copies of all bundle descriptions in the given state. No data 
	 * pertaining to resolution is copied (resolution status, resolver, etc).
	 *  
	 * @param state a state to be copied
	 * @return the created state
	 */
	public State createState(State state);
	/**
	 * Creates a bundle description from the given parameters.
	 * 
	 * @param id id for the bundle 
	 * @param symbolicName symbolic name for the bundle (may be 
	 * <code>null</code>) 
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle
	 * @param required version constraints for all required bundles (may be 
	 * <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be  
	 * created. Should be <code>null</code> if the bundle is not a fragment
	 * @param packages version constraints for all packages imported and 
	 * exported (may be <code>null</code>)
	 * @param providedPackages names of all provided packages (may be 
	 * <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @return the created bundle description
	 * @deprecated use another version of createBundleDescription
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, PackageSpecification[] packages, String[] providedPackages, boolean singleton);
	/**
	 * Creates a bundle description from the given parameters.
	 * 
	 * @param id id for the bundle 
	 * @param symbolicName symbolic name for the bundle (may be 
	 * <code>null</code>) 
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle
	 * @param required version constraints for all required bundles (may be 
	 * <code>null</code>)
	 * @param hosts version constraints specifying the hosts for the bundle to be  
	 * created (may be <code>null</code>)
	 * @param packages version constraints for all packages imported and 
	 * exported (may be <code>null</code>)
	 * @param providedPackages names of all provided packages (may be 
	 * <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @return the created bundle description
	 */	
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification[] hosts, PackageSpecification[] packages, String[] providedPackages, boolean singleton);	
	/**
	 * Returns a bundle description based on the information in the supplied manifest dictionary.
	 * The manifest should contain String keys and String values which correspond to 
	 * proper OSGi manifest headers and values.
	 * 
	 * @param manifest a collection of OSGi manifest headers and values
	 * @param location the URL location of the bundle
	 * @param id the id of the bundle
	 * @return a bundle description derived from the given information
	 * @throws BundleException if an error occurs while reading the manifest 
	 */
	public BundleDescription createBundleDescription(Dictionary manifest, String location, long id) throws BundleException;
	/**
	 * Creates a bundle description that is a copy of the given description.
	 * 
	 * @param original the bundle description to be copied
	 * @return the created bundle description
	 */
	public BundleDescription createBundleDescription(BundleDescription original);
	/**
	 * Creates a bundle specification from the given parameters.
	 * 
	 * @param requiredSymbolicName the symbolic name for the required bundle
	 * @param requiredVersion the required version (may be <code>null</code>)
	 * @param matchRule the match rule
	 * @param export whether the required bundle should be re-exported 
	 * @param optional whether the constraint should be optional
	 * @return the created bundle specification
	 * @see VersionConstraint for information on the available match rules
	 */
	public BundleSpecification createBundleSpecification(String requiredSymbolicName, Version requiredVersion, byte matchRule, boolean export, boolean optional);
	/**
	 * Creates a bundle specification that is a copy of the given constraint.
	 *  
	 * @param original the constraint to be copied
	 * @return the created bundle specification
	 */
	public BundleSpecification createBundleSpecification(BundleSpecification original);
	/**
	 * Creates a host specification from the given parameters.
	 *  
	 * @param hostSymbolicName the symbolic name for the host bundle
	 * @param hostVersion the version for the host bundle (may be <code>null</code>)
	 * @param matchRule the match rule
	 * @param reloadHost whether the host should be reloaded when the fragment 
	 * is added/removed
	 * @return the created host specification
	 * @see VersionConstraint for information on the available match rules 
	 */
	public HostSpecification createHostSpecification(String hostSymbolicName, Version hostVersion, byte matchRule, boolean reloadHost);
	/**
	 * Creates a host specification that is a copy of the given constraint.
	 * 
	 * @param original the constraint to be copied
	 * @return the created host specification
	 */
	public HostSpecification createHostSpecification(HostSpecification original);
	/**
	 * Creates a package specification from the given parameters.
	 *  
	 * @param packageName the package name
	 * @param packageVersion the package version (may be <code>null</code>)
	 * @param exported whether the constraint describes a exported package or 
	 * imported package
	 * @return the created package specification
	 */
	public PackageSpecification createPackageSpecification(String packageName, Version packageVersion, boolean exported);
	/**
	 * Creates a package specification that is a copy of the given constraint
	 * @param original the constraint to be copied
	 * @return the created package specification 
	 */
	public PackageSpecification createPackageSpecification(PackageSpecification original);
	/**
	 * Persists the given state in the given output stream. Closes the stream.
	 * 
	 * @param state the state to be written
	 * @param stream the stream where to write the state to
	 * @throws IOException if an IOException happens while writing the state to 
	 * the stream
	 * @throws IllegalArgumentException if the state provided was not created by 
	 * this factory
	 */
	public void writeState(State state, DataOutputStream stream) throws IOException;
	/**
	 * Reads a persisted state from the given stream. Closes the stream.
	 * 
	 * @param stream the stream where to read the state from
	 * @return the state read
	 * @throws IOException if an IOException happens while reading the state from 
	 * the stream
	 */
	public State readState(DataInputStream stream) throws IOException;
}