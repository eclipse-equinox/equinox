/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Device;
import org.osgi.service.device.DriverSelector;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * DriverSelectorTracker class. This class tracks all DriverSelector services.
 *
 */
public class DriverSelectorTracker extends ServiceTracker {
	/** Driver service name */
	protected final static String clazz = "org.osgi.service.device.DriverSelector"; //$NON-NLS-1$

	/** LogService object */
	protected LogService log;

	/** DeviceManager object. */
	protected Activator manager;

	/**
	 * Create the DriverTracker.
	 *
	 * @param manager DeviceManager object.
	 * @param device DeviceTracker we are working for.
	 */
	public DriverSelectorTracker(Activator manager) {
		super(manager.context, clazz, null);

		this.manager = manager;
		log = manager.log;

		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, "DriverSelectorTracker constructor"); //$NON-NLS-1$
		}

		open();
	}

	/**
	 * Select the matching driver.
	 *
	 * @param device Device service being matched.
	 * @param matches Array of the successful matches from Driver services.
	 * @return ServiceReference to best matched Driver or null of their is no match.
	 */
	public ServiceReference select(ServiceReference device, Match[] matches) {
		if (Activator.DEBUG) {
			log.log(device, LogService.LOG_DEBUG, "DriverSelector select called"); //$NON-NLS-1$
		}

		//This should give us the highest ranking DriverSelector (if available)
		ServiceReference selector = getServiceReference();

		if (selector != null) {
			DriverSelector service = (DriverSelector) getService(selector);

			try {
				int index = service.select(device, matches);

				if (index == DriverSelector.SELECT_NONE) {
					return null;
				}

				return matches[index].getDriver();
			} catch (Throwable t) {
				log.log(selector, LogService.LOG_ERROR, DeviceMsg.DriverSelector_error_during_match, t);
			}
		}

		return defaultSelection(matches);
	}

	/**
	 * Default match selection algorithm from OSGi SPR2 spec.
	 *
	 * @param matchArray An array of the successful matches.
	 * @return ServiceReference to the selected Driver service
	 */
	public ServiceReference defaultSelection(Match[] matches) {
		int size = matches.length;

		int max = Device.MATCH_NONE;
		ServiceReference reference = null;

		for (int i = 0; i < size; i++) {
			Match driver = matches[i];

			int match = driver.getMatchValue();

			if (match >= max) {
				if (match == max) /* we must break the tie */
				{
					reference = breakTie(reference, driver.getDriver());
				} else {
					max = match;
					reference = driver.getDriver();
				}
			}
		}

		return reference;
	}

	/**
	 * Select the service with the highest service.ranking. Break ties
	 * buy selecting the lowest service.id.
	 *
	 */
	public ServiceReference breakTie(ServiceReference ref1, ServiceReference ref2) {
		//first we check service rankings
		Object property = ref1.getProperty(org.osgi.framework.Constants.SERVICE_RANKING);

		int ref1Ranking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;

		property = ref2.getProperty(org.osgi.framework.Constants.SERVICE_RANKING);

		int ref2Ranking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;

		if (ref1Ranking > ref2Ranking) {
			return ref1;
		} else if (ref2Ranking > ref1Ranking) {
			return ref2;
		} else // The rankings must match here
		{
			//we now check service ids
			long ref1ID = ((Long) (ref1.getProperty(org.osgi.framework.Constants.SERVICE_ID))).longValue();

			long ref2ID = ((Long) (ref2.getProperty(org.osgi.framework.Constants.SERVICE_ID))).longValue();

			if (ref1ID < ref2ID) {
				return ref1;
			}

			return ref2;
		}
	}
}
