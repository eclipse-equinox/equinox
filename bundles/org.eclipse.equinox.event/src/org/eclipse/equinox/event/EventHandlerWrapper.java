/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.event;

import java.security.Permission;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.event.*;
import org.osgi.service.log.LogService;

/**
 * A wrapper for EventHandlers. This class caches property values and 
 * performs final checks before calling the wrapped handler.
 *
 */
public class EventHandlerWrapper {
	private final ServiceReference reference;
	private final LogService log;
	private final BundleContext context;
	private EventHandler handler;
	private String[] topics;
	private Filter filter;
	
	/**
	 * Create an EventHandlerWrapper. 

	 * @param reference Reference to the EventHandler
	 * @param context Bundle Context of the Event Admin bundle
	 * @param log LogService object for logging
	 */
	public EventHandlerWrapper(ServiceReference reference, BundleContext context, LogService log) {
		this.reference = reference;
		this.context = context;
		this.log = log;
	}

	/**
	 * Cache values from service properties
	 * 
	 * @return true if the handler should be called; false if the handler should not be called
	 */
	public synchronized boolean init() {
		topics = null;
		filter = null;

		// Get topic names
		Object o = reference.getProperty(EventConstants.EVENT_TOPIC);
		if (o instanceof String) {
			topics = new String[] {(String)o};
		}
		else if (o instanceof String[]) {
			topics = (String[]) o;
		}
		
		if (topics == null) {
			return false;
		}
		
		// get filter
		o = reference.getProperty(EventConstants.EVENT_FILTER);
		if (o instanceof String) {
			try {
				filter = context.createFilter((String)o);
			}
			catch (InvalidSyntaxException e) {
				log.log(LogService.LOG_ERROR,NLS.bind(EventAdminMsg.EVENT_INVALID_HANDLER_FILTER, o), e);
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Flush the handler service if it has been obtained.
	 */
	public void flush() {
		synchronized (this) {
			if (handler == null) {
				return;
			}
			handler = null;
		}
		context.ungetService(reference);
	}
	
	/**
	 * Get the event topics for the wrapped handler.
	 * 
	 * @return The wrapped handler's event topics
	 */
	public synchronized String[] getTopics()  {
		return topics;
	}
	
	/**
	 * Return the wrapped handler. 
	 * @return The wrapped handler.
	 */
	private EventHandler getHandler() {
		synchronized (this) {
			// if we already have a handler, return it
			if (handler != null) {
				return handler;
			}
		}
		
		// we don't have the handler, so lets get it outside the sync region
		EventHandler tempHandler = (EventHandler)context.getService(reference);

		synchronized (this) {
			// do we still need the handler we just got?
			if (handler == null) {
				handler = tempHandler;
				return handler;
			}
			// get the current handler
			tempHandler = handler;
		}
		
		// unget the handler we just got since we don't need it
		context.ungetService(reference);
		
		// return the current handler (copied into the local var)
		return tempHandler;
	}
	
	/**
	 * Get the filter object
	 * 
	 * @return The handler's filter
	 */
	private synchronized Filter getFilter() {
		return filter;
	}

	/**
	 * Dispatch event to handler. Perform final tests before actually calling the handler.
	 * 
	 * @param event The event to dispatch
	 * @param perm The permission to be checked
	 */
	public void handleEvent(Event event, Permission perm) {
		Bundle bundle = reference.getBundle();
		// is service unregistered?
		if (bundle == null) {
			return;
		}
		
		// filter match
		Filter eventFilter = getFilter();
		if ((eventFilter != null) && !event.matches(eventFilter)) {
			return;
		}
		
		// permission check
		if ((perm != null) && (!bundle.hasPermission(perm))) {
			return;
		}
		
		// get handler service
		EventHandler handlerService = getHandler();
		if (handlerService == null) {
			return;
		}
	
		try {
			handlerService.handleEvent(event);
		}
		catch (Throwable t) {
			// log/handle any Throwable thrown by the listener
			log.log(LogService.LOG_ERROR, NLS.bind(EventAdminMsg.EVENT_DISPATCH_HANDLER_EXCEPTION, event, handlerService), t);
		}
	}
}
