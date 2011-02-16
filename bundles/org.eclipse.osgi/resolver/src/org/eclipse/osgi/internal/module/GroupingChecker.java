/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;

/*
 * The GroupingChecker checks the 'uses' directive on exported packages for consistency
 */
public class GroupingChecker {
	final PackageRoots nullPackageRoots = new PackageRoots(null);
	// a mapping of bundles to their package roots; keyed by
	// ResolverBundle -> HashMap of packages; keyed by
	// package name -> PackageRoots
	private Map<ResolverBundle, Map<String, PackageRoots>> bundles = new HashMap<ResolverBundle, Map<String, PackageRoots>>();

	/*
	 * This method fully populates a bundles package roots for the purpose of resolving
	 * a dynamic import.  Package roots must be fully populated because we need all the
	 * roots to do proper uses constraint verification on a dynamic import supplier.
	 */
	public void populateRoots(ResolverBundle bundle) {
		if (bundles.containsKey(bundle))
			// only do the full populate the first time (bug 337272)
			return;
		// process all requires
		BundleConstraint[] requires = bundle.getRequires();
		for (int j = 0; j < requires.length; j++) {
			ResolverBundle selectedSupplier = (ResolverBundle) requires[j].getSelectedSupplier();
			if (selectedSupplier != null)
				isConsistentInternal(bundle, selectedSupplier, new ArrayList<ResolverBundle>(1), true, null);
		}
		// process all imports
		// must check resolved imports to get any dynamically resolved imports
		ExportPackageDescription[] imports = bundle.getBundleDescription().getResolvedImports();
		for (ExportPackageDescription importPkg : imports) {
			List<ResolverExport> exports = bundle.getResolver().getResolverExports().get(importPkg.getName());
			for (ResolverExport export : exports) {
				if (export.getExportPackageDescription() == importPkg)
					isConsistentInternal(bundle, export, true, null);
			}
		}
	}

	/*
	 * Re-populates the package roots or an importing bundle with the given export
	 * This is done after wiring a package from a dynamic import (bug 337272)
	 */
	public void populateRoots(ResolverBundle importingBundle, ResolverExport export) {
		Map<String, PackageRoots> packageRoots = bundles.get(importingBundle);
		if (packageRoots != null)
			packageRoots.remove(export.getName());
		PackageRoots roots = getPackageRoots(export.getExporter(), export.getName(), null);
		packageRoots.put(export.getName(), roots);
	}

	/*
	 * Verifies the uses constraint consistency for the requiringBundle with the possible matching bundle.
	 * If an inconsistency is found the export inconsistency is returned; otherwise null is returned
	 */
	public PackageRoots[][] isConsistent(ResolverBundle requiringBundle, ResolverBundle matchingBundle) {
		List<PackageRoots[]> results = isConsistentInternal(requiringBundle, matchingBundle, new ArrayList<ResolverBundle>(1), false, null);
		return results == null ? null : results.toArray(new PackageRoots[results.size()][]);
	}

	private List<PackageRoots[]> isConsistentInternal(ResolverBundle requiringBundle, ResolverBundle matchingBundle, List<ResolverBundle> visited, boolean dynamicImport, List<PackageRoots[]> results) {
		// needed to prevent endless cycles
		if (visited.contains(matchingBundle))
			return results;
		visited.add(matchingBundle);
		// check that the packages exported by the matching bundle are consistent
		ResolverExport[] matchingExports = matchingBundle.getExportPackages();
		for (int i = 0; i < matchingExports.length; i++) {
			ResolverExport matchingExport = matchingExports[i];
			if (matchingExports[i].getSubstitute() != null)
				matchingExport = (ResolverExport) matchingExports[i].getSubstitute();
			results = isConsistentInternal(requiringBundle, matchingExport, dynamicImport, results);
		}
		// check that the packages from reexported bundles are consistent
		BundleConstraint[] supplierRequires = matchingBundle.getRequires();
		for (int j = 0; j < supplierRequires.length; j++) {
			ResolverBundle reexported = (ResolverBundle) supplierRequires[j].getSelectedSupplier();
			if (reexported == null || !((BundleSpecification) supplierRequires[j].getVersionConstraint()).isExported())
				continue;
			results = isConsistentInternal(requiringBundle, reexported, visited, dynamicImport, results);
		}
		return results;
	}

	/*
	 * Verifies the uses constraint consistency for the importingBundle with the possible matching export.
	 * If an inconsistency is found the export returned; otherwise null is returned
	 */
	public PackageRoots[][] isConsistent(ResolverBundle importingBundle, ResolverExport matchingExport) {
		List<PackageRoots[]> results = isConsistentInternal(importingBundle, matchingExport, false, null);
		return results == null ? null : results.toArray(new PackageRoots[results.size()][]);
	}

