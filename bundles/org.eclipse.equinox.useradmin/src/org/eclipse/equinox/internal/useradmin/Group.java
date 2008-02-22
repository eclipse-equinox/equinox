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

import java.util.Enumeration;
import java.util.Vector;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A named grouping of roles.
 * <p>
 * Whether or not a given authorization context implies a Group role
 * depends on the members of that role.
 * <p>
 * A Group role can have two kinds of member roles: <i>basic</i> and
 * <i>required</i>.
 * A Group role is implied by an authorization context if all of
 * its required member roles are implied
 * and at least one of its basic member roles is implied.
 * <p>
 * A Group role must contain at least one basic member role in order
 * to be implied. In other words, a Group without any basic member
 * roles is never implied by any authorization context.
 * <p>
 * A User role always implies itself.
 * <p>
 * No loop detection is performed when adding members to groups, which
 * means that it is possible to create circular implications. Loop
 * detection is instead done when roles are checked. The semantics is that
 * if a role depends on itself (i.e., there is an implication loop), the
 * role is not implied.
 * <p>
 * The rule that a group must have at least one basic member to be implied
 * is motivated by the following example:
 *
 * <pre>
 * group foo
 *   required members: marketing
 *   basic members: alice, bob
 * </pre>
 *
 * Privileged operations that require membership in "foo" can be performed
 * only by alice and bob, who are in marketing.
 * <p>
 * If alice and bob ever transfer to a different department, anybody in
 * marketing will be able to assume the "foo" role, which certainly must be
 * prevented.
 * Requiring that "foo" (or any Group role for that matter) must have at least
 * one basic member accomplishes that.
 * <p>
 * However, this would make it impossible for a group to be implied by just
 * its required members. An example where this implication might be useful
 * is the following declaration: "Any citizen who is an adult is allowed to
 * vote."
 * An intuitive configuration of "voter" would be:
 *
 * <pre>
 * group voter
 *   required members: citizen, adult
 *      basic members:
 * </pre>
 *
 * However, according to the above rule, the "voter" role could never be
 * assumed by anybody, since it lacks any basic members.
 * In order to address this deficiency a predefined role named
 * "user.anyone" can be specified, which is always implied.
 * The desired implication of the "voter" group can then be achieved by
 * specifying "user.anyone" as its basic member, as follows:
 *
 * <pre>
 * group voter
 *   required members: citizen, adult
 *      basic members: user.anyone
 * </pre>
 */

public class Group extends User implements org.osgi.service.useradmin.Group {

	protected Vector requiredMembers;
	protected Vector basicMembers;

	protected Group(String name, UserAdmin useradmin) {
		super(name, useradmin);
		this.useradmin = useradmin;
		basicMembers = new Vector();
		requiredMembers = new Vector();
	}

	/**
	 * Adds the specified role as a basic member to this Group.
	 *
	 * @param role The role to add as a basic member.
	 *
	 * @return <code>true</code> if the given role could be added as a basic
	 * member,
	 * and <code>false</code> if this Group already contains a role whose name
	 * matches that of the specified role.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
	 */
	public boolean addMember(org.osgi.service.useradmin.Role role) {
		useradmin.checkAlive();
		useradmin.checkAdminPermission();
		//only need to check for null for the public methods
		if (role == null) {
			return (false);
		}
		synchronized (useradmin) {
			if (basicMembers.contains(role)) {
				return (false);
			}
			return (addMember(role, true));
		}
	}

	// When we are loading from storage this method is called directly.  We
	// do not want to write to storage when we are loading form storage.
	protected boolean addMember(org.osgi.service.useradmin.Role role, boolean store) {
		((org.eclipse.equinox.internal.useradmin.Role) role).addImpliedRole(this);
		if (store) {
			try {
				useradmin.userAdminStore.addMember(this, (org.eclipse.equinox.internal.useradmin.Role) role);
			} catch (BackingStoreException ex) {
				return (false);
			}
		}
		basicMembers.addElement(role);
		return (true);
	}

	/**
	 * Adds the specified role as a required member to this Group.
	 *
	 * @param role The role to add as a required member.
	 *
	 * @return <code>true</code> if the given role could be added as a required
	 * member, and <code>false</code> if this Group already contains a role
	 * whose name matches that of the specified role.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
	 */
	public boolean addRequiredMember(org.osgi.service.useradmin.Role role) {
		useradmin.checkAlive();
		useradmin.checkAdminPermission();
		if (role == null) {
			return (false);
		}
		synchronized (useradmin) {
			if (requiredMembers.contains(role)) {
				return (false);
			}
			return (addRequiredMember(role, true));
		}
	}

