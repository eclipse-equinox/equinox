/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

/**
 * A state wire represents a decision made by a resolver to wire a requirement to a capability.
 * There are 4 parts to a state wire.
 * <ul>
 * <li>The requirement which may have been specified by a host bundle or one of its attached fragments.</li>
 * <li>The host bundle which is associated with the requirement.  There are cases where the host 
 * bundle may not be the same as the bundle which declared the requirement.  For example, if a fragment
 * specifies additional requirements.</li>
 * <li>The capability which may have been specified by a host bundle or one of its attached fragments.</li>
 * <li>The host bundle which is associated with the capability.  There are cases where the host
 * bundle may not be the same as the bundle which declared the capability.  For example, if a fragment
 * specifies additional capabilities.</li>
 * </ul>
 * @since 3.7
 */
public class StateWire {
	private final BundleDescription requirementHost;
	private final VersionConstraint declaredRequirement;
	private final BundleDescription capabilityHost;
	private final BaseDescription declaredCapability;

	/**
	 * Constructs a new state wire.
	 * @param requirementHost the bundle hosting the requirement.
	 * @param declaredRequirement the declared requirement.  The bundle declaring the requirement may be different from the requirement host.
	 * @param capabilityHost the bundle hosting the capability.
	 * @param declaredCapability the declared capability.  The bundle declaring the capability may be different from the capability host.
	 */
	public StateWire(BundleDescription requirementHost, VersionConstraint declaredRequirement, BundleDescription capabilityHost, BaseDescription declaredCapability) {
		super();
		this.requirementHost = requirementHost;
		this.declaredRequirement = declaredRequirement;
		this.capabilityHost = capabilityHost;
		this.declaredCapability = declaredCapability;

	}

	/**
	 * Gets the requirement host.
	 * @return the requirement host.
	 */
	public BundleDescription getRequirementHost() {
		return requirementHost;
	}

	/**
	 * Gets the declared requirement.
	 * @return the declared requirement.
	 */
	public VersionConstraint getDeclaredRequirement() {
		return declaredRequirement;
	}

	/**
	 * gets the capability host.
	 * @return the capability host.
	 */
	public BundleDescription getCapabilityHost() {
		return capabilityHost;
	}

	/**
	 * gets the declared capability.
	 * @return the declared capability.
	 */
	public BaseDescription getDeclaredCapability() {
		return declaredCapability;
	}
}
