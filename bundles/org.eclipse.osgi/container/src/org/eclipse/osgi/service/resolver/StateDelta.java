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
 * A state delta contains all the changes to bundles within a state.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface StateDelta {
	/**
	 * Returns an array of all the bundle deltas in this delta regardless of type.
	 * 
	 * @return an array of bundle deltas
	 */
	public BundleDelta[] getChanges();

	/**
	 * Returns an array of all the members of this delta which match the given
	 * flags. If an exact match is requested then only delta members whose type
	 * exactly matches the given mask are included. Otherwise, all bundle deltas
	 * whose type's bit-wise and with the mask is non-zero are included.
	 *
	 * @param mask  match mask
	 * @param exact whether exact match or bit-wise matching should be performed
	 * @return an array of bundle deltas matching the given match criteria.
	 */
	public BundleDelta[] getChanges(int mask, boolean exact);

	/**
	 * Returns the state whose changes are represented by this delta.
	 * 
	 * @return the state
	 */
	public State getState();

	/**
	 * Returns the resolver hook exception if one occurred while resolving the
	 * state.
	 * 
	 * @since 3.7
	 */
	public ResolverHookException getResovlerHookException();
}
