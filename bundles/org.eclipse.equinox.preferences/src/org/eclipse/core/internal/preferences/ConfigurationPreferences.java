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

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * @since 3.0
 */
public class ConfigurationPreferences extends EclipsePreferences {

	// cached values
	private int segmentCount;
	private String qualifier;
	private IPath location;
	private IEclipsePreferences loadLevel;
	// cache which nodes have been loaded from disk
	private static Set<String> loadedNodes = Collections.synchronizedSet(new HashSet<String>());
	private static boolean initialized = false;
	private static IPath baseLocation;

	static {
		Location location = PreferencesOSGiUtils.getDefault().getConfigurationLocation();
		if (location != null) {
			URL url = location.getURL();
			if (url != null)
				baseLocation = new Path(url.getFile());
		}
	}

	/**
	 * Default constructor. Should only be called by #createExecutableExtension.
	 */
	public ConfigurationPreferences() {
		this(null, null);
	}

	private ConfigurationPreferences(EclipsePreferences parent, String name) {
		super(parent, name);

		initializeChildren();

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);
		if (segmentCount < 2)
			return;

		// cache the qualifier
		qualifier = getSegment(path, 1);

		// cache the location
		if (qualifier == null)
			return;
		if (baseLocation != null)
			location = computeLocation(baseLocation, qualifier);
	}

	@Override
	protected IPath getLocation() {
		return location;
	}

	@Override
	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.name());
	}

	@Override
	protected void loaded() {
		loadedNodes.add(name());
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
				node = (EclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	protected void initializeChildren() {
		if (initialized || parent == null)
			return;
		try {
			synchronized (this) {
				if (baseLocation == null)
					return;
				for (String n : computeChildren(baseLocation)) {
					addChild(n, null);
				}
			}
		} finally {
			initialized = true;
		}
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new ConfigurationPreferences(nodeParent, nodeName);
	}
}
