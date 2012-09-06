/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
 * A disabled info represents a policy decision to disable a bundle which exists in a {@link State}.
 * Bundles may be disabled by adding disabled info with the {@link State#addDisabledInfo(DisabledInfo)}
 * method and enabled by removing disabled info with the {@link State#removeDisabledInfo(DisabledInfo)} method.
 * A bundle is not considered to be enabled unless there are no disabled info objects for the bundle.
 * <p>
 * While resolving the bundle if the {@link Resolver} encounters a {@link BundleDescription} which 
 * has disabled info returned by {@link State#getDisabledInfos(BundleDescription)} then the bundle 
 * must not be allowed to resolve and a ResolverError of type {@link ResolverError#DISABLED_BUNDLE}
 * must be added to the state.
 * </p>
 * @see State
 * @since 3.4
 */
public final class DisabledInfo {
	private final String policyName;
	private final String message;
	private final BundleDescription bundle;

	/**
	 * DisabledInfo constructor.
	 * @param policyName the name of the policy
	 * @param message the message, may be <code>null</code>
	 * @param bundle the bundle
	 */
	public DisabledInfo(String policyName, String message, BundleDescription bundle) {
		if (policyName == null || bundle == null)
			throw new IllegalArgumentException();
		this.policyName = policyName;
		this.message = message;
		this.bundle = bundle;
	}

	/**
	 * Returns the name of the policy which disabled the bundle.
	 * @return the name of the policy
	 */
	public String getPolicyName() {
		return policyName;
	}

	/**
	 * Returns the message describing the reason the bundle is disabled.
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the bundle which is disabled
	 * @return the bundle which is disabled
	 */
	public BundleDescription getBundle() {
		return bundle;
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof DisabledInfo))
			return false;
		DisabledInfo other = (DisabledInfo) obj;
		if (getBundle() == other.getBundle() && getPolicyName().equals(other.getPolicyName())) {
			if (getMessage() == null ? other.getMessage() == null : getMessage().equals(other.getMessage()))
				return true;
		}
		return false;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bundle == null ? 0 : bundle.hashCode());
		result = prime * result + (policyName == null ? 0 : policyName.hashCode());
		result = prime * result + (message == null ? 0 : message.hashCode());
		return result;
	}
}
