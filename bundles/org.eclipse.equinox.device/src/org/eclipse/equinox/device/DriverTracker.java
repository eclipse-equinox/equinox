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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Vector;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * DriverTracker class. This class tracks all Driver services.
 *
 */
public class DriverTracker extends ServiceTracker {
	/** Driver service name */
	protected final static String clazz = "org.osgi.service.device.Driver"; //$NON-NLS-1$

	/** LogService object */
	protected LogService log;

	/** Dictionary mapping DRIVER_ID strings <==> Driver ServiceReferences */
	protected Hashtable drivers;

	/** DeviceManager object. */
	protected Activator manager;

	/** Dictionary mapping Driver ID String =>
	 *  Hashtable (Device ServiceReference => cached Match objects) */
	protected Hashtable matches;

	/** Dictionary mapping Driver ID String =>
	 *  Hashtable (Device ServiceReference => cached referral String) */
	protected Hashtable referrals;

	/**
	 * Create the DriverTracker.
	 *
	 * @param manager DeviceManager object.
	 * @param device DeviceTracker we are working for.
	 */
	public DriverTracker(Activator manager) {
		super(manager.context, clazz, null);

		this.manager = manager;
		log = manager.log;

		drivers = new Hashtable(37);
		matches = new Hashtable(37);
		referrals = new Hashtable(37);

		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, this + " constructor"); //$NON-NLS-1$
		}

		open();
	}

	/**
	 * A service is being added to the ServiceTracker.
	 *
	 * <p>This method is called before a service which matched
	 * the search parameters of the ServiceTracker is
	 * added to the ServiceTracker. This method should return the
	 * service object to be tracked for this ServiceReference.
	 * The returned service object is stored in the ServiceTracker
	 * and is available from the getService and getServices
	 * methods.
	 *
	 * @param reference Reference to service being added to the ServiceTracker.
	 * @return The service object to be tracked for the
	 * ServiceReference or <tt>null</tt> if the ServiceReference should not
	 * be tracked.
	 */
	public Object addingService(ServiceReference reference) {
		if (Activator.DEBUG) {
			log.log(reference, LogService.LOG_DEBUG, this + " adding service"); //$NON-NLS-1$
		}

		String driver_id = getDriverID(reference);

		if (drivers.get(driver_id) != null) {
			log.log(reference, LogService.LOG_WARNING, NLS.bind(DeviceMsg.Multiple_Driver_services_with_the_same_DRIVER_ID, driver_id));

			return (null); /* don't track this driver */
		}

		drivers.put(driver_id, reference);
		drivers.put(reference, driver_id);

		manager.driverServiceRegistered = true;

		/* OSGi SPR2 Device Access 1.1
		 * Section 8.4.3 - When a new Driver service is registered,
		 * the Device Attachment Algorithm must be applied to all
		 * idle Device services.
		 *
		 * We do not refine idle Devices when the manager has not fully
		 * started or the Driver service is from a bundle just installed
		 * by the devicemanager.
		 */
		Bundle bundle = reference.getBundle();

		if (manager.running && !manager.locators.isUninstallCandidate(bundle)) {
			manager.refineIdleDevices();
		}

		return (context.getService(reference));
	}

	/**
	 * A service tracked by the ServiceTracker has been modified.
	 *
	 * <p>This method is called when a service being tracked
	 * by the ServiceTracker has had it properties modified.
	 *
	 * @param reference Reference to service that has been modified.
	 * @param service The service object for the modified service.
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		if (Activator.DEBUG) {
			log.log(reference, LogService.LOG_DEBUG, this + " modified service"); //$NON-NLS-1$
		}

		String driver_id = getDriverID(reference);

		String old_id = (String) drivers.get(reference);

		if (!driver_id.equals(old_id)) {
			drivers.put(driver_id, reference);
			drivers.put(reference, driver_id);
			drivers.remove(old_id);
		}
	}

	/**
	 * A service tracked by the ServiceTracker is being removed.
	 *
	 * <p>This method is called after a service is no longer being tracked
	 * by the ServiceTracker.
	 *
	 * @param reference Reference to service that has been removed.
	 * @param service The service object for the removed service.
	 */
	public void removedService(ServiceReference reference, Object object) {
		if (Activator.DEBUG) {
			log.log(reference, LogService.LOG_DEBUG, this + " removing service"); //$NON-NLS-1$
		}

		String driver_id = getDriverID(reference);
		drivers.remove(driver_id);
		drivers.remove(reference);

		matches.remove(driver_id);
		referrals.remove(driver_id);

		context.ungetService(reference);

		/* OSGi SPR2 Device Access 1.1
		 * Section 8.4.4 - When a Driver service is unregistered,
		 * the Device Attachment Algorithm must be applied to all
		 * idle Device services.
		 *
		 * We do not refine idle Devices when the manager has not fully
		 * started or the Driver service is from a bundle just installed
		 * by the devicemanager.
		 */

		Bundle bundle = reference.getBundle();

		if (manager.running && !manager.locators.isUninstallCandidate(bundle)) {
			DriverUpdate update = new DriverUpdate(bundle, manager);

			Thread thread = (new SecureAction()).createThread(update, DeviceMsg.DeviceManager_Update_Wait);

			thread.start();
		}
	}

	/**
	 * Return the DRIVER_ID string for a ServiceReference.
	 *
	 * Per Section 8.4.3 of the OSGi SP R2 spec,
	 * "A Driver service registration must have a DRIVER_ID property"
	 *
	 * This method is somewhat more lenient. If no DRIVER_ID property
	 * is set, it will use the Bundle's location instead.
	 *
	 * @param reference Reference to driver service.
	 * @param log LogService object.
	 * @return DRIVER_ID string.
	 */
	public String getDriverID(final ServiceReference reference) {
		String driver_id = (String) reference.getProperty(org.osgi.service.device.Constants.DRIVER_ID);

		if (driver_id == null) {
			log.log(reference, LogService.LOG_WARNING, DeviceMsg.Driver_service_has_no_DRIVER_ID);
			driver_id = (String) AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					return reference.getBundle().getLocation();
				}
			});
		}

		return (driver_id);
	}

	/**
	 * Get the ServiceReference for a given DRIVER_ID.
	 *
	 * @param driver_id
	 * @return ServiceReference to a Driver service.
	 */
	public ServiceReference getDriver(String driver_id) {
		return ((ServiceReference) drivers.get(driver_id));
	}

	/**
	 * Search the driver list to find the best match for the device.
	 *
	 * @return ServiceReference to best matched Driver or null of their is no match.
	 */
	public ServiceReference match(ServiceReference device, Vector exclude) {
		if (Activator.DEBUG) {
			log.log(device, LogService.LOG_DEBUG, this + ": Driver match called"); //$NON-NLS-1$
		}

		ServiceReference[] references = getServiceReferences();

		if (references != null) {
			int size = references.length;

			Vector successfulMatches = new Vector(size);

			for (int i = 0; i < size; i++) {
				ServiceReference driver = references[i];

				if (exclude.contains(driver)) {
					if (Activator.DEBUG) {
						log.log(driver, LogService.LOG_DEBUG, this + ": Driver match excluded: " + drivers.get(driver)); //$NON-NLS-1$
					}
				} else {
					if (Activator.DEBUG) {
						log.log(driver, LogService.LOG_DEBUG, this + ": Driver match called: " + drivers.get(driver)); //$NON-NLS-1$
					}

					Match match = getMatch(driver, device);

					if (match == null) {
						Driver service = (Driver) getService(driver);

						if (service == null) {
							continue;
						}

						int matchValue = Device.MATCH_NONE;

						try {
							matchValue = service.match(device);
						} catch (Throwable t) {
							log.log(driver, LogService.LOG_ERROR, DeviceMsg.Driver_error_during_match, t);

							continue;
						}

						if (Activator.DEBUG) {
							log.log(driver, LogService.LOG_DEBUG, this + ": Driver match value: " + matchValue); //$NON-NLS-1$
						}

						match = new Match(driver, matchValue);

						storeMatch(driver, device, match);
					}

					if (match.getMatchValue() > Device.MATCH_NONE) {
						successfulMatches.addElement(match);
					}
				}
			}

			size = successfulMatches.size();

			if (size > 0) {
				Match[] matchArray = new Match[size];
				successfulMatches.copyInto(matchArray);

				return manager.selectors.select(device, matchArray);
			}
		}

		return null;
	}

	public Match getMatch(ServiceReference driver, ServiceReference device) {
		String driverid = getDriverID(driver);

		Hashtable driverMatches = (Hashtable) matches.get(driverid);

		if (driverMatches == null) {
			return null;
		}

		return (Match) driverMatches.get(device);
	}

	public void storeMatch(ServiceReference driver, ServiceReference device, Match match) {
		String driverid = getDriverID(driver);

		Hashtable driverMatches = (Hashtable) matches.get(driverid);

		if (driverMatches == null) {
			driverMatches = new Hashtable(37);

			matches.put(driverid, driverMatches);
		}

		driverMatches.put(device, match);
	}

	/**
	 * Attempt to attach the driver to the device. If the driver
	 * refers, add the referred driver to the driver list.
	 *
	 * @param driver Driver to attach
	 * @param device Device to be attached
	 * @return true is the Driver successfully attached.
	 */
	public boolean attach(ServiceReference driver, ServiceReference device, Vector exclude) {
		if (Activator.DEBUG) {
			log.log(driver, LogService.LOG_DEBUG, this + ": Driver attach called: " + drivers.get(driver)); //$NON-NLS-1$
		}

		Driver service = (Driver) getService(driver);

		if (service != null) {
			String referral = getReferral(driver, device);

			if (referral == null) {
				try {
					referral = service.attach(device);
				} catch (Throwable t) {
					log.log(driver, LogService.LOG_ERROR, DeviceMsg.Driver_error_during_attach, t);

					exclude.addElement(driver);

					return (false);
				}

				storeReferral(driver, device, (referral == null) ? "" : referral); //$NON-NLS-1$
			} else {
				if (referral.length() == 0) {
					referral = null;
				}
			}

			if (referral == null) {
				log.log(device, LogService.LOG_INFO, NLS.bind(DeviceMsg.Device_attached_by_DRIVER_ID, drivers.get(driver)));

				manager.locators.usingDriverBundle(driver.getBundle());

				return (true);
			}

			log.log(device, LogService.LOG_INFO, NLS.bind(DeviceMsg.Device_referred_to, referral));
			manager.locators.loadDriver(referral, this);
		}

		exclude.addElement(driver);

		return (false);
	}

	public String getReferral(ServiceReference driver, ServiceReference device) {
		String driverid = getDriverID(driver);

		Hashtable driverReferrals = (Hashtable) referrals.get(driverid);

		if (driverReferrals == null) {
			return null;
		}

		return (String) driverReferrals.get(device);
	}

	public void storeReferral(ServiceReference driver, ServiceReference device, String referral) {
		String driverid = getDriverID(driver);

		Hashtable driverReferrals = (Hashtable) referrals.get(driverid);

		if (driverReferrals == null) {
			driverReferrals = new Hashtable(37);

			referrals.put(driverid, driverReferrals);
		}

		driverReferrals.put(device, referral);
	}

	public String toString() {
		return "DriverTracker"; //$NON-NLS-1$
	}

	public class DriverUpdate implements Runnable, ServiceListener, BundleListener {
		private Activator manager_;
		private Bundle bundle;
		private BundleContext contxt;

		/** if false the thread must terminate */
		private volatile boolean running;

		private long updatewait;

		DriverUpdate(Bundle bundle, Activator manager) {
			this.manager_ = manager;
			this.bundle = bundle;

			contxt = manager_.context;
			updatewait = manager_.updatewait;
			running = true;

			contxt.addBundleListener(this);
			try {
				contxt.addServiceListener(this, manager_.driverFilter.toString());
			} catch (InvalidSyntaxException e) {
				/* this should not happen */
			}
		}

		public void run() {
			// 1. Wait for some time
			// 2. if bundle registers Driver; terminate
			// 3. if bundle uninstalls; cancel wait
			// 4. manager.refineIdleDevices()

			try {
				if (updatewait > 0) {
					synchronized (this) {
						wait(updatewait);
					}
				}
			} catch (InterruptedException e) {
				//do nothing
			}

			contxt.removeServiceListener(this);
			contxt.removeBundleListener(this);

			if (running) {
				manager.refineIdleDevices();
			}
		}

		public void serviceChanged(ServiceEvent event) {
			if ((event.getType() == ServiceEvent.REGISTERED) && bundle.equals(event.getServiceReference().getBundle())) {
				contxt.removeServiceListener(this);

				running = false; /* cancel */

				/* should probably interrupt waiting thread here */
			}
		}

		public void bundleChanged(BundleEvent event) {
			if ((event.getType() == Bundle.UNINSTALLED) && bundle.equals(event.getBundle())) {
				contxt.removeBundleListener(this);

				updatewait = 0; /* avoid wait */

				/* should probably interrupt waiting thread here */
			}
		}
	}
}