	public PackageRoots[][] isConsistent(ResolverBundle requiringBundle, GenericCapability matchingCapability) {
		String[] uses = matchingCapability.getUsesDirective();
		if (uses == null)
			return null;
		ArrayList<PackageRoots[]> results = new ArrayList<PackageRoots[]>(0);
		for (String usedPackage : uses) {
			PackageRoots providingRoots = getPackageRoots(matchingCapability.getResolverBundle(), usedPackage, null);
			providingRoots.addConflicts(requiringBundle, usedPackage, null, results);
		}
		return results.size() == 0 ? null : results.toArray(new PackageRoots[results.size()][]);
	}

	/*
	 * Verifies the uses constraint consistency for the importingBundle with the possible dynamic matching export.
	 * If an inconsistency is found the export returned; otherwise null is returned.
	 * Dynamic imports must perform extra checks to ensure that existing wires to package roots are 
	 * consistent with the possible matching dynamic export.
	 */
	public PackageRoots[][] isDynamicConsistent(ResolverBundle importingBundle, ResolverExport matchingExport) {
		List<PackageRoots[]> results = isConsistentInternal(importingBundle, matchingExport, true, null);
		return results == null ? null : results.toArray(new PackageRoots[results.size()][]);
	}

	private List<PackageRoots[]> isConsistentInternal(ResolverBundle importingBundle, ResolverExport matchingExport, boolean dyanamicImport, List<PackageRoots[]> results) {
		PackageRoots exportingRoots = getPackageRoots(matchingExport.getExporter(), matchingExport.getName(), null);
		// check that the exports uses packages are consistent with existing package roots
		results = exportingRoots.isConsistentClassSpace(importingBundle, null, results);
		if (!dyanamicImport)
			return results;
		// for dynamic imports we must check that each existing root is consistent with the possible matching export
		PackageRoots importingRoots = getPackageRoots(importingBundle, matchingExport.getName(), null);
		Map<String, PackageRoots> importingPackages = bundles.get(importingBundle);
		if (importingPackages != null)
			for (Iterator<PackageRoots> allImportingPackages = importingPackages.values().iterator(); allImportingPackages.hasNext();) {
				PackageRoots roots = allImportingPackages.next();
				if (roots != importingRoots)
					results = roots.isConsistentClassSpace(exportingRoots, matchingExport.getExporter(), null, results);
			}
		// We also must check any generic capabilities are consistent
		GenericConstraint[] genericRequires = importingBundle.getGenericRequires();
		for (GenericConstraint constraint : genericRequires) {
			if (!constraint.supplierHasUses())
				continue;
			GenericCapability supplier = (GenericCapability) constraint.getSelectedSupplier();
			String[] uses = supplier.getUsesDirective();
			if (uses != null)
				for (String usedPackage : uses) {
					if (usedPackage.equals(matchingExport.getName())) {
						results = exportingRoots.addConflicts(supplier.getResolverBundle(), usedPackage, null, results);
					}
				}
		}
		return results;
	}

	/*
	 * returns package roots for a specific package name for a specific bundle
	 */
	PackageRoots getPackageRoots(ResolverBundle bundle, String packageName, List<ResolverBundle> visited) {
		Map<String, PackageRoots> packages = bundles.get(bundle);
		if (packages == null) {
			packages = new HashMap<String, PackageRoots>(5);
			bundles.put(bundle, packages);
		}
		PackageRoots packageRoots = packages.get(packageName);
		if (packageRoots == null) {
			packageRoots = createPackageRoots(bundle, packageName, visited == null ? new ArrayList<ResolverBundle>(1) : visited);
			packages.put(packageName, packageRoots);
		}
		return packageRoots != null ? packageRoots : nullPackageRoots;
	}

