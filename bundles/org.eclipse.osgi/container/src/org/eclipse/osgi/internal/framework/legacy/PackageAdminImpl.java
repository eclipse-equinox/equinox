/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.framework.legacy;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.container.Capabilities;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.*;

@Deprecated
public class PackageAdminImpl implements PackageAdmin {
	private final ModuleContainer container;

	/* 
	 * We need to make sure that the GetBundleAction class loads early to prevent a ClassCircularityError when checking permissions.
	 * See bug 161561
	 */
	static {
		Class<?> c;
		c = GetBundleAction.class;
		c.getName(); // to prevent compiler warnings
	}

	static class GetBundleAction implements PrivilegedAction<Bundle> {
		private Class<?> clazz;
		private PackageAdminImpl impl;

		public GetBundleAction(PackageAdminImpl impl, Class<?> clazz) {
			this.impl = impl;
			this.clazz = clazz;
		}

		public Bundle run() {
			return impl.getBundlePriv(clazz);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param container the container
	 */
	public PackageAdminImpl(ModuleContainer container) {
		this.container = container;
	}

	public ExportedPackage[] getExportedPackages(Bundle bundle) {
		if (bundle == null) {
			return getExportedPackages((String) null);
		}
		Module module = StartLevelImpl.getModule(bundle);
		Collection<ModuleRevision> revisions = module == null ? Collections.<ModuleRevision> emptyList() : module.getRevisions().getModuleRevisions();

		Collection<ExportedPackage> allExports = new ArrayList<ExportedPackage>();
		for (ModuleRevision revision : revisions) {
			ModuleWiring wiring = revision.getWiring();
			if (wiring != null) {
				List<ModuleCapability> providedPackages = wiring.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
				if (providedPackages != null) {
					for (ModuleCapability providedPackage : providedPackages) {
						allExports.add(new ExportedPackageImpl(providedPackage, wiring));
					}
				}
			}
		}
		return allExports.isEmpty() ? null : allExports.toArray(new ExportedPackage[allExports.size()]);
	}

	public ExportedPackage getExportedPackage(String name) {
		ExportedPackage[] allExports = getExportedPackages(name);
		if (allExports == null)
			return null;
		ExportedPackage result = null;
		for (int i = 0; i < allExports.length; i++) {
			if (name.equals(allExports[i].getName())) {
				if (result == null) {
					result = allExports[i];
				} else {
					Version curVersion = result.getVersion();
					Version newVersion = allExports[i].getVersion();
					if (newVersion.compareTo(curVersion) >= 0)
						result = allExports[i];
				}
			}
		}
		return result;
	}

	public ExportedPackage[] getExportedPackages(String name) {
		String filter = "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + (name == null ? "*" : name) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		Map<String, String> directives = Collections.<String, String> singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		Map<String, Boolean> attributes = Collections.singletonMap(Capabilities.SYNTHETIC_REQUIREMENT, Boolean.TRUE);
		Requirement packageReq = ModuleContainer.createRequirement(PackageNamespace.PACKAGE_NAMESPACE, directives, attributes);
		Collection<BundleCapability> packageCaps = container.getFrameworkWiring().findProviders(packageReq);
		InternalUtils.filterCapabilityPermissions(packageCaps);
		List<ExportedPackage> result = new ArrayList<ExportedPackage>();
		for (BundleCapability capability : packageCaps) {
			ModuleWiring wiring = (ModuleWiring) capability.getRevision().getWiring();
			if (wiring != null) {
				Collection<ModuleWiring> wirings = Collections.emptyList();
				if ((capability.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
					// This is a fragment, just get all the host wirings
					List<ModuleWire> hostWires = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
					if (hostWires != null && !hostWires.isEmpty()) {
						wirings = new ArrayList<ModuleWiring>(hostWires.size());
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
					if (!moduleWiring.getSubstitutedNames().contains(capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
						result.add(new ExportedPackageImpl((ModuleCapability) capability, moduleWiring));
					}
				}
			}
		}
		return (result.size() == 0 ? null : result.toArray(new ExportedPackage[result.size()]));
	}

	public void refreshPackages(Bundle[] input) {
		container.getFrameworkWiring().refreshBundles(input == null ? null : Arrays.asList(input));
	}

	public boolean resolveBundles(Bundle[] input) {
		return container.getFrameworkWiring().resolveBundles(input == null ? null : Arrays.asList(input));
	}

	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		String filter = "(" + BundleNamespace.BUNDLE_NAMESPACE + "=" + (symbolicName == null ? "*" : symbolicName) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		Map<String, String> directives = Collections.<String, String> singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		Map<String, Boolean> attributes = Collections.singletonMap(Capabilities.SYNTHETIC_REQUIREMENT, Boolean.TRUE);
		Requirement bundleReq = ModuleContainer.createRequirement(BundleNamespace.BUNDLE_NAMESPACE, directives, attributes);
		Collection<BundleCapability> bundleCaps = container.getFrameworkWiring().findProviders(bundleReq);
		InternalUtils.filterCapabilityPermissions(bundleCaps);
		Collection<RequiredBundle> result = new ArrayList<RequiredBundle>();
		for (BundleCapability capability : bundleCaps) {
			BundleWiring wiring = capability.getRevision().getWiring();
			if (wiring != null) {
				result.add(new RequiredBundleImpl(capability, wiring));
			}
		}
		return result.isEmpty() ? null : result.toArray(new RequiredBundle[result.size()]);
	}

	public Bundle[] getBundles(String symbolicName, String versionRange) {
		if (symbolicName == null) {
			throw new IllegalArgumentException();
		}
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) {
			// need to alias system.bundle to the implementation BSN
			symbolicName = EquinoxContainer.NAME;
		}
		VersionRange range = versionRange == null ? null : new VersionRange(versionRange);
		String filter = (range != null ? "(&" : "") + "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + symbolicName + ")" + (range != null ? range.toFilterString(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE) + ")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		Requirement identityReq = ModuleContainer.createRequirement(IdentityNamespace.IDENTITY_NAMESPACE, Collections.<String, String> singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter), Collections.<String, Object> emptyMap());
		Collection<BundleCapability> identityCaps = container.getFrameworkWiring().findProviders(identityReq);

