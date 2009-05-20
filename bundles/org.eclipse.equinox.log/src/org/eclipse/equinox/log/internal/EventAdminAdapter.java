/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.Arrays;
import java.util.HashSet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EventAdminAdapter implements ServiceTrackerCustomizer {

	private static final String[] LOG_TOPICS_ARRAY = {"*", "org/*", "org/osgi/*", "org/osgi/service/*", "org/osgi/service/log/*", "org/osgi/service/log/LogEntry/*", "org/osgi/service/log/LogEntry/LOG_ERROR", "org/osgi/service/log/LogEntry/LOG_WARNING", "org/osgi/service/log/LogEntry/LOG_INFO", "org/osgi/service/log/LogEntry/LOG_DEBUG", "org/osgi/service/log/LogEntry/LOG_OTHER"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$//$NON-NLS-10$ //$NON-NLS-11$
	private static HashSet logTopics = new HashSet(Arrays.asList(LOG_TOPICS_ARRAY));

	private ServiceTracker eventAdminTracker;
	private ServiceTracker eventHandlerTracker;
	private BundleContext context;
	private EventAdmin eventAdmin;
	private int logEventHandlers;
	private ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private EventAdminLogListener logListener;

	public EventAdminAdapter(BundleContext context, ExtendedLogReaderServiceFactory logReaderServiceFactory) {
		this.context = context;
		this.logReaderServiceFactory = logReaderServiceFactory;
		eventAdminTracker = new ServiceTracker(context, EventAdmin.class.getName(), this);
		eventHandlerTracker = new ServiceTracker(context, EventHandler.class.getName(), this);
	}

	public void start() {
		eventHandlerTracker.open();
		eventAdminTracker.open();
	}

	public void stop() {
		eventAdminTracker.close();
		eventHandlerTracker.close();
	}

	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);
		if (service instanceof EventAdmin && eventAdmin == null) {
			eventAdmin = (EventAdmin) service;
		} else if (service instanceof EventHandler && hasLogTopic(reference.getProperty(EventConstants.EVENT_TOPIC))) {
			logEventHandlers++;
		}

		if (eventAdmin != null && logEventHandlers > 0 && logListener == null) {
			logListener = new EventAdminLogListener(eventAdmin);
			logReaderServiceFactory.addLogListener(logListener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
		}

		return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		removedService(reference, service);
		addingService(reference);
	}

	public void removedService(ServiceReference reference, Object service) {
		if (service == eventAdmin) {
			eventAdmin = null;
		} else if (service instanceof EventHandler && hasLogTopic(reference.getProperty(EventConstants.EVENT_TOPIC))) {
			logEventHandlers--;
		}

		if (logListener != null && (eventAdmin == null || logEventHandlers == 0)) {
			logReaderServiceFactory.removeLogListener(logListener);
			logListener = null;
		}
	}

	private static boolean hasLogTopic(Object property) {
		if (property instanceof String)
			return logTopics.contains(property);

		if (property instanceof String[]) {
			String[] topics = (String[]) property;
			for (int i = 0; i < topics.length; i++) {
				if (logTopics.contains(topics[i]))
					return true;
			}
		}
		return false;
	}
}
