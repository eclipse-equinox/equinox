/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.datalocation;

import java.net.URL;

/**
 * A Location represents a URL which may have a default value, may be read only, may 
 * or may not have a current value and may be cascaded on to a parent location.
 * @since 3.0
 */
public interface Location {
	/**
	 * Returns true if this location allows a default value to be assigned
	 * @return whether or not this location can assign a default value
	 */
	public boolean allowsDefault();	
	
	/**
	 * Returns the parent of this location or <code>null</code> if none is available.
	 * @return the parent of this location or <code>null</code>
	 */
	public Location getParentLocation();
	
	/**
	 * Returns the actual URL of this location.  If the location's value has been set, 
	 * that value is returned.  If the value is not set and the location allows defaults, 
	 * the value is set to the default and returned.  In all other cases <code>null</code>
	 * is returned.
	 * @return the URL for this location or <code>null</code> if none
	 */
	public URL getURL();
	
	/**
	 * Returns true if this location has a value. 
	 * @return whether or not the value is set
	 */
	public boolean isSet();
	/**
	 * Return true if this locaiton represents a read only location.  The read only character
	 * of a location is not in enforced in any way but rather expresses the intention of the
	 * location's creator.
	 * @return whether the location is read only
	 */
	public boolean isReadOnly();
	
	/**
	 * Sets the location's value to the given URL.  If the location already has a value an 
	 * exception is thrown.
	 * @param value the value of this location
	 * @throws IllegalStateException if the location's value is already set
	 */
	public void setURL(URL value) throws IllegalStateException;
}
