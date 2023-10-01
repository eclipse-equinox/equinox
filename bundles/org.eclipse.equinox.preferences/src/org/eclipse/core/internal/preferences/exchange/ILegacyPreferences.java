/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences.exchange;

/**
 * Provides initialization of the legacy preferences as described in the Plugin
 * class.
 *
 * @deprecated
 */
public interface ILegacyPreferences {
	/**
	 * The method tries to initialize the preferences using the legacy
	 * Plugin#initializeDefaultPluginPreferences method.
	 *
	 * @param object - plugin to initialize
	 * @param name   - ID of the plugin to be initialized
	 */
	public Object init(Object object, String name);
}
