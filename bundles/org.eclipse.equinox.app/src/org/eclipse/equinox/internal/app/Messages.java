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
package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.app.messages"; //$NON-NLS-1$

	// application
	public static String application_invalidExtension;
	public static String application_noIdFound;
	public static String application_notFound;
	public static String application_returned;

	// product
	public static String provider_invalid;
	public static String provider_invalid_general;
	public static String product_notFound;

	// container
	public static String container_notFound;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String EclipseScheduledApplication_7;
}
