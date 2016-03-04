/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class ExtendedLogServiceTest extends TestCase {

	private Bundle bundle;
	private ExtendedLogService log;
	private ServiceReference logReference;
	private ExtendedLogReaderService reader;
	private ServiceReference readerReference;
	private ServiceReference<LoggerAdmin> loggerAdminReference;
	private LoggerAdmin loggerAdmin;
	LoggerContext rootLoggerContext;
	Map<String, LogLevel> rootLogLevels;

	private TestListener listener;

	public ExtendedLogServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		bundle = OSGiTestsActivator.getContext().getBundle();
		logReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogReaderService.class.getName());
		loggerAdminReference = OSGiTestsActivator.getContext().getServiceReference(LoggerAdmin.class);

		log = (ExtendedLogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (ExtendedLogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
		loggerAdmin = OSGiTestsActivator.getContext().getService(loggerAdminReference);

		listener = new TestListener();
		reader.addLogListener(listener);

		rootLoggerContext = loggerAdmin.getLoggerContext(null);
		rootLogLevels = rootLoggerContext.getLogLevels();

		Map<String, LogLevel> copyLogLevels = new HashMap<String, LogLevel>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	protected void tearDown() throws Exception {
		rootLoggerContext.setLogLevels(rootLogLevels);
		reader.removeLogListener(listener);
		OSGiTestsActivator.getContext().ungetService(loggerAdminReference);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	public void testLogContext() throws Exception {
		synchronized (listener) {
			log.log(this, LogService.LOG_INFO, null);
			listener.waitForLogEntry();
		}
		assertTrue(listener.getEntryX().getContext() == this);
	}

	public void testNullLogContext() throws Exception {
		synchronized (listener) {
			log.log(null, LogService.LOG_INFO, null);
			listener.waitForLogEntry();
		}
		assertTrue(listener.getEntryX().getContext() == null);
	}

	public void testLogContextWithNullThrowable() throws Exception {
		synchronized (listener) {
			log.log(this, LogService.LOG_INFO, null, null);
			listener.waitForLogEntry();
		}
		assertTrue(listener.getEntryX().getContext() == this);
	}

	public void testIsLoggableTrue() throws Exception {
		if (!log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testNotIsLoggableWithNoListener() throws Exception {
		reader.removeLogListener(listener);
		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testNotIsLoggableWithListener() throws Exception {
		reader.addLogListener(listener, new LogFilter() {

			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return false;
			}
		});
		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testNamedLoggerLogNull() throws Exception {
		synchronized (listener) {
			log.getLogger("test").log(null, 0, null, null);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testNullLoggerLogNull() throws Exception {
		synchronized (listener) {
			log.getLogger((String) null).log(null, 0, null, null);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertEquals("Wrong logger name.", "LogService", entry.getLoggerName());
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testNamedLoggerLogFull() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, message, t);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	public void testNamedLoggerLogFullWithNullBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger(null, "test").log(logReference, LogService.LOG_INFO, message, t);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	public void testNamedLoggerLogFullWithBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger(bundle, "test").log(logReference, LogService.LOG_INFO, message, t);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	public void testLoggerIsLoggableTrue() throws Exception {
		reader.addLogListener(listener, new LogFilter() {

			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				if (loggerName.equals("test"))
					return true;
				return false;
			}
		});
		if (!log.getLogger("test").isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testLoggerNotIsLoggableWithListener() throws Exception {
		reader.addLogListener(listener, new LogFilter() {

			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				if (loggerName.equals("test"))
					return false;
				return true;
			}
		});
		if (log.getLogger("test").isLoggable(LogService.LOG_INFO))
			fail();
	}
}
