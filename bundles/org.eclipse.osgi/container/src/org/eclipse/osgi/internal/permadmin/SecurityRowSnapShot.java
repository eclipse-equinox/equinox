/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityRowSnapShot implements ConditionalPermissionInfo {

	private final String name;
	private final ConditionInfo[] conditionInfos;
	private final PermissionInfo[] permissionInfos;
	private final String decision;

	public SecurityRowSnapShot(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos, String decision) {
		if (permissionInfos == null || permissionInfos.length == 0)
			throw new IllegalArgumentException("It is invalid to have empty permissionInfos"); //$NON-NLS-1$
		decision = decision.toLowerCase();
		boolean d = ConditionalPermissionInfo.DENY.equals(decision);
		boolean a = ConditionalPermissionInfo.ALLOW.equals(decision);
		if (!(d | a))
			throw new IllegalArgumentException("Invalid decision: " + decision); //$NON-NLS-1$
		conditionInfos = conditionInfos == null ? new ConditionInfo[0] : conditionInfos;
		this.name = name;
		// must create copies of the passed in arrays to prevent changes
		this.conditionInfos = (ConditionInfo[]) SecurityRow.cloneArray(conditionInfos);
		this.permissionInfos = (PermissionInfo[]) SecurityRow.cloneArray(permissionInfos);
		this.decision = decision;
	}

	public ConditionInfo[] getConditionInfos() {
		return (ConditionInfo[]) SecurityRow.cloneArray(conditionInfos);
	}

	public String getAccessDecision() {
		return decision;
	}

	public String getName() {
		return name;
	}

	public PermissionInfo[] getPermissionInfos() {
		return (PermissionInfo[]) SecurityRow.cloneArray(permissionInfos);
	}

	/**
	 * @deprecated
	 */
	public void delete() {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return getEncoded();
	}

	public String getEncoded() {
		return SecurityRow.getEncoded(name, conditionInfos, permissionInfos, DENY.equalsIgnoreCase(decision));
	}

	public boolean equals(Object obj) {
		// doing the simple (slow) thing for now
		if (obj == this)
			return true;
		if (!(obj instanceof ConditionalPermissionInfo))
			return false;
		// we assume the encoded string provides a canonical (comparable) form
		return getEncoded().equals(((ConditionalPermissionInfo) obj).getEncoded());
	}

	public int hashCode() {
		return SecurityRow.getHashCode(name, conditionInfos, permissionInfos, getAccessDecision());
	}

}
