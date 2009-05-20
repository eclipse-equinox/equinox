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

import java.util.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.device.Device;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * DeviceTracker class. This class has the logic for refining a
 * Device service.
 *
 */
public class DeviceTracker extends ServiceTracker {
	/** OSGi Device class name */
	protected final static String clazz = "org.osgi.service.device.Device"; //$NON-NLS-1$

	/** DeviceManager object. */
	protected Activator manager;

	/** reference to Device service we are attempting to refine */
	protected ServiceReference device;

	/** LogService object */
	protected LogService log;

	/** Device services properties */
	protected Dictionary properties;

	/** if false the algorithm must terminate */
	protected volatile boolean running;

	/**
	 * Create a DeviceTracker from a ServiceReference.
	 *
	 * @param manager DeviceManager object
	 * @param device ServiceReference to the Device service.
	 * @param id ID of DeviceTracker object
	 */
	public DeviceTracker(Activator manager, ServiceReference device) {
		super(manager.context, device, null);

		this.manager = manager;
		log = manager.log;

		if (Activator.DEBUG) {
			log.log(device, LogService.LOG_DEBUG, this + " constructor"); //$NON-NLS-1$
		}

		open();
	}

	/**
	 * Close the Device.
	 */

	public void close() {
		if (device != null) {
			if (Activator.DEBUG) {
				log.log(device, LogService.LOG_DEBUG, this + " closing"); //$NON-NLS-1$
			}

			running = false; /* request thread to stop */

			super.close();

			device = null;
		}
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
			log.log(reference, LogService.LOG_DEBUG, this + " adding Device service"); //$NON-NLS-1$
		}

		device = reference;

		running = true;

		properties = new Properties(reference);

		return (reference);
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
		properties = new Properties(reference);
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
	public void removedService(ServiceReference reference, Object service) {
		if (running) {
			log.log(reference, LogService.LOG_WARNING, DeviceMsg.Device_service_unregistered);
			running = false; /* request algorithm to stop */
		} else {
			if (Activator.DEBUG) {
				log.log(reference, LogService.LOG_DEBUG, this + " removing Device service"); //$NON-NLS-1$
			}
		}
		super.removedService(reference, service);
	}

	/**
	 * Attempt to refine this Device service.
	 *
	 */
	public void refine() {
		if (Activator.DEBUG) {
			log.log(device, LogService.LOG_DEBUG, this + " refining " + device); //$NON-NLS-1$
		}

		if (running && isIdle()) {
			/* List of excluded drivers from this algorithm run */
			DriverTracker drivers = manager.drivers;

			manager.locators.loadDrivers(properties, drivers);

			Vector exclude = new Vector(drivers.size());

			while (running) {
				ServiceReference driver = drivers.match(device, exclude);

				if (driver == null) {
					noDriverFound();
					break;
				}

				if (drivers.attach(driver, device, exclude)) {
					break;
				}
			}
		}

		close();
	}

	/**
	 * Determine if the device service tracked by this object is idle.
	 *
	 * OSGi SP R2 Section 8.2.2 defines in idle device service as:
	 * "A Device service is not used by any other bundle according to the Framework;
	 * it is called an idle Device service."
	 *
	 * This method defines it as:
	 *  A Device service is not used by any DRIVER bundle according to the Framework;
	 *  it is called an idle Device service.
	 *
	 * Thus if a non-driver bundle uses a device service, it is still considered
	 * idle by this method.
	 *
	 * @return true if the device service is idle.
	 */
	public boolean isIdle() {
		if (Activator.DEBUG) {
			log.log(device, LogService.LOG_DEBUG, "Check device service idle: " + device); //$NON-NLS-1$
		}

		Filter filter_ = manager.driverFilter;
		Bundle[] users = device.getUsingBundles();

		int userCount = (users == null) ? 0 : users.length;

		for (int i = 0; i < userCount; i++) {
			ServiceReference[] services = users[i].getRegisteredServices();

			int servicesCount = (services == null) ? 0 : services.length;

			for (int j = 0; j < servicesCount; j++) {
				if (filter_.match(services[j])) {
					if (Activator.DEBUG) {
						log.log(LogService.LOG_DEBUG, "Device " + device + " already in use by bundle " + users[i]); //$NON-NLS-1$ //$NON-NLS-2$
					}

					return (false);
				}
			}
		}

		return (true);
	}

	/**
	 * Called by the device manager after it has failed to attach
	 * any driver to the device.
	 * <p>
	 * If the device can be configured in alternate ways, the driver
	 * may respond by unregistering the device service and registering
	 * a different device service instead.</p>
	 */

	public void noDriverFound() {
		BundleContext contxt = manager.context;

		Object service = contxt.getService(device);

		try {
			//It is possible that this is a Free Format Device that does not
			//implement Device
			if (service instanceof Device) {
				log.log(device, LogService.LOG_INFO, DeviceMsg.Device_noDriverFound_called);

				try {
					((Device) service).noDriverFound();
				} catch (Throwable t) {
					log.log(device, LogService.LOG_ERROR, NLS.bind(DeviceMsg.Device_noDriverFound_error, t));
				}
			}
		} finally {
			contxt.ungetService(device);
		}

	}

	public String toString() {
		return "DeviceTracker"; //$NON-NLS-1$
	}

	/**
	 * Readonly Dictionary for device properties.
	 *
	 */
	static class Properties extends Hashtable {
		private static final long serialVersionUID = -8489170394007899809L;
		/**
		 * keys in original case.
		 */
		protected Vector keys;

		/**
		 * Create a properties object for the service.
		 *
		 * @param device The service to get the properties of.
		 */
		protected Properties(ServiceReference device) {
			super();

			String[] props = device.getPropertyKeys();

			if (props != null) {
				int size = props.length;

				keys = new Vector(size);

				for (int i = 0; i < size; i++) {
					String key = props[i];
					Object value = device.getProperty(key);

					if (value != null) {
						keys.addElement(key);

						super.put(key.toLowerCase(), value);
					}
				}
			} else {
				keys = new Vector(0);
			}
		}

		/**
		 * Override keys to support case-preserving of keys.
		 */
		public Enumeration keys() {
			return (keys.elements());
		}

		/**
		 * Override get to support case-insensitivity.
		 *
		 * @param key header name.
		 */
		public Object get(Object key) {
			if (key instanceof String) {
				return (super.get(((String) key).toLowerCase()));
			}

			return (null);
		}

		/**
		 * Override put to disable it. This Dictionary is readonly once built.
		 *
		 * @param key header name.
		 * @param value header value.
		 * @throws UnsupportedOperationException
		 */
		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Override remove to disable it. This Dictionary is readonly once built.
		 *
		 * @param key header name.
		 * @throws UnsupportedOperationException
		 */
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}
	}
}
