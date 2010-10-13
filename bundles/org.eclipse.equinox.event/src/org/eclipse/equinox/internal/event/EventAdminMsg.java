/*******************************************************************************
 * Copyright (c) 1999, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.event;

import org.eclipse.osgi.util.NLS;

public class EventAdminMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.event.ExternalMessages"; //$NON-NLS-1$

	public static String EVENT_ASYNC_THREAD_NAME;
	public static String EVENT_NULL_EVENT;
	public static String EVENT_NO_TOPICPERMISSION_PUBLISH;
	public static String EVENT_DISPATCH_HANDLER_EXCEPTION;
	public static String EVENT_INVALID_HANDLER_FILTER;
	public static String EVENT_INVALID_HANDLER_TOPICS;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, EventAdminMsg.class);
	}
}
