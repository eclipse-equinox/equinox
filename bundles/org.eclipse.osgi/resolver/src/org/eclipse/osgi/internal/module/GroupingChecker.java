/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleSpecification;

/*
 * The GroupingChecker checks the 'uses' directive on exported packages for consistency
 */
public class GroupingChecker {
	final PackageRoots nullPackageRoots = new PackageRoots(null, null);
	// a mapping of bundles to their package roots; keyed by
	// ResolverBundle -> HashMap of packages; keyed by
	// package name -> PackageRoots[]
	private HashMap bundles = new HashMap();

	/*
	 * This method fully populates a bundles package roots for the purpose of resolving
	 * a dynamic import.  Package roots must be fully populated because we need all the
	 * roots to do proper uses constraint verification on a dynamic import supplier.
	 */
	public void populateRoots(ResolverBundle bundle) {
		bundles.remove(bundle);
		// process all requires
		BundleConstraint[] requires = bundle.getRequires();
		for (int j = 0; j < requires.length; j++) {
			ResolverBundle selectedSupplier = (ResolverBundle) requires[j].getSelectedSupplier();
			if (selectedSupplier != null)
				isConsistentInternal(bundle, selectedSupplier, new ArrayList(1), true);
		}
		// process all imports
		ResolverImport[] imports = bundle.getImportPackages();
		for (int j = 0; j < imports.length; j++) {
			ResolverExport selectedSupplier = (ResolverExport) imports[j].getSelectedSupplier();
			if (selectedSupplier != null)
				isConsistentInternal(bundle, selectedSupplier, true);
		}
	}

	/*
	 * Verifies the uses constraint consistency for the requiringBundle with the possible matching bundle.
	 * If an inconsistency is found the export inconsistency is returned; otherwise null is returned
	 */
	public ResolverExport isConsistent(ResolverBundle requiringBundle, ResolverBundle matchingBundle) {
		ResolverExport inConsistentExport = isConsistentInternal(requiringBundle, matchingBundle, new ArrayList(1), false);
		if (inConsistentExport != null)
			return inConsistentExport;
		return null;
	}

	private ResolverExport isConsistentInternal(ResolverBundle requiringBundle, ResolverBundle matchingBundle, ArrayList visited, boolean dynamicImport) {
		// needed to prevent endless cycles
		if (visited.contains(matchingBundle))
			return null;
		visited.add(matchingBundle);
		// check that the packages exported by the matching bundle are consistent
		ResolverExport[] matchingExports = matchingBundle.getExportPackages();
		for (int i = 0; i < matchingExports.length; i++) {
			if (matchingExports[i].isDropped())
				continue;
			if (!isConsistentInternal(requiringBundle, matchingExports[i], dynamicImport))
				return matchingExports[i];
		}
		// check that the packages from reexported bundles are consistent
		BundleConstraint[] supplierRequires = matchingBundle.getRequires();
		for (int j = 0; j < supplierRequires.length; j++) {
			ResolverBundle reexported = (ResolverBundle) supplierRequires[j].getSelectedSupplier();
			if (reexported == null || !((BundleSpecification) supplierRequires[j].getVersionConstraint()).isExported())
				continue;
			ResolverExport inConsistentExport = isConsistentInternal(requiringBundle, reexported, visited, dynamicImport);
			if (inConsistentExport != null)
				return inConsistentExport;
		}
		return null;
	}

	/*
	 * Verifies the uses constraint consistency for the importingBundle with the possible matching export.
	 * If an inconsistency is found the export returned; otherwise null is returned
	 */
	public ResolverExport isConsistent(ResolverBundle importingBundle, ResolverExport matchingExport) {
		if (!isConsistentInternal(importingBundle, matchingExport, false))
			return matchingExport;
		return null;
	}

	/*
	 * Verifies the uses constraint consistency for the importingBundle with the possible dynamioc matching export.
	 * If an inconsistency is found the export returned; otherwise null is returned.
	 * Dynamic imports must perform extra checks to ensure that existing wires to package roots are 
	 * consistent with the possible matching dynamic export.
	 */
	public ResolverExport isDynamicConsistent(ResolverBundle importingBundle, ResolverExport matchingExport) {
		if (!isConsistentInternal(importingBundle, matchingExport, true))
			return matchingExport;
		return null;
	}

	private boolean isConsistentInternal(ResolverBundle importingBundle, ResolverExport matchingExport, boolean dyanamicImport) {
		PackageRoots exportingRoots = getPackageRoots(matchingExport.getExporter(), matchingExport.getName(), null);
		// check that the exports uses packages are consistent with existing package roots
		if (!exportingRoots.isConsistentClassSpace(importingBundle, null))
			return false;
		if (!dyanamicImport)
			return true;
		// for dynamic imports we must check that each existing root is consistent with the possible matching export
		PackageRoots importingRoots = getPackageRoots(importingBundle, matchingExport.getName(), null);
		HashMap importingPackages = (HashMap) bundles.get(importingBundle);
		if (importingPackages != null)
			for (Iterator allImportingPackages = importingPackages.values().iterator(); allImportingPackages.hasNext();) {
				PackageRoots roots = (PackageRoots) allImportingPackages.next();
				if (roots != importingRoots && !roots.isConsistentClassSpace(exportingRoots, null))
					return false;
			}
		return true;
	}

