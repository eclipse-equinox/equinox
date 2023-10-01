/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.ui.storage.view;

import org.eclipse.equinox.security.storage.ISecurePreferences;

public interface ISecurePreferencesSelection {
	/**
	 * Called by child elements to inform container that current selection has been
	 * modified
	 * 
	 * @param selectedNode newly selected element
	 */
	public void setSelection(ISecurePreferences selectedNode);

	/**
	 * Called by child elements to inform container that information has been
	 * modified.
	 */
	public void modified();
}
