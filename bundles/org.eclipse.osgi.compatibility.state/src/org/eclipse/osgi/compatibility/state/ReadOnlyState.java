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

	public boolean addBundle(BundleDescription description) {
		throw new UnsupportedOperationException();
	}

	public StateDelta compare(State state) throws BundleException {
		return platformAdmin.getSystemState().compare(state);
	}

	public BundleDescription getBundle(long id) {
		return platformAdmin.getSystemState().getBundle(id);
	}

	public BundleDescription getBundle(String symbolicName, Version version) {
		return platformAdmin.getSystemState().getBundle(symbolicName, version);
	}

	public BundleDescription getBundleByLocation(String location) {
		return platformAdmin.getSystemState().getBundleByLocation(location);
	}

	public BundleDescription[] getBundles() {
		return platformAdmin.getSystemState().getBundles();
	}

	public BundleDescription[] getBundles(String symbolicName) {
		return platformAdmin.getSystemState().getBundles(symbolicName);
	}

	public StateDelta getChanges() {
		return platformAdmin.getSystemState().getChanges();
	}

	public ExportPackageDescription[] getExportedPackages() {
		return platformAdmin.getSystemState().getExportedPackages();
	}

	public StateObjectFactory getFactory() {
		return platformAdmin.getSystemState().getFactory();
	}

	public BundleDescription[] getResolvedBundles() {
		return platformAdmin.getSystemState().getResolvedBundles();
	}

	public long getTimeStamp() {
		return platformAdmin.getTimeStamp();
	}

	public boolean isEmpty() {
		return platformAdmin.getSystemState().isEmpty();
	}

	public boolean isResolved() {
		return platformAdmin.getSystemState().isResolved();
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

	public Dictionary[] getPlatformProperties() {
		return platformAdmin.getSystemState().getPlatformProperties();
	}

	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		throw new UnsupportedOperationException();
	}

	public void setTimeStamp(long timeStamp) {
		throw new UnsupportedOperationException();
	}

	public ExportPackageDescription[] getSystemPackages() {
		return platformAdmin.getSystemState().getSystemPackages();
	}

	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		throw new UnsupportedOperationException();
	}

	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		return platformAdmin.getSystemState().getResolverErrors(bundle);
	}

	public void removeResolverErrors(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	public long getHighestBundleId() {
		return platformAdmin.getSystemState().getHighestBundleId();
	}

	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidPaths) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getDisabledBundles() {
		return platformAdmin.getSystemState().getDisabledBundles();
	}

	public void addDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		return platformAdmin.getSystemState().getDisabledInfos(bundle);
	}

	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		return platformAdmin.getSystemState().getDisabledInfo(bundle, policyName);
	}

	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	public BundleDescription[] getRemovalPending() {
		throw new UnsupportedOperationException();
	}

	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles) {
		return platformAdmin.getSystemState().getDependencyClosure(bundles);
	}

	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports) {
		throw new UnsupportedOperationException();
	}

	public void setResolverHookFactory(ResolverHookFactory hookFactory) {
		// do nothing
	}

}
