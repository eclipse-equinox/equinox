/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public class ReadOnlySystemState implements State {

	private final ModuleContainer container;
	private final ModuleDatabase database;
	private final StateDelta delta = new StateDelta() {
		@Override
		public State getState() {
			// TODO Auto-generated method stub
			return ReadOnlySystemState.this;
		}

		@Override
		public ResolverHookException getResovlerHookException() {
			return null;
		}

		@Override
		public BundleDelta[] getChanges(int mask, boolean exact) {
			return new BundleDelta[0];
		}

		@Override
		public BundleDelta[] getChanges() {
			return new BundleDelta[0];
		}
	};

	public ReadOnlySystemState(ModuleContainer container, ModuleDatabase database) {
		this.container = container;
		this.database = database;
	}

	@Override
	public boolean addBundle(BundleDescription description) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta compare(State baseState) throws BundleException {
		return delta;
	}

	@Override
	public BundleDescription removeBundle(long bundleId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeBundle(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean updateBundle(BundleDescription newDescription) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta getChanges() {
		return delta;
	}

	@Override
	public BundleDescription[] getBundles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDescription getBundle(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDescription getBundle(String symbolicName, Version version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDescription getBundleByLocation(String location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTimeStamp() {
		return database.getRevisionsTimestamp();
	}

	@Override
	public void setTimeStamp(long newTimeStamp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedWires) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeBundleComplete(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeResolverErrors(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResolverError[] getResolverErrors(BundleDescription bundle) {
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
	public StateDelta resolve(boolean incremental) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StateDelta resolve() {
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
	public void setOverrides(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BundleDescription[] getResolvedBundles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDescription[] getRemovalPending() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExportPackageDescription[] getExportedPackages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDescription[] getBundles(String symbolicName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateObjectFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setPlatformProperties(Dictionary<?, ?>[] platformProperties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dictionary[] getPlatformProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExportPackageDescription[] getSystemPackages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateHelper getStateHelper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getHighestBundleId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidNativePaths) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BundleDescription[] getDisabledBundles() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DisabledInfo[] getDisabledInfos(BundleDescription bundle) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setResolverHookFactory(ResolverHookFactory hookFactory) {
		throw new UnsupportedOperationException();
	}

}
