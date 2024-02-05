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
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class RootPreferences extends EclipsePreferences {

	public RootPreferences() {
		super(null, ""); //$NON-NLS-1$
	}

	@Override
	public void flush() throws BackingStoreException {
		// flush all children
		BackingStoreException exception = null;
		String[] names = childrenNames();
		for (String n : names) {
			try {
				node(n).flush();
			} catch (BackingStoreException e) {
				// store the first exception we get and still try and flush
				// the rest of the children.
				if (exception == null) {
					exception = e;
				}
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	private synchronized IEclipsePreferences getChild(String key) {
		if (children == null) {
			return null;
		}
		Object value = children.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof IEclipsePreferences eclipsePreferences) {
			return eclipsePreferences;
		}
		// lazy initialization
		IEclipsePreferences child = PreferencesService.getDefault().createNode(key);
		addChild(key, child);
		return child;
	}

	@Override
	public Preferences node(String path) {
		return getNode(path, true); // create if not found
	}

	public Preferences getNode(String path, boolean create) {
		if (path.length() == 0 || (path.length() == 1 && path.charAt(0) == IPath.SEPARATOR)) {
			return this;
		}
		int startIndex = path.charAt(0) == IPath.SEPARATOR ? 1 : 0;
		int endIndex = path.indexOf(IPath.SEPARATOR, startIndex + 1);
		String scope = path.substring(startIndex, endIndex == -1 ? path.length() : endIndex);
		IEclipsePreferences child;
		if (create) {
			child = getChild(scope);
			if (child == null) {
				child = new EclipsePreferences(this, scope);
				addChild(scope, child);
			}
		} else {
			child = getChild(scope, null, false);
			if (child == null) {
				return null;
			}
		}
		return child.node(endIndex == -1 ? "" : path.substring(endIndex + 1)); //$NON-NLS-1$
	}

	@Override
	public void sync() throws BackingStoreException {
		// sync all children
		BackingStoreException exception = null;
		String[] names = childrenNames();
		for (String n : names) {
			try {
				node(n).sync();
			} catch (BackingStoreException e) {
				// store the first exception we get and still try and sync
				// the rest of the children.
				if (exception == null) {
					exception = e;
				}
			}
		}
		if (exception != null) {
			throw exception;
		}
	}
}
