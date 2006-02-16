/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.osgi;

import java.io.File;
import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.PlatformAdmin;

/**
 * The registry strategy used by the Equinox extension registry. Adds to the OSGi registry:
 * - Use debug information supplied via .options files
 * - Use Eclipse logging
 * - Use Eclipse platform state for cache validation
 * - Supplied alternative cache location (primarily used with shared installs)
 * 
 * @since org.eclipse.equinox.registry 3.2
 */
public class EquinoxRegistryStrategy extends RegistryStrategyOSGI {

	public static final String PLUGIN_NAME = "org.eclipse.equinox.registry"; //$NON-NLS-1$
	public static final String OPTION_DEBUG = PLUGIN_NAME + "/debug"; //$NON-NLS-1$
	public static final String OPTION_DEBUG_EVENTS = PLUGIN_NAME + "/debug/events"; //$NON-NLS-1$

	private static boolean DEBUG_ECLIPSE_REGISTRY = OSGIUtils.getDefault().getBooleanDebugOption(OPTION_DEBUG, false);
	private static boolean DEBUG_ECLIPSE_EVENTS = OSGIUtils.getDefault().getBooleanDebugOption(OPTION_DEBUG_EVENTS, false);

	public EquinoxRegistryStrategy(File theStorageDir, boolean cacheReadOnly, Object key) {
		super(theStorageDir, cacheReadOnly, key);
	}

	public boolean debug() {
		return DEBUG_ECLIPSE_REGISTRY;
	}

	public boolean debugRegistryEvents() {
		return DEBUG_ECLIPSE_EVENTS;
	}
	
	public final void log(IStatus status) {
		RuntimeLog.log(status);
	}

	public long cacheComputeState() {
		PlatformAdmin admin = OSGIUtils.getDefault().getPlatformAdmin();
		return admin == null ? -1 : admin.getState(false).getTimeStamp();
	}

	// Eclipse extension registry cache can be found in one of the two locations:
	// a) in the local configuration area (standard location passed in by the platform)
	// b) in the shared configuration area (typically, shared install is used) 
	public File cacheAlternativeLocation() {
		// In debug be careful of LocationManager.computeSharedConfigurationLocation
		// and PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area" - it
		// seems to work differently comparing to stand-alone execution.
		Location currentLocation = OSGIUtils.getDefault().getConfigurationLocation();
		if (currentLocation == null)
			return null;
		Location parentLocation = currentLocation.getParentLocation();
		if (parentLocation == null)
			return null;
		String theRegistryLocation = parentLocation.getURL().getFile() + '/' + IRegistryConstants.RUNTIME_NAME;
		return new File(theRegistryLocation);
	}

}
