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
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public abstract class SingletonEclipsePreferences extends EclipsePreferences {
	// cached values
	private final String qualifier;
	private final int segmentCount;
	private IPath location;
	private IEclipsePreferences loadLevel;
	private final Set<String> loadedNodes;
	private final AtomicBoolean initialized;

	SingletonEclipsePreferences(EclipsePreferences parent, String name, Set<String> loadedNodes,
			AtomicBoolean initialized) {
		super(parent, name);
		this.loadedNodes = loadedNodes;
		this.initialized = initialized;

		initializeChildren();

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);
		// cache the qualifier
		qualifier = segmentCount < 2 ? null : getSegment(path, 1);
	}

	abstract IPath getBaseLocation();

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
		if (location == null && qualifier != null) {
			location = computeLocation(getBaseLocation(), qualifier);
		}
		return location;
	}

	/*
	 * Return the node at which these preferences are loaded/saved.
	 */
	@Override
	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null) {
				return null;
			}
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 2; i < segmentCount; i++) {
				node = (IEclipsePreferences) node.parent();
			}
			loadLevel = node;
		}
		return loadLevel;
	}

	/*
	 * Initialize the children for the root of this node. Store the names as keys in
	 * the children table so we can lazily load them later.
	 */
	protected void initializeChildren() {
		if (initialized.get() || parent == null) {
			return;
		}
		try {
			synchronized (this) {
				IPath baseLocation = getBaseLocation();
				if (baseLocation != null) {
					for (String n : computeChildren(baseLocation)) {
						addChild(n, null);
					}
				}
			}
		} finally {
			initialized.set(true);
		}
	}

}
