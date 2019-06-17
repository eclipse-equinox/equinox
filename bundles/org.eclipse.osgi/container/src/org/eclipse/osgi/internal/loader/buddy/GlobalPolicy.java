/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.internal.loader.buddy;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Global policy is an implementation of a buddy policy. It is responsible
 * for looking up a class within the global set of exported classes. If multiple
 * version of the same package are exported in the system, the exported package
 * with the highest version will be returned.
 */
@SuppressWarnings("deprecation")
public class GlobalPolicy implements IBuddyPolicy {
	private PackageAdmin admin;

	public GlobalPolicy(PackageAdmin admin) {
		this.admin = admin;
	}

	@Override
	public Class<?> loadClass(String name) {
		ExportedPackage pkg = admin.getExportedPackage(BundleLoader.getPackageName(name));
		if (pkg == null)
			return null;
		try {
			return pkg.getExportingBundle().loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public URL loadResource(String name) {
		//get all exported packages that match the resource's package
		ExportedPackage pkg = admin.getExportedPackage(BundleLoader.getResourcePackageName(name));
		if (pkg == null)
			return null;
		return pkg.getExportingBundle().getResource(name);
	}

	@Override
	public Enumeration<URL> loadResources(String name) {
		//get all exported packages that match the resource's package
		ExportedPackage[] pkgs = admin.getExportedPackages(BundleLoader.getResourcePackageName(name));
		if (pkgs == null || pkgs.length == 0)
			return null;

		//get all matching resources for each package
		Enumeration<URL> results = null;
		for (ExportedPackage pkg : pkgs) {
			try {
				results = BundleLoader.compoundEnumerations(results, pkg.getExportingBundle().getResources(name));
			}catch (IOException e) {
				//ignore IO problems and try next package
			}
		}

		return results;
	}
}
