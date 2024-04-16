/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
 *     Connexta, LLC - performance improvements
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class SecurityRow implements ConditionalPermissionInfo {
	/* Used to find condition constructors getConditions */
	static final Class<?>[] conditionMethodArgs = new Class[] { Bundle.class, ConditionInfo.class };
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
	final Map<BundlePermissions, Condition[]> bundleConditions;
	final Object bundleConditionsLock = new Object();

	public SecurityRow(SecurityAdmin securityAdmin, String name, ConditionInfo[] conditionInfos,
			PermissionInfo[] permissionInfos, String decision) {
		if (permissionInfos == null || permissionInfos.length == 0)
			throw new IllegalArgumentException("It is invalid to have empty permissionInfos"); //$NON-NLS-1$
		this.securityAdmin = securityAdmin;
		this.conditionInfos = conditionInfos == null ? new ConditionInfo[0] : conditionInfos;
		decision = decision.toLowerCase();
		boolean d = ConditionalPermissionInfo.DENY.equals(decision);
		boolean a = ConditionalPermissionInfo.ALLOW.equals(decision);
		if (!(d | a))
			throw new IllegalArgumentException("Invalid decision: " + decision); //$NON-NLS-1$
		this.deny = d;
		this.name = name;
		this.permissionInfoCollection = new PermissionInfoCollection(permissionInfos);
		if (conditionInfos == null || conditionInfos.length == 0)
			bundleConditions = null;
		else
			bundleConditions = new HashMap<>();
	}

	static SecurityRowSnapShot createSecurityRowSnapShot(String encoded) {
		return (SecurityRowSnapShot) createConditionalPermissionInfo(null, encoded);
	}

	static SecurityRow createSecurityRow(SecurityAdmin securityAdmin, String encoded) {
		return (SecurityRow) createConditionalPermissionInfo(securityAdmin, encoded);
	}

	private static ConditionalPermissionInfo createConditionalPermissionInfo(SecurityAdmin securityAdmin,
			String encoded) {
		encoded = encoded.trim();
		if (encoded.length() == 0)
			throw new IllegalArgumentException("Empty encoded string is invalid"); //$NON-NLS-1$
		char[] chars = encoded.toCharArray();
		int end = encoded.length() - 1;
		char lastChar = chars[end];
		if (lastChar != '}' && lastChar != '"')
			throw new IllegalArgumentException(encoded);
		String encodedName = null;
		if (lastChar == '"') {
			// we have a name: an empty name must have at least 2 chars for the quotes
			if (chars.length < 2)
				throw new IllegalArgumentException(encoded);
			int endName = encoded.length() - 1;
			int startName = endName - 1;
			while (startName > 0) {
				if (chars[startName] == '"') {
					startName--;
					if (startName > 0 && chars[startName] == '\\')
						startName--;
					else {
						startName++;
						break;
					}
				}
				startName--;
			}
			if (chars[startName] != '"')
				throw new IllegalArgumentException(encoded);
			encodedName = unescapeString(encoded.substring(startName + 1, endName));
			end = encoded.lastIndexOf('}', startName);
		}
		int start = encoded.indexOf('{');
		if (start < 0 || end < start)
			throw new IllegalArgumentException(encoded);

		String decision = encoded.substring(0, start);
		decision = decision.trim();
		if (decision.length() == 0 || (!ConditionalPermissionInfo.DENY.equalsIgnoreCase(decision)
				&& !ConditionalPermissionInfo.ALLOW.equalsIgnoreCase(decision)))
			throw new IllegalArgumentException(encoded);

		List<ConditionInfo> condList = new ArrayList<>();
		List<PermissionInfo> permList = new ArrayList<>();
		int pos = start + 1;
		while (pos < end) {
			while (pos < end && chars[pos] != '[' && chars[pos] != '(')
				pos++;
			if (pos == end)
				break; // no perms or conds left
			int startPos = pos;
			char endChar = chars[startPos] == '[' ? ']' : ')';
			while (pos < end && chars[pos] != endChar) {
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
		if (permList.size() == 0)
			throw new IllegalArgumentException("No Permission infos: " + encoded); //$NON-NLS-1$
		ConditionInfo[] conds = condList.toArray(new ConditionInfo[condList.size()]);
		PermissionInfo[] perms = permList.toArray(new PermissionInfo[permList.size()]);
		if (securityAdmin == null)
			return new SecurityRowSnapShot(encodedName, conds, perms, decision);
		return new SecurityRow(securityAdmin, encodedName, conds, perms, decision);
	}

	static Object cloneArray(Object[] array) {
		if (array == null)
			return null;
		Object result = Array.newInstance(array.getClass().getComponentType(), array.length);
		System.arraycopy(array, 0, result, 0, array.length);
		return result;
	}

	private static void escapeString(String str, StringBuilder output) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				output.append('\\');
				output.append(c);
				break;
			case '\r':
				output.append("\\r"); //$NON-NLS-1$
				break;
			case '\n':
				output.append("\\n"); //$NON-NLS-1$
				break;
			default:
				output.append(c);
				break;
			}
		}
	}

	private static String unescapeString(String str) {
		StringBuilder output = new StringBuilder(str.length());
		int end = str.length();
		for (int i = 0; i < end; i++) {
			char c = str.charAt(i);
			if (c == '\\') {
				i++;
				if (i < end) {
					c = str.charAt(i);
					switch (c) {
					case '"':
					case '\\':
						break;
					case 'r':
						c = '\r';
						break;
					case 'n':
						c = '\n';
						break;
					default:
						c = '\\';
						i--;
						break;
					}
				}
			}
			output.append(c);
		}

		return output.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ConditionInfo[] getConditionInfos() {
		// must make a copy for the public API method to prevent modification
		return (ConditionInfo[]) cloneArray(conditionInfos);
	}

	ConditionInfo[] internalGetConditionInfos() {
		return conditionInfos;
	}

	@Override
	public String getAccessDecision() {
		return deny ? ConditionalPermissionInfo.DENY : ConditionalPermissionInfo.ALLOW;
	}

	@Override
	public PermissionInfo[] getPermissionInfos() {
		// must make a copy for the public API method to prevent modification
		return (PermissionInfo[]) cloneArray(permissionInfoCollection.getPermissionInfos());
	}

	PermissionInfo[] internalGetPermissionInfos() {
		return permissionInfoCollection.getPermissionInfos();
	}

	/**
	 * @deprecated
	 */
	@Override
	public void delete() {
		securityAdmin.delete(this, true);
	}

	Condition[] getConditions(BundlePermissions bundlePermissions) {
		synchronized (bundleConditionsLock) {
			Condition[] conditions = null;
			if (bundleConditions != null) {
				conditions = bundleConditions.get(bundlePermissions);
			}
			if (conditions == null) {
				conditions = new Condition[conditionInfos.length];
				for (int i = 0; i < conditionInfos.length; i++) {
					/*
					 * TODO: Can we pre-get the Constructors in our own constructor
					 */
					Class<?> clazz;
					try {
						clazz = Class.forName(conditionInfos[i].getType());
					} catch (ClassNotFoundException e) {
						/* If the class isn't there, we fail */
						return null;
					}
					Constructor<?> constructor = null;
					Method method = getConditionMethod(clazz);
					if (method == null) {
						constructor = getConditionConstructor(clazz);
						if (constructor == null) {
							// TODO should post a FrameworkEvent of type error here
							conditions[i] = Condition.FALSE;
							continue;
						}
					}

					Object[] args = { bundlePermissions.getBundle(), conditionInfos[i] };
					try {
						if (method != null)
							conditions[i] = (Condition) method.invoke(null, args);
						else
							conditions[i] = (Condition) constructor.newInstance(args);
					} catch (Exception e) {
						// TODO should post a FrameworkEvent of type error here
						conditions[i] = Condition.FALSE;
					}
				}
				if (bundleConditions != null) {
					bundleConditions.put(bundlePermissions, conditions);
				}
			}
			return conditions;
		}
	}

	private Method getConditionMethod(Class<?> clazz) {
		for (Method checkMethod : clazz.getMethods()) {
			if (checkMethod.getName().equals("getCondition") //$NON-NLS-1$
					&& (checkMethod.getModifiers() & Modifier.STATIC) == Modifier.STATIC //
					&& checkParameterTypes(checkMethod.getParameterTypes())) {
				return checkMethod;
			}
		}
		return null;
	}

	private Constructor<?> getConditionConstructor(Class<?> clazz) {
		for (Constructor<?> checkConstructor : clazz.getConstructors()) {
			if (checkParameterTypes(checkConstructor.getParameterTypes())) {
				return checkConstructor;
			}
		}
		return null;
	}

	private boolean checkParameterTypes(Class<?>[] foundTypes) {
		if (foundTypes.length != conditionMethodArgs.length) {
			return false;
		}

		for (int i = 0; i < foundTypes.length; i++) {
			if (!foundTypes[i].isAssignableFrom(conditionMethodArgs[i])) {
				return false;
			}
		}
		return true;
	}

	Decision evaluate(BundlePermissions bundlePermissions, Permission permission) {
		if (bundleConditions == null || bundlePermissions == null)
			return evaluatePermission(bundlePermissions, permission);
		Condition[] conditions = getConditions(bundlePermissions);
		if (conditions == ABSTAIN_LIST)
			return DECISION_ABSTAIN;
		if (conditions == SATISFIED_LIST)
			return evaluatePermission(bundlePermissions, permission);

		boolean empty = true;
		List<Condition> postponedConditions = null;
		Decision postponedPermCheck = null;
		for (int i = 0; i < conditions.length; i++) {
			Condition condition = conditions[i];
			if (condition == null)
				continue; // this condition must have been satisfied && !mutable in a previous check
			if (!isPostponed(condition)) {
				// must call isMutable before calling isSatisfied according to the
				// specification.
				boolean mutable = condition.isMutable();
				if (condition.isSatisfied()) {
					if (!mutable)
						conditions[i] = null; // ignore this condition for future checks
				} else {
					if (!mutable)
						// this will cause the row to always abstain; mark this to be ignored in future
						// checks
						synchronized (bundleConditionsLock) {
							bundleConditions.put(bundlePermissions, ABSTAIN_LIST);
						}
					return DECISION_ABSTAIN;
				}
			} else { // postponed case
				if (postponedPermCheck == null)
					// perform a permission check now
					postponedPermCheck = evaluatePermission(bundlePermissions, permission);
				if (postponedPermCheck == DECISION_ABSTAIN)
					return postponedPermCheck; // no need to postpone the condition if the row abstains
				// this row will deny or allow the permission; must queue the postponed
				// condition
				if (postponedConditions == null)
					postponedConditions = new ArrayList<>(1);
				postponedConditions.add(condition);
			}
			empty &= conditions[i] == null;
		}
		if (empty) {
			synchronized (bundleConditionsLock) {
				bundleConditions.put(bundlePermissions, SATISFIED_LIST);
			}
		}
		if (postponedPermCheck != null)
			return new Decision(postponedPermCheck.decision | SecurityTable.POSTPONED,
					postponedConditions.toArray(new Condition[postponedConditions.size()]), this, bundlePermissions);
		return evaluatePermission(bundlePermissions, permission);
	}

	private boolean isPostponed(Condition condition) {
		// postponed checks can only happen if we are using a supported security manager
		return condition.isPostponed() && securityAdmin.getSupportedSecurityManager() != null;
	}

	private Decision evaluatePermission(BundlePermissions bundlePermissions, Permission permission) {
		return permissionInfoCollection.implies(bundlePermissions, permission)
				? (deny ? DECISION_DENIED : DECISION_GRANTED)
				: DECISION_ABSTAIN;
	}

	@Override
	public String toString() {
		return getEncoded();
	}

	@Override
	public String getEncoded() {
		return getEncoded(name, conditionInfos, internalGetPermissionInfos(), deny);
	}

	@Override
	public boolean equals(Object obj) {
		// doing the simple (slow) thing for now
		if (obj == this)
			return true;
		if (!(obj instanceof ConditionalPermissionInfo))
			return false;
		// we assume the encoded string provides a canonical (comparable) form
		return getEncoded().equals(((ConditionalPermissionInfo) obj).getEncoded());
	}

	@Override
	public int hashCode() {
		return getHashCode(name, internalGetConditionInfos(), internalGetPermissionInfos(), getAccessDecision());
	}

	static int getHashCode(String name, ConditionInfo[] conds, PermissionInfo[] perms, String decision) {
		int h = 31 * 17 + decision.hashCode();
		for (ConditionInfo cond : conds) {
			h = 31 * h + cond.hashCode();
		}
		for (PermissionInfo perm : perms) {
			h = 31 * h + perm.hashCode();
		}
		if (name != null)
			h = 31 * h + name.hashCode();
		return h;
	}

	static String getEncoded(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos,
			boolean deny) {
		StringBuilder result = new StringBuilder();
		if (deny)
			result.append(ConditionalPermissionInfo.DENY);
		else
			result.append(ConditionalPermissionInfo.ALLOW);
		result.append(" { "); //$NON-NLS-1$
		if (conditionInfos != null)
			for (ConditionInfo conditionInfo : conditionInfos) {
				result.append(conditionInfo.getEncoded()).append(' ');
			}
		if (permissionInfos != null)
			for (PermissionInfo permissionInfo : permissionInfos) {
				result.append(permissionInfo.getEncoded()).append(' ');
			}
		result.append('}');
		if (name != null) {
			result.append(" \""); //$NON-NLS-1$
			escapeString(name, result);
			result.append('"');
		}
		return result.toString();
	}

	PermissionInfoCollection getPermissionInfoCollection() {
		return permissionInfoCollection;
	}

	void clearCaches() {
		permissionInfoCollection.clearPermissionCache();
		if (bundleConditions != null)
			synchronized (bundleConditionsLock) {
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

		void handleImmutable(Condition condition, boolean isSatisfied, boolean mutable) {
			if (mutable || !condition.isPostponed())
				return; // do nothing
			if (isSatisfied) {
				synchronized (row.bundleConditionsLock) {
					Condition[] rowConditions = row.bundleConditions.get(bundlePermissions);
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
				synchronized (row.bundleConditionsLock) {
					row.bundleConditions.put(bundlePermissions, ABSTAIN_LIST);
				}
			}
		}
	}
}
