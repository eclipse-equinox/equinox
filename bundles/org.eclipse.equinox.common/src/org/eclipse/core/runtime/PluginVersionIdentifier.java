/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.util.StringTokenizer;
import java.util.Vector;
import org.eclipse.core.internal.runtime.CommonMessages;
import org.eclipse.core.internal.runtime.IRuntimeConstants;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

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
 * service level component and qualifier components. A difference
 * in the major component is interpreted as an incompatible version
 * change. A difference in the minor (and not the major) component
 * is interpreted as a compatible version change. The service
 * level component is interpreted as a cumulative and compatible
 * service update of the minor version component. The qualifier is
 * not interpreted, other than in version comparisons. The 
 * qualifiers are compared using lexicographical string comparison.
 * </p>
 * <p>
 * Version identifiers can be matched as perfectly equal, equivalent,
 * compatible or greaterOrEqual.
 * </p><p>
 * This class can be used without OSGi running.
 * </p><p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p>
 * @see java.lang.String#compareTo(java.lang.String)
 * @deprecated clients should use {@link org.osgi.framework.Version} instead
 */
public final class PluginVersionIdentifier {

	private Version version;

	private static final String SEPARATOR = "."; //$NON-NLS-1$

	/**
	 * Creates a plug-in version identifier from its components.
	 * 
	 * @param major major component of the version identifier
	 * @param minor minor component of the version identifier
	 * @param service service update component of the version identifier
	 */
	public PluginVersionIdentifier(int major, int minor, int service) {
		this(major, minor, service, null);
	}

