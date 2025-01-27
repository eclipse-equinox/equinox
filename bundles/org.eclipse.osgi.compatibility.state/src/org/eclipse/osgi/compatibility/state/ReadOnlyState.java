/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import org.eclipse.osgi.internal.resolver.StateHelperImpl;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public final class ReadOnlyState implements State {
	private final PlatformAdminImpl platformAdmin;

	public ReadOnlyState(PlatformAdminImpl platformAdmin) {
		this.platformAdmin = platformAdmin;
	}

	@Override
	public boolean addBundle(BundleDescription description) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta compare(State state) throws BundleException {
		return platformAdmin.getSystemState().compare(state);
	}

	@Override
	public BundleDescription getBundle(long id) {
		return platformAdmin.getSystemState().getBundle(id);
	}

	@Override
	public BundleDescription getBundle(String symbolicName, Version version) {
		return platformAdmin.getSystemState().getBundle(symbolicName, version);
	}

	@Override
	public BundleDescription getBundleByLocation(String location) {
		return platformAdmin.getSystemState().getBundleByLocation(location);
	}

	@Override
	public BundleDescription[] getBundles() {
		return platformAdmin.getSystemState().getBundles();
	}

	@Override
	public BundleDescription[] getBundles(String symbolicName) {
		return platformAdmin.getSystemState().getBundles(symbolicName);
	}

	@Override
	public StateDelta getChanges() {
		return platformAdmin.getSystemState().getChanges();
	}

	@Override
	public ExportPackageDescription[] getExportedPackages() {
		return platformAdmin.getSystemState().getExportedPackages();
	}

	@Override
	public StateObjectFactory getFactory() {
		return platformAdmin.getSystemState().getFactory();
	}

	@Override
	public BundleDescription[] getResolvedBundles() {
		return platformAdmin.getSystemState().getResolvedBundles();
	}

	@Override
	public long getTimeStamp() {
		return platformAdmin.getTimeStamp();
	}

	@Override
	public boolean isEmpty() {
		return platformAdmin.getSystemState().isEmpty();
	}

	@Override
	public boolean isResolved() {
		return platformAdmin.getSystemState().isResolved();
	}

	@Override
	public boolean removeBundle(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BundleDescription removeBundle(long bundleId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta resolve() {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta resolve(boolean incremental) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta resolve(BundleDescription[] discard) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta resolve(BundleDescription[] resolve, boolean discard) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean updateBundle(BundleDescription newDescription) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] host, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolveImports) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedRequirements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeBundleComplete(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Resolver getResolver() {
		return null;
	}

	@Override
	public void setResolver(Resolver value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties[]) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dictionary[] getPlatformProperties() {
		return platformAdmin.getSystemState().getPlatformProperties();
	}

	@Override
	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTimeStamp(long timeStamp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ExportPackageDescription[] getSystemPackages() {
		return platformAdmin.getSystemState().getSystemPackages();
	}

	@Override
	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		return platformAdmin.getSystemState().getResolverErrors(bundle);
	}

	@Override
	public void removeResolverErrors(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	@Override
	public long getHighestBundleId() {
		return platformAdmin.getSystemState().getHighestBundleId();
	}

	@Override
	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidPaths) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BundleDescription[] getDisabledBundles() {
		return platformAdmin.getSystemState().getDisabledBundles();
	}

	@Override
	public void addDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		return platformAdmin.getSystemState().getDisabledInfos(bundle);
	}

	@Override
	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		return platformAdmin.getSystemState().getDisabledInfo(bundle, policyName);
	}

	@Override
	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BundleDescription[] getRemovalPending() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles) {
		return platformAdmin.getSystemState().getDependencyClosure(bundles);
	}

	@Override
	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setResolverHookFactory(ResolverHookFactory hookFactory) {
		// do nothing
	}

}
