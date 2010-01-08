/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.datalocation;

import java.io.IOException;
import java.net.URL;

/**
 * A Location represents a URL which may have a default value, may be read only, may 
 * or may not have a current value and may be cascaded on to a parent location.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface Location {

	/**
	 * Constant which defines the filter string for acquiring the service which
	 * specifies the instance location.
	 * 
	 * @since 3.2
	 */
	public static final String INSTANCE_FILTER = "(&(objectClass=" + Location.class.getName() + ")(type=osgi.instance.area))"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Constant which defines the filter string for acquiring the service which
	 * specifies the install location.
	 * 
	 * @since 3.2
	 */
	public static final String INSTALL_FILTER = "(&(objectClass=" + Location.class.getName() + ")(type=osgi.install.area))"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Constant which defines the filter string for acquiring the service which
	 * specifies the configuration location.
	 * 
	 * @since 3.2
	 */
	public static final String CONFIGURATION_FILTER = "(&(objectClass=" + Location.class.getName() + ")(type=osgi.configuration.area))"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Constant which defines the filter string for acquiring the service which
	 * specifies the user location.
	 * 
	 * @since 3.2
	 */
	public static final String USER_FILTER = "(&(objectClass=" + Location.class.getName() + ")(type=osgi.user.area))"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Constant which defines the filter string for acquiring the service which
	 * specifies the eclipse home location.
	 * 
	 * @since 3.4
	 */
	public static final String ECLIPSE_HOME_FILTER = "(&(objectClass=" + Location.class.getName() + ")(type=eclipse.home.location))"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Returns <code>true</code> if this location allows a default value to be assigned
	 * and <code>false</code> otherwise.
	 * 
	 * @return whether or not this location can have a default value assigned
	 */
	public boolean allowsDefault();

	/**
	 * Returns the default value of this location if any.  If no default is available then
	 * <code>null</code> is returned. Note that even locations which allow defaults may still
	 * return <code>null</code>.
	 * 
	 * @return the default value for this location or <code>null</code>
	 */
	public URL getDefault();

	/**
	 * Returns the parent of this location or <code>null</code> if none is available.
	 * 
	 * @return the parent of this location or <code>null</code>
	 */
	public Location getParentLocation();

	/**
	 * Returns the actual {@link URL} of this location.  If the location's value has been set, 
	 * that value is returned.  If the value is not set and the location allows defaults, 
	 * the value is set to the default and returned.  In all other cases <code>null</code>
	 * is returned.
	 * 
	 * @return the URL for this location or <code>null</code> if none
	 */
	public URL getURL();

	/**
	 * Returns <code>true</code> if this location has a value and <code>false</code>
	 * otherwise.
	 * 
	 * @return boolean value indicating whether or not the value is set
	 */
	public boolean isSet();

	/**
	 * Returns <code>true</code> if this location represents a read only location and
	 * <code>false</code> otherwise.  The read only character
	 * of a location is not in enforced in any way but rather expresses the intention of the
	 * location's creator.
	 * 
	 * @return boolean value indicating whether the location is read only
	 */
	public boolean isReadOnly();

	/**
	 * Sets and optionally locks the location's value to the given {@link URL}.  If the location 
	 * already has a value an exception is thrown.  If locking is requested and fails, <code>false</code>
	 * is returned and the {@link URL} of this location is not set.
	 * 
	 * @param value the value of this location
	 * @param lock whether or not to lock this location 
	 * @return whether or not the location was successfully set and, if requested, locked.
	 * @throws IllegalStateException if the location's value is already set
	 * @deprecated use {@link #set(URL, boolean)} instead.
	 */
	public boolean setURL(URL value, boolean lock) throws IllegalStateException;

	/**
	 * Sets and optionally locks the location's value to the given {@link URL}.  If the location 
	 * already has a value an exception is thrown.  If locking is requested and fails, <code>false</code>
	 * is returned and the {@link URL} of this location is not set.
	 * 
	 * @param value the value of this location
	 * @param lock whether or not to lock this location 
	 * @return whether or not the location was successfully set and, if requested, locked.
	 * @throws IllegalStateException if the location's value is already set
	 * @throws IOException if there was an unexpected problem while acquiring the lock
	 * @since 3.4
	 */
	public boolean set(URL value, boolean lock) throws IllegalStateException, IOException;

	/**
	 * Sets and optionally locks the location's value to the given {@link URL} using the given lock file.  If the location 
	 * already has a value an exception is thrown.  If locking is requested and fails, <code>false</code>
	 * is returned and the {@link URL} of this location is not set.
	 * 
	 * @param value the value of this location
	 * @param lock whether or not to lock this location 
	 * @param lockFilePath the path to the lock file.  This path will be used to establish locks on this location.
	 * The path may be an absolute path or it may be relative to the given URL.  If a <code>null</code>
	 * value is used then a default lock path will be used for this location.
	 * @return whether or not the location was successfully set and, if requested, locked.
	 * @throws IllegalStateException if the location's value is already set
	 * @throws IOException if there was an unexpected problem while acquiring the lock
	 * @since 3.5
	 */
	public boolean set(URL value, boolean lock, String lockFilePath) throws IllegalStateException, IOException;

	/**
	 * Attempts to lock this location with a canonical locking mechanism and return
	 * <code>true</code> if the lock could be acquired.  Not all locations can be 
	 * locked.
	 * <p>
	 * Locking a location is advisory only.  That is, it does not prevent other applications from 
	 * modifying the same location
	 * </p>
	 * @return true if the lock could be acquired; otherwise false is returned
	 * 
	 * @exception IOException if there was an unexpected problem while acquiring the lock
	 */
	public boolean lock() throws IOException;

	/**
	 * Releases the lock on this location.  If the location is not already locked, no action 
	 * is taken.
	 */
	public void release();

	/**
	 * Returns <code>true</code> if this location is locked and <code>false</code>
	 * otherwise.
	 * @return boolean value indicating whether or not this location is locked
	 * @throws IOException if there was an unexpected problem reading the lock
	 * @since 3.4
	 */
	public boolean isLocked() throws IOException;

	/**
	 * Constructs a new location.
	 * @param parent the parent location.  A <code>null</code> value is allowed.
	 * @param defaultValue the default value of the location. A <code>null</code> value is allowed.
	 * @param readonly true if the location is read-only.
	 * @return a new location.
	 * @since 3.4
	 */
	public Location createLocation(Location parent, URL defaultValue, boolean readonly);

	/**
	 * Returns a URL to the specified path within this location.  The path 
	 * of the returned URL may not exist yet.  It is the responsibility of the 
	 * client to create the content of the data area returned if it does not exist.
	 * <p>
	 * This method can be used to obtain a private area within the given location. 
	 * For example use the symbolic name of a bundle to obtain a data area specific 
	 * to that bundle.
	 * </p>
	 * <p>
	 * Clients should check if the location is read only before writing anything
	 * to the returned data area.  An <code>IOException</code> will be thrown if
	 * this method is called and the location URL has not been set and there is
	 * no default value for this location.
	 * </p>
	 * 
	 * @param path the name of the path to get from this location
	 * @return the URL to the data area with the specified path.
	 * @throws IOException if the location URL is not already set
	 * @since 3.6
	 */
	public URL getDataArea(String path) throws IOException;
}
