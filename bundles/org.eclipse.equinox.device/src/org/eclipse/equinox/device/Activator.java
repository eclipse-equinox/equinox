/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * DeviceManager bundle. This bundle implements the OSGi Device Access 1.1
 * specification.
 *
 * This implementation does not include the optimizations in section
 * 8.7.4 of the OSGi SP R2 spec.
 *
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer, FrameworkListener, Runnable {
	protected final static boolean DEBUG = false;

	/** DeviceManager BundleContext */
	protected BundleContext context;

	/** LogTracker object */
	protected LogTracker log;

	/** if false the thread must terminate */
	protected volatile boolean running;

	/** DeviceManager thread */
	protected Thread thread;

	/** DriverTracker for Driver services. */
	protected DriverTracker drivers;

	/** Tracker for DriverLocator services */
	protected DriverLocatorTracker locators;

	/** Tracker for DriverSelector services */
	protected DriverSelectorTracker selectors;

	/** ServiceTracker object for device services */
	protected ServiceTracker devices;

	/** filter for Device services */
	protected Filter deviceFilter;

	/** filter for Driver services */
	protected Filter driverFilter;

	/**
	 * Linked List item
	 */
	static class DeviceService {
		/** object for this item */
		final DeviceTracker device;
		/** next item in event queue */
		DeviceService next;

		/**
		 * Constructor for work queue item
		 *
		 * @param o Object for this event
		 */
		DeviceService(DeviceTracker device) {
			this.device = device;
			next = null;
		}
	}

	/** item at the head of the event queue */
	private DeviceService head;
	/** item at the tail of the event queue */
	private DeviceService tail;

	/** number of milliseconds to wait before refining idle Device services */
	protected long updatewait;

	/** set to true by DriverTracker when a Driver Service is registered */
	protected volatile boolean driverServiceRegistered;

	/**
	 * Create a DeviceManager object.
	 *
	 */

	public Activator() {
		super();
	}

	/**
	 * Start the Device Manager.
	 *
	 * @param context The device manager's bundle context
	 */

	public void start(BundleContext contxt) throws Exception {
		this.context = contxt;
		running = false;

		log = new LogTracker(context, System.err);

		try {
			deviceFilter = context.createFilter("(|(" + org.osgi.framework.Constants.OBJECTCLASS + "=" + DeviceTracker.clazz + ////-1$ ////-2$ //$NON-NLS-1$ //$NON-NLS-2$
					")(" + org.osgi.service.device.Constants.DEVICE_CATEGORY + "=*))"); //$NON-NLS-1$ //$NON-NLS-2$

			driverFilter = context.createFilter("(" + org.osgi.framework.Constants.OBJECTCLASS + "=" + DriverTracker.clazz + ")"); ////-1$ ////-2$ ////-3$ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (InvalidSyntaxException e) {
			log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.Unable_to_create_Filter_for_DeviceManager, e)); ////-1$
			throw e;
		}

		updatewait = 5 * 1000L;

		String prop = context.getProperty("org.eclipse.equinox.device.updatewait"); //$NON-NLS-1$

		if (prop != null) {
			try {
				updatewait = Long.parseLong(prop) * 1000L;
			} catch (NumberFormatException e) {
				//do nothing
			}
		}

		Bundle systemBundle = context.getBundle(0);

		if ((systemBundle != null) && ((systemBundle.getState() & Bundle.STARTING) != 0)) { /* if the system bundle is starting */
			context.addFrameworkListener(this);
		} else {
			startDeviceManager();
		}

		log.log(LogService.LOG_INFO, DeviceMsg.DeviceManager_started);
	}

	/**
	 * Receive notification of a general framework event.
	 *
	 * @param event The FrameworkEvent.
	 */
	public void frameworkEvent(FrameworkEvent event) {
		switch (event.getType()) {
			case FrameworkEvent.STARTED : {
				context.removeFrameworkListener(this);

				try {
					startDeviceManager();
				} catch (Throwable t) {
					log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.DeviceManager_has_thrown_an_error, t)); ////-1$
				}

				break;
			}
		}
	}

	/**
	 * Start the DeviceManager thread.
	 *
	 */
	public void startDeviceManager() {
		if (!running) {
			head = null;
			tail = null;

			locators = new DriverLocatorTracker(this);

			selectors = new DriverSelectorTracker(this);

			drivers = new DriverTracker(this);

			devices = new ServiceTracker(context, deviceFilter, this);
			devices.open();

			running = true;
			driverServiceRegistered = false;

			thread = (new SecureAction()).createThread(this, "DeviceManager"); //$NON-NLS-1$
			thread.start(); /* Start DeviceManager thread */
		}
	}

	/**
	 * Stop the Device Manager bundle.
	 *
	 * @param context The device manager's bundle context
	 */

	public void stop(BundleContext contxt) throws Exception {
		context.removeFrameworkListener(this);

		if (running) {
			Thread t = thread;

			running = false; /* request thread to stop */

			if (t != null) {
				t.interrupt();

				synchronized (t) {
					while (t.isAlive()) /* wait for thread to complete */
					{
						try {
							t.wait(0);
						} catch (InterruptedException e) {
							//	do nothing
						}
					}
				}
			}
		}

		if (drivers != null) {
			drivers.close();
			drivers = null;
		}

		if (devices != null) {
			devices.close();
			devices = null;
		}

		if (locators != null) {
			locators.close();
			locators = null;
		}

		if (selectors != null) {
			selectors.close();
			selectors = null;
		}

		if (log != null) {
			log.close();
			log = null;
		}

		this.context = null;
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
			this.log.log(reference, LogService.LOG_DEBUG, "DeviceManager device service registered"); //$NON-NLS-1$
		}

		enqueue(new DeviceTracker(this, reference));

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
		// do nothing
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
			log.log(reference, LogService.LOG_DEBUG, "DeviceManager device service unregistered"); //$NON-NLS-1$
		}

		/* We do not implement optional driver reclamation.
		 * Thus we take no specific action upon Device service unregistration .
		 */
	}

	public void refineIdleDevices() {
		if (Activator.DEBUG) {
			log.log(LogService.LOG_DEBUG, "DeviceManager refining idle device services"); //$NON-NLS-1$
		}

		ServiceReference[] references = devices.getServiceReferences();

		if (references != null) {
			int size = references.length;

			for (int i = 0; i < size; i++) {
				ServiceReference device = references[i];

				enqueue(new DeviceTracker(this, device));
			}
		}
	}

	/**
	 * Main thread for DeviceManager.
	 *
	 * Attempt to refine all Device services that are not in use
	 * by a driver bundle.
	 */
	public void run() {
		while (running) {
			DeviceTracker device;

			try {
				device = dequeue();
			} catch (InterruptedException e) {
				continue;
			}

			try {
				device.refine();
			} catch (Throwable t) {
				log.log(LogService.LOG_ERROR, NLS.bind(DeviceMsg.DeviceManager_has_thrown_an_error, t)); ////-1$
			}
		}
	}

	/**
	 * Queue the object to be processed on the work thread.
	 * The thread is notified.
	 *
	 * @param device Work item.
	 */
	public synchronized void enqueue(DeviceTracker device) {
		if (device != null) {
			if (Activator.DEBUG) {
				log.log(LogService.LOG_DEBUG, "DeviceManager queuing DeviceTracker"); //$NON-NLS-1$
			}

			DeviceService item = new DeviceService(device);

			if (head == null) /* if the queue was empty */
			{
				head = item;
				tail = item;
			} else /* else add to end of queue */
			{
				tail.next = item;
				tail = item;
			}
		}

		notify();
	}

	/**
	 * Dequeue an object from the work thread.
	 * If the queue is empty, this method blocks.
	 *
	 * @return Dequeue object from the work thread.
	 * @throws InterruptedException If the queue has been stopped.
	 */
	private synchronized DeviceTracker dequeue() throws InterruptedException {
		while (running && (head == null)) {
			// TODO need to determine if this code is needed (bug 261197)
			/* This should be included per Section 8.7.7 of the OSGi SP R2
			 * spec, but it causes the OSGi SP R2 Test Suite to fail.
			 * We should turn this on for R3.

			 if (driverServiceRegistered)
			 */
			//			if (false) {
			//				driverServiceRegistered = false;
			//
			//				refineIdleDevices();
			//			} else {
			locators.uninstallDriverBundles();

			try {
				if (Activator.DEBUG) {
					log.log(LogService.LOG_DEBUG, "DeviceManager waiting on queue"); //$NON-NLS-1$
				}

				wait();
			} catch (InterruptedException e) {
				// do nothing
			}
			//			}
		}

		if (!running) /* if we are stopping */
		{
			throw new InterruptedException(); /* throw an exception */
		}

		DeviceService item = head;
		head = item.next;
		if (head == null) {
			tail = null;
		}

		return (item.device);
	}
}
