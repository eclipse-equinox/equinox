/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log;

import org.eclipse.osgi.util.NLS;

public class LogMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.log.ExternalMessages"; //$NON-NLS-1$

	public static String Log_created_Log_Size;
	public static String Log_modified_Log_Size;
	public static String Log_modified_Log_Threshold;
	public static String OSGi_Log_Service_IBM_Implementation;
	public static String BundleEvent;
	public static String ServiceEvent;
	public static String FrameworkEvent;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, LogMsg.class);
	}
}