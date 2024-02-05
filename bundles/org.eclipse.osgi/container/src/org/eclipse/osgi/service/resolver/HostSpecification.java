/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.service.resolver;

/**
 * A representation of one host bundle constraint as seen in a bundle manifest
 * and managed by a state and resolver.
 * <p>
 * This interface is not intended to be implemented by clients. The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * 
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface HostSpecification extends VersionConstraint {
	/**
	 * Returns the list of host BundleDescriptions that satisfy this
	 * HostSpecification
	 * 
	 * @return the list of host BundleDescriptions that satisfy this
	 *         HostSpecification
	 */
	public BundleDescription[] getHosts();

	/**
	 * Returns if this HostSpecification is allowed to have multiple hosts
	 * 
	 * @return true if this HostSpecification is allowed to have multiple hosts
	 */
	public boolean isMultiHost();
}
