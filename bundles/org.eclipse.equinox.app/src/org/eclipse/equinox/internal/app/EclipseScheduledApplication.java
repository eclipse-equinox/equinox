/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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

package org.eclipse.equinox.internal.app;

import java.security.Guard;
import java.security.GuardedObject;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ScheduledApplication;
import org.osgi.service.event.*;
import org.osgi.util.tracker.ServiceTracker;

public class EclipseScheduledApplication implements ScheduledApplication, EventHandler {
	private static final String FILTER_PREFIX = "(&(objectclass=" + ApplicationDescriptor.class.getName() + ")(" //$NON-NLS-1$ //$NON-NLS-2$
			+ ApplicationDescriptor.APPLICATION_PID + "="; //$NON-NLS-1$
	private static final String FILTER_POSTFIX = "))"; //$NON-NLS-1$

	private final boolean recurring;
	private final String topic;
	private final String eventFilter;
	private final Map<String, Object> args;
	private final String appPid;
	private final String id;
	private ServiceRegistration sr;
	private final ServiceTracker appTracker;
	private boolean removed = false;

	EclipseScheduledApplication(BundleContext context, String id, String appPid, Map<String, Object> args, String topic,
			String eventFilter, boolean recurring) throws InvalidSyntaxException {
		this.id = id;
		this.appPid = appPid;
		this.args = args;
		this.topic = topic == null || topic.trim().equals("") || topic.trim().equals("*") ? null : topic; //$NON-NLS-1$ //$NON-NLS-2$
		this.eventFilter = eventFilter;
		this.recurring = recurring;
		appTracker = new ServiceTracker(context, context.createFilter(FILTER_PREFIX + appPid + FILTER_POSTFIX), null);
		Activator.openTracker(appTracker, false);
	}

	@Override
	public String getScheduleId() {
		return id;
	}

	String getAppPid() {
		return appPid;
	}

	@Override
	public synchronized String getTopic() {
		if (removed) {
			throw new IllegalStateException(Messages.scheduled_app_removed);
		}
		return topic;
	}

	@Override
	public synchronized String getEventFilter() {
		if (removed) {
			throw new IllegalStateException(Messages.scheduled_app_removed);
		}
		return eventFilter;
	}

	@Override
	public synchronized boolean isRecurring() {
		if (removed) {
			throw new IllegalStateException(Messages.scheduled_app_removed);
		}
		return recurring;
	}

	@Override
	public synchronized ApplicationDescriptor getApplicationDescriptor() {
		if (removed) {
			throw new IllegalStateException(Messages.scheduled_app_removed);
		}
		return (ApplicationDescriptor) Activator.getService(appTracker);
	}

	@Override
	public synchronized Map<String, Object> getArguments() {
		if (removed) {
			throw new IllegalStateException(Messages.scheduled_app_removed);
		}
		return args == null ? null : new HashMap<>(args);
	}

	private Map<String, Object> getArguments(Event trigger) {
		Map<String, Object> result = args == null ? new HashMap<>() : getArguments();
		result.put(TRIGGERING_EVENT, new GuardedObject(trigger, new TriggerGuard(trigger.getTopic())));
		return result;
	}

	@Override
	public synchronized void remove() {
		if (removed) {
			return;
		}
		removed = true;
		AppPersistence.removeScheduledApp(this);
		if (sr != null) {
			sr.unregister();
		}
		sr = null;
		appTracker.close();
	}

	@Override
	public synchronized void handleEvent(Event event) {
		try {
			if (removed) {
				return;
			}
			ApplicationDescriptor desc = getApplicationDescriptor();
			if (desc == null) {
				// in this case the application descriptor was removed;
				// we must return and keep the scheduled app incase the application comes back
				return;
			}
			desc.launch(getArguments(event));
		} catch (Exception e) {
			String message = NLS.bind(Messages.scheduled_app_launch_error, sr);
			Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, e, null));
			return; // return here to avoid removing non-recurring apps when an error occurs
		}
		if (!isRecurring()) {
			remove();
		}
	}

	synchronized void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
		if (removed) { // just incase we were removed before the sr was set
			sr.unregister();
		}
	}

	/*
	 * This is used to guard the event topic argument which is passed to an
	 * application when we are launching it from a scheduling.
	 */
	public class TriggerGuard implements Guard {
		String eventTopic;

		public TriggerGuard(String topic) {
			this.eventTopic = topic;
		}

		/*
		 * does the proper TopicPermission check for the event topic
		 */
		@Override
		public void checkGuard(Object object) throws SecurityException {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null) {
				sm.checkPermission(new TopicPermission(eventTopic, TopicPermission.SUBSCRIBE));
			}
		}

	}
}
