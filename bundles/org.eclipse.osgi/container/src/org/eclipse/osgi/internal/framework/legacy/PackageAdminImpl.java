/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
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
		Collection<ModuleRevision> revisions;
		if (bundle != null) {
			Module module = StartLevelImpl.getModule(bundle);
			revisions = module == null ? Collections.<ModuleRevision> emptyList() : module.getRevisions().getModuleRevisions();
		} else {
			revisions = new HashSet<ModuleRevision>();
			for (Module module : container.getModules()) {
				revisions.addAll(module.getRevisions().getModuleRevisions());
			}
			for (ModuleRevision revision : container.getRemovalPending()) {
				revisions.add(revision);
			}
		}

		Collection<ExportedPackage> allExports = new ArrayList<ExportedPackage>();
		for (ModuleRevision revision : revisions) {
			ModuleWiring wiring = revision.getWiring();
			if (wiring != null) {
				List<ModuleCapability> providedPackages = wiring.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
				for (ModuleCapability providedPackage : providedPackages) {
					allExports.add(new ExportedPackageImpl(providedPackage, wiring));
				}
			}
		}
		return allExports.isEmpty() ? null : allExports.toArray(new ExportedPackage[allExports.size()]);
	}

	public ExportedPackage getExportedPackage(String name) {
		ExportedPackage[] allExports = getExportedPackages((Bundle) null);
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
		ExportedPackage[] allExports = getExportedPackages((Bundle) null);
		if (allExports == null)
			return null;
		List<ExportedPackage> result = new ArrayList<ExportedPackage>(1); // rare to have more than one
		for (int i = 0; i < allExports.length; i++)
			if (name.equals(allExports[i].getName()))
				result.add(allExports[i]);
		return (result.size() == 0 ? null : result.toArray(new ExportedPackage[result.size()]));
	}

	public void refreshPackages(Bundle[] input) {
		container.getFrameworkWiring().refreshBundles(input == null ? null : Arrays.asList(input));
	}

	public boolean resolveBundles(Bundle[] input) {
		return container.getFrameworkWiring().resolveBundles(input == null ? null : Arrays.asList(input));
	}

	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		// TODO need to handle null symbolicName here.
		Collection<ModuleRevision> revisions = container.getRevisions(symbolicName, null);
		Collection<RequiredBundle> result = new ArrayList<RequiredBundle>();
		for (ModuleRevision revision : revisions) {
			ModuleWiring wiring = revision.getWiring();
			if (wiring != null) {
				List<ModuleCapability> bundleCapabilities = wiring.getModuleCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
				for (ModuleCapability bundleCapability : bundleCapabilities) {
					result.add(new RequiredBundleImpl(bundleCapability, wiring));
				}
			}
		}
		return result.isEmpty() ? null : result.toArray(new RequiredBundle[result.size()]);
	}

	public Bundle[] getBundles(String symbolicName, String versionRange) {
		if (symbolicName == null) {
			throw new IllegalArgumentException();
		}
		Collection<ModuleRevision> revisions = container.getRevisions(symbolicName, null);
		if (revisions.isEmpty()) {
			return null;
		}
		List<ModuleRevision> sorted = new LinkedList<ModuleRevision>(revisions);
		Collections.sort(sorted, new Comparator<ModuleRevision>() {
			@Override
			public int compare(ModuleRevision m1, ModuleRevision m2) {
				return m2.getVersion().compareTo(m1.getVersion());
			}
		});
		if (versionRange != null) {
			VersionRange range = new VersionRange(versionRange);
			for (Iterator<ModuleRevision> iSorted = sorted.iterator(); iSorted.hasNext();) {
				if (!range.includes(iSorted.next().getVersion())) {
					iSorted.remove();
				}
			}
		}

		if (sorted.isEmpty()) {
			return null;
		}

		// This code depends on the array of bundles being in descending
		// version order.
		Bundle[] result = new Bundle[sorted.size()];
		int i = 0;
		for (ModuleRevision revision : sorted) {
			result[i] = revision.getBundle();
			i++;
		}

		return result;
	}

	public Bundle[] getFragments(Bundle bundle) {
		ModuleWiring wiring = getWiring(bundle);
		if (wiring == null) {
			return null;
		}
		List<ModuleWire> hostWires = wiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
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

	public Bundle getBundle(@SuppressWarnings("rawtypes") final Class clazz) {
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
			Set<Bundle> requireing = new HashSet<Bundle>();

			addRequirers(requireing, providerWiring);

			return requireing.toArray(new Bundle[requireing.size()]);
		}

		private static void addRequirers(Set<Bundle> requiring, BundleWiring providerWiring) {
			List<BundleWire> requirerWires = providerWiring.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
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
