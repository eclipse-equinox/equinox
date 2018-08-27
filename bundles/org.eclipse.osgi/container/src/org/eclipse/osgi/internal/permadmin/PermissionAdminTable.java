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

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.permissionadmin.PermissionInfo;

public class PermissionAdminTable {
	private final Map<String, PermissionInfoCollection> locations = new HashMap<>();

	String[] getLocations() {
		return locations.keySet().toArray(new String[locations.size()]);
	}

	PermissionInfo[] getPermissions(String location) {
		PermissionInfoCollection collection = locations.get(location);
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
		return locations.get(location);
	}

	PermissionInfoCollection[] getCollections() {
		String[] currentLocations = getLocations();
		PermissionInfoCollection[] results = new PermissionInfoCollection[currentLocations.length];
		for (int i = 0; i < results.length; i++)
			results[i] = getCollection(currentLocations[i]);
		return results;
	}
}
