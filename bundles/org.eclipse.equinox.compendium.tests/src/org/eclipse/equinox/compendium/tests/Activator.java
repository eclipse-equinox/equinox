/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
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
package org.eclipse.equinox.compendium.tests;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.*;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {
	public static String BUNDLE_COORDINATOR = "org.eclipse.equinox.coordinator"; //$NON-NLS-1$
	public static String BUNDLE_EVENT = "org.eclipse.equinox.event"; //$NON-NLS-1$
	public static String BUNDLE_METATYPE = "org.eclipse.equinox.metatype"; //$NON-NLS-1$
	public static String BUNDLE_USERADMIN = "org.eclipse.equinox.useradmin"; //$NON-NLS-1$

	public void start(BundleContext bc) throws Exception {
		FrameworkUtil.getBundle(org.apache.felix.scr.info.ScrInfo.class).start();
	}

	public void stop(BundleContext bc) throws Exception { // no-op
	}

	public static BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(Activator.class).getBundleContext();
	}

	public static Bundle getBundle(String symbolicName) {
		return Platform.getBundle(symbolicName);
	}
}
