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

import java.util.Vector;

/**
 * This interface encapsulates an authorization context on which bundles
 * can base authorization decisions where appropriate.
 * <p>
 * Bundles associate the privilege to access restricted resources or
 * operations with roles. Before granting access to a restricted resource
 * or operation, a bundle will check if the Authorization object passed
 * to it possesses the required role, by calling its hasRole method.
 * <p>
 * Authorization contexts are instantiated by calling
 * {@link UserAdmin#getAuthorization}
 * <p>
 * <font size="+1">Trusting Authorization objects.</font>
 * <p>
 * There are no restrictions regarding the creation of Authorization objects.
 * Hence, a service must only accept Authorization objects from bundles that 
 * has been authorized to use the service using code based (or Java 2) 
 * permissions.
 * <p>
 * In some cases it is useful to use ServicePermissions to do the code based 
 * access control. A service basing user access control on Authorization
 * objects passed to it, will then require that a calling bundle has the
 * ServicePermission to get the service in question. This is the most
 * convenient way. The framework will do the code based permission check
 * when the calling bundle attempts to get the service from the service
 * registry.
 * <p>
 * Example: A servlet using a service on a user's behalf. The bundle with the 
 * servlet must be given the ServicePermission to get the Service.
 * <p>
 * However, in some cases the code based permission checks need to be more 
 * fine-grained. A service might allow all bundles to get it, but 
 * require certain code based permissions for some of its methods.
 * <p>
 * Example: A servlet using a service on a user's behalf, where some 
 * service functionality is open to anyone, and some is restricted by code
 * based permissions. When a restricted method is called 
 * (e.g., one handing over
 * an Authorization object), the service explicitly checks that the calling 
 * bundle has permission to make the call. 
 */
public class Authorization implements org.osgi.service.useradmin.Authorization {

	protected UserAdmin useradmin;
	protected Role user;
	protected String name; //user to distinguish between the anonymous user and user.anyone

	protected Authorization(User user, UserAdmin useradmin) {
		this.useradmin = useradmin;
		if (user != null) {
			this.user = user;
			name = user.getName();
		} else {
			//anonymous user
			this.user = (Role) useradmin.getRole(Role.anyoneString);
			name = null;
		}
	}

	/**
	 * Gets the name of the {@link User} that this Authorization
	 * context was created for.
	 * 
	 * @return The name of the {@link User} that this Authorization
	 * context was created for, or <code>null</code> if no user was specified
	 * when this Authorization context was created.
	 */
	public String getName() {
		useradmin.checkAlive();
		return (name);
	}

	/**
	 * Checks if the role with the specified name is implied by this 
	 * Authorization context.
	 * <p>

	 * Bundles must define globally unique role names that are associated with
	 * the privilege of accessing restricted resources or operations.
	 * System administrators will grant users access to these resources, by
	 * creating a {@link Group} for each role and adding {@link User}s to it.
	 *
	 * @param name The name of the role to check for.
	 *
	 * @return <code>true</code> if this Authorization context implies the
	 * specified role, otherwise <code>false</code>.
	 */
	public boolean hasRole(String name_) {
		useradmin.checkAlive();
		synchronized (useradmin) {
			Role checkRole = (org.eclipse.equinox.internal.useradmin.Role) useradmin.getRole(name_);
			if (checkRole == null) {
				return (false);
			}
			return (checkRole.isImpliedBy(user, new Vector()));
		}
	}

	/**
	 * Gets the names of all roles encapsulated by this Authorization context.
	 *
	 * @return The names of all roles encapsulated by this Authorization 
	 * context, or <code>null</code> if no roles are in the context.
	 */
	public String[] getRoles() {
		useradmin.checkAlive();

		// go through all of the roles and find out which ones are implied by this 
		// authorization context.
		synchronized (useradmin) //we don't want anything changing while we get the list
		{
			int length = useradmin.roles.size();
			Vector result = new Vector(length);
			for (int i = 0; i < length; i++) {
				Role role = (Role) useradmin.roles.elementAt(i);
				if (role.isImpliedBy(user, new Vector())) {
					String roleName = role.getName();
					//exclude user.anyone from the list
					if (!roleName.equals(Role.anyoneString)) {
						result.addElement(roleName);
					}
				}
			}
			int size = result.size();
			if (size == 0) {
				return (null);
			}
			String[] copyrole = new String[size];
			result.copyInto(copyrole);
			return (copyrole);
		}
	}
}
