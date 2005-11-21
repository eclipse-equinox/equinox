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
 * Provides initialization of the legacy preferences as described in
 * the Plugin class.
 * 
 * @depreceated
 */
public interface ILegacyPreferences {
	/**
	 * The method tries to initialize the preferences using the legacy Plugin method.
	 * 
	 * @param object - plugin to initialize
	 * @param name - ID of the plugin to be initialized
	 * 
	 * @see Plugin#initializeDefaultPluginPreferences
	 */
	public void init(Object object, String name);
}
