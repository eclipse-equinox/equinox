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
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * This class builds bundle description objects from manifests
 */
class StateBuilder {
	static BundleDescription createBundleDescription(Dictionary manifest, String location) throws BundleException {
		BundleDescriptionImpl result = new BundleDescriptionImpl();
		result.setUniqueId((String) manifest.get(Constants.BUNDLE_GLOBALNAME));
		if (result.getUniqueId() == null)
			result.setUniqueId((String) manifest.get(Constants.BUNDLE_NAME));
		String version = (String) manifest.get(Constants.BUNDLE_VERSION);
		result.setVersion((version != null) ? new Version(version) : Version.EMPTY_VERSION);
		result.setLocation(location);
		ManifestElement[] host = ManifestElement.parseBundleDescriptions((String) manifest.get(Constants.HOST_BUNDLE));
		if (host != null)
			result.setHost(createHostSpecification(host[0]));
		ManifestElement[] imports = ManifestElement.parsePackageDescription((String) manifest.get(Constants.IMPORT_PACKAGE));
		ManifestElement[] exports = ManifestElement.parsePackageDescription((String) manifest.get(Constants.EXPORT_PACKAGE));
		result.setPackages(createPackages(exports, imports));
		ManifestElement[] provides = ManifestElement.parsePackageDescription((String) manifest.get(Constants.PROVIDE_PACKAGE));
		result.setProvidedPackages(createProvidedPackages(provides));
		ManifestElement[] requires = ManifestElement.parseBundleDescriptions((String) manifest.get(Constants.REQUIRE_BUNDLE));
		result.setRequiredBundles(createRequiredBundles(requires));
		return result;
	}
	private static BundleSpecification[] createRequiredBundles(ManifestElement[] specs) {
		if (specs == null)
			return null;
		BundleSpecification[] result = new BundleSpecification[specs.length];
		for (int i = 0; i < specs.length; i++)
			result[i] = createRequiredBundle(specs[i]);
		return result;
	}
	private static BundleSpecification createRequiredBundle(ManifestElement spec) {
		BundleSpecificationImpl result = new BundleSpecificationImpl();
		result.setName(spec.getValue());
		String version = spec.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (version != null)
			result.setVersionSpecification(new Version(version));
		result.setMatchingRule(parseMatchingRule(spec.getAttribute(Constants.VERSION_MATCH_ATTRIBUTE)));
		result.setExported(spec.getAttribute(Constants.PROVIDE_PACKAGES_ATTRIBUTE) != null);
		result.setOptional(spec.getAttribute(Constants.OPTIONAL_ATTRIBUTE) != null);
		return result;
	}
	private static byte parseMatchingRule(String match) {
		if (match == null)
			return VersionConstraint.GREATER_EQUAL_MATCH;
		if (match.equals(Constants.VERSION_MATCH_EQUIVALENT))
			return VersionConstraint.EQUIVALENT_MATCH;
		if (match.equals(Constants.VERSION_MATCH_COMPATIBLE))
			return VersionConstraint.COMPATIBLE_MATCH;
		if (match.equals(Constants.VERSION_MATCH_PERFECT))
			return VersionConstraint.PERFECT_MATCH;
		return VersionConstraint.GREATER_EQUAL_MATCH;
	}
	private static String[] createProvidedPackages(ManifestElement[] specs) {
		if (specs == null || specs.length == 0)
			return null;
		String[] result = new String[specs.length];
		for (int i = 0; i < specs.length; i++)
			result[i] = specs[i].getValue();
		return result;
	}
	private static PackageSpecification[] createPackages(ManifestElement[] exported, ManifestElement[] imported) {
		int capacity = (exported == null ? 0 : exported.length) + (imported == null ? 0 : imported.length);
		if (capacity == 0)
			return null;
		Map packages = new HashMap(capacity);
		if (imported != null)
			for (int i = 0; i < imported.length; i++)
				packages.put(imported[i].getValue(), createPackage(imported[i], false));
		if (exported != null)
			for (int i = 0; i < exported.length; i++)
				packages.put(exported[i].getValue(), createPackage(exported[i], true));
		return (PackageSpecification[]) packages.values().toArray(new PackageSpecification[packages.size()]);
	}
	private static PackageSpecification createPackage(ManifestElement spec, boolean export) {
		PackageSpecificationImpl result = new PackageSpecificationImpl();
		result.setName(spec.getValue());
		String version = spec.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
		if (version != null)
			result.setVersionSpecification(new Version(version));
		result.setExport(export);
		return result;
	}
	private static HostSpecification createHostSpecification(ManifestElement spec) {
		if (spec == null)
			return null;
		HostSpecificationImpl result = new HostSpecificationImpl();
		result.setName(spec.getValue());
		String version = spec.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (version != null)
			result.setVersionSpecification(new Version(version));
		result.setMatchingRule(parseMatchingRule(spec.getAttribute(Constants.VERSION_MATCH_ATTRIBUTE)));
		result.setReloadHost(false); //$NON-NLS-1$
		return result;
	}
}