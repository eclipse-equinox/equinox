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
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;

public class SecurityTableUpdate implements ConditionalPermissionUpdate {

	private final SecurityAdmin securityAdmin;
	private final List rows;
	private final long timeStamp;

	public SecurityTableUpdate(SecurityAdmin securityAdmin, SecurityRow[] rows, long timeStamp) {
		this.securityAdmin = securityAdmin;
		this.timeStamp = timeStamp;
		// must make a snap shot of the security rows.
		this.rows = new ArrayList(rows.length);
		for (int i = 0; i < rows.length; i++)
			// Use SecurityRowSnapShot to prevent modification before commit 
			// and to throw exceptions from delete
			this.rows.add(new SecurityRowSnapShot(rows[i].getName(), rows[i].internalGetConditionInfos(), rows[i].internalGetPermissionInfos(), rows[i].getGrantDecision()));
	}

	public boolean commit() {
		return securityAdmin.commit(rows, timeStamp);
	}

	public List getConditionalPermissionInfos() {
		// it is fine to return the internal list; it is a snap shot and we allow clients to modify it.
		return rows;
	}

}
