/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.useradmin;

import java.util.Dictionary;
import java.util.Vector;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * A User managed by a {@link UserAdmin} service.
 * <p>
 * In this context, the term &quot;user&quot; is not limited to just
 * human beings.
 * Instead, it refers to any entity that may have any number of
 * credentials associated with it that it may use to authenticate itself.
 * <p>
 * In general, User objects are associated with a specific {@link UserAdmin}
 * service (namely the one that created them), and cannot be used with other
 * UserAdmin services.
 * <p>
 * A User may have credentials (and properties, inherited from {@link Role})
 * associated with it. Specific {@link UserAdminPermission}s are required to
 * read or change a User's credentials.
 * <p>
 * Credentials are Dictionary objects and have semantics that are similar
 * to the properties in Role.
 */

public class User extends Role implements org.osgi.service.useradmin.User {

	protected UserAdminHashtable credentials;

	protected User(String name, UserAdmin useradmin) {
		super(name, useradmin);
		this.useradmin = useradmin;
		credentials = new UserAdminHashtable(this, useradmin, UserAdminHashtable.CREDENTIALS);
	}

	/**
	 * Returns a Dictionary of the credentials of this User. Any changes
	 * to the returned Dictionary will change the credentials of this User.
	 * This will cause a UserAdminEvent of type
	 * {@link UserAdminEvent#ROLE_CHANGED} to be broadcast to any
	 * UserAdminListeners.
	 * <p>
	 * Only objects of type String may be used as credential keys, and only
	 * objects of type <code>String</code> or of type <code>byte[]</code>
	 * may be used as credential values. Any other types will cause an exception
	 * of type <code>IllegalArgumentException</code> to be raised.
	 * <p>
	 * In order to retrieve a credential from the returned Dictionary,
	 * a {@link UserAdminPermission} named after the credential name (or
	 * a prefix of it) with action <code>getCredential</code> is required.
	 * <p>
	 * In order to add or remove a credential from the returned Dictionary,
	 * a {@link UserAdminPermission} named after the credential name (or
	 * a prefix of it) with action <code>changeCredential</code> is required.
	 *
	 * @return Dictionary containing the credentials of this User.
	 */

	public Dictionary getCredentials() {
		useradmin.checkAlive();
		return (credentials);
	}

	/**
	 * Checks to see if this User has a credential with the specified key
	 * set to the specified value.
	 * <p>
	 * If the specified credential value is not of type <tt>String</tt> or
	 * <tt>byte[]</tt>, it is ignored, that is, <tt>false</tt> is returned
	 * (as opposed to an <tt>IllegalArgumentException</tt> being raised).
	 *
	 * @param key The credential key.
	 * @param value The credential value.
	 *
	 * @return <code>true</code> if this user has the specified credential;
	 * <code>false</code> otherwise.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> named after the credential
	 * key (or a prefix of it) with action <code>getCredential</code>.
	 */
	public boolean hasCredential(String key, Object value) {
		useradmin.checkAlive();
		Object checkValue = credentials.get(key);
		if (checkValue != null) {
			if (value instanceof String) {
				if (checkValue.equals(value)) {
					return (true);
				}
			} else if (value instanceof byte[]) {
				if (!(checkValue instanceof byte[]))
					return (false);
				byte[] valueArray = (byte[]) value;
				byte[] checkValueArray = (byte[]) checkValue;
				int length = valueArray.length;
				if (length != checkValueArray.length) {
					return (false);
				}
				for (int i = 0; i < length; i++) {
					if (valueArray[i] != checkValueArray[i]) {
						return (false);
					}
				}
				return (true);
			}
		}
		return (false); //if checkValue is null	
	}

	/**
	 * Returns the type of this role.
	 *
	 * @return The role's type.
	 */
	public int getType() {
		useradmin.checkAlive();
		return org.osgi.service.useradmin.Role.USER;
	}

	//A user always implies itself
	protected boolean isImpliedBy(Role role, Vector checkLoop) {
		if (checkLoop.contains(name)) {
			//we have a circular dependency
			return (false);
		}
		checkLoop.addElement(name);
		return ((role.getName()).equals(name));
	}

}
