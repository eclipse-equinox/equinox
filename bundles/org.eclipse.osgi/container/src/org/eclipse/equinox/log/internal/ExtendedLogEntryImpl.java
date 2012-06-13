/*******************************************************************************
 * Copyright (c) 2006, 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;

public class ExtendedLogEntryImpl implements ExtendedLogEntry, LogEntry {

	private static long nextSequenceNumber = 1L;
	private static long nextThreadId = 1L;
	private static final Map<Thread, Long> threadIds = createThreadIdMap();

	private final String loggerName;
	private final Bundle bundle;
	private final int level;
	private final String message;
	private final Throwable throwable;
	private final Object contextObject;
	private final long time;
	private final long threadId;
	private final String threadName;
	private final long sequenceNumber;

	private static Map<Thread, Long> createThreadIdMap() {
		try {
			Thread.class.getMethod("getId", (Class[]) null); //$NON-NLS-1$
		} catch (NoSuchMethodException e) {
			return new WeakHashMap<Thread, Long>();
		}
		return null;
	}

	private static long getId(Thread thread) {
		if (threadIds == null)
			return thread.getId();

		Long threadId = threadIds.get(thread);
		if (threadId == null) {
			threadId = new Long(nextThreadId++);
			threadIds.put(thread, threadId);
		}
		return threadId.longValue();
	}

	public ExtendedLogEntryImpl(Bundle bundle, String loggerName, Object contextObject, int level, String message, Throwable throwable) {
		this.time = System.currentTimeMillis();
		this.loggerName = loggerName;
		this.bundle = bundle;
		this.level = level;
		this.message = message;
		this.throwable = throwable;
		this.contextObject = contextObject;

		Thread currentThread = Thread.currentThread();
		this.threadName = currentThread.getName();

		synchronized (ExtendedLogEntryImpl.class) {
			this.threadId = getId(currentThread);
			this.sequenceNumber = nextSequenceNumber++;
		}
	}

	public String getLoggerName() {
		return loggerName;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public long getThreadId() {
		return threadId;
	}

	public String getThreadName() {
		return threadName;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public Throwable getException() {
		return throwable;
	}

	public int getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	@SuppressWarnings("rawtypes")
	public ServiceReference getServiceReference() {
		if (contextObject != null && contextObject instanceof ServiceReference)
			return (ServiceReference) contextObject;

		return null;
	}

	public long getTime() {
		return time;
	}

	public Object getContext() {
		return contextObject;
	}
}
