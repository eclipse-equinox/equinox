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
import org.osgi.service.condpermadmin.ConditionalPermissionInfoBase;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityInfoBase implements ConditionalPermissionInfoBase {

	private final String name;
	private final ConditionInfo[] conditionInfos;
	private final PermissionInfo[] permissionInfos;
	private final String decision;

	public SecurityInfoBase(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos, String decision) {
		this.name = name;
		this.conditionInfos = conditionInfos;
		this.permissionInfos = permissionInfos;
		this.decision = decision;
	}

	public ConditionInfo[] getConditionInfos() {
		return conditionInfos;
	}

	public String getGrantDecision() {
		return decision;
	}

	public String getName() {
		return name;
	}

	public PermissionInfo[] getPermissionInfos() {
		return permissionInfos;
	}

}
