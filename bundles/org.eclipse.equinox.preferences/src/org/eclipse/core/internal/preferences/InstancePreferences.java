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
import org.eclipse.core.internal.runtime.MetaDataKeeper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * @since 3.0
 */
public class InstancePreferences extends SingletonEclipsePreferences {

	// cache which nodes have been loaded from disk
	private static final Set<String> LOADED_NODES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
	private static IPath baseLocation;

	@Override
	IPath getBaseLocation() {
		// If we are running with -data=@none we won't have an instance location.
		// By leaving the value of baseLocation as null we still allow the users
		// to set preferences in this scope but the values will not be persisted
		// to disk when #flush() is called.
		if (baseLocation == null) {
			Location instanceLocation = PreferencesOSGiUtils.getDefault().getInstanceLocation();
			if (instanceLocation != null && (instanceLocation.isSet() || instanceLocation.allowsDefault())) {
				baseLocation = MetaDataKeeper.getMetaArea().getStateLocation(IPreferencesConstants.RUNTIME_NAME);
			}
		}
		return baseLocation;
	}

	/**
	 * Default constructor. Should only be called by #createExecutableExtension.
	 */
	public InstancePreferences() {
		this(null, null);
	}

	private InstancePreferences(EclipsePreferences parent, String name) {
		super(parent, name, LOADED_NODES, INITIALIZED);
		// don't cache the location until later in case instance prefs are
		// accessed before the instance location is set.
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new InstancePreferences(nodeParent, nodeName);
	}
}
