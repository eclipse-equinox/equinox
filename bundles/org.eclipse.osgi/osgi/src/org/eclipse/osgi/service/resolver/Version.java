/*******************************************************************************
 * Copyright (c) 2000, 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * <p>
 * Version identifier for a plug-in. In its string representation, 
 * it consists of up to 4 tokens separated by a decimal point.
 * The first 3 tokens are positive integer numbers, the last token
 * is an uninterpreted string (no whitespace characters allowed).
 * For example, the following are valid version identifiers 
 * (as strings):
 * <ul>
 *   <li><code>0.0.0</code></li>
 *   <li><code>1.0.127564</code></li>
 *   <li><code>3.7.2.build-127J</code></li>
 *   <li><code>1.9</code> (interpreted as <code>1.9.0</code>)</li>
 *   <li><code>3</code> (interpreted as <code>3.0.0</code>)</li>
 * </ul>
 * </p>
 * <p>
 * The version identifier can be decomposed into a major, minor, 
 * micro level component and qualifier components. A difference
 * in the major component is interpreted as an incompatible version
 * change. A difference in the minor (and not the major) component
 * is interpreted as a compatible version change. The micro
 * level component is interpreted as a cumulative and compatible
 * micro update of the minor version component. The qualifier is
 * not interpreted, other than in version comparisons. The 
 * qualifiers are compared using lexicographical string comparison.
 * </p>
 * <p>
 * Version identifiers can be matched as perfectly equal, equivalent,
 * compatible or greaterOrEqual.
 * </p>
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p>
 * @see IPluginDescriptor#getVersionIdentifier
 * @see java.lang.String#compareTo 
 */
public final class Version implements Comparable {

	private int major = 0;
	private int minor = 0;
	private int micro = 0;
	private String qualifier = ""; //$NON-NLS-1$
	private boolean inclusive = true;

	private static final String SEPARATOR = "."; //$NON-NLS-1$

	public static Version emptyVersion = new Version(0, 0, 0);
	public static Version maxVersion = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	/**
	 * Creates a plug-in version identifier from a given Version.
	 * 
	 * @param version 
	 */
	public Version(Version version) {
		this(version.major, version.minor, version.micro, version.qualifier);
		this.inclusive = version.inclusive;
	}

	/**
	 * Creates a plug-in version identifier from its components.
	 * 
	 * @param major major component of the version identifier
	 * @param minor minor component of the version identifier
	 * @param micro micro update component of the version identifier
	 */
	public Version(int major, int minor, int micro) {
		this(major, minor, micro, null);
	}

	/**
	 * Creates a plug-in version identifier from its components.
	 * 
	 * @param major major component of the version identifier
	 * @param minor minor component of the version identifier
	 * @param micro micro update component of the version identifier
	 * @param qualifier qualifier component of the version identifier. 
	 * Qualifier characters that are not a letter or a digit are replaced.
	 */
	public Version(int major, int minor, int micro, String qualifier) throws IllegalArgumentException {

		// Do the test outside of the assert so that they 'Policy.bind' 
		// will not be evaluated each time (including cases when we would
		// have passed by the assert).

		if (major < 0)
			throw new IllegalArgumentException("Negative major");
		if (minor < 0)
			throw new IllegalArgumentException("Negative minor");
		if (micro < 0)
			throw new IllegalArgumentException("Negative micro");
		if (qualifier == null)
			qualifier = ""; //$NON-NLS-1$

		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.qualifier = verifyQualifier(qualifier);
	}

