/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.ArrayList;
import org.eclipse.osgi.service.resolver.VersionConstraint;

public class GenericConstraint extends ResolverConstraint {
	private ArrayList matchingCapability;

	GenericConstraint(ResolverBundle bundle, VersionConstraint constraint) {
		super(bundle, constraint);
	}

	boolean isOptional() {
		return false;
	}

	boolean isSatisfiedBy(VersionSupplier vs) {
		return !vs.getResolverBundle().isUninstalled() && getVersionConstraint().isSatisfiedBy(vs.getBaseDescription());
	}

	public void setMatchingCapability(GenericCapability capability) {
		if (capability == null) {
			matchingCapability = null;
			return;
		}
		if (matchingCapability == null)
			matchingCapability = new ArrayList(1);
		matchingCapability.add(capability);
	}

	public GenericCapability[] getMatchingCapabilities() {
		return matchingCapability == null || matchingCapability.size() == 0 ? null : (GenericCapability[]) matchingCapability.toArray(new GenericCapability[matchingCapability.size()]);
	}

	void removeMatchingCapability(GenericCapability capability) {
		if (matchingCapability != null)
			matchingCapability.remove(capability);
	}
}
