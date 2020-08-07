/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.useradmin;

import org.eclipse.osgi.framework.eventmgr.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.osgi.util.tracker.ServiceTracker;

/*
 *  UserAdminEventProducer is responsible for sending out UserAdminEvents
 *  to all UserAdminListeners.
 */

public class UserAdminEventProducer extends ServiceTracker implements EventDispatcher {

	protected ServiceReference userAdmin;
	static protected final String userAdminListenerClass = "org.osgi.service.useradmin.UserAdminListener"; //$NON-NLS-1$
	protected LogTracker log;
	/** List of UserAdminListeners */
	protected CopyOnWriteIdentityMap listeners;
	/** EventManager for event delivery. */
	protected EventManager eventManager;

	protected UserAdminEventProducer(ServiceReference userAdmin, BundleContext context, LogTracker log) {
		super(context, userAdminListenerClass, null);
		this.userAdmin = userAdmin;
		this.log = log;
		ThreadGroup eventGroup = new ThreadGroup("Equinox User Admin"); //$NON-NLS-1$
		eventGroup.setDaemon(true);
		eventManager = new EventManager("UserAdmin Event Dispatcher", eventGroup); //$NON-NLS-1$
		listeners = new CopyOnWriteIdentityMap<>();

		open();
	}

	@Override
	public void close() {
		super.close();
		listeners.clear();
		eventManager.close();
		userAdmin = null;
	}

	protected void generateEvent(int type, Role role) {
		if (userAdmin != null) {
			UserAdminEvent event = new UserAdminEvent(userAdmin, type, role);

			/* queue to hold set of listeners */
			ListenerQueue queue = new ListenerQueue(eventManager);

			/* add set of UserAdminListeners to queue */
			queue.queueListeners(listeners.entrySet(), this);

			/* dispatch event to set of listeners */
			queue.dispatchEventAsynchronous(0, event);
		}
	}

	/**
	 * A service is being added to the <tt>ServiceTracker</tt> object.
	 *
	 * <p>This method is called before a service which matched
	 * the search parameters of the <tt>ServiceTracker</tt> object is
	 * added to it. This method should return the
	 * service object to be tracked for this <tt>ServiceReference</tt> object.
	 * The returned service object is stored in the <tt>ServiceTracker</tt> object
	 * and is available from the <tt>getService</tt> and <tt>getServices</tt>
	 * methods.
	 *
	 * @param reference Reference to service being added to the <tt>ServiceTracker</tt> object.
	 * @return The service object to be tracked for the
	 * <tt>ServiceReference</tt> object or <tt>null</tt> if the <tt>ServiceReference</tt> object should not
	 * be tracked.
	 */
	@Override
	public Object addingService(ServiceReference reference) {
		Object service = super.addingService(reference);

		listeners.put(service, service);

		return service;
	}

	/**
	 * A service tracked by the <tt>ServiceTracker</tt> object has been removed.
	 *
	 * <p>This method is called after a service is no longer being tracked
	 * by the <tt>ServiceTracker</tt> object.
	 *
	 * @param reference Reference to service that has been removed.
	 * @param service The service object for the removed service.
	 */
	@Override
	public void removedService(ServiceReference reference, Object service) {
		listeners.remove(service);

		super.removedService(reference, service);
	}

	/**
	 * This method is the call back that is called once for each listener.
	 * This method must cast the EventListener object to the appropriate listener
	 * class for the event type and call the appropriate listener method.
	 *
	 * @param listener This listener must be cast to the appropriate listener
	 * class for the events created by this source and the appropriate listener method
	 * must then be called.
	 * @param listenerObject This is the optional object that was passed to
	 * ListenerList.addListener when the listener was added to the ListenerList.
	 * @param eventAction This value was passed to the EventQueue object via one of its
	 * dispatchEvent* method calls. It can provide information (such
	 * as which listener method to call) so that this method
	 * can complete the delivery of the event to the listener.
	 * @param eventObject This object was passed to the EventQueue object via one of its
	 * dispatchEvent* method calls. This object was created by the event source and
	 * is passed to this method. It should contain all the necessary information (such
	 * as what event object to pass) so that this method
	 * can complete the delivery of the event to the listener.
	 */
	@Override
	public void dispatchEvent(Object listener, Object listenerObject, int eventAction, Object eventObject) {
		if (userAdmin == null) {
			return;
		}

		UserAdminListener ual = (UserAdminListener) listener;
		try {
			ual.roleChanged((UserAdminEvent) eventObject);
		} catch (Throwable t) {
			log.log(userAdmin, LogService.LOG_WARNING, UserAdminMsg.Event_Delivery_Exception, t);
		}
	}
}
