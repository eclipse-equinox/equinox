/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.security.AccessControlContext;
import java.util.Enumeration;
import java.util.Vector;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 *
 * Implements ConditionalPermissionAdmin.
 * 
 * @version $Revision$
 */
public class ConditionalPermissionAdminImpl implements ConditionalPermissionAdmin {
	/**
	 * The Vector of current ConditionalPermissionInfos.  
	 */
	Vector condPerms = new Vector();
	Framework framework;
	PermissionStorage storage;

	/**
	 * @param framework
	 * @param permissionStorage
	 */
	public ConditionalPermissionAdminImpl(Framework framework, PermissionStorage permissionStorage) {
		this.framework = framework;
		this.storage = permissionStorage;
		// TODO: need to deserialize from storage
	}

	/**
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#addConditionalPermissionInfo(org.osgi.service.condpermadmin.ConditionInfo[], org.osgi.service.permissionadmin.PermissionInfo[])
	 */
	public ConditionalPermissionInfo addConditionalPermissionInfo(ConditionInfo[] conds, PermissionInfo[] perms) {
		ConditionalPermissionInfoImpl condPermInfo = new ConditionalPermissionInfoImpl(conds, perms);
		condPerms.add(condPermInfo);
		// TODO We need to save it off here
		return condPermInfo;
	}

	/**
	 * Returns an Enumeration of current ConditionalPermissionInfos. Each element will be of type 
	 * ConditionalPermissionInfoImpl.
	 * @return an Enumeration of current ConditionalPermissionInfos.
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#getConditionalPermissionInfos()
	 */
	public Enumeration getConditionalPermissionInfos() {
		return condPerms.elements();
	}

	/**
	 * @see org.osgi.service.condpermadmin.ConditionalPermissionAdmin#getAccessControlContext(java.lang.String[])
	 */
	public AccessControlContext getAccessControlContext(String[] signers) {
		// TODO This may not need to be implemented if MEG doesn't need it.
		return null;
	}
}
