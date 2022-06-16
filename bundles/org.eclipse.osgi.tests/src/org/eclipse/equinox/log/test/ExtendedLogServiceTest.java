/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class ExtendedLogServiceTest {

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

	@Before
	public void setUp() throws Exception {
		bundle = OSGiTestsActivator.getBundle();
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

		Map<String, LogLevel> copyLogLevels = new HashMap<>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	@After
	public void tearDown() throws Exception {
		rootLoggerContext.setLogLevels(rootLogLevels);
		reader.removeLogListener(listener);
		OSGiTestsActivator.getContext().ungetService(loggerAdminReference);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	@Test
	public void testLogContext() throws Exception {
		log.log(this, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getContext() == this);
	}

	@Test
	public void testNullLogContext() throws Exception {
		log.log(null, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getContext() == null);
	}

	@Test
	public void testLogContextWithNullThrowable() throws Exception {
		log.log(this, LogService.LOG_INFO, null, null);
		assertTrue(listener.getEntryX().getContext() == this);
	}

	@Test
	public void testIsLoggableTrue() throws Exception {
		if (!log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	@Test
	public void testNotIsLoggableWithNoListener() throws Exception {
		reader.removeLogListener(listener);
		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	@Test
	public void testNotIsLoggableWithListener() throws Exception {
		reader.addLogListener(listener, (b, loggerName, logLevel) -> false);
		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	@Test
	public void testNamedLoggerLogNull() throws Exception {
		log.getLogger("test").log(null, 0, null, null);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	@Test
	public void testNullLoggerLogNull() throws Exception {
		log.getLogger((String) null).log(null, 0, null, null);
		ExtendedLogEntry entry = listener.getEntryX();
		assertEquals("Wrong logger name.", "LogService" + "." + bundle.getSymbolicName(), entry.getLoggerName());
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	@Test
	public void testNamedLoggerLogFull() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		log.getLogger("test").log(logReference, LogService.LOG_INFO, message, t);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	@Test
	public void testNamedLoggerLogFullWithNullBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		log.getLogger(null, "test").log(logReference, LogService.LOG_INFO, message, t);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	@Test
	public void testNamedLoggerLogFullWithBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		log.getLogger(bundle, "test").log(logReference, LogService.LOG_INFO, message, t);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getBundle() == bundle);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	@Test
	public void testLoggerIsLoggableTrue() throws Exception {
		reader.addLogListener(listener, (b, loggerName, logLevel) -> {
			if (loggerName.equals("test"))
				return true;
			return false;
		});
		if (!log.getLogger("test").isLoggable(LogService.LOG_INFO))
			fail();
	}

	@Test
	public void testLoggerNotIsLoggableWithListener() throws Exception {
		reader.addLogListener(listener, (b, loggerName, logLevel) -> {
			if (loggerName.equals("test"))
				return false;
			return true;
		});
		if (log.getLogger("test").isLoggable(LogService.LOG_INFO))
			fail();
	}
}
