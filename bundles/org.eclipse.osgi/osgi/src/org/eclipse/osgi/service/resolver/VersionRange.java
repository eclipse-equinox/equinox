/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

public class VersionRange {

	private Version minVersion;
	private Version maxVersion;

	/**
	 * Constructs a VersionRange with the specified minVersion and maxVersion.
	 * @param minVersion the minimum version of the range
	 * @param maxVersion the maximum version of the range
	 */
	public VersionRange(Version minVersion, Version maxVersion) {
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}

	/**
	 * Constructs a VersionRange from the given versionRange String.
	 * @param versionRange a version range String that specifies a range of
	 * versions.
	 */
	public VersionRange(String versionRange) {
		if (versionRange == null || versionRange.length() == 0)
			return;
		versionRange = versionRange.trim();
		if (versionRange.charAt(0) == '[' || versionRange.charAt(0) == '(') {
			int comma = versionRange.indexOf(',');
			if (comma < 0)
				throw new IllegalArgumentException();
			char last = versionRange.charAt(versionRange.length() - 1);
			if (last != ']' && last != ')')
				throw new IllegalArgumentException();

			minVersion = new Version(versionRange.substring(1, comma), versionRange.charAt(0) == '[');
			maxVersion = new Version(versionRange.substring(comma + 1, versionRange.length() - 1), last == ']');
		} else {
			minVersion = new Version(versionRange);
			maxVersion = Version.maxVersion;
		}
	}

	/**
	 * Returns the minimum Version of this VersionRange
	 * @return the minimum Version of this VersionRange
	 */
	public Version getMinimum() {
		return minVersion;
	}

	/**
	 * Returns the maximum Version of this VersionRange
	 * @return the maximum Version of this VersionRange
	 */
	public Version getMaximum() {
		return maxVersion;
	}

	/**
	 * Returns whether the given version is included in this VersionRange.
	 * This will depend on the minimum and maximum versions of this VersionRange
	 * and the given version.
	 * 
	 * @param version a version to be tested for inclusion in this VersionRange. 
	 * (may be <code>null</code>)
	 * @return <code>true</code> if the version is include, 
	 * <code>false</code> otherwise 
	 */
	public boolean isIncluded(Version version) {
		Version minRequired = getMinimum();
		if (minRequired == null)
			return true;
		if (version == null)
			return false;
		Version maxRequired = getMaximum() == null ? Version.maxVersion : getMaximum();
		int minCheck = minRequired.isInclusive() ? 0 : 1;
		int maxCheck = maxRequired.isInclusive() ? 0 : -1;
		return version.compareTo(minRequired) >= minCheck && version.compareTo(maxRequired) <= maxCheck;

	}

	public String toString() {
		if (minVersion != null && Version.maxVersion.equals(maxVersion)) {
			return minVersion.toString();
		}
		StringBuffer result = new StringBuffer();
		if (minVersion != null)
			result.append(minVersion.isInclusive() ? '[' : '(');
		result.append(minVersion);
		result.append(',');
		result.append(maxVersion);
		if (maxVersion != null)
			result.append(maxVersion.isInclusive() ? ']' : ')');
		return result.toString();
	}
}