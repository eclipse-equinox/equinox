/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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
import java.io.FilePermission;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class PermissionInfoCollection extends PermissionCollection {
	private static final long serialVersionUID = 3140511562980923957L;
	/* Used to find permission constructors in addPermissions */
	static private final Class<?> twoStringClassArray[] = new Class[] { String.class, String.class };
	static private final Class<?> oneStringClassArray[] = new Class[] { String.class };
	static private final Class<?> noArgClassArray[] = new Class[] {};
	static private final Class<?>[][] permClassArrayArgs = new Class[][] { noArgClassArray, oneStringClassArray,
			twoStringClassArray };
	static private final String ALL_PERMISSION_NAME = AllPermission.class.getName();
	static final String FILE_PERMISSION_NAME = FilePermission.class.getName();
	static final String ALL_FILES = "<<ALL FILES>>"; //$NON-NLS-1$

	/* @GuardedBy(cachedPermissionCollections) */
	private final Map<Class<? extends Permission>, PermissionCollection> cachedPermissionCollections = new HashMap<>();
	private final Map<BundlePermissions, PermissionCollection> cachedRelativeFilePermissionCollections;
	private final boolean hasAllPermission;
	private final PermissionInfo[] permInfos;

	public PermissionInfoCollection(PermissionInfo[] permInfos) {
		this.permInfos = permInfos;
		boolean tempAllPermissions = false;
		boolean allAbsolutePaths = true;
		for (PermissionInfo info : permInfos) {
			if (ALL_PERMISSION_NAME.equals(info.getType())) {
				tempAllPermissions = true;
			} else if (FILE_PERMISSION_NAME.equals(info.getType())) {
				if (!(new File(info.getActions()).isAbsolute())) {
					allAbsolutePaths = false;
				}
			}
		}
		this.hasAllPermission = tempAllPermissions;
		this.cachedRelativeFilePermissionCollections = allAbsolutePaths ? null : new HashMap<>();
		setReadOnly(); // collections are managed with ConditionalPermissionAdmin
	}

	@Override
	public void add(Permission arg0) {
		throw new SecurityException();
	}

	@Override
	public Enumeration<Permission> elements() {
		// TODO return an empty enumeration for now;
		return Collections.emptyEnumeration();
	}

	@Override
	public boolean implies(Permission perm) {
		return implies(null, perm);
	}

	boolean implies(final BundlePermissions bundlePermissions, Permission perm) {
		if (hasAllPermission)
			return true;
		final Class<? extends Permission> permClass = perm.getClass();
		PermissionCollection collection = getCachedCollection(bundlePermissions, permClass);
		// must populate the collection outside of the lock to prevent class loader
		// deadlock
		if (collection == null) {
			collection = perm.newPermissionCollection();
			if (collection == null) {
				collection = new PermissionsHash();
			}
			try {
				final PermissionCollection targetCollection = collection;
				AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
					addPermissions(bundlePermissions, targetCollection, permClass);
					return null;
				});

			} catch (Exception e) {
				if (e instanceof PrivilegedActionException) {
					e = ((PrivilegedActionException) e).getException();
				}
				throw new SecurityException("Exception creating permissions: " + permClass + ": " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			collection = cacheCollection(bundlePermissions, permClass, collection);
		}
		return collection.implies(perm);
	}

	PermissionCollection getCachedCollection(BundlePermissions bundlePermissions,
			Class<? extends Permission> permClass) {
		synchronized (cachedPermissionCollections) {
			if (bundlePermissions != null && cachedRelativeFilePermissionCollections != null
					&& FILE_PERMISSION_NAME.equals(permClass.getName())) {
				return cachedRelativeFilePermissionCollections.get(bundlePermissions);
			}
			return cachedPermissionCollections.get(permClass);
		}
	}

	private PermissionCollection cacheCollection(BundlePermissions bundlePermissions,
			Class<? extends Permission> permClass, PermissionCollection collection) {
		synchronized (cachedPermissionCollections) {
			// check to see if another thread beat this thread at adding the collection
			boolean relativeFiles = bundlePermissions != null && cachedRelativeFilePermissionCollections != null
					&& FILE_PERMISSION_NAME.equals(permClass.getName());
			PermissionCollection exists = relativeFiles ? cachedRelativeFilePermissionCollections.get(bundlePermissions)
					: cachedPermissionCollections.get(permClass);
			if (exists != null) {
				collection = exists;
			} else {
				if (relativeFiles) {
					cachedRelativeFilePermissionCollections.put(bundlePermissions, collection);
				} else {
					cachedPermissionCollections.put(permClass, collection);
				}
			}
			return collection;
		}
	}

	PermissionInfo[] getPermissionInfos() {
		return permInfos;
	}

	void addPermissions(BundlePermissions bundlePermissions, PermissionCollection collection,
			Class<? extends Permission> permClass) throws Exception {
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
		if (constructor == null) {
			throw new NoSuchMethodException(permClass.getName() + ".<init>()"); //$NON-NLS-1$
		}
		/*
		 * TODO: We need to cache the permission constructors to enhance performance
		 * (see bug 118813).
		 */
		for (PermissionInfo permInfo : permInfos) {
			if (permInfo.getType().equals(permClassName)) {
				String args[] = new String[numArgs];
				if (numArgs > 0) {
					args[0] = permInfo.getName();
				}
				if (numArgs > 1) {
					args[1] = permInfo.getActions();
				}
				if (permInfo.getType().equals(FILE_PERMISSION_NAME)) {
					// map FilePermissions for relative names to the bundle's data area
					if (!args[0].equals(ALL_FILES)) {
						File file = new File(args[0]);
						if (!file.isAbsolute()) { // relative name
							File target = bundlePermissions == null ? null
									: bundlePermissions.getBundle().getDataFile(permInfo.getName());
							if (target == null) {
								// ignore if we cannot find the data area
								continue;
							}
							args[0] = target.getPath();
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
			if (cachedRelativeFilePermissionCollections != null) {
				cachedRelativeFilePermissionCollections.clear();
			}
		}
	}
}
