/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;

public class BasePermissionStorage implements PermissionStorage {

	private HashMap locations = new HashMap();
	private String[] defaultInfos;
	private String[] condPermInfos;
	private BaseStorage storage;
	private boolean dirty;

	BasePermissionStorage(BaseStorage storage) {
		this.storage = storage;
	}

	public String[] getLocations() throws IOException {
		synchronized (locations) {
			String[] result = new String[locations.size()];
			int i = 0;
			for (Iterator iLocs = locations.keySet().iterator(); iLocs.hasNext(); i++)
				result[i] = (String) iLocs.next();
			return result;
		}
	}

	public String[] getPermissionData(String location) throws IOException {
		if (location == null)
			return defaultInfos;
		synchronized (locations) {
			if (locations.size() == 0)
				return null;
			return (String[]) locations.get(location);
		}
	}

	public void setPermissionData(String location, String[] data) throws IOException {
		if (location == null) {
			defaultInfos = data;
			return;
		}
		synchronized (locations) {
			if (data == null)
				locations.remove(location);
			else
				locations.put(location, data);
		}
		setDirty(true);
		storage.requestSave();
	}

	public void saveConditionalPermissionInfos(String[] infos) throws IOException {
		condPermInfos = infos;
		setDirty(true);
		storage.requestSave();
	}

	public String[] getConditionalPermissionInfos() throws IOException {
		return condPermInfos;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
}
