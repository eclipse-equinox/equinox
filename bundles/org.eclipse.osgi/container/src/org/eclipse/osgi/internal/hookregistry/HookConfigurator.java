/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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

package org.eclipse.osgi.internal.hookregistry;

/**
 * A hook configurator is used to add hooks to the hook registry.
 * @see HookRegistry
 */
public interface HookConfigurator {
	/**
	 * Adds hooks to the specified hook registry.
	 * @param hookRegistry the hook registry used to add hooks
	 */
	public void addHooks(HookRegistry hookRegistry);
}
