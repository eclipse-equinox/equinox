/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.*;
import java.security.*;
import java.util.ArrayList;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * 
 * This is a runtime embodiment of the data stored in ConditionalPermissionInfo.
 * It has methods to facilitate the management of Conditions and Permissions at
 * runtime.
 */
public class ConditionalPermissionInfoImpl implements ConditionalPermissionInfo, Serializable {
	private static final long serialVersionUID = 3258130245704825139L;
	/**
	 * The permissions enabled by the associated Conditions.
	 */
	PermissionInfo perms[];
	/**
	 * The conditions that must be satisfied to enable the corresponding
	 * permissions.
	 */
	ConditionInfo conds[];

	/**
	 * The name of the ConditionalPermissionInfo
	 */
	private String name;

	/**
	 * When true, this object has been deleted and any information retrieved
	 * from it should be discarded.
	 */
	private boolean deleted = false;

	/**
	 * When true, this object has been deleted and any information retrieved
	 * from it should be discarded.
	 */
	boolean isDeleted() {
		return deleted;
	}

	public ConditionalPermissionInfoImpl(String encoded) {
		decode(encoded);
	}

	public ConditionalPermissionInfoImpl(String name, ConditionInfo conds[], PermissionInfo perms[]) {
		this.name = name;
		this.conds = conds;
		this.perms = perms;
	}

	private void decode(String encoded) {
		int start = encoded.indexOf('{');
		int end = encoded.lastIndexOf('}');
		if (start < 0 || end < start)
			throw new IllegalArgumentException(encoded);
		if (start != 0)
			name = encoded.substring(0, start);
		char[] chars = encoded.substring(start + 1, end).toCharArray();
		ArrayList condList = new ArrayList();
		ArrayList permList = new ArrayList();
		int pos = 0;
		while (pos < chars.length) {
			while (pos < chars.length && chars[pos] != '[' && chars[pos] != '(')
				pos++;
			if (pos == chars.length)
				break; // no perms or conds left
			int startPos = pos;
			char endChar = chars[startPos] == '[' ? ']' : ')';
			while (chars[pos] != endChar) {
				if (chars[pos] == '"') {
					pos++;
					while (chars[pos] != '"') {
						if (chars[pos] == '\\')
							pos++;
						pos++;
					}
				}
				pos++;
			}
			int endPos = pos;
			String token = new String(chars, startPos, endPos - startPos + 1);
			if (endChar == ']')
				condList.add(new ConditionInfo(token));
			else
				permList.add(new PermissionInfo(token));
			pos++;
		}
		conds = (ConditionInfo[]) condList.toArray(new ConditionInfo[condList.size()]);
		perms = (PermissionInfo[]) permList.toArray(new PermissionInfo[permList.size()]);
	}

	public String getName() {
		return name;
	}

	/**
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionInfo#getConditionInfos()
	 */
	public ConditionInfo[] getConditionInfos() {
		if (conds == null)
			return null;
		ConditionInfo[] results = new ConditionInfo[conds.length];
		System.arraycopy(conds, 0, results, 0, conds.length);
		return results;
	}

	/* Used to find permission constructors in addPermissions */
	static private final Class twoStringClassArray[] = new Class[] {String.class, String.class};
	static private final Class oneStringClassArray[] = new Class[] {String.class};
	static private final Class noArgClassArray[] = new Class[] {};
	static private final Class[][] permClassArrayArgs = new Class[][] {noArgClassArray, oneStringClassArray, twoStringClassArray};
	/* Used to find condition constructors getConditions */
	static private final Class[] condClassArray = new Class[] {Bundle.class, ConditionInfo.class};