	private PackageRoots createPackageRoots(ResolverBundle bundle, String packageName, List<ResolverBundle> visited) {
		if (visited.contains(bundle))
			return null;
		visited.add(bundle); // prevent endless cycles
		// check imports
		if (bundle.getBundleDescription().isResolved()) {
			// must check resolved imports to get any dynamically resolved imports 
			ExportPackageDescription[] imports = bundle.getBundleDescription().getResolvedImports();
			for (ExportPackageDescription importPkg : imports) {
				if (importPkg.getExporter() == bundle.getBundleDescription() || !importPkg.getName().equals(packageName))
					continue;
				List<ResolverExport> exports = bundle.getResolver().getResolverExports().get(packageName);
				for (ResolverExport export : exports) {
					if (export.getExportPackageDescription() == importPkg)
						return getPackageRoots(export.getExporter(), packageName, visited);
				}
			}
		} else {
			ResolverImport imported = bundle.getImport(packageName);
			if (imported != null && imported.getSelectedSupplier() != null) {
				// make sure we are not resolved to our own import
				ResolverExport selectedExport = (ResolverExport) imported.getSelectedSupplier();
				if (selectedExport.getExporter() != bundle) {
					// found resolved import; get the roots from the resolved exporter;
					// this is all the roots if the package is imported
					return getPackageRoots(selectedExport.getExporter(), packageName, visited);
				}
			}
		}
		// check if the bundle exports the package
		ResolverExport[] exports = bundle.getExports(packageName);
		List<PackageRoots> roots = new ArrayList<PackageRoots>(0);
		// check roots from required bundles
		BundleConstraint[] requires = bundle.getRequires();
		for (int i = 0; i < requires.length; i++) {
			ResolverBundle supplier = (ResolverBundle) requires[i].getSelectedSupplier();
			if (supplier == null)
				continue; // no supplier, probably optional
			if (supplier.getExport(packageName) != null) {
				// the required bundle exports the package; get the package roots from it
				PackageRoots requiredRoots = getPackageRoots(supplier, packageName, visited);
				if (requiredRoots != nullPackageRoots)
					roots.add(requiredRoots);
			} else {
				// the bundle does not export the package; but it may reexport another bundle that does
				BundleConstraint[] supplierRequires = supplier.getRequires();
				for (int j = 0; j < supplierRequires.length; j++) {
					ResolverBundle reexported = (ResolverBundle) supplierRequires[j].getSelectedSupplier();
					if (reexported == null || !((BundleSpecification) supplierRequires[j].getVersionConstraint()).isExported())
						continue;
					if (reexported.getExport(packageName) != null) {
						// the reexported bundle exports the package; get the package roots from it
						PackageRoots reExportedRoots = getPackageRoots(reexported, packageName, visited);
						if (reExportedRoots != nullPackageRoots)
							roots.add(reExportedRoots);
					}
				}
			}
		}
		if (exports.length > 0 || roots.size() > 1) {
			PackageRoots[] requiredRoots = roots.toArray(new PackageRoots[roots.size()]);
			if (exports.length == 0) {
				PackageRoots superSet = requiredRoots[0];
				for (int i = 1; i < requiredRoots.length; i++) {
					if (requiredRoots[i].superSet(superSet)) {
						superSet = requiredRoots[i];
					} else if (!superSet.superSet(requiredRoots[i])) {
						superSet = null;
						break;
					}
				}
				if (superSet != null)
					return superSet;
			}
			// in this case we cannot share the package roots object; must create one specific for this bundle
			PackageRoots result = new PackageRoots(packageName);
			// first merge all the roots from required bundles
			for (int i = 0; i < requiredRoots.length; i++)
				result.merge(requiredRoots[i]);
			// always add this bundles exports to the end if it exports the package
			for (int i = 0; i < exports.length; i++)
				result.addRoot(exports[i]);
			return result;
		}
		return roots.size() == 0 ? nullPackageRoots : roots.get(0);
	}

	public void clear() {
		bundles.clear();
	}

	public void clear(ResolverBundle rb) {
		bundles.remove(rb);
	}

	class PackageRoots {
		private String name;
		private ResolverExport[] roots;

		PackageRoots(String name) {
			this.name = name;
		}

		public boolean hasRoots() {
			return roots != null && roots.length > 0;
		}

		public void addRoot(ResolverExport export) {
			if (roots == null) {
				roots = new ResolverExport[] {export};
				return;
			}
			// need to do an extra check to make sure we are not adding the same package name 
			// from multiple versions of the same bundle
			String exportBSN = export.getExporter().getName();
			if (exportBSN != null) {
				// first one wins
				for (int i = 0; i < roots.length; i++)
					if (export.getExporter() != roots[i].getExporter() && exportBSN.equals(roots[i].getExporter().getName()))
						return;
			}
			if (!contains(export, roots)) {
				ResolverExport[] newRoots = new ResolverExport[roots.length + 1];
				System.arraycopy(roots, 0, newRoots, 0, roots.length);
				newRoots[roots.length] = export;
				roots = newRoots;
			}
		}

		private boolean contains(ResolverExport export, ResolverExport[] exports) {
			for (int i = 0; i < exports.length; i++)
				if (exports[i] == export)
					return true;
			return false;
		}

		public void merge(PackageRoots packageRoots) {
			if (packageRoots == null || packageRoots.roots == null)
				return;
			int size = packageRoots.roots.length;
			for (int i = 0; i < size; i++)
				addRoot(packageRoots.roots[i]);
		}

