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

public interface StateObjectFactory {
	public State createState();
	public State createState(State state);	
	public BundleDescription createBundleDescription(long id, String globalName, Version version, String location, BundleSpecification[] required, HostSpecification host, PackageSpecification[] packages, String[] providedPackages);
	/**
	 * Returns a bundle description based on the information in the supplied manifest dictionary.
	 * The manifest should contain String keys and String values which correspond to 
	 * proper OSGi manifest headers and values.
	 * 
	 * @param manifest a collection of OSGi manifest headers and values
	 * @param location the URL location of the bundle
	 * @param id the id of the bundle
	 * @return a bundle description derived from the given information
	 */	
	public BundleDescription createBundleDescription(Dictionary manifest, String location, long id);
	public BundleDescription createBundleDescription(BundleDescription original);
	public BundleSpecification createBundleSpecification(BundleDescription parentBundle, String hostGlobalName, Version hostVersion, byte matchRule, boolean export, boolean optional);
	public BundleSpecification createBundleSpecification(BundleSpecification original);	
	public HostSpecification createHostSpecification(BundleDescription parentBundle, String hostGlobalName, Version hostVersion, byte matchRule, boolean reloadHost);
	public HostSpecification createHostSpecification(HostSpecification original);	
	public PackageSpecification createPackageSpecification(BundleDescription parentBundle, String packageName, Version packageVersion, boolean exported);
	public PackageSpecification createPackageSpecification(PackageSpecification original);

}
