/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateHelper;
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
	volatile int stoppingIndex = 0;
	BundleDescription[] allToStop = null;

	private void logCycles(Object[][] cycles) {
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
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_CYCLES_FOUND", cycleText); //$NON-NLS-1$
			FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, null, null);
			EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
		}
	}

	public void stopBundles() {
		allToStop = EclipseAdaptor.getDefault().getState().getResolvedBundles();
		StateHelper stateHelper = EclipseAdaptor.getDefault().getPlatformAdmin().getStateHelper();
		Object[][] cycles = stateHelper.sortBundles(allToStop);
		logCycles(cycles);
		basicStopBundles();
	}

	private void basicStopBundles() {
		BundleContext context = EclipseAdaptor.getDefault().getContext();
		// stop all active bundles in the reverse order of Require-Bundle
		for (stoppingIndex = allToStop.length - 1; stoppingIndex >= 0; stoppingIndex--) {
			try {
				AbstractBundle toStop = (AbstractBundle) context.getBundle(allToStop[stoppingIndex].getBundleId());
				if (toStop.getState() != Bundle.ACTIVE || !(toStop instanceof BundleHost) || toStop.getBundleId() == 0)
					continue;
				if (!((EclipseBundleData) toStop.getBundleData()).isAutoStartable())
					continue;
				toStop.stop();
			} catch (Exception e) {
				String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE", allToStop[stoppingIndex].toString()); //$NON-NLS-1$
				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, e, null);
				EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
			}
		}
	}

	public boolean isStopped(String symbolicId) {
		if (!EclipseAdaptor.getDefault().isStopping())
			return false;

		//We return false for the bundle currently being stopped
		for (int i = allToStop.length - 1; i > stoppingIndex; i--) {
			if (symbolicId.equals(allToStop[i].getSymbolicName()))
				return true;
		}
		return false;
	}
}