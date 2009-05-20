/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Vector;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.device.DriverLocator;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * DriverLocatorTracker class. This class tracks all DriverLocator services.
 *
 */
public class DriverLocatorTracker extends ServiceTracker {
	protected final static String clazz = "org.osgi.service.device.DriverLocator"; //$NON-NLS-1$

	/** DeviceManager object. */
	protected Activator manager;

	/** LogService object */
	protected LogService log;

	/** List of bundles to be uninstalled. */
	protected Vector bundles;

	/**
	 * Create the DriverLocatorTracker.
	 *
	 * @param context Device manager bundle context.
	 * @param log LogService object
	 */
	public DriverLocatorTracker(Activator manager) {
		super(manager.context, clazz, null);

		this.manager = manager;
		log = manager.log;
		bundles = new Vector(10, 10);

		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, "DriverLocatorTracker constructor"); //$NON-NLS-1$
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
			log.log(reference, LogService.LOG_DEBUG, "DriverLocatorTracker adding service"); //$NON-NLS-1$
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
		//do nothing
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
			log.log(reference, LogService.LOG_DEBUG, "DriverLocatorTracker removing service"); //$NON-NLS-1$
		}

		context.ungetService(reference);
	}

	/**
	 * Call the DriverLocator services in an attempt to locate and
	 * install driver bundles to refine the device service.
	 *
	 * @param locators Array of DriverLocator objects
	 * @param drivers Dictionary of drivers with key=DRIVER_ID, value=Driver object
	 */
	public void loadDrivers(Dictionary properties, DriverTracker drivers) {
		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocatorTracker loadDrivers called"); //$NON-NLS-1$
		}

		ServiceReference[] references = getServiceReferences();

		if (references != null) {
			int size = references.length;

			for (int i = 0; i < size; i++) {
				ServiceReference locator = references[i];
				DriverLocator service = (DriverLocator) getService(locator);

				if (service != null) {
					if (Activator.DEBUG) {
						log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator findDrivers called"); //$NON-NLS-1$
					}

					try {
						String[] driver_ids = service.findDrivers(properties);

						if (Activator.DEBUG) {
							int count = (driver_ids == null) ? 0 : driver_ids.length;

							StringBuffer sb = new StringBuffer();

							sb.append('<');

							for (int k = 0; k < count; k++) {
								if (k > 0) {
									sb.append(',');
								}
								sb.append(driver_ids[k]);
							}

							sb.append('>');

							log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator findDrivers returned: " + sb); //$NON-NLS-1$
						}

						if (driver_ids != null) {
							int count = driver_ids.length;

							for (int j = 0; j < count; j++) {
								String driver_id = driver_ids[j];

								if (drivers.getDriver(driver_id) == null) {
									if (Activator.DEBUG) {
										log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator loadDriver called for driver: " + driver_id); //$NON-NLS-1$
									}

									try {
										InputStream in = service.loadDriver(driver_id);

										if (Activator.DEBUG) {
											log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator loadDriver returned: " + in); //$NON-NLS-1$
										}

										installDriverBundle(driver_id, in);
									} catch (Throwable t) {
										log.log(locator, LogService.LOG_ERROR, NLS.bind(DeviceMsg.DriverLocator_unable_to_load_driver, driver_id), t);
									}
								}
							}
						}
					} catch (Throwable t) {
						log.log(locator, LogService.LOG_ERROR, DeviceMsg.DriverLocator_error_calling_findDrivers, t);
					}
				}
			}
		}
	}

	/**
	 * Get an <code>InputStream</code> from which the driver bundle providing a driver with the giving ID can be installed.
	 *
	 * @param id the ID of the driver that needs to be installed.
	 * bundle can be installed
	 */

	public void loadDriver(String driver_id, DriverTracker drivers) {
		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocatorTracker loadDriver called for driver: " + driver_id); //$NON-NLS-1$
		}

		if (drivers.getDriver(driver_id) == null) {
			ServiceReference[] references = getServiceReferences();

			if (references != null) {
				int size = references.length;

				for (int i = 0; i < size; i++) {
					ServiceReference locator = references[i];
					DriverLocator service = (DriverLocator) getService(locator);

					if (service != null) {
						if (Activator.DEBUG) {
							log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator loadDriver called for driver: " + driver_id); //$NON-NLS-1$
						}

						try {
							InputStream in = service.loadDriver(driver_id);

							if (Activator.DEBUG) {
								log.log(locator, LogService.LOG_DEBUG, Thread.currentThread().getName() + ": DriverLocator loadDriver returned: " + in); //$NON-NLS-1$
							}

							if (in != null) {
								installDriverBundle(driver_id, in);

								break;
							}
						} catch (Throwable t) {
							log.log(locator, LogService.LOG_ERROR, NLS.bind(DeviceMsg.DriverLocator_unable_to_load_driver, driver_id), t);
						}
					}
				}
			}
		}
	}

	/**
	 * Install a Driver bundle.
	 *
	 * @param driver_id DRIVER_ID for new driver bundle.
	 * @param in InputStream to a new driver bundle.
	 */
	public void installDriverBundle(String driver_id, InputStream in) {
		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": installDriverBundle from InputStream: " + driver_id); //$NON-NLS-1$
		}

		if (in != null) {
			Bundle bundle = null;

			try {
				bundle = context.installBundle(driver_id, in);
				/* installBundle will close the InputStream */

				if (Activator.DEBUG) {
					log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": Driver bundle installed: " + driver_id); //$NON-NLS-1$
				}

				synchronized (bundles) {
					if (!bundles.contains(bundle)) {
						bundles.addElement(bundle);
					}
				}

				bundle.start();

				if (Activator.DEBUG) {
					log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": Driver bundle started: " + driver_id); //$NON-NLS-1$
				}
			} catch (BundleException e) {
				log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.Unable_to_install_or_start_driver_bundle, driver_id), e);

				if (bundle != null) {
					bundles.removeElement(bundle);

					try {
						bundle.uninstall();

						if (Activator.DEBUG) {
							log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": Driver bundle uninstalled: " + driver_id); //$NON-NLS-1$
						}
					} catch (BundleException ee) {
						log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.Unable_to_uninstall_driver_bundle_number, driver_id), ee);
					}

					bundle = null;
				}
			}
		}
	}

	/**
	 * Remove bundle from uninstall list.
	 *
	 * @param bundle bundle to remove from list.
	 */
	public void usingDriverBundle(Bundle bundle) {
		bundles.removeElement(bundle);
	}

	/**
	 * Uninstall the recently installed but unused driver bundles.
	 *
	 */
	public void uninstallDriverBundles() {
		int size;
		Bundle[] uninstall = null;

		synchronized (bundles) {
			size = bundles.size();

			if (size > 0) {
				uninstall = new Bundle[size];
				bundles.copyInto(uninstall);
			}
		}

		for (int i = 0; i < size; i++) {
			Bundle bundle = uninstall[i];

			if ((bundle.getState() & Bundle.UNINSTALLED) == 0) { /* if bundle not already uninstalled */
				try {
					bundle.uninstall();

					if (Activator.DEBUG) {
						log.log(LogService.LOG_DEBUG, Thread.currentThread().getName() + ": Driver bundle uninstalled"); //$NON-NLS-1$
					}
				} catch (BundleException ee) {
					log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.Unable_to_uninstall_driver_bundle, ee));
				}
			}
		}

		bundles.removeAllElements();
	}

	public boolean isUninstallCandidate(Bundle bundle) {
		return bundles.contains(bundle);
	}
}
