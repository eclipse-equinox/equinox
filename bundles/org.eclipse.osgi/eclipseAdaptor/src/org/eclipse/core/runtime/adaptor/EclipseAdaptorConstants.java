/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

/**
 * Declares constants for manifest headers introduced by the Eclipse adaptor. 
 */
interface EclipseAdaptorConstants {
	public static final String PI_ECLIPSE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	/**
	 * The "Eclipse-AutoStart" header.
	 */
	String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$
	/**
	 * The "Eclipse-AutoStop" header.
	 */
	String ECLIPSE_AUTOSTOP = "Eclipse-AutoStop"; //$NON-NLS-1$
	//TODO decide what to do with this header 
	String LEGACY = "Legacy"; //$NON-NLS-1$
	//TODO rename it to Eclipse-PluginClass	
	String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$
	/**
	 * The "exceptions" attribute for ECLIPSE_AUTOSTART header.
	 */
	String EXCEPTIONS_ATTRIBUTE = "exceptions"; //$NON-NLS-1$
}