/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.hookregistry;

import org.osgi.framework.BundleActivator;

/**
 * A factory for creating bundle activators for an
 * equinox hook.
 */
public interface ActivatorHookFactory {
	/**
	 * Creates an activator for an equinox hook.  The returned
	 * activator will be called when the system bundle is
	 * started and stopped.
	 *
	 */
	public BundleActivator createActivator();
}
