/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.useradmin;

import org.eclipse.osgi.util.NLS;

public class UserAdminMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.useradmin.ExternalMessages"; //$NON-NLS-1$

	public static String adding_Credential_to__15;
	public static String adding_member__18;
	public static String adding_required_member__21;
	public static String removing_member__24;
	public static String Unable_to_load_role__27;
	public static String Backing_Store_Read_Exception;
	public static String Backing_Store_Write_Exception;
	public static String Event_Delivery_Exception;
	public static String CREATE_NULL_ROLE_EXCEPTION;
	public static String CREATE_INVALID_TYPE_ROLE_EXCEPTION;
	public static String INVALID_KEY_EXCEPTION;
	public static String INVALID_VALUE_EXCEPTION;
	public static String USERADMIN_UNREGISTERED_EXCEPTION;
	public static String Service_Vendor;
	public static String OSGi_User_Admin_service_IBM_Implementation_3;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, UserAdminMsg.class);
	}
}