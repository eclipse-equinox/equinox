/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor;

/**
 * A hook configurator is used to add hooks to the hook registry.
 * @see HookRegistry
 * @since 3.2
 */
public interface HookConfigurator {
	/**
	 * Adds hooks to the specified hook registry.
	 * @param hookRegistry the hook registry used to add hooks
	 */
	public void addHooks(HookRegistry hookRegistry);
}
