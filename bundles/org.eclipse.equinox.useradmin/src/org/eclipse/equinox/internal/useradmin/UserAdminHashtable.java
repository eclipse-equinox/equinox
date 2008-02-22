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
import java.util.Hashtable;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.useradmin.UserAdminEvent;

/*  The UserAdminHashtable is a Hashtable that generates UserAdminEvents when there
 *  is a change to the Hashtable.  This is used specifically to store Role properties
 *  and User credentials.
 */

public class UserAdminHashtable extends Hashtable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397030865421289240L;
	protected Role role;
	protected int propertyType;
	protected UserAdmin userAdmin;
	protected UserAdminStore userAdminStore;

	//TODO - split this into two classes so we don't have to do this
	protected static final int CREDENTIALS = 0;
	protected static final int PROPERTIES = 1;

	protected UserAdminHashtable(Role role, UserAdmin userAdmin, int propertyType) {
		this.role = role;
		this.propertyType = propertyType;
		this.userAdmin = userAdmin;
		this.userAdminStore = userAdmin.userAdminStore;
	}

	/*
	 *  We want to generate an event every time we put something into the hashtable, except
	 *  upon initialization where role data is being read from persistent store.
	 */
	protected synchronized Object put(String key, Object value, boolean generateEvent) {

		if (generateEvent) {
			if (propertyType == UserAdminHashtable.PROPERTIES) {
				try {
					userAdminStore.addProperty(role, key, value);
				} catch (BackingStoreException ex) {
					return (null);
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
			} else if (propertyType == UserAdminHashtable.CREDENTIALS) {
				try {
					userAdminStore.addCredential(role, key, value);
				} catch (BackingStoreException ex) {
					return (null);
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
			}
		}
		Object retVal = super.put(key, value);
		return retVal;
	}

	public Object put(Object key, Object value) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(UserAdminMsg.INVALID_KEY_EXCEPTION);
		}

		if (!((value instanceof String) || (value instanceof byte[]))) {
			throw new IllegalArgumentException(UserAdminMsg.INVALID_VALUE_EXCEPTION);
		}

		String name = (String) key;

		switch (propertyType) {
			case PROPERTIES :
				userAdmin.checkChangePropertyPermission(name);
				break;
			case CREDENTIALS :
				userAdmin.checkChangeCredentialPermission(name);
				break;
		}

		return put(name, value, true);
	}

	public synchronized Object remove(Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(UserAdminMsg.INVALID_KEY_EXCEPTION);
		}

		String name = (String) key;

		switch (propertyType) {
			case PROPERTIES :
				userAdmin.checkChangePropertyPermission(name);
				try {
					userAdminStore.removeProperty(role, name);
				} catch (BackingStoreException ex) {
					return (null);
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
				break;
			case CREDENTIALS :
				userAdmin.checkChangeCredentialPermission(name);
				try {
					userAdminStore.removeCredential(role, name);
				} catch (BackingStoreException ex) {
					return (null);
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
				break;
		}

		return super.remove(name);
	}

	public synchronized void clear() {
		Enumeration e = keys();

		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();

			switch (propertyType) {
				case PROPERTIES :
					userAdmin.checkChangePropertyPermission(name);
					break;
				case CREDENTIALS :
					userAdmin.checkChangeCredentialPermission(name);
					break;
			}
		}

		switch (propertyType) {
			case PROPERTIES :
				try {
					userAdminStore.clearProperties(role);
				} catch (BackingStoreException ex) {
					return;
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
				break;
			case CREDENTIALS :
				try {
					userAdminStore.clearCredentials(role);
				} catch (BackingStoreException ex) {
					return;
				}
				userAdmin.eventProducer.generateEvent(UserAdminEvent.ROLE_CHANGED, role);
				break;
		}

		super.clear();
	}

	public Object get(Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(UserAdminMsg.INVALID_KEY_EXCEPTION);
		}

		String name = (String) key;

		if (propertyType == CREDENTIALS) {
			userAdmin.checkGetCredentialPermission(name);
		}

		return super.get(name);
	}
}
