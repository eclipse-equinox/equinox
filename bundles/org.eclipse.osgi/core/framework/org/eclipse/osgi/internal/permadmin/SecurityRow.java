/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.lang.reflect.*;
import java.security.Permission;
import java.util.*;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class SecurityRow implements ConditionalPermissionInfo {
	/* Used to find condition constructors getConditions */
	static final Class[] conditionMethodArgs = new Class[] {Bundle.class, ConditionInfo.class};
	static Condition[] ABSTAIN_LIST = new Condition[0];
	static Condition[] SATISFIED_LIST = new Condition[0];
	static final Decision DECISION_ABSTAIN = new Decision(SecurityTable.ABSTAIN, null, null, null);
	static final Decision DECISION_GRANTED = new Decision(SecurityTable.GRANTED, null, null, null);
	static final Decision DECISION_DENIED = new Decision(SecurityTable.DENIED, null, null, null);

	private final SecurityAdmin securityAdmin;
	private final String name;
	private final ConditionInfo[] conditionInfos;
	private final PermissionInfoCollection permissionInfoCollection;
	private final boolean deny;
	/* GuardedBy(bundleConditions) */
	private final HashMap bundleConditions;

	public SecurityRow(SecurityAdmin securityAdmin, String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos, String decision) {
		this.securityAdmin = securityAdmin;
		this.conditionInfos = conditionInfos;
		this.deny = ConditionalPermissionInfoBase.DENY.equals(decision);
		this.name = name;
		this.permissionInfoCollection = new PermissionInfoCollection(permissionInfos);
		if (conditionInfos == null || conditionInfos.length == 0)
			bundleConditions = null;
		else
			bundleConditions = new HashMap();
	}

	static SecurityRow createSecurityRow(SecurityAdmin securityAdmin, String encoded) {
		int start = encoded.indexOf('{');
		int end = encoded.lastIndexOf('}');
		if (start < 0 || end < start)
			throw new IllegalArgumentException(encoded);
		String decision = null;
		if (encoded.charAt(encoded.length() - 1) == '!')
			decision = ConditionalPermissionInfoBase.DENY;
		String encodedName = null;
		if (start != 0)
			encodedName = encoded.substring(0, start);
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
		ConditionInfo[] conds = (ConditionInfo[]) condList.toArray(new ConditionInfo[condList.size()]);
		PermissionInfo[] perms = (PermissionInfo[]) permList.toArray(new PermissionInfo[permList.size()]);
		return new SecurityRow(securityAdmin, encodedName, conds, perms, decision);
	}

	public String getName() {
		return name;
	}

	public ConditionInfo[] getConditionInfos() {
		return conditionInfos;
	}

	public String getGrantDecision() {
		return deny ? ConditionalPermissionInfoBase.DENY : ConditionalPermissionInfoBase.ALLOW;
	}

	public PermissionInfo[] getPermissionInfos() {
		return permissionInfoCollection.getPermissionInfos();
	}

	public void delete() {
		securityAdmin.delete(this, true);
	}

	Condition[] getConditions(Bundle bundle) {
		Condition[] conditions = new Condition[conditionInfos.length];
		for (int i = 0; i < conditionInfos.length; i++) {
			/*
			 * TODO: Can we pre-get the Constructors in our own constructor
			 */
			Class clazz;
			try {
				clazz = Class.forName(conditionInfos[i].getType());
			} catch (ClassNotFoundException e) {
				/* If the class isn't there, we fail */
				return null;
			}
			Constructor constructor = null;
			Method method = null;
			try {
				method = clazz.getMethod("getCondition", conditionMethodArgs); //$NON-NLS-1$
				if ((method.getModifiers() & Modifier.STATIC) == 0)
					method = null;
			} catch (NoSuchMethodException e) {
				// This is a normal case
			}
			if (method == null)
				try {
					constructor = clazz.getConstructor(conditionMethodArgs);
				} catch (NoSuchMethodException e) {
					// TODO should post a FrameworkEvent of type error here
					conditions[i] = Condition.FALSE;
					continue;
				}

			Object args[] = {bundle, conditionInfos[i]};
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

	Decision evaluate(BundlePermissions bundlePermissions, Permission permission) {
		if (bundleConditions == null)
			return evaluatePermission(permission);
		Condition[] conditions;
		synchronized (bundleConditions) {
			conditions = (Condition[]) bundleConditions.get(bundlePermissions);
			if (conditions == null) {
				conditions = getConditions(bundlePermissions.getBundle());
				bundleConditions.put(bundlePermissions, conditions);
			}
		}
		if (conditions == ABSTAIN_LIST)
			return DECISION_ABSTAIN;
		if (conditions == SATISFIED_LIST)
			return evaluatePermission(permission);

		boolean empty = true;
		List postponedConditions = null;
		Decision postponedPermCheck = null;
		for (int i = 0; i < conditions.length; i++) {
			Condition condition = conditions[i];
			if (condition == null)
				continue; // this condition must have been satisfied && !mutable in a previous check
			// TODO need to clarify on the spec if nonMutable postponed conditions can be evaluated immediately
			if (!isPostponed(condition) || !condition.isMutable()) {
				if (condition.isSatisfied()) {
					if (!condition.isMutable())
						conditions[i] = null; // ignore this condition for future checks
				} else {
					if (!condition.isMutable())
						// this will cause the row to always abstain; mark this to be ignored in future checks
						synchronized (bundleConditions) {
							bundleConditions.put(bundlePermissions, ABSTAIN_LIST);
						}
					return DECISION_ABSTAIN;
				}
			} else { // postponed case
				if (postponedPermCheck == null)
					// perform a permission check now
					postponedPermCheck = evaluatePermission(permission);
				if (postponedPermCheck == DECISION_ABSTAIN)
					return postponedPermCheck; // no need to postpone the condition if the row abstains
				// this row will deny or allow the permission; must queue the postponed condition
				if (postponedConditions == null)
					postponedConditions = new ArrayList(1);
				postponedConditions.add(condition);
			}
			empty &= conditions[i] == null;
		}
		if (empty) {
			synchronized (bundleConditions) {
				bundleConditions.put(bundlePermissions, SATISFIED_LIST);
			}
		}
		if (postponedPermCheck != null)
			return new Decision(postponedPermCheck.decision | SecurityTable.POSTPONED, (Condition[]) postponedConditions.toArray(new Condition[postponedConditions.size()]), this, bundlePermissions);
		return evaluatePermission(permission);
	}

	private boolean isPostponed(Condition condition) {
		// postponed checks can only happen if we are using a supported security manager
		return condition.isPostponed() && securityAdmin.getSupportedSecurityManager() != null;
	}

	private Decision evaluatePermission(Permission permission) {
		return permissionInfoCollection.implies(permission) ? (deny ? DECISION_DENIED : DECISION_GRANTED) : DECISION_ABSTAIN;
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
		if (deny)
			result.append('!');
		return result.toString();
	}

	PermissionInfoCollection getPermissionInfoCollection() {
		return permissionInfoCollection;
	}

	void clearCaches() {
		permissionInfoCollection.clearPermissionCache();
		if (bundleConditions != null)
			synchronized (bundleConditions) {
				bundleConditions.clear();
			}
	}

	static class Decision {
		final int decision;
		final Condition[] postponed;
		private final SecurityRow row;
		private final BundlePermissions bundlePermissions;

		Decision(int decision, Condition[] postponed, SecurityRow row, BundlePermissions bundlePermissions) {
			this.decision = decision;
			this.postponed = postponed;
			this.row = row;
			this.bundlePermissions = bundlePermissions;
		}

		void handleImmutable(Condition condition, boolean isSatisfied) {
			if (condition.isMutable() || !condition.isPostponed())
				return; // do nothing
			if (isSatisfied) {
				synchronized (row.bundleConditions) {
					Condition[] rowConditions = (Condition[]) row.bundleConditions.get(bundlePermissions);
					boolean isEmpty = true;
					for (int i = 0; i < rowConditions.length; i++) {
						if (rowConditions[i] == condition)
							if (isSatisfied)
								rowConditions[i] = null;
						isEmpty &= rowConditions[i] == null;
					}
					if (isEmpty)
						row.bundleConditions.put(bundlePermissions, SATISFIED_LIST);
				}
			} else {
				synchronized (row.bundleConditions) {
					row.bundleConditions.put(bundlePermissions, ABSTAIN_LIST);
				}
			}
		}
	}
}
