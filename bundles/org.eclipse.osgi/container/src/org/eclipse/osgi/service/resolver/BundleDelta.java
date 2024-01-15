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
 * BundleDeltas represent the changes related to an individual bundle between
 * two states.
 * <p>
 * This interface is not intended to be implemented by clients. The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * 
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface BundleDelta extends Comparable<BundleDelta> {

	/**
	 * Delta type constant (bit mask) indicating that the bundle has been added to
	 * the new state.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int ADDED = 0x1;
	/**
	 * Delta type constant (bit mask) indicating that the bundle is no longer
	 * present in the new state.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int REMOVED = 0x2;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has been updated
	 * between the old and new state. Note that an update delta may in fact
	 * represent a downgrading of the bundle to a previous version.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int UPDATED = 0x4;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has become resolved
	 * in the new state.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int RESOLVED = 0x8;
	/**
	 * Delta type constant (bit mask) indicating that the bundle has become
	 * unresolved in the new state. Note that newly added bundles are unresolved by
	 * default and as such, do not transition to unresolved state so this flag is
	 * not set.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int UNRESOLVED = 0x10;
	/**
	 * Delta type constant (bit mask) indicating that the bundles and packages which
	 * this bundle requires/imports (respectively) have changed in the new state.
	 * 
	 * @see BundleDelta#getType
	 * @deprecated this type is no longer valid
	 */
	public static final int LINKAGE_CHANGED = 0x20;

	/**
	 * Delta type constant (bit mask) indicating that the bundles which this bundle
	 * optionally requires have changed in the new state.
	 * 
	 * @see BundleDelta#getType
	 * @deprecated this type is no longer valid
	 */
	public static final int OPTIONAL_LINKAGE_CHANGED = 0x40;

	/**
	 * Delta type constant (bit mask) indicating that the this bundle is pending a
	 * removal. Note that bundles with this flag set will also have the
	 * {@link BundleDelta#REMOVED} flag set. A bundle will have this flag set if it
	 * has been removed from the state but has other existing bundles in the state
	 * that depend on it.
	 * 
	 * @see BundleDelta#getType
	 */
	public static final int REMOVAL_PENDING = 0x80;

	/**
	 * Delta type constant (bit mask) indicating that the this bundle has completed
	 * a pending removal. A bundle will complete a pending removal only after it has
	 * been re-resolved by the resolver.
	 */
	public static final int REMOVAL_COMPLETE = 0x100;

	/**
	 * Returns the BundleDescription that this bundle delta is for.
	 * 
	 * @return the BundleDescription that this bundle delta is for.
	 */
	public BundleDescription getBundle();

	/**
	 * Returns the type of change which occured. The return value is composed of by
	 * bit-wise masking the relevant flags from the set ADDED, REMOVED, UPDATED,
	 * RESOLVED, UNRESOLVED, LINKAGE_CHANGED, REMOVAL_PENDING, REMOVAL_COMPLETE.
	 * Note that bundle start and stop state changes are not captured in the delta
	 * as they do not represent structural changes but rather transient runtime
	 * states.
	 * 
	 * @return the type of change which occured
	 */
	public int getType();

	/**
	 * Answers an integer indicating the relative positions of the receiver and the
	 * argument in the natural order of elements of the receiver's class.
	 * <p>
	 * The natural order of elements is determined by the bundle id of the
	 * BundleDescription that this bundle delta is for.
	 *
	 * @return int which should be &lt;0 if the receiver should sort before the
	 *         argument, 0 if the receiver should sort in the same position as the
	 *         argument, and &gt;0 if the receiver should sort after the argument.
	 * @param obj another BundleDelta an object to compare the receiver to
	 * @exception ClassCastException if the argument can not be converted into
	 *                               something comparable with the receiver.
	 * @since 3.7
	 */
	@Override
	public int compareTo(BundleDelta obj);
}
