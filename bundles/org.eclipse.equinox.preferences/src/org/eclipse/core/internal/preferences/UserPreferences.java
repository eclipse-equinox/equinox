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
package org.eclipse.core.internal.preferences;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.UserScope;

public class UserPreferences extends SingletonEclipsePreferences {

	// cache which nodes have been loaded from disk
	private static final Set<String> LOADED_NODES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

	/**
	 * Default constructor. Should only be called by #createExecutableExtension.
	 */
	public UserPreferences() {
		this(null, null);
	}

	private UserPreferences(EclipsePreferences parent, String name) {
		super(parent, name, LOADED_NODES, INITIALIZED);
	}

	@Override
	IPath getBaseLocation() {
		return UserScope.INSTANCE.getLocation();
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new UserPreferences(nodeParent, nodeName);
	}
}
