/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.util.ArrayList;
import org.eclipse.osgi.service.resolver.PackageSpecification;
import org.eclipse.osgi.service.resolver.Version;
import org.osgi.framework.Constants;

public class ExportedPackageImpl extends SingleSourcePackage implements org.osgi.service.packageadmin.ExportedPackage {

	String specVersion;
	PackageSpecification packageSpec;	//TODO Useless

	public ExportedPackageImpl(PackageSpecification packageSpec, BundleLoaderProxy supplier) {
		super(packageSpec.getName(), supplier);
		this.packageSpec = packageSpec;
		Version version = this.packageSpec.getActualVersion();
		if (version != null) {
			this.specVersion = version.toString();
		}
	}

	public String getName() {
		return getId();
	}

	public org.osgi.framework.Bundle getExportingBundle() {
		if (supplier.isStale()) {
			return null;
		}
		return supplier.getBundle();
	}

	public org.osgi.framework.Bundle[] getImportingBundles() {
		if (supplier.isStale()) {
			return null;
		}

		Bundle[] dependentBundles = supplier.getDependentBundles();
		ArrayList importingBundles = new ArrayList();

		// always add self
		importingBundles.add(supplier.getBundle());

		for (int i = 0; i < dependentBundles.length; i++) {
			Bundle bundle = dependentBundles[i];
			BundleLoader bundleLoader = bundle.getBundleLoader();
			/* check to make sure this package is really imported;
			 * do not call bundleLoader.getPackageExporter() here because we do
			 * not want to cause the bundle to dynamically import any packages
			 * that may not have been referenced yet.
			 */
			if (bundleLoader.importedPackages != null && bundleLoader.importedPackages.getByKey(getId()) != null) {
				importingBundles.add(bundle);
			}
		}

		Bundle[] result = new Bundle[importingBundles.size()];
		importingBundles.toArray(result);
		return result;
	}

	public String getSpecificationVersion() {
		return specVersion;
	}

	public boolean isRemovalPending() {
		Bundle bundle = supplier.getBundle();
		return bundle.framework.packageAdmin.removalPending.contains(supplier);
	}

	public String toString() {
		StringBuffer result = new StringBuffer(getId());
		if (specVersion != null) {
			result.append("; ").append(Constants.PACKAGE_SPECIFICATION_VERSION);
			result.append("=\"").append(specVersion).append("\"");
		}
		return result.toString();
	}
}