	/**
	 * Adds the permissions of the given type (if any) that are part of this
	 * ConditionalPermissionInfo to the specified collection. The Permission
	 * instances are constructed using the specified permClass.
	 * 
	 * @param collection the collection to add to.
	 * @param permClass the class to use to construct Permission instances.
	 * @return the number of Permissions added.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IllegalArgumentException
	 */
	int addPermissions(AbstractBundle bundle, PermissionCollection collection, Class permClass) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		String permClassName = permClass.getName();
		Constructor constructor = null;
		int numArgs = -1;
		for (int i = permClassArrayArgs.length - 1 ; i >= 0; i--) {
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
		int count = 0;
		/*
		 * TODO: We need to cache the permission constructors to enhance performance (see bug 118813).
		 */
		for (int i = 0; i < perms.length; i++) {
			if (perms[i].getType().equals(permClassName)) {
				count++;
				String args[] = new String[numArgs];
				if (numArgs > 0)
					args[0] = perms[i].getName();
				if (numArgs > 1)
					args[1] = perms[i].getActions();
				if (perms[i].getType().equals("java.io.FilePermission")) { //$NON-NLS-1$
					// map FilePermissions for relative names to the bundle's data area
					if (!args[0].equals("<<ALL FILES>>")) { //$NON-NLS-1$
						File file = new File(args[0]);
						if (!file.isAbsolute()) { // relative name
							if (bundle == null) // default permissions
								continue; // no relative file permissions
							File target = bundle.framework.getDataFile(bundle, args[0]);
							if (target == null) // no bundle data file area
								continue; // no relative file permissions
							args[0] = target.getPath();
						}
					}
				}
				collection.add((Permission) constructor.newInstance(args));
			}
		}
		return count;
	}

	/**
	 * Returns the Condition objects associated with this ConditionalPermissionInfo.
	 * 
	 * @param bundle the bundle to be used to construct the Conditions.
	 * 
	 * @return the array of Conditions that must be satisfied before permissions
	 *         in the ConditionPermissionInfoImpl can be used.
	 */
	Condition[] getConditions(Bundle bundle) {
		Condition conditions[] = new Condition[conds.length];
		for (int i = 0; i < conds.length; i++) {
			/*
			 * TODO: I think we can pre-get the Constructors in our own
			 * constructor
			 */
			Class clazz;
			try {
				clazz = Class.forName(conds[i].getType());
			} catch (ClassNotFoundException e) {
				/* If the class isn't there, we fail */
				return null;
			}
			Constructor constructor = null;
			Method method = null;
			try {
				method = clazz.getMethod("getCondition", condClassArray); //$NON-NLS-1$
				if ((method.getModifiers() & Modifier.STATIC) == 0)
					method = null;
			} catch (NoSuchMethodException e) {
				// This is a normal case
			}
			if (method == null)
				try {
					constructor = clazz.getConstructor(condClassArray);
				} catch (NoSuchMethodException e) {
					// TODO should post a FrameworkEvent of type error here
					conditions[i] = Condition.FALSE;
					continue;
				}

			Object args[] = {bundle, conds[i]};
			try {
				if (method != null)
					conditions[i] = (Condition) method.invoke(null, args);
				else
					conditions[i] = (Condition) constructor.newInstance(args);
			} catch (Throwable t) {
				// TODO should post a FrameworkEvent of type error here
				conditions[i] = Condition.FALSE;
			}
		}
		return conditions;
	}

	/**
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionInfo#getPermissionInfos()
	 */
	public PermissionInfo[] getPermissionInfos() {
		if (perms == null)
			return null;
		PermissionInfo[] results = new PermissionInfo[perms.length];
		System.arraycopy(perms, 0, results, 0, perms.length);
		return results;
	}

	/**
	 * 
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionInfo#delete()
	 */
	public void delete() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AllPermission());
		deleted = true;
		condAdmin.deleteConditionalPermissionInfo(this);
	}

	private static ConditionalPermissionAdminImpl condAdmin;

	static void setConditionalPermissionAdminImpl(ConditionalPermissionAdminImpl condAdmin) {
		ConditionalPermissionInfoImpl.condAdmin = condAdmin;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if (name != null)
			result.append(name);
		ConditionInfo[] curConds = getConditionInfos();
		PermissionInfo[] curPerms = getPermissionInfos();
		result.append('{').append(' ');
		if (curConds != null)
			for (int i = 0; i < curConds.length; i++)
				result.append(curConds[i].getEncoded()).append(' ');
		if (curPerms != null)
			for (int i = 0; i < curPerms.length; i++)
				result.append(curPerms[i].getEncoded()).append(' ');
		result.append('}');
		return result.toString();
	}
}
