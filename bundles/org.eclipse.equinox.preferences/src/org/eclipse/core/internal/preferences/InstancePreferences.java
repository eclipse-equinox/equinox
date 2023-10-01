/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences;

import java.util.*;
import org.eclipse.core.internal.runtime.MetaDataKeeper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * @since 3.0
 */
public class InstancePreferences extends EclipsePreferences {

	// cached values
	private String qualifier;
	private int segmentCount;
	private IEclipsePreferences loadLevel;
	private IPath location;
	// cache which nodes have been loaded from disk
	private static Set<String> loadedNodes = Collections.synchronizedSet(new HashSet<String>());
	private static boolean initialized = false;
	private static IPath baseLocation;

	/* package */static IPath getBaseLocation() {
		// If we are running with -data=@none we won't have an instance location.
		// By leaving the value of baseLocation as null we still allow the users
		// to set preferences in this scope but the values will not be persisted
		// to disk when #flush() is called.
		if (baseLocation == null) {
			Location instanceLocation = PreferencesOSGiUtils.getDefault().getInstanceLocation();
			if (instanceLocation != null && (instanceLocation.isSet() || instanceLocation.allowsDefault()))
				baseLocation = MetaDataKeeper.getMetaArea().getStateLocation(IPreferencesConstants.RUNTIME_NAME);
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
		super(parent, name);

		initializeChildren();

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);
		if (segmentCount < 2)
			return;

		// cache the qualifier
		qualifier = getSegment(path, 1);

		// don't cache the location until later in case instance prefs are
		// accessed before the instance location is set.
	}

	@Override
	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.name());
	}

	@Override
	protected void loaded() {
		loadedNodes.add(name());
	}

	@Override
	protected IPath getLocation() {
		if (location == null)
			location = computeLocation(getBaseLocation(), qualifier);
		return location;
	}

	/*
	 * Return the node at which these preferences are loaded/saved.
	 */
	@Override
	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null)
				return null;
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 2; i < segmentCount; i++)
				node = (IEclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	/*
	 * Initialize the children for the root of this node. Store the names as keys in
	 * the children table so we can lazily load them later.
	 */
	protected void initializeChildren() {
		if (initialized || parent == null)
			return;
		try {
			synchronized (this) {
				for (String n : computeChildren(getBaseLocation())) {
					addChild(n, null);
				}
			}
		} finally {
			initialized = true;
		}
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new InstancePreferences(nodeParent, nodeName);
	}
}
