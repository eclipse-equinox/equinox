/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public final class ReadOnlyState implements State {
	private final State target;

	public ReadOnlyState(State target) {
		this.target = target;
	}

	public boolean addBundle(BundleDescription description) {
		throw new UnsupportedOperationException();
	}

	public StateDelta compare(State state) throws BundleException {
		return target.compare(state);
	}

	public BundleDescription getBundle(long id) {
		return target.getBundle(id);
	}

	public BundleDescription getBundle(String symbolicName, Version version) {
		return target.getBundle(symbolicName, version);
	}

	public BundleDescription getBundleByLocation(String location) {
		return target.getBundleByLocation(location);
	}

	public BundleDescription[] getBundles() {
		return target.getBundles();
	}

	public BundleDescription[] getBundles(String symbolicName) {
		return target.getBundles(symbolicName);
	}

	public StateDelta getChanges() {
		return target.getChanges();
	}

	public ExportPackageDescription[] getExportedPackages() {
		return target.getExportedPackages();
	}

	public StateObjectFactory getFactory() {
		return target.getFactory();
	}

	public BundleDescription[] getResolvedBundles() {
		return target.getResolvedBundles();
	}

	public long getTimeStamp() {
		return target.getTimeStamp();
	}

	public boolean isEmpty() {
		return target.isEmpty();
	}

	public boolean isResolved() {
		return target.isResolved();
	}

	public boolean removeBundle(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription removeBundle(long bundleId) {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve() {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve(boolean incremental) {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve(BundleDescription[] discard) {
		throw new UnsupportedOperationException();
	}

	public StateDelta resolve(BundleDescription[] resolve, boolean discard) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("deprecation")
	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	public boolean updateBundle(BundleDescription newDescription) {
		throw new UnsupportedOperationException();
	}

	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] host, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolveImports) {
		throw new UnsupportedOperationException();
	}

	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedRequirements) {
		throw new UnsupportedOperationException();
	}

	public void removeBundleComplete(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	public Resolver getResolver() {
		return null;
	}

	public void setResolver(Resolver value) {
		throw new UnsupportedOperationException();
	}

	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties) {
		throw new UnsupportedOperationException();
	}

	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties[]) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	public Dictionary[] getPlatformProperties() {
		return target.getPlatformProperties();
	}

	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		throw new UnsupportedOperationException();
	}

	public void setTimeStamp(long timeStamp) {
		throw new UnsupportedOperationException();
	}

	public ExportPackageDescription[] getSystemPackages() {
		return target.getSystemPackages();
	}

	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		throw new UnsupportedOperationException();
	}

	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		return target.getResolverErrors(bundle);
	}

	public void removeResolverErrors(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	public long getHighestBundleId() {
		return target.getHighestBundleId();
	}

	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidPaths) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getDisabledBundles() {
		return target.getDisabledBundles();
	}

	public void addDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		return target.getDisabledInfos(bundle);
	}

	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		return target.getDisabledInfo(bundle, policyName);
	}

	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getRemovalPending() {
		throw new UnsupportedOperationException();
	}

	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles) {
		return target.getDependencyClosure(bundles);
	}

	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports) {
		throw new UnsupportedOperationException();
	}

	public void setResolverHookFactory(ResolverHookFactory hookFactory) {
		// do nothing
	}

}
