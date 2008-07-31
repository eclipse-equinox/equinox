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

import java.util.ArrayList;
import java.util.List;
import org.osgi.service.condpermadmin.ConditionalPermissionsUpdate;

public class SecurityTableUpdate implements ConditionalPermissionsUpdate {

	private final SecurityAdmin securityAdmin;
	private final List rows;
	private final long timeStamp;

	public SecurityTableUpdate(SecurityAdmin securityAdmin, SecurityRow[] rows, long timeStamp) {
		this.securityAdmin = securityAdmin;
		this.timeStamp = timeStamp;
		this.rows = new ArrayList(rows.length);
		for (int i = 0; i < rows.length; i++)
			this.rows.add(new SecurityInfoBase(rows[i].getName(), rows[i].getConditionInfos(), rows[i].getPermissionInfos(), rows[i].getGrantDecision()));
	}

	public boolean commit() {
		return securityAdmin.commit(rows, timeStamp);
	}

	public List getConditionalPermissionInfoBases() {
		return rows;
	}

}
