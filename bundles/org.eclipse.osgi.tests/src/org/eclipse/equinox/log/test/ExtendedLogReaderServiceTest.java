/*******************************************************************************
 * Copyright (c) 2012, 2022 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.log.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

@SuppressWarnings("deprecation") // LogService
public class ExtendedLogReaderServiceTest {

	private ExtendedLogService log;
	private ServiceReference logReference;
	private ExtendedLogReaderService reader;
	private ServiceReference readerReference;
	private ServiceReference<LoggerAdmin> loggerAdminReference;
	private LoggerAdmin loggerAdmin;
	LoggerContext rootLoggerContext;
	Map<String, LogLevel> rootLogLevels;
	boolean called;

	@Before
	public void setUp() throws Exception {
		logReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogReaderService.class.getName());
		loggerAdminReference = OSGiTestsActivator.getContext().getServiceReference(LoggerAdmin.class);

		log = (ExtendedLogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (ExtendedLogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
		loggerAdmin = OSGiTestsActivator.getContext().getService(loggerAdminReference);

		rootLoggerContext = loggerAdmin.getLoggerContext(null);
		rootLogLevels = rootLoggerContext.getLogLevels();

		Map<String, LogLevel> copyLogLevels = new HashMap<>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	@After
	public void tearDown() throws Exception {
		rootLoggerContext.setLogLevels(rootLogLevels);
		OSGiTestsActivator.getContext().ungetService(loggerAdminReference);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	@Test
	public void testaddFilteredListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, (b, loggerName, logLevel) -> true);
		log.log(LogService.LOG_INFO, "info");
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	@Test
	public void testaddNullFilterr() throws Exception {
		TestListener listener = new TestListener();

		try {
			reader.addLogListener(listener, null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	@Test
	public void testaddFilteredListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, (b, loggerName, logLevel) -> false);

		if (log.isLoggable(LogService.LOG_INFO))
			fail();

		reader.addLogListener(listener, (b, loggerName, logLevel) -> true);
		log.log(LogService.LOG_INFO, "info");
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	@Test
	public void testaddNullListener() throws Exception {
		try {
			reader.addLogListener(null);
		} catch (IllegalArgumentException t) {
			return;
		}
		fail();
	}

	@Test
	public void testBadFilter() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, (b, loggerName, logLevel) -> {
			throw new RuntimeException("Expected error for testBadFilter.");
		});

		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	@Test
	public void testSynchronousLogListener() throws Exception {
		final Thread loggerThread = Thread.currentThread();
		called = false;
		SynchronousLogListener listener = entry -> {
			assertTrue(Thread.currentThread() == loggerThread);
			called = true;
		};
		reader.addLogListener(listener);
		log.log(LogService.LOG_INFO, "info");
		assertTrue(called);
	}

	@Test
	public void testExtendedLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		String threadName = Thread.currentThread().getName();
		long threadId = getCurrentThreadId();
		log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
		ExtendedLogEntry entry = listener.getEntryX();
		long sequenceNumberBefore = entry.getSequenceNumber();

		log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
		entry = listener.getEntryX();
		assertTrue(entry.getBundle() == OSGiTestsActivator.getBundle());
		assertTrue(entry.getMessage().equals("info"));
		assertTrue(entry.getException().getMessage().equals("test"));
		assertTrue(entry.getServiceReference() == logReference);
		assertTrue(entry.getTime() >= timeBeforeLog);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);

		assertTrue(entry.getLoggerName().equals("test"));
		assertTrue(entry.getThreadName().equals(threadName));
		if (threadId >= 0)
			assertTrue(entry.getThreadId() == threadId);
		assertTrue(entry.getContext() == logReference);
		assertTrue(entry.getSequenceNumber() > sequenceNumberBefore);
	}

	private long getCurrentThreadId() {
		Thread current = Thread.currentThread();
		try {
			Method getId = Thread.class.getMethod("getId");
			Long id = (Long) getId.invoke(current);
			return id.longValue();
		} catch (Throwable t) {
			return -1;
		}
	}
}
