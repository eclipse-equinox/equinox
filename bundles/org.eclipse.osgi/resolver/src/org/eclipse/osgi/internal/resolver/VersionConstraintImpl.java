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
	private Version versionSpecification;
	private byte matchingRule = GREATER_EQUAL_MATCH;
	private BundleDescription bundle;
	private BundleDescription supplier;
	private Version actualVersion;	
	public String getName() {
		return name;
	}
	public Version getVersionSpecification() {
		return versionSpecification;
	}
	public Version getActualVersion() {
		return actualVersion;
	}
	public byte getMatchingRule() {
		return matchingRule;
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
	public void setVersionSpecification(Version versionSpecification) {
		this.versionSpecification = versionSpecification;
	}
	public void setBundle(BundleDescription bundle) {
		this.bundle = bundle;
	}
	public void unresolve() {
		actualVersion = null;
		supplier = null;
	}
	public boolean isSatisfiedBy(Version provided) {
		Version required = getVersionSpecification();
		if (required == null)
			return true;
		switch (getMatchingRule()) {
			case QUALIFIER_MATCH :
				return provided.matchQualifier(required);
			case MICRO_MATCH :
				return provided.matchMicro(required);
			case MINOR_MATCH :
				return provided.matchMinor(required);
			case GREATER_EQUAL_MATCH:
				return provided.matchGreaterOrEqualTo(required);
			default :
				return provided.matchMajor(required);				
		}
	}
	public String toString() {
		return "name: " + name + " - version: " + versionSpecification;
	}
}