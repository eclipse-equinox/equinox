/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev - bug 203599 - events if jobs bundle has been stopped     
 *******************************************************************************/
package org.eclipse.core.internal.registry.osgi;

import java.io.File;
import java.util.Map;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The registry strategy used by the Equinox extension registry. Adds to the
 * OSGi registry:
 * <p><ul>
 * <li>Use debug information supplied via .options files</li> 
 * <li>Use Eclipse logging - Use Eclipse platform state for cache validation</li>
 * <li>Event scheduling is done using Eclipse job scheduling mechanism</li>
 * </ul></p> 
 * 
 * @since org.eclipse.equinox.registry 3.2
 */
public class EquinoxRegistryStrategy extends RegistryStrategyOSGI {

	public static final String PLUGIN_NAME = "org.eclipse.equinox.registry"; //$NON-NLS-1$
	public static final String OPTION_DEBUG = PLUGIN_NAME + "/debug"; //$NON-NLS-1$
	public static final String OPTION_DEBUG_EVENTS = PLUGIN_NAME + "/debug/events"; //$NON-NLS-1$

	private static boolean DEBUG_ECLIPSE_REGISTRY = OSGIUtils.getDefault().getBooleanDebugOption(OPTION_DEBUG, false);
	private static boolean DEBUG_ECLIPSE_EVENTS = OSGIUtils.getDefault().getBooleanDebugOption(OPTION_DEBUG_EVENTS, false);

	private boolean useJobs = true;

	public EquinoxRegistryStrategy(File[] theStorageDir, boolean[] cacheReadOnly, Object key) {
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

	public long getContainerTimestamp() {
		BundleContext context = Activator.getContext();
		if (context == null) {
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.bundle_not_activated, null));
			return -1;
		}
		// use a string here instead of the class to prevent class loading.
		ServiceReference ref = context.getServiceReference("org.eclipse.osgi.service.resolver.PlatformAdmin"); //$NON-NLS-1$
		if (ref == null)
			return -1;
		return EquinoxUtils.getContainerTimestamp(context, ref);
	}

	/**
	 * This method will attempt to use Eclipse Jobs mechanism to schedule registry events. If, at any time, 
	 * Eclipse Jobs mechanism is missing, this method will fallback on the "internal" event scheduling
	 * provided by the registry strategy.
	 * 
	 * Once the switch to the fallback mechanism occurred, no further attempt to use scheduling from the Jobs bundle
	 * will be made (until registry bundle is restarted). Avoiding repeated checks in this scenario will ensure that 
	 * most users see no performance degradation and that order of registry events remains consistent. 
	 */
	public final void scheduleChangeEvent(Object[] listeners, Map deltas, Object registry) {
		if (useJobs) {
			try {
				new ExtensionEventDispatcherJob(listeners, deltas, registry).schedule();
				return; // all done - most typical use case
			} catch (NoClassDefFoundError e) {
				useJobs = false; // Jobs are missing
			} catch (IllegalStateException e) {
				useJobs = false; // Jobs bundles was stopped
			}
		}
		super.scheduleChangeEvent(listeners, deltas, registry);
	}

}
