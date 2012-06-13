/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.io.IOException;

/**
 * Permission Storage interface for managing a persistent storage of
 * bundle permissions.
 *
 * <p>This class is used to provide methods to manage
 * persistent storage of bundle permissions. The PermissionStorage object
 * is returned by the FrameworkAdaptor object and is called 
 * to persistently store bundle permissions.
 *
 * <p>The permission data will typically take the form of encoded
 * <tt>PermissionInfo</tt> Strings.
 * See org.osgi.service.permissionadmin.PermissionInfo.
 *
 * <p>For example
 * <pre>
 *      PermissionStorage storage = adaptor.getPermissionStorage();
 *      try {
 *          storage.setPermissionData(location, permissions);
 *      } catch (IOException e) {
 *          // Take some error action.
 *      }
 * </pre>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.1
 */
public interface PermissionStorage {
	/**
	 * Returns the locations that have permission data assigned to them,
	 * that is, locations for which permission data
	 * exists in persistent storage.
	 *
	 * @return The locations that have permission data in
	 * persistent storage, or <tt>null</tt> if there is no permission data
	 * in persistent storage.
	 * @throws IOException If a failure occurs accessing persistent storage.
	 */
	public String[] getLocations() throws IOException;

	/**
	 * Gets the permission data assigned to the specified
	 * location.
	 *
	 * @param location The location whose permission data is to
	 * be returned.
	 * The location can be <tt>null</tt> for the default permission data.
	 *
	 * @return The permission data assigned to the specified
	 * location, or <tt>null</tt> if that location has not been assigned any
	 * permission data.
	 * @throws IOException If a failure occurs accessing persistent storage.
	 */
	public String[] getPermissionData(String location) throws IOException;

	/**
	 * Assigns the specified permission data to the specified
	 * location.
	 *
	 * @param location The location that will be assigned the
	 * permissions.
	 * The location can be <tt>null</tt> for the default permission data.
	 * @param data The permission data to be assigned, or <tt>null</tt>
	 * if the specified location is to be removed from persistent storaqe.
	 * @throws IOException If a failure occurs modifying persistent storage.
	 */
	public void setPermissionData(String location, String[] data) throws IOException;

	/**
	 * Persists the array of encoded ConditionalPermissionInfo strings
	 * @param infos an array of encoded ConditionalPermissionInfo strings
	 * @throws IOException If a failure occurs modifying persistent storage.
	 * @since 3.2
	 */
	public void saveConditionalPermissionInfos(String[] infos) throws IOException;

	/**
	 * Returns the persistent array of encoded ConditionalPermissionInfo strings
	 * @return an array of encoded ConditionalPermissionInfo strings or null 
	 * if none exist in persistent storage.
	 * @throws IOException If a failure occurs accessing persistent storage.
	 * @since 3.2
	 */
	public String[] getConditionalPermissionInfos() throws IOException;
}
