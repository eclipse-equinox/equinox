/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.osgi.framework.Constants;

/*
 * A companion to ImportPackageSpecification from the state used while resolving
 */
public class ResolverImport extends ResolverConstraint {
	// only used for dynamic imports
	private String name;

	ResolverImport(ResolverBundle bundle, ImportPackageSpecification ips) {
		super(bundle, ips);
	}

	@Override
	boolean isOptional() {
		return ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) constraint).getDirective(Constants.RESOLUTION_DIRECTIVE));
	}

	boolean isDynamic() {
		return ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(((ImportPackageSpecification) constraint).getDirective(Constants.RESOLUTION_DIRECTIVE));
	}

	@Override
	public String getName() {
		if (name != null) {
			return name; // return the required package set for a dynamic import
		}
		return super.getName();
	}

	// used for dynamic import package when wildcards are used
	void setName(String requestedPackage) {
		this.name = requestedPackage;
	}
}