		public List<PackageRoots[]> isConsistentClassSpace(ResolverBundle importingBundle, List<PackageRoots> visited, List<PackageRoots[]> results) {
			if (roots == null)
				return results;
			if (visited == null)
				visited = new ArrayList<PackageRoots>(1);
			if (visited.contains(this))
				return results;
			visited.add(this);
			int size = roots.length;
			for (int i = 0; i < size; i++) {
				ResolverExport root = roots[i];
				String[] uses = root.getUsesDirective();
				if (uses == null)
					continue;
				for (int j = 0; j < uses.length; j++) {
					if (uses[j].equals(root.getName()))
						continue;
					PackageRoots thisUsedRoots = getPackageRoots(root.getExporter(), uses[j], null);
					PackageRoots importingUsedRoots = getPackageRoots(importingBundle, uses[j], null);
					if (thisUsedRoots == importingUsedRoots)
						continue;
					if (thisUsedRoots != nullPackageRoots && importingUsedRoots != nullPackageRoots)
						if (!(subSet(thisUsedRoots.roots, importingUsedRoots.roots) || subSet(importingUsedRoots.roots, thisUsedRoots.roots))) {
							if (results == null)
								results = new ArrayList<PackageRoots[]>(1);
							results.add(new PackageRoots[] {this, importingUsedRoots});
						}
					// need to check the usedRoots consistency for transitive closure
					results = thisUsedRoots.isConsistentClassSpace(importingBundle, visited, results);
				}
			}
			return results;
		}

		public List<PackageRoots[]> isConsistentClassSpace(PackageRoots exportingRoots, ResolverBundle exporter, List<PackageRoots> visited, List<PackageRoots[]> results) {
			if (roots == null)
				return results;
			int size = roots.length;
			for (int i = 0; i < size; i++) {
				ResolverExport root = roots[i];
				String[] uses = root.getUsesDirective();
				if (uses == null)
					continue;
				if (visited == null)
					visited = new ArrayList<PackageRoots>(1);
				if (visited.contains(this))
					return results;
				visited.add(this);
				for (int j = 0; j < uses.length; j++) {
					if (uses[j].equals(root.getName()) || !uses[j].equals(exportingRoots.name))
						continue;
					PackageRoots thisUsedRoots = getPackageRoots(root.getExporter(), uses[j], null);
					PackageRoots exportingUsedRoots = getPackageRoots(exporter, uses[j], null);
					if (thisUsedRoots == exportingRoots)
						return results;
					if (thisUsedRoots != nullPackageRoots && exportingUsedRoots != nullPackageRoots)
						if (!(subSet(thisUsedRoots.roots, exportingUsedRoots.roots) || subSet(exportingUsedRoots.roots, thisUsedRoots.roots))) {
							if (results == null)
								results = new ArrayList<PackageRoots[]>(1);
							results.add(new PackageRoots[] {this, exportingUsedRoots});
						}
					// need to check the usedRoots consistency for transitive closure
					results = thisUsedRoots.isConsistentClassSpace(exportingRoots, exporter, visited, results);
				}
			}
			return results;
		}

		List<PackageRoots[]> addConflicts(ResolverBundle bundle, String usedPackage, List<PackageRoots> visited, List<PackageRoots[]> results) {
			PackageRoots bundleUsedRoots = getPackageRoots(bundle, usedPackage, null);
			if (this == bundleUsedRoots)
				return results;
			if (this != nullPackageRoots && bundleUsedRoots != nullPackageRoots)
				if (!(subSet(this.roots, bundleUsedRoots.roots) || subSet(bundleUsedRoots.roots, this.roots))) {
					if (results == null)
						results = new ArrayList<PackageRoots[]>(1);
					results.add(new PackageRoots[] {this, bundleUsedRoots});
				}
			// need to check the usedRoots consistency for transitive closure
			return this.isConsistentClassSpace(bundleUsedRoots, bundle, visited, results);
		}

		// TODO this is a behavioral change; before we only required 1 supplier to match; now roots must be subsets
		private boolean subSet(ResolverExport[] superSet, ResolverExport[] subSet) {
			for (int i = 0; i < subSet.length; i++) {
				boolean found = false;
				for (int j = 0; j < superSet.length; j++)
					// compare by exporter in case the bundle exports the package multiple times
					if (subSet[i].getExporter() == superSet[j].getExporter()) {
						found = true;
						break;
					}
				if (!found)
					return false;
			}
			return true;
		}

		public boolean superSet(PackageRoots subSet) {
			return subSet(roots, subSet.roots);
		}

		public String getName() {
			return name;
		}

		public ResolverExport[] getRoots() {
			return roots;
		}
	}
}
