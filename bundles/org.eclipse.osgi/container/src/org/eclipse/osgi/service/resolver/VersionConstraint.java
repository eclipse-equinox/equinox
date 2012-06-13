/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import org.osgi.framework.wiring.BundleRequirement;

/**
 * VersionConstraints represent the relationship between two bundles (in the 
 * case of bundle requires) or a bundle and a package (in the case of import/export).
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface VersionConstraint extends Cloneable {

	/**
	 * Returns this constraint's name.
	 * 
	 * @return this constraint's name
	 */
	public String getName();

	/**
	 * Returns the version range for this constraint.
	 * @return the version range for this constraint, or <code>null</code>
	 */
	public VersionRange getVersionRange();

	/**
	 * Returns the bundle that declares this constraint.
	 * 
	 * @return a bundle description
	 */
	public BundleDescription getBundle();

	/**
	 * Returns whether this constraint is resolved. A resolved constraint 
	 * is guaranteed to have its supplier defined. 
	 * 
	 * @return <code>true</code> if this bundle is resolved, <code>false</code> 
	 * otherwise
	 */
	public boolean isResolved();

	/**
	 * Returns whether this constraint could be satisfied by the given supplier.
	 * This will depend on the suppliers different attributes including its name,
	 * versions and other arbitrary attributes
	 * 
	 * @param supplier a supplier to be tested against this constraint (may be 
	 * <code>null</code>)
	 * @return <code>true</code> if this constraint could be resolved using the supplier, 
	 * <code>false</code> otherwise 
	 */
	public boolean isSatisfiedBy(BaseDescription supplier);

	/**
	 * Returns the supplier that satisfies this constraint, if it is resolved.
	 *  
	 * @return a supplier, or <code>null</code> 
	 * @see #isResolved()
	 */
	public BaseDescription getSupplier();

	/**
	 * Returns the requirement represented by this constraint.
	 * Some constraint types may not be able to represent 
	 * a requirement.  In such cases <code>null</code> is
	 * returned.
	 * @return the requirement represented by this constraint
	 * @since 3.7
	 */
	public BundleRequirement getRequirement();

	/**
	 * Returns the user object associated to this constraint, or 
	 * <code>null</code> if none exists.
	 *  
	 * @return the user object associated to this constraint,
	 * or <code>null</code>
	 * @since 3.8
	 */
	public Object getUserObject();

	/**
	 * Associates a user-provided object to this constraint, or
	 * removes an existing association, if <code>null</code> is provided. The 
	 * provided object is not interpreted in any ways by this 
	 * constrain.
	 * 
	 * @param userObject an arbitrary object provided by the user, or 
	 * <code>null</code>
	 * @since 3.8
	 */
	public void setUserObject(Object userObject);
}
