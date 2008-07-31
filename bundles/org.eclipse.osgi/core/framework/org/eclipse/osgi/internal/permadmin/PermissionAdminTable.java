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

import java.util.HashMap;
import org.osgi.service.permissionadmin.PermissionInfo;

public class PermissionAdminTable {
	private final HashMap locations = new HashMap();

	String[] getLocations() {
		return (String[]) locations.keySet().toArray(new String[locations.size()]);
	}

	PermissionInfo[] getPermissions(String location) {
		PermissionInfoCollection collection = (PermissionInfoCollection) locations.get(location);
		if (collection != null)
			return collection.getPermissionInfos();
		return null;
	}

	void setPermissions(String location, PermissionInfo[] permissions) {
		if (permissions == null) {
			locations.remove(location);
			return;
		}
		locations.put(location, new PermissionInfoCollection(permissions));
	}

	PermissionInfoCollection getCollection(String location) {
		return (PermissionInfoCollection) locations.get(location);
	}

	PermissionInfoCollection[] getCollections() {
		String[] currentLocations = getLocations();
		PermissionInfoCollection[] results = new PermissionInfoCollection[currentLocations.length];
		for (int i = 0; i < results.length; i++)
			results[i] = getCollection(currentLocations[i]);
		return results;
	}
}
