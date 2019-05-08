/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.security.*;
import java.util.*;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class PermissionInfoCollection extends PermissionCollection {
	private static final long serialVersionUID = 3140511562980923957L;
	/* Used to find permission constructors in addPermissions */
	static private final Class<?> twoStringClassArray[] = new Class[] {String.class, String.class};
	static private final Class<?> oneStringClassArray[] = new Class[] {String.class};
	static private final Class<?> noArgClassArray[] = new Class[] {};
	static private final Class<?>[][] permClassArrayArgs = new Class[][] {noArgClassArray, oneStringClassArray, twoStringClassArray};

	/* @GuardedBy(cachedPermisssionCollections) */
	private final Map<Class<? extends Permission>, PermissionCollection> cachedPermissionCollections = new HashMap<>();
	private final boolean hasAllPermission;
	private final PermissionInfo[] permInfos;

	public PermissionInfoCollection(PermissionInfo[] permInfos) {
		this.permInfos = permInfos;
		boolean tempAllPermissions = false;
		for (int i = 0; i < permInfos.length && !tempAllPermissions; i++)
			if (permInfos[i].getType().equals(AllPermission.class.getName()))
				tempAllPermissions = true;
		this.hasAllPermission = tempAllPermissions;
		setReadOnly(); // collections are managed with ConditionalPermissionAdmin
	}

	@Override
	public void add(Permission arg0) {
		throw new SecurityException();
	}

	@Override
	public Enumeration<Permission> elements() {
		// TODO return an empty enumeration for now; 
		return BundlePermissions.EMPTY_ENUMERATION;
	}

	@Override
	public boolean implies(Permission perm) {
		if (hasAllPermission)
			return true;
		final Class<? extends Permission> permClass = perm.getClass();
		PermissionCollection collection;
		synchronized (cachedPermissionCollections) {
			collection = cachedPermissionCollections.get(permClass);
		}
		// must populate the collection outside of the lock to prevent class loader deadlock
		if (collection == null) {
			collection = perm.newPermissionCollection();
			if (collection == null) {
				collection = new PermissionsHash();
			}
			try {
				final PermissionCollection targetCollection = collection;
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws Exception {
						addPermissions(targetCollection, permClass);
						return null;
					}

				});

			} catch (Exception e) {
				if (e instanceof PrivilegedActionException) {
					e = ((PrivilegedActionException) e).getException();
				}
				throw new SecurityException("Exception creating permissions: " + permClass + ": " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			synchronized (cachedPermissionCollections) {
				// check to see if another thread beat this thread at adding the collection
				PermissionCollection exists = cachedPermissionCollections.get(permClass);
				if (exists != null)
					collection = exists;
				else
					cachedPermissionCollections.put(permClass, collection);
			}
		}
		return collection.implies(perm);
	}

	PermissionInfo[] getPermissionInfos() {
		return permInfos;
	}

	void addPermissions(PermissionCollection collection, Class<? extends Permission> permClass) throws Exception {
		String permClassName = permClass.getName();
		Constructor<? extends Permission> constructor = null;
		int numArgs = -1;
		for (int i = permClassArrayArgs.length - 1; i >= 0; i--) {
			try {
				constructor = permClass.getConstructor(permClassArrayArgs[i]);
				numArgs = i;
				break;
			} catch (NoSuchMethodException e) {
				// ignore
			}
		}
		if (constructor == null)
			throw new NoSuchMethodException(permClass.getName() + ".<init>()"); //$NON-NLS-1$
		/*
		 * TODO: We need to cache the permission constructors to enhance performance (see bug 118813).
		 */
		for (int i = 0; i < permInfos.length; i++) {
			if (permInfos[i].getType().equals(permClassName)) {
				String args[] = new String[numArgs];
				if (numArgs > 0)
					args[0] = permInfos[i].getName();
				if (numArgs > 1)
					args[1] = permInfos[i].getActions();

				if (permInfos[i].getType().equals("java.io.FilePermission")) { //$NON-NLS-1$
					// map FilePermissions for relative names to the bundle's data area
					if (!args[0].equals("<<ALL FILES>>")) { //$NON-NLS-1$
						File file = new File(args[0]);
						if (!file.isAbsolute()) { // relative name
							// TODO need to figure out how to do relative FilePermissions from the dataFile
							continue;
						}
					}
				}
				collection.add(constructor.newInstance((Object[]) args));
			}
		}
	}

	void clearPermissionCache() {
		synchronized (cachedPermissionCollections) {
			cachedPermissionCollections.clear();
		}
	}
}
