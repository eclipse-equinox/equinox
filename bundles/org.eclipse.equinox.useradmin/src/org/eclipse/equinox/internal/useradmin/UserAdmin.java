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

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * This interface is used to manage a database of named roles, which can
 * be used for authentication and authorization purposes.
 * <p>
 * This version of UserAdmin defines two types of roles: "User" and
 * "Group". Each type of role is represented by an "int" constant and an
 * interface. The range of positive integers is reserved for new types of
 * roles that may be added in the future. When defining proprietary role
 * types, negative constant values must be used.
 * <p>
 * Every role has a name and a type.
 * <p>
 * A {@link User} role can be configured with credentials (e.g., a password)
 * and properties (e.g., a street address, phone number, etc.).
 * <p>
 * A {@link Group} role represents an aggregation of {@link User} and
 * {@link Group} roles. In
 * other words, the members of a Group role are roles themselves.
 * <p>
 * Every UserAdmin manages and maintains its own
 * namespace of roles, in which each role has a unique name.
 */

public class UserAdmin implements org.osgi.service.useradmin.UserAdmin {

	protected Vector users;
	protected Vector roles;
	protected BundleContext context;
	protected UserAdminEventProducer eventProducer;
	protected boolean alive;
	protected UserAdminStore userAdminStore;
	protected UserAdminPermission adminPermission;
	protected ServiceReference reference;
	protected LogTracker log;

