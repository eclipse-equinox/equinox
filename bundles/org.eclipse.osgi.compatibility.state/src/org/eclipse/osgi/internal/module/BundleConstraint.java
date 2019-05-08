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

import org.eclipse.osgi.service.resolver.*;

/*
 * A companion to BundleSpecification from the state for use while resolving
 */
public class BundleConstraint extends ResolverConstraint {
	BundleConstraint(ResolverBundle bundle, VersionConstraint bundleConstraint) {
		super(bundle, bundleConstraint);
	}

	@Override
	boolean isOptional() {
		if (constraint instanceof HostSpecification)
			return false;
		return ((BundleSpecification) constraint).isOptional();
	}
}
