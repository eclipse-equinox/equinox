/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import junit.framework.TestCase;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class ExtendedLogServiceTest extends TestCase {

	private Bundle bundle;
	private ExtendedLogService log;
	private ServiceReference logReference;
	private ExtendedLogReaderService reader;
	private ServiceReference readerReference;
	private TestListener listener;

	public ExtendedLogServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		bundle = OSGiTestsActivator.getContext().getBundle();
		logReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(ExtendedLogReaderService.class.getName());

		log = (ExtendedLogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (ExtendedLogReaderService) OSGiTestsActivator.getContext().getService(readerReference);

		listener = new TestListener();
		reader.addLogListener(listener);
	}

	protected void tearDown() throws Exception {
		reader.removeLogListener(listener);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	public void testLogContext() throws Exception {
		synchronized (listener) {
			log.log(this, LogService.LOG_INFO, null);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getContext() == this);
	}

	public void testNullLogContext() throws Exception {
		synchronized (listener) {
			log.log(null, LogService.LOG_INFO, null);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getContext() == null);
	}

	public void testLogContextWithNullThrowable() throws Exception {
		synchronized (listener) {
			log.log(this, LogService.LOG_INFO, null, null);
			listener.wait();
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
			listener.wait();
		}
		assertTrue(listener.getEntryX().getLoggerName() == "test");
		assertTrue(listener.getEntry().getLevel() == 0);
		assertTrue(listener.getEntry().getMessage() == null);
		assertTrue(listener.getEntry().getException() == null);
		assertTrue(listener.getEntry().getServiceReference() == null);
	}

	public void testNullLoggerLogNull() throws Exception {
		synchronized (listener) {
			log.getLogger(null).log(null, 0, null, null);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getLoggerName() == null);
		assertTrue(listener.getEntry().getLevel() == 0);
		assertTrue(listener.getEntry().getMessage() == null);
		assertTrue(listener.getEntry().getException() == null);
		assertTrue(listener.getEntry().getServiceReference() == null);
	}

	public void testNamedLoggerLogFull() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, message, t);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getLoggerName() == "test");
		assertTrue(listener.getEntry().getBundle() == bundle);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
		assertTrue(listener.getEntry().getMessage().equals(message));
		assertTrue(listener.getEntry().getException().getMessage().equals(t.getMessage()));
		assertTrue(listener.getEntry().getServiceReference() == logReference);
	}

	public void testNamedLoggerLogFullWithNullBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger(null, "test").log(logReference, LogService.LOG_INFO, message, t);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getLoggerName() == "test");
		assertTrue(listener.getEntry().getBundle() == bundle);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
		assertTrue(listener.getEntry().getMessage().equals(message));
		assertTrue(listener.getEntry().getException().getMessage().equals(t.getMessage()));
		assertTrue(listener.getEntry().getServiceReference() == logReference);
	}

	public void testNamedLoggerLogFullWithBundle() throws Exception {
		String message = "test";
		Throwable t = new Throwable("test");
		synchronized (listener) {
			log.getLogger(bundle, "test").log(logReference, LogService.LOG_INFO, message, t);
			listener.wait();
		}
		assertTrue(listener.getEntryX().getLoggerName() == "test");
		assertTrue(listener.getEntry().getBundle() == bundle);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
		assertTrue(listener.getEntry().getMessage().equals(message));
		assertTrue(listener.getEntry().getException().getMessage().equals(t.getMessage()));
		assertTrue(listener.getEntry().getServiceReference() == logReference);
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
