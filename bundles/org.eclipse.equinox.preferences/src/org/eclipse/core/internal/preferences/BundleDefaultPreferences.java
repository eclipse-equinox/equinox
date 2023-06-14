/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.*;

/**
 * This class represents a preference node in the "bundle_defaults" scope. This scope is
 * used to represent default values which are set by the bundle in either its preference
 * initializer or in a file included with the bundle.
 *
 * This differs from the regular default scope because it does not contain values set
 * by the product preference customization or the command-line.
 *
 * @since 3.3
 */
public class BundleDefaultPreferences extends EclipsePreferences {

	private static Set<String> loadedNodes = Collections.synchronizedSet(new HashSet<String>());
	private String qualifier;
	private int segmentCount;
	private IEclipsePreferences loadLevel;

	/*
	 * Default constructor.
	 */
	public BundleDefaultPreferences() {
		this(null, null);
	}

	private BundleDefaultPreferences(EclipsePreferences parent, String name) {
		super(parent, name);
		// cache the segment count
		IPath path = IPath.fromOSString(absolutePath());
		segmentCount = path.segmentCount();
		if (segmentCount < 2)
			return;

		// cache the qualifier
		String scope = path.segment(0);
		if (BundleDefaultsScope.SCOPE.equals(scope))
			qualifier = path.segment(1);

		// cache the location
		if (qualifier == null)
			return;
	}


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


	@Override
	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.name());
	}


	@Override
	protected void loaded() {
		loadedNodes.add(name());
	}


	@Override
	protected void load() {
		// ensure that the same node in the "default" scope is loaded so this one is
		// initialized properly
		String relativePath = DefaultPreferences.getScopeRelativePath(absolutePath());
		if (relativePath != null) {
			// touch the node to force a load
			PreferencesService.getDefault().getRootNode().node(DefaultScope.SCOPE).node(relativePath);
		}
	}


	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new BundleDefaultPreferences(nodeParent, nodeName);
	}
}
