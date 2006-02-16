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
package org.eclipse.core.internal.registry.osgi;

import java.io.File;
import java.util.Map;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.osgi.service.resolver.PlatformAdmin;

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
		PlatformAdmin admin = OSGIUtils.getDefault().getPlatformAdmin();
		return admin == null ? -1 : admin.getState(false).getTimeStamp();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// Use Eclipse job scheduling mechanism

	private final static class ExtensionEventDispatcherJob extends Job {
		// an "identy rule" that forces extension events to be queued		
		private final static ISchedulingRule EXTENSION_EVENT_RULE = new ISchedulingRule() {
			public boolean contains(ISchedulingRule rule) {
				return rule == this;
			}

			public boolean isConflicting(ISchedulingRule rule) {
				return rule == this;
			}
		};
		private Map deltas;
		private Object[] listenerInfos;
		private Object registry;

		public ExtensionEventDispatcherJob(Object[] listenerInfos, Map deltas, Object registry) {
			// name not NL'd since it is a system job
			super("Registry event dispatcher"); //$NON-NLS-1$
			setSystem(true);
			this.listenerInfos = listenerInfos;
			this.deltas = deltas;
			this.registry = registry;
			// all extension event dispatching jobs use this rule
			setRule(EXTENSION_EVENT_RULE);
		}

		public IStatus run(IProgressMonitor monitor) {
			return RegistryStrategy.processChangeEvent(listenerInfos, deltas, registry);
		}
	}

	public final void scheduleChangeEvent(Object[] listeners, Map deltas, Object registry) {
		new ExtensionEventDispatcherJob(listeners, deltas, registry).schedule();
	}

}
