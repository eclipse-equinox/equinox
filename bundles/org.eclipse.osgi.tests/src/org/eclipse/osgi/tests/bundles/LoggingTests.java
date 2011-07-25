/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.*;
import junit.framework.*;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.*;
import org.osgi.service.event.*;
import org.osgi.service.log.*;

public class LoggingTests extends AbstractBundleTests {
	static final String EQUINOX_LOGGER = "org.eclipse.equinox.logger";

	public static Test suite() {
		return new TestSuite(LoggingTests.class);
	}

	class LogServiceReference {
		private final ServiceReference logRef;
		final ExtendedLogService logService;
		private final ServiceReference readerRef;
		final ExtendedLogReaderService readerService;
		private final ServiceReference fwkLogRef;
		final FrameworkLog fwkLog;

		public LogServiceReference(ServiceReference logRef, ExtendedLogService logService, ServiceReference readerRef, ExtendedLogReaderService readerService, ServiceReference fwkLogRef, FrameworkLog fwkLog) {

			this.logRef = logRef;
			this.logService = logService;
			this.readerRef = readerRef;
			this.readerService = readerService;
			this.fwkLogRef = fwkLogRef;
			this.fwkLog = fwkLog;
		}

		public void unget(BundleContext bc) {
			bc.ungetService(logRef);
			bc.ungetService(readerRef);
			bc.ungetService(fwkLogRef);
		}
	}

	class ILogEntry {
		final IStatus status;
		final String plugin;

		ILogEntry(IStatus status, String plugin) {
			this.status = status;
			this.plugin = plugin;
		}
	}

	class TestListener {
		final List context;
		boolean found = false;

		TestListener(List context) {
			this.context = context;
		}

		synchronized boolean waitforContext() {
			if (context.size() > 0)
				try {
					wait(5000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			return context.size() == 0;

		}

		synchronized void checkContext(IStatus check) {
			if (check == null)
				return;
			if (context.size() > 0) {
				IStatus expected = (IStatus) context.get(0);
				try {
					assertEquals(expected.getSeverity(), check.getSeverity());
					assertEquals(expected.getCode(), check.getCode());
					assertEquals(expected.getMessage(), check.getMessage());
					assertEquals(expected.getPlugin(), check.getPlugin());
				} catch (AssertionFailedError e) {
					return;
				}
				context.remove(0);
				if (context.size() == 0)
					notifyAll();
			}
		}

		synchronized void addContext(List moreContext) {
			context.addAll(moreContext);
		}
	}

	class TestILogListener extends TestListener implements ILogListener {
		TestILogListener(List context) {
			super(context);
		}

		List entries = new ArrayList();

		public void logging(IStatus status, String plugin) {
			entries.add(new ILogEntry(status, plugin));
			checkContext(status);
		}

		List getEntries() {
			return entries;
		}
	}

	class TestLogListener extends TestListener implements LogListener, LogFilter {
		TestLogListener(List context) {
			super(context);
		}

		List entries = new ArrayList();

		public void logged(LogEntry entry) {
			entries.add(entry);
			Object check = ((ExtendedLogEntry) entry).getContext();
			if (check instanceof FrameworkLogEntry)
				check = ((FrameworkLogEntry) check).getContext();
			if (check instanceof IStatus)
				checkContext((IStatus) check);
		}

		List getEntries() {
			return entries;
		}

		public boolean isLoggable(Bundle bundle, String loggerName, int logLevel) {
			return EQUINOX_LOGGER.equals(loggerName);
		}
	}

	private LogServiceReference getLogService(BundleContext bc) {
		ServiceReference logRef = bc.getServiceReference(ExtendedLogService.class.getName());
		assertNotNull("log service ref is null", logRef);
		ServiceReference readerRef = bc.getServiceReference(ExtendedLogReaderService.class.getName());
		assertNotNull("log reader ref is null", readerRef);
		ServiceReference fwkLogRef = bc.getServiceReference(FrameworkLog.class.getName());
		assertNotNull("framework log ref is null", fwkLogRef);

		ExtendedLogService logService = (ExtendedLogService) bc.getService(logRef);
		assertNotNull("log service is null", logService);
		ExtendedLogReaderService readerService = (ExtendedLogReaderService) bc.getService(readerRef);
		assertNotNull("reader service is null", readerService);
		FrameworkLog fwkLog = (FrameworkLog) bc.getService(fwkLogRef);
		assertNotNull("framework log is null", fwkLog);
		return new LogServiceReference(logRef, logService, readerRef, readerService, fwkLogRef, fwkLog);
	}

	public void testLogService() throws BundleException {
		// Tests listeners at all levels when logged with the LogService
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		testBundle.start();
		BundleContext bc = testBundle.getBundleContext();
		LogServiceReference logRef = getLogService(bc);
		IStatus a = new Status(IStatus.ERROR, testBundle.getSymbolicName(), getName());
		List context = new ArrayList();
		context.add(a);
		TestILogListener iLogListener1 = new TestILogListener(new ArrayList(context));
		TestILogListener iLogListener2 = new TestILogListener(new ArrayList(context));
		TestLogListener logListener = new TestLogListener(new ArrayList(context));

		ILog testLog = Platform.getLog(testBundle);
		testLog.addLogListener(iLogListener1);
		Platform.addLogListener(iLogListener2);
		logRef.readerService.addLogListener(logListener, logListener);
		try {
			ExtendedLogService logService = logRef.logService;
			Logger logger = logService.getLogger(testBundle, EQUINOX_LOGGER);
			logger.log(a, LogService.LOG_ERROR, a.getMessage());
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			logger.log(new FrameworkLogEntry(a, testBundle.getSymbolicName(), a.getSeverity(), a.getCode(), a.getMessage(), 0, null, null), LogService.LOG_ERROR, a.getMessage());
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			logListener.entries.clear();
			logger.log(LogService.LOG_ERROR, a.getMessage());
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			// there is no status here
			assertFalse("Missing context", logListener.waitforContext());
			// but we should have received one log entry
			assertEquals("Wrong number of entries", 1, logListener.entries.size());
		} finally {
			testLog.removeLogListener(iLogListener1);
			Platform.removeLogListener(iLogListener2);
			logRef.readerService.removeLogListener(logListener);
			logRef.unget(bc);
		}
	}

	public void testPluginILog() throws BundleException {
		// Tests listeners at all levels when logged with a plugin ILog
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		testBundle.start();
		BundleContext bc = testBundle.getBundleContext();
		LogServiceReference logRef = getLogService(bc);
		IStatus a = new Status(IStatus.ERROR, testBundle.getSymbolicName(), getName());
		List context = new ArrayList();
		context.add(a);
		TestILogListener iLogListener1 = new TestILogListener(new ArrayList(context));
		TestILogListener iLogListener2 = new TestILogListener(new ArrayList(context));
		TestLogListener logListener = new TestLogListener(new ArrayList(context));

		ILog testLog = Platform.getLog(testBundle);
		testLog.addLogListener(iLogListener1);
		Platform.addLogListener(iLogListener2);
		logRef.readerService.addLogListener(logListener, logListener);
		try {
			testLog.log(a);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			testLog.log(a);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());
		} finally {
			testLog.removeLogListener(iLogListener1);
			Platform.removeLogListener(iLogListener2);
			logRef.readerService.removeLogListener(logListener);
			logRef.unget(bc);
		}
	}

