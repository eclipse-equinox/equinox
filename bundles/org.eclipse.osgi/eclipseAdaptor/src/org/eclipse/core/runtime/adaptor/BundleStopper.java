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

import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * Implementation for the runtime shutdown hook that provides 
 * support for legacy bundles. All legacy bundles are stopped 
 * in the proper order.
 */
public class BundleStopper {
	private class ReferenceKey {
		private long referrerId;
		private long referredId;

		public ReferenceKey(long referrerId, long referredId) {
			this.referrerId = referrerId;
			this.referredId = referredId;
		}

		public boolean equals(Object obj) {
			return referredId == ((ReferenceKey) obj).referredId && referrerId == ((ReferenceKey) obj).referrerId;
		}

		public int hashCode() {
			return ((int) (referredId & 0xFFFF)) << 16 + (int) (referrerId & 0xFFFF);
		}
	}

	public void stopBundles() {
		Map references = new HashMap();
		Bundle[] allBundles = EclipseAdaptor.getDefault().getContext().getBundles();
		// a map in the form: bundle -> REQUIRE_BUNDLE header value
		Map allToStop = new HashMap(allBundles.length);
		// a map in the form: bundle symbolic name -> bundle
		Map toStopWithNames = new HashMap(allBundles.length);
		selectBundlesToStop(allBundles, allToStop, toStopWithNames);
		Bundle[] orderedBundles = orderBundles(references, allToStop, toStopWithNames);
		stopBundles(orderedBundles);
	}

	private void stopBundles(Bundle[] orderedBundles) {
		// stop all active legacy bundles in the reverse order of Require-Bundle
		for (int i = orderedBundles.length - 1; i >= 0; i--) {
			try {
				if (orderedBundles[i].getState() == Bundle.ACTIVE)
					orderedBundles[i].stop();
			} catch (Exception e) {
				String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE", orderedBundles[i].toString()); //$NON-NLS-1$
				FrameworkLogEntry entry = new FrameworkLogEntry(EclipseAdaptorConstants.PI_ECLIPSE_OSGI, message, 0, e, null);
				EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
			}
		}
	}

	private Bundle[] orderBundles(Map references, Map allToStop, Map toStopWithNames) {
		// find dependencies betweeen them
		for (Iterator i = allToStop.entrySet().iterator(); i.hasNext();) {
			Map.Entry pair = (Map.Entry) i.next();
			Bundle toStop = (Bundle) pair.getKey();
			String requiredBundleNames = (String) pair.getValue();
			// no Require-Bundle entry - does not depend on other bundles			
			if (requiredBundleNames == null)
				continue;
			try {
				//TODO Can't we use the State instead of reparsing the headers?
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, requiredBundleNames);
				for (int j = 0; j < elements.length; j++) {
					String requiredBundleName = elements[j].getValue();
					// ignore dependencies on bundles that we are not stopping				
					Bundle requiredBundle = (Bundle) toStopWithNames.get(requiredBundleName);
					if (requiredBundle != null)
						references.put(new ReferenceKey(toStop.getBundleId(), requiredBundle.getBundleId()), new Object[] {toStop, requiredBundle});
				}
			} catch (BundleException e) {
				// should never happen, since the framework accepted this bundle, but...
				String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_BUNDLESTOPPER_ERROR_STOPPING_BUNDLE", toStop); //$NON-NLS-1$				
				FrameworkLogEntry entry = new FrameworkLogEntry(EclipseAdaptorConstants.PI_ECLIPSE_OSGI, message, 0, e, null);
				EclipseAdaptor.getDefault().getFrameworkLog().log(entry);
			}
		}

		//TODO The ordering should be done with taking all the required bundles into account, and then the filtering should be done. Otherwise this can result in a bad ordering in the shutting down.
		//TODO Maybe should we also consider the import?
		Bundle[] orderedBundles = (Bundle[]) allToStop.keySet().toArray(new Bundle[allToStop.size()]);
		Object[][] cycles = ComputeNodeOrder.computeNodeOrder(orderedBundles, (Object[][]) references.values().toArray(new Object[references.size()][]));
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
		return orderedBundles;
	}

	private void selectBundlesToStop(Bundle[] allBundles, Map allToStop, Map toStopWithNames) {
		// gather all active "auto-stoppable" bundles
		for (int i = 0; i < allBundles.length; i++) {
			if (allBundles[i].getState() != Bundle.ACTIVE)
				continue;
			// we are looking for three headers: LEGACY, ECLIPSE_AUTOSTOP and REQUIRE_BUNDLE	
			//TODO Here we can remove the test on LEGACY
			Dictionary headers = allBundles[i].getHeaders();
			String autoStop = (String) headers.get(EclipseAdaptorConstants.ECLIPSE_AUTOSTOP);
			if (autoStop == null)
				autoStop = (String) headers.get(EclipseAdaptorConstants.LEGACY); //$NON-NLS-1$
			if (!"true".equalsIgnoreCase(autoStop)) //$NON-NLS-1$
				continue;
			// remember we want to stop this bundle
			allToStop.put(allBundles[i], headers.get(Constants.REQUIRE_BUNDLE));
			// bundles with symbolic names may be required by others (we will order them later)
			String symbolicName = allBundles[i].getSymbolicName();
			if (symbolicName != null)
				toStopWithNames.put(symbolicName, allBundles[i]);
		}
	}
}