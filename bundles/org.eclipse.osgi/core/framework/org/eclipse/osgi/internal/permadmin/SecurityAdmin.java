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

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;
import org.eclipse.osgi.framework.internal.core.*;
import org.osgi.framework.AdminPermission;
import org.osgi.service.condpermadmin.*;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class SecurityAdmin implements PermissionAdmin, ConditionalPermissionAdmin {
	private static final PermissionCollection DEFAULT_DEFAULT;
	static {
		AllPermission allPerm = new AllPermission();
		DEFAULT_DEFAULT = allPerm.newPermissionCollection();
		if (DEFAULT_DEFAULT != null)
			DEFAULT_DEFAULT.add(allPerm);
	}

	private static final String ADMIN_IMPLIED_ACTIONS = AdminPermission.RESOURCE + ',' + AdminPermission.METADATA + ',' + AdminPermission.CLASS;

	/* @GuardedBy(lock) */
	private final PermissionAdminTable permAdminTable = new PermissionAdminTable();
	/* @GuardedBy(lock) */
	private SecurityTable condAdminTable;
	/* @GuardedBy(lock) */
	private PermissionInfoCollection permAdminDefaults;
	/* @GuardedBy(lock) */
	private long timeStamp = 0;
	/* @GuardedBy(lock) */
	private long nextID = System.currentTimeMillis();
	/* @GuardedBy(lock) */
	private final PermissionStorage permissionStorage;
	private final Object lock = new Object();
	private final Framework framework;
	private final PermissionInfo[] impliedPermissionInfos;
	private final EquinoxSecurityManager supportedSecurityManager;

	public SecurityAdmin(EquinoxSecurityManager supportedSecurityManager, Framework framework, PermissionStorage permissionStorage) throws IOException {
		this.supportedSecurityManager = supportedSecurityManager;
		this.framework = framework;
		this.permissionStorage = new SecurePermissionStorage(permissionStorage);
		this.impliedPermissionInfos = SecurityAdminUtils.getPermissionInfos(getClass().getResource(Constants.OSGI_BASE_IMPLIED_PERMISSIONS), framework);
		String[] encodedDefaultInfos = permissionStorage.getPermissionData(null);
		PermissionInfo[] defaultInfos = getPermissionInfos(encodedDefaultInfos);
		if (defaultInfos != null)
			permAdminDefaults = new PermissionInfoCollection(defaultInfos);
		String[] locations = permissionStorage.getLocations();
		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				String[] encodedLocationInfos = permissionStorage.getPermissionData(locations[i]);
				if (encodedLocationInfos != null) {
					PermissionInfo[] locationInfos = getPermissionInfos(encodedLocationInfos);
					permAdminTable.setPermissions(locations[i], locationInfos);
				}
			}
		}
		String[] encodedCondPermInfos = permissionStorage.getConditionalPermissionInfos();
		if (encodedCondPermInfos == null)
			condAdminTable = new SecurityTable(this, new SecurityRow[0]);
		else {
			SecurityRow[] rows = new SecurityRow[encodedCondPermInfos.length];
			for (int i = 0; i < rows.length; i++)
				rows[i] = SecurityRow.createSecurityRow(this, encodedCondPermInfos[i]);
			condAdminTable = new SecurityTable(this, rows);
		}
	}

	private static PermissionInfo[] getPermissionInfos(String[] encodedInfos) {
		if (encodedInfos == null)
			return null;
		PermissionInfo[] results = new PermissionInfo[encodedInfos.length];
		for (int i = 0; i < results.length; i++)
			results[i] = new PermissionInfo(encodedInfos[i]);
		return results;
	}

	boolean checkPermission(Permission permission, BundlePermissions bundlePermissions) {
		// check permissions by location
		PermissionInfoCollection locationCollection;
		SecurityTable curCondAdminTable;
		PermissionInfoCollection curPermAdminDefaults;
		// save off the current state of the world while holding the lock
		synchronized (lock) {
			// get location the hard way to avoid permission check
			locationCollection = permAdminTable.getCollection(bundlePermissions.getBundle().getBundleData().getLocation());
			curCondAdminTable = condAdminTable;
			curPermAdminDefaults = permAdminDefaults;
		}
		if (locationCollection != null)
			return locationCollection.implies(permission);
		// if conditional admin table is empty the fall back to defaults
		if (curCondAdminTable.isEmpty())
			return curPermAdminDefaults != null ? curPermAdminDefaults.implies(permission) : DEFAULT_DEFAULT.implies(permission);
		// check the condition table	
		int result = curCondAdminTable.evaluate(bundlePermissions, permission);
		if ((result & SecurityTable.GRANTED) != 0)
			return true;
		if ((result & SecurityTable.DENIED) != 0)
			return false;
		if ((result & SecurityTable.POSTPONED) != 0)
			return true;
		return false;
	}

	public PermissionInfo[] getDefaultPermissions() {
		synchronized (lock) {
			if (permAdminDefaults == null)
				return null;
			return permAdminDefaults.getPermissionInfos();
		}
	}

	public String[] getLocations() {
		synchronized (lock) {
			String[] results = permAdminTable.getLocations();
			return results.length == 0 ? null : results;
		}
	}

	public PermissionInfo[] getPermissions(String location) {
		synchronized (lock) {
			return permAdminTable.getPermissions(location);
		}
	}

	public void setDefaultPermissions(PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			if (permissions == null)
				permAdminDefaults = null;
			else
				permAdminDefaults = new PermissionInfoCollection(permissions);
			try {
				permissionStorage.setPermissionData(null, getEncodedPermissionInfos(permissions));
			} catch (IOException e) {
				// log
				e.printStackTrace();
			}
		}
	}

	private static void checkAllPermission() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AllPermission());
	}

	private static String[] getEncodedPermissionInfos(PermissionInfo[] permissions) {
		if (permissions == null)
			return null;
		String[] encoded = new String[permissions.length];
		for (int i = 0; i < encoded.length; i++)
			encoded[i] = permissions[i].getEncoded();
		return encoded;
	}

	public void setPermissions(String location, PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			permAdminTable.setPermissions(location, permissions);
			try {
				permissionStorage.setPermissionData(location, getEncodedPermissionInfos(permissions));
			} catch (IOException e) {
				// TODO log
				e.printStackTrace();
			}
		}
	}

	void delete(SecurityRow securityRow, boolean firstTry) {
		ConditionalPermissionsUpdate update = createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		for (Iterator iRows = rows.iterator(); iRows.hasNext();) {
			ConditionalPermissionInfoBase info = (ConditionalPermissionInfoBase) iRows.next();
			if (securityRow.getName().equals(info.getName())) {
				iRows.remove();
				synchronized (lock) {
					if (!update.commit()) {
						if (firstTry)
							// try again
							delete(securityRow, false);
					}
				}
				break;
			}
		}
	}

	public ConditionalPermissionInfo addConditionalPermissionInfo(ConditionInfo[] conds, PermissionInfo[] perms) {
		return setConditionalPermissionInfo(null, conds, perms, true);
	}

	public ConditionalPermissionInfoBase createConditionalPermissionInfoBase(String name, ConditionInfo[] conditions, PermissionInfo[] permissions, String decision) {
		return new SecurityInfoBase(name, conditions, permissions, decision);
	}

	public ConditionalPermissionsUpdate createConditionalPermissionsUpdate() {
		synchronized (lock) {
			return new SecurityTableUpdate(this, condAdminTable.getRows(), timeStamp);
		}
	}

	public AccessControlContext getAccessControlContext(String[] signers) {
		// TODO Auto-generated method stub
		return null;
	}

	public ConditionalPermissionInfo getConditionalPermissionInfo(String name) {
		synchronized (lock) {
			return condAdminTable.getRow(name);
		}
	}

	public Enumeration getConditionalPermissionInfos() {
		// could implement our own Enumeration, but we don't care about performance here.  Just do something simple:
		synchronized (lock) {
			SecurityRow[] rows = condAdminTable.getRows();
			Vector vRows = new Vector(rows.length);
			for (int i = 0; i < rows.length; i++)
				vRows.add(rows[i]);
			return vRows.elements();
		}
	}

	public ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds, PermissionInfo[] perms) {
		return setConditionalPermissionInfo(name, conds, perms, true);
	}

	private ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds, PermissionInfo[] perms, boolean firstTry) {
		ConditionalPermissionsUpdate update = createConditionalPermissionsUpdate();
		List rows = update.getConditionalPermissionInfoBases();
		ConditionalPermissionInfoBase infoBase = createConditionalPermissionInfoBase(name, conds, perms, ConditionalPermissionInfoBase.ALLOW);
		int index = -1;
		if (name == null) {
			rows.add(infoBase);
			index = rows.size() - 1;
		} else {
			for (int i = 0; i < rows.size() && index < 0; i++) {
				ConditionalPermissionInfoBase info = (ConditionalPermissionInfoBase) rows.get(i);
				if (name.equals(info.getName())) {
					rows.set(i, infoBase);
					index = i;
				}
			}
			if (index < 0) {
				rows.add(infoBase);
				index = rows.size() - 1;
			}
		}
		synchronized (lock) {
			if (!update.commit()) {
				if (firstTry)
					// try again
					setConditionalPermissionInfo(name, conds, perms, false);
			}
			return condAdminTable.getRow(index);
		}
	}

	boolean commit(List rows, long updateStamp) {
		checkAllPermission();
		synchronized (lock) {
			if (updateStamp != timeStamp)
				return false;
			SecurityRow[] newRows = new SecurityRow[rows.size()];
			Collection names = new ArrayList();
			for (int i = 0; i < newRows.length; i++) {
				Object rowObj = rows.get(i);
				if (!(rowObj instanceof ConditionalPermissionInfoBase))
					throw new IllegalStateException("Invalid type \"" + rowObj.getClass().getName() + "\" at row: " + i);
				ConditionalPermissionInfoBase infoBaseRow = (ConditionalPermissionInfoBase) rowObj;
				String name = infoBaseRow.getName();
				if (name == null)
					name = generateName();
				if (names.contains(name))
					throw new IllegalStateException("Duplicate name \"" + name + "\" at row: " + i);
				newRows[i] = new SecurityRow(this, name, infoBaseRow.getConditionInfos(), infoBaseRow.getPermissionInfos(), infoBaseRow.getGrantDecision());
			}
			condAdminTable = new SecurityTable(this, newRows);
			try {
				permissionStorage.saveConditionalPermissionInfos(condAdminTable.getEncodedRows());
			} catch (IOException e) {
				// TODO log
				e.printStackTrace();
			}
			timeStamp += 1;
			return true;
		}
	}

	/* GuardedBy(lock) */
	private String generateName() {
		return "generated_" + Long.toString(nextID++); //$NON-NLS-1$;
	}

	public EquinoxProtectionDomain createProtectionDomain(AbstractBundle bundle) {
		PermissionInfoCollection impliedPermissions = getImpliedPermission(bundle);
		PermissionInfo[] restrictedInfos = getFileRelativeInfos(SecurityAdminUtils.getPermissionInfos(bundle.getEntry("OSGI-INF/permissions.perm"), framework), bundle); //$NON-NLS-1$
		PermissionInfoCollection restrictedPermissions = restrictedInfos == null ? null : new PermissionInfoCollection(restrictedInfos);
		BundlePermissions bundlePermissions = new BundlePermissions(bundle, this, impliedPermissions, restrictedPermissions);
		return new EquinoxProtectionDomain(bundlePermissions);
	}

	private PermissionInfoCollection getImpliedPermission(AbstractBundle bundle) {
		if (impliedPermissionInfos == null)
			return null;
		// create the implied AdminPermission actions for this bundle
		PermissionInfo impliedAdminPermission = new PermissionInfo(AdminPermission.class.getName(), "(id=" + bundle.getBundleId() + ")", ADMIN_IMPLIED_ACTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		PermissionInfo[] bundleImpliedInfos = new PermissionInfo[impliedPermissionInfos.length + 1];
		System.arraycopy(impliedPermissionInfos, 0, bundleImpliedInfos, 0, impliedPermissionInfos.length);
		bundleImpliedInfos[impliedPermissionInfos.length] = impliedAdminPermission;
		return new PermissionInfoCollection(getFileRelativeInfos(bundleImpliedInfos, bundle));
	}

	private PermissionInfo[] getFileRelativeInfos(PermissionInfo[] permissionInfos, AbstractBundle bundle) {
		if (permissionInfos == null)
			return null;
		PermissionInfo[] results = new PermissionInfo[permissionInfos.length];
		for (int i = 0; i < permissionInfos.length; i++) {
			results[i] = permissionInfos[i];
			if ("java.io.FilePermission".equals(permissionInfos[i].getType())) { //$NON-NLS-1$
				File file = new File(permissionInfos[i].getName());
				if (!file.isAbsolute()) { // relative name
					File target = bundle.getBundleData().getDataFile(permissionInfos[i].getName());
					if (target != null)
						results[i] = new PermissionInfo(permissionInfos[i].getType(), target.getPath(), permissionInfos[i].getActions());
				}
			}
		}
		return results;
	}

	public void clearCaches() {
		PermissionInfoCollection[] permAdminCollections;
		SecurityRow[] condAdminRows;
		synchronized (lock) {
			permAdminCollections = permAdminTable.getCollections();
			condAdminRows = condAdminTable.getRows();
		}
		for (int i = 0; i < permAdminCollections.length; i++)
			permAdminCollections[i].clearPermissionCache();
		for (int i = 0; i < condAdminRows.length; i++)
			condAdminRows[i].clearCaches();
	}

	EquinoxSecurityManager getSupportedSecurityManager() {
		return supportedSecurityManager != null ? supportedSecurityManager : getSupportedSystemSecurityManager();
	}

	static private EquinoxSecurityManager getSupportedSystemSecurityManager() {
		try {
			EquinoxSecurityManager equinoxManager = (EquinoxSecurityManager) System.getSecurityManager();
			return equinoxManager != null && equinoxManager.inCheckPermission() ? equinoxManager : null;
		} catch (ClassCastException e) {
			return null;
		}
	}
}
