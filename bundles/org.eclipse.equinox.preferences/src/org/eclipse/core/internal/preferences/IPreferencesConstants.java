/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

/**
 * Container for the constants used by this plugin. 
 * @since org.eclipse.equinox.preferences 1.0
 */
public interface IPreferencesConstants {
	/**
	 * Backward compatibilty: name of the original runtime plugin
	 */
	public static final String RUNTIME_NAME = "org.eclipse.core.runtime"; //$NON-NLS-1$

	/**
	 * Name of this plugin
	 */
	public static final String PREFERS_NAME = "org.eclipse.equinox.preferences"; //$NON-NLS-1$

	/**
	 * Command line options
	 */
	public static final String PLUGIN_CUSTOMIZATION = "-plugincustomization"; //$NON-NLS-1$

}
