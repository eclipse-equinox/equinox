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

import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityRowSnapShot implements ConditionalPermissionInfo {

	private final String name;
	private final ConditionInfo[] conditionInfos;
	private final PermissionInfo[] permissionInfos;
	private final String decision;

	public SecurityRowSnapShot(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos, String decision) {
		this.name = name;
		// must create copies of the passed in arrays to prevent changes
		this.conditionInfos = (ConditionInfo[]) SecurityRow.cloneArray(conditionInfos);
		this.permissionInfos = (PermissionInfo[]) SecurityRow.cloneArray(permissionInfos);
		this.decision = decision;
	}

	public ConditionInfo[] getConditionInfos() {
		return (ConditionInfo[]) SecurityRow.cloneArray(conditionInfos);
	}

	public String getGrantDecision() {
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

}
