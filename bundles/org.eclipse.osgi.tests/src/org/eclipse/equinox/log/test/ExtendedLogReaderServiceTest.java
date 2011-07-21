/*******************************************************************************
 * Copyright (c) 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.test;

import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.*;

public class ExtendedLogReaderServiceTest extends TestCase {

	private ExtendedLogService log;
	private ServiceReference logReference;
	private ExtendedLogReaderService reader;
	private ServiceReference readerReference;
	private boolean called;

	public ExtendedLogReaderServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		logReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogReaderService.class.getName());

		log = (ExtendedLogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (ExtendedLogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
	}

	protected void tearDown() throws Exception {
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	public void testaddFilteredListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return true;
			}
		});
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddNullFilterr() throws Exception {
		TestListener listener = new TestListener();

		try {
			reader.addLogListener(listener, null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testaddFilteredListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return false;
			}
		});

		if (log.isLoggable(LogService.LOG_INFO))
			fail();

		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return true;
			}
		});
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddNullListener() throws Exception {
		try {
			reader.addLogListener(null);
		} catch (IllegalArgumentException t) {
			return;
		}
		fail();
	}

	public void testBadFilter() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				throw new RuntimeException("Expected error for testBadFilter.");
			}
		});

		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testSynchronousLogListener() throws Exception {
		final Thread loggerThread = Thread.currentThread();
		called = false;
		LogListener listener = new SynchronousLogListener() {
			public void logged(LogEntry entry) {
				assertTrue(Thread.currentThread() == loggerThread);
				called = true;
			}
		};
		reader.addLogListener(listener);
		log.log(LogService.LOG_INFO, "info");
		assertTrue(called);
	}

	public void testExtendedLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		String threadName = Thread.currentThread().getName();
		long threadId = getCurrentThreadId();
		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
			listener.wait();
		}
		long sequenceNumberBefore = listener.getEntryX().getSequenceNumber();

		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
			listener.wait();
		}
		assertTrue(listener.getEntryX().getBundle() == OSGiTestsActivator.getContext().getBundle());
		assertTrue(listener.getEntryX().getMessage().equals("info"));
		assertTrue(listener.getEntryX().getException().getMessage().equals("test"));
		assertTrue(listener.getEntryX().getServiceReference() == logReference);
		assertTrue(listener.getEntryX().getTime() >= timeBeforeLog);
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);

		assertTrue(listener.getEntryX().getLoggerName().equals("test"));
		assertTrue(listener.getEntryX().getThreadName().equals(threadName));
		if (threadId >= 0)
			assertTrue(listener.getEntryX().getThreadId() == threadId);
		assertTrue(listener.getEntryX().getContext() == logReference);
		assertTrue(listener.getEntryX().getSequenceNumber() > sequenceNumberBefore);
	}

	private long getCurrentThreadId() {
		Thread current = Thread.currentThread();
		try {
			Method getId = Thread.class.getMethod("getId", null);
			Long id = (Long) getId.invoke(current, null);
			return id.longValue();
		} catch (Throwable t) {
			return -1;
		}
	}
}
