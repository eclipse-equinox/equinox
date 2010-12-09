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

import java.util.Map;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.Capability;

/*
 * A companion to BaseDescription from the state used while resolving.
 */
public abstract class VersionSupplier implements Capability {
	final protected BaseDescription base;
	final private Capability capability;
	private VersionSupplier substitute;

	VersionSupplier(BaseDescription base) {
		this.base = base;
		this.capability = base.getCapability();
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

	// returns the version supplier that has been substituted for this version supplier
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

	public String getNamespace() {
		return capability.getNamespace();
	}

	public Map<String, String> getDirectives() {
		return capability.getDirectives();
	}

	public Map<String, Object> getAttributes() {
		return capability.getAttributes();
	}

	public BundleRevision getProviderRevision() {
		return capability.getProviderRevision();
	}

	@Override
	public int hashCode() {
		return capability.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return capability.equals(obj);
	}

}
