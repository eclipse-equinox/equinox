/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.event.mapper;

/**
 * @version $Revision: 1.1 $
 */
public interface Constants {
	// constants for Event common properties; event specific properties are
	// defined in the corresponding event adapter.
	public static final String BUNDLE = "bundle"; //$NON-NLS-1$
	public static final String BUNDLE_ID = "bundle.id"; //$NON-NLS-1$
	public static final String BUNDLE_SYMBOLICNAME = "bundle.symbolicName"; //$NON-NLS-1$
	public static final String EVENT = "event"; //$NON-NLS-1$
	public static final String EXCEPTION = "exception"; //$NON-NLS-1$
	public static final String EXCEPTION_CLASS = "exception.class"; //$NON-NLS-1$
	public static final String EXCEPTION_MESSAGE = "exception.message"; //$NON-NLS-1$
	public static final String MESSAGE = "message"; //$NON-NLS-1$
	public static final String SERVICE = "service"; //$NON-NLS-1$
	public static final String SERVICE_ID = "service.id"; //$NON-NLS-1$
	public static final String SERVICE_OBJECTCLASS = "service.objectClass"; //$NON-NLS-1$
	public static final String SERVICE_PID = "service.pid"; //$NON-NLS-1$
	public static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
	public static final char TOPIC_SEPARATOR = '/';
}