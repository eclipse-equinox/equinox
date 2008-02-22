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
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * The base interface for Role objects managed by the {@link UserAdmin}
 * service.
 * <p>
 * This interface exposes the characteristics shared by all Roles: a name,
 * a type, and a set of properties.
 * <p>
 * Properties represent public information about the Role that can be read by
 * anyone. Specific {@link UserAdminPermission}s are required to
 * change a Role's properties.
 * <p>
 * Role properties are Dictionary objects. Changes to
 * these objects are propagated to the {@link UserAdmin} service and
 * made persistent.
 * <p>
 * Every UserAdmin contains a set of predefined roles that are always present
 * and cannot be removed. All predefined roles are of type <tt>ROLE</tt>.
 * This version of the <tt>org.osgi.service.useradmin</tt> package defines a
 * single predefined role named &quot;user.anyone&quot;, which is inherited
 * by any other role. Other predefined roles may be added in the future.
 */

public class Role implements org.osgi.service.useradmin.Role {

	protected String name;
	protected UserAdminHashtable properties;
	protected Vector impliedRoles;
	protected UserAdmin useradmin;
	protected static final String anyoneString = "user.anyone"; //$NON-NLS-1$
	protected boolean exists = true;

	protected Role(String name, UserAdmin useradmin) {
		this.name = name;
		this.properties = new UserAdminHashtable(this, useradmin, UserAdminHashtable.PROPERTIES);
		this.useradmin = useradmin;

		// This is used only to track which Groups this role is directly a member of.
		// This info is needed so when we delete a Role, we know which groups to remove
		// it from.
		impliedRoles = new Vector();
	}

	/**
	 * Returns the name of this role.
	 *
	 * @return The role's name.
	 */

	public String getName() {
		useradmin.checkAlive();
		return (name);
	}

	/**
	 * Returns the type of this role.
	 *
	 * @return The role's type.
	 */
	public int getType() {
		useradmin.checkAlive();
		return (org.osgi.service.useradmin.Role.ROLE);
	}

	/**
	 * Returns a Dictionary of the (public) properties of this Role. Any changes
	 * to the returned Dictionary will change the properties of this Role. This
	 * will cause a UserAdminEvent of type {@link UserAdminEvent#ROLE_CHANGED}
	 * to be broadcast to any UserAdminListeners.
	 * <p>
	 * Only objects of type <tt>String</tt> may be used as property keys, and
	 * only objects of type <tt>String</tt> or <tt>byte[]</tt>
	 * may be used as property values.
	 * Any other types will cause an exception of type
	 * <tt>IllegalArgumentException</tt> to be raised.
	 * <p>
	 * In order to add, change, or remove a property in the returned Dictionary,
	 * a {@link UserAdminPermission} named after the property name (or
	 * a prefix of it) with action <code>changeProperty</code> is required.
	 *
	 * @return Dictionary containing the properties of this Role.
	 */
	public Dictionary getProperties() {
		useradmin.checkAlive();
		return (properties);
	}

	protected void addImpliedRole(Group group) {
		impliedRoles.addElement(group);
	}

	protected void removeImpliedRole(Group group) {
		if (exists) //this prevents a loop when destroy is called
		{
			impliedRoles.removeElement(group);
		}
	}

	//we are being deleted so delete ourselves from all of the groups
	protected synchronized void destroy() {
		exists = false;
		Enumeration e = impliedRoles.elements();
		while (e.hasMoreElements()) {
			Group group = (Group) e.nextElement();
			if (group.exists) //so we don't try to remove any groups twice from storage   
			{
				group.removeMember(this);
			}
		}
		properties = null;
		impliedRoles = null;
	}

	protected boolean isImpliedBy(Role role, Vector checkLoop) { //Roles do not imply themselves
		//The user.anyone role is always implied
		if (checkLoop.contains(name)) {
			//we have a circular dependency
			return (false);
		}
		checkLoop.addElement(name);
		return (name.equals(Role.anyoneString));
	}

}