		if (identityCaps.isEmpty()) {
			return null;
		}
		List<Bundle> sorted = new ArrayList<Bundle>(identityCaps.size());
		for (BundleCapability capability : identityCaps) {
			Bundle b = capability.getRevision().getBundle();
			// a sanity check incase this is an old revision
			if (symbolicName.equals(b.getSymbolicName()) && !sorted.contains(b)) {
				sorted.add(b);
			}
		}
		Collections.sort(sorted, new Comparator<Bundle>() {
			@Override
			public int compare(Bundle b1, Bundle b2) {
				return b2.getVersion().compareTo(b1.getVersion());
			}
		});

		if (sorted.isEmpty()) {
			return null;
		}

		return sorted.toArray(new Bundle[sorted.size()]);
	}

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
		Collection<Bundle> fragments = new ArrayList<Bundle>(hostWires.size());
		for (ModuleWire wire : hostWires) {
			Bundle fragment = wire.getRequirer().getBundle();
			if (fragment != null) {
				fragments.add(fragment);
			}
		}
		return fragments.isEmpty() ? null : fragments.toArray(new Bundle[fragments.size()]);
	}

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
		Collection<Bundle> hosts = new ArrayList<Bundle>(hostWires.size());
		for (ModuleWire wire : hostWires) {
			Bundle host = wire.getProvider().getBundle();
			if (host != null) {
				hosts.add(host);
			}
		}
		return hosts.isEmpty() ? null : hosts.toArray(new Bundle[hosts.size()]);
	}

	private ModuleWiring getWiring(Bundle bundle) {
		Module module = StartLevelImpl.getModule(bundle);
		if (module == null) {
			return null;
		}

		List<ModuleRevision> revisions = module.getRevisions().getModuleRevisions();
		if (revisions.isEmpty()) {
			return null;
		}

		return revisions.get(0).getWiring();
	}

	Bundle getBundlePriv(Class<?> clazz) {
		ClassLoader cl = clazz.getClassLoader();
		if (cl instanceof BundleReference) {
			return ((BundleReference) cl).getBundle();
		}
		if (cl == getClass().getClassLoader()) {
			return container.getModule(0).getBundle();
		}
		return null;
	}

	public Bundle getBundle(final Class clazz) {
		if (System.getSecurityManager() == null)
			return getBundlePriv(clazz);
		return AccessController.doPrivileged(new GetBundleAction(this, clazz));
	}

	public int getBundleType(Bundle bundle) {
		Module module = StartLevelImpl.getModule(bundle);
		if (module == null) {
			return 0;
		}
		List<BundleRevision> revisions = module.getRevisions().getRevisions();
		if (revisions.isEmpty()) {
			return 0;
		}
		return (revisions.get(0).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0 ? PackageAdmin.BUNDLE_TYPE_FRAGMENT : 0;
	}

	public Collection<Bundle> getRemovalPendingBundles() {
		return container.getFrameworkWiring().getRemovalPendingBundles();
	}

	public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
		return container.getFrameworkWiring().getDependencyClosure(bundles);
	}

	static class ExportedPackageImpl implements ExportedPackage {

		private final ModuleCapability packageCapability;
		private final ModuleWiring providerWiring;

		public ExportedPackageImpl(ModuleCapability packageCapability, ModuleWiring providerWiring) {
			this.packageCapability = packageCapability;
			this.providerWiring = providerWiring;
		}

		public String getName() {
			return (String) packageCapability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
		}

		public Bundle getExportingBundle() {
			if (!providerWiring.isInUse())
				return null;
			return providerWiring.getBundle();
		}

		public Bundle[] getImportingBundles() {
			if (!providerWiring.isInUse()) {
				return null;
			}
			Set<Bundle> importing = new HashSet<Bundle>();

			String packageName = getName();
			addRequirers(importing, providerWiring, packageName);

			List<ModuleWire> providedPackages = providerWiring.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
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
				// we don't hold locks while checking the graph, just return if no longer isInUse
				return;
			}
			for (ModuleWire requireBundleWire : requirerWires) {
				Bundle requirer = requireBundleWire.getRequirer().getBundle();
				if (importing.contains(requirer)) {
					continue;
				}
				importing.add(requirer);

				// if reexported then need to add any requirers of the reexporter
				String reExport = requireBundleWire.getRequirement().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
				ModuleWiring requirerWiring = requireBundleWire.getRequirerWiring();
				if (BundleNamespace.VISIBILITY_REEXPORT.equals(reExport)) {
					addRequirers(importing, requirerWiring, packageName);
				}
				// also need to add any importers of the same package as the wiring exports; case of aggregations
				if (!requirerWiring.equals(wiring)) {
					List<ModuleWire> providedPackages = requirerWiring.getProvidedModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
					if (providedPackages != null) {
						for (ModuleWire packageWire : providedPackages) {
							if (packageName.equals(packageWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
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
		public String getSpecificationVersion() {
			return getVersion().toString();
		}

		public Version getVersion() {
			Version version = (Version) packageCapability.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			return version == null ? Version.emptyVersion : version;
		}

		public boolean isRemovalPending() {
			return !providerWiring.isCurrent();
		}

		public String toString() {
			return packageCapability.toString();
		}
	}

	private static class RequiredBundleImpl implements RequiredBundle {
		private final BundleCapability bundleCapability;
		private final BundleWiring providerWiring;

		public RequiredBundleImpl(BundleCapability bundleCapability, BundleWiring providerWiring) {
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
			Set<Bundle> requiring = new HashSet<Bundle>();

			addRequirers(requiring, providerWiring);

			return requiring.toArray(new Bundle[requiring.size()]);
		}

		private static void addRequirers(Set<Bundle> requiring, BundleWiring providerWiring) {
			List<BundleWire> requirerWires = providerWiring.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
			if (requirerWires == null) {
				// we don't hold locks while checking the graph, just return if no longer isInUse
				return;
			}
			for (BundleWire requireBundleWire : requirerWires) {
				Bundle requirer = requireBundleWire.getRequirer().getBundle();
				if (requiring.contains(requirer)) {
					continue;
				}
				requiring.add(requirer);
				String reExport = requireBundleWire.getRequirement().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
				if (BundleNamespace.VISIBILITY_REEXPORT.equals(reExport)) {
					addRequirers(requiring, requireBundleWire.getRequirerWiring());
				}
			}
		}

		@Override
		public Version getVersion() {
			Version version = (Version) bundleCapability.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			return version == null ? Version.emptyVersion : version;
		}

		public boolean isRemovalPending() {
			return !providerWiring.isCurrent();
		}

		public String toString() {
			return bundleCapability.toString();
		}
	}
}
