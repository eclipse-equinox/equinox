/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

import org.osgi.framework.BundleActivator;

/**
 * A factory for creating bundle activators for an equinox hook.
 */
public interface ActivatorHookFactory {
	/**
	 * Creates an activator for an equinox hook. The returned activator will be
	 * called when the system bundle is started and stopped.
	 */
	public BundleActivator createActivator();
}
