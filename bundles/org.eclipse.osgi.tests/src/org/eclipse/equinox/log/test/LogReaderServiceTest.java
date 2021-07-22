/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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

	@Override
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

		Map<String, LogLevel> copyLogLevels = new HashMap<>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	@Override
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
		log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	public void testaddListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		reader.addLogListener(listener);
		log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
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
		log.log(logReference, LogService.LOG_INFO, "info", new Throwable("test")); //$NON-NLS-1$ //$NON-NLS-2$
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
		TestListener listener = new TestListener(testBundle.getLocation());
		reader.addLogListener(listener);

		testBundle.start();

		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof BundleEvent);
	}

	public void testLogBundleEventSynchronous() throws Exception {
		// this is just a bundle that is harmless to start/stop
		final Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		final AtomicReference<Thread> logThread = new AtomicReference<>();
		SynchronousLogListener listener = entry -> {
			if (entry.getBundle() == testBundle) {
				logThread.compareAndSet(null, Thread.currentThread());
			}
		};
		reader.addLogListener(listener);
		testBundle.start();

		assertEquals("Wrong thread for synchronous bundle event logs.", Thread.currentThread(), logThread.get());

	}

	public void testLogServiceEventInfo() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof ServiceEvent);
	}

	public void testLogServiceEventDebug() throws Exception {
		ServiceRegistration registration = OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);

		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		registration.setProperties(new Hashtable());
		ExtendedLogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_DEBUG);
		assertEquals("Wrong level.", LogLevel.DEBUG, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof ServiceEvent);
	}

	public void testLogFrameworkEvent() throws Exception {
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		final AtomicReference<LogEntry> logEntry = new AtomicReference<>();
		final CountDownLatch countDown = new CountDownLatch(1);
		LogListener listener = entry -> {
			if ((entry.getLoggerName()).startsWith("Events.Framework.")) {
				logEntry.set(entry);
				countDown.countDown();
			}
		};
		reader.addLogListener(listener);
		installer.refreshPackages(new Bundle[] {testBundle});

		countDown.await(5, TimeUnit.SECONDS);

		ExtendedLogEntry entry = (ExtendedLogEntry) logEntry.get();
		assertNotNull("Framework event not logged", entry);
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertEquals("Wrong level.", LogLevel.INFO, entry.getLogLevel());
		assertTrue("Wrong context: " + entry.getContext(), entry.getContext() instanceof FrameworkEvent);
		assertEquals("Wrong bundle.", getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION), entry.getBundle());
		assertEquals("Wrong logger name.", "Events.Framework." + entry.getBundle().getSymbolicName(), entry.getLoggerName());
	}

	public void testLogFrameworkEventType() throws Exception {
		final List<LogEntry> events = new CopyOnWriteArrayList<>();
		final CountDownLatch countDown = new CountDownLatch(3);
		final Bundle b = getContext().getBundle();
		LogListener listener = entry -> {
			if (b.equals(entry.getBundle())) {
				events.add(entry);
				countDown.countDown();
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
		Map<String, Object> configuration = new HashMap<>();
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
		Map<String, Object> configuration = new HashMap<>();
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

	public void testLoggerContextSetLogLevelsWithBundleInstalledAndLogger() throws Exception {
		Bundle bundle = null;
		String loggerName = "test.logger";
		try {
			bundle = installer.installBundle("test.logging.a");
			bundle.start();
			Logger logger = log.getLogger(bundle, loggerName, Logger.class);
			assertNotNull("Logger cannot be null", logger);
			//Bundle is installed and a logger is associated with that bundle before setting the log level
			setAndAssertLogLevel(bundle.getSymbolicName(), loggerName);

			TestListener listener = new TestListener(bundle.getLocation());
			reader.addLogListener(listener);
			for (LogLevel logLevel : LogLevel.values()) {
				String message = logLevel.name() + " MESSAGE";
				doLogging(bundle, logger, listener, logLevel, message);
			}
		} finally {
			if (bundle != null) {
				bundle.stop();
				bundle.uninstall();
			}
		}
	}

	public void testLoggerContextSetLogLevelsWithBundleInstalledAndNoLogger() throws Exception {
		Bundle bundle = null;
		String loggerName = "test.logger";
		try {
			bundle = installer.installBundle("test.logging.a");
			bundle.start();
			//Bundle is installed but a logger is not associated with the bundle before setting the log level
			setAndAssertLogLevel(bundle.getSymbolicName(), loggerName);
			Logger logger = log.getLogger(bundle, loggerName, Logger.class);
			assertNotNull("Logger cannot be null", logger);
			TestListener listener = new TestListener(bundle.getLocation());
			reader.addLogListener(listener);
			for (LogLevel logLevel : LogLevel.values()) {
				String message = logLevel.name() + " MESSAGE";
				doLogging(bundle, logger, listener, logLevel, message);
			}
		} finally {
			if (bundle != null) {
				bundle.stop();
				bundle.uninstall();
			}
		}
	}

	public void testLoggerContextSetLogLevelsWithoutBundleAndLogger() throws Exception {
		Bundle bundle = null;
		String loggerName = "test.logger";
		//Bundle is not installed and also the logger is not associated with the bundle before setting the log level
		setAndAssertLogLevel("test.logging.a", loggerName);
		try {
			bundle = installer.installBundle("test.logging.a");
			bundle.start();
			Logger logger = log.getLogger(bundle, loggerName, Logger.class);
			assertNotNull("Logger cannot be null", logger);
			TestListener listener = new TestListener(bundle.getLocation());
			reader.addLogListener(listener);
			for (LogLevel logLevel : LogLevel.values()) {
				String message = logLevel.name() + " MESSAGE";
				doLogging(bundle, logger, listener, logLevel, message);
			}
		} finally {
			if (bundle != null) {
				bundle.stop();
				bundle.uninstall();
			}
		}
	}

	private void setAndAssertLogLevel(String loggerContextName, String loggerName) {
		LoggerContext loggerContext = loggerAdmin.getLoggerContext(loggerContextName);
		Map<String, LogLevel> logLevels = loggerContext.getLogLevels();
		logLevels.put(loggerName, LogLevel.TRACE);
		loggerContext.setLogLevels(logLevels);
		assertEquals("Log levels not set for " + loggerContext.getName(), logLevels, loggerContext.getLogLevels());
		assertEquals("Wrong effective level", LogLevel.TRACE, loggerContext.getEffectiveLogLevel(loggerName));
	}

	private void doLogging(Bundle bundle, Logger logger, TestListener listener, LogLevel logLevel, String message) throws Exception {
		logToLogger(logger, message, logLevel);
		ExtendedLogEntry logEntry = listener.getEntryX();
		assertEquals("Wrong message logged", message, logEntry.getMessage());
		assertEquals("Wrong Log level", logLevel, logEntry.getLogLevel());
		assertEquals("Wrong Logger name", logger.getName(), logEntry.getLoggerName());
		assertEquals("Wrong bundle", bundle.getSymbolicName(), logEntry.getBundle().getSymbolicName());
	}

	private void logToLogger(Logger logger, String message, LogLevel logLevel) {
		switch (logLevel) {
			case AUDIT :
				logger.audit(message);
				break;
			case ERROR :
				logger.error(message);
				break;
			case WARN :
				logger.warn(message);
				break;
			case INFO :
				logger.info(message);
				break;
			case DEBUG :
				logger.debug(message);
				break;
			case TRACE :
				logger.trace(message);
				break;
			default :
				fail("Unknown Log level");
		}
	}

	public void testBundleEventsLogged() throws Exception {
		String testBundleLoc = installer.getBundleLocation("test.logging.a");
		TestListener listener = new TestListener(testBundleLoc);
		reader.addLogListener(listener);
		Bundle bundle = installer.installBundle("test.logging.a");

		bundle.start();
		bundle.stop();
		bundle.update();
		bundle.start();
		bundle.stop();
		bundle.uninstall();

		assertBundleEventLog("BundleEvent INSTALLED", bundle, listener);
		assertBundleEventLog("BundleEvent RESOLVED", bundle, listener);
		assertBundleEventLog("BundleEvent STARTING", bundle, listener);
		assertBundleEventLog("BundleEvent STARTED", bundle, listener);
		assertBundleEventLog("BundleEvent STOPPING", bundle, listener);
		assertBundleEventLog("BundleEvent STOPPED", bundle, listener);
		assertBundleEventLog("BundleEvent UNRESOLVED", bundle, listener);
		assertBundleEventLog("BundleEvent UPDATED", bundle, listener);
		assertBundleEventLog("BundleEvent RESOLVED", bundle, listener);
		assertBundleEventLog("BundleEvent STARTING", bundle, listener);
		assertBundleEventLog("BundleEvent STARTED", bundle, listener);
		assertBundleEventLog("BundleEvent STOPPING", bundle, listener);
		assertBundleEventLog("BundleEvent STOPPED", bundle, listener);
		assertBundleEventLog("BundleEvent UNRESOLVED", bundle, listener);
		assertBundleEventLog("BundleEvent UNINSTALLED", bundle, listener);

	}

	private void assertBundleEventLog(String message, Bundle bundle, TestListener listener) throws InterruptedException {
		LogEntry logEntry = listener.getEntryX();
		assertEquals("Wrong message.", message, logEntry.getMessage());
		assertEquals("Wrong bundle.", bundle, logEntry.getBundle());
		assertEquals("Wrong logger name.", "Events.Bundle." + bundle.getSymbolicName(), logEntry.getLoggerName());

	}

}
