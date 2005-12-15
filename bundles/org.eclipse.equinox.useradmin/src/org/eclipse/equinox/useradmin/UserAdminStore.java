/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.useradmin;

import java.security.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.*;

/*
 *  UserAdminStore is responsible for managing the persistence data of the useradmin
 *  service.  It uses the PersistenceNode service as its underlying storage.
 */

public class UserAdminStore {

	static protected final String propertiesNode = "properties"; //$NON-NLS-1$
	static protected final String credentialsNode = "credentials"; //$NON-NLS-1$
	static protected final String membersNode = "members"; //$NON-NLS-1$
	static protected final String basicString = "basic"; //$NON-NLS-1$
	static protected final String requiredString = "required"; //$NON-NLS-1$
	static protected final String typeString = "type"; //$NON-NLS-1$
	static protected final String persistenceUserName = "UserAdmin"; //$NON-NLS-1$

	protected ServiceReference prefsRef;
	protected ServiceRegistration userAdminListenerReg;
	protected UserAdmin useradmin;
	protected LogService log;
	protected Preferences rootNode;
	protected PreferencesService preferencesService;

	protected UserAdminStore(PreferencesService preferencesService, UserAdmin useradmin, LogService log) {
		this.preferencesService = preferencesService;
		this.useradmin = useradmin;
		this.log = log;
	}

