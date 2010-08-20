/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;

/*
 * A companion to BaseDescription from the state used while resolving.
 */
public abstract class VersionSupplier implements Capability {
	protected BaseDescription base;
	private VersionSupplier substitute;

	VersionSupplier(BaseDescription base) {
		this.base = base;
	}

	public Version getVersion() {
		return base.getVersion();
	}

	public String getName() {
		return base.getName();
	}

	public BaseDescription getBaseDescription() {
		return base;
	}

	// returns true if this version supplier has been dropped and is no longer available as a wire
	VersionSupplier getSubstitute() {
		return substitute;
	}

	// sets the dropped status.  This should only be called by the VersionHashMap 
	// when VersionSuppliers are removed
	void setSubstitute(VersionSupplier substitute) {
		this.substitute = substitute;
	}

	/*
	 * returns the BundleDescription which supplies this VersionSupplier
	 */
	abstract public BundleDescription getBundleDescription();

	/*
	 * returns the ResolverBundle which supplies this VersionSupplier 
	 */
	abstract ResolverBundle getResolverBundle();

	public String toString() {
		return base.toString();
	}
}
