/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import java.security.Guard;
import java.security.GuardedObject;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ScheduledApplication;
import org.osgi.service.event.*;
import org.osgi.util.tracker.ServiceTracker;

public class EclipseScheduledApplication implements ScheduledApplication, EventHandler {
	private static final String FILTER_PREFIX = "(&(objectclass=" + ApplicationDescriptor.class.getName() + ")(" + ApplicationDescriptor.APPLICATION_PID + "="; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final String FILTER_POSTFIX = "))"; //$NON-NLS-1$
	private static final String TRIGGERING_EVENT = "org.osgi.triggeringevent"; //$NON-NLS-1$

	private boolean recurring;
	private String topic;
	private String eventFilter;
	private Map args;
	private String appPid;
	private Integer id;
	private ServiceRegistration sr;
	private ServiceTracker appTracker;
	private boolean removed = false;

	EclipseScheduledApplication(BundleContext context, Integer id, String appPid, Map args, String topic, String eventFilter, boolean recurring) throws InvalidSyntaxException {
		this.id = id;
		this.appPid = appPid;
		this.args = args;
		this.topic = topic == null || topic.trim().equals("") || topic.trim().equals("*") ? null : topic; //$NON-NLS-1$ //$NON-NLS-2$
		this.eventFilter = eventFilter;
		this.recurring = recurring;
		appTracker = new ServiceTracker(context, context.createFilter(FILTER_PREFIX + appPid + FILTER_POSTFIX), null);
		AppManager.openTracker(appTracker, false);
	}

	Integer getID() {
		return id;
	}

	String getAppPid() {
		return appPid;
	}

	public String getTopic() {
		return topic;
	}

	public String getEventFilter() {
		return eventFilter;
	}

	public boolean isRecurring() {
		return recurring;
	}

	public synchronized ApplicationDescriptor getApplicationDescriptor() {
		if (removed)
			throw new IllegalStateException(Messages.EclipseScheduledApplication_7);
		return (ApplicationDescriptor) AppManager.getService(appTracker);
	}

	public Map getArguments() {
		return args == null ? null : new HashMap(args);
	}

	private Map getArguments(Event trigger) {
		Map result = args == null ? new HashMap() : getArguments();
		result.put(TRIGGERING_EVENT, new GuardedObject(trigger, new TriggerGuard(trigger.getTopic())));
		return result;
	}

	public synchronized void remove() {
		if (removed)
			return;
		removed  = true;
		AppManager.removeScheduledApp(this);
		if (sr != null)
			sr.unregister();
		appTracker.close();
	}

	public synchronized void handleEvent(Event event) {
		try {
			ApplicationDescriptor desc = getApplicationDescriptor();
			if (desc == null)
				return;
			desc.launch(getArguments(event));
		} catch (Exception e) {
			// TODO should log this
			return; // return here to avoid removing non-recurring apps when an error occurs
		}
		if (!isRecurring())
			remove();
	}

	void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
		if (removed) // just incase we were removed before the sr was set
			sr.unregister();
	}

	public class TriggerGuard implements Guard {
		String eventTopic;
		public TriggerGuard(String topic) {
			this.eventTopic = topic;
		}

		public void checkGuard(Object object) throws SecurityException {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null)
				sm.checkPermission(new TopicPermission(eventTopic, TopicPermission.SUBSCRIBE));
		}

	}
}