	public void testRuntimeLog() throws BundleException {
		// Tests listeners at all levels when logged with the RuntimeLog
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		testBundle.start();
		BundleContext bc = testBundle.getBundleContext();
		LogServiceReference logRef = getLogService(bc);
		IStatus a = new Status(IStatus.ERROR, testBundle.getSymbolicName(), getName());
		List context = new ArrayList();
		context.add(a);
		TestILogListener iLogListener1 = new TestILogListener(new ArrayList(context));
		TestILogListener iLogListener2 = new TestILogListener(new ArrayList(context));
		TestLogListener logListener = new TestLogListener(new ArrayList(context));

		ILog testLog = Platform.getLog(testBundle);
		testLog.addLogListener(iLogListener1);
		Platform.addLogListener(iLogListener2);
		logRef.readerService.addLogListener(logListener, logListener);
		try {
			RuntimeLog.log(a);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			RuntimeLog.log(a);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());
		} finally {
			testLog.removeLogListener(iLogListener1);
			Platform.removeLogListener(iLogListener2);
			logRef.readerService.removeLogListener(logListener);
			logRef.unget(bc);
		}
	}

	public void testFrameworkLog() throws BundleException {
		// Tests listeners at all levels when logged with the FrameworkLog
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		testBundle.start();
		BundleContext bc = testBundle.getBundleContext();
		LogServiceReference logRef = getLogService(bc);
		IStatus a = new Status(IStatus.ERROR, testBundle.getSymbolicName(), getName());
		List context = new ArrayList();
		context.add(a);
		TestILogListener iLogListener1 = new TestILogListener(new ArrayList(context));
		TestILogListener iLogListener2 = new TestILogListener(new ArrayList(context));
		TestLogListener logListener = new TestLogListener(new ArrayList(context));

		ILog testLog = Platform.getLog(testBundle);
		testLog.addLogListener(iLogListener1);
		Platform.addLogListener(iLogListener2);
		logRef.readerService.addLogListener(logListener, logListener);
		try {
			FrameworkLogEntry fwkLogEntry = new FrameworkLogEntry(a, testBundle.getSymbolicName(), a.getSeverity(), a.getCode(), a.getMessage(), 0, null, null);
			logRef.fwkLog.log(fwkLogEntry);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			logRef.fwkLog.log(fwkLogEntry);
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			assertTrue("Missing context", logListener.waitforContext());

			iLogListener1.addContext(new ArrayList(context));
			iLogListener2.addContext(new ArrayList(context));
			logListener.addContext(new ArrayList(context));

			logListener.entries.clear();
			logRef.fwkLog.log(new FrameworkLogEntry(testBundle.getSymbolicName(), a.getSeverity(), a.getCode(), a.getMessage(), 0, null, null));
			assertTrue("Missing context", iLogListener1.waitforContext());
			assertTrue("Missing context", iLogListener2.waitforContext());
			// there is no status here
			assertFalse("Missing context", logListener.waitforContext());
			// but we should have received one log entry
			assertEquals("Wrong number of entries", 1, logListener.entries.size());
		} finally {
			testLog.removeLogListener(iLogListener1);
			Platform.removeLogListener(iLogListener2);
			logRef.readerService.removeLogListener(logListener);
			logRef.unget(bc);
		}
	}

