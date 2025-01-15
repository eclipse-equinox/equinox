/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.identity;

/**
 * 
 * Identifies string in a manifest like <code>plugin.xml</code> that should
 * match string in another manifest or in Java code
 */
public interface ContributionIdentity {

	/**
	 * Required to match historical contributions defined by a key only
	 */
	BundleIdentity HISTORICAL = new BundleIdentity() {

		@Override
		public String symbolic() {
			return ""; //$NON-NLS-1$
		}
	};

	/**
	 * The identity of contributing bundle, typically a part of
	 * {@link ContributionIdentity#id()}
	 * 
	 * @return contributing bundle identity
	 */
	BundleIdentity bundle();

	/**
	 * The key of contribution, typically a part of
	 * {@link ContributionIdentity#id()}
	 * 
	 * @return key of contribution
	 */
	String key();

	/**
	 * Full qualified id of this contribution, typically contains both
	 * {@link BundleIdentity#symbolic()} and {@link ContributionIdentity#key()}
	 * 
	 * @return full qualified id
	 */
	String id();

}