	/**
	 * Creates a plug-in version identifier from the given string.
	 * The string represenation consists of up to 4 tokens 
	 * separated by decimal point.
	 * For example, the following are valid version identifiers 
	 * (as strings):
	 * <ul>
	 *   <li><code>0.0.0</code></li>
	 *   <li><code>1.0.127564</code></li>
	 *   <li><code>3.7.2.build-127J</code></li>
	 *   <li><code>1.9</code> (interpreted as <code>1.9.0</code>)</li>
	 *   <li><code>3</code> (interpreted as <code>3.0.0</code>)</li>
	 * </ul>
	 * </p>
	 * 
	 * @param versionId string representation of the version identifier. 
	 * Qualifier characters that are not a letter or a digit are replaced.
	 */
	public Version(String versionId) {
		if (versionId == null)
			versionId = "0.0.0";
		Object[] parts = parseVersion(versionId);
		this.major = ((Integer) parts[0]).intValue();
		this.minor = ((Integer) parts[1]).intValue();
		this.micro = ((Integer) parts[2]).intValue();
		this.qualifier = (String) parts[3];
	}

	/**
	 * Validates the given string as a plug-in version identifier.
	 * 
	 * @param version the string to validate
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given string is valid as a plug-in version identifier, otherwise a status
	 *		object indicating what is wrong with the string
	 * @since 2.0
	 */
	public static RuntimeException validateVersion(String version) {
		try {
			parseVersion(version);
		} catch (RuntimeException e) {
			return e;
			// OG...
			// return new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR, e.getMessage(), e);
		}
		return null;
		// OG...
		// return new Status(IStatus.OK, Platform.PI_RUNTIME, IStatus.OK, Policy.bind("ok"), null); //$NON-NLS-1$
	}

	private static Object[] parseVersion(String versionId) {

		// Do the test outside of the assert so that they 'Policy.bind' 
		// will not be evaluated each time (including cases when we would
		// have passed by the assert).
		if (versionId == null)
			throw new IllegalArgumentException("Null version string");

		String s = versionId.trim();
		if (s.equals("")) //$NON-NLS-1$
			throw new IllegalArgumentException("Empty version string");
		if (s.startsWith(SEPARATOR))
			throw new IllegalArgumentException("Invalid version format");
		if (s.endsWith(SEPARATOR))
			throw new IllegalArgumentException("Invalid version format");
		if (s.indexOf(SEPARATOR + SEPARATOR) != -1)
			throw new IllegalArgumentException("Invalid version format");

		StringTokenizer st = new StringTokenizer(s, SEPARATOR);
		Vector elements = new Vector(4);

		while (st.hasMoreTokens())
			elements.addElement(st.nextToken());

		int elementSize = elements.size();

		if (elementSize <= 0)
			throw new IllegalArgumentException("Invalid version format (no token)");
		if (elementSize > 4)
			throw new IllegalArgumentException("Invalid version format (more than 4 tokens)");

		int[] numbers = new int[3];
		try {
			numbers[0] = Integer.parseInt((String) elements.elementAt(0));
			if (numbers[0] < 0)
				throw new IllegalArgumentException("Negative major");
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid major");
		}

		try {
			if (elementSize >= 2) {
				numbers[1] = Integer.parseInt((String) elements.elementAt(1));
				if (numbers[1] < 0)
					throw new IllegalArgumentException("Negative minor");
			} else
				numbers[1] = 0;
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid minor");
		}

		try {
			if (elementSize >= 3) {
				numbers[2] = Integer.parseInt((String) elements.elementAt(2));
				if (numbers[2] < 0)
					throw new IllegalArgumentException("Invalid micro");
			} else
				numbers[2] = 0;
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid micro");
		}

		// "result" is a 4-element array with the major, minor, micro, and qualifier
		Object[] result = new Object[4];
		result[0] = new Integer(numbers[0]);
		result[1] = new Integer(numbers[1]);
		result[2] = new Integer(numbers[2]);
		if (elementSize >= 4)
			result[3] = verifyQualifier((String) elements.elementAt(3));
		else
			result[3] = ""; //$NON-NLS-1$
		return result;
	}

	/**
	 * Compare version identifiers for equality. Identifiers are
	 * equal if all of their components are equal.
	 *
	 * @param object an object to compare
	 * @return whehter or not the two objects are equal
	 */
	public boolean equals(Object object) {
		if (!(object instanceof Version))
			return false;
		Version v = (Version) object;
		return v.getMajorComponent() == major && v.getMinorComponent() == minor && v.getMicroComponent() == micro && v.getQualifierComponent().equals(qualifier);
	}

