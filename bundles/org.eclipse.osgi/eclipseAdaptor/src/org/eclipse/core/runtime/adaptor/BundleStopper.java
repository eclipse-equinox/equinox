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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Implementation for the runtime shutdown hook that provides 
 * support for legacy bundles. All legacy bundles are stopped 
 * in the proper order.
 */
public class BundleStopper {
	private void logCycles(Object[][] cycles) {
		// log cycles
		if (cycles.length > 0) {
			StringBuffer cycleText = new StringBuffer("["); //$NON-NLS-1$			
			for (int i = 0; i < cycles.length; i++) {
				cycleText.append('[');
				for (int j = 0; j < cycles[i].length; j++) {
					cycleText.append(((Bundle) cycles[i][j]).getSymbolicName());
					cycleText.append(',');
				}
				cycleText.insert(cycleText.length() - 1, ']');
			}
			cycleText.setCharAt(cycleText.length() - 1, ']');
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE", cycleText); //$NON-NLS-1$
			FrameworkLogEntry entry = new FrameworkLogEntry(EclipseAdaptorConstants.PI_ECLIPSE_OSGI, message, 0, null, null);
			EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
		}
	}

	private BundleDescription[] selectBundlesToStop(Bundle[] allBundles) {
		State state = EclipseAdaptor.getDefault().getState();
		List allToStop = new ArrayList(allBundles.length);
		// gather all active "auto-stoppable" bundles
		for (int i = 0; i < allBundles.length; i++) {
			if (!(allBundles[i] instanceof BundleHost) || allBundles[i].getBundleId() == 0 || allBundles[i].getState() != Bundle.ACTIVE)
				continue;			
			BundleHost host = ((BundleHost) allBundles[i]);
			if (!((EclipseBundleData) host.getBundleData()).isAutoStart())
				continue;
			allToStop.add(state.getBundle(host.getBundleId()));
		}
		return (BundleDescription[]) allToStop.toArray(new BundleDescription[allToStop.size()]);
	}

	public void stopBundles() {
		Bundle[] allBundles = EclipseAdaptor.getDefault().getContext().getBundles();
		BundleDescription[] allToStop = selectBundlesToStop(allBundles);
		StateHelper stateHelper = EclipseAdaptor.getDefault().getPlatformAdmin().getStateHelper();
		Object[][] cycles = stateHelper.sortBundles(allToStop);
		logCycles(cycles);
		stopBundles(allToStop);
	}

	private void stopBundles(BundleDescription[] orderedBundles) {
		BundleContext context = EclipseAdaptor.getDefault().getContext();
		// stop all active bundles in the reverse order of Require-Bundle
		for (int i = orderedBundles.length - 1; i >= 0; i--) {
			try {
				Bundle toStop = context.getBundle(orderedBundles[i].getBundleId());
				if (toStop.getState() == Bundle.ACTIVE)
					toStop.stop();
			} catch (Exception e) {
				String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE", orderedBundles[i].toString()); //$NON-NLS-1$
				FrameworkLogEntry entry = new FrameworkLogEntry(EclipseAdaptorConstants.PI_ECLIPSE_OSGI, message, 0, e, null);
				EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
			}
		}
	}
}