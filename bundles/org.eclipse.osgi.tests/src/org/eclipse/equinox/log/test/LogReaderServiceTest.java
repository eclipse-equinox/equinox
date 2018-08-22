/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class LogReaderServiceTest extends AbstractBundleTests {

	private LogService log;
	private ServiceReference logReference;
	private LogReaderService reader;
	private ServiceReference readerReference;
	private ServiceReference<LoggerAdmin> loggerAdminReference;
	private LoggerAdmin loggerAdmin;
	LoggerContext rootLoggerContext;
	Map<String, LogLevel> rootLogLevels;

	public LogReaderServiceTest(String name) {
		setName(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		logReference = OSGiTestsActivator.getContext().getServiceReference(LogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(LogReaderService.class.getName());
		loggerAdminReference = OSGiTestsActivator.getContext().getServiceReference(LoggerAdmin.class);

		log = (LogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (LogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
		loggerAdmin = OSGiTestsActivator.getContext().getService(loggerAdminReference);

		rootLoggerContext = loggerAdmin.getLoggerContext(null);
		rootLogLevels = rootLoggerContext.getLogLevels();

		Map<String, LogLevel> copyLogLevels = new HashMap<String, LogLevel>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	protected void tearDown() throws Exception {
		rootLoggerContext.setLogLevels(rootLogLevels);
		OSGiTestsActivator.getContext().ungetService(loggerAdminReference);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
		super.tearDown();
	}

	public void testaddListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
			listener.waitForLogEntry();
		}
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	public void testaddListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		reader.addLogListener(listener);
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
			listener.waitForLogEntry();
		}
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	public void testaddNullListener() throws Exception {
		try {
			reader.addLogListener(null);
		} catch (IllegalArgumentException t) {
			return;
		}
		fail();
	}

	public void testBadListener() throws Exception {
		LogListener listener = new LogListener() {
			public synchronized void logged(LogEntry entry) {
				notifyAll();
				throw new RuntimeException("Expected error for testBadListener."); //$NON-NLS-1$
			}
		};
		reader.addLogListener(listener);

		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
			listener.wait();
		}
	}

	public void testLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		synchronized (listener) {
			log.log(logReference, LogService.LOG_INFO, "info", new Throwable("test")); //$NON-NLS-1$ //$NON-NLS-2$
			listener.waitForLogEntry();
		}
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getBundle() == OSGiTestsActivator.getContext().getBundle());
		assertTrue(entry.getMessage().equals("info")); //$NON-NLS-1$
		assertTrue(entry.getException().getMessage().equals("test")); //$NON-NLS-1$
		assertTrue(entry.getServiceReference() == logReference);
		assertTrue(entry.getTime() >= timeBeforeLog);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
	}

	public void testLogBundleEventInfo() throws Exception {
		// this is just a bundle that is harmless to start/stop
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		TestListener listener = new TestListener(testBundle);
		reader.addLogListener(listener);
		synchronized (listener) {
			testBundle.start();
			listener.waitForLogEntry();
		}

		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof BundleEvent);
	}

	public void testLogBundleEventSynchronous() throws Exception {
		// this is just a bundle that is harmless to start/stop
		final Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		final AtomicReference<Thread> logThread = new AtomicReference<>();
		LogListener listener = new SynchronousLogListener() {
			@Override
			public void logged(LogEntry entry) {
				if (entry.getBundle() == testBundle) {
					logThread.compareAndSet(null, Thread.currentThread());
				}
			}
		};
		reader.addLogListener(listener);
		testBundle.start();

		assertEquals("Wrong thread for synchronous bundle event logs.", Thread.currentThread(), logThread.get());
	}

	public void testLogServiceEventInfo() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof ServiceEvent);
	}

	public void testLogServiceEventDebug() throws Exception {
		ServiceRegistration registration = OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);

		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			registration.setProperties(new Hashtable());
			listener.waitForLogEntry();
		}
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_DEBUG);
		assertEquals("Wrong level.", LogLevel.DEBUG, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof ServiceEvent);
	}

	public void testLogFrameworkEvent() throws Exception {
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		final AtomicReference<LogEntry> logEntry = new AtomicReference<>();
		final CountDownLatch countDown = new CountDownLatch(1);
		LogListener listener = new LogListener() {
			@Override
			public void logged(LogEntry entry) {
				if ("Events.Framework".equals(entry.getLoggerName())) {
					logEntry.set(entry);
					countDown.countDown();
				}
			}
		};
		reader.addLogListener(listener);
		installer.refreshPackages(new Bundle[] {testBundle});

		countDown.await(5, TimeUnit.SECONDS);

		ExtendedLogEntry entry = (ExtendedLogEntry) logEntry.get();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof FrameworkEvent);
	}

	public void testLogFrameworkEventType() throws Exception {
		final List<LogEntry> events = new CopyOnWriteArrayList<LogEntry>();
		final CountDownLatch countDown = new CountDownLatch(3);
		final Bundle b = getContext().getBundle();
		LogListener listener = new LogListener() {
			@Override
			public void logged(LogEntry entry) {
				if (b.equals(entry.getBundle())) {
					events.add(entry);
					countDown.countDown();
				}
			}
		};
		reader.addLogListener(listener);

		//publishing an event with ERROR
		b.adapt(Module.class).getContainer().getAdaptor().publishContainerEvent(ContainerEvent.ERROR, b.adapt(Module.class), new Exception());
		//publishing an event with WARNING
		b.adapt(Module.class).getContainer().getAdaptor().publishContainerEvent(ContainerEvent.WARNING, b.adapt(Module.class), new Exception());
		//publishing an event with INFO
		b.adapt(Module.class).getContainer().getAdaptor().publishContainerEvent(ContainerEvent.INFO, b.adapt(Module.class), new Exception());

		countDown.await(2, TimeUnit.SECONDS);
		assertEquals("Wrong number of events", 3, events.size());
		assertEquals("Wrong type.", LogLevel.ERROR, events.get(0).getLogLevel());
		assertEquals("Wrong type.", LogLevel.WARN, events.get(1).getLogLevel());
		assertEquals("Wrong type.", LogLevel.INFO, events.get(2).getLogLevel());

	}

	public void testLogHistory1() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_LOG_HISTORY_MAX, "10");
		Equinox equinox = new Equinox(configuration);
		equinox.start();

		try {
			LogService testLog = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(LogService.class));
			LogReaderService testReader = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(LogReaderService.class));
			assertEquals("Expecting no logs.", 0, countLogEntries(testReader.getLog(), 0));
			// log 9 things
			for (int i = 0; i < 9; i++) {
				testLog.log(LogService.LOG_WARNING, String.valueOf(i));
			}
			assertEquals("Wrong number of logs.", 9, countLogEntries(testReader.getLog(), 8));

			// log 9 more things
			for (int i = 9; i < 18; i++) {
				testLog.log(LogService.LOG_WARNING, String.valueOf(i));
			}

			// should only be the last 10 logs (17-8)
			assertEquals("Wrong number of logs.", 10, countLogEntries(testReader.getLog(), 17));
		} finally {
			try {
				equinox.stop();
			} catch (BundleException e) {
				// ignore
			}
		}
	}

	public void testLogHistory2() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.start();

		try {
			LogService testLog = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(LogService.class));
			LogReaderService testReader = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(LogReaderService.class));
			assertEquals("Expecting no logs.", 0, countLogEntries(testReader.getLog(), 0));
			// log 9 things
			for (int i = 0; i < 9; i++) {
				testLog.log(LogService.LOG_WARNING, String.valueOf(i));
			}
			assertEquals("Wrong number of logs.", 0, countLogEntries(testReader.getLog(), 0));
		} finally {
			try {
				equinox.stop();
			} catch (BundleException e) {
				// ignore
			}
		}
	}

	private int countLogEntries(Enumeration logEntries, int startingMessage) {
		int count = 0;
		while (logEntries.hasMoreElements()) {
			LogEntry entry = (LogEntry) logEntries.nextElement();
			assertEquals("Wrong log message.", String.valueOf(startingMessage), entry.getMessage());
			startingMessage--;
			count++;
		}
		return count;
	}
}
