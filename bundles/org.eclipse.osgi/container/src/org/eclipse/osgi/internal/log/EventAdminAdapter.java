/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.internal.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EventAdminAdapter implements ServiceTrackerCustomizer<Object, Object> {
	public static final String EVENT_TOPIC = "event.topics"; //$NON-NLS-1$
	private static final String[] LOG_TOPICS_ARRAY = { "*", "org/*", "org/osgi/*", "org/osgi/service/*", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"org/osgi/service/log/*", "org/osgi/service/log/LogEntry/*", "org/osgi/service/log/LogEntry/LOG_ERROR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"org/osgi/service/log/LogEntry/LOG_WARNING", "org/osgi/service/log/LogEntry/LOG_INFO", //$NON-NLS-1$ //$NON-NLS-2$
			"org/osgi/service/log/LogEntry/LOG_DEBUG", "org/osgi/service/log/LogEntry/LOG_OTHER" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final Object LOG_TOPIC_TOKEN = new Object();
	private static Collection<String> logTopics = new HashSet<>(Arrays.asList(LOG_TOPICS_ARRAY));
	private static Collection<String> eventAdminObjectClass = Arrays.asList("org.osgi.service.event.EventAdmin"); //$NON-NLS-1$
	private static Collection<String> eventHandlerObjectClass = Arrays.asList("org.osgi.service.event.EventHandler"); //$NON-NLS-1$

	private ServiceTracker<Object, Object> eventAdminTracker;
	private ServiceTracker<Object, Object> eventHandlerTracker;
	private BundleContext context;
	private ServiceReference<Object> eventAdmin;
	private int logEventHandlers;
	private ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private EventAdminLogListener logListener;

	public EventAdminAdapter(BundleContext context, ExtendedLogReaderServiceFactory logReaderServiceFactory) {
		this.context = context;
		this.logReaderServiceFactory = logReaderServiceFactory;
		eventAdminTracker = new ServiceTracker<>(context, "org.osgi.service.event.EventAdmin", this); //$NON-NLS-1$
		eventHandlerTracker = new ServiceTracker<>(context, "org.osgi.service.event.EventHandler", this); //$NON-NLS-1$
	}

	public void start() {
		eventAdminTracker.open(true);
		eventHandlerTracker.open(true);
	}

	public void stop() {
		eventAdminTracker.close();
		eventHandlerTracker.close();
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		Object toTrack = null;
		Object objectClass = reference.getProperty(Constants.OBJECTCLASS);
		Object topics = reference.getProperty(EVENT_TOPIC);
		if (checkServiceProp(objectClass, eventAdminObjectClass) && eventAdmin == null) {
			toTrack = reference;
			eventAdmin = reference;
		} else if (checkServiceProp(objectClass, eventHandlerObjectClass) && checkServiceProp(topics, logTopics)) {
			logEventHandlers++;
			toTrack = LOG_TOPIC_TOKEN;
		}

		if (eventAdmin != null && logEventHandlers > 0 && logListener == null) {
			try {
				logListener = new EventAdminLogListener(context.getService(eventAdmin));
			} catch (ClassNotFoundException | NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logReaderServiceFactory.addLogListener(logListener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
		}

		return toTrack;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object tracked) {
		removedService(reference, tracked);
		addingService(reference);
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object tracked) {
		if (tracked == eventAdmin) {
			eventAdmin = null;
			context.ungetService(reference);
		} else if (LOG_TOPIC_TOKEN == tracked) {
			logEventHandlers--;
		}

		if (logListener != null && (eventAdmin == null || logEventHandlers == 0)) {
			logReaderServiceFactory.removeLogListener(logListener);
			logListener = null;
		}
	}

	private static boolean checkServiceProp(Object property, Collection<String> check) {
		if (property instanceof String)
			return check.contains(property);

		if (property instanceof String[]) {
			String[] topics = (String[]) property;
			for (String topic : topics) {
				if (check.contains(topic)) {
					return true;
				}
			}
		}

		if (property instanceof Collection) {
			for (Object prop : (Collection<?>) property)
				if (check.contains(prop))
					return true;
		}
		return false;
	}
}
