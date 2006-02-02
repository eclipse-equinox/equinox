/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.util.Hashtable;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Implementation for the runtime shutdown hook that provides 
 * support for legacy bundles. All legacy bundles are stopped 
 * in the proper order.
 * 
 * <p>Internal class.</p>
 */
public class BundleStopper {
	/* must be a synchronized object */
	private Hashtable stoppedBundles;
	private BundleDescription[] allToStop = null;
	private BundleContext context;
	private FrameworkAdaptor adaptor;

	public BundleStopper(BundleContext context, FrameworkAdaptor adaptor) {
		this.context = context;
		this.adaptor = adaptor;
	}

	private void logCycles(Object[][] cycles) {
		if (!Debug.DEBUG || !Debug.DEBUG_ENABLED)
			return;

		// log cycles
		if (cycles.length > 0) {
			StringBuffer cycleText = new StringBuffer("["); //$NON-NLS-1$			
			for (int i = 0; i < cycles.length; i++) {
				cycleText.append('[');
				for (int j = 0; j < cycles[i].length; j++) {
					cycleText.append(((BundleDescription) cycles[i][j]).getSymbolicName());
					cycleText.append(',');
				}
				cycleText.insert(cycleText.length() - 1, ']');
			}
			cycleText.setCharAt(cycleText.length() - 1, ']');
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_BUNDLESTOPPER_CYCLES_FOUND, cycleText);
			FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.WARNING, 0, message, 0, null, null);
			adaptor.getFrameworkLog().log(entry);
		}
	}

	public void stopBundles() {
		allToStop = adaptor.getState().getResolvedBundles();
		StateHelper stateHelper = adaptor.getPlatformAdmin().getStateHelper();
		Object[][] cycles = stateHelper.sortBundles(allToStop);
		logCycles(cycles);
		stoppedBundles = new Hashtable(allToStop.length);
		basicStopBundles();
	}

	private void basicStopBundles() {
		// stop all active bundles in the reverse order of Require-Bundle
		for (int stoppingIndex = allToStop.length - 1; stoppingIndex >= 0; stoppingIndex--) {
			AbstractBundle toStop = (AbstractBundle) context.getBundle(allToStop[stoppingIndex].getBundleId());
			BaseData bundledata = (BaseData) toStop.getBundleData();
			EclipseStorageHook storageHook = (EclipseStorageHook) bundledata.getStorageHook(EclipseStorageHook.KEY);
			if (toStop.getBundleId() != 0 && storageHook != null && storageHook.isAutoStartable()) {
				try {
					if ((toStop.getState() == Bundle.ACTIVE) && (toStop instanceof BundleHost))
						toStop.stop();
				} catch (Exception e) {
					String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE, allToStop[stoppingIndex].toString());
					FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null);
					adaptor.getFrameworkLog().log(entry);
				} finally {
					stoppedBundles.put(toStop, toStop);
				}
			}
		}
	}

	public boolean isStopped(Bundle bundle) {
		if (stoppedBundles == null)
			return false;
		return stoppedBundles.get(bundle) != null;
	}
}
