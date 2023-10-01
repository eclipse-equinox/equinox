/******************************************************************************
 * Copyright (c) 2016 Alex Blewitt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.loader.classpath;

/**
 * Stores a pair of {@link TitleVersionVendor} package attributes for the
 * Implementation- and Specification- entries.
 */
class ManifestPackageAttributes {
	/**
	 * Constant used when no titles, versions or vendors are specified for the
	 * package.
	 */
	static final ManifestPackageAttributes NONE = new ManifestPackageAttributes(TitleVersionVendor.NONE,
			TitleVersionVendor.NONE);
	private final TitleVersionVendor implementation;
	private final TitleVersionVendor specification;

	/**
	 * Utility method to return the first version, or the second if it is null
	 * 
	 * @param first  the string to return if not null
	 * @param second the fallback value if the first is null
	 * @return the first value, or the second value if the first is null
	 */
	private static String or(String first, String second) {
		return first == null ? second : first;
	}

	/**
	 * Factory method for creating ManifestPackageAttributes. If any of the given
	 * title, version, or vendor values are <code>null</code> then the
	 * defaultAttributes will be used. If the defaultAttributes is null or returns
	 * all <code>null</code> values then <code>null</code> values will be used. If
	 * <code>null</code> values are used for all of the versions, titles, and
	 * vendors then {@link #NONE} is returned.
	 * 
	 * @param specificationTitle    the package specification title
	 * @param specificationVersion  the package specification version
	 * @param specificationVendor   the package specification vendor
	 * @param implementationTitle   the package implementation title
	 * @param implementationVersion the package implementation version
	 * @param implementationVendor  the package implementation vendor
	 * @param defaultAttributes     the default attributes to use when the specified
	 *                              title, version or vendor is <code>null</code>.
	 */
	static ManifestPackageAttributes of(String specificationTitle, String specificationVersion,
			String specificationVendor, String implementationTitle, String implementationVersion,
			String implementationVendor, ManifestPackageAttributes defaultAttributes) {
		if (defaultAttributes == null) {
			defaultAttributes = NONE;
		}
		return of(//
				or(specificationTitle, defaultAttributes.getSpecification().getTitle()), //
				or(specificationVersion, defaultAttributes.getSpecification().getVersion()), //
				or(specificationVendor, defaultAttributes.getSpecification().getVendor()), //
				or(implementationTitle, defaultAttributes.getImplementation().getTitle()), //
				or(implementationVersion, defaultAttributes.getImplementation().getVersion()), //
				or(implementationVendor, defaultAttributes.getImplementation().getVendor())//
		);
	}

	private static ManifestPackageAttributes of(String specificationTitle, String specificationVersion,
			String specificationVendor, String implementationTitle, String implementationVersion,
			String implementationVendor) {
		TitleVersionVendor specification = TitleVersionVendor.of(specificationTitle, specificationVersion,
				specificationVendor);
		TitleVersionVendor implementation = TitleVersionVendor.of(implementationTitle, implementationVersion,
				implementationVendor);
		if (specification == TitleVersionVendor.NONE && implementation == TitleVersionVendor.NONE) {
			return NONE;
		}
		return new ManifestPackageAttributes(implementation, specification);
	}

	private ManifestPackageAttributes(TitleVersionVendor implementation, TitleVersionVendor specification) {
		if (implementation == null || specification == null) {
			throw new IllegalArgumentException();
		}
		this.implementation = implementation;
		this.specification = specification;
	}

	/**
	 * Returns the title, version and vendor for the package implementation.
	 * 
	 * @return the title, version and vendor for the package implemetnation.
	 */
	TitleVersionVendor getImplementation() {
		return implementation;
	}

	/**
	 * Returns the title, version and vendor for the package specification.
	 * 
	 * @return the title, version and vendor for the package specification.
	 */
	TitleVersionVendor getSpecification() {
		return specification;
	}
}
