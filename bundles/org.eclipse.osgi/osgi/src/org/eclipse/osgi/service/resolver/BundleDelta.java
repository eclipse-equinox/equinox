/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

/**
 * BundleDeltas represent the changes related to an individual bundle between two
 * states.
 */
public interface BundleDelta {

	/**
	 * Delta type constant (bit mask) indicating that the bundle has been added
	 * to the new state. 
	 * @see BundleDelta#getType
	 */
	public static final int ADDED = 0x1;
	/**
	 * Delta type constant (bit mask) indicating that the bundle is no longer present in 
	 * the new state.
	 * @see BundleDelta#getType
	 */
	public static final int REMOVED = 0x2;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has been updated
	 * between the old and new state.  Note that an update delta may in fact represent
	 * a downgrading of the bundle to a previous version. 
	 * @see BundleDelta#getType
	 */
	public static final int UPDATED = 0x4;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has become resolved
	 * in the new state.  
	 * @see BundleDelta#getType
	 */
	public static final int RESOLVED = 0x8;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has become unresolved
	 * in the new state. Note that newly added bundles are unresolved by default and 
	 * as such, do not transition to unresolved state so this flag is not set.
	 * @see BundleDelta#getType
	 */
	public static final int UNRESOLVED = 0x10;
	/**
	 * Delta type constant (bit mask) indicating that the bundles and packages which this
	 * bundle requires/imports (respectively) have changed in the new state.
	 * @see BundleDelta#getType
	 */
	public static final int LINKAGE_CHANGED = 0x20;

	/**
	 * Delta type constant (bit mask) indicating that the bundles which this
	 * bundle optionally requires have changed in the new state.
	 * @see BundleDelta#getType
	 */
	public static final int OPTIONAL_LINKAGE_CHANGED = 0x40;

	/**
	 * 
	 * @return
	 */
	public BundleDescription getBundle();

	/**
	 * Returns the type of change which occured.  The return value is composed
	 * of by bit-wise masking the relevant flags from the set ADDED, REMOVED, 
	 * CHANGED, RESOLVED, UNRESOLVED, LINKAGE_CHANGED. 
	 * Note that bundle start and stop state changes are not captured in the 
	 * delta as they do not represent structural changes but rather transient
	 * runtime states.
	 * @return the type of change which occured
	 */
	public int getType();

}