	/**
	 * Returns a hash code value for the object. 
	 *
	 * @return an integer which is a hash code value for this object.
	 */
	public int hashCode() {
		int code = major + minor + micro; // R1.0 result
		if (qualifier.equals("")) //$NON-NLS-1$
			return code;
		else
			return code + qualifier.hashCode();
	}

	/**
	 * Returns the major (incompatible) component of this 
	 * version identifier.
	 *
	 * @return the major version
	 */
	public int getMajorComponent() {
		return major;
	}

	/**
	 * Returns the minor (compatible) component of this 
	 * version identifier.
	 *
	 * @return the minor version
	 */
	public int getMinorComponent() {
		return minor;
	}

	/**
	 * Returns the micro level component of this 
	 * version identifier.
	 *
	 * @return the micro level
	 */
	public int getMicroComponent() {
		return micro;
	}

	/**
	 * Returns the qualifier component of this 
	 * version identifier.
	 *
	 * @return the qualifier
	 */
	public String getQualifierComponent() {
		return qualifier;
	}

	/**
	 * Returns whether this Version is inclusive when used as a
	 * VersionRange minimum or maximum.
	 * @return <code>true</code> if this Version is inclusive, 
	 * <code>false</code> otherwise
	 */
	public boolean isInclusive() {
		return inclusive;
	}

	/**
	 * Sets the inclusive property of this Version
	 * @param inclusive
	 */
	public void setInclusive(boolean inclusive) {
		this.inclusive = inclusive;
	}

