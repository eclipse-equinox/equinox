/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;

public class BasePermissionStorage implements PermissionStorage {

	private Map<String, String[]> locations = new HashMap<String, String[]>();
	private String[] defaultInfos;
	private String[] condPermInfos;
	private BaseStorage storage;
	private boolean dirty;

	BasePermissionStorage(BaseStorage storage) {
		this.storage = storage;
	}

	/**
	 * @throws IOException  
	 */
	public String[] getLocations() throws IOException {
		synchronized (locations) {
			String[] result = new String[locations.size()];
			int i = 0;
			for (Iterator<String> iLocs = locations.keySet().iterator(); iLocs.hasNext(); i++)
				result[i] = iLocs.next();
			return result;
		}
	}

	/**
	 * @throws IOException  
	 */
	public String[] getPermissionData(String location) throws IOException {
		if (location == null)
			return defaultInfos;
		synchronized (locations) {
			if (locations.size() == 0)
				return null;
			return locations.get(location);
		}
	}

	/**
	 * @throws IOException  
	 */
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

	/**
	 * @throws IOException  
	 */
	public void saveConditionalPermissionInfos(String[] infos) throws IOException {
		condPermInfos = infos;
		setDirty(true);
		storage.requestSave();
	}

	/**
	 * @throws IOException  
	 */
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
