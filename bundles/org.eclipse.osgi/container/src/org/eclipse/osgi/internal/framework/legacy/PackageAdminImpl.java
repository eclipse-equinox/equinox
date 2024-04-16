/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.osgi.internal.framework.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.container.Capabilities;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
//import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

@SuppressWarnings("deprecation")
public class PackageAdminImpl implements PackageAdmin {
	private final FrameworkWiring frameworkWiring;
	private final EquinoxContainer equinoxContainer;

	public PackageAdminImpl(EquinoxContainer equinoxContainer, FrameworkWiring frameworkWiring) {
		this.equinoxContainer = equinoxContainer;
		this.frameworkWiring = frameworkWiring;
	}

	@Override
	public ExportedPackage[] getExportedPackages(Bundle bundle) {
		if (bundle == null) {
			return getExportedPackages((String) null);
		}

		Collection<BundleRevision> revisions = bundle.adapt(BundleRevisions.class).getRevisions();

		Collection<ExportedPackage> allExports = new ArrayList<>();
		for (BundleRevision revision : revisions) {
			ModuleWiring wiring = (ModuleWiring) revision.getWiring();
			if (wiring != null) {
				List<ModuleCapability> providedPackages = wiring
						.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
				if (providedPackages != null) {
					for (ModuleCapability providedPackage : providedPackages) {
						allExports.add(new ExportedPackageImpl(providedPackage, wiring));
					}
				}
			}
		}
		return allExports.isEmpty() ? null : allExports.toArray(new ExportedPackage[allExports.size()]);
	}

	@Override
	public ExportedPackage getExportedPackage(String name) {
		ExportedPackage[] allExports = getExportedPackages(name);
		if (allExports == null)
			return null;
		ExportedPackage result = null;
		for (ExportedPackage allExport : allExports) {
			if (name.equals(allExport.getName())) {
				if (result == null) {
					result = allExport;
				} else {
					Version curVersion = result.getVersion();
					Version newVersion = allExport.getVersion();
					if (newVersion.compareTo(curVersion) >= 0) {
						result = allExport;
					}
				}
			}
		}
		return result;
	}

	@Override
	public ExportedPackage[] getExportedPackages(String name) {
		String filter = "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + (name == null ? "*" : name) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		Map<String, String> directives = Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		Map<String, Boolean> attributes = Collections.singletonMap(Capabilities.SYNTHETIC_REQUIREMENT, Boolean.TRUE);
		Requirement packageReq = ModuleContainer.createRequirement(PackageNamespace.PACKAGE_NAMESPACE, directives,
				attributes);
		Collection<BundleCapability> packageCaps = frameworkWiring.findProviders(packageReq);
		InternalUtils.filterCapabilityPermissions(packageCaps);
		List<ExportedPackage> result = new ArrayList<>();
		for (BundleCapability capability : packageCaps) {
			ModuleWiring wiring = (ModuleWiring) capability.getRevision().getWiring();
			if (wiring != null) {
				Collection<ModuleWiring> wirings = Collections.emptyList();
				if ((capability.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
					// This is a fragment, just get all the host wirings
					List<ModuleWire> hostWires = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
					if (hostWires != null && !hostWires.isEmpty()) {
						wirings = new ArrayList<>(hostWires.size());
						for (ModuleWire hostWire : hostWires) {
							ModuleWiring hostWiring = hostWire.getProviderWiring();
							if (hostWiring != null) {
								wirings.add(hostWiring);
							}
						}
					}
				} else {
					// just a single host wiring
					wirings = Collections.singletonList(wiring);
				}
				for (ModuleWiring moduleWiring : wirings) {
					Object pkgName = capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
					if (pkgName instanceof String && !moduleWiring.isSubstitutedPackage((String) pkgName)) {
						result.add(new ExportedPackageImpl((ModuleCapability) capability, moduleWiring));
					}
				}
			}
		}
		return (result.size() == 0 ? null : result.toArray(new ExportedPackage[result.size()]));
	}

	@Override
	public void refreshPackages(Bundle[] input) {
		frameworkWiring.refreshBundles(input == null ? null : Arrays.asList(input));
	}

	@Override
	public boolean resolveBundles(Bundle[] input) {
		return frameworkWiring.resolveBundles(input == null ? null : Arrays.asList(input));
	}

	@Override
	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		String filter = "(" + BundleNamespace.BUNDLE_NAMESPACE + "=" + (symbolicName == null ? "*" : symbolicName) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ")"; //$NON-NLS-1$
		Map<String, String> directives = Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		Map<String, Boolean> attributes = Collections.singletonMap(Capabilities.SYNTHETIC_REQUIREMENT, Boolean.TRUE);
		Requirement bundleReq = ModuleContainer.createRequirement(BundleNamespace.BUNDLE_NAMESPACE, directives,
				attributes);
		Collection<BundleCapability> bundleCaps = frameworkWiring.findProviders(bundleReq);
		InternalUtils.filterCapabilityPermissions(bundleCaps);
		Collection<RequiredBundle> result = new ArrayList<>();
		for (BundleCapability capability : bundleCaps) {
			ModuleWiring wiring = (ModuleWiring) capability.getRevision().getWiring();
			if (wiring != null) {
				result.add(new RequiredBundleImpl((ModuleCapability) capability, wiring));
			}
		}
		return result.isEmpty() ? null : result.toArray(new RequiredBundle[result.size()]);
	}