	protected void init() throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {

				public Object run() throws BackingStoreException {
					rootNode = preferencesService.getUserPreferences(persistenceUserName);
					loadRoles();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {

			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void addRole(final org.osgi.service.useradmin.Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences node = rootNode.node(role.getName());
					node.putInt(typeString, role.getType());
					node.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void removeRole(final org.osgi.service.useradmin.Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences node = rootNode.node(role.getName());
					node.removeNode();
					rootNode.node("").flush(); //$NON-NLS-1$
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void clearProperties(final org.osgi.service.useradmin.Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences propertyNode = rootNode.node(role.getName() + "/" + propertiesNode); //$NON-NLS-1$
					propertyNode.clear();
					propertyNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void addProperty(final org.osgi.service.useradmin.Role role, final String key, final Object value) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences propertyNode = rootNode.node(role.getName() + "/" + propertiesNode); //$NON-NLS-1$
					if (value instanceof String) {
						propertyNode.put(key, (String) value);
					} else //must be a byte array, then
					{
						propertyNode.putByteArray(key, (byte[]) value);
					}
					propertyNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void removeProperty(final org.osgi.service.useradmin.Role role, final String key) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences propertyNode = rootNode.node(role.getName() + "/" + propertiesNode); //$NON-NLS-1$
					propertyNode.remove(key);
					propertyNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void clearCredentials(final org.osgi.service.useradmin.Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences credentialNode = rootNode.node(role.getName() + "/" + credentialsNode); //$NON-NLS-1$
					credentialNode.clear();
					credentialNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void addCredential(final org.osgi.service.useradmin.Role role, final String key, final Object value) throws BackingStoreException {

		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences credentialNode = rootNode.node(role.getName() + "/" + credentialsNode); //$NON-NLS-1$
					if (value instanceof String) {
						credentialNode.put(key, (String) value);
					} else //assume it is a byte array
					{
						credentialNode.putByteArray(key, (byte[]) value);
					}
					credentialNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, NLS.bind(UserAdminMsg.Backing_Store_Write_Exception, new Object[] {NLS.bind(UserAdminMsg.adding_Credential_to__15, role.getName())}), ex);
			throw ((BackingStoreException) ex.getException());
		}

	}

	protected void removeCredential(final org.osgi.service.useradmin.Role role, final String key) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences credentialNode = rootNode.node(role.getName() + "/" + credentialsNode); //$NON-NLS-1$
					credentialNode.remove(key);
					credentialNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void addMember(final Group group, final Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences memberNode = rootNode.node(group.getName() + "/" + membersNode); //$NON-NLS-1$
					memberNode.put(role.getName(), basicString);
					memberNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, NLS.bind(UserAdminMsg.Backing_Store_Write_Exception, new Object[] {NLS.bind(UserAdminMsg.adding_member__18, role.getName(), group.getName())}), ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void addRequiredMember(final Group group, final Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences memberNode = rootNode.node(group.getName() + "/" + membersNode); //$NON-NLS-1$
					memberNode.put(role.getName(), requiredString);
					memberNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, NLS.bind(UserAdminMsg.Backing_Store_Write_Exception, new Object[] {NLS.bind(UserAdminMsg.adding_required_member__21, role.getName(), group.getName())}), ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void removeMember(final Group group, final Role role) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					Preferences memberNode = rootNode.node(group.getName() + "/" + membersNode); //$NON-NLS-1$
					memberNode.remove(role.getName());
					memberNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			log.log(log.LOG_ERROR, NLS.bind(UserAdminMsg.Backing_Store_Write_Exception, new Object[] {NLS.bind(UserAdminMsg.removing_member__24, role.getName(), group.getName())}), ex);
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void loadRoles() throws BackingStoreException {
		synchronized (this) {
			createAnonRole();

			String[] children = rootNode.node("").childrenNames(); //$NON-NLS-1$

			for (int i = 0; i < children.length; i++) {
				if (useradmin.getRole(children[i]) == null) //check to see if it is already loaded
				{ //(we may have had to load some roles out of
					loadRole(rootNode.node(children[i]), null); // order due to dependencies) 
					//modified to solve defect 95982
				}
			}
		}
	}

	/* modified to solve defect 95982 */
	protected void loadRole(Preferences node, Role role) throws BackingStoreException {
		int type = node.getInt(typeString, Integer.MIN_VALUE);

		if (type == Integer.MIN_VALUE) {
			String errorString = NLS.bind(UserAdminMsg.Backing_Store_Read_Exception, new Object[] {NLS.bind(UserAdminMsg.Unable_to_load_role__27, node.name())});
			BackingStoreException ex = new BackingStoreException(errorString);
			log.log(log.LOG_ERROR, errorString, ex);
			throw (ex);
		}
		if (role == null) {
			role = (Role) useradmin.createRole(node.name(), type, false);
		}
		Preferences propsNode = node.node(propertiesNode);
		String[] keys = propsNode.keys();
		UserAdminHashtable properties = (UserAdminHashtable) role.getProperties();
		String value;

		//load properties
		for (int i = 0; i < keys.length; i++) {
			value = propsNode.get(keys[i], null);
			properties.put(keys[i], value, false);
		}

		//load credentials
		if (type == Role.USER || type == Role.GROUP) {
			Object credValue;
			Preferences credNode = node.node(credentialsNode);
			keys = credNode.keys();
			UserAdminHashtable credentials = (UserAdminHashtable) ((User) role).getCredentials();
			for (int i = 0; i < keys.length; i++) {
				credValue = credNode.get(keys[i], null);
				credentials.put(keys[i], credValue, false);
			}
		}

		//load group members
		if (type == Role.GROUP) {
			Preferences memberNode = node.node(membersNode);
			keys = memberNode.keys();
			for (int i = 0; i < keys.length; i++) {
				value = memberNode.get(keys[i], null);
				Role member = (Role) useradmin.getRole(keys[i]);
				if (member == null) //then we have not loaded this one yet, so load it
				{
					loadRole(rootNode.node(keys[i]), null); // modified to solve defect 95982
					member = (Role) useradmin.getRole(keys[i]);
				}
				if (value.equals(requiredString)) {
					((Group) role).addRequiredMember(member, false);
				} else {
					((Group) role).addMember(member, false);
				}
			}
		}
	}

	protected void destroy() {
		try {
			rootNode.flush();
			rootNode = null;
			preferencesService = null;
		} catch (BackingStoreException ex) {
			log.log(log.LOG_ERROR, UserAdminMsg.Backing_Store_Write_Exception, ex);
		}
	}

	private void createAnonRole() throws BackingStoreException {
		Role role = null;
		if (!rootNode.nodeExists(Role.anyoneString)) {
			//If the user.anyone role is not present, create it
			role = (Role) useradmin.createRole(Role.anyoneString, Role.ROLE, true);
		}
		/* modified to solve defect 95982 */
		if (role != null)
			loadRole(rootNode.node(Role.anyoneString), role);
		else
			loadRole(rootNode.node(Role.anyoneString), null);
	}

}
