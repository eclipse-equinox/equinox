/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String MESSAGES_NAME = "org.eclipse.equinox.internal.app.messages"; //$NON-NLS-1$

	// application
	public static String application_invalidExtension;
	public static String application_noIdFound;
	public static String application_notFound;
	public static String application_returned;
	public static String application_errorStartDefault;
	public static String application_error_stopping;
	public static String application_error_state_stopped;
	public static String application_error_starting;
	public static String application_error_noMainThread;
	public static String application_instance_stopped;

	// product
	public static String provider_invalid;
	public static String provider_invalid_general;
	public static String product_notFound;

	// scheduled
	public static String scheduled_app_removed;
	public static String scheduled_app_launch_error;

	// persistence
	public static String persistence_error_saving;

	// singletons
	public static String singleton_running;
	public static String apps_running;
	public static String main_running;
	public static String max_running;

	// console
	public static String console_help_app_commands_header;
	public static String console_help_activeapps_description;
	public static String console_help_apps_description;
	public static String console_help_arguments;
	public static String console_help_lockapp_description;
	public static String console_help_schedapp_arguments;
	public static String console_help_schedapp_description;
	public static String console_help_startapp_description;
	public static String console_help_stopapp_description;
	public static String console_help_unlockapp_description;
	public static String console_help_unschedapp_description;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(MESSAGES_NAME, Messages.class);
	}

}