	@Override
	public Bundle[] getBundles(String symbolicName, String versionRange) {
		if (symbolicName == null) {
			throw new IllegalArgumentException();
		}
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) {
			// need to alias system.bundle to the implementation BSN
			symbolicName = EquinoxContainer.NAME;
		}
		VersionRange range = versionRange == null ? null : new VersionRange(versionRange);
		String filter = (range != null ? "(&" : "") + "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + symbolicName //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ ")" //$NON-NLS-1$
				+ (range != null ? range.toFilterString(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE) + ")" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		Requirement identityReq = ModuleContainer.createRequirement(IdentityNamespace.IDENTITY_NAMESPACE,
				Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter), Collections.emptyMap());
		Collection<BundleCapability> identityCaps = frameworkWiring.findProviders(identityReq);

		if (identityCaps.isEmpty()) {
			return null;
		}
		List<Bundle> sorted = new ArrayList<>(identityCaps.size());
		for (BundleCapability capability : identityCaps) {
			Bundle b = capability.getRevision().getBundle();
			// a sanity check incase this is an old revision
			if (symbolicName.equals(b.getSymbolicName()) && !sorted.contains(b)) {
				sorted.add(b);
			}
		}
		Collections.sort(sorted, Comparator.comparing(Bundle::getVersion).reversed());

		if (sorted.isEmpty()) {
			return null;
		}

		return sorted.toArray(new Bundle[sorted.size()]);
	}

	@Override
	public Bundle[] getFragments(Bundle bundle) {
		ModuleWiring wiring = getWiring(bundle);
		if (wiring == null) {
			return null;
		}
		List<ModuleWire> hostWires = wiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		if (hostWires == null) {
			// we don't hold locks while checking the graph, just return if no longer valid
			return null;
		}
		Collection<Bundle> fragments = new ArrayList<>(hostWires.size());
		for (ModuleWire wire : hostWires) {
			Bundle fragment = wire.getRequirer().getBundle();
			if (fragment != null) {
				fragments.add(fragment);
			}
		}
		return fragments.isEmpty() ? null : fragments.toArray(new Bundle[fragments.size()]);
	}

	@Override
	public Bundle[] getHosts(Bundle bundle) {
		ModuleWiring wiring = getWiring(bundle);
		if (wiring == null) {
			return null;
		}
		List<ModuleWire> hostWires = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
		if (hostWires == null) {
			// we don't hold locks while checking the graph, just return if no longer valid
			return null;
		}
		Collection<Bundle> hosts = new ArrayList<>(hostWires.size());
		for (ModuleWire wire : hostWires) {
			Bundle host = wire.getProvider().getBundle();
			if (host != null) {
				hosts.add(host);
			}
		}
		return hosts.isEmpty() ? null : hosts.toArray(new Bundle[hosts.size()]);
	}

	private ModuleWiring getWiring(Bundle bundle) {
		BundleRevision current = bundle.adapt(BundleRevision.class);
		if (current == null) {
			return null;
		}
		return (ModuleWiring) current.getWiring();
	}

	@Override
	public Bundle getBundle(final Class<?> clazz) {
		return equinoxContainer.getBundle(clazz);
	}

	@Override
	public int getBundleType(Bundle bundle) {
		BundleRevision current = bundle.adapt(BundleRevision.class);
		if (current == null) {
			return 0;
		}
		return (current.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0 ? PackageAdmin.BUNDLE_TYPE_FRAGMENT : 0;
	}

	public Collection<Bundle> getRemovalPendingBundles() {
		return frameworkWiring.getRemovalPendingBundles();
	}

	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
		return frameworkWiring.getDependencyClosure(bundles);
	}

	static class ExportedPackageImpl implements ExportedPackage {

		private final ModuleCapability packageCapability;
		private final ModuleWiring providerWiring;

		public ExportedPackageImpl(ModuleCapability packageCapability, ModuleWiring providerWiring) {
			this.packageCapability = packageCapability;
			this.providerWiring = providerWiring;
		}

		@Override
		public String getName() {
			return (String) packageCapability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
		}

		@Override
		public Bundle getExportingBundle() {
			if (!providerWiring.isInUse())
				return null;
			return providerWiring.getBundle();
		}

		@Override
		public Bundle[] getImportingBundles() {
			if (!providerWiring.isInUse()) {
				return null;
			}
			Set<Bundle> importing = new HashSet<>();

			String packageName = getName();
			addRequirers(importing, providerWiring, packageName);

			List<ModuleWire> providedPackages = providerWiring
					.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
			if (providedPackages == null) {
				// we don't hold locks while checking the graph, just return if no longer valid
				return null;
			}
			for (ModuleWire packageWire : providedPackages) {
				if (packageCapability.equals(packageWire.getCapability())) {
					importing.add(packageWire.getRequirer().getBundle());
					if (packageWire.getRequirerWiring().isSubstitutedPackage(packageName)) {
						addRequirers(importing, packageWire.getRequirerWiring(), packageName);
					}
				}
			}
			return importing.toArray(new Bundle[importing.size()]);
		}

		private static void addRequirers(Set<Bundle> importing, ModuleWiring wiring, String packageName) {
			List<ModuleWire> requirerWires = wiring.getProvidedModuleWires(BundleNamespace.BUNDLE_NAMESPACE);
			if (requirerWires == null) {
				// we don't hold locks while checking the graph, just return if no longer
				// isInUse
				return;
			}
			for (ModuleWire requireBundleWire : requirerWires) {
				Bundle requirer = requireBundleWire.getRequirer().getBundle();
				if (importing.contains(requirer)) {
					continue;
				}
				importing.add(requirer);

				// if reexported then need to add any requirers of the reexporter
				String reExport = requireBundleWire.getRequirement().getDirectives()
						.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
				ModuleWiring requirerWiring = requireBundleWire.getRequirerWiring();
				if (BundleNamespace.VISIBILITY_REEXPORT.equals(reExport)) {
					addRequirers(importing, requirerWiring, packageName);
				}
				// also need to add any importers of the same package as the wiring exports;
				// case of aggregations
				if (!requirerWiring.equals(wiring)) {
					List<ModuleWire> providedPackages = requirerWiring
							.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
					if (providedPackages != null) {
						for (ModuleWire packageWire : providedPackages) {
							if (packageName.equals(packageWire.getCapability().getAttributes()
									.get(PackageNamespace.PACKAGE_NAMESPACE))) {
								importing.add(packageWire.getRequirer().getBundle());
								if (packageWire.getRequirerWiring().isSubstitutedPackage(packageName)) {
									addRequirers(importing, packageWire.getRequirerWiring(), packageName);
								}
							}
						}
					}
				}
			}
		}

		/**
		 * @deprecated
		 */
		@Override
		public String getSpecificationVersion() {
			return getVersion().toString();
		}

		@Override
		public Version getVersion() {
			Version version = (Version) packageCapability.getAttributes()
					.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			return version == null ? Version.emptyVersion : version;
		}

		@Override
		public boolean isRemovalPending() {
			return !providerWiring.isCurrent();
		}

		@Override
		public String toString() {
			return packageCapability.toString();
		}
	}

	private static class RequiredBundleImpl implements RequiredBundle {
		private final ModuleCapability bundleCapability;
		private final ModuleWiring providerWiring;

		public RequiredBundleImpl(ModuleCapability bundleCapability, ModuleWiring providerWiring) {
			this.bundleCapability = bundleCapability;
			this.providerWiring = providerWiring;
		}

		@Override
		public String getSymbolicName() {
			return (String) bundleCapability.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE);
		}

		@Override
		public Bundle getBundle() {
			if (!providerWiring.isInUse())
				return null;
			return providerWiring.getBundle();
		}

		@Override
		public Bundle[] getRequiringBundles() {
			if (!providerWiring.isInUse()) {
				return null;
			}
			Set<Bundle> requiring = new HashSet<>();

			addRequirers(requiring, providerWiring);

			return requiring.toArray(new Bundle[requiring.size()]);
		}

		private static void addRequirers(Set<Bundle> requiring, ModuleWiring providerWiring) {
			List<ModuleWire> requirerWires = providerWiring.getProvidedModuleWires(BundleNamespace.BUNDLE_NAMESPACE);
			if (requirerWires == null) {
				// we don't hold locks while checking the graph, just return if no longer
				// isInUse
				return;
			}
			for (ModuleWire requireBundleWire : requirerWires) {
				Bundle requirer = requireBundleWire.getRequirer().getBundle();
				if (requiring.contains(requirer)) {
					continue;
				}
				requiring.add(requirer);
				String reExport = requireBundleWire.getRequirement().getDirectives()
						.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
				if (BundleNamespace.VISIBILITY_REEXPORT.equals(reExport)) {
					addRequirers(requiring, requireBundleWire.getRequirerWiring());
				}
			}
		}

		@Override
		public Version getVersion() {
			Version version = (Version) bundleCapability.getAttributes()
					.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			return version == null ? Version.emptyVersion : version;
		}

		@Override
		public boolean isRemovalPending() {
			return !providerWiring.isCurrent();
		}

		@Override
		public String toString() {
			return bundleCapability.toString();
		}
	}
}
