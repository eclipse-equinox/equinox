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

import org.eclipse.osgi.service.resolver.*;

public abstract class VersionConstraintImpl implements VersionConstraint {
	private String name;
	private VersionRange versionRange;
	private byte matchingRule = -1;
	private BundleDescription bundle;
	private BundleDescription supplier;
	private Version actualVersion;

	public String getName() {
		return name;
	}

	public Version getVersionSpecification() {
		return versionRange == null ? null : versionRange.getMinimum();
	}

	public Version getActualVersion() {
		return actualVersion;
	}

	public byte getMatchingRule() {
		if (matchingRule != -1)
			return matchingRule;
		if (versionRange == null || versionRange.getMinimum() == null) {
			matchingRule = NO_MATCH;
			return matchingRule;
		}

		Version minimum = versionRange.getMinimum();
		Version maximum = versionRange.getMaximum() == null ? Version.maxVersion : versionRange.getMaximum();

		if (maximum.equals(Version.maxVersion))
			matchingRule = GREATER_EQUAL_MATCH;
		else if (minimum.equals(maximum))
			matchingRule = QUALIFIER_MATCH;
		else if (!minimum.isInclusive() || maximum.isInclusive())
			matchingRule = OTHER_MATCH;
		else if (minimum.getMajorComponent() == maximum.getMajorComponent() - 1)
			matchingRule = MAJOR_MATCH;
		else if (minimum.getMajorComponent() != maximum.getMajorComponent())
			matchingRule = OTHER_MATCH;
		else if (minimum.getMinorComponent() == maximum.getMinorComponent() - 1)
			matchingRule = MINOR_MATCH;
		else if (minimum.getMinorComponent() != maximum.getMinorComponent())
			matchingRule = OTHER_MATCH;
		else if (minimum.getMicroComponent() == maximum.getMicroComponent() - 1)
			matchingRule = MICRO_MATCH;
		else
			matchingRule = OTHER_MATCH;

		return matchingRule;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public BundleDescription getBundle() {
		return bundle;
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	public boolean isResolved() {
		return supplier != null;
	}

	public void setActualVersion(Version actualVersion) {
		this.actualVersion = actualVersion;
	}

	public void setSupplier(BundleDescription supplier) {
		this.supplier = supplier;
	}

	public void setMatchingRule(byte matchingRule) {
		this.matchingRule = matchingRule;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBundle(BundleDescription bundle) {
		this.bundle = bundle;
	}

	public void setVersionRange(VersionRange versionRange) {
		this.versionRange = versionRange;
	}

	public void unresolve() {
		actualVersion = null;
		supplier = null;
	}

	public boolean isSatisfiedBy(Version provided) {
		return versionRange == null ? true : versionRange.isIncluded(provided);
	}

	public String toString() {
		return "name: " + name + " - version: " + versionRange; //$NON-NLS-1$ //$NON-NLS-2$
	}
}