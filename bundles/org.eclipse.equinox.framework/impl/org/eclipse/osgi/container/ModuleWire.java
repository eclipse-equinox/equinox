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

public class ModuleWire implements BundleWire {
	private final ModuleCapability capability;
	private final ModuleRevision hostingProvider;
	private final ModuleRequirement requirement;
	private final ModuleRevision hostingRequirer;

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
		return hostingProvider.getWiring();
	}

	@Override
	public ModuleWiring getRequirerWiring() {
		return hostingRequirer.getWiring();
	}

	@Override
	public ModuleRevision getProvider() {
		return capability.getRevision();
	}

	@Override
	public ModuleRevision getRequirer() {
		return requirement.getRevision();
	}

	public String toString() {
		return getRequirement() + " -> " + getCapability(); //$NON-NLS-1$
	}
}
