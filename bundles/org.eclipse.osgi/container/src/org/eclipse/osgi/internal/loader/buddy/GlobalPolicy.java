/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class GlobalPolicy implements IBuddyPolicy {
	private PackageAdmin admin;

	public GlobalPolicy(PackageAdmin admin) {
		this.admin = admin;
	}

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

	public URL loadResource(String name) {
		//get all exported packages that match the resource's package
		ExportedPackage pkg = admin.getExportedPackage(BundleLoader.getResourcePackageName(name));
		if (pkg == null)
			return null;
		return pkg.getExportingBundle().getResource(name);
	}

	public Enumeration<URL> loadResources(String name) {
		//get all exported packages that match the resource's package
		ExportedPackage[] pkgs = admin.getExportedPackages(BundleLoader.getResourcePackageName(name));
		if (pkgs == null || pkgs.length == 0)
			return null;

		//get all matching resources for each package
		Enumeration<URL> results = null;
		for (int i = 0; i < pkgs.length; i++) {
			try {
				results = BundleLoader.compoundEnumerations(results, pkgs[i].getExportingBundle().getResources(name));
			} catch (IOException e) {
				//ignore IO problems and try next package
			}
		}

		return results;
	}
}
