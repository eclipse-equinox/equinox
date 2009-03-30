/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import org.osgi.framework.Version;

/**
 * This class represents a version range.
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class VersionRange {
	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	/**
	 * An empty version range: "0.0.0".  The empty version range includes all valid versions
	 * (any version greater than or equal to the version 0.0.0).
	 */
	public static final VersionRange emptyRange = new VersionRange(null);

	private final Version minVersion;
	private final boolean includeMin;
	private final Version maxVersion;
	private final boolean includeMax;

	/**
	 * Constructs a VersionRange with the specified minVersion and maxVersion.
	 * @param minVersion the minimum version of the range. If <code>null</code>
	 * then {@link Version#emptyVersion} is used.
	 * @param maxVersion the maximum version of the range. If <code>null</code>
	 * then new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
	 * is used. 
	 */
	public VersionRange(Version minVersion, boolean includeMin, Version maxVersion, boolean includeMax) {
		this.minVersion = minVersion == null ? Version.emptyVersion : minVersion;
		this.includeMin = includeMin;
		this.maxVersion = maxVersion == null ? VersionRange.versionMax : maxVersion;
		this.includeMax = includeMax;
	}

	/**
	 * Creates a version range from the specified string.
	 * 
	 * <p>
	 * Here is the grammar for version range strings.
	 * <pre>
	 * version-range ::= interval | atleast
	 * interval ::= ( include-min | exclude-min ) min-version ',' max-version ( include-max | exclude-max )
	 * atleast ::= version
	 * floor ::= version
	 * ceiling ::= version
	 * include-min ::= '['
	 * exclude-min ::= '('
	 * include-max ::= ']'
	 * exclude-max ::= ')'
	 * </pre>
	 * </p>
	 * 
	 * @param versionRange string representation of the version range or <code>null</code>
	 * for the empty range "0.0.0"
	 * @see Version#Version(String) definition of <code>version</code>
	 */
	public VersionRange(String versionRange) {
		if (versionRange == null || versionRange.length() == 0) {
			minVersion = Version.emptyVersion;
			includeMin = true;
			maxVersion = VersionRange.versionMax;
			includeMax = true;
			return;
		}
		versionRange = versionRange.trim();
		if (versionRange.charAt(0) == '[' || versionRange.charAt(0) == '(') {
			int comma = versionRange.indexOf(',');
			if (comma < 0)
				throw new IllegalArgumentException();
			char last = versionRange.charAt(versionRange.length() - 1);
			if (last != ']' && last != ')')
				throw new IllegalArgumentException();

			minVersion = Version.parseVersion(versionRange.substring(1, comma).trim());
			includeMin = versionRange.charAt(0) == '[';
			maxVersion = Version.parseVersion(versionRange.substring(comma + 1, versionRange.length() - 1).trim());
			includeMax = last == ']';
		} else {
			minVersion = Version.parseVersion(versionRange.trim());
			includeMin = true;
			maxVersion = VersionRange.versionMax;
			includeMax = true;
		}
	}

	/**
	 * Returns the minimum Version of this VersionRange.
	 * @return the minimum Version of this VersionRange
	 */
	public Version getMinimum() {
		return minVersion;
	}

	/**
	 * Indicates if the minimum version is included in the version range.
	 * @return true if the minimum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMinimum() {
		return includeMin;
	}

	/**
	 * Returns the maximum Version of this VersionRange.
	 * @return the maximum Version of this VersionRange
	 */
	public Version getMaximum() {
		return maxVersion;
	}

	/**
	 * Indicates if the maximum version is included in the version range.
	 * @return true if the maximum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMaximum() {
		return includeMax;
	}

	/**
	 * Returns whether the given version is included in this VersionRange.
	 * This will depend on the minimum and maximum versions of this VersionRange
	 * and the given version.
	 * 
	 * @param version a version to be tested for inclusion in this VersionRange. 
	 * If <code>null</code> then {@link Version#emptyVersion} is used.
	 * @return <code>true</code> if the version is included, 
	 * <code>false</code> otherwise 
	 */
	public boolean isIncluded(Version version) {
		if (version == null)
			version = Version.emptyVersion;
		int minCheck = includeMin ? 0 : 1;
		int maxCheck = includeMax ? 0 : -1;
		return version.compareTo(minVersion) >= minCheck && version.compareTo(maxVersion) <= maxCheck;

	}

	public boolean equals(Object object) {
		if (!(object instanceof VersionRange))
			return false;
		VersionRange vr = (VersionRange) object;
		if (minVersion.equals(vr.getMinimum()) && includeMin == vr.includeMin)
			if (maxVersion.equals(vr.getMaximum()) && includeMax == vr.includeMax)
				return true;
		return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxVersion.hashCode();
		result = prime * result + minVersion.hashCode();
		result = prime * result + (includeMax ? 1231 : 1237);
		result = prime * result + (includeMin ? 1231 : 1237);
		return result;
	}

	/**
	 * Returns the string representation of this version range.
	 * The encoded format uses the following grammar:
	 * <pre>
	 * version-range ::= interval | atleast
	 * interval ::= ( include-min | exclude-min ) min-version ',' max-version ( include-max | exclude-max )
	 * atleast ::= version
	 * floor ::= version
	 * ceiling ::= version
	 * include-min ::= '['
	 * exclude-min ::= '('
	 * include-max ::= ']'
	 * exclude-max ::= ')'
	 * </pre>
	 * The following are some examples of version range strings and their predicate 
	 * equivalent:
	 * <pre>
	 * [1.2.3, 4.5.6) -> 1.2.3 <= x < 4.5.6
	 * [1.2.3, 4.5.6] -> 1.2.3 <= x <= 4.5.6
	 * (1.2.3, 4.5.6) -> 1.2.3 < x < 4.5.6
	 * (1.2.3, 4.5.6] -> 1.2.3 < x <= 4.5.6
	 * 1.2.3          -> 1.2.3 <= x
	 * </pre>
	 * Note that a simple version (e.g. &quot;1.2.3&quot;) indicates a version range which is
	 * any version greater than or equal to the specified version.
	 * @return The string representation of this version range.
	 * @see Version#toString() string representation of <code>version</code>
	 */
	public String toString() {
		if (VersionRange.versionMax.equals(maxVersion))
			return minVersion.toString(); // we assume infinity max; use simple version (i.e version="1.0")
		StringBuffer result = new StringBuffer();
		result.append(includeMin ? '[' : '(');
		result.append(minVersion);
		result.append(',');
		result.append(maxVersion);
		result.append(includeMax ? ']' : ')');
		return result.toString();
	}
}