	/**
	 * Creates a plug-in version identifier from its components.
	 * 
	 * @param major major component of the version identifier
	 * @param minor minor component of the version identifier
	 * @param service service update component of the version identifier
	 * @param qualifier qualifier component of the version identifier. 
	 * Qualifier characters that are not a letter or a digit are replaced.
	 */
	public PluginVersionIdentifier(int major, int minor, int service, String qualifier) {
		// Do the test outside of the assert so that they 'Policy.bind' 
		// will not be evaluated each time (including cases when we would
		// have passed by the assert).

		if (major < 0)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveMajor, major + SEPARATOR + minor + SEPARATOR + service + SEPARATOR + qualifier));
		if (minor < 0)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveMinor, major + SEPARATOR + minor + SEPARATOR + service + SEPARATOR + qualifier));
		if (service < 0)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveService, major + SEPARATOR + minor + SEPARATOR + service + SEPARATOR + qualifier));

		this.version = new Version(major, minor, service, qualifier);
	}

	/**
	 * Creates a plug-in version identifier from the given string.
	 * The string representation consists of up to 4 tokens 
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
	public PluginVersionIdentifier(String versionId) {
		Object[] parts = parseVersion(versionId);
		version = new Version(((Integer) parts[0]).intValue(), ((Integer) parts[1]).intValue(), ((Integer) parts[2]).intValue(), (String) parts[3]);
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
	public static IStatus validateVersion(String version) {
		try {
			parseVersion(version);
		} catch (RuntimeException e) {
			return new Status(IStatus.ERROR, IRuntimeConstants.PI_RUNTIME, IStatus.ERROR, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	private static Object[] parseVersion(String versionId) {

		// Do the test outside of the assert so that they 'Policy.bind' 
		// will not be evaluated each time (including cases when we would
		// have passed by the assert).
		if (versionId == null)
			Assert.isNotNull(null, CommonMessages.parse_emptyPluginVersion);
		String s = versionId.trim();
		if (s.equals("")) //$NON-NLS-1$
			Assert.isTrue(false, CommonMessages.parse_emptyPluginVersion);
		if (s.startsWith(SEPARATOR))
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_separatorStartVersion, s));
		if (s.endsWith(SEPARATOR))
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_separatorEndVersion, s));
		if (s.indexOf(SEPARATOR + SEPARATOR) != -1)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_doubleSeparatorVersion, s));

		StringTokenizer st = new StringTokenizer(s, SEPARATOR);
		Vector elements = new Vector(4);

		while (st.hasMoreTokens())
			elements.addElement(st.nextToken());

		int elementSize = elements.size();

		if (elementSize <= 0)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_oneElementPluginVersion, s));
		if (elementSize > 4)
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_fourElementPluginVersion, s));

		int[] numbers = new int[3];
		try {
			numbers[0] = Integer.parseInt((String) elements.elementAt(0));
			if (numbers[0] < 0)
				Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveMajor, s));
		} catch (NumberFormatException nfe) {
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_numericMajorComponent, s));
		}

		try {
			if (elementSize >= 2) {
				numbers[1] = Integer.parseInt((String) elements.elementAt(1));
				if (numbers[1] < 0)
					Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveMinor, s));
			} else
				numbers[1] = 0;
		} catch (NumberFormatException nfe) {
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_numericMinorComponent, s));
		}

		try {
			if (elementSize >= 3) {
				numbers[2] = Integer.parseInt((String) elements.elementAt(2));
				if (numbers[2] < 0)
					Assert.isTrue(false, NLS.bind(CommonMessages.parse_postiveService, s));
			} else
				numbers[2] = 0;
		} catch (NumberFormatException nfe) {
			Assert.isTrue(false, NLS.bind(CommonMessages.parse_numericServiceComponent, s));
		}

		// "result" is a 4-element array with the major, minor, service, and qualifier
		Object[] result = new Object[4];
		result[0] = new Integer(numbers[0]);
		result[1] = new Integer(numbers[1]);
		result[2] = new Integer(numbers[2]);
		if (elementSize >= 4)
			result[3] = (String) elements.elementAt(3);
		else
			result[3] = ""; //$NON-NLS-1$
		return result;
	}

	/**
	 * Compare version identifiers for equality. Identifiers are
	 * equal if all of their components are equal.
	 *
	 * @param object an object to compare
	 * @return whether or not the two objects are equal
	 */
	public boolean equals(Object object) {
		if (!(object instanceof PluginVersionIdentifier))
			return false;
		PluginVersionIdentifier v = (PluginVersionIdentifier) object;
		return version.equals(v.version);
	}

	/**
	 * Returns a hash code value for the object. 
	 *
	 * @return an integer which is a hash code value for this object.
	 */
	public int hashCode() {
		return version.hashCode();
	}

	/**
	 * Returns the major (incompatible) component of this 
	 * version identifier.
	 *
	 * @return the major version
	 */
	public int getMajorComponent() {
		return version.getMajor();
	}

	/**
	 * Returns the minor (compatible) component of this 
	 * version identifier.
	 *
	 * @return the minor version
	 */
	public int getMinorComponent() {
		return version.getMinor();
	}

	/**
	 * Returns the service level component of this 
	 * version identifier.
	 *
	 * @return the service level
	 */
	public int getServiceComponent() {
		return version.getMicro();
	}

	/**
	 * Returns the qualifier component of this 
	 * version identifier.
	 *
	 * @return the qualifier
	 */
	public String getQualifierComponent() {
		return version.getQualifier();
	}

	/**
	 * Compares two version identifiers to see if this one is
	 * greater than or equal to the argument.
	 * <p>
	 * A version identifier is considered to be greater than or equal
	 * if its major component is greater than the argument major 
	 * component, or the major components are equal and its minor component
	 * is greater than the argument minor component, or the
	 * major and minor components are equal and its service component is
	 * greater than the argument service component, or the major, minor and
	 * service components are equal and the qualifier component is
	 * greater than the argument qualifier component (using lexicographic
	 * string comparison), or all components are equal.
	 * </p>
	 *
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is compatible with the given version identifier, and
	 *    <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean isGreaterOrEqualTo(PluginVersionIdentifier id) {
		if (id == null)
			return false;
		if (getMajorComponent() > id.getMajorComponent())
			return true;
		if ((getMajorComponent() == id.getMajorComponent()) && (getMinorComponent() > id.getMinorComponent()))
			return true;
		if ((getMajorComponent() == id.getMajorComponent()) && (getMinorComponent() == id.getMinorComponent()) && (getServiceComponent() > id.getServiceComponent()))
			return true;
		if ((getMajorComponent() == id.getMajorComponent()) && (getMinorComponent() == id.getMinorComponent()) && (getServiceComponent() == id.getServiceComponent()) && (getQualifierComponent().compareTo(id.getQualifierComponent()) >= 0))
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for compatibility.
	 * <p>
	 * A version identifier is considered to be compatible if its major 
	 * component equals to the argument major component, and its minor component
	 * is greater than or equal to the argument minor component.
	 * If the minor components are equal, than the service level of the
	 * version identifier must be greater than or equal to the service level
	 * of the argument identifier. If the service levels are equal, the two 
	 * version identifiers are considered to be equivalent if this qualifier is 
	 * greater or equal to the qualifier of the argument (using lexicographic
	 * string comparison).
	 * </p>
	 *
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is compatible with the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean isCompatibleWith(PluginVersionIdentifier id) {
		if (id == null)
			return false;
		if (getMajorComponent() != id.getMajorComponent())
			return false;
		if (getMinorComponent() > id.getMinorComponent())
			return true;
		if (getMinorComponent() < id.getMinorComponent())
			return false;
		if (getServiceComponent() > id.getServiceComponent())
			return true;
		if (getServiceComponent() < id.getServiceComponent())
			return false;
		if (getQualifierComponent().compareTo(id.getQualifierComponent()) >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for equivalency.
	 * <p>
	 * Two version identifiers are considered to be equivalent if their major 
	 * and minor component equal and are at least at the same service level 
	 * as the argument. If the service levels are equal, the two version
	 * identifiers are considered to be equivalent if this qualifier is 
	 * greater or equal to the qualifier of the argument (using lexicographic
	 * string comparison).
	 * 
	 * </p>
	 *
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is equivalent to the given version identifier, and
	 *    <code>false</code> otherwise
	 */
	public boolean isEquivalentTo(PluginVersionIdentifier id) {
		if (id == null)
			return false;
		if (getMajorComponent() != id.getMajorComponent())
			return false;
		if (getMinorComponent() != id.getMinorComponent())
			return false;
		if (getServiceComponent() > id.getServiceComponent())
			return true;
		if (getServiceComponent() < id.getServiceComponent())
			return false;
		if (getQualifierComponent().compareTo(id.getQualifierComponent()) >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Compares two version identifiers for perfect equality.
	 * <p>
	 * Two version identifiers are considered to be perfectly equal if their
	 * major, minor, service and qualifier components are equal
	 * </p>
	 *
	 * @param id the other version identifier
	 * @return <code>true</code> is this version identifier
	 *    is perfectly equal to the given version identifier, and
	 *    <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean isPerfect(PluginVersionIdentifier id) {
		if (id == null)
			return false;
		if ((getMajorComponent() != id.getMajorComponent()) || (getMinorComponent() != id.getMinorComponent()) || (getServiceComponent() != id.getServiceComponent()) || (!getQualifierComponent().equals(id.getQualifierComponent())))
			return false;
		else
			return true;
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
	public boolean isGreaterThan(PluginVersionIdentifier id) {

		if (id == null) {
			if (getMajorComponent() == 0 && getMinorComponent() == 0 && getServiceComponent() == 0 && getQualifierComponent().equals("")) //$NON-NLS-1$
				return false;
			else
				return true;
		}

		if (getMajorComponent() > id.getMajorComponent())
			return true;
		if (getMajorComponent() < id.getMajorComponent())
			return false;
		if (getMinorComponent() > id.getMinorComponent())
			return true;
		if (getMinorComponent() < id.getMinorComponent())
			return false;
		if (getServiceComponent() > id.getServiceComponent())
			return true;
		if (getServiceComponent() < id.getServiceComponent())
			return false;
		if (getQualifierComponent().compareTo(id.getQualifierComponent()) > 0)
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
		return version.toString();
	}

}