	public void testEventAdminAdapter1() {
		ServiceReference eventRef = getContext().getServiceReference(EventAdmin.class.getName());
		assertNotNull("No eventAdmin.", eventRef);
		LogServiceReference logRef = getLogService(getContext());

		final List events = new ArrayList();
		EventHandler testHandler = new EventHandler() {

			public void handleEvent(Event event) {
				synchronized (events) {
					events.add(event);
					events.notifyAll();
				}
			}
		};

		Dictionary props = new Hashtable();
		props.put(EventConstants.EVENT_TOPIC, "org/osgi/service/log/LogEntry/*");
		ServiceRegistration handlerReg = getContext().registerService(EventHandler.class.getName(), testHandler, props);
		try {
			logRef.logService.log(LogService.LOG_ERROR, getName());
			Event testEvent = null;
			synchronized (events) {
				if (events.size() == 0)
					try {
						events.wait(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				if (events.size() > 0)
					testEvent = (Event) events.get(0);
			}
			assertNotNull("No event fired", testEvent);
			assertEquals("Wrong message", getName(), testEvent.getProperty("message"));
		} finally {
			handlerReg.unregister();
			logRef.unget(getContext());
		}
	}

	public void testEventAdminAdapter2() {
		ServiceReference eventRef = getContext().getServiceReference(EventAdmin.class.getName());
		assertNotNull("No eventAdmin.", eventRef);
		LogServiceReference logRef = getLogService(getContext());

		final List events = new ArrayList();
		EventHandler testHandler = new EventHandler() {

			public void handleEvent(Event event) {
				synchronized (events) {
					events.add(event);
					events.notifyAll();
				}
				throw new NullPointerException();
			}
		};

		Dictionary props = new Hashtable();
		props.put(EventConstants.EVENT_TOPIC, "org/osgi/service/log/LogEntry/*");
		ServiceRegistration handlerReg = getContext().registerService(EventHandler.class.getName(), testHandler, props);
		try {
			logRef.logService.log(LogService.LOG_ERROR, getName());
			int size = 0;
			synchronized (events) {
				for (int i = 0; i < 3 && events.size() < 3; i++)
					try {
						events.wait(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				size = events.size();
			}
			assertEquals("Should only get one event from bad handler", 2, size);
		} finally {
			handlerReg.unregister();
			logRef.unget(getContext());
		}
	}

	public void testBug347183() throws BundleException {
		// test recursive logging
		final Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		testBundle.start();
		LogServiceReference logRef = getLogService(getContext());
		ILogListener recurseLog = new ILogListener() {

			public void logging(IStatus status, String plugin) {
				Platform.getLog(testBundle).log(status);
			}
		};
		Platform.addLogListener(recurseLog);
		try {
			logRef.fwkLog.log(new FrameworkLogEntry(getContext().getBundle().getSymbolicName(), FrameworkLogEntry.ERROR, 0, "Test message", 0, null, null));

			// prime the log so we don't have to create it in the logging call
			Platform.getLog(testBundle);

			logRef.fwkLog.log(new FrameworkLogEntry(getContext().getBundle().getSymbolicName(), FrameworkLogEntry.ERROR, 0, "Test message", 0, null, null));
		} finally {
			Platform.removeLogListener(recurseLog);
		}
	}
}
