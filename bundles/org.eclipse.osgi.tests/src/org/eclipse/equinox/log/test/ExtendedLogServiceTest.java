/*******************************************************************************
s
s This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogFilter;
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

		Map<String, LogLevel> copyLogLevels = new HashMap<>(rootLogLevels);
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
		log.log(this, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getContext() == this);
	}

	public void testNullLogContext() throws Exception {
		log.log(null, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getContext() == null);
	}

	public void testLogContextWithNullThrowable() throws Exception {
		log.log(this, LogService.LOG_INFO, null, null);
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
		log.getLogger("test").log(null, 0, null, null);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLoggerName() == "test");
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testNullLoggerLogNull() throws Exception {
		log.getLogger((String) null).log(null, 0, null, null);
		ExtendedLogEntry entry = listener.getEntryX();
		assertEquals("Wrong logger name.", "LogService" + "." + bundle.getSymbolicName(), entry.getLoggerName());
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

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