	/**
	 * Compares two version identifiers to see if this one is
	 * greater than or equal to the argument.
	 * <p>
	 * A version identifier is considered to be greater than or equal
	 * if its major component is greater than the argument major 
	 * component, or the major components are equal and its minor component
	 * is greater than the argument minor component, or the
	 * major and minor components are equal and its micro component is
	 * greater than the argument micro component, or the major, minor and
	 * micro components are equal and the qualifier component is
	 * greated than the argument qualifier component (using lexicographic
	 * string comparison), or all components are equal.
	 * <p>
	 * TODO deprecated use {@link VersionRange}
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is compatible with the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean matchGreaterOrEqualTo(Version id) {
		if (id == null)
			return false;
		if (major > id.getMajorComponent())
			return true;
		if ((major == id.getMajorComponent()) && (minor > id.getMinorComponent()))
			return true;
		if ((major == id.getMajorComponent()) && (minor == id.getMinorComponent()) && (micro > id.getMicroComponent()))
			return true;
		if ((major == id.getMajorComponent()) && (minor == id.getMinorComponent()) && (micro == id.getMicroComponent()) && (qualifier.compareTo(id.getQualifierComponent()) >= 0))
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for major match.
	 * <p>
	 * A version identifier is considered to match on major if its major 
	 * component equals to the argument major component, and its minor component
	 * is greater than or equal to the argument minor component.
	 * If the minor components are equal, than the micro level of the
	 * version identifier must be greater than or equal to the micro level
	 * of the argument identifier. If the micro levels are equal, the two 
	 * version identifiers are considered to be equivalent if this qualifier is 
	 * greated or equal to the qualifier of the argument (using lexicographic
	 * string comparison).
	 * <p>
	 * TODO deprecated use {@link VersionRange}
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is compatible with the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean matchMajor(Version id) {
		if (id == null)
			return false;
		if (major != id.getMajorComponent())
			return false;
		if (minor > id.getMinorComponent())
			return true;
		if (minor < id.getMinorComponent())
			return false;
		if (micro > id.getMicroComponent())
			return true;
		if (micro < id.getMicroComponent())
			return false;
		if (qualifier.compareTo(id.getQualifierComponent()) >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for equivalency.
	 * <p>
	 * Two version identifiers are considered to be equivalent if their major 
	 * and minor component equal and are at least at the same micro level 
	 * as the argument. If the micro levels are equal, the two version
	 * identifiers are considered to be equivalent if this qualifier is 
	 * greated or equal to the qualifier of the argument (using lexicographic
	 * string comparison).
	 * 
	 * <p>
	 * TODO deprecated use {@link VersionRange}
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is equivalent to the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean matchMinor(Version id) {
		if (id == null)
			return false;
		if (major != id.getMajorComponent())
			return false;
		if (minor != id.getMinorComponent())
			return false;
		if (micro > id.getMicroComponent())
			return true;
		if (micro < id.getMicroComponent())
			return false;
		if (qualifier.compareTo(id.getQualifierComponent()) >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for micro match.
	 * <p>
	 * Two version identifiers are considered to micro match if their major, 
	 * minor and micro components equal and this qualifier is 
	 * greated or equal to the qualifier of the argument (using lexicographic
	 * string comparison).
	 * 
	 * <p>
	 * TODO deprecated use {@link VersionRange}
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    matches on micro to the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean matchMicro(Version id) {
		if (id == null)
			return false;
		if (major != id.getMajorComponent() || minor != id.getMinorComponent() || micro != id.getMicroComponent())
			return false;
		if (qualifier.compareTo(id.getQualifierComponent()) >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for perfect equality.
	 * <p>
	 * Two version identifiers are considered to be perfectly equal if their
	 * major, minor, micro and qualifier components are equal
	 * <p>
	 * TODO deprecated use {@link VersionRange}
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is perfectly equal to the given version identifier, and
	 *    <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean matchQualifier(Version id) {
		return equals(id);
	}

	/**
	 * Compares two version identifiers for order using multi-decimal
	 * comparison. 
	 *
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is greater than the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean isGreaterThan(Version id) {

		if (id == null) {
			if (major == 0 && minor == 0 && micro == 0 && qualifier.equals("")) //$NON-NLS-1$
				return false;
			else
				return true;
		}

		if (major > id.getMajorComponent())
			return true;
		if (major < id.getMajorComponent())
			return false;
		if (minor > id.getMinorComponent())
			return true;
		if (minor < id.getMinorComponent())
			return false;
		if (micro > id.getMicroComponent())
			return true;
		if (micro < id.getMicroComponent())
			return false;
		if (qualifier.compareTo(id.getQualifierComponent()) > 0)
			return true;
		else
			return false;

	}

	/**
	 * Returns the string representation of this version identifier. 
	 * The result satisfies
	 * <code>vi.equals(new PluginVersionIdentifier(vi.toString()))</code>.
	 *
	 * @return the string representation of this plug-in version identifier
	 */
	public String toString() {
		String base = major + SEPARATOR + minor + SEPARATOR + micro;
		// R1.0 result
		if (qualifier.equals("")) //$NON-NLS-1$
			return base;
		else
			return base + SEPARATOR + qualifier;
	}

	private static String verifyQualifier(String s) {
		char[] chars = s.trim().toCharArray();
		boolean whitespace = false;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (!(Character.isLetter(c) || Character.isDigit(c))) {
				chars[i] = '-';
				whitespace = true;
			}
		}
		return whitespace ? new String(chars) : s;
	}

	/**
	 * Compares this Version object with the specified Version object for order.  
	 * Returns a negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.<p>
	 *
	 * @param   o the Version object to be compared.
	 * @return  a negative integer, zero, or a positive integer as this object
	 *		is less than, equal to, or greater than the specified Version object.
	 *
	 * @throws ClassCastException if the specified object's type
	 *         is not Version.
	 */
	public int compareTo(Object o) {
		if (!(o instanceof Version))
			throw new ClassCastException();

		Version version = (Version) o;
		if (equals(o))
			return 0;

		if (isGreaterThan((Version) o))
			return 1;

		return -1;
	}
}