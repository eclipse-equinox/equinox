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
	String ECLIPSE_PARAMETERS = "Eclipse-Parameters"; //$NON-NLS-1$
	/**
	 * An element for Eclipse-Parameters header.
	 */
	String AUTOMATE = "automate"; //$NON-NLS-1$
	/**
	 * An attribute for the "automate" header, meaning that this bundle should be 
	 * automatically started during classloading.
	 */
	String START_ATTRIBUTE = "start"; //$NON-NLS-1$
	/**
	 * An attribute for the "automate" header, meaning that this bundle should be 
	 * automatically stopped during shutdown.
	 */	
	String STOP_ATTRIBUTE = "stop"; //$NON-NLS-1$
	/**
	 * The "Plugin-Class" header.
	 */	
	String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$
	/**
	 * The "Legacy" header.
	 */
	String LEGACY = "Legacy"; //$NON-NLS-1$	
}
