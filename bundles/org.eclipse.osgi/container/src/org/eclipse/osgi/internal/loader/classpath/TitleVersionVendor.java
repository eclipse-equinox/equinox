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
 * Stores a (title,version,vendor) triple for Specification-* or Implementation-*
 */
class TitleVersionVendor {
	/**
	 * Constant value when no title, version or vendor are specified for the package.
	 */
	static final TitleVersionVendor NONE = new TitleVersionVendor(null, null, null);
	private final String title;
	private final String version;
	private final String vendor;

	/**
	 * Factory for creating TitleVersionVendor objects.  If the given title, version and
	 * vendor are <code>null</code> then {@link #NONE} is returned.
	 * @param title
	 * @param version
	 * @param vendor
	 * @return
	 */
	static TitleVersionVendor of(String title, String version, String vendor) {
		if (title == null && version == null && vendor == null) {
			return NONE;
		}
		return new TitleVersionVendor(title, version, vendor);
	}

	private TitleVersionVendor(String title, String version, String vendor) {
		this.title = title;
		this.version = version;
		this.vendor = vendor;
	}

	/**
	 * Returns the title
	 * @return the title
	 */
	String getTitle() {
		return title;
	}

	/**
	 * returns the version
	 * @return the version
	 */
	String getVersion() {
		return version;
	}

	/**
	 * returns the vendor
	 * @return the vendor
	 */
	String getVendor() {
		return vendor;
	}
}
