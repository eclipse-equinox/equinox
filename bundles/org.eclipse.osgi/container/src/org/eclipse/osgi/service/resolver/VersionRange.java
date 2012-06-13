/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
public class VersionRange extends org.osgi.framework.VersionRange {
	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	private static final char INCLUDE_MIN = org.osgi.framework.VersionRange.LEFT_CLOSED;
	private static final char EXCLUDE_MIN = org.osgi.framework.VersionRange.LEFT_OPEN;
	private static final char INCLUDE_MAX = org.osgi.framework.VersionRange.RIGHT_CLOSED;
	private static final char EXCLUDE_MAX = org.osgi.framework.VersionRange.RIGHT_OPEN;

	/**
	 * An empty version range: "0.0.0".  The empty version range includes all valid versions
	 * (any version greater than or equal to the version 0.0.0).
	 */
	public static final VersionRange emptyRange = new VersionRange("0.0.0"); //$NON-NLS-1$

	/**
	 * Constructs a VersionRange with the specified minVersion and maxVersion.
	 * @param minVersion the minimum version of the range. If <code>null</code>
	 * then {@link Version#emptyVersion} is used.
	 * @param maxVersion the maximum version of the range. If <code>null</code>
	 * then new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
	 * is used. 
	 */
	public VersionRange(Version minVersion, boolean includeMin, Version maxVersion, boolean includeMax) {
		super(includeMin ? INCLUDE_MIN : EXCLUDE_MIN, minVersion == null ? Version.emptyVersion : minVersion, versionMax.equals(maxVersion) ? null : maxVersion, includeMax ? INCLUDE_MAX : EXCLUDE_MAX); 
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
		super(versionRange == null || versionRange.length() == 0 ? "0.0.0" : versionRange); //$NON-NLS-1$
	}

	/**
	 * Returns the minimum Version of this VersionRange.
	 * @return the minimum Version of this VersionRange
	 */
	public Version getMinimum() {
		return getLeft();
	}

	/**
	 * Indicates if the minimum version is included in the version range.
	 * @return true if the minimum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMinimum() {
		return getLeftType() == VersionRange.LEFT_CLOSED;
	}

	/**
	 * Returns the maximum Version of this VersionRange.
	 * <p>
	 * This method is deprecated.  For ranges that have no maximum this method
	 * incorrectly returns a version equal to 
	 * <code>Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)</code>.
	 * Use {@link org.osgi.framework.VersionRange#getRight()} instead.
	 * @return the maximum Version of this VersionRange
	 * @deprecated use {@link org.osgi.framework.VersionRange#getRight()}
	 */
	public Version getMaximum() {
		Version right = getRight();
		return right == null ? versionMax : right;
	}

	/**
	 * Indicates if the maximum version is included in the version range.
	 * @return true if the maximum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMaximum() {
		return getRightType() == VersionRange.RIGHT_CLOSED;
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
		return includes(version);
	}
}
