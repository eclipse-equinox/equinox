/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;

public class SecurityTableUpdate implements ConditionalPermissionUpdate {

	private final SecurityAdmin securityAdmin;
	private final List<ConditionalPermissionInfo> rows;
	private final long timeStamp;

	public SecurityTableUpdate(SecurityAdmin securityAdmin, SecurityRow[] rows, long timeStamp) {
		this.securityAdmin = securityAdmin;
		this.timeStamp = timeStamp;
		// must make a snap shot of the security rows.
		this.rows = new ArrayList<>(rows.length);
		for (SecurityRow row : rows) {
			// Use SecurityRowSnapShot to prevent modification before commit
			// and to throw exceptions from delete
			this.rows.add(new SecurityRowSnapShot(row.getName(), row.internalGetConditionInfos(), row.internalGetPermissionInfos(), row.getAccessDecision()));
		}
	}

	@Override
	public boolean commit() {
		return securityAdmin.commit(rows, timeStamp);
	}

	@Override
	public List<ConditionalPermissionInfo> getConditionalPermissionInfos() {
		// it is fine to return the internal list; it is a snap shot and we allow clients to modify it.
		return rows;
	}

}
