/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.internal.resolver.GenericSpecificationImpl;
import org.eclipse.osgi.service.resolver.GenericSpecification;
import org.osgi.framework.Constants;

public class GenericConstraint extends ResolverConstraint {

	private final boolean effective;
	private boolean supplierHasUses;

	GenericConstraint(ResolverBundle bundle, GenericSpecification constraint) {
		super(bundle, constraint);
		String effectiveDirective = constraint.getRequirement().getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
		effective = effectiveDirective == null || Constants.EFFECTIVE_RESOLVE.equals(effectiveDirective);
	}

	boolean isOptional() {
		return (((GenericSpecification) constraint).getResolution() & GenericSpecification.RESOLUTION_OPTIONAL) != 0;
	}

	boolean isFromRequiredEE() {
		return (((GenericSpecification) constraint).getResolution() & GenericSpecificationImpl.RESOLUTION_FROM_BREE) != 0;
	}

	boolean isMultiple() {
		return !supplierHasUses && (((GenericSpecification) constraint).getResolution() & GenericSpecification.RESOLUTION_MULTIPLE) != 0;
	}

	boolean isEffective() {
		return effective;
	}

	public String getNameSpace() {
		return ((GenericSpecification) getVersionConstraint()).getType();
	}

	public VersionSupplier[] getMatchingCapabilities() {
		if (isMultiple())
			return getPossibleSuppliers();
		VersionSupplier supplier = getSelectedSupplier();
		return supplier == null ? null : new VersionSupplier[] {supplier};
	}

	@Override
	void addPossibleSupplier(VersionSupplier supplier) {
		// if there is a supplier with uses constraints then we no longer allow multiples
		supplierHasUses |= ((GenericCapability) supplier).getUsesDirective() != null;
		super.addPossibleSupplier(supplier);
	}

	@Override
	void clearPossibleSuppliers() {
		super.clearPossibleSuppliers();
		supplierHasUses = false;
	}

	boolean supplierHasUses() {
		return supplierHasUses;
	}
}
