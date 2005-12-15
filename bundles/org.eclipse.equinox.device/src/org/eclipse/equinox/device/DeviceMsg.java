/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import org.eclipse.osgi.util.NLS;

public class DeviceMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.device.ExternalMessages"; //$NON-NLS-1$

	public static String DeviceManager_started;
	public static String Device_service_unregistered;
	public static String Device_noDriverFound_called;
	public static String Multiple_Driver_services_with_the_same_DRIVER_ID;
	public static String DeviceManager_Update_Wait;
	public static String Driver_service_has_no_DRIVER_ID_property;
	public static String Device_attached_by_DRIVER_ID;
	public static String Device_referred_to;
	public static String Unable_to_create_Filter_for_DeviceManager;
	public static String DeviceManager_has_thrown_an_error;
	public static String Device_noDriverFound_error;
	public static String DriverLocator_unable_to_load_driver;
	public static String DriverLocator_error_calling_findDrivers;
	public static String Unable_to_install_or_start_driver_bundle;
	public static String Unable_to_uninstall_driver_bundle;
	public static String Unable_to_uninstall_driver_bundle_number;
	public static String DriverSelector_error_during_match;
	public static String Driver_service_has_no_DRIVER_ID;
	public static String Driver_error_during_match;
	public static String Driver_error_during_attach;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, DeviceMsg.class);
	}
}