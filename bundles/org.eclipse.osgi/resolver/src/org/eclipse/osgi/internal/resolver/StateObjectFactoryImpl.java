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
package org.eclipse.osgi.internal.resolver;

import java.io.*;
import java.util.Dictionary;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class StateObjectFactoryImpl implements StateObjectFactory {
	public static final byte NO_MATCH = 0;
	public static final byte QUALIFIER_MATCH = 1;
	public static final byte MICRO_MATCH = 5;
	public static final byte MINOR_MATCH = 2;
	public static final byte MAJOR_MATCH = 3;
	public static final byte GREATER_EQUAL_MATCH = 4;
	public static final byte OTHER_MATCH = 5;

	public BundleDescription createBundleDescription(Dictionary manifest, String location, long id) throws BundleException {
		BundleDescriptionImpl result;
		result = (BundleDescriptionImpl) StateBuilder.createBundleDescription(manifest, location);
		result.setBundleId(id);
		return result;
	}

	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, PackageSpecification[] packages, String[] providedPackages, boolean singleton) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(id);
		bundle.setSymbolicName(symbolicName);
		bundle.setVersion(version);
		bundle.setLocation(location);
		bundle.setRequiredBundles(required);
		bundle.setPackages(packages);
		bundle.setHost(host);
		bundle.setProvidedPackages(providedPackages);
		bundle.setSingleton(singleton);
		return bundle;
	}

	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification[] hosts, PackageSpecification[] packages, String[] providedPackages, boolean singleton) {
		HostSpecification host = hosts == null || hosts.length == 0 ? null : hosts[1];
		return createBundleDescription(id, symbolicName, version, location, required, host, packages, providedPackages, singleton);
	}

	public BundleDescription createBundleDescription(BundleDescription original) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(original.getBundleId());
		bundle.setSymbolicName(original.getSymbolicName());
		bundle.setVersion(original.getVersion());
		bundle.setLocation(original.getLocation());
		BundleSpecification[] originalRequired = original.getRequiredBundles();
		BundleSpecification[] newRequired = new BundleSpecification[originalRequired.length];
		for (int i = 0; i < newRequired.length; i++)
			newRequired[i] = createBundleSpecification(originalRequired[i]);
		bundle.setRequiredBundles(newRequired);
		PackageSpecification[] originalPackages = original.getPackages();
		PackageSpecification[] newPackages = new PackageSpecification[originalPackages.length];
		for (int i = 0; i < newPackages.length; i++)
			newPackages[i] = createPackageSpecification(originalPackages[i]);
		bundle.setPackages(newPackages);
		if (original.getHost() != null)
			bundle.setHost(createHostSpecification(original.getHost()));
		String[] originalProvidedPackages = original.getProvidedPackages();
		String[] newProvidedPackages = new String[originalProvidedPackages.length];
		System.arraycopy(originalProvidedPackages, 0, newProvidedPackages, 0, originalProvidedPackages.length);
		bundle.setProvidedPackages(newProvidedPackages);
		bundle.setSingleton(original.isSingleton());
		return bundle;
	}

	public BundleSpecification createBundleSpecification(String requiredSymbolicName, Version requiredVersion, byte matchingRule, boolean export, boolean optional) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();
		bundleSpec.setName(requiredSymbolicName);
		setVersionRange(bundleSpec, matchingRule, requiredVersion);
		bundleSpec.setExported(export);
		bundleSpec.setOptional(optional);
		return bundleSpec;
	}

	public BundleSpecification createBundleSpecification(BundleSpecification original) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();
		bundleSpec.setName(original.getName());
		bundleSpec.setVersionRange(original.getVersionRange());
		bundleSpec.setExported(original.isExported());
		bundleSpec.setOptional(original.isOptional());
		return bundleSpec;
	}

	public HostSpecification createHostSpecification(String hostSymbolicName, Version hostVersion, byte matchingRule, boolean reloadHost) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();
		hostSpec.setName(hostSymbolicName);
		setVersionRange(hostSpec, matchingRule, hostVersion);
		hostSpec.setReloadHost(reloadHost);
		return hostSpec;
	}

	public HostSpecification createHostSpecification(HostSpecification original) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();
		hostSpec.setName(original.getName());
		hostSpec.setVersionRange(original.getVersionRange());
		hostSpec.setReloadHost(original.reloadHost());
		return hostSpec;
	}

	public PackageSpecification createPackageSpecification(String packageName, Version packageVersion, boolean exported) {
		PackageSpecificationImpl packageSpec = new PackageSpecificationImpl();
		packageSpec.setName(packageName);
		packageSpec.setVersionRange(new VersionRange(packageVersion, Version.maxVersion));
		packageSpec.setExport(exported);
		return packageSpec;
	}

	public PackageSpecification createPackageSpecification(PackageSpecification original) {
		PackageSpecificationImpl packageSpec = new PackageSpecificationImpl();
		packageSpec.setName(original.getName());
		packageSpec.setVersionRange(original.getVersionRange());
		packageSpec.setExport(original.isExported());
		return packageSpec;
	}

	public SystemState createSystemState() {
		SystemState state = new SystemState();
		state.setFactory(this);
		return state;
	}

	public State createState() {
		StateImpl state = new UserState();
		state.setFactory(this);
		return state;
	}

	public State createState(State original) {
		StateImpl newState = new UserState();
		newState.setFactory(this);
		newState.setTimeStamp(original.getTimeStamp());
		BundleDescription[] bundles = original.getBundles();
		for (int i = 0; i < bundles.length; i++)
			newState.basicAddBundle(createBundleDescription(bundles[i]));
		newState.setResolved(false);
		return newState;
	}

	public SystemState readSystemState(DataInputStream stream, long expectedTimeStamp) throws IOException {
		StateReader reader = new StateReader();
		SystemState restoredState = new SystemState();
		if (!reader.loadState(restoredState, stream, expectedTimeStamp))
			return null;
		restoredState.setFactory(this);
		return restoredState;
	}

	public State readState(DataInputStream stream) throws IOException {
		StateReader reader = new StateReader();
		StateImpl restoredState = (StateImpl) createState();
		if (!reader.loadState(restoredState, stream))
			return null;
		return restoredState;
	}

	public void writeState(State state, DataOutputStream stream) throws IOException {
		if (state.getFactory() != this)
			throw new IllegalArgumentException();
		StateWriter writer = new StateWriter();
		writer.saveState((StateImpl) state, stream);
	}

	private void setVersionRange(VersionConstraintImpl constraint, int matchingRule, Version minVersion) {
		if (matchingRule == NO_MATCH || matchingRule == OTHER_MATCH)
			return;
		if (minVersion == null)
			return;
		switch (matchingRule) {
			case QUALIFIER_MATCH : {
				constraint.setVersionRange(new VersionRange(minVersion, minVersion));
				break;
			}
			case MICRO_MATCH : {
				Version maxVersion = new Version(minVersion.getMajorComponent(), minVersion.getMinorComponent(), minVersion.getMicroComponent() + 1, "", false); //$NON-NLS-1$
				constraint.setVersionRange(new VersionRange(minVersion, maxVersion));
				break;
			}
			case MINOR_MATCH : {
				Version maxVersion = new Version(minVersion.getMajorComponent(), minVersion.getMinorComponent() + 1, 0, "", false); //$NON-NLS-1$
				constraint.setVersionRange(new VersionRange(minVersion, maxVersion));
				break;
			}
			case MAJOR_MATCH : {
				Version maxVersion = new Version(minVersion.getMajorComponent() + 1, 0, 0, "", false); //$NON-NLS-1$
				constraint.setVersionRange(new VersionRange(minVersion, maxVersion));
				break;
			}
			case GREATER_EQUAL_MATCH : {
				constraint.setVersionRange(new VersionRange(minVersion, Version.maxVersion));
				break;
			}
		}
	}
}