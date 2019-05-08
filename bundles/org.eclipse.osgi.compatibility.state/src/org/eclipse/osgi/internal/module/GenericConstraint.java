/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Karsten Thoms (itemis) - Consider devmode for effective status computation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.internal.resolver.GenericSpecificationImpl;
import org.eclipse.osgi.service.resolver.GenericSpecification;
import org.osgi.framework.Constants;

public class GenericConstraint extends ResolverConstraint {

	private final boolean effective;
	private final boolean multiple;

	GenericConstraint(ResolverBundle bundle, GenericSpecification constraint, boolean developmentMode) {
		super(bundle, constraint);
		String effectiveDirective = constraint.getRequirement().getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
		effective = effectiveDirective == null || Constants.EFFECTIVE_RESOLVE.equals(effectiveDirective) || (Constants.EFFECTIVE_ACTIVE.equals(effectiveDirective) && developmentMode);
		multiple = (constraint.getResolution() & GenericSpecification.RESOLUTION_MULTIPLE) != 0;
	}

	@Override
	boolean isOptional() {
		return (((GenericSpecification) constraint).getResolution() & GenericSpecification.RESOLUTION_OPTIONAL) != 0;
	}

	boolean isFromRequiredEE() {
		return (((GenericSpecification) constraint).getResolution() & GenericSpecificationImpl.RESOLUTION_FROM_BREE) != 0;
	}

	boolean isMultiple() {
		return multiple;
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
}
