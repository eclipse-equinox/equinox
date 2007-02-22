/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	boolean isOptional() {
		if (constraint instanceof HostSpecification)
			return false;
		return ((BundleSpecification) constraint).isOptional();
	}
}
