package org.osgi.service.condpermadmin;

import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * This interface describes a binding of a set of Conditions to a set of
 * Permissions. Instances of this interface are obtained from the
 * ConditionalPermissionAdmin service. This interface is also used to remove
 * ConditionalPermissionCollections from ConditionPermissionAdmin.
 */
public interface ConditionalPermissionInfo {
	/**
	 * Returns the ConditionInfos for the Conditions that must be satisfied to
	 * enable this ConditionalPermissionCollection.
	 */
	ConditionInfo[] getConditionInfos();

	/**
	 * Returns the PermissionInfos for the Permission in this
	 * ConditionalPermissionCollection.
	 */
	PermissionInfo[] getPermissionInfos();

	/**
	 * Removes the ConditionalPermissionCollection from the
	 * ConditionalPermissionAdmin.
	 */
	void delete();
}
