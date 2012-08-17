/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.osgi.framework.wiring.BundleRequirement;

/*
 * A companion to VersionConstraint from the state used while resolving
 */
public abstract class ResolverConstraint {
	final protected ResolverBundle bundle;
	protected VersionConstraint constraint;
	private BundleRequirement requrement;
	private VersionSupplier[] possibleSuppliers;
	private int selectedSupplierIndex = 0;

	ResolverConstraint(ResolverBundle bundle, VersionConstraint constraint) {
		this.bundle = bundle;
		this.constraint = constraint;
		this.requrement = constraint.getRequirement();
	}

	// returns the Resolver bundle requiring the ResolverConstraint
	ResolverBundle getBundle() {
		return bundle;
	}

	// returns the BundleDescription requiring the ResolverConstraint
	BundleDescription getBundleDescription() {
		return bundle.getBundleDescription();
	}

	// returns whether this constraint is from an attached fragment
	boolean isFromFragment() {
		return constraint.getBundle().getHost() != null;
	}

	// Same as VersionConstraint but does additinal permission checks
	boolean isSatisfiedBy(VersionSupplier vs) {
		if (vs.getResolverBundle().isUninstalled() || !bundle.getResolver().getPermissionChecker().checkPermission(constraint, vs.getBaseDescription()))
			return false;
		return vs.getSubstitute() == null && constraint.isSatisfiedBy(vs.getBaseDescription());
	}

	// returns the companion VersionConstraint object from the State
	VersionConstraint getVersionConstraint() {
		return constraint;
	}

	// returns the name of this constraint
	public String getName() {
		return constraint.getName();
	}

	public String toString() {
		return constraint.toString();
	}

	// returns whether this constraint is optional
	abstract boolean isOptional();

	void addPossibleSupplier(VersionSupplier supplier) {
		if (supplier == null)
			return;
		// we hope multiple suppliers are rare so do simple array expansion here.
		if (possibleSuppliers == null) {
			possibleSuppliers = new VersionSupplier[] {supplier};
			return;
		}
		VersionSupplier[] newSuppliers = new VersionSupplier[possibleSuppliers.length + 1];
		System.arraycopy(possibleSuppliers, 0, newSuppliers, 0, possibleSuppliers.length);
		newSuppliers[possibleSuppliers.length] = supplier;
		possibleSuppliers = newSuppliers;
	}

	public void removePossibleSupplier(VersionSupplier supplier) {
		if (possibleSuppliers == null || supplier == null)
			return;
		int index = -1;
		for (int i = 0; i < possibleSuppliers.length; i++) {
			if (possibleSuppliers[i] == supplier) {
				index = i;
				break;
			}
		}
		if (index >= 0) {
			if (possibleSuppliers.length == 1) {
				possibleSuppliers = null;
				return;
			}
			VersionSupplier[] newSuppliers = new VersionSupplier[possibleSuppliers.length - 1];
			System.arraycopy(possibleSuppliers, 0, newSuppliers, 0, index);
			if (index < possibleSuppliers.length - 1)
				System.arraycopy(possibleSuppliers, index + 1, newSuppliers, index, possibleSuppliers.length - index - 1);
			possibleSuppliers = newSuppliers;
		}
	}

	int getNumPossibleSuppliers() {
		if (possibleSuppliers == null)
			return 0;
		return possibleSuppliers.length;
	}

	boolean selectNextSupplier() {
		if (possibleSuppliers == null || selectedSupplierIndex >= possibleSuppliers.length)
			return false;
		selectedSupplierIndex += 1;
		return selectedSupplierIndex < possibleSuppliers.length;
	}

	VersionSupplier getSelectedSupplier() {
		if (possibleSuppliers == null || selectedSupplierIndex >= possibleSuppliers.length)
			return null;
		return possibleSuppliers[selectedSupplierIndex];
	}

	void setSelectedSupplier(int selectedSupplier) {
		this.selectedSupplierIndex = selectedSupplier;
	}

	int getSelectedSupplierIndex() {
		return this.selectedSupplierIndex;
	}

	VersionSupplier[] getPossibleSuppliers() {
		return possibleSuppliers;
	}

	void clearPossibleSuppliers() {
		possibleSuppliers = null;
		selectedSupplierIndex = 0;
	}

	void setVersionConstraint(VersionConstraint constraint) {
		this.constraint = constraint;
		this.requrement = constraint.getRequirement();
	}

	BundleRequirement getRequirement() {
		return requrement;
	}
}