	protected UserAdmin(PreferencesService preferencesService, BundleContext context) throws Exception {
		roles = new Vector();
		users = new Vector();
		this.context = context;

		log = new LogTracker(context, System.out);
		alive = true;
		//This handles user admin persistence
		try {
			userAdminStore = new UserAdminStore(preferencesService, this, log);
			userAdminStore.init();
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, UserAdminMsg.Backing_Store_Read_Exception, e);
			throw e;
		}
	}

	protected void setServiceReference(ServiceReference reference) {
		if (this.reference == null) {
			this.reference = reference;

			eventProducer = new UserAdminEventProducer(reference, context, log);
		}
	}

	/**
	 * Creates a role with the given name and of the given type.
	 *
	 * <p> If a role was created, a UserAdminEvent of type
	 * {@link UserAdminEvent#ROLE_CREATED} is broadcast to any
	 * UserAdminListener.
	 *
	 * @param name The name of the role to create.
	 * @param type The type of the role to create. Must be either
	 * {@link Role#USER} or {@link Role#GROUP}.
	 *
	 * @return The newly created role, or <code>null</code> if a role with
	 * the given name already exists.
	 *
	 * @throws IllegalArgumentException if <tt>type</tt> is invalid.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
	 */
	public org.osgi.service.useradmin.Role createRole(String name, int type) {
		checkAlive();
		checkAdminPermission();
		if (name == null) {
			throw (new IllegalArgumentException(UserAdminMsg.CREATE_NULL_ROLE_EXCEPTION));
		}
		if ((type != org.osgi.service.useradmin.Role.GROUP) && (type != org.osgi.service.useradmin.Role.USER)) {
			throw (new IllegalArgumentException(UserAdminMsg.CREATE_INVALID_TYPE_ROLE_EXCEPTION));
		}
		//if the role already exists, return null
		if (getRole(name) != null) {
			return (null);
		}

		synchronized (this) {
			return createRole(name, type, true);
		}
	}

	protected org.osgi.service.useradmin.Role createRole(String name, int type, boolean store) {
		Role newRole = null;
		if (type == org.osgi.service.useradmin.Role.ROLE) {
			newRole = new Role(name, this);
		} else if (type == org.osgi.service.useradmin.Role.USER) {
			newRole = new User(name, this);
		} else if (type == org.osgi.service.useradmin.Role.GROUP) {
			newRole = new Group(name, this);
		} else //unknown type
		{
			return (null);
		}
		if (store) {
			try {
				userAdminStore.addRole(newRole);
			} catch (BackingStoreException ex) {
				return (null);
			}
			if (eventProducer != null) {
				eventProducer.generateEvent(UserAdminEvent.ROLE_CREATED, newRole);
			}
		}
		if (type == org.osgi.service.useradmin.Role.GROUP || type == org.osgi.service.useradmin.Role.USER) {
			users.addElement(newRole);
		}
		roles.addElement(newRole);
		return (newRole);
	}

	/**
	 * Removes the role with the given name from this UserAdmin.
	 *
	 * <p> If the role was removed, a UserAdminEvent of type
	 * {@link UserAdminEvent#ROLE_REMOVED} is broadcast to any
	 * UserAdminListener.
	 *
	 * @param name The name of the role to remove.
	 *
	 * @return <code>true</code> If a role with the given name is present in this
	 * UserAdmin and could be removed, otherwise <code>false</code>.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
	 */
	public boolean removeRole(String name) {
		checkAlive();
		checkAdminPermission();
		if (name.equals(Role.anyoneString)) {
			//silently ignore
			return (true);
		}
		synchronized (this) {
			Role role = (org.eclipse.equinox.internal.useradmin.Role) getRole(name);
			if (role != null) {
				try {
					userAdminStore.removeRole(role);
				} catch (BackingStoreException ex) {
					return (false);
				}
				roles.removeElement(role);
				users.removeElement(role);
				role.destroy();
				eventProducer.generateEvent(UserAdminEvent.ROLE_REMOVED, role);
				role = null;
				return (true);
			}
			return (false);
		}
	}

	/**
	 * Gets the role with the given name from this UserAdmin.
	 *
	 * @param name The name of the role to get.
	 *
	 * @return The requested role, or <code>null</code> if this UserAdmin does
	 * not have a role with the given name.
	 */
	public org.osgi.service.useradmin.Role getRole(String name) {
		checkAlive();
		if (name == null) {
			return (null);
		}
		synchronized (this) {
			Enumeration e = roles.elements();
			while (e.hasMoreElements()) {
				Role role = (Role) e.nextElement();
				if (role.getName().equals(name)) {
					return (role);
				}
			}
			return (null);
		}
	}

	/**
	 * Gets the roles managed by this UserAdmin that have properties matching
	 * the specified LDAP filter criteria. See
	 * <code>org.osgi.framework.Filter</code> or IETF RFC 2254 for a
	 * description of the filter syntax. If a <code>null</code> filter is
	 * specified, all roles managed by this UserAdmin are returned.
	 *
	 * @param filter The filter criteria to match.
	 *
	 * @return The roles managed by this UserAdmin whose properties
	 * match the specified filter criteria, or all roles if a
	 * <code>null</code> filter is specified.
	 *
	 */
	public org.osgi.service.useradmin.Role[] getRoles(String filterString) throws InvalidSyntaxException {
		checkAlive();
		Vector returnedRoles;
		synchronized (this) {
			if (filterString == null) {
				returnedRoles = roles;
			} else {
				Filter filter = context.createFilter(filterString); //We do this first so an
				//InvalidSyntaxException will be
				//thrown even if there are no roles
				//present.
				returnedRoles = new Vector();
				for (int i = 0; i < roles.size(); i++) {
					Role role = (Role) roles.elementAt(i);
					if (filter.match(role.getProperties())) {
						returnedRoles.addElement(role);
					}
				}
			}
			int size = returnedRoles.size();
			if (size == 0) {
				return (null);
			}
			Role[] roleArray = new Role[size];
			returnedRoles.copyInto(roleArray);
			return (roleArray);
		}
	}

	/**
	 * Gets the user with the given property key-value pair from the UserAdmin
	 * database. This is a convenience method for retrieving a user based on
	 * a property for which every user is supposed to have a unique value
	 * (within the scope of this UserAdmin), such as a user's
	 * X.500 distinguished name.
	 *
	 * @param key The property key to look for.
	 * @param value The property value to compare with.
	 *
	 * @return A matching user, if <em>exactly</em> one is found. If zero or
	 * more than one matching users are found, <code>null</code> is returned.
	 */
	public org.osgi.service.useradmin.User getUser(String key, String value) {
		checkAlive();
		if (key == null) {
			return (null);
		}
		User user;
		User foundUser = null;
		Dictionary props;
		String keyValue;
		synchronized (this) {
			Enumeration e = users.elements();
			while (e.hasMoreElements()) {
				user = (User) e.nextElement();
				props = user.getProperties();
				keyValue = (String) props.get(key);
				if (keyValue != null && keyValue.equals(value)) {
					if (foundUser != null) {
						return (null); //we found more than one match	
					}
					foundUser = user;
				}
			}
			return (foundUser);
		}
	}

	/**
	 * Creates an Authorization object that encapsulates the specified user
	 * and the roles it possesses. The <code>null</code> user is interpreted
	 * as the anonymous user.
	 *
	 * @param user The user to create an Authorization object for, or
	 * <code>null</code> for the anonymous user.
	 *
	 * @return the Authorization object for the specified user.
	 */
	public org.osgi.service.useradmin.Authorization getAuthorization(org.osgi.service.useradmin.User user) {
		checkAlive();
		return (new Authorization((User) user, this));
	}

	protected synchronized void destroy() {
		alive = false;
		eventProducer.close();
		userAdminStore.destroy();

		log.close();
	}

	public void checkAdminPermission() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			if (adminPermission == null) {
				adminPermission = new UserAdminPermission(UserAdminPermission.ADMIN, null);
			}
			sm.checkPermission(adminPermission);
		}
	}

	public void checkGetCredentialPermission(String credential) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(new org.osgi.service.useradmin.UserAdminPermission(credential, org.osgi.service.useradmin.UserAdminPermission.GET_CREDENTIAL));
		}
	}

	public void checkChangeCredentialPermission(String credential) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(new org.osgi.service.useradmin.UserAdminPermission(credential, org.osgi.service.useradmin.UserAdminPermission.CHANGE_CREDENTIAL));
		}
	}

	public void checkChangePropertyPermission(String property) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(new org.osgi.service.useradmin.UserAdminPermission(property, org.osgi.service.useradmin.UserAdminPermission.CHANGE_PROPERTY));
		}
	}

	public void checkAlive() {
		if (!alive) {
			throw (new IllegalStateException(UserAdminMsg.USERADMIN_UNREGISTERED_EXCEPTION));
		}
	}

}
