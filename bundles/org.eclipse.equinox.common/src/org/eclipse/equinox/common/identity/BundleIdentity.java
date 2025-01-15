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
 * <code>Bundle-SymbolicName</code> to be used as qualifier
 * 
 * @see BundleIdentityRecord
 * @see ContributionIdentity
 */
public interface BundleIdentity {

	/**
	 * <code>Bundle-SymbolicName</code> value
	 * 
	 * @return bundle symbolic name
	 */
	String symbolic();

}
