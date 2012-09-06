/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * A specification which depends on a generic capability
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface GenericSpecification extends VersionConstraint {
	/**
	 * The optional resolution type
	 * @see #getResolution()
	 */
	public static final int RESOLUTION_OPTIONAL = 0x01;
	/**
	 * The multiple resolution type
	 * @see #getResolution()
	 */
	public static final int RESOLUTION_MULTIPLE = 0x02;

	/**
	 * Returns a matching filter used to match with a suppliers attributes
	 * @return a matching filter used to match with a suppliers attributes
	 */
	public String getMatchingFilter();

	/**
	 * Returns the type of generic specification
	 * @return the type of generic specification
	 */
	public String getType();

	/**
	 * Returns the resolution type of the required capability.  The returned
	 * value is a bit mask that may have the optional bit {@link #RESOLUTION_OPTIONAL}
	 * and/or the multiple bit {@link #RESOLUTION_MULTIPLE} set.
	 * 
	 * @return the resolution type of the required capability
	 */
	public int getResolution();

	/**
	 * Returns the suppliers of the capability.  If the the resolution is multiple then
	 * more than one supplier may be returned
	 * @return the suppliers of the capability
	 */
	public GenericDescription[] getSuppliers();
}
