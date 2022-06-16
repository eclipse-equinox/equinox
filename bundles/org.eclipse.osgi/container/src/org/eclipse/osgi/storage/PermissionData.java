/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.storage;

import java.io.*;
import java.util.*;

/**
 * Permission Storage interface for managing a persistent storage of
 * bundle permissions.
 *
 * <p>This class is used to provide methods to manage
 * persistent storage of bundle permissions.
 */
public class PermissionData {
	private static final int PERMDATA_VERSION = 1;
	private final Map<String, String[]> locations = new HashMap<>();
	private String[] defaultInfos;
	private String[] condPermInfos;
	private boolean dirty;

	/**
	 * Returns the locations that have permission data assigned to them,
	 * that is, locations for which permission data
	 * exists in persistent storage.
	 *
	 * @return The locations that have permission data in
	 * persistent storage, or <tt>null</tt> if there is no permission data
	 * in persistent storage.
	 */
	public String[] getLocations() {
		synchronized (locations) {
			String[] result = new String[locations.size()];
			int i = 0;
			for (Iterator<String> iLocs = locations.keySet().iterator(); iLocs.hasNext(); i++)
				result[i] = iLocs.next();
			return result;
		}
	}

	/**
	 * Gets the permission data assigned to the specified
	 * location.
	 *
	 * @param location The location whose permission data is to
	 * be returned.
	 * The location can be <tt>null</tt> for the default permission data.
	 *
	 * @return The permission data assigned to the specified
	 * location, or <tt>null</tt> if that location has not been assigned any
	 * permission data.
	 */
	public String[] getPermissionData(String location) {
		if (location == null)
			return defaultInfos;
		synchronized (locations) {
			if (locations.size() == 0)
				return null;
			return locations.get(location);
		}
	}

	/**
	 * Assigns the specified permission data to the specified
	 * location.
	 *
	 * @param location The location that will be assigned the
	 * permissions.
	 * The location can be <tt>null</tt> for the default permission data.
	 * @param data The permission data to be assigned, or <tt>null</tt>
	 * if the specified location is to be removed from persistent storaqe.
	 */
	public void setPermissionData(String location, String[] data) {
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
	}

	/**
	 * Persists the array of encoded ConditionalPermissionInfo strings
	 * @param infos an array of encoded ConditionalPermissionInfo strings
	 */
	public void saveConditionalPermissionInfos(String[] infos) {
		condPermInfos = infos;
		setDirty(true);
	}

	/**
	 * Returns the persistent array of encoded ConditionalPermissionInfo strings
	 * @return an array of encoded ConditionalPermissionInfo strings or null
	 * if none exist in persistent storage.
	 */
	public String[] getConditionalPermissionInfos() {
		return condPermInfos;
	}

	boolean isDirty() {
		return dirty;
	}

	private void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	void readPermissionData(DataInputStream in) throws IOException {
		int version = in.readInt();
		int dataSize = in.readInt();
		byte[] bytes = new byte[dataSize];
		in.readFully(bytes);
		if (PERMDATA_VERSION == version) {
			DataInputStream temp = new DataInputStream(new ByteArrayInputStream(bytes));
			try {
				// read the default permissions first
				int numPerms = temp.readInt();
				if (numPerms > 0) {
					String[] perms = new String[numPerms];
					for (int i = 0; i < numPerms; i++)
						perms[i] = temp.readUTF();
					setPermissionData(null, perms);
				}
				int numLocs = temp.readInt();
				if (numLocs > 0) {
					for (int i = 0; i < numLocs; i++) {
						String loc = temp.readUTF();
						numPerms = temp.readInt();
						String[] perms = new String[numPerms];
						for (int j = 0; j < numPerms; j++)
							perms[j] = temp.readUTF();
						setPermissionData(loc, perms);
					}
				}
				int numCondPerms = temp.readInt();
				if (numCondPerms > 0) {
					String[] condPerms = new String[numCondPerms];
					for (int i = 0; i < numCondPerms; i++) {
						condPerms[i] = temp.readUTF();
					}
					saveConditionalPermissionInfos(condPerms);
				}

			} finally {
				setDirty(false);
				temp.close();
			}
		}
	}

	void savePermissionData(DataOutputStream out) throws IOException {
		out.writeInt(PERMDATA_VERSION);
		// create a temporary in memory stream so we can figure out the length
		ByteArrayOutputStream tempBytes = new ByteArrayOutputStream();
		DataOutputStream temp = new DataOutputStream(tempBytes);
		// always write the default permissions first
		String[] defaultPerms = getPermissionData(null);
		temp.writeInt(defaultPerms == null ? 0 : defaultPerms.length);
		if (defaultPerms != null)
			for (String defaultPerm : defaultPerms) {
				temp.writeUTF(defaultPerm);
			}
		String[] locs = getLocations();
		temp.writeInt(locs == null ? 0 : locs.length);
		if (locs != null)
			for (String loc : locs) {
				temp.writeUTF(loc);
				String[] perms = getPermissionData(loc);
				temp.writeInt(perms == null ? 0 : perms.length);
				if (perms != null) {
					for (String perm : perms) {
						temp.writeUTF(perm);
					}
				}
			}
		String[] condPerms = getConditionalPermissionInfos();
		temp.writeInt(condPerms == null ? 0 : condPerms.length);
		if (condPerms != null)
			for (String condPerm : condPerms) {
				temp.writeUTF(condPerm);
			}
		temp.close();

		out.writeInt(tempBytes.size());
		out.write(tempBytes.toByteArray());
		setDirty(false);
	}
}
