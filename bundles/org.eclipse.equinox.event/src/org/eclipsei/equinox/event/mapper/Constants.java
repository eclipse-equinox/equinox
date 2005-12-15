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
package org.eclipsei.equinox.event.mapper;

/**
 * @version $Revision: 1.4 $
 */
public interface Constants {
	// constants for Event common properties; event specific properties are
	// defined in the corresponding event adapter.
	public static final String	BUNDLE				= "bundle";
	public static final String	BUNDLE_ID			= "bundle.id";
	public static final String	BUNDLE_SYMBOLICNAME	= "bundle.symbolicName";
	public static final String	EVENT				= "event";
	public static final String	EXCEPTION			= "exception";
	public static final String	EXCEPTION_CLASS		= "exception.class";
	public static final String	EXCEPTION_MESSAGE	= "exception.message";
	public static final String	MESSAGE				= "message";
	public static final String	SERVICE				= "service";
	public static final String	SERVICE_ID			= "service.id";
	public static final String	SERVICE_OBJECTCLASS	= "service.objectClass";
	public static final String	SERVICE_PID			= "service.pid";
	public static final String	TIMESTAMP			= "timestamp";
	public static final char	TOPIC_SEPARATOR		= '/';
}