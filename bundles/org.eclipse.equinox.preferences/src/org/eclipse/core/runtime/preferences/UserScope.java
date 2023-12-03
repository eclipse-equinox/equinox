/*******************************************************************************
 * Copyright (c) 2023, 2024 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.preferences;

import org.eclipse.core.internal.preferences.AbstractScope;
import org.eclipse.core.runtime.IPath;

/**
 * @since 3.11
 */
public class UserScope extends AbstractScope {

	/**
	 * String constant (value of <code>"user"</code>) used for the scope name for
	 * the user preference scope.
	 */
	public static final String SCOPE = "user"; //$NON-NLS-1$

	private static final IPath USER_HOME_PREFERENCE_LOCATION;
	static {
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		USER_HOME_PREFERENCE_LOCATION = IPath.forWindows(userHome).append(".eclipse"); //$NON-NLS-1$
	}

	/**
	 * Singleton instance of a User Scope object. Typical usage is:
	 * <code>UserScope.INSTANCE.getNode(...);</code>
	 *
	 * @since 3.4
	 */
	public static final IScopeContext INSTANCE = new UserScope();

	private UserScope() { // static use only via INSTANCE
	}

	@Override
	public String getName() {
		return SCOPE;
	}

	@Override
	public IPath getLocation() {
		return USER_HOME_PREFERENCE_LOCATION;
	}

}