	protected boolean addRequiredMember(org.osgi.service.useradmin.Role role, boolean store) {
		((org.eclipse.equinox.internal.useradmin.Role) role).addImpliedRole(this);
		if (store) {
			try {
				useradmin.userAdminStore.addRequiredMember(this, (org.eclipse.equinox.internal.useradmin.Role) role);
			} catch (BackingStoreException ex) {
				return (false);
			}
		}
		requiredMembers.addElement(role);
		return (true);
	}

	/**
	 * Removes the specified role from this Group.
	 *
	 * @param role The role to remove from this Group.
	 *
	 * @return <code>true</code> if the role could be removed,
	 * otherwise <code>false</code>.
	 *
	 * @throws SecurityException If a security manager exists and the caller
	 * does not have the <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
	 */
	public boolean removeMember(org.osgi.service.useradmin.Role role) {
		useradmin.checkAlive();
		useradmin.checkAdminPermission();
		if (role == null) {
			return (false);
		}
		synchronized (useradmin) {
			try {
				useradmin.userAdminStore.removeMember(this, (org.eclipse.equinox.internal.useradmin.Role) role);
			} catch (BackingStoreException ex) {
				return (false);
			}
			//The role keeps track of which groups it is a member of so it can remove itself from
			//the group if it is deleted.  In this case, this group is being removed from the role's
			//list.
			((org.eclipse.equinox.internal.useradmin.Role) role).removeImpliedRole(this);

			// We don't know if the Role to be removed is a basic orrequired member, or both.  We
			// simply try to remove it from both.
			boolean removeRequired = requiredMembers.removeElement(role);
			boolean removeBasic = basicMembers.removeElement(role);
			return (removeRequired || removeBasic);
		}
	}

	/**
	 * Gets the basic members of this Group.
	 *
	 * @return The basic members of this Group, or <code>null</code> if this
	 * Group does not contain any basic members.
	 */
	public org.osgi.service.useradmin.Role[] getMembers() {
		useradmin.checkAlive();
		synchronized (useradmin) {
			if (basicMembers.isEmpty()) {
				return (null);
			}
			Role[] roles = new Role[basicMembers.size()];
			basicMembers.copyInto(roles);
			return (roles);
		}
	}

	/**
	 * Gets the required members of this Group.
	 *
	 * @return The required members of this Group, or <code>null</code> if this
	 * Group does not contain any required members.
	 */
	public org.osgi.service.useradmin.Role[] getRequiredMembers() {
		useradmin.checkAlive();
		synchronized (useradmin) {
			if (requiredMembers.isEmpty()) {
				return (null);
			}
			Role[] roles = new Role[requiredMembers.size()];
			requiredMembers.copyInto(roles);
			return (roles);
		}
	}

	/**
	 * Returns the type of this role.
	 *
	 * @return The role's type.
	 */
	public int getType() {
		useradmin.checkAlive();
		return (org.osgi.service.useradmin.Role.GROUP);
	}

	protected boolean isImpliedBy(Role role, Vector checkLoop) {
		if (checkLoop.contains(name)) {
			//we have a circular dependency
			return (false);
		}
		if (name.equals(role.getName())) //A User always implies itself.  A Group is a User.
		{
			return (true);
		}
		checkLoop.addElement(name);
		Vector requiredCheckLoop = (Vector) checkLoop.clone();
		Vector basicCheckLoop = (Vector) checkLoop.clone();
		Enumeration e = requiredMembers.elements();

		//check to see if we imply all of the 0 or more required roles
		Role requiredRole;
		while (e.hasMoreElements()) {
			requiredRole = (Role) e.nextElement();
			if (!requiredRole.isImpliedBy(role, requiredCheckLoop)) {
				return (false);
			}
		}
		//check to see if we imply any of the basic roles (there must be at least one)
		e = basicMembers.elements();
		Role basicRole;
		while (e.hasMoreElements()) {
			basicRole = (Role) e.nextElement();
			if (basicRole.isImpliedBy(role, basicCheckLoop)) {
				return (true);
			}
		}
		return (false);
	}

}
