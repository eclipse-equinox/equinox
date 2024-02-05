/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Unify Configuration- and InstancePreferences into common super-class
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.ConfigurationScope;

/**
 * @since 3.0
 */
public class ConfigurationPreferences extends SingletonEclipsePreferences {

	// cache which nodes have been loaded from disk
	private static final Set<String> LOADED_NODES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
	private static final IPath BASE_LOCATION = ConfigurationScope.INSTANCE.getLocation();

	/**
	 * Default constructor. Should only be called by #createExecutableExtension.
	 */
	public ConfigurationPreferences() {
		this(null, null);
	}

	private ConfigurationPreferences(EclipsePreferences parent, String name) {
		super(parent, name, LOADED_NODES, INITIALIZED);
	}

	@Override
	IPath getBaseLocation() {
		return BASE_LOCATION;
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new ConfigurationPreferences(nodeParent, nodeName);
	}
}