	/*
	 * returns package roots for a specific package name for a specific bundle
	 */
	PackageRoots getPackageRoots(ResolverBundle bundle, String packageName, ArrayList visited) {
		HashMap packages = (HashMap) bundles.get(bundle);
		if (packages == null) {
			packages = new HashMap(5);
			bundles.put(bundle, packages);
		}
		PackageRoots packageRoots = (PackageRoots) packages.get(packageName);
		if (packageRoots == null) {
			packageRoots = createPackageRoots(bundle, packageName, visited == null ? new ArrayList(1) : visited);
			packages.put(packageName, packageRoots);
		}
		return packageRoots != null ? packageRoots : nullPackageRoots;
	}

	private PackageRoots createPackageRoots(ResolverBundle bundle, String packageName, ArrayList visited) {
		if (visited.contains(bundle))
			return null;
		visited.add(bundle); // prevent endless cycles
		// check imports
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
		// check if the bundle exports the package
		ResolverExport export = bundle.getExport(packageName);
		ArrayList roots = new ArrayList(0);
		// check roots from required bundles
		BundleConstraint[] requires = bundle.getRequires();
		for (int i = 0; i < requires.length; i++) {
			ResolverBundle supplier = (ResolverBundle) requires[i].getSelectedSupplier();
			if (supplier == null)
				continue; // no supplier, probably optional
			if (supplier.getExport(packageName) != null) {
				// the required bundle exports the package; get the package roots from it
				PackageRoots requiredRoots = getPackageRoots(supplier, packageName, visited);
				if (requiredRoots != null)
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
						if (reexported != null)
							roots.add(reExportedRoots);
					}
				}
			}
		}
		if (export != null || roots.size() > 1) {
			PackageRoots[] requiredRoots = (PackageRoots[]) roots.toArray(new PackageRoots[roots.size()]);
			if (export == null) {
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
			PackageRoots result = new PackageRoots(packageName, bundle);
			// first merge all the roots from required bundles
			for (int i = 0; i < requiredRoots.length; i++)
				result.merge(requiredRoots[i]);
			if (export != null)
				// always add this bundles export to the end if it exports the package
				result.addRoot(export);
			return result;
		}
		return (PackageRoots) (roots.size() == 0 ? nullPackageRoots : roots.get(0));
	}

	public void clear() {
		bundles.clear();
	}

	public void remove(ResolverBundle rb) {
		bundles.remove(rb);
	}

	private class PackageRoots {
		private String name;
		private ResolverBundle bundle;
		private ResolverExport[] roots;

		PackageRoots(String name, ResolverBundle bundle) {
			this.name = name;
			this.bundle = bundle;
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
					if (exportBSN.equals(roots[i].getExporter().getName()))
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

		public boolean isConsistentClassSpace(ResolverBundle importingBundle, ArrayList visited) {
			if (roots == null)
				return true;
			int size = roots.length;
			for (int i = 0; i < size; i++) {
				ResolverExport root = roots[i];
				String[] uses = root.getUsesDirective();
				if (uses == null)
					continue;
				if (visited == null)
					visited = new ArrayList(1);
				if (visited.contains(this))
					return true;
				visited.add(this);
				for (int j = 0; j < uses.length; j++) {
					if (uses[j].equals(root.getName()))
						continue;
					PackageRoots thisUsedRoots = getPackageRoots(root.getExporter(), uses[j], null);
					PackageRoots importingUsedRoots = getPackageRoots(importingBundle, uses[j], null);
					if (thisUsedRoots == importingUsedRoots)
						return true;
					if (thisUsedRoots != nullPackageRoots && importingUsedRoots != nullPackageRoots)
						if (!(subSet(thisUsedRoots.roots, importingUsedRoots.roots) || subSet(importingUsedRoots.roots, thisUsedRoots.roots)))
							return false;
					// need to check the usedRoots consistency for transitive closure
					if (!thisUsedRoots.isConsistentClassSpace(importingBundle, visited))
						return false;
				}
			}
			return true;
		}

		public boolean isConsistentClassSpace(PackageRoots exportingRoots, ArrayList visited) {
			if (roots == null)
				return true;
			int size = roots.length;
			for (int i = 0; i < size; i++) {
				ResolverExport root = roots[i];
				String[] uses = root.getUsesDirective();
				if (uses == null)
					continue;
				if (visited == null)
					visited = new ArrayList(1);
				if (visited.contains(this))
					return true;
				visited.add(this);
				for (int j = 0; j < uses.length; j++) {
					if (uses[j].equals(root.getName()) || !uses[j].equals(exportingRoots.name))
						continue;
					PackageRoots thisUsedRoots = getPackageRoots(root.getExporter(), uses[j], null);
					PackageRoots exportingUsedRoots = getPackageRoots(exportingRoots.bundle, uses[j], null);
					if (thisUsedRoots == exportingRoots)
						return true;
					if (thisUsedRoots != nullPackageRoots && exportingUsedRoots != nullPackageRoots)
						if (!(subSet(thisUsedRoots.roots, exportingUsedRoots.roots) || subSet(exportingUsedRoots.roots, thisUsedRoots.roots)))
							return false;
					// need to check the usedRoots consistency for transitive closure
					if (!thisUsedRoots.isConsistentClassSpace(exportingRoots, visited))
						return false;
				}
			}
			return true;
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
	}
}
