/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import org.osgi.framework.wiring.BundleWire;

/**
 * An implementation of {@link BundleWire}.
 */
public class ModuleWire implements BundleWire {
	private final ModuleCapability capability;
	private final ModuleRevision hostingProvider;
	private final ModuleRequirement requirement;
	private final ModuleRevision hostingRequirer;
	// indicates that the wire points to valid wirings
	// technically this should be a separate flag for requirer vs provider but that seems like overkill
	private volatile boolean isValid = true;

	ModuleWire(ModuleCapability capability, ModuleRevision hostingProvider, ModuleRequirement requirement, ModuleRevision hostingRequirer) {
		super();
		this.capability = capability;
		this.hostingProvider = hostingProvider;
		this.requirement = requirement;
		this.hostingRequirer = hostingRequirer;
	}

	@Override
	public ModuleCapability getCapability() {
		return capability;
	}

	@Override
	public ModuleRequirement getRequirement() {
		return requirement;
	}

	@Override
	public ModuleWiring getProviderWiring() {
		if (!isValid) {
			return null;
		}
		return hostingProvider.getWiring();
	}

	@Override
	public ModuleWiring getRequirerWiring() {
		if (!isValid) {
			return null;
		}
		return hostingRequirer.getWiring();
	}

	@Override
	public ModuleRevision getProvider() {
		return hostingProvider;
	}

	@Override
	public ModuleRevision getRequirer() {
		return hostingRequirer;
	}

	public String toString() {
		return getRequirement() + " -> " + getCapability(); //$NON-NLS-1$
	}

	void invalidate() {
		this.isValid = false;
	}

}
