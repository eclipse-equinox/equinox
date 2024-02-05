/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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

import java.util.Objects;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Abstract super-class for scope context object contributed by the Platform.
 *
 * @since 3.0
 */
public abstract class AbstractScope implements IScopeContext {

	/*
	 * Default path hierarchy for nodes is /<scope>/<qualifier>.
	 *
	 * @see
	 * org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
	 */
	@Override
	public IEclipsePreferences getNode(String qualifier) {
		if (qualifier == null) {
			throw new IllegalArgumentException();
		}
		return (IEclipsePreferences) PreferencesService.getDefault().getRootNode().node(getName()).node(qualifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IScopeContext other //
				&& getName().equals(other.getName()) //
				&& Objects.equals(getLocation(), other.getLocation());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getLocation());
	}
}
