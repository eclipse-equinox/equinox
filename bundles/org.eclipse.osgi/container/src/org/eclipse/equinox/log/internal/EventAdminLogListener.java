/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.lang.reflect.*;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

public class EventAdminLogListener implements SynchronousLogListener {

	// constants for Event topic substring
	public static final String TOPIC = "org/osgi/service/log/LogEntry"; //$NON-NLS-1$
	public static final char TOPIC_SEPARATOR = '/';
	// constants for Event types
	public static final String LOG_ERROR = "LOG_ERROR"; //$NON-NLS-1$
	public static final String LOG_WARNING = "LOG_WARNING"; //$NON-NLS-1$
	public static final String LOG_INFO = "LOG_INFO"; //$NON-NLS-1$
	public static final String LOG_DEBUG = "LOG_DEBUG"; //$NON-NLS-1$
	public static final String LOG_OTHER = "LOG_OTHER"; //$NON-NLS-1$
	// constants for Event properties
	public static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
	public static final String MESSAGE = "message"; //$NON-NLS-1$
	public static final String LOG_LEVEL = "log.level"; //$NON-NLS-1$
	public static final String LOG_ENTRY = "log.entry"; //$NON-NLS-1$
	public static final String SERVICE = "service"; //$NON-NLS-1$
	public static final String SERVICE_ID = "service.id"; //$NON-NLS-1$
	public static final String SERVICE_OBJECTCLASS = "service.objectClass"; //$NON-NLS-1$
	public static final String SERVICE_PID = "service.pid"; //$NON-NLS-1$
	public static final String BUNDLE = "bundle"; //$NON-NLS-1$
	public static final String BUNDLE_ID = "bundle.id"; //$NON-NLS-1$
	public static final String BUNDLE_SYMBOLICNAME = "bundle.symbolicName"; //$NON-NLS-1$
	public static final String EVENT = "event"; //$NON-NLS-1$
	public static final String EXCEPTION = "exception"; //$NON-NLS-1$
	public static final String EXCEPTION_CLASS = "exception.class"; //$NON-NLS-1$
	public static final String EXCEPTION_MESSAGE = "exception.message"; //$NON-NLS-1$

	private final Object eventAdmin;
	private final Method postEvent;
	private final Constructor<?> event;

	public EventAdminLogListener(Object eventAdmin) throws ClassNotFoundException, NoSuchMethodException {
		this.eventAdmin = eventAdmin;
		Class<?> eventAdminClass = eventAdmin.getClass();
		ClassLoader cl = eventAdminClass.getClassLoader();
		Class<?> eventClass = cl.loadClass("org.osgi.service.event.Event"); //$NON-NLS-1$

		postEvent = eventAdminClass.getMethod("postEvent", eventClass); //$NON-NLS-1$
		event = eventClass.getConstructor(String.class, Dictionary.class);
	}

	public void logged(LogEntry entry) {
		try {
			Object convertedEvent = convertEvent(entry);
			postEvent.invoke(eventAdmin, convertedEvent);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if ((t instanceof RuntimeException))
				throw (RuntimeException) t;
			if ((t instanceof Error))
				throw (Error) t;
			// unexpected
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			// unexpected
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			// unexpected
			throw new RuntimeException(e);
		}
	}

	private Object convertEvent(LogEntry entry) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		String topic = TOPIC;
		int level = entry.getLevel();
		switch (level) {
			case LogService.LOG_ERROR :
				topic += TOPIC_SEPARATOR + LOG_ERROR;
				break;
			case LogService.LOG_WARNING :
				topic += TOPIC_SEPARATOR + LOG_WARNING;
				break;
			case LogService.LOG_INFO :
				topic += TOPIC_SEPARATOR + LOG_INFO;
				break;
			case LogService.LOG_DEBUG :
				topic += TOPIC_SEPARATOR + LOG_DEBUG;
				break;
			default : // other log levels are represented by LOG_OTHER
				topic += TOPIC_SEPARATOR + LOG_OTHER;
		}
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		Bundle bundle = entry.getBundle();
		if (bundle == null) {
			throw new RuntimeException("LogEntry.getBundle() returns null"); //$NON-NLS-1$
		}
		putBundleProperties(properties, bundle);
		Throwable t = entry.getException();
		if (t != null) {
			putExceptionProperties(properties, t);
		}
		ServiceReference<?> ref = entry.getServiceReference();
		if (ref != null) {
			putServiceReferenceProperties(properties, ref);
		}
		properties.put(LOG_ENTRY, entry);
		properties.put(LOG_LEVEL, new Integer(entry.getLevel()));
		if (entry.getMessage() != null)
			properties.put(MESSAGE, entry.getMessage());
		properties.put(TIMESTAMP, new Long(entry.getTime()));
		return event.newInstance(topic, properties);
	}

	public static void putServiceReferenceProperties(Hashtable<String, Object> properties, ServiceReference<?> ref) {
		properties.put(SERVICE, ref);
		properties.put(SERVICE_ID, ref.getProperty(org.osgi.framework.Constants.SERVICE_ID));
		Object o = ref.getProperty(org.osgi.framework.Constants.SERVICE_PID);
		if ((o != null) && (o instanceof String)) {
			properties.put(SERVICE_PID, o);
		}
		Object o2 = ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		if ((o2 != null) && (o2 instanceof String[])) {
			properties.put(SERVICE_OBJECTCLASS, o2);
		}
	}

	public static void putBundleProperties(Hashtable<String, Object> properties, Bundle bundle) {
		properties.put(BUNDLE_ID, new Long(bundle.getBundleId()));
		String symbolicName = bundle.getSymbolicName();
		if (symbolicName != null) {
			properties.put(BUNDLE_SYMBOLICNAME, symbolicName);
		}
		properties.put(BUNDLE, bundle);
	}

	public static void putExceptionProperties(Hashtable<String, Object> properties, Throwable t) {
		properties.put(EXCEPTION, t);
		properties.put(EXCEPTION_CLASS, t.getClass().getName());
		String message = t.getMessage();
		if (message != null) {
			properties.put(EXCEPTION_MESSAGE, t.getMessage());
		}
	}
